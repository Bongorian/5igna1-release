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

## Physical-device debug checks — 2026-09-08

Model 25060RK16C, Android 16 / API 36; development package `com.bongorian.signa1.debug`, source `cc470e3`. Installed the F-Droid debug APK and instrumentation APK after owner authorization. Four instrumentation actions passed:

- `effects`: all 13 fault shaders, named controls, snapshot replay, bypass and causal composition.
- `capture-contract`: displayed-frame reservation while later frames arrive, saved JPEG pixel equality and capture timestamp/state metadata.
- `state`: commit/cancel/cleanup and GPU behavior.
- `compatibility`: camera catalogs, low-resolution selection, JPEG without an encoder, session recovery and front/back startup.

Detailed local results are in ignored `verification/debug-session/`. This is one-device coverage, not a cross-device or sustained-performance assessment. A test photo was saved by the capture-contract check. No submitted release artifacts, signing keys or published tags were changed.

## Preview/editor fixes — 2026-09-08

On the same physical device, a flat-gray Pixel Damage fixture with the owner's stored identity reproduced a 62% damaged region instead of sparse sites. Replacing the GPU Pixel Damage sine hash with bounded arithmetic restored 6.9–8.9% damaged sites across three identities at 192×256 and 1080×1440. The RAW integer processing path is unchanged.

Additional checks passed:

- Editor: preview and panel bounds do not overlap; slider changes reach live frames without committing; multi-selection, random chain generation, Apply, Cancel and stale-dialog dismissal preserve the correct state/layout.
- Default camera catalogs: maximum supported JPEG/live, RAW and video selections; lower-resolution recovery and front/back camera sessions.
- Largest live JPEG with Pixel Damage + Row Error + Chroma Error: 3072×4096 (12.6 MP) saved successfully on this device. This is the camera's exposed live stream, not a claim about every manufacturer's high-resolution still mode.
- All 13 shader/control fixtures and the displayed-frame JPEG capture contract passed again.
- Unit tests: 5 fixtures in four variants, 20 executions, zero failures/errors. The added randomizer check covers legal format-specific routes, varied combinations, immutable input, bounded controls and saved-state roundtrip.
- `test lint assembleFdroidDebug assemblePlayDebug assembleFdroidDebugAndroidTest`, locale/repository guards and whitespace checks passed.

## Limits and remaining hardware validation

JPEG now saves the actual selected live camera signal (defaulting to the largest supported stream within the GPU texture limit), not a later full-resolution still exposure. Display and encoder use the same canonical images; a high-fps recording can contain frames skipped by display refresh.

RAW photos remain separate exposures and use a different representation adapter. RGB CFA/reconstruction previews are approximations. STREAM ERROR reuses samples from the same decoded frame; there is no actual codec-packet corruption or previous-frame datamoshing. Processed RAW video and a UI for arbitrary intermediate taps are not implemented; the common state and causal-prefix APIs support later extension.

Physical-device coverage is limited to the checks above on one device. Cross-GPU behavior, sustained thermal pressure, long/high-resolution recordings, real-device sensor response and RAW/DNG behavior still need hardware testing. The original RAW recording queue and packaging code were retained; historical device results are not new validation.

## Lean chain UI — 2026-09-08

On the attached Android 16 / API 36 device, the revised debug UI passed editor geometry and touch-control checks, draft apply/cancel, random generation, stale-dialog dismissal, all 13 faults in the RAW catalog (including CRT / DISPLAY), explicit JPEG conversion with draft retention, and JPEG–RAW route persistence. State/lifecycle and displayed-frame JPEG capture-contract checks also passed. RAW catalog and CRT tuning screenshots were inspected. The editor keeps the camera above its panel; longer control lists scroll. `test`, `lint`, both debug flavors, and the instrumentation APK build passed; 301 localized strings passed the locale check. Submitted release artifacts and signing material were not rebuilt or changed.

## JPG / RAW and custom settings surfaces — 2026-09-08

