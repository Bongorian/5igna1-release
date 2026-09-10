# Development and device verification

[日本語・詳細手順](DEVELOPMENT.ja.md) · [Build setup](building.md) · [Evidence and limits](VALIDATION.md)

Use the debug application `com.bongorian.signa1.debug`; it coexists with the release app. Build requirements and outputs belong in [building.md](building.md). Version values come from `app/build.gradle`; changed release code needs a new versionCode before publication.

```sh
./tools/build.sh test assembleFdroidDebug assembleFdroidDebugAndroidTest lintFdroidDebug
adb devices -l
adb -s DEVICE install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
adb -s DEVICE install -r app/build/outputs/apk/androidTest/fdroid/debug/app-fdroid-debug-androidTest.apk
adb -s DEVICE shell am instrument -w -e action normal-capture \
  com.bongorian.signa1.debug.test/com.bongorian.signa1.DeviceChecks
```

Replace DEVICE with the intended emulator or device. Camera tests save actual media; choose the scene intentionally. Check for `result=PASS`, no `failure`, and `INSTRUMENTATION_CODE: -1`.

| Action | Purpose |
|---|---|
| `camera-lenses` | Enumerated lens selection, physical result identity, JPEG/MP4 saves and recording switch guard |
| `normal-capture` | Single-output capture, released history, immutable JPEG state and saved mode flags |
| `capture-contract` | ADVANCED ON: pinned displayed JPEG pixels and timestamp/state metadata despite later camera frames |
| `settings-auto` | Immediate settings, rapid changes, guide replay and recreation |
| `state`, `editor`, `effects` | Transactions, lifecycle and base fault GPU behavior |
| `experimental` | Additional stages, sensitivity, bypass and strict capture |
| `tutorial`, `tutorial-permission` | Interactive guide, locale/layout, camera suspension and permission order |
| `video` | Short actual MP4, using `-e resolutionAudio true` to exercise linked audio |
| `raw`, `raw-video` | Device-dependent DNG/sequence integration |
| `raw-echo` | Full-chain RAW transitions, mode-tab resolution shortcuts, experimental replay, historical JPEG/MP4 capture and RAW bypass |

Video parameters include `videoKey` (an advertised `WIDTHxHEIGHT@FPS`), `seconds`, `sound`, `bitrate`, `codec`, `resolutionAudio` and `advanced`. Test both audio OFF and linked/ordinary audio on supported sizes. `tools/check-media.py` checks decoded media and timestamps; use ffprobe to inspect audio sample rate. `tools/check-dng.py` validates DNG through LibRaw. Full procedure and Wi-Fi ADB setup are in the Japanese edition.

`verification/` and `dist/` are ignored local working directories. Keep SDK paths, keys, passwords, device addresses and private captures out of Git. Publish only scoped summaries of results in `docs/audit/`; [VALIDATION.md](VALIDATION.md) is the current evidence index.

## 1.5.0 checks

- `action seed`: SEED field refresh, exact integer input, Apply/Cancel, EVENT SEED and recording guard.
- `action transport`: model UI, settings order, pixel identity, intermediate resolution, network hold/resume and LED gaps.
- `action tap`: image/video fixtures, injection boundary, playback/recording and stop/save on Home (add `-e leave lock` on an emulator without a secure lock to check screen locking). Requires `files/tap-fixture.mp4` in DEV app storage.

## LIGHT / GPU — unreleased

- `action light-mode`: immediate exclusive mode switches, view-sized preview, unchanged full-resolution JPEG/RAW/video output, live-camera byte comparison, resized view, NETWORK retention and ADVANCED history. Its own saved fixtures are removed.
- `action gpu-detail`: bounded CFA/DEMOSAIC shader candidates, output comparisons, final-display transfer and view-cap timing. Test-only alternatives are not shipping shaders.

## Camera intent checks — unreleased

`action camera-intents` uses a separate-UID caller fixture to verify result delivery. See [camera integration](CAMERA_INTEGRATION.md#verification) for the opt-in build and test commands.
