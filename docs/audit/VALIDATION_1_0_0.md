# Tested scope and current limits

Historical audit: results and branch/release status describe the named source and test date. For current behavior, see [architecture](../ARCHITECTURE.md) and [validation](../VALIDATION.md).

This is a record of what was checked for 5igna1 1.0.0. It is not a guarantee of every device's camera capabilities or sustained frame rate.

## Build and behavior checks

- JDK 17 on macOS: clean build, lint for both flavors, fdroidDebug, unsigned fdroidRelease, and playDebug passed.
- JVM checks: 3 behavior fixtures × 4 build variants = 12 tests passed. Coverage includes state snapshots, edit/cancel behavior, RAW row/byte/CFA transformations, and LIVE FAULT selection/recovery.
- The initial public source passed CI on Ubuntu and macOS. [Verified run](https://github.com/Bongorian/5igna1-release/actions/runs/34147880216) · [Current runs](https://github.com/Bongorian/5igna1-release/actions/workflows/android.yml)
- Dependency inventory, prohibited-SDK check, translation consistency, release metadata, and wrapper integrity checks passed.
- The distribution APK's signing certificate, package ID, version, release mode, and SHA-256 were verified. The release key is separate from the Play upload key.

## Camera and UI checks

Android Emulator API 37 was used to check startup, capture, chain/adjustment screens, app languages, and the offline license screen. Current screenshots were captured from that UI with an AOSP test pattern.

REDMI K80 Ultra / Android 16 was used during development for language switching, camera restoration, state preservation, JPEG saving, GPU/state behavior, and limited RAW tests. Results on that device do not establish support on every manufacturer or camera.

## RAW and recording limits

- RAW preview approximates processed RAW behavior on RGB. It is not a live RAW developer, and a developed DNG can differ from the preview.
- Additional RAW behavior fixtures covered byte/word displacement, Bayer row preservation, CFA phase permutation, bounds, and zero-strength behavior. A short processed-DNG capture was decoded with LibRaw during development.
- RAW video on the K80 back camera was tested briefly at a 2 fps target, saving two DNG frames and checking ZIP contents, dimensions, timestamps, and manifest. Sustained recording at the camera's nominal 12 fps ceiling remains untested.
- An emulator with incomplete DNG metadata correctly rejected RAW video and returned to normal video.
- Multi-hour recording, sustained high-resolution/many-stage performance, and every device's RAW pipeline remain unverified. A low frame rate from an emulator's software renderer is not a hardware performance benchmark.
- Android 12 is the minimum supported version, but capture and language behavior have not been verified on a physical Android 12 device.

For a device report, include its model, Android version, camera, capture mode, resolution/fps, and effect settings. [Troubleshooting](../TROUBLESHOOTING.md) · [Contributing](../../CONTRIBUTING.md)
