# Device recommendations, normal capture and linked audio

Development branch `codex/adaptive-capture-audio`, based on `b5b5768` after published 1.3.1; checked on 2026-09-09. These changes are unreleased. [Current behavior](../FORMATS.md) · [Device budgets](../PERFORMANCE.md) · [Validation index](../VALIDATION.md)

## Review and implementation

The previous device profile classified only low-RAM/total-memory information, offering roughly 1/2 MP JPEG and HD/FHD video. Current starting tiers add available CPU core count, conservative handling of unknown RAM, and VGA/HD/FHD video budgets. Camera/encoder modes and timing still constrain the actual selection. Core count is not a CPU/GPU benchmark. Existing manual sizes and saved switches remain unchanged; a saved Recommended choice resolves using the new policy.

ADVANCED OFF reuses one processed output texture instead of retaining three displayed images. It still runs the same FAULT chain at the chosen dimensions. JPEG readback and immutable state are captured together on the GL thread before file work; the exact last displayed camera sample is not required. ADVANCED ON retains the existing synchronous displayed-frame lease and bounded three-slot history. Reconfiguration releases old output textures. Normal presentation acknowledgement rejects tokens from before the current camera generation. RAW photos continue to use separate exposures and immutable latched state; original RAW sequences remain untouched.

The independent resolution-linked audio switch defaults OFF and does not turn on recording audio or LIVE microphone input. Ordinary mono AAC uses requested tiers from the actual video output area, and queries platform sample-rate/bitrate support. A failed capability query falls back to the existing 48 kHz / 192 kbps request. The framework can further adjust encoding parameters. [Android audio capabilities](https://developer.android.com/reference/android/media/MediaCodecInfo.AudioCapabilities) and [MediaRecorder settings](https://developer.android.com/reference/android/media/MediaRecorder) define these platform constraints.

## Build and deterministic checks

JDK 17 / macOS: 25 JVM tests per variant, 100 executions across four variants, zero failures/errors. Existing Java migration golden hashes and independent RAW differential references remain unchanged. Added cases cover tier boundaries, unknown RAM, CPU constraints, portrait/landscape audio equivalence, OFF behavior and independent settings copies.

F-Droid debug, unsigned F-Droid release, Play debug and instrumentation APKs build. No dependency or shader/RAW arithmetic changes were introduced. Locale, repository, release-metadata and local documentation checks are part of this pass. Integration checks are recorded below.

## Encoded audio and video

Pixel 9 Android Emulator / API 37, SwiftShader, debug application only. The camera uses an owner-supplied photograph as its fixture. Audio checks use the emulator’s input, not a measured physical microphone response. Each clip is approximately three seconds.

| Actual output | Linked audio | Encoded AAC sample rate | Encoded audio bitrate |
|---|---|---:|---:|
| 240×320 (QVGA) | ON | 8,000 Hz | 24,000 bit/s |
| 480×640 (VGA) | ON | 16,000 Hz | 48,000 bit/s |
| 720×1280 (HD) | ON | 32,000 Hz | 64,002 bit/s |
| 960×1280 (above HD, within Full HD tier) | ON | 48,000 Hz | 96,000 bit/s |
| 240×320 (QVGA) | OFF | 48,000 Hz | 96,480 bit/s |

The higher tier requested 128 kbps, and OFF requested the existing 192 kbps; this emulator encoded near 96 kbps mono. Requested bitrate is therefore not an exact output guarantee. All five clips pass decoded-frame, monotonic timestamp, media-origin and audio/video-duration checks. Encoded video rates ranged from 17.71 to 29.64 fps on the software emulator; these are functional checks, not 30 fps performance claims.

An initial audio test lacked RECORD_AUDIO permission and failed before recorder configuration; granting the test app’s microphone permission allowed recording. The emulator did not offer the requested 1920×1080 mode, so the within-Full-HD audio tier was checked at its available 1280×960 mode. Exact Full HD and 4K camera recording remain device checks; the tier policy itself is unit-tested at those boundaries.

## Limits

The calculated reduction of two full-size RGBA8 history textures is about 15.8 MiB at 1920×1080, excluding driver/camera buffers and other allocations. It is not a measured whole-process peak-memory, battery or thermal improvement. FAULT per-frame work is unchanged. No physical phone was connected for this pass; listening quality, device-specific AAC adjustments, hardware RAW integration under normal mode and sustained heat/recording need separate physical-device validation.

Original 1.3.1 artifacts/tags, signing identities, Play drafts and F-Droid candidate metadata are preserved. The application version remains the development baseline; a future release requires a higher version/code.

## Final integration checks

On the final debug build, `normal-capture`, `settings-auto`, `capture-contract` and `editor` all pass. Normal capture deliberately delays file saving, advances the camera to a different FAULT, and confirms the saved snapshot retains the original BIT ERROR state; it also checks that history textures are released. The strict test retains its exact JPEG pixel and timestamp comparison. Settings checks cover the new audio toggle’s immediate persistence, its independence from recording audio, rapid changes, recreation and reopening. Effect draft apply/cancel and unobscured preview remain valid. The English Settings screenshot was visually inspected.

The final build completed F-Droid debug/unsigned release, Play debug, instrumentation packaging, all 100 JVM executions and F-Droid debug lint with no errors. All 437 translated strings and placeholders match. The documentation check covers 478 local links and requires a role for every docs file; release metadata, repository guards and whitespace checks also pass. No store or website deployment was performed in this development pass.

## Release follow-up — 2026-09-09

Published as 1.4.0 / code 13 from `6a2c27871d776201996c418724037558c514b21d`, with unchanged application behavior from the verified development update. The release build passed lint, all 100 JVM executions, both debug flavors, unsigned F-Droid release, dependency and metadata checks. The signed FOSS APK was verified against the existing distribution certificate and downloaded anonymously after publication; SHA-256 is `76e9c904f8cde0eb77d04bdd9cd03e2f6ffda68767e9bdcf3190a04830bc1d31`. The release is marked latest for Obtainium. The connected phone’s DEV app was updated to 1.4.0-debug and launched successfully; the release app was not replaced. Hosted privacy pages were aligned with the app’s CPU-core-count disclosure.
