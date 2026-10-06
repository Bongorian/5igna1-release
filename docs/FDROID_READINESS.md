# F-Droid readiness — submission review

## Current candidate — 2026-10-06

[MR !48124](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48124) now targets
**1.7.5 / versionCode 24**, pinned to the full immutable upstream commit
`73faba9ca51f6d00ac68385b16a18eca68deba6b`.
This responds to the [request to update before testing](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48124#note_3949320110).
The MR uses normal F-Droid server signing, not signature copying.

- Build recipe and CurrentVersion fields were updated together in
  [abef6948](https://gitlab.com/bongorian/fdroiddata/-/commit/abef6948fb90f348f8cba6e1df9795a284685aec).
- Local `fdroid lint com.bongorian.signa1` passed with fdroidserver 2.4.5 and
  fdroiddata's category definitions.
- [F-Droid CI](https://gitlab.com/bongorian/fdroiddata/-/pipelines/2916472861): all 9 jobs passed,
  including build/source scan and the binary scanner in `check apk` on the code-24 APK.
- `com.google.android.play:app-update:2.1.0` is a `playImplementation` dependency.
  `src/fdroid/.../DistributionUpdates.kt` is a no-op. The generated F-Droid APK passed the independent non-free class scan.
- Unreleased R8 optimization changes are not part of this candidate.

[Replied to the review](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/48124#note_3960262300)
with the pinned version and CI evidence. Maintainer testing, merge and public
F-Droid delivery remain pending. See [distribution status](LAUNCH_TASKS.md).

## Historical 1.0.0 audit — 2026-09-08

The evidence below describes the original **1.0.0 / versionCode 8** submission,
not the current candidate. Published artifacts, tags and signing identities remain
unchanged. Current automation is documented in [Play uploads](PLAY_AUTOMATION.md).

## Audit summary

| Area | Result |
|---|---|
| Shared core | UI, effects, RAW, and export remain in src/main. No flavor-only core features. |
| Runtime dependencies | AndroidX, Kotlin/coroutines, and Guava ListenableFuture; release graphs match across flavors. |
| License | Apache-2.0 project code; reviewed runtime declarations, including the Kotlin Boost exception. Required texts are bundled offline in the APK. |
| Proprietary services | No GMS, Billing, Firebase, AdMob, Analytics, or proprietary SDK in the APK. Automatic downloadable emoji-font initialization is disabled. |
| Binary blobs | No vendored app JAR/AAR/SO/DEX. The tracked Gradle wrapper matches its official checksum. |
| Network | No INTERNET permission. Camera, optional audio, and optional location are used. |
| Assets | Current icons, generated title art, and AOSP-pattern screenshots are recorded in the asset inventory. Old Toren1BD screenshots and their Git objects are not imported. |
| Credentials | Keys and local signing/recovery files remain ignored. The staged public snapshot is checked for known credential and binary patterns. |
| Metadata | Japanese/English fastlane descriptions, current UI screenshots, and candidate F-Droid metadata are present. |
| fdroidserver 2.4.5 | New-public-source candidate lint passed; a source-copy scan found fatal 0 / warnings 0. Full server build remains pending. |

## License classifications

- **No issue identified:** owner-confirmed source/logo, reviewed runtime dependencies, Kotlin Boost exception, Material settings icon, current AOSP-pattern screenshots.
- **Publisher review:** generated promotional artwork and final store presentation.
- **Non-FOSS:** no such SDK or asset identified in the APK.
- **Unknown:** none among the current bundled assets/code and resolved Maven dependencies. Unconfirmed old backgrounds were excluded from this public history rather than re-licensed.

The checks combine source review, resolved dependencies, POM declarations, embedded notices, asset records, and owner provenance confirmation. They are not a legal guarantee or a complete unknown-SDK detector.

[Dependency inventory](audit/dependencies.json) · [Third-party licenses](../THIRD_PARTY_LICENSES.md) · [Asset audit](audit/ASSETS.md) · [Metadata candidate](fdroid/com.bongorian.signa1.yml) · [Launch tasks](LAUNCH_TASKS.md)
