# Repository rules

- Keep UI, effects, RAW processing, export, settings, file I/O and all core features in `app/src/main`.
- Do not add proprietary SDKs to `main`. Google Play Billing, GMS and other Play-only APIs belong exclusively in `src/play` and `playImplementation`.
- AndroidX and other freely licensed Google-authored libraries are not Play Services; audit their licenses before adding them to shared code.
- Keep `assembleFdroidRelease` buildable from a clean checkout without credentials. The F-Droid flavor must remain FOSS, ad-free and tracking-free.
- Do not gate effects, RAW, resolution, export or other core functionality by distribution. Only distribution services and voluntary support entry points may differ.
- Keep `com.bongorian.signa1` as the release application ID. Discuss a required identity or signing migration before changing it. Debug uses `.debug`.
- Never commit release keys, passwords, credentials or local signing files. Do not reuse a Play upload key as a distribution signing key implicitly.
- Keep literal `versionName` / `versionCode` in `app/build.gradle` readable by F-Droid. Release tags are `v<versionName>`; increment `versionCode` for every release, across all channels.
- Do not add dependencies with unknown licenses. Update `THIRD_PARTY_LICENSES.md`, the dependency inventory and bundled notices when dependencies change.
- Record source, author/license and generation inputs for new assets. Do not assume an Internet image is redistributable.
- Do not change image-processing results during refactoring without behavior tests or output comparisons.
- `Effects` owns IDs and processing order, including generated GLSL definitions. Preserve RAW Bayer phase except for effects explicitly designed to alter it. RAW previews are approximations; do not promise identical rendered results.
- Preserve immutable capture snapshots, editor apply/cancel behavior, camera lifecycle cleanup and bounded RAW recording queues.
- Use Android string resources and the existing standard app-language API. Do not introduce a custom runtime translation layer.
- Run relevant checks before committing. Distribution changes require `lint`, `test`, `assembleFdroidDebug`, `assembleFdroidRelease`, `assemblePlayDebug`, `tools/check-locales.py` and release metadata checks.
- Keep public documentation and store descriptions consistent with the code. Do not invent store URLs, performance claims, preset functionality or supported hardware capabilities.
- Repository visibility, store submissions and publication of a draft release are release-owner actions; preparation does not authorize publishing them.

- Keep README and public user guides in English, with a linked Japanese edition. Treat README as the project’s public presentation: explain the creative intent, actual behavior, and first-use path without unverified claims. Preserve the distinction between fixed fault patterns, LIVE time variation, RGB approximations, and RAW sample processing.

## Owner direction — 2026-09-08, submissions pending

- Preserve the submitted 1.0.0 / versionCode 8 artifacts, existing upload and distribution signing keys, and the published v1.0.0 tag (6954138585d55af161d62196e77b941531780855). Never overwrite, regenerate, delete, or retarget them during development.
- The owner reports F-Droid submission awaiting merge and Google Play closed testing awaiting approval. Keep those submitted candidates stable; do not submit replacements as part of routine debugging.
- Owner authorized merging the completed redesign into main and advancing the version on 2026-09-08. Continue development in /Users/bongorian/Documents/ChatGPT/Apps/5igna1-fault-system; main now prepares 1.1.0 / versionCode 9. Use debug builds (com.bongorian.signa1.debug, 5igna1 DEV) for device testing; keep the release application and its data intact.
- Before the next store submission, allocate a higher versionCode and an appropriate new versionName; do not reuse published release identity for changed code.

## Owner direction — Play update, 2026-09-09

- The owner explicitly requested applying the current 1.1.0 source to Google Play and reports nine testers currently using 1.0.0. This authorizes a 1.1.0 / code 9 update to the existing closed-test Alpha track and the matching store materials. Preserve its tester group and country settings.
- Continue preserving the original 1.0.0 artifacts, v1.0.0 tag and signing keys. F-Droid remains on its existing submission; this Play update does not authorize changing that submission or publishing a GitHub release.


## Owner direction — tutorial development, 2026-09-09

