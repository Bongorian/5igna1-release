#!/usr/bin/env python3
"""Validate release metadata and package only a verified, signed FOSS APK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
APPLICATION_ID = "com.bongorian.signa1"


def version_info():
    gradle = (ROOT / "app/build.gradle").read_text()
    version = re.search(r"^\s*versionName\s+'([^']+)'", gradle, re.M).group(1)
    code = int(re.search(r"^\s*versionCode\s+(\d+)", gradle, re.M).group(1))
    if not re.fullmatch(r"(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)", version) or code < 1:
        raise ValueError("Use a stable X.Y.Z versionName and a positive versionCode")
    return {"versionName": version, "versionCode": code, "tag": "v" + version,
            "apkName": f"5igna1-v{version}.apk", "applicationId": APPLICATION_ID}


def check(tag=None):
    info = version_info()
    if tag is not None and tag != info["tag"]:
        raise ValueError(f"Tag must match versionName: expected {info['tag']}")
    changelog = (ROOT / "CHANGELOG.md").read_text()
    if f"## [{info['versionName']}]" not in changelog:
        raise ValueError("Missing release section in CHANGELOG.md")
    for locale in ("ja-JP", "en-US"):
        directory = ROOT / "fastlane/metadata/android" / locale
        for filename, limit in (("title.txt", 30), ("short_description.txt", 80), ("full_description.txt", 4000)):
            text = (directory / filename).read_text().strip()
            if not text or len(text) > limit:
                raise ValueError(f"Invalid metadata length: {locale}/{filename}")
        changes = (directory / "changelogs" / f"{info['versionCode']}.txt").read_text().strip()
        if not changes or len(changes) > 500:
            raise ValueError(f"Invalid release notes: {locale}")
    # All existing release tags must have lower codes. Re-running the current tag is allowed.
    tags = subprocess.check_output(["git", "tag", "--list", "v*"], cwd=ROOT, text=True).splitlines()
    for old in tags:
        if old == info["tag"] or not re.fullmatch(r"v\d+\.\d+\.\d+", old):
            continue
        old_gradle = subprocess.check_output(["git", "show", f"{old}:app/build.gradle"], cwd=ROOT, text=True)
        match = re.search(r"^\s*versionCode\s+(\d+)", old_gradle, re.M)
        if match is None or int(match.group(1)) >= info["versionCode"]:
            raise ValueError(f"versionCode must increase beyond release {old}")
    return info


def sdk_tool(name):
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        local = ROOT / "local.properties"
        if local.exists():
            match = re.search(r"^sdk.dir=(.+)$", local.read_text(), re.M)
            if match:
                sdk = match.group(1)
    if not sdk and (Path.home() / "Library/Android/sdk").is_dir():
        sdk = str(Path.home() / "Library/Android/sdk")
    candidate = Path(sdk or "") / "build-tools/35.0.0" / name
    if not candidate.is_file():
        raise ValueError(f"Install Android build-tools 35.0.0 and set ANDROID_HOME ({name})")
    return str(candidate)


def package(apk, certificate, output):
    info = check()
    fingerprint = certificate.replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", fingerprint):
        raise ValueError("A confirmed release signing certificate SHA-256 is required")
    verification = subprocess.check_output([sdk_tool("apksigner"), "verify", "--verbose", "--print-certs", str(apk)], text=True)
    actual = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)", verification)
    if actual is None or actual.group(1).lower() != fingerprint or "CN=Android Debug" in verification:
        raise ValueError("APK signing certificate is unexpected or a debug certificate")
    badging = subprocess.check_output([sdk_tool("aapt"), "dump", "badging", str(apk)], text=True)
    expected = f"package: name='{APPLICATION_ID}' versionCode='{info['versionCode']}' versionName='{info['versionName']}'"
    if not badging.startswith(expected) or "application-debuggable" in badging:
        raise ValueError("APK identity/version does not match, or the APK is debuggable")
    metadata = json.loads((apk.parent / "output-metadata.json").read_text())
    if metadata.get("variantName") != "fdroidRelease" or metadata.get("applicationId") != APPLICATION_ID:
        raise ValueError("GitHub Releases must use the fdroidRelease output")
    if apk.name not in [entry["outputFile"] for entry in metadata["elements"]]:
        raise ValueError("APK is not the file declared by the Gradle output metadata")
    output.mkdir(parents=True, exist_ok=True)
    target = output / info["apkName"]
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() != digest:
        raise ValueError("Refusing to overwrite a different artifact with the same release name")
    shutil.copy2(apk, target)
    target.with_name(target.name + ".sha256").write_text(f"{digest}  {target.name}\n")
    print(f"Packaged {target.name} and {target.name}.sha256; signing certificate verified")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("info")
    validation = sub.add_parser("check")
    validation.add_argument("--tag")
    packaging = sub.add_parser("package")
    packaging.add_argument("--apk", type=Path, default=ROOT / "app/build/outputs/apk/fdroid/release/app-fdroid-release.apk")
    packaging.add_argument("--certificate-sha256", required=True)
    packaging.add_argument("--output", type=Path, default=ROOT / "dist/github-release")
    args = parser.parse_args()
    if args.command == "info":
        print(json.dumps(version_info()))
    elif args.command == "check":
        info = check(args.tag)
        print(f"PASS: {info['tag']} / versionCode {info['versionCode']} / metadata")
    else:
        package(args.apk, args.certificate_sha256, args.output)


if __name__ == "__main__":
    main()
