# Compact capture controls — 2026-09-10

Scope update — 2026-09-10: this development change shipped in [1.6.0](../../CHANGELOG.md). Original test conditions below remain historical. See the [audit inventory](README.md) for the separate unreleased Pixel 9 fix.

Development work on `codex/adaptive-window-modes`, after 1.5.1. This is not a release record.

## Behavior

- Capture mode icons are 20 dp, with 44 dp visible button backgrounds inside 48 dp touch rows. The primary shutter remains 80 dp. Capture format and LIVE controls use the same row height; portrait LIVE uses one line. Available width and font scaling determine the label fit.
- Tapping the format control cycles JPG/RAW on supported cameras. Video cycles MP4/RAW ZIP only after enabling RAW video switching in Settings. Enabling the setting does not select RAW automatically; disabling it returns RAW video to MP4. Existing saved RAW video selections migrate with the option enabled. TAP keeps its JPG/MP4 export controls.
- Landscape chain sheets align with the actual controls and safe screen edges. Effect catalog labels can grow vertically when text is enlarged.
- All eight guide pages were updated in English, Japanese and Chinese. The format page has isolated JPG/RAW practice. Guide cards avoid their highlighted control, and each page scrolls its underlying target into view when necessary.

## Verification

- All four flavor/build-type unit-test configurations, F-Droid and Play debug assembly, and F-Droid debug lint.
- Physical device: format cycling, RAW video opt-in persistence/opt-out, recording guard, TAP exports and legacy migration.
- Physical device and phone/tablet emulators: portrait and both landscape directions, control geometry, editor edges and guide target placement. A 320 dp-wide phone with 1.3 font scale covers narrow-screen behavior.
- Tablet: first launch, page restoration, skip/back/completion, immediate settings persistence, all three guide languages and camera recovery.
- Locale placeholders and documentation links checked separately. These checks establish the tested configurations, not all Android devices or accessibility settings.