- The owner requested first-launch and Settings-accessible tutorials on a development branch, with no physical device connected. Keep this work on `codex/onboarding-tutorial`; use the emulator and debug builds.
- This feature is not part of the signed Play 1.1.0 / code 9 submission. Preserve that bundle and main; allocate a higher versionCode before any future submission of changed application code.

## Owner direction — Kotlin rewrite, 2026-09-09

- The owner requested a complete, lean Kotlin rewrite. Continue on `codex/kotlin-cleanup`, based on the tutorial development branch. Application and test sources belong in Kotlin source sets; shaders stay GLSL and build/release utilities use their existing suitable languages.
- Preserve the submitted Play bundle, main, release tags, and signing material. Validate the development rewrite with debug builds and the emulator; this work does not authorize another store submission.
- Keep migration golden hashes fixed to the pre-rewrite Java output. Do not replace their expected values with Kotlin output to make a refactor pass.

## Owner direction — physical Kotlin debugging, 2026-09-09

- The owner explicitly authorized installing and debugging the Kotlin build on the connected physical device. Use `com.bongorian.signa1.debug` and preserve the installed release application. Short camera/RAW/video test captures are within this debugging scope; keep release submissions and main unchanged.

## Owner direction — GitHub releases, 2026-09-09

- The owner explicitly authorized publishing current main as 1.1.0 / code 9, the tutorial branch as 1.2.0 / code 10, and the optimized Kotlin branch as 1.3.0 / code 11. Publish verified GitHub APKs using the existing distribution key, with 1.3.0 as latest. This supersedes earlier development-only restrictions for these GitHub releases. Preserve existing tags, signing keys and submitted store artifacts; this request does not change store submissions.

## Owner direction — Play 1.3.0 update, 2026-09-09

- The owner explicitly requested distributing 1.3.0 on Play while skipping 1.2.0. The existing Alpha track accepted 1.3.0 / code 11 for review, using the existing Play upload key and unchanged tester/country settings. Managed publishing remains on; approval and the subsequent publication action are pending. See docs/PLAY_1_3_0.md.
- Preserve the submitted code-11 AAB and prior artifacts. Any changed application code in a future submission needs a higher versionCode.

## Owner direction — interactive guide and thermal audit, 2026-09-09

- The owner requested a development branch for an interactive guide, investigating/improving heat, and assessing image/noise/other-frame injection boundaries. Work on `codex/interactive-guide-thermal-inputs`; preserve main, published releases and the submitted code-11 artifacts. Injection is an assessment, not a requested feature implementation. Continue authorized debug-only physical-device testing.
- The owner subsequently requested immediate persistence in the Settings panel, replacing its Apply action. This supersedes settings draft/cancel semantics for `QualityDialog`; effect and LIVE editors retain their existing apply/cancel behavior.

## Owner direction — experimental device response, 2026-09-09

- The owner approved implementing per-stage motion/audio/frame-timing/temperature/app-CPU sensitivity and motion blur, thermal noise and smear on the current development branch. Use one Settings switch, default OFF; preserve selections and values while bypassing the additions when OFF. Inputs react with LIVE ON and respect global input controls and permissions. Sensitivity range is 0–4, with native routes defaulting to 1 and unused routes to 0.
- The owner approved physical DEV installation and testing, then clarified that the earlier confirmation requirement applied only to this implementation discussion. Do not keep asking for routine implementation or testing confirmations. Preserve release applications, submitted artifacts and main.

## Owner direction — 1.3.1 release, 2026-09-09

- The owner requested releasing the current changes as 1.3.1, with experimental features, ADVANCED MODE and audio defaulting to OFF. Publish the verified GitHub/Obtainium APK as 1.3.1 / code 12 with the existing distribution key. Retain saved choices and earlier artifacts/tags. This does not replace the pending Play 1.3.0 submission or the F-Droid submission.

## Owner direction — automatic Play uploads, 2026-09-09

