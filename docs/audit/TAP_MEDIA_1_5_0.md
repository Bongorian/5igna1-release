# TAP, LIVE and media/display development check

Historical evidence for 1.5.0. Its background-recording behavior was removed in 1.5.1; see the current [recording guide](../RECORDING.md).

Development branch: `codex/raw-chain-time-echo`. This work does not change release identities or store submissions.

- TIME ECHO exposes probability only; history retention, opportunities and burst lengths are bounded and randomized internally. Manual triggering remains available.
- LIVE uses separate Time, Inputs and TIME ECHO pages with persistent navigation and Apply/Back actions.
- Experimental TAP imports a user-selected image/video after READOUT and before DATA. The preview button controls source playback; the shutter captures JPEG or records MP4. RAW selections for the camera are retained.
- Ordinary background work pauses; only an explicitly started output recording continues through its foreground service. The recording notification offers Stop when notifications are permitted.
- Settings orders saved metadata, experimental features, then app information.
- LED sampling does not add black gaps between elements; module failures and refresh bands remain independent.
- MEDIA/DISPLAY profiles and resolution behavior are documented in [Effects](../EFFECTS.md) ([Japanese](../EFFECTS.ja.md)). Original VHS/CRT controls and migration outputs remain compatible.

Validation on 2026-09-09:

- 33 unit tests per variant passed, including the unchanged migration hashes, old settings migration, seeded network stalls and disabled-LIVE behavior.
- All unit-test variants, lint, F-Droid debug/release and Play debug builds passed; translated resources and placeholders passed in English, Japanese and Chinese.
- Physical DEV test: TAP boundary/orientation, image/video import, independent source play/pause and output recording, JPEG/MP4 saving, foreground-only preview, camera/TAP background recording and return.
- Physical GPU comparison: all 16 MEDIA/DISPLAY combinations, exact Digital thru pixels, VHS/DVD intermediate resolution, composite/component difference, upsampling choices, previous-frame network hold and resume.
- Physical UI check: eight model selections, advanced profile editors, metadata → experiments → app ordering; LIVE pages and representative transport/settings screenshots inspected.

Local verification captures are excluded from source control. The release app and its data were preserved. This connected-device check is not a reproduction of the separately reported Pixel 8a / 1.4.0 RAW crash.

- SEED audit: fixed stale values after RESEED, shared exact-integer editing, retained locks and draft transactions, active-stage filtering, inactive-model suppression and recording guard. Physical `action seed` passed in basic/advanced modes, including overflow rejection and signed 64-bit limits.

- Editor spacing uses an idempotent minimum 8 dp separation between neighboring control groups/buttons, horizontally and vertically. Numeric seed fields and RESEED are distinct targets.

## Publication verification

- [GitHub 1.5.0](https://github.com/Bongorian/5igna1-release/releases/tag/v1.5.0) published from `636c4ea44c07a11834b687b67e93893180b70256`, version code 14, with the unchanged distribution certificate. APK SHA-256: `d9f86f40829971d42f7950d4e7bda12f28fd63815d4d909ddd1dae68059d54e7`. Anonymous latest-release API, tag target and APK/checksum downloads matched the verified local artifacts.
- [Automatic Play upload](https://github.com/Bongorian/5igna1-release/actions/runs/34352867100) succeeded on 2026-09-09, uploading 1.5.0 / 14 as an Alpha draft. AAB SHA-256: `8633b052aab22bf080e9f213f2d99918dde840549eb7206e657a7dca27b0a15c`. Review submission and publication remain separate.
- [GitHub Pages](https://bongorian.github.io/5igna1-release/) updated for 1.5.0, preserving tester recruitment. [Deployment](https://github.com/Bongorian/5igna1-release/actions/runs/34353160509) succeeded; public landing/policy content matched source `9a86239`.
- The redundant tag-triggered draft-creation job was cancelled after the manually verified release was already public. No public assets were replaced.
