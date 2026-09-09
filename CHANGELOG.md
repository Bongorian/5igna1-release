# Changelog

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