- The owner requested free automatic AAB uploads whenever a GitHub release is published. Configure GitHub Actions and the existing Play app for signed Alpha drafts, using the existing Play upload key. This authorizes the required service account, scoped permissions, GitHub environment and integration into main, including the already published 1.3.1 source.
- Automatic uploads must preserve ongoing reviews, existing releases, tester/country settings and signing identities. Do not automatically submit for review or publish to testers/production. Fail if another draft or review prevents the upload; never cancel review as a fallback.

## Owner direction — 1.4.0 release, 2026-09-09

- The owner requested publishing the adaptive capture/audio update as 1.4.0. Integrate the verified source into main and publish the signed GitHub/Obtainium APK as 1.4.0 / code 13 using the existing distribution key. Preserve existing releases and store submissions. The configured Play automation may attempt an Alpha draft upload; never remove another draft or cancel review to force it through.

## Owner direction — 1.5.0 release, 2026-09-09

- The owner requested auditing/fixing SEED and RESEED, organizing documentation, and releasing the completed TAP/LIVE/MEDIA/DISPLAY work as 1.5.0. Integrate verified source into main and publish the signed GitHub/Obtainium APK as 1.5.0 / code 14 with the existing distribution key. Preserve earlier artifacts, signing identities and store submissions. The configured Play automation may upload an Alpha draft; never cancel review or delete another draft to force it through.

## Owner direction — 1.5.1 release, 2026-09-09

- The owner requested stopping and saving all recording on leaving the app, removing background recording services and unnecessary permissions, and releasing 1.5.1 / code 15. Publish the verified GitHub/Obtainium APK with the existing key; configured Play automation may upload an Alpha draft. Preserve earlier releases and reviews. Organize public documentation for first-time readers, separating current behavior from dated evidence.

## Owner direction — 1.6.0 release, 2026-09-10

- The owner requested releasing the verified adaptive window, display polish and compact capture/guide changes as 1.6.0 / code 16. Integrate the source into main and publish the signed GitHub/Obtainium APK with the existing distribution key. Configured Play automation may upload an Alpha draft; preserve existing reviews, releases and signing identities.

## Owner direction — GitHub Pages with every release, 2026-09-10

- Every app release must include updating the existing GitHub Pages site on `codex/privacy-pages` to current behavior. Review version labels, feature and operation descriptions, guide links, screenshot captions, and all privacy languages against the released source. Publish and verify the public site as part of release completion. Keep the recruitment purpose and distinguish GitHub availability from actual Play publication. This is standing authorization for routine Pages updates accompanying a requested release.

## Owner direction — LIGHT mode and GPU efficiency, 2026-09-10

- The owner requested detailed per-frame CFA/DEMOSAIC GPU measurements and output-preserving calculation/transfer improvements, then authorized view-sized preview processing in a separate LIGHT MODE. LIGHT starts OFF and is mutually exclusive with ADVANCED and EXPERT; merely turning those two off does not enable LIGHT. Preserve saved FAULT values, selected photo/video dimensions, and ADVANCED's exact displayed-frame capture.
- Work on `codex/gpu-detail-investigation`, using DEV builds and output comparisons. Apply verified non-destructive efficiencies across modes where their conditions hold. Keep unstable shader candidates test-only. This is development authorization, not a new release or store/Pages publication request.

## Owner direction — GPU defaults and TAP controls, 2026-09-10

- On `codex/gpu-adaptive-defaults`, use GPU identity, measured performance and physical preview size to recommend initial output resolution and preview fps. A fresh setup starts LIGHT when Android marks low RAM, RAM is unknown or below 5 GiB, or available CPU cores are at most four. This supersedes the earlier unconditional LIGHT-OFF default; preserve existing choices.
- Keep the shutter centered in TAP, place save-format selection outside the centered capture-mode group, and play imported video by default on recording start and pause on recording stop, honoring explicit transport choices. Continue DEV testing; no release, main integration or publication is requested.

## Owner direction — 1.6.1 release, 2026-09-10

