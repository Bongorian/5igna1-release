# Changelog

## Unreleased

- Handle standard photo/video camera launches and explicitly targeted external JPEG/MP4 capture requests.
- Review, retake or cancel before returning a capture; honor caller output URIs and video limits while preserving normal RAW settings.

## [1.7.0] - 2026-09-10

- Select exposed physical wide, ultra-wide, telephoto and front cameras; use sensor-specific resolution and RAW capabilities.
- Remove digital zoom. Offer only advertised optical focal lengths on compatible lenses; fixed-focal-length cameras use lens switching.
- Preserve TAP chain selections, support Google Photos-compatible media providers, and stop/save recording at source-video completion.
- Retain imported video audio; resolution-linked audio converts to the output quality tier, while OFF preserves supported compressed source audio.
- Store recording-start signal settings in MP4 and extend metadata inspection and Use these settings to videos. Later changes and LIVE timing are not replayed.

## [1.6.2] - 2026-09-10

- Show the displayed saved photo’s signal-chain metadata and reuse its recorded chain, LEVEL, seeds and parameters as current settings.
- Separate signal controls from media navigation and style metadata details with clear type hierarchy.
- Loop long signal-chain labels inside their button. Preserve original media and capture format/resolution.

## [1.6.1] - 2026-09-10

- Recommend photo/video resolution and preview fps using GPU identity, a cached short rendering measurement, screen size and existing memory/camera limits.
- Start fresh constrained devices in LIGHT MODE; preserve saved choices and selected output dimensions. LIGHT reduces preview processing while photos render at the chosen save size.
- Keep the TAP shutter centered. Start imported-video playback with recording by default and pause on stop; honor explicit transport choices.
- Place capture modes centrally and save-format selection outside.
- Include post-1.6.0 RAW compatibility, compact UI and verified GPU transfer improvements; preserve selected fault processing.

## [1.6.0] — 2026-09-10

Version name: 1.6.0. Version code: 16.

- Adapt capture and effect editors to rotation, narrow windows and resizable layouts; correct camera display rotation.
- Refine capture icons and button sizing, keep portrait LIVE on one line, and align landscape editors with the controls.
- Cycle JPG/RAW directly from the format button; allow MP4/RAW ZIP switching only when RAW video switching is enabled in Settings.
- Refresh all eight interactive guide pages in English, Japanese and Chinese, with isolated format practice and visible targets on small screens.
- Preserve the seed field and scroll position while reseeding.
- Refine NETWORK scanline noise and coherent square LED module faults.
- Add editable feedback drafts with optional device details, off by default.

## [1.5.1] — 2026-09-09

- Stop and save camera, TAP and RAW recording when the capture screen loses foreground activity, including Home, app switching and screen lock. Returning never restarts recording.
- Remove the recording foreground service, its camera/microphone/data-sync/media-processing permissions, wake lock and notification permission request.
- Preserve captures and settings; keep photo processing paused while hidden.
- Organize recording, permissions, privacy and current user guides around user actions; retain dated validation evidence separately.

## [1.5.0] — 2026-09-09

Version name: 1.5.0. Version code: 14.

- Add consistent horizontal and vertical spacing between editor buttons.
- Correct SEED/RESEED field refresh, expose exact seed editing in both editor modes, preserve draft Apply/Cancel behavior and fixed values, and restrict reseeding to active supported stages outside recording.
- Add experimental image/video TAP injection after READOUT, with separate source playback and output recording.
- Randomize TIME ECHO from one probability control and reorganize LIVE pages and experimental settings.
- Expand MEDIA to VHS, DVD, Digital thru and composite/component Analog thru; add optional media-resolution reduction.
- Expand DISPLAY to CRT, Digital thru upconversion, Network stalls/loss and LED faults without black element gaps.
- Pause ordinary background work; allow active recording to continue through a foreground service.
- Preserve the selected chain when switching JPEG/RAW and organize current guides, seed behavior and dated validation evidence.

## [1.4.0] — 2026-09-09

Version name: 1.4.0. Version code: 13.

