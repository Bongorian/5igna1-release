#!/usr/bin/env python3
"""CI-only publication of verified artifacts to a draft, never over a published release."""
import json
import os
from pathlib import Path
import subprocess

from release import ROOT, check

info = check(os.environ["RELEASE_TAG"])
directory = ROOT / "dist/github-release"
artifacts = [directory / info["apkName"], directory / (info["apkName"] + ".sha256")]
if not all(path.is_file() for path in artifacts):
    raise SystemExit("Verified APK/checksum missing")
view = subprocess.run(["gh", "release", "view", info["tag"], "--json", "isDraft"], capture_output=True, text=True)
if view.returncode == 0:
    if not json.loads(view.stdout)["isDraft"]:
        raise SystemExit("Published releases are immutable; create a new version instead")
    subprocess.run(["gh", "release", "upload", info["tag"], *map(str, artifacts), "--clobber"], check=True)
else:
    # gh verifies the existing remote tag. A network/auth error still fails at create.
    notes = directory / "release-notes.md"
    changes = (ROOT / "fastlane/metadata/android/en-US/changelogs" / f"{info['versionCode']}.txt").read_text().strip()
    notes.write_text(changes + "\n\nAndroid 12 or later. This APK is the FOSS fdroid flavor.\n"
                     "Verify the .apk.sha256 file before installing. Updates require the same signing certificate.\n"
                     "Publish this draft after checking the APK and store metadata; Obtainium ignores drafts.\n")
    subprocess.run(["gh", "release", "create", info["tag"], "--verify-tag", "--draft", "--title", f"5igna1 {info['versionName']}",
                    "--notes-file", str(notes), *map(str, artifacts)], check=True)
