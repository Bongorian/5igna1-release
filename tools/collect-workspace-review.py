#!/usr/bin/env python3
"""Collect full-resolution screenshots produced by workspace-review on the review emulator."""
import argparse
import datetime
import hashlib
import json
import os
import re
from pathlib import Path
import struct
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--output', type=Path, default=Path('docs/ui-review'))
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('This collector is for emulator review captures.')
sdk = Path(os.environ.get('ANDROID_HOME', Path.home() / 'Library/Android/sdk'))
adb = [str(sdk / 'platform-tools/adb'), '-s', args.serial]
density = subprocess.check_output(adb + ['shell', 'wm', 'density'], text=True)
density_values = re.findall(r'(?:Physical|Override) density: (\d+)', density)
if not density_values or int(density_values[-1]) != 480:
    raise SystemExit(f'Expected effective 480 dpi, got {density.strip()}')
api = int(subprocess.check_output(adb + ['shell', 'getprop', 'ro.build.version.sdk'], text=True).strip())
shots = ['01-portrait-auto', '02-portrait-fault', '03-portrait-pro', '04-pro-exposure',
         '05-landscape', '06-landscape-collapsed', '07-landscape-video',
         '08-landscape-recording-collapsed', '09-settings']
metadata = {
    'capturedAtUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'avd': 'Signal_Review_1280_480', 'api': api, 'width': 1280, 'height': 2772,
    'densityDpi': 480, 'navigation': 'three-button',
    'source': 'Android emulator emulated camera with app ROW ERROR; unedited screenshots',
    'files': [],
}
source = hashlib.sha256()
for path in sorted(Path('app/src/main').rglob('*')):
    if path.is_file():
        source.update(path.as_posix().encode() + b'\0' + path.read_bytes())
metadata['appSourceSha256'] = source.hexdigest()
for language in ['ja', 'en']:
    target = args.output / language
    target.mkdir(parents=True, exist_ok=True)
    for shot in shots:
        data = subprocess.check_output(adb + ['exec-out', 'run-as', 'com.bongorian.signa1.debug',
            'cat', f'files/verification/language-workspace-{language}-{shot}.png'])
        if data[:8] != b'\x89PNG\r\n\x1a\n':
            raise SystemExit(f'Not a PNG: {language}/{shot}')
        width, height = struct.unpack('>II', data[16:24])
        expected = (2772, 1280) if 'landscape' in shot else (1280, 2772)
        if (width, height) != expected:
            raise SystemExit(f'Wrong dimensions for {language}/{shot}: {width}×{height}')
        path = target / f'{shot}.png'
        path.write_bytes(data)
        metadata['files'].append({'path': str(path.relative_to(args.output)), 'width': width,
            'height': height, 'sha256': hashlib.sha256(data).hexdigest()})
(args.output / 'manifest.json').write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + '\n')
print(f'Collected {len(metadata["files"])} original screenshots in {args.output}')
