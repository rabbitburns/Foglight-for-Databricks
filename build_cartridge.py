"""
Build DatabricksAgent-<version>.car Foglight cartridge.
Usage: python build_cartridge.py [version]
Output: target/DatabricksAgent-<version>.car
"""
import sys
import zipfile
import os

VERSION = sys.argv[1] if len(sys.argv) > 1 else "1.0.18"
VER_FLAT = VERSION.replace(".", "_")

CDT_BINDING = f"""\
<?xml version="1.0" encoding="UTF-8"?>
<adapter type="FglAM">
    <cdt-bindings>
        <cdt-config type="DatabricksAgent" file="databricks-model-root-cdt.xml"/>
    </cdt-bindings>
</adapter>
"""


def make_manifest(topo_size, binding_size, model_root_size, wcf_files):
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

    with open("assembly/topology/cdt.xml", "rb") as f:
        model_root_bytes = f.read()

    with open("assembly/topology/topology-types.xml", "rb") as f:
        topo_bytes = f.read()

    binding_bytes = CDT_BINDING.encode("utf-8")

    wcf_entries, wcf_contents = collect_wcf_files("assembly/wcf")

    manifest = make_manifest(
        len(topo_bytes),
        len(binding_bytes),
        len(model_root_bytes),
        wcf_entries,
    )

    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.xml", manifest)
        z.writestr(f"{tdir}/topology-types.xml", topo_bytes)
        z.writestr(f"{cdir}/cdt-binding.xml", binding_bytes)
        z.writestr(f"{cdir}/databricks-model-root-cdt.xml", model_root_bytes)
        for rel_path, data in wcf_contents:
            z.writestr(f"{wdir}/{rel_path}", data)

    print(f"Built {out}  ({os.path.getsize(out):,} bytes)")
    with zipfile.ZipFile(out) as z:
        for e in z.infolist():
            print(f"  {e.file_size:6d}  {e.filename}")


if __name__ == "__main__":
    main()
