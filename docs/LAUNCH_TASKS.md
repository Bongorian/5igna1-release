# Distribution status and owner checklist

Last updated: **2026-10-06 (Japan time)**. Dates in individual rows identify their evidence. This is the single current distribution-status record. Release procedures explain how to publish; dated reports preserve evidence and are not current status.

| Stage | Verified state | Evidence / next step |
|---|---|---|
| GitHub / Obtainium | 1.7.5 / code 24 published; signed APK and checksum publicly verified | [Release](https://github.com/Bongorian/5igna1-release/releases/tag/v1.7.5); APK SHA-256 `c99133bdf34fc002df5dced38edbc0e089dd372126b8166639aede1c9c8ac2cb` |
| Current source | 1.7.5 / code 24: photo settings exchange, optional diagnostics, video haptics and viewer/tutorial refinements | Integrated into main; immutable v1.7.5 at `73faba9`; DEV installed |
| Google Play AAB upload | 1.7.5 / code 24 uploaded to Alpha as a draft | [Successful upload](https://github.com/Bongorian/5igna1-release/actions/runs/35462910259); SHA-256 `eacf07410b3325e3dadcfdacbf0697d92a57a96d849f88c3256af6f0f7a30bd6` |
| Google Play production | Public Japanese listing with purchase button and 1.7.5 release notes verified on October 3 | [Google Play](https://play.google.com/store/apps/details?id=com.bongorian.signa1); selected regions: Japan, Taiwan, United States and Canada |
| Google Play tester delivery | Current served version not verified; historical confirmation is 1.3.0 / code 11 on September 9 | [Historical record](PLAY_1_3_0.md); do not treat this as the current version |
| F-Droid | MR updated to 1.7.5 / code 24 on October 6; all 9 CI jobs passed and review replied; maintainer testing/merge pending | [Review and validation](FDROID_READINESS.md); [MR !48124](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48124) |
| Public website | Product landing and Google Play links deployed and verified October 3; historical screenshot captions retained | [Website](https://bongorian.github.io/5igna1-release/); [deployment](https://github.com/Bongorian/5igna1-release/actions/runs/37120576487) |

For each release, update the source, GitHub publication and website rows after verifying them. Update the Play upload row only from a successful upload result. Review and tester-delivery rows require separate Console evidence. Record the checked date and link; use “not verified” when no current evidence exists.

## Remaining owner work

- [x] Complete initial Play production publication (public listing verified October 3).
- [x] Update the F-Droid candidate and respond to review with successful server-side build/scanner results (October 6).
- [ ] Await F-Droid maintainer testing, merge and public delivery.
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
