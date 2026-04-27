"""
Build DatabricksAgent-<version>.car Foglight cartridge.
Usage: python build_cartridge.py [version]
Output: target/DatabricksAgent-<version>.car
"""
import sys
import zipfile
import os
import io

VERSION = sys.argv[1] if len(sys.argv) > 1 else "1.0.18"
VER_FLAT = VERSION.replace(".", "_")

# Agent protocol version — matches agent.manifest ver/build-id and FglAM directory name.
# This is independent of the cartridge version.
AGENT_VER = "1.0.6"

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
    <installer name="DatabricksAgent-{AGENT_VER}.zip" version="{AGENT_VER}" filename="DatabricksAgent-{AGENT_VER}.zip" type="manual">
        <agent-types>
            <agent-type name="DatabricksAgent"/>
        </agent-types>
    </installer>
</installers>
"""


def build_agent_zip():
    """Build the agent deployment zip (embedded in the .car as an installer).
    Structure mirrors what FglAM expects under its agents/ directory."""
    buf = io.BytesIO()
    base = f"DatabricksAgent/{AGENT_VER}-{AGENT_VER}"
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        # agent.manifest
        with open("src/main/resources/config/agent.manifest", "rb") as f:
            z.writestr(f"{base}/config/agent.manifest", f.read())
        # properties template
        z.writestr(f"{base}/config/databricks.properties", DATABRICKS_PROPERTIES_TEMPLATE)
        # agent jar
        with open("target/databricks-agent.jar", "rb") as f:
            z.writestr(f"{base}/lib/databricks-agent.jar", f.read())
        # jackson jars
        for jar_path in JACKSON_JARS:
            jar_name = os.path.basename(jar_path)
            with open(jar_path, "rb") as f:
                z.writestr(f"{base}/lib/{jar_name}", f.read())
    return buf.getvalue()


def make_manifest(topo_size, binding_size, model_root_size, wcf_files,
                  installers_xml_size, agent_zip_size):
    wcf_entries = '\n'.join(
        f'            <file name="{path}" size="{size}"/>' if not is_dir
        else f'            <file name="{path}" directory="true"/>'
        for path, size, is_dir in wcf_files
    )
    agent_zip_name = f"DatabricksAgent-{AGENT_VER}.zip"
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

        <component type="Installers" deployment-item="installers.xml">
            <identity name="DatabricksAgent-Installer" version="{VERSION}"
                      creation-date="2026-04-05T00:00:00Z"/>
            <file name="installers.xml" size="{installers_xml_size}"/>
            <file name="{agent_zip_name}" size="{agent_zip_size}"/>
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


def main():
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
    agent_zip_bytes = build_agent_zip()
    agent_zip_name = f"DatabricksAgent-{AGENT_VER}.zip"

    wcf_entries, wcf_contents = collect_wcf_files("assembly/wcf")

    manifest = make_manifest(
        len(topo_bytes),
        len(binding_bytes),
        len(model_root_bytes),
        wcf_entries,
        len(installers_bytes),
        len(agent_zip_bytes),
    )

    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.xml", manifest)
        z.writestr(f"{tdir}/topology-types.xml", topo_bytes)
        z.writestr(f"{cdir}/cdt-binding.xml", binding_bytes)
        z.writestr(f"{cdir}/databricks-model-root-cdt.xml", model_root_bytes)
        for rel_path, data in wcf_contents:
            z.writestr(f"{wdir}/{rel_path}", data)
        z.writestr(f"{idir}/installers.xml", installers_bytes)
        z.writestr(f"{idir}/{agent_zip_name}", agent_zip_bytes)

    print(f"Built {out}  ({os.path.getsize(out):,} bytes)")
    with zipfile.ZipFile(out) as z:
        for e in z.infolist():
            print(f"  {e.file_size:6d}  {e.filename}")


if __name__ == "__main__":
    main()
