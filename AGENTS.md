# Repository rules

- Keep UI, effects, RAW processing, export, settings, file I/O and all core features in `app/src/main`.
- Do not add proprietary SDKs to `main`. Google Play Billing, GMS and other Play-only APIs belong exclusively in `src/play` and `playImplementation`.
- AndroidX and other freely licensed Google-authored libraries are not Play Services; audit their licenses before adding them to shared code.
- Keep `assembleFdroidRelease` buildable from a clean checkout without credentials. The F-Droid flavor must remain FOSS, ad-free and tracking-free.
- Do not gate effects, RAW, resolution, export or other core functionality by distribution. Only distribution services and voluntary support entry points may differ.
- Keep `com.bongorian.signa1` as the release application ID. Discuss a required identity or signing migration before changing it. Debug uses `.debug`.
- Never commit release keys, passwords, credentials or local signing files. Do not reuse a Play upload key as a distribution signing key implicitly.
- Keep literal `versionName` / `versionCode` in `app/build.gradle` readable by F-Droid. Release tags are `v<versionName>`; increment `versionCode` for every release, across all channels.
- Do not add dependencies with unknown licenses. Update `THIRD_PARTY_LICENSES.md`, the dependency inventory and bundled notices when dependencies change.
- Record source, author/license and generation inputs for new assets. Do not assume an Internet image is redistributable.
- Do not change image-processing results during refactoring without behavior tests or output comparisons.
- `Effects` owns IDs and processing order, including generated GLSL definitions. Preserve RAW Bayer phase except for effects explicitly designed to alter it. RAW previews are approximations; do not promise identical rendered results.
- Preserve immutable capture snapshots, editor apply/cancel behavior, camera lifecycle cleanup and bounded RAW recording queues.
- Use Android string resources and the existing standard app-language API. Do not introduce a custom runtime translation layer.
- Run relevant checks before committing. Distribution changes require `lint`, `test`, `assembleFdroidDebug`, `assembleFdroidRelease`, `assemblePlayDebug`, `tools/check-locales.py` and release metadata checks.
- Keep public documentation and store descriptions consistent with the code. Do not invent store URLs, performance claims, preset functionality or supported hardware capabilities.
- Repository visibility, store submissions and publication of a draft release are release-owner actions; preparation does not authorize publishing them.

- Keep README and public user guides in English, with a linked Japanese edition. Treat README as the project’s public presentation: explain the creative intent, actual behavior, and first-use path without unverified claims. Preserve the distinction between fixed fault patterns, LIVE time variation, RGB approximations, and RAW sample processing.

## Owner direction — 2026-09-08, submissions pending

- Preserve the submitted 1.0.0 / versionCode 8 artifacts, existing upload and distribution signing keys, and the published v1.0.0 tag (6954138585d55af161d62196e77b941531780855). Never overwrite, regenerate, delete, or retarget them during development.
- The owner reports F-Droid submission awaiting merge and Google Play closed testing awaiting approval. Keep those submitted candidates stable; do not submit replacements as part of routine debugging.
- Owner authorized merging the completed redesign into main and advancing the version on 2026-09-08. Continue development in /Users/bongorian/Documents/ChatGPT/Apps/5igna1-fault-system; main now prepares 1.1.0 / versionCode 9. Use debug builds (com.bongorian.signa1.debug, 5igna1 DEV) for device testing; keep the release application and its data intact.
- Before the next store submission, allocate a higher versionCode and an appropriate new versionName; do not reuse published release identity for changed code.

## Owner direction — Play update, 2026-09-09

- The owner explicitly requested applying the current 1.1.0 source to Google Play and reports nine testers currently using 1.0.0. This authorizes a 1.1.0 / code 9 update to the existing closed-test Alpha track and the matching store materials. Preserve its tester group and country settings.
- Continue preserving the original 1.0.0 artifacts, v1.0.0 tag and signing keys. F-Droid remains on its existing submission; this Play update does not authorize changing that submission or publishing a GitHub release.


## Owner direction — tutorial development, 2026-09-09

- The owner requested first-launch and Settings-accessible tutorials on a development branch, with no physical device connected. Keep this work on `codex/onboarding-tutorial`; use the emulator and debug builds.
- This feature is not part of the signed Play 1.1.0 / code 9 submission. Preserve that bundle and main; allocate a higher versionCode before any future submission of changed application code.

