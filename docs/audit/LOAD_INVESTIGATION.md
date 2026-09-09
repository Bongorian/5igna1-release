# Rendering and RAW workload investigation — 2026-09-10

[日本語](LOAD_INVESTIGATION.ja.md) · [Measurements](load-investigation-results.json) · [Inventory](README.md)

Source: `a325a5a` plus investigation-only Android instrumentation on `codex/render-load-investigation`. Pixel 9, Mali-G715, DEV APK. Production code is unchanged. The prototypes are not release-ready optimizations.

## Findings and priority

1. **RAW thermal-noise cell caching is a focused candidate.** At 1920×1080, grain size 8, current processing took a median 240.87 ms versus 149.43 ms with a row-cell cache (38% shorter). Each tested output matched every byte. At grain 1, 238.75 versus 234.10 ms is a small difference, not a persuasive general gain. This helps processed RAW with this effect; it does not speed up ordinary GPU preview. Broader grains, seeds, dimensions and cancellation behavior need regression coverage before adopting it.
2. **Remove the final transport copy selectively, not universally.** The ordinary chain already borrows its unpublished output as a ping-pong target. The transport path always finishes with another draw. A test-only direct-final-target variant produced byte-identical output for LED, Analog→LED and DVD→CRT at 1280×720 and 1920×1080. At 1080p, LED improved in both runs: 1.554→1.318 ms and 1.911→1.735 ms (about 15% and 9%). DVD→CRT improved in one run (3.395→2.805 ms) but regressed in another (2.760→3.199 ms); Analog→LED did not improve at 1080p in either run. Fewer draws do not guarantee faster completion on this tile-based GPU. Do not remove network-history, resizing or nearest-neighbor copies without separate correctness tests. External camera textures, aliased targets and more device GPUs remain untested for the prototype.
3. **Do not enable fixed RAW pauses by default.** Alongside real 1080×1920, thirteen-effect preview, a synthetic 4080×3072 six-effect RAW chain took 2.57–2.93 s normally and 4.52–4.61 s with a 2 ms pause after approximately 8 ms of work. Both paths retained about 59 fps (idle 59.3 fps), and output hashes matched. Worker CPU time also increased in the paused trials. This device did not demonstrate preview starvation, so the extra delay has no demonstrated benefit. Consider deadline-driven yielding only if another device actually misses preview deadlines.

## Where the work is

Final 1080p GPU run, median of three round medians, completion through `glFinish`: thirteen ordinary effects 8.731 ms; CFA ERROR 3.088 ms; DEMOSAIC ERROR 2.614 ms; active SMEAR 2.004 ms; active MOTION BLUR 1.746 ms. CLEAN was 0.976 ms. These are offscreen fixed-workload timings, not camera-to-display latency or energy. Per-effect costs are not additive; DVFS and fixed submission overhead matter.

Isolated 1920×1080 RAW stage medians: SMEAR 475.89 ms, MOTION BLUR 336.38 ms, THERMAL NOISE 239.02 ms, PIXEL DAMAGE 143.90 ms, EXPOSURE 86.56 ms; remaining four base stages about 39–48 ms. RAW blur/smear already precompute sample coordinates. Their remaining 9/8 sample reads cannot simply be dropped without changing the image. Exposure, row faults, bit regions and CFA groups already cache some shared work. Program uniforms and shaders are also cached in the current GPU renderer.

Synthetic full-size DNG writing with captured camera metadata took 18–32 ms to a discard sink and 67–70 ms to a private cache file (25,093,616 bytes). This isolates writer work from RAW processing. The file timings include stream close but not fsync, MediaStore publication, thumbnails, camera acquisition or a durable-storage guarantee. They must not be generalized to every phone's storage.

## Method and limits

GPU: fixed patterned RGBA input, 720p/1080p, five warm-up draws per variant and three rounds of fifteen measurements. Paired variants alternate order. Completion timing includes `glFinish`; CPU submission timing excludes that wait. Readback and hashing are outside measured intervals. Pixel comparisons cover the final fixed frame for each tested case and round, not all animated frames. The first run accidentally bypassed three experimental effects; those entries are explicitly excluded in JSON. The final run enabled them with nonzero overrides.

RAW scheduling: actual camera preview, EXPERT enabled to avoid changing frame caps, ADVANCED disabled, immutable synthetic RAW input, normal/paused/paused/normal order. This stresses competition without inserting fake photographic output into the user's gallery. It measures average rendered fps, not display-latency percentiles or shutter latency. Stage/cache tests run after detaching the camera. Cache comparisons alternate order and compare all bytes. Temporary DNG files were removed.

Phone remained USB-powered; CPU/GPU clocks were not locked. Battery readings: first GPU run 36.6°C/status 0; RAW competition 38°C/status 0; final GPU run 39°C/status 1. Different thermal/governor conditions limit cross-run ranking. No reduction in power consumption or sustained surface temperature has been demonstrated. No low-end physical device was tested.

Reproduce using DeviceChecks `action=load-investigation`, `part=gpu` or `part=raw` on an unlocked DEV device. `LoadInvestigation` owns the benchmark; `TransportTargetProbe` is an isolated copy of the current renderer with the candidate final-copy omission. Keep these out of production. A shipping change requires wider byte comparisons, live-camera/recording/ADVANCED coverage and matched-condition performance tests.