Photo selection now offers JPG and sensor-processed RAW only; legacy original-RAW preferences migrate to RAW. Device checks passed the custom picker, cancellation, selection, migration and toggle behavior, plus chain preservation across format changes. RAW capture saved a 4096×3072 DNG on the attached device. Format, LIVE and capture settings screenshots were inspected. App-owned platform alert dialogs, popup menus and default switches were replaced with shared dark panels and custom toggle styling. Both debug flavors, unit tests, lint and the 301-string locale check passed. Android-owned permission and external-app screens remain controlled by Android.

## Shared camera album, LIVE direction and ADVANCED MODE — 2026-09-08

The attached Android 16 / API 36 device saved JPG (1080×1920), processed DNG (4096×3072) and MP4 (3072×4096, short silent recording) to the same MediaStore folder, `DCIM/5igna1/`. Existing media was not moved; experimental RAW ZIP stays in Download.

Device checks passed LIVE preview isolation and apply/cancel, saved style/clock settings, HOLD with advancing camera timestamps, HIT and time reset, plus preview geometry with the LIVE editor below the camera. ADVANCED checks passed exact numeric input and invalid-value rejection, draft isolation and persistence, signed 64-bit event-seed entry/persistence, a direct pixel-density GPU result, and minimum/maximum SIGNAL/PROFILE rendering for all 13 faults. Editor, state/lifecycle and displayed-frame JPEG snapshot checks also passed. Advanced and LIVE UI screenshots were inspected.

Unit checks cover the complete internal parameter catalog, finite bounds, automatic/fixed values, immutable snapshots, RAW bindings, seed serialization/reproducibility, time wrapping/reversal/stepping, performance envelopes and preview forks. Both debug flavors, unit tests, lint, the 338-string locale check, repository guard and whitespace check passed. Tests cover one physical device and short captures; they do not establish long recording or cross-device performance. Submitted release artifacts, signing material and public tags remain unchanged.

A repeated dialog-transition check exposed missing TextureView update callbacks on this device: the UI had consumed a newer timestamp while the history acknowledgement remained older. A foreground-only 100 ms fallback now reads the UI-consumed SurfaceTexture timestamp, preserving the rule that submitted-but-unseen buffers are never acknowledged. The advanced device test suppresses update callbacks deliberately; snapshot capture and state/lifecycle checks are rerun with this recovery.

## LIVE as current evolution; global advanced setting — 2026-09-08

LIVE now places time progression first and names variation patterns by their behavior. Periods and update intervals use seconds, with equivalent duration migration from previous tempo/beat preferences. PAUSE/RESUME, TRIGGER and RESET TIME use equal widths and gaps. ADVANCED MODE is a single Settings preference; per-fault switches are removed. Device checks passed global setting draft cancellation/application and persistence, absence of a per-fault switch, seconds-based LIVE persistence and time behavior, and editor geometry. Screenshots of Settings, the LIVE panel and main time-control row were inspected. Both debug flavors, tests, lint, 343-string locale and repository checks passed.

## Adaptive workload and device recommendations — 2026-09-08

On the attached Android 16 / API 36 device, the recommended JPG and video outputs were 1080×1920. With a real camera and unchanged Pixel Damage settings, simulated severe thermal input reduced processed preview frames from 47–48 to 12 per two seconds. This measures render count, not temperature or power reduction. LIVE was off, output dimensions and effect state were retained, critical thermal input paused rendering/camera capture, and simulated sustained cooling resumed the preview. A recommended 1080×1920 MP4 recording was finalized successfully on a simulated critical-heat transition, saved to DCIM/5igna1, and did not restart recording on recovery. Camera catalogs/manual maximum, low-resolution fallback, front/back recovery, displayed-frame JPEG pixel/snapshot equality and state/lifecycle checks passed.

Recommendation-button draft cancellation, legacy maximum migration, explicit size/maximum retention and conservative pixel limits passed. Settings, reduced-rate and cooling UI screenshots were inspected. Unit tests cover pacing, rapid reductions, slow recovery, thermal/battery/headroom signals, critical pause/cooldown and render-cost limits. Tests and lint passed for both debug distributions; 351 translated strings and repository/whitespace checks passed. Pixel 6 Pro temperature, power and sustained-recording behavior remain unverified because that device is not connected. Tests simulated heat inputs inside the app without changing OS thermal settings or deliberately heating the device. Submitted release artifacts and keys remain unchanged.

