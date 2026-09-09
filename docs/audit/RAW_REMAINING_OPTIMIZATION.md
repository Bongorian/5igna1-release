# Remaining RAW effects optimization — 2026-09-09

[Japanese edition](RAW_REMAINING_OPTIMIZATION.ja.md) · [First optimization pass](RAW_OPTIMIZATION.md)

Unreleased work on `codex/kotlin-cleanup`, based on `5063f14`. This pass specializes the four remaining CPU RAW effects. PIXEL DAMAGE and ROW ERROR retain the earlier optimization.

- EXPOSURE: evaluate its gain and sine once per row; retain the exact Float/Double conversions, expression order and rounding.
- BIT ERROR: read immutable controls once and cache the decision for each block. Block coordinates, seeded hashing, bit selection and clipping are unchanged.
- ADDRESS ERROR: reuse the decision within each byte-addressed region, preserving odd offsets, regions crossing rows, and end-of-buffer behavior.
- CFA ERROR: reuse the decision within each region, preserving phase selection and odd-dimension edge behavior.

Every stage still reads from its unchanged input and retains the original copy and output-clipping behavior. No approximation, new random generator, floating-point reassociation, parallel reduction, GPU processing or dependency is introduced. BIT ERROR and CFA ERROR each use a BooleanArray of the number of horizontal regions, reused across rows; RAW peak memory has not been measured.

## Output validation

The frozen pre-optimization `RawGlitchReference.kt` and Java migration golden hashes remain unchanged. The differential suite passes 1,500 cases covering all six individual effects, all-six chains and randomized routes, seeds, levels, LIVE states, dimensions and black/white levels. Another 774 comparisons target odd region sizes, odd byte offsets, CFA phases, image edges, probability endpoints and exposure rounding. Input immutability is checked. This is a broad deterministic corpus, not an exhaustive enumeration of all possible input buffers.

`testFdroidDebugUnitTest` passes all 11 test methods. `assembleFdroidDebug`, `lintFdroidDebug` (0 errors, 136 existing warnings and 1 hint), and `git diff --check` pass.

## Physical-device measurements

Model 25060RK16C, Android 16; F-Droid debug APKs. Before means `5063f14`, which already includes the PIXEL DAMAGE and ROW ERROR improvements. Order: before → after → after → before → before → after. Each run starts a new instrumented process with the same external benchmark APK. The camera is detached before RAW timing.

Each workload uses the same 1920×1080 RAW16 data (black 256, white 4095), default effect controls and LEVEL, fixed seed 123456 and LIVE disabled. Each case has ten warm-up calls and twenty timed calls. Values below are medians of three run medians; parentheses show their minimum–maximum, in milliseconds. Camera acquisition and file saving are excluded.

| Workload | Before, ms | After, ms | Speedup |
|---|---:|---:|---:|
| EXPOSURE | 798.82 (776.35–829.77) | 70.68 (70.55–71.76) | 11.30× |
| BIT ERROR | 218.15 (208.84–219.54) | 38.91 (38.77–39.93) | 5.61× |
| ADDRESS ERROR | 254.63 (253.96–308.48) | 41.30 (41.01–41.32) | 6.17× |
| CFA ERROR | 230.70 (229.92–242.69) | 44.07 (42.78–44.11) | 5.24× |
| All six RAW effects | 1679.39 (1647.83–1733.22) | 423.92 (420.84–437.91) | 3.96× |

For every workload, the full output SHA-256 matches across all six runs and differs from the input hash. All-six chaining is tested as well as each changed effect alone. [Raw records and APK hashes](raw-remaining-results.json).

Battery temperature: 40.1–40.1 °C. Reported thermal statuses: [0] (0 means no reported throttling); power connected. CPU clocks were not locked. These are short fixed-workload results on one device and debug build; they do not establish end-to-end capture speed, sustained recording, release-build or all-settings performance. The final optimized development APK was restored on the device, and the temporary measurement APK was removed.
