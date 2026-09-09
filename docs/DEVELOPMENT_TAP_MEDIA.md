# TAP, LIVE and media/display development check

Development branch: `codex/raw-chain-time-echo`. This work does not change release identities or store submissions.

- TIME ECHO exposes probability only; history retention, opportunities and burst lengths are bounded and randomized internally. Manual triggering remains available.
- LIVE uses separate Time, Inputs and TIME ECHO pages with persistent navigation and Apply/Back actions.
- Experimental TAP imports a user-selected image/video after READOUT and before DATA. The preview button controls source playback; the shutter captures JPEG or records MP4. RAW selections for the camera are retained.
- Ordinary background work pauses; only an explicitly started output recording continues through its foreground service. The recording notification offers Stop when notifications are permitted.
- Settings orders saved metadata, experimental features, then app information.
- LED sampling does not add black gaps between elements; module failures and refresh bands remain independent.
- MEDIA/DISPLAY profiles and resolution behavior are documented in [Effects](EFFECTS.md) ([Japanese](EFFECTS.ja.md)). Original VHS/CRT controls and migration outputs remain compatible.

Validation on 2026-09-09:

- 30 unit tests per variant passed, including the unchanged migration hashes, old settings migration, seeded network stalls and disabled-LIVE behavior.
- All unit-test variants, lint, F-Droid debug/release and Play debug builds passed; translated resources and placeholders passed in English, Japanese and Chinese.
- Physical DEV test: TAP boundary/orientation, image/video import, independent source play/pause and output recording, JPEG/MP4 saving, foreground-only preview, camera/TAP background recording and return.
- Physical GPU comparison: all 16 MEDIA/DISPLAY combinations, exact Digital thru pixels, VHS/DVD intermediate resolution, composite/component difference, upsampling choices, previous-frame network hold and resume.
- Physical UI check: eight model selections, advanced profile editors, metadata → experiments → app ordering; LIVE pages and representative transport/settings screenshots inspected.

Local verification captures are excluded from source control. The release app and its data were preserved. This connected-device check is not a reproduction of the separately reported Pixel 8a / 1.4.0 RAW crash.
