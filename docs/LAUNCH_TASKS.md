# Distribution status and owner checklist

Last documented check: **2026-09-09 (Japan time)**. This is a dated maintainer status, not a live store monitor. [Release procedure](RELEASING.md) · [Documentation map](DOCUMENTATION.md)

| Channel | Last verified state | Record |
|---|---|---|
| GitHub / Obtainium | Signed stable APK releases; earlier tags retained | [Latest GitHub release](https://github.com/Bongorian/5igna1-release/releases/latest) |
| Google Play Alpha | 1.3.0 / code 11 published to selected testers | [1.3.0 record](PLAY_1_3_0.md) |
| Google Play Alpha draft | 1.4.0 / code 13 uploaded as an unpublished draft; review/publication require Console action | [Automatic upload records](PLAY_AUTOMATION.md) |
| F-Droid | Owner-reported 1.0.0 / code 8 submission awaiting merge; later release work did not replace it | [Candidate audit](FDROID_READINESS.md) |
| Current release source | 1.5.0 / code 14 source: TAP, MEDIA/DISPLAY, SEED and editor UI | [Guide scope](README.md) |

## Remaining owner work

- [ ] Complete Play testing/review/publication as appropriate; preserve tester and country settings.
- [ ] Respond to F-Droid review and complete the server-side build/merge process.
- [ ] Confirm an Obtainium import/install on a device.
- [ ] Keep a verified signing-key backup on separate offline media. The known Documents backup is on the same computer.
- [ ] Confirm signature compatibility before claiming cross-store in-place updates. The Play upload key is not the installed APK signing key.
- [ ] Allocate a new version/code and update release notes before releasing changed application code.

GitHub-triggered Play draft uploads are configured. This is separate from the optional GitHub APK signing workflow’s `release` environment. A local signed APK release does not require that optional workflow. Never place keys, passwords or recovery files in source, issues or release assets.

## Canonical links

- [Repository / Obtainium source](https://github.com/Bongorian/5igna1-release)
- [Latest APK release](https://github.com/Bongorian/5igna1-release/releases/latest)
- [Project website](https://bongorian.github.io/5igna1-release/)
- [Privacy policy](https://bongorian.github.io/5igna1-release/privacy/)
- [Distribution certificate](DISTRIBUTION_CERTIFICATE.md)

Do not invent public store listing links. Initial launch preparation and download checks are in [the 1.0.0 report](DISTRIBUTION_REPORT.md); earlier Play submission steps are in [1.1.0](PLAY_1_1_0.md) and [1.3.0](PLAY_1_3_0.md). Those dated records are retained as evidence, not as the current task list.
