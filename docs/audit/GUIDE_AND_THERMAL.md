# Interactive guide, immediate settings and thermal investigation

[日本語](GUIDE_AND_THERMAL.ja.md) · [Raw measurements](guide-thermal-results.json) · [Injection assessment](DATA_INJECTION_POINTS.md)

Development branch: `codex/interactive-guide-thermal-inputs`, based on 1.3.0. Published releases and submitted bundles are unchanged.

## Changes

The first-launch/Settings guide now spotlights eight actual controls, with a deterministic practice signal and an interactive LEVEL slider. Practice does not change capture settings or save media. It uses no recurring animation, camera input or additional assets/dependencies. Camera processing, presentation acknowledgements and location collection stop while it is open; permission, lifecycle and surface callbacks respect this pause. Closing resumes the camera. Settings temporarily hides while the guide shows the underlying controls.

Following the owner's additional request, Settings saves choices immediately. It has a close button rather than Apply/Back actions; closing, recreating or opening the guide does not cancel selections. The recommended action also disables EXPERT. Previously it changed resolution/quality but left EXPERT active, bypassing the adaptive limits. Language and ADVANCED changes do not reconfigure the camera. Superseded queued camera configurations are skipped, and stale camera setup cannot persist older preferences over newer choices. Fault and LIVE editors retain their own apply/cancel behavior.

## Findings and limits

The connected device was configured for maximum 3072×4096 JPEG signal resolution with EXPERT enabled. At that resolution the pipeline processes about six times as many pixels per rendered frame as 1080×1920, before multiplying by fault passes. EXPERT deliberately bypasses the app's adaptive preview and thermal controls. It remains an explicit user option; this branch does not silently change existing choices or reduce effect quality.

The old tutorial left camera processing running behind its opaque panel. In a 12-second 1.3.0 sample with PIXEL DAMAGE, ROW ERROR and VHS at LEVEL .55, LIVE off, it rendered 359 frames (29.91 fps) and consumed 5410 ms of process CPU time. A valid post-fix guide sample rendered **0 frames** over 12.059 seconds and consumed **166 ms** of CPU time. CPU rate was 45.08% → 1.38% of one core in these samples. This demonstrates removal of unnecessary guide work, not a measured whole-device power or surface-temperature reduction.

A sequential 1.1.0 comparison at the same nominal settings produced 20.08 fps recommended, 6 fps maximum/normal, and 30.08 fps maximum/EXPERT. The earlier 1.3.0 sample produced 14.25, 6 and 29.92 fps. CPU figures varied and the recommended workloads did not reach matching output rates. These short runs do **not** establish or rule out a general version-to-version thermal regression.

All samples used one Android 16 / Mali-G925 phone connected to AC power. Battery telemetry was around 37.5–37.8°C, thermal status 0. There was no surface thermometer, power meter or sustained matched-temperature experiment. Camera timeout runs and a 60-second run interrupted by navigation were excluded. The valid post-fix guide sample checked foreground/guide presence every 100 ms. GPU submission timing is not GPU energy consumption. Long-session heating during ordinary shooting remains unproven by this experiment.

## Verification

The device guide test covers all eight pages in Japanese, English and Chinese, actual highlight bounds, camera detachment, practice-state isolation, saved page restoration, skip/Back/completion, Settings replay and camera recovery. All 24 screenshots were reviewed. Subsequent emulator checks cover immediate settings persistence and Settings replay. Local unit tests, Kotlin compilation, Android test compilation, lint and locale consistency checks pass. At 1080×1920 and font scale 1.5, emulator settings-auto, expert and capture-contract checks passed, including JPEG selection persistence, rapid changes, recreation, EXPERT recording behavior, and displayed/saved JPEG pixel equality. All 12 unit tests and 416-string locale checks passed. Detailed final run outcomes are recorded in the Japanese companion report.

No shader or RAW processing mathematics changed. Data injection was assessed separately and is not implemented.

The permission-denied guide check also passed: the camera permission prompt appears only after dismissal.
