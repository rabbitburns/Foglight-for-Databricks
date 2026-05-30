"""
Build DatabricksAgent-<version>.car Foglight cartridge.
Usage: python build_cartridge.py [version] [--deploy] [--fglam-dir=PATH]
Output: target/DatabricksAgent-<version>.car

--deploy          Compile Java, build .car, and deploy agent JAR directly into FglAM's agent cache.
                  FglAM re-reads the JAR on next agent stop/start — no FglAM restart needed.
--fglam-dir=PATH  Override default FglAM root (default: C:\\Quest\\Foglight\\fglam)
"""
import sys
import re
import subprocess
import zipfile
import os
import io
import tarfile
import gzip

_args = sys.argv[1:]
VERSION  = next((a for a in _args if not a.startswith("--")), "1.0.18")
DEPLOY   = "--deploy" in _args
FGLAM_DIR = next((a.split("=", 1)[1] for a in _args if a.startswith("--fglam-dir=")),
                 r"C:\Quest\Foglight\fglam")
VER_FLAT = VERSION.replace(".", "_")

# Agent package version — must change with every release so FglAM knows to download the new .gar.
# Synced to cartridge version so any new .car automatically triggers a FglAM package update.
AGENT_VER = VERSION

# Jackson jars bundled with the agent
M2 = os.path.join(os.environ.get("USERPROFILE", os.path.expanduser("~")), ".m2", "repository")
JACKSON_JARS = [
    os.path.join(M2, "com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar"),
    os.path.join(M2, "com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar"),
    os.path.join(M2, "com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar"),
]

DATABRICKS_PROPERTIES_TEMPLATE = """\
workspaceUrl=https://<your-workspace>.azuredatabricks.net/
accessToken=<your-token-here>
collectionIntervalSeconds=60
accountId=default
accountName=Databricks
"""

CDT_BINDING = f"""\
<?xml version="1.0" encoding="UTF-8"?>
<adapter type="FglAM">
    <cdt-bindings>
        <cdt-config type="DatabricksAgent" file="databricks-model-root-cdt.xml"/>
    </cdt-bindings>
</adapter>
"""

INSTALLERS_XML = f"""\
<?xml version="1.0" encoding="UTF-8"?>
<installers>
    <installer name="DatabricksAgent" version="{AGENT_VER}" filename="DatabricksAgent.gar" type="fglam-client-pkg">
        <agent-types>
            <agent-type name="DatabricksAgent"/>
        </agent-types>
    </installer>
</installers>
"""


def build_agent_gar():
    """Build the agent .gar (gzip tar) for remote FglAM deployment.
    Structure: agent.manifest at root + lib/*.jar — matches fglam-client-pkg format."""
    tar_buf = io.BytesIO()
    with tarfile.open(fileobj=tar_buf, mode="w") as tar:
        def add_bytes(name, data):
            info = tarfile.TarInfo(name=name)
            info.size = len(data)
            tar.addfile(info, io.BytesIO(data))

        with open("src/main/resources/config/agent.manifest", "r", encoding="utf-8") as f:
            manifest_text = f.read()
        manifest_text = re.sub(r'\bver="[^"]*"', f'ver="{VERSION}"', manifest_text)
        manifest_text = re.sub(r'\bbuild-id="[^"]*"', f'build-id="{VERSION}"', manifest_text)
        add_bytes("agent.manifest", manifest_text.encode("utf-8"))

        with open("target/databricks-agent.jar", "rb") as f:
            add_bytes("lib/databricks-agent.jar", f.read())

        for jar_path in JACKSON_JARS:
            jar_name = os.path.basename(jar_path)
            with open(jar_path, "rb") as f:
                add_bytes(f"lib/{jar_name}", f.read())

    gz_buf = io.BytesIO()
    with gzip.GzipFile(fileobj=gz_buf, mode="wb", mtime=0) as gz:
        gz.write(tar_buf.getvalue())
    return gz_buf.getvalue()


