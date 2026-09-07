#!/usr/bin/env python3
"""Record resolved Maven coordinates, artifact hashes and published license declarations.

Run :app:dependencyInventory first. --write refreshes the review snapshot; --check
only compares it, so CI cannot silently approve a new dependency. Uses no network.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "docs/audit/dependencies.json"
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"


def pom_url(coordinate):
    group, artifact, version = coordinate.split(":")
    host = "https://dl.google.com/dl/android/maven2/" if group.startswith(("androidx.", "com.android.")) else "https://repo.maven.apache.org/maven2/"
    return f"{host}{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"


def read_pom(coordinate, visited=None):
    visited = set() if visited is None else visited
    if coordinate in visited:
        raise ValueError(f"Cyclic POM parent: {coordinate}")
    visited.add(coordinate)
    group, artifact, version = coordinate.split(":")
    files = sorted((CACHE / group / artifact / version).glob("*/*.pom"))
    if not files:
        raise ValueError(f"POM unavailable: {coordinate}; resolve dependencies first")
    content = files[0].read_bytes()
    root = ET.fromstring(content)
    for node in root.iter():
        node.tag = node.tag.split("}")[-1]
    licenses = [{"name": node.findtext("name"), "url": node.findtext("url")} for node in root.findall("licenses/license")]
    source = {"coordinate": coordinate, "url": pom_url(coordinate), "sha256": hashlib.sha256(content).hexdigest()}
    if not licenses:
        parent = root.find("parent")
        if parent is None:
            raise ValueError(f"No license declaration: {coordinate}; manual review required")
        inherited = read_pom(":".join(parent.findtext(key) for key in ("groupId", "artifactId", "version")), visited)
        licenses, source = inherited["licenses"], inherited["license_declaration"]
    return {"licenses": licenses, "license_declaration": source, "pom": pom_url(coordinate), "scm": root.findtext("scm/url")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--check", action="store_true")
    args = parser.parse_args()
    resolution = json.loads((ROOT / "app/build/reports/distribution/dependencies.json").read_text())
    if args.check:
        previous = json.loads(SNAPSHOT.read_text())
        if previous["resolution"] != resolution:
            raise SystemExit("Dependency versions/artifacts changed. Review licenses, then run --write and update notices.")
        print("PASS: resolved dependencies match the reviewed inventory")
        return
    modules = sorted({module for scope in resolution.values() for module in scope["modules"]})
    inventory = []
    for coordinate in modules:
        entry = {"coordinate": coordinate, **read_pom(coordinate)}
        entry["scopes"] = [name for name, scope in resolution.items() if coordinate in scope["modules"]]
        entry["packaged_runtime"] = any(artifact["module"] == coordinate for artifact in resolution["fdroidReleaseRuntimeClasspath"]["artifacts"])
        # POMs are declarations, not proof about every embedded source file.
        inventory.append(entry)
    document = {"schema": 1, "method": "Resolved Gradle graphs and Maven POM declarations (including parent POMs). Embedded source exceptions are reviewed separately in THIRD_PARTY_LICENSES.md.",
                "resolution": resolution, "modules": inventory}
    SNAPSHOT.parent.mkdir(parents=True, exist_ok=True)
    SNAPSHOT.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n")
    print(f"Recorded {len(inventory)} dependencies for review: {SNAPSHOT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
