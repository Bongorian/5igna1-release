#!/usr/bin/env python3
"""Create/reuse the dedicated UI review AVD without replacing an existing device's data."""
import os
from pathlib import Path
import shlex
import subprocess

sdk = Path(os.environ.get('ANDROID_HOME', Path.home() / 'Library/Android/sdk'))
name = 'Signal_Review_1280_480'
package = 'system-images;android-35;google_apis;arm64-v8a'
image = sdk / 'system-images/android-35/google_apis/arm64-v8a'
if not image.is_dir():
    raise SystemExit(f'Install {package} with sdkmanager first.')
avd_root = Path(os.environ.get('ANDROID_AVD_HOME', Path.home() / '.android/avd'))
config = avd_root / f'{name}.avd/config.ini'
if not config.exists():
    subprocess.run([str(sdk / 'cmdline-tools/latest/bin/avdmanager'), 'create', 'avd',
                    '-n', name, '-k', package, '-d', 'pixel_7'], input='no\n', text=True, check=True)
values = dict(line.split('=', 1) for line in config.read_text().splitlines() if '=' in line)
values.update({
    'hw.lcd.width': '1280', 'hw.lcd.height': '2772', 'hw.lcd.density': '480',
    'hw.camera.back': 'emulated', 'hw.camera.front': 'emulated', 'hw.ramSize': '3072',
    'hw.gpu.enabled': 'yes', 'hw.gpu.mode': 'auto', 'showDeviceFrame': 'no',
})
config.write_text('\n'.join(f'{key}={value}' for key, value in values.items()) + '\n')
print(f'Review AVD ready: {name} (1280×2772, 480 dpi, API 35, ARM64)')
print(shlex.join([str(sdk / 'emulator/emulator'), '-avd', name, '-no-window', '-no-audio',
                  '-no-snapshot', '-gpu', 'swiftshader_indirect']))
