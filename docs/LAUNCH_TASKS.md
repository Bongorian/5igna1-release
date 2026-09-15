# Distribution status and owner checklist

Last verified: **2026-09-15 (Japan time)**. This is the single current distribution-status record. Release procedures explain how to publish; dated reports preserve evidence and are not current status.

| Stage | Verified state | Evidence / next step |
|---|---|---|
| GitHub / Obtainium | 1.7.4 / code 23 published; signed APK and checksum publicly verified | [Release](https://github.com/Bongorian/5igna1-release/releases/tag/v1.7.4) |
| Current source | 1.7.4 / code 23: stale-frame crash fix and transition checks | Published as immutable v1.7.4; DEV installed |
| Google Play AAB upload | 1.7.4 / code 23 uploaded to Alpha as a draft | [Successful upload](https://github.com/Bongorian/5igna1-release/actions/runs/34929014377); SHA-256 `8db8e471daaa4827d910c5d6c0f4203ee6e4f7446df9ff2c77aa48ea4fd3f254` |
| Google Play review submission | Current submission status not verified | Check Play Console; upload success does not prove submission |
| Google Play tester delivery | Current served version not verified; historical confirmation is 1.3.0 / code 11 on September 9 | [Historical record](PLAY_1_3_0.md); do not treat this as the current version |
| F-Droid | Owner-reported 1.0.0 / code 8 submission; current review/merge status not rechecked | [Candidate audit](FDROID_READINESS.md) |
| Public website | 1.7.4 content and all three privacy languages publicly verified | [Website](https://bongorian.github.io/5igna1-release/) |

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
