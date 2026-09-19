# Distribution status and owner checklist

Last verified: **2026-09-20 (Japan time)**. This is the single current distribution-status record. Release procedures explain how to publish; dated reports preserve evidence and are not current status.

| Stage | Verified state | Evidence / next step |
|---|---|---|
| GitHub / Obtainium | 1.7.5 / code 24 published; signed APK and checksum publicly verified | [Release](https://github.com/Bongorian/5igna1-release/releases/tag/v1.7.5); APK SHA-256 `c99133bdf34fc002df5dced38edbc0e089dd372126b8166639aede1c9c8ac2cb` |
| Current source | 1.7.5 / code 24: photo settings exchange, optional diagnostics, video haptics and viewer/tutorial refinements | Integrated into main; immutable v1.7.5 at `73faba9`; DEV installed |
| Google Play AAB upload | 1.7.5 / code 24 uploaded to Alpha as a draft | [Successful upload](https://github.com/Bongorian/5igna1-release/actions/runs/35462910259); SHA-256 `eacf07410b3325e3dadcfdacbf0697d92a57a96d849f88c3256af6f0f7a30bd6` |
| Google Play review submission | Current submission status not verified | Check Play Console; upload success does not prove submission |
| Google Play tester delivery | Current served version not verified; historical confirmation is 1.3.0 / code 11 on September 9 | [Historical record](PLAY_1_3_0.md); do not treat this as the current version |
| F-Droid | Owner-reported 1.0.0 / code 8 submission; current review/merge status not rechecked | [Candidate audit](FDROID_READINESS.md) |
| Public website | 1.7.5 content and all three privacy languages publicly verified | [Website](https://bongorian.github.io/5igna1-release/); [deployment](https://github.com/Bongorian/5igna1-release/actions/runs/35462912482) |

For each release, update the source, GitHub publication and website rows after verifying them. Update the Play upload row only from a successful upload result. Review and tester-delivery rows require separate Console evidence. Record the checked date and link; use “not verified” when no current evidence exists.

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

Do not invent public store listing links. Initial launch preparation and download checks are in [the 1.0.0 report](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/DISTRIBUTION_REPORT.md); earlier Play submission steps are in [1.1.0](PLAY_1_1_0.md) and [1.3.0](PLAY_1_3_0.md). Those dated records are retained as evidence, not as the current task list.