## EXPERT MODE / settings — 2026-09-08

Expert mode passed device checks for draft cancel/apply, persistence, preview rendering (59 frames in 2 seconds), continued recording under simulated critical temperature and high rendering cost, manual stop/save, and restoration of normal thermal protection when disabled. ADVANCED and normal adaptive-load regression checks passed. The settings sections and spacing were visually reviewed. These checks use simulated app inputs; they do not change Android thermal controls or establish sustained device performance.

Panels, controls, fields and preview outlines now share a 12 dp radius; small badges use 4 dp. Typography uses shared sans-serif styles, 18 sp titles, 13 sp setting labels and 12 sp explanatory copy, with consistent line spacing and no extra tracking on Japanese headings. The FAULT catalog uses two columns with larger text. Device checks passed for EXPERT (58 rendered frames in 2 seconds in the final build), ADVANCED, and the 13-effect RAW catalog / JPG round-trip. Settings and effect screens were visually reviewed. Unit checks, lint, both debug builds, and 364 translated strings passed.

LIVE apply/cancel, time controls and unobscured-preview checks also passed; LIVE and ADVANCED typography were visually reviewed on the same device.

## Capture workspace and mixed-media preview — 2026-09-08

The format menu is anchored beneath its button. Light/GPS/audio icons occupy the top toolbar, audio is available in photo mode, the large capture logo is removed, and FAULT STATE uses live event meters without covering the preview. Device checks passed for menu position and format selection/cancel, utility position/audio toggling, meter rows, editor apply/cancel, camera disconnection recovery and deliberate preview-input stalls.

External video testing found a viewer remaining in the capture task after attempted dismissal; an app relaunch was also delivered to that external viewer. Normal photo/video viewing now uses the internal mixed-media viewer, with external viewing isolated in a separate document task. On the connected Android 16 device, a JPEG and 1080×1920/30 fps MP4 were saved, browsed using real horizontal swipes, and video play/pause/seek was verified. Six downward-swipe video dismissals each released the player and surface and returned to newly acknowledged camera frames. Immediate close during loading, an unavailable video URI, background cleanup and subsequent video recording/saving also passed. The catalog included both media kinds (43 accessible captures at the first run). Photos, videos and the returned camera UI were visually reviewed; dark fixture media reflected the camera scene.

Unit tests, lint, both debug builds, 380 localized strings and repository checks passed. These are real-device short-run checks, not a guarantee for every decoder or long playback session. The submitted release artifacts, signing material and published tag remain unchanged.

## 1.1.0 source integration — 2026-09-08

Allocated versionName 1.1.0 / versionCode 9 for the next release. `lint`, `test`, `assembleFdroidDebug`, `assembleFdroidRelease` (unsigned), `assemblePlayDebug`, `assembleFdroidDebugAndroidTest`, `verifyFossDependencies` and `dependencyInventory` passed. Release metadata, 380 translated strings, repository guards, dependency inventory and whitespace checks passed. Verified release/debug APK IDs and version metadata, 267 local Markdown links, and all three privacy HTML bodies against bundled text. Earlier physical-device checks above cover the unchanged app behavior; this integration changes version metadata and documentation only. Existing v1.0.0 and submitted code-8 artifacts remain the published/submitted baseline; no new signing, store submission, tag or release publication is part of this integration. Store screenshots still represent 1.0.0 and must be refreshed before the next store submission.


## Development tutorial — 2026-09-09

Branch `codex/onboarding-tutorial` adds the guide after the submitted Play 1.1.0 / code 9 candidate. It has not been merged into main or submitted to a store. No physical device was connected.