## Owner direction — Kotlin rewrite, 2026-09-09

- The owner requested a complete, lean Kotlin rewrite. Continue on `codex/kotlin-cleanup`, based on the tutorial development branch. Application and test sources belong in Kotlin source sets; shaders stay GLSL and build/release utilities use their existing suitable languages.
- Preserve the submitted Play bundle, main, release tags, and signing material. Validate the development rewrite with debug builds and the emulator; this work does not authorize another store submission.
- Keep migration golden hashes fixed to the pre-rewrite Java output. Do not replace their expected values with Kotlin output to make a refactor pass.

## Owner direction — physical Kotlin debugging, 2026-09-09

- The owner explicitly authorized installing and debugging the Kotlin build on the connected physical device. Use `com.bongorian.signa1.debug` and preserve the installed release application. Short camera/RAW/video test captures are within this debugging scope; keep release submissions and main unchanged.

## Owner direction — GitHub releases, 2026-09-09

- The owner explicitly authorized publishing current main as 1.1.0 / code 9, the tutorial branch as 1.2.0 / code 10, and the optimized Kotlin branch as 1.3.0 / code 11. Publish verified GitHub APKs using the existing distribution key, with 1.3.0 as latest. This supersedes earlier development-only restrictions for these GitHub releases. Preserve existing tags, signing keys and submitted store artifacts; this request does not change store submissions.

## Owner direction — Play 1.3.0 update, 2026-09-09

- The owner explicitly requested distributing 1.3.0 on Play while skipping 1.2.0. The existing Alpha track accepted 1.3.0 / code 11 for review, using the existing Play upload key and unchanged tester/country settings. Managed publishing remains on; approval and the subsequent publication action are pending. See docs/PLAY_1_3_0.md.
- Preserve the submitted code-11 AAB and prior artifacts. Any changed application code in a future submission needs a higher versionCode.

## Owner direction — interactive guide and thermal audit, 2026-09-09

- The owner requested a development branch for an interactive guide, investigating/improving heat, and assessing image/noise/other-frame injection boundaries. Work on `codex/interactive-guide-thermal-inputs`; preserve main, published releases and the submitted code-11 artifacts. Injection is an assessment, not a requested feature implementation. Continue authorized debug-only physical-device testing.
- The owner subsequently requested immediate persistence in the Settings panel, replacing its Apply action. This supersedes settings draft/cancel semantics for `QualityDialog`; effect and LIVE editors retain their existing apply/cancel behavior.

## Owner direction — experimental device response, 2026-09-09

- The owner approved implementing per-stage motion/audio/frame-timing/temperature/app-CPU sensitivity and motion blur, thermal noise and smear on the current development branch. Use one Settings switch, default OFF; preserve selections and values while bypassing the additions when OFF. Inputs react with LIVE ON and respect global input controls and permissions. Sensitivity range is 0–4, with native routes defaulting to 1 and unused routes to 0.
- The owner approved physical DEV installation and testing, then clarified that the earlier confirmation requirement applied only to this implementation discussion. Do not keep asking for routine implementation or testing confirmations. Preserve release applications, submitted artifacts and main.

## Owner direction — 1.3.1 release, 2026-09-09

- The owner requested releasing the current changes as 1.3.1, with experimental features, ADVANCED MODE and audio defaulting to OFF. Publish the verified GitHub/Obtainium APK as 1.3.1 / code 12 with the existing distribution key. Retain saved choices and earlier artifacts/tags. This does not replace the pending Play 1.3.0 submission or the F-Droid submission.

## Owner direction — automatic Play uploads, 2026-09-09

- The owner requested free automatic AAB uploads whenever a GitHub release is published. Configure GitHub Actions and the existing Play app for signed Alpha drafts, using the existing Play upload key. This authorizes the required service account, scoped permissions, GitHub environment and integration into main, including the already published 1.3.1 source.
- Automatic uploads must preserve ongoing reviews, existing releases, tester/country settings and signing identities. Do not automatically submit for review or publish to testers/production. Fail if another draft or review prevents the upload; never cancel review as a fallback.