- The owner requests reduced additional verification, then publication of completed work as 1.6.1 / code 17. Integrate into main, publish the signed GitHub/Obtainium APK with the existing key and update Pages. Preserve previous artifacts and pending reviews; automatic Play draft upload may run.

## Owner direction — saved signal reuse, 2026-09-10

- The owner requested showing the current saved photo’s signal-chain metadata and applying its settings from the saved-media viewer. Scope changes to that viewer, its reader/resources and verification; preserve capture output and metadata-writing behavior. Work on `codex/saved-signal-settings`, using DEV checks. This request does not authorize another release.

## Owner direction — 1.6.2 release, 2026-09-10

- Publish saved-photo signal reuse and viewer styling as 1.6.2 / code 18 with minimal additional checks. Integrate into main, sign with the existing distribution key, publish GitHub/Obtainium artifacts and update Pages. Preserve existing releases and store reviews.

## Owner direction — TAP provider, end and source audio fixes, 2026-09-10

- On `codex/tap-media-audio-fixes`, preserve/show the selected chain across image/video TAP imports, allow Google Photos-compatible media providers, stop/save recording at source-video completion, and retain source audio. Resolution-linked audio ON converts source audio to the output-resolution quality tier; OFF preserves supported compressed source audio. Source audio takes precedence over microphone recording. Keep the READOUT boundary and preserve originals. DEV testing is authorized; no new release requested.

## Owner direction — video signal metadata, 2026-09-10

- Extend MP4 metadata inspection and Use these settings to the immutable recording-start signal state, like photos. Do not record timed changes. Include camera/TAP, audio finalization and automatic segments. Continue DEV verification on `codex/tap-media-audio-fixes`; no new release requested.

## Owner direction — push completed work, 2026-09-10

- After completing and checking each task, push its commits on the working branch to origin. This is standing authorization for routine branch pushes, including the completed TAP fixes and video metadata work. Main integration, release publication and store submissions still follow their separately authorized scope. Do not force-push or overwrite unrelated remote work.

## Owner direction — multiple camera sensors, 2026-09-10

- Add explicit selection of exposed wide/ultra-wide/telephoto/front/external cameras and physical sensors, beyond the existing zoom toggle. Work on `codex/multi-camera-selection`, preserve fault settings and capture lifecycle, and use matching physical characteristics/results for sensor-specific output. Verify with the connected DEV device and push completed work. No release requested.

- The owner then prohibited digital zoom and requested optical-only adjustment where possible. Remove digital crop/zoom controls and their saved-state restoration; keep the native field of view. Prefer explicit physical sensors when exposed. Only use controllable advertised optical focal lengths, without digitally filling intermediate ratios; fixed-focal-length devices cannot provide continuous optical zoom.

## Owner direction — 1.7.0 release, 2026-09-10

- Release all completed TAP, video metadata and optical-only multi-camera changes as 1.7.0 / code 19. Integrate into main, publish the verified APK with the existing distribution key, and update the existing GitHub Pages site. Preserve prior releases and store reviews; the configured Play automation may upload an Alpha draft.

## Owner direction — camera intents, 2026-09-10

- Implement ordinary camera launch and external photo/video result contracts on `codex/camera-intents`. External capture needs JPEG/MP4 only, not RAW. Preserve normal RAW settings, published releases and main; push completed checked work. No new release is requested.
- The owner prohibits computer use for the rest of this session until explicitly changed. Use source inspection, CLI builds and automated tests; do not use native/browser UI control, screenshots or accessibility inspection.

## Owner direction — Network display clarity, 2026-09-12

- Improve Network display with separate freeze interval/duration controls and source-frame cadence/resolution degradation, replacing its synthetic noise overlay. Work on `codex/network-display-clarity`, preserve CRT/LED settings, and push verified work. No new release requested. The computer-use prohibition remains in force; CLI builds and offscreen synthetic rendering tests are permitted.

## Owner direction — time-model audit, 2026-09-12

- Inventory each model’s internal parameters and assess consistency with the time model on `codex/time-model-audit`. Deliver source-based findings, reproducible characterization and a proposed implementation order. This request is assessment, not authorization to redesign production timing or release another build. Keep the installed Pixel build and the computer-use prohibition in place; push completed checked audit work.