- Android emulator Pixel 9 / API 37: first-launch display, forward/back pages, skip and completion persistence, system Back, activity recreation at page 3, Settings replay with an uncommitted settings draft, and camera recovery passed (`DeviceChecks`, action `tutorial`).
- With camera permission revoked, the guide remained the active readable window; the permission prompt appeared only after dismissal (`tutorial-permission`).
- All five pages were visually reviewed in English, Japanese and Simplified Chinese at normal and 1.5× font scale. The body scrolls independently of navigation buttons. Screenshot checks wait for dialog transitions to settle.
- `lint`, `test`, F-Droid debug / unsigned release, Play debug and instrumentation APK builds passed. Locale validation covers 395 strings; repository, release metadata and whitespace checks passed.
- Tests use only the debug application ID. Existing version 8 artifacts, signed Play version 9 bundle, main branch and published tag remain unchanged. A future submission must allocate a new release version/code.

## Kotlin rewrite — 2026-09-09

Branch `codex/kotlin-cleanup`, based on the tutorial development branch, migrates all application and test Java sources to Kotlin 2.2.20. The application has 44 Kotlin files, including separate camera screen construction, capture storage, and immutable collection helpers. GLSL shaders and release/build utility languages remain appropriate to their jobs.

- A clean build passed `lint`, `lintPlayDebug`, `test`, F-Droid debug and unsigned release, Play debug, instrumentation packaging, FOSS dependency checks, and the dependency inventory. All 36 JVM test executions passed (nine tests in each of four flavor/build-type combinations). Lint reported no errors; existing resource/style warnings remain.
- `MigrationGoldenTest` uses fixed SHA-256 expectations captured from Java commit `c0ce2eb` before conversion. All effect IDs at four levels, 120 deterministic frames, and RAW buffers at three sizes produce identical evaluated frames and RAW bytes. Expected hashes were not regenerated from Kotlin results.
- Pixel 9 / API 37 emulator actions passed: `state`, `effects`, `pixel-preview`, `advanced`, `product-ui`, `capture-contract`, `mixed-preview`, and `tutorial`. These cover edit commit/cancel, all 13 fault shaders and named controls, sparse pixel placement, advanced values, camera interruption/recovery, displayed-frame JPEG pixel equality and metadata, mixed photo/video playback and cleanup, and tutorial persistence/locales.
- Capture testing caught and corrected an unnecessary non-null requirement on original JPEG metadata bytes; displayed-frame captures correctly allow those bytes to be absent. The tutorial test now waits for window input focus before sending system Back and waits for dismissal, avoiding a WindowManager timing race.
- Dependency licenses and bundled notices were refreshed, including the Kotlin standard library's ThreeTen BSD notice. The release runtime graphs remain identical across flavors. Translation checks cover 395 strings.
- No physical device was connected. RAW byte fixtures pass, but hardware RAW/DNG/RAW-video behavior needs a future device run. All emulator operations use `com.bongorian.signa1.debug`. Main, signed submitted bundles, existing keys, and release tags remain unchanged; no store submission is part of this rewrite.

## Kotlin physical-device check — 2026-09-09

The owner connected an Android 16 device (model `25060RK16C`, device `dali`) over USB and authorized installation and debugging. Installed the Kotlin debug application from `43ee376` as `5igna1 DEV`; the release application was not replaced.

- Eight instrumented actions passed: `capture-contract`, `raw-original`, `raw`, `raw-video`, `video`, `state`, `advanced`, and `product-ui`. Camera capability queries reported RAW on both camera IDs; capture checks used the active rear camera.
- Original and processed 4096×3072 DNG files both opened, unpacked, and developed with LibRaw on the host. The processed capture used ROW ERROR at 70% LEVEL.
- A short RAW recording produced a finalized ZIP with two original DNG frames, matching dimensions, timestamps, and manifest. This is a short functional check, not an endurance or throughput claim.
- The silent MP4 decoded fully at 1080×1920, 3.029 seconds, and 30.05 fps, with monotonic timestamps and a correct media time origin. The on-screen preview rate was lower under the existing adaptive policy; the encoded clip retained its 30 fps cadence.
- JPEG pixels and snapshot metadata matched the pinned displayed frame. Edit commit/cancel, advanced controls, camera interruptions, and stalled-preview recovery passed. No new application defect was found in this run. Captures remain in the device's shared 5igna1 camera folder; test settings were restored.
