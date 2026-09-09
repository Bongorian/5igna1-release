# GPU detail and LIGHT MODE — 2026-09-10

[日本語](GPU_DETAIL_LIGHT.ja.md) · [Measurements](gpu-detail-light-results.json) · [Modes](../PERFORMANCE.md)

Unreleased development after 1.6.0, branch `codex/gpu-detail-investigation`, based on `936903f`. Pixel 9 DEV only; production/release APKs and published Pages are unchanged. The earlier [load investigation](LOAD_INVESTIGATION.md) established the starting candidates.

## Implemented

- **Explicit LIGHT MODE, default OFF.** LIGHT excludes ADVANCED and EXPERT; enabling either turns LIGHT off. Both being OFF does not implicitly enable LIGHT. Settings changes save immediately, and existing FAULT parameters are retained.
- **View-sized effect rendering in LIGHT.** The cap uses physical preview-view pixels, preserves aspect ratio, does not upscale small input, and follows window resize. Camera input and selected saved-output dimensions remain unchanged. A LIGHT JPEG renders the latest input once at the selected output resolution before immutable readback; it never enlarges the small preview. Preview fine detail can differ and capture need not match the last displayed frame. RGB recording retains full-size processing and returns to the cap after stopping. NETWORK's retained processed image also stays full size; RAW remains a separate sensor exposure. TIME ECHO retains its existing bounded, lower-resolution input history.
- **Common standalone LED copy omission.** A standalone LED pass may write directly to an owned, unpublished output texture. Unknown destinations, source aliases and mixed chains keep the original route. This applies to all modes and rendering callers that provide the existing owned-target contract. No shader formulas, neighbor sample counts or processing order were changed.

The capture path has an extra full-resolution render on LIGHT shots that use a smaller preview. Shutter latency percentiles were not measured. Existing automatic cadence and thermal control remain enabled; LIGHT does not add a lower frame-rate cap.

## Measurements

The full-size fixture came from the device configuration: source/output 3072×4080, preview view 986×1309. A view cap gives 985×1309, about 90% fewer intermediate output pixels. These are RGBA fixture render/completion times, including the final display-sized blit, not camera-to-display latency or electric power.

| Workload | Full-size processing | View-capped processing |
|---|---:|---:|
| CLEAN | 8.24 ms | 5.23 ms |
| CFA, full coverage | 16.74 ms | 6.54 ms |
| DEMOSAIC, full interpolation | 8.15 ms | 4.46 ms |
| CFA + DEMOSAIC | 19.56 ms | 7.46 ms |
| All thirteen, with the above overrides | 40.94 ms | 7.55 ms |

Table uses the first detail run; the complete second run is retained in JSON. Fewer pixels do not imply an equal reduction in total time or power. The camera still supplies the selected full-sized signal; this experiment isolates GPU processing.

The implemented standalone LED path was then compared against a frozen pre-change renderer with a real external camera texture, at 1080×1920. Median of four round medians: **3.680→2.895 ms**, about 21% shorter. All compared bytes matched. The earlier 2D trials showed about 9–15% reductions. These are different workloads and thermal conditions, not a universal speedup range.

## Candidates not adopted

Three CFA/DEMOSAIC variants conditionally skip unused neighbors, share Bayer phase calculations, or combine both. The final comparison suite checked 243 cases per variant (729 comparisons), spanning odd dimensions, three input patterns, phases, region coverage, seeds, interpolation scales and 2D transforms. All compared bytes matched; timed 720p/1080p outputs also matched.

Performance was inconsistent. In the second run at 1080p, conditional reads changed CFA 6.557→6.344 ms, but DEMOSAIC 2.620→2.805 ms. All thirteen changed only 8.653→8.641 ms. Sharing phases and specializing away an identity texture transform did not establish a consistent general benefit. All remain test-only; the application shader is unchanged. Small 2D tests do not establish equivalence across every precision/driver/input combination. The first run's correctness matrix had correlated parameters; the second expanded matrix supersedes it for coverage.

