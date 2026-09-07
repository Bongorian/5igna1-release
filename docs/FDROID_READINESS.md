# F-Droid readiness — Almost Ready

Audit date: 2026-09-08. Candidate: **5igna1 1.0.0 / versionCode 8**.

The current source builds as FOSS. Candidate metadata lint and a scan of the new public source snapshot passed with zero source errors or warnings. This repository starts from the audited current source, with a new Git history and the old development screenshots excluded. Acceptance into F-Droid remains a separate process.

## Remaining work

1. Validate the public v1.0.0 tag with `fdroid build --test` in F-Droid's Linux environment, then submit and respond to review.
2. Decide whether ordinary F-Droid signing is sufficient. Cross-source updates are not guaranteed; reproducible builds/signature copying have not been validated.
3. Complete the publisher's final review of generated promotional artwork and device behavior.

GitHub distribution signing is configured locally and the key/recovery files are backed up. GitHub signing Secrets and Play's app-signing choice are still pending, but neither is required for a normal F-Droid server-signed build.

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
