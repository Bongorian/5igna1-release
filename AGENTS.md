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