A separate same-size fixture showed that removing the last display blit can help some workloads, but the full chain did not improve consistently. The application's output buffer is also needed for capture/history/recording, and variable-size resampling changes the image. No general direct-to-window bypass was introduced. Mixed transport copy omission also remains excluded following the earlier inconsistent results.

## Verification

- Standard F-Droid debug JVM suite: 42 tests, no failures. Debug and instrumentation APK builds and `lintFdroidDebug` passed; locale keys/placeholders passed for English/Japanese/Chinese.
- `light-mode` on Pixel 9 passed: immediate/exclusive mode switches and preference reload; 985×1309 preview with 3072×4080 JPEG; full-size live-camera output byte-identical to an independent renderer; window resize preserving save dimensions; 1080×1920 MP4; 4080×3072 DNG; NETWORK full-size retention; return to reduced preview after video; full-size ADVANCED history.
- LED: 49 additional exact comparisons, including real OES camera input, 2D fixtures, owned/unknown outputs, mixed routes, varied identities/amounts and a source/output alias. Four timed real-camera rounds also compared bytes.
- Existing `capture-contract` passed: acknowledged displayed frame, later camera frames, JPEG pixels and immutable timestamp/state metadata. `normal-capture` also passed after making its fixture explicitly disable the new LIGHT option; its first run had inherited LIGHT and correctly produced LIGHT metadata, which failed the old normal-only assertion.
- Saved LIGHT test photos/videos were removed after validation and settings restored. UI screenshot checked locally; camera-containing screenshots and media are not committed.

GPU detail trials use five warmups, four rounds, paired/counterbalanced variants and `glFinish` completion. CPU submission time excludes the completion wait. Readbacks/hash comparisons are outside timing. The final LED check uses five warmups and four rounds of twenty alternating samples. USB/charger power was attached, clocks were not fixed, and thermal/DVFS variation remains. No power, sustained heat, low-end-device or shutter-latency percentile claim is made.

Reproduce with DeviceChecks `action=gpu-detail` (bounded test-only shader/transfer experiments) and `action=light-mode` (application integration). `TransportCopyReference` freezes the pre-change renderer; `GpuDetailInvestigation` owns the rejected variants. The latter is not a general performance test runner for arbitrary production revisions.

## 1.6.1 release follow-up — 2026-09-10

The owner authorized release with reduced additional device testing. 1.6.1/code 17 includes this LIGHT implementation, the preceding DNG compatibility fix, and GPU-aware initial recommendations plus TAP controls. GPU names identify hardware/software/fallback paths; measured CFA+DEMOSAIC+CRT cost and physical view size refine the RAM ceiling. Fresh constrained setups start LIGHT; saved choices remain unchanged. TAP starts playback with recording unless transport was explicitly selected, stops playback with recording, and keeps the shutter centered. Format selection is outside the central mode group.

Validation: lint, 184 unit tests across four variants, F-Droid debug/signed release and Play debug builds, locale/metadata/docs checks passed. Pixel 9 DEV (`Mali-G715`) exercised real heat deferral, subsequent measurement and cache reuse, fresh/saved preference cases and format controls. Cached probe fit: 1.825493 ms overhead + 6.243365 ms/MP; the startup budget was 1,295,604 pixels with a policy ceiling of 60 fps. The retained manual high-resolution setup actually ran with a 15 fps limit: recommendations do not guarantee achieved cadence.

TAP device checks passed centered shutter, automatic start/pause/save, manual transport, image output and TAP/camera background save. The broader legacy test later timed out starting its audio recording; no new audio regression test pass is claimed. A cache test initially compared the startup view budget with a subsequently resized view; it was corrected to test the actual policy contract, then passed. Local evidence: `/tmp/pixel9-gpu-defaults-pass.log`, `/tmp/pixel9-format-defaults.log`, `/tmp/pixel9-tap-defaults.log`, `/tmp/release-1.6.1-build.log` (not distributed). No sustained heat/power or additional-device validation was performed.
