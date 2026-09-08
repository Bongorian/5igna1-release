# FAULT redesign validation

Unreleased work based on public release source `7dd83a7`, checked on 2026-09-08. [Japanese summary](VALIDATION.ja.md) · [Design and migration](design/FAULT_SYSTEM.md). The [1.0.0 validation record](audit/VALIDATION_1_0_0.md) is historical and does not establish physical-device coverage for this redesign.

## Build and independent behavior checks

JDK 17 / macOS, with the repository Android SDK configuration:

- `test`: four behavior fixtures in four variants, 16 tests, zero failures/errors.
- `assembleFdroidDebug`, unsigned `assembleFdroidRelease`, `assemblePlayDebug`, `assembleFdroidDebugAndroidTest`: passed.
- `lint` and `lintPlayDebug`: zero fatal/error findings; 43 warnings remain (including existing UI/platform recommendations).
- `tools/check-locales.py`, `tools/check-repository.py`, `git diff --check`: passed.

Fixtures cover named controls and schema reset, 64-bit identity roundtrip/isolation, pure snapshot reads, continuous drift, incident onset/recovery, stable routes, device coupling, immutable profile/fault maps, RAW byte/bit/CFA/exposure samples, Bayer-preserving readout, bounded RAW samples, and causal tap prefixes. A scheduling fixture verifies that UI acknowledgement and a synchronous shutter lease protect an old displayed frame while later camera frames arrive, with bounded backpressure and lifecycle invalidation.

## Android Emulator checks

Pixel 9 AVD / API 37.1; camera test pattern, not a physical photographic subject:

- All 13 shaders: visible output, exact replay of a fixed snapshot, every named control, orientation, LEVEL-zero bypass and causal chain composition.
- Displayed-signal capture: reserve a displayed image, allow later camera frames, capture its timestamp, change the selected fault, then compare saved JPEG pixels against that reserved image compressed with the same quality. Pixels and timestamp/state EXIF matched.
- Short silent VHS MP4: saved at 720 × 1280. The post-specialization run reported about 28 processed frames/s, versus about 8 before specialization. This is a short emulator observation, not a hardware benchmark or sustained-performance claim.
- Commit/cancel/stale-editor behavior, stable LIVE route, microphone foreground cleanup, low-resolution recovery, photo startup without a video encoder, front/back camera switching, and Japanese/English/Chinese/system locale recreation were checked. Capture settings, fault settings and session LIVE state survived language changes.

## Limits and remaining hardware validation

JPEG now saves the actual selected live camera signal (standard up to 2 MP; offered maximum bounded to 8 MP), not a later full-resolution still exposure. Display and encoder use the same canonical images; a high-fps recording can contain frames skipped by display refresh.

RAW photos remain separate exposures and use a different representation adapter. RGB CFA/reconstruction previews are approximations. STREAM ERROR reuses samples from the same decoded frame; there is no actual codec-packet corruption or previous-frame datamoshing. Processed RAW video and a UI for arbitrary intermediate taps are not implemented; the common state and causal-prefix APIs support later extension.

This redesign has not been verified on a physical camera, across GPU vendors, under sustained thermal pressure, or for long/high-resolution recordings. Real-device sensor response and RAW/DNG behavior need hardware testing. The original RAW recording queue and packaging code were retained; historical device results are not new validation.