def make_manifest(topo_size, binding_size, model_root_size, wcf_files,
                  installers_xml_size, agent_zip_size, monitoring_policy_size):
    wcf_entries = '\n'.join(
        f'            <file name="{path}" size="{size}"/>' if not is_dir
        else f'            <file name="{path}" directory="true"/>'
        for path, size, is_dir in wcf_files
    )
    return f"""\
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
    <cartridge domain="Cloud" foglight-version="5.7.5.7" classloading-style="local_first" type="free">
        <identity name="DatabricksAgent" version="{VERSION}" buildId="{VERSION}"
                  creation-date="2026-04-05T00:00:00Z" author="Quest Software"/>

        <component type="Topology Types">
            <identity name="DatabricksAgent-TopologyTypes" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="topology-types.xml" size="{topo_size}"/>
        </component>

        <component type="CDT" deployment-item="cdt-binding.xml">
            <identity name="DatabricksAgent-CDT" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="cdt-binding.xml" size="{binding_size}"/>
            <file name="databricks-model-root-cdt.xml" size="{model_root_size}"/>
        </component>

        <component type="WCF Dashboard" deployment-item="wcf">
            <identity name="DatabricksAgent-WCF" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="wcf" directory="true"/>
{wcf_entries}
        </component>

        <component type="Monitoring Policy">
            <identity name="DatabricksAgent-Properties" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="monitoring-policy.xml" size="{monitoring_policy_size}"/>
        </component>

        <component type="Installers" deployment-item="installers.xml">
            <identity name="DatabricksAgent-Installer" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="installers.xml" size="{installers_xml_size}"/>
            <file name="DatabricksAgent.gar" size="{agent_zip_size}"/>
        </component>
    </cartridge>
</manifest>
""".encode("utf-8")


def collect_wcf_files(wcf_src_dir):
    """Walk assembly/wcf/ and return list of (zip_path, byte_content) for all files,
    plus directory entries needed by the manifest."""
    entries = []   # (manifest_path, size, is_dir)  — for manifest
    contents = []  # (zip_path, bytes)               — for zip
    for root, dirs, files in os.walk(wcf_src_dir):
        for fname in sorted(files):
            abs_path = os.path.join(root, fname)
            rel = os.path.relpath(abs_path, os.path.dirname(wcf_src_dir)).replace("\\", "/")
            with open(abs_path, "rb") as f:
                data = f.read()
            entries.append((rel, len(data), False))
            contents.append((rel, data))
    return entries, contents


def find_mvn():
    """Return path to mvn executable, searching common install locations if not on PATH."""
    import shutil
    mvn = shutil.which("mvn") or shutil.which("mvn.cmd")
    if mvn:
        return mvn
    candidates = [
        r"C:\Apache\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd",
        r"C:\Program Files\Apache\maven\bin\mvn.cmd",
        r"C:\tools\maven\bin\mvn.cmd",
    ]
    for c in candidates:
        if os.path.isfile(c):
            return c
    # Search C:\Apache for any mvn.cmd
    for root, dirs, files in os.walk(r"C:\Apache"):
        for f in files:
            if f in ("mvn.cmd", "mvn"):
                return os.path.join(root, f)
    sys.exit("ERROR: mvn not found. Add Maven bin to PATH or install Maven.")


def validate_wcf():
    """Validate all WCF module XML files for known JiBX-fatal errors:
    1. view/composite-view elements appearing after script-function elements —
       Foglight drops the entire module when ordering is violated.
    2. Unknown property names in specific component types — JiBX throws
       'Unexpected property name' and drops the module (e.g. showLabels in bubble chart).
    """
    import xml.etree.ElementTree as ET

    # Properties that are NOT valid for these component types (confirmed by JiBX errors).
    # Scrolling in wcf.grid2: use pageOptions > scrollbars (enum: auto), not scroll (bool).
    INVALID_PROPS = {
        "wcf.html-chart.scatter.bubble": {"showLabels"},
        "wcf.grid2":                     {"scroll", "scrollbars"},  # scrollbars belongs inside pageOptions
    }

    errors = []
    for root, dirs, files in os.walk("assembly/wcf"):
        for fname in files:
            if fname != "wcf.xml":
                continue
            path = os.path.join(root, fname)
            try:
                tree = ET.parse(path)
            except ET.ParseError as e:
                errors.append(f"{path}: XML parse error: {e}")
                continue
            module = tree.getroot()

            # Check 1: view/composite-view ordering relative to script-functions
            first_sf_pos = None
            for pos, child in enumerate(module):
                tag = child.tag.split("}")[-1]
                if tag == "script-function" and first_sf_pos is None:
                    first_sf_pos = pos
                if tag in ("view", "composite-view") and first_sf_pos is not None:
                    vid = child.get("id", "?")
                    errors.append(
                        f"{path}: view/composite-view id={vid} appears after a "
                        f"script-function (pos {first_sf_pos}). "
                        f"All views must precede all script-functions."
                    )

            # Check 2: invalid property names per component type
            for view in module.iter():
                tag = view.tag.split("}")[-1]
                if tag not in ("view", "composite-view"):
                    continue
                component = view.get("component", "")
                invalid = INVALID_PROPS.get(component)
                if not invalid:
                    continue
                config = view.find("config")
                if config is None:
                    continue
                for prop in config.findall("property"):
                    pname = prop.get("name", "")
                    if pname in invalid:
                        errors.append(
                            f"{path}: view id={view.get('id')} component={component} "
                            f"has invalid property '{pname}' (not supported by JiBX schema)."
                        )

    if errors:
        print("WCF VALIDATION FAILED:")
        for e in errors:
            print(f"  {e}")
        sys.exit(1)
    print("WCF validation passed.")


