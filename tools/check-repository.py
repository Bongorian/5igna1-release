#!/usr/bin/env python3
"""Small release guards for credentials, wrapper integrity and shared core sources."""
import hashlib
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_WRAPPER = "2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
private_names = {"signing.properties", "fdroid-signing.properties", "keystore.properties", "key.properties", ".env"}
for name in filter(None, tracked):
    path = Path(name)
    if path.name in private_names or path.suffix.lower() in {".jks", ".keystore", ".p12", ".pfx", ".pem"} or ".signing" in path.parts:
        raise SystemExit(f"Credential/signing file is tracked: {name}")
    content = (ROOT / path).read_bytes()
    if re.search(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----", content):
        raise SystemExit(f"Private key material in tracked file: {name}")
    if path.suffix in {".aar", ".so", ".dex", ".apk", ".aab"} or (path.suffix == ".jar" and name != "gradle/wrapper/gradle-wrapper.jar"):
        raise SystemExit(f"Unreviewed binary dependency/output: {name}")
actual = hashlib.sha256((ROOT / "gradle/wrapper/gradle-wrapper.jar").read_bytes()).hexdigest()
if actual != EXPECTED_WRAPPER:
    raise SystemExit("Gradle wrapper differs from the official 8.11.1 checksum")
for flavor in ("play", "fdroid"):
    for name in ("Effects.java", "RawGlitch.java", "EffectChain.java", "EffectState.java", "PhotoRenderer.java", "effect.glsl"):
        if list((ROOT / "app/src" / flavor).rglob(name)):
            raise SystemExit(f"Core processing must stay shared: {flavor}/{name}")
for name in ("LICENSE", "NOTICE", "THIRD_PARTY_LICENSES.md", "CONTRIBUTING.md", "AGENTS.md"):
    if not (ROOT / name).is_file():
        raise SystemExit(f"Missing publication document: {name}")
print("PASS: no tracked credentials/blobs, official wrapper, shared processing sources and publication documents")
