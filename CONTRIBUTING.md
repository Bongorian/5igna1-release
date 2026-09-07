# Contributing to 5igna1

Bug reports, translations, device checks, and thoughtful image-processing changes are welcome. Start with the [project statement](docs/ABOUT.md) and [repository rules](AGENTS.md).

## Build and check

Use JDK 17, Android SDK 36, and Build Tools 35.0.0. [Full build guide](docs/building.md)

```sh
./tools/build.sh lint lintPlayDebug test assembleFdroidDebug assembleFdroidRelease assemblePlayDebug
python3 tools/check-locales.py
python3 tools/release.py check
python3 tools/check-repository.py
```

## Report a problem

Use [Issues](https://github.com/Bongorian/5igna1-release/issues). Include app version and source, device model, Android version, camera, format, resolution/fps, effect chain, and reproduction steps. Explain the expected and actual result. Review attached media or logs for personal information and GPS metadata.

## Make a change

Branch from main and open a focused pull request. Codex branches use `codex/` by default. Describe the problem, resulting behavior, relevant checks, and device conditions that remain untested. For image-processing changes, include reproducible input, settings, and output comparisons.

Keep shared UI, processing, RAW, export, and other core functions in `src/main`. Play-only services belong in `src/play` and `playImplementation`. Keep the F-Droid flavor FOSS, ad-free, tracking-free, and buildable without credentials. Do not create core-feature differences between distributions.

Public-facing English documentation should describe current behavior clearly. Keep the Japanese guides linked and mark any translation that needs an update. Distinguish fixed patterns from time variation and RGB approximations from RAW processing. Promotional images must not be presented as captured samples.

## Dependencies, assets, and licensing

Review upstream licenses and transitive dependencies before adding a library. Update the [dependency inventory](docs/audit/dependencies.json), third-party licenses, and bundled notices. Do not add dependencies with unknown licenses.

Record each asset's author, source, license, and modifications in the [asset audit](docs/audit/ASSETS.md). For generated assets, record generation inputs, method, and applicable terms. Preserve third-party notices; do not assume that the project's license re-licenses someone else's material.

Contribute your original code under this project's Apache-2.0 license. Never commit signing keys, local signing files, passwords, or credentials.

## Release rules

1. Update the literal `versionName` (`X.Y.Z`) and increase `versionCode` beyond every distributed version, across all sources. The first public version is 1.0.0 / 8.
2. Add a section to `CHANGELOG.md` and Japanese/English fastlane changelogs for the code.
3. Verify both flavors, licenses, and signing continuity.
4. Tag the reviewed commit `vX.Y.Z`. Do not move a published tag or replace a published APK.
5. Sign the FOSS release, verify the certificate and APK identity, and attach the APK and SHA-256 to a release draft. Publish after checking it. Tag CI can prepare the draft once release Secrets are configured; a local signed release is also supported.
6. Follow the separate Play/F-Droid submission procedures and update actual listing URLs.

See [Releasing](docs/RELEASING.md) and the [launch checklist](docs/LAUNCH_TASKS.md).