def compile_java():
    mvn = find_mvn()
    env = os.environ.copy()
    result = subprocess.run(
        [mvn, "package", "-Dmaven.test.skip=true", "-q"],
        env=env, capture_output=True, text=True
    )
    if result.returncode != 0:
        print("Maven compilation failed:")
        print(result.stdout)
        print(result.stderr)
        sys.exit(1)
    print("Java compiled.")


def main():
    validate_wcf()
    compile_java()
    os.makedirs("target", exist_ok=True)
    out = f"target/DatabricksAgent-{VERSION}.car"

    cart = f"DatabricksAgent-{VER_FLAT}"
    tdir = f"{cart}/DatabricksAgent-TopologyTypes-{VER_FLAT}"
    cdir = f"{cart}/DatabricksAgent-CDT-{VER_FLAT}"
    wdir = f"{cart}/DatabricksAgent-WCF-{VER_FLAT}"
    idir = f"{cart}/DatabricksAgent-Installer-{VER_FLAT}"

    with open("assembly/topology/cdt.xml", "rb") as f:
        model_root_bytes = f.read()

    with open("assembly/topology/topology-types.xml", "rb") as f:
        topo_bytes = f.read()

    binding_bytes = CDT_BINDING.encode("utf-8")
    installers_bytes = INSTALLERS_XML.encode("utf-8")
    agent_zip_bytes = build_agent_gar()
    agent_zip_name = "DatabricksAgent.gar"

    with open("assembly/agent-properties/monitoring-policy.xml", "rb") as f:
        monitoring_policy_bytes = f.read()

    wcf_entries, wcf_contents = collect_wcf_files("assembly/wcf")

    manifest = make_manifest(
        len(topo_bytes),
        len(binding_bytes),
        len(model_root_bytes),
        wcf_entries,
        len(installers_bytes),
        len(agent_zip_bytes),
        len(monitoring_policy_bytes),
    )

    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.xml", manifest)
        z.writestr(f"{tdir}/topology-types.xml", topo_bytes)
        z.writestr(f"{cdir}/cdt-binding.xml", binding_bytes)
        z.writestr(f"{cdir}/databricks-model-root-cdt.xml", model_root_bytes)
        for rel_path, data in wcf_contents:
            z.writestr(f"{wdir}/{rel_path}", data)
        pdir = f"{cart}/DatabricksAgent-Properties-{VER_FLAT}"
        z.writestr(f"{pdir}/monitoring-policy.xml", monitoring_policy_bytes)
        z.writestr(f"{idir}/installers.xml", installers_bytes)
        z.writestr(f"{idir}/{agent_zip_name}", agent_zip_bytes)

    print(f"Built {out}  ({os.path.getsize(out):,} bytes)")
    with zipfile.ZipFile(out) as z:
        for e in z.infolist():
            print(f"  {e.file_size:6d}  {e.filename}")

    if DEPLOY:
        # Find the agent cache dir: fglam/agents/DatabricksAgent/<ver>-<ver>/lib/
        agents_root = os.path.join(FGLAM_DIR, "agents", "DatabricksAgent")
        if not os.path.isdir(agents_root):
            print(f"\nWARNING: FglAM agent cache not found: {agents_root}")
            print("Skipping local deployment. Use --fglam-dir=PATH to override.")
        else:
            # Pick the most recently modified version dir — that's the one FglAM is using
            ver_dirs = sorted(
                [d for d in os.listdir(agents_root)
                 if os.path.isdir(os.path.join(agents_root, d))],
                key=lambda d: os.path.getmtime(os.path.join(agents_root, d)),
                reverse=True
            )
            if not ver_dirs:
                print(f"\nWARNING: No version directory found under {agents_root}")
            else:
                lib_dir = os.path.join(agents_root, ver_dirs[0], "lib")
                dest = os.path.join(lib_dir, "databricks-agent.jar")
                with open("target/databricks-agent.jar", "rb") as f:
                    jar_bytes = f.read()
                with open(dest, "wb") as f:
                    f.write(jar_bytes)
                print(f"\nDeployed databricks-agent.jar to {dest}")
                print("Next steps:")
                print("  1. Install the .car in Foglight (Administration -> Cartridges)")
                print("  2. Stop the DatabricksAgent instance in Administration -> Agents")
                print("  3. Start it — new code loads immediately, no FglAM restart needed.")


if __name__ == "__main__":
    main()
