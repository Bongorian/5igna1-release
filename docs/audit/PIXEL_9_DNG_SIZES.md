# Pixel 9 RAW/DNG compatibility — 2026-09-10

Development branch: `codex/dng-compatible-raw`, after 1.6.0. No release publication is part of this fix.

## Observed failure

Installed Play 1.5.1 / code 15 crashed three times while writing a RAW photo. Android DngCreator threw AssertionError because the input dimensions matched neither the pre-correction active array nor pixel array. This was an exception crash, not a recorded low-memory termination.

Pixel 9 rear camera 0 reports 4080×3072 sensor arrays and RAW streams at 4080×3072, 4080×2288, 2032×1536 and 2016×1136. The previous still-photo list accepted all streams; its default 12-million-pixel limit could select the incompatible 4080×2288 stream. The exact selected size at the historical crash was unavailable. The issue remained in 1.6.0.

## Shared fix

DNG compatibility is checked against each camera's metadata, separately for normal and maximum-resolution sensor modes. Both metadata entries must exist. Only full pixel-array or pre-correction-active-array dimensions are offered. This covers ordinary and high-resolution RAW stream candidates; RAW video uses the same predicate. Existing saved incompatible choices fall back through the filtered list. Cameras without a compatible RAW candidate use the existing JPEG fallback.

Saving rechecks dimensions before processing or creating the output. An Android writer AssertionError is handled as a save failure, with pending-output cleanup and existing capture-state recovery. Bayer samples are not resized and sensor metadata is not rewritten to disguise an incompatible stream.

## Verification

On Pixel 9, DEV only:

- Processed RAW with recommended size: three consecutive captures in one activity, all saved at 4080×3072.
- Original RAW with the former 4080×2288 choice: three consecutive captures, all saved at 4080×3072.
- Representative processed and original DNGs each passed LibRaw open, unpack and development.
- JPEG: two consecutive successful saves.
- Debug capability log lists 4080×3072 as the only RAW still and RAW video size for camera 0.

All four variant unit suites passed, 39 tests each (156 executions), including Pixel 9 cropped/binned rejection, active versus pixel-array dimensions, transposed/empty rejection and maximum-mode dimensions. F-Droid/Play debug builds, instrumentation assembly and F-Droid debug lint passed. No physical maximum-resolution-mode device or every manufacturer has been tested. The installed Play app and its data were left unchanged.

[Android DngCreator](https://developer.android.com/reference/android/hardware/camera2/DngCreator) · [Android writer dimension check](https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/jni/android_hardware_camera2_DngCreator.cpp)