- Revise recommended photo/video sizes using RAM, available CPU cores and supported camera outputs. Preserve manual selections.
- Reuse one processed output with ADVANCED OFF, retaining all FAULT processing and immutable capture state. ADVANCED ON keeps precise displayed-frame capture.
- Add optional resolution-linked mono AAC audio for ordinary video, disabled by default. Device encoder limits can adjust actual audio parameters.
- Organize current guides, development procedures and dated validation records.
- Verify 100 JVM test executions, normal/strict capture and short encoded-audio clips on the emulator. Sustained physical-device heat and recording performance remain unmeasured.

## [1.3.1] — 2026-09-09

Version name: 1.3.1. Version code: 12.

- Add an interactive first-launch guide with practice controls and paused camera processing.
- Save Settings changes immediately and restore adaptive protection when choosing recommended settings.
- Add experimental per-stage device input sensitivity, motion blur, thermal noise and smear for RGB and processed RAW.
- Precompute RAW blur/smear sampling positions to reduce processing work. High-resolution processed RAW can still take several seconds.
- Default experimental features, ADVANCED MODE and video audio to OFF; LIVE audio input also defaults to OFF. Existing saved choices are retained.
- Verify physical-device JPEG, processed DNG and 30fps MP4, plus 22 behavior tests. Long-duration thermal behavior remains unverified.

## [1.3.0] — 2026-09-09

Version name: 1.3.0. Version code: 11.

- Rewrite the application and behavior checks in Kotlin, retaining the 1.2.0 tutorial.
- Optimize all six RAW faults with exact region caching and verified unchanged output.
- Reuse RAW chain buffers and GPU intermediate textures, and remove an extra DNG-save buffer copy.
- Verify RAW and GPU output equivalence and physical-device JPEG/DNG capture. GPU changes primarily reduce memory; measured offscreen GPU completion time did not improve.

## [1.2.0] — 2026-09-09

Version name: 1.2.0. Version code: 10.

- Add an offline five-page first-launch tutorial with Settings replay, skip/back navigation, activity-state restoration and English/Japanese/Simplified Chinese text.
- Includes the 1.1.0 fault-system redesign.

## [1.1.0] — 2026-09-09

Version name: 1.1.0. Version code: 9. GitHub release from main; the matching Play Alpha update was submitted on 2026-09-09.

- Rebuild the signal path around 13 causal faults, stable identities, named controls, and optional LIVE device inputs. The old 1.0.0 effect IDs/settings are reset during migration; saved captures remain intact.
- Add random chains and controls, a two-column fault catalog, and ADVANCED parameter editing with AUTO/fixed values.
- Preserve the selected chain across format changes; RAW applies its six supported faults.
- Save JPEG from the displayed signal, with conservative device recommendations, adaptive preview workload, and an optional EXPERT mode.
- Refine the capture layout, top light/GPS/audio controls, anchored format menu, and FAULT STATE meters.
- Browse photos and videos together inside the app, with photo zoom, video playback/seek, sideways browsing and downward dismissal. Improve camera recovery and viewer lifecycle cleanup.
- Store new photos and MP4 videos in DCIM/5igna1; existing files stay in place. RAW ZIP remains in Download/5igna1.
- Update English/Japanese guides, all three in-app privacy translations, and store preparation documents.

## [1.0.0] — 2026-09-08

The first public release of 5igna1.

- CLEAN and 16 imaging-fault effects, with a fixed-order chain and per-stage adjustments.
- LIVE FAULT for automatic changes over time.
- Processed JPEG, AVC/HEVC MP4, and original/processed DNG on supported cameras.
- Experimental, silent RAW video as DNG sequences in ZIP files.
- Japanese, English, and Simplified Chinese UI.
- Shared Play/F-Droid core features, with a signed FOSS APK for GitHub and Obtainium.
- On-device processing, optional GPS and audio, and no ads or tracking SDKs.

Version name: 1.0.0. Version code: 8. Earlier numbers were internal development versions, not releases from this repository.

[Release notes and downloads](https://github.com/Bongorian/5igna1-release/releases/tag/v1.0.0)