## Owner direction — time-model consistency, 2026-09-12

- The owner approved proceeding with the audit’s implementation. Work on `codex/time-model-consistency`: resolve active models before compilation, separate delivery/content clocks, expose effective values and active parameters, unify incident/noise timing and seed rules, and clarify Exposure/LED generators. This supersedes the assessment-only restriction above. Keep richer temporal afterglow/multi-frame blur as separate future work. Preserve old golden evidence, saved-key readability and release artifacts; no new release is requested. Continue CLI/offscreen verification under the computer-use ban and push checked work.

## Owner direction — 1.7.1 release, 2026-09-12

- The owner requests integrating all completed changes into main, installing on the connected device, and releasing 1.7.1 / code 20. Update the installed DEV app without replacing the release app, publish the verified APK with the existing distribution key, and update GitHub Pages. Preserve older releases and store reviews; configured Play automation may upload an Alpha draft. The computer-use ban remains in effect.

## Owner direction — camera and chain controls, 2026-09-12

- On `codex/camera-chain-controls`, make the lower-right icon switch front/rear and the viewfinder button choose lenses on that side or advertised optical focal lengths. Keep digital zoom prohibited. Show TAP controls only with Experimental enabled; suppress confirmation vibration during audio-enabled recording. Refresh chain selection, adjustment and inspection for readability, with separate selected/applied counts. Keep the computer-use ban; use CLI checks, preserve published artifacts and push checked work. No new release or device installation requested.

## Owner direction — PRO camera workspace, 2026-09-12

- Implement capability-aware real camera adjustments in a PRO shooting mode and reorganize the capture UI for later owner review on `codex/pro-camera-workspace`. Keep camera controls separate from FAULT and processing modes, preserve native optical framing and unsupported-device fallbacks. Stop the wireless-device investigation. Continue CLI and offscreen verification under the computer-use prohibition, provide a DEV review build, and push checked work. No new release is requested.

- USB review follow-up: fix empty PRO controls on the connected device, use PHOTO/VIDEO/TAP labels in all languages, move location outside the preview, restore audio toggles, show focal-length choices directly, and standardize control typography and spacing. Verify camera requests offscreen and install the corrected DEV build on the connected USB device; preserve release app and the computer-use ban.

- The owner further requested a single horizontal top row and differentiated button heights throughout the app. Use shared primary/standard/toolbar/compact sizes, retaining the larger shutter; apply to capture, editors, viewer and help actions, then install the checked DEV update over USB.

- Screenshot review: reduce all action-height tiers to 48/40/32/28 dp, make capture modes text tabs and collapsed FAULT visually compact. In landscape move preview overlays out to the controls, hide the preview badge, and provide a collapsible controls column with capture/stop still available. The supplied screenshot may be inspected; no new computer-use or screenshot capture is authorized. Install the checked DEV update over USB.

- Restore the original 80 dp shutter. Landscape collapse must retain the same CaptureButton instance, size, colors and photo/video/stop behavior. Replace ambiguous glyph-only disclosures with consistent drawn chevrons, explicit open/close labels and accessible state for both the workspace and FAULT. Account for connected-device dimensions; install the checked DEV build.

## Owner direction — emulator visual review, 2026-09-12

- The owner explicitly permits computer use again. Create an emulator matching the connected device’s 1280×2772 resolution and effective 480 dpi, capture and inspect screenshots, improve UI with that evidence, and update Git resources. Move readiness/status text out of the preview to a compact top overview. Emulator screenshots and UI instrumentation are authorized; this supersedes the earlier computer-use ban. Keep release/store publication scope unchanged.

- The owner clarified that Open/Close words should not be visible. Convey disclosure through icon placement and typography while retaining accessibility labels and state.

- Chain review follow-up: refine chain card radii and live-preview action spacing, replace Details/Adjust labels with icons, verify on the matched emulator, update review resources and install the checked USB DEV build.
