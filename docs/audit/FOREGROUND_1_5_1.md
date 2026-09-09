# 1.5.1 recording lifecycle validation

Date: 2026-09-09. Scope: 1.5.1 / versionCode 15. [Current recording contract](../RECORDING.md) · [Evidence index](../VALIDATION.md).

## Behavior

Leaving the capture activity stops recording and finalizes the recorded file. There is no background recording surface or service. The manifest no longer requests `FOREGROUND_SERVICE`, its CAMERA/MICROPHONE/DATA_SYNC/MEDIA_PROCESSING types, `WAKE_LOCK` or `POST_NOTIFICATIONS`. Camera and optional microphone/location permissions remain.

The hidden-state flag prevents new render frames and new recording starts. GL cleanup stops the recorder before releasing resources. RAW stops accepting frames and finalizes its bounded pending queue. Photo processing waits while hidden. Thumbnail requests are cancelled on pause and deferred until return. Saved media and preference identities are unchanged.

## Device evidence

Physical Android device: 25060RK16C; DEV application only. The release application and its data were preserved.

- TAP image boundary/orientation, JPEG save, selected video import and independent source playback/output recording passed.
- Home during TAP MP4, camera MP4 and audio-enabled camera MP4 stopped recording and published a decodable file. Audio-enabled output retained an audio track.
- After cleanup, camera, TAP source and analysis microphone were released; rendered-frame count remained unchanged while hidden.
- Return restored preview without restarting recording; TAP returned with its source paused.
- RAW Home-exit test saved a ZIP containing a DNG frame, matching metadata, timestamps and manifest, then validated resumed RAW capture.
- Installed package had none of the removed foreground-service, wake-lock or notification permissions.

The assertions wait for resource cleanup as well as file publication because those complete on different threads.

## Build and package evidence

- Android 17 emulator: screen-lock exit passed the same TAP/camera/audio MP4 stop/save, decode, resource-release and return checks. This supplements the physical Home-exit test; it does not establish hardware RAW support on the emulator.
- JVM tests: 33 per variant, four variants, no failures; existing image-processing golden hashes unchanged.
- Lint (F-Droid and Play), F-Droid debug/release and Play debug builds passed. Locale, documentation-link, release-metadata and dependency checks passed.
- Both packaged F-Droid release and Play debug manifests contain only camera, microphone, optional location and the AndroidX app-private receiver permission. None contains FGS, wake-lock or notification permissions.

## Limits

The tests verify termination, file validity and resource release. They do not quantify battery savings, prove compatibility across all devices, or guarantee a file after encoder failure, insufficient storage or forced process termination. Finalizing accepted RAW frames can continue briefly after the screen closes; it does not capture additional frames.

## Publication

GitHub release [v1.5.1](https://github.com/Bongorian/5igna1-release/releases/tag/v1.5.1) points to `51f1b8d823c9cf87d26fcbc2608cfcc8caaab6fe`. APK SHA-256: `7ae9e3876327e02c944f95d3c395e26afb1f71841b5ee4db967194eaebbc5e9d`. The public APK and checksum were downloaded anonymously and matched the verified local artifacts. The existing distribution certificate and all prior releases were preserved.

The duplicate tag-triggered APK preparation run was cancelled after the verified local APK was published; it did not replace release assets. The GitHub Pages update completed in [run 34356292045](https://github.com/Bongorian/5igna1-release/actions/runs/34356292045), with app recording/privacy descriptions aligned to 1.5.1 and the existing tester form retained.

[Play upload 34356251537](https://github.com/Bongorian/5igna1-release/actions/runs/34356251537) completed successfully and uploaded 1.5.1 / code 15 as an Alpha draft. AAB SHA-256: `bd937cd0bbc55ea68db3e37ae5c5277e85634f2c6856329605a7b5991df99d5b`. It did not submit for review or publish to testers.
