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
