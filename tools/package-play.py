#!/usr/bin/env python3
"""Validate Play metadata and package an existing signed playRelease AAB. Requires Pillow."""
from pathlib import Path
from PIL import Image
import hashlib
import shutil
import subprocess
import zipfile

from release import ROOT, check

info = check()
metadata = ROOT / 'fastlane/metadata/android'
for locale in ('ja-JP', 'en-US'):
    folder = metadata / locale
    icon = folder / 'images/icon.png'
    with Image.open(icon) as im:
        assert im.size == (512, 512) and im.mode == 'RGBA'
    assert icon.stat().st_size <= 1024 * 1024
    with Image.open(folder / 'images/featureGraphic.png') as im:
        assert im.size == (1024, 500) and im.mode == 'RGB'
    shots = sorted((folder / 'images/phoneScreenshots').glob('*.png'))
    assert len(shots) >= 4
    for shot in shots:
        with Image.open(shot) as im:
            assert im.size == (1080, 1920), shot

aab = ROOT / 'app/build/outputs/bundle/playRelease/app-play-release.aab'
with zipfile.ZipFile(aab) as archive:
    assert any(name.endswith(('.RSA', '.DSA', '.EC')) for name in archive.namelist()), 'AAB is unsigned'
    assert not any(name.endswith('.so') for name in archive.namelist())
# Validate the JAR signature rather than relying on a filename inside the archive.
subprocess.run(['jarsigner', '-verify', str(aab)], check=True)
out = ROOT / 'dist' / f"5igna1-v{info['versionName']}-google-play"
out.mkdir(parents=True, exist_ok=True)
shutil.copy2(aab, out / f"5igna1-v{info['versionName']}.aab")
# Only explicit public documentation/assets are copied. No key or signing properties.
for name in ('README.md', 'console-declarations.md', 'asset-provenance.md'):
    shutil.copy2(ROOT / 'store/google-play' / name, out / name)
shutil.copytree(ROOT / 'store/google-play/privacy', out / 'privacy', dirs_exist_ok=True)
shutil.copytree(metadata, out / 'metadata/android', dirs_exist_ok=True)
files = sorted(path for path in out.rglob('*') if path.is_file() and path.name != 'SHA256SUMS.txt')
assert not any(path.suffix in ('.p12', '.pfx', '.jks', '.keystore', '.properties', '.pem') for path in files)
(out / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + str(path.relative_to(out)) + '\n' for path in files))
archive = shutil.make_archive(str(out), 'zip', out.parent, out.name)
print('Validated and packaged:', archive)
