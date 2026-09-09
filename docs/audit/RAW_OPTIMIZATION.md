# RAW processing optimization — 2026-09-09

[Japanese edition](RAW_OPTIMIZATION.ja.md)

Unreleased work on `codex/kotlin-cleanup`, based on `66e1950`. Only the CPU RAW implementations of PIXEL DAMAGE and ROW ERROR are specialized. Other effects, processing order, input ownership, copies, shaders, RAW capture and export remain unchanged.

## Exactness

PIXEL DAMAGE reads immutable parameters once per invocation and computes each column fault once per column. Per-pixel grain and pixel-fault hashing retain their seeds, coordinates and original arithmetic. ROW ERROR calculates displacement once per row and reads immutable parameters once per invocation. Both retain the original floating-point operation order, integer conversion, `Math.round`, clipping, Bayer-preserving offsets and reads from the unchanged stage input. No approximation or parallel reduction is introduced.

`RawGlitchReference.kt` freezes the pre-optimization implementation as a test-only oracle. A deterministic differential test compares all output bytes over 600 combinations of seeds, levels, LIVE states, routes, dimensions (including odd dimensions and one-pixel axes), and black/white levels. It also checks that input bytes remain unchanged and outputs do not alias the input. Random sample bytes include values outside the declared black/white interval to exercise clipping. The existing Java migration golden hashes are unchanged and pass.

Validation: `testFdroidDebugUnitTest` (10 test methods, zero failures/errors), `assembleFdroidDebug`, `lintFdroidDebug` (zero errors; 136 existing warnings and one hint), and `git diff --check`. This test corpus supports byte-exact compatibility; it is not an exhaustive enumeration of every possible input.

## Device measurement

Model 25060RK16C, Android 16, F-Droid debug APKs. Order: before → after → after → before → before → after. Each run used a new instrumented process, the same external measurement APK, fixed 1920×1080 RAW16 input and PIXEL DAMAGE + ROW ERROR. Ten warm-ups precede thirty timed calls; each row below is their median. Camera acquisition and file saving are excluded. The before APK is byte-identical to the Kotlin APK in the earlier Java/Kotlin comparison.

| Run | RAW processing, ms |
|---|---:|
| before-1 | 843.71 |
| after-2 | 195.17 |
| after-3 | 196.05 |
| before-4 | 845.52 |
| before-5 | 851.78 |
| after-6 | 194.16 |

Median of the three run medians: **845.52 ms → 195.17 ms**, **4.33×** throughput and **76.9%** less elapsed processing time for this workload. All six full output hashes match the existing baseline: `ddf781b92ef666a42e0f0d708dc9bbb0579646979291c78b26de439ff59fdb8d`.

Battery temperature ranged from 38.0 to 38.4 °C; reported thermal statuses were [0] (0 means no reported throttling). Power was connected. This is a short synthetic workload on one device, not an end-to-end shutter latency, release-build, sustained-recording or universal RAW speed claim. CPU clocks were not locked. No RAW peak-memory measurement was made; PIXEL DAMAGE adds one BooleanArray and one IntArray of image width for column caching.

[Raw measurement records and APK hashes](raw-optimization-results.json). The optimized development APK was restored to the device after measurement; the temporary benchmark APK was removed.
