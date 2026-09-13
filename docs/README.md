# 5igna1 guide

[Project](../README.md) · [日本語](README.ja.md)

Covers release 1.7.2 and subsequent UI fixes on main. Start with [your first photograph](GETTING_STARTED.md), look up [controls](USAGE.md), or enable [PRO camera settings](PRO_CAMERA.md).

## Use the app

- [Every glitch is an encounter](ABOUT.md) · [日本語](ABOUT.ja.md)
- [Installation and updates](INSTALLATION.md)
- [Your first photograph](GETTING_STARTED.md) · [日本語](GETTING_STARTED.ja.md)
- [Controls reference](USAGE.md) · [日本語](USAGE.ja.md)
- [PRO camera controls and workspace review](PRO_CAMERA.md) · [日本語](PRO_CAMERA.ja.md)
- [Creative starting points](RECIPES.md) · [日本語](RECIPES.ja.md)
- [Fault points and controls](EFFECTS.md) · [日本語](EFFECTS.ja.md)
- [LIVE FAULT](LIVE_FAULT.md) · [日本語](LIVE_FAULT.ja.md)
- [SEED and RESEED](SEEDS.md) · [日本語](SEEDS.ja.md)
- [Time and parameter contract](TIME_MODEL.md) · [日本語](TIME_MODEL.ja.md)
- [ADVANCED MODE](ADVANCED_MODE.md) · [日本語](ADVANCED_MODE.ja.md)
- [Experimental device response](EXPERIMENTAL_SIGNALS.md) · [日本語](EXPERIMENTAL_SIGNALS.ja.md)
- [Capture formats](FORMATS.md) · [日本語](FORMATS.ja.md)
- [Experimental RAW video](RAW_VIDEO.md) · [日本語](RAW_VIDEO.ja.md)
- [Recording, app visibility and permissions](RECORDING.md) · [日本語](RECORDING.ja.md)
- [Heat and workload](PERFORMANCE.md) · [日本語](PERFORMANCE.ja.md)
- [5igna1 Privacy Policy](PRIVACY.md) · [日本語](PRIVACY.ja.md)
- [Troubleshooting](TROUBLESHOOTING.md)

## Build and verify

- [Building 5igna1](building.md)
- [Architecture](ARCHITECTURE.md) · [日本語](ARCHITECTURE.ja.md)
- [RAW representation adapter](raw-pipeline.md) · [日本語](raw-pipeline.ja.md)
- [Development and device verification](DEVELOPMENT.md) · [日本語](DEVELOPMENT.ja.md)
- [UI verification](UI_REVIEW.md) · [日本語](UI_REVIEW.ja.md)
- [Validation and limits](VALIDATION.md) · [日本語](VALIDATION.ja.md)

## Maintain and distribute

- [Releasing 5igna1](RELEASING.md)
- [Automatic Play uploads](PLAY_AUTOMATION.md) · [日本語](PLAY_AUTOMATION.ja.md)
- [Distribution signing certificate](DISTRIBUTION_CERTIFICATE.md)
- [F-Droid readiness — submission awaiting merge](FDROID_READINESS.md)
- [Distribution status and owner checklist](LAUNCH_TASKS.md)
- [Google Play 1.1.0 closed-test update](PLAY_1_1_0.md)
- [Google Play 1.3.0 closed-test update](PLAY_1_3_0.md)

[Provenance and retained measurements](audit/README.md) · [F-Droid submission candidate](fdroid/com.bongorian.signa1.yml)

## Documentation maintenance

Update the relevant guide instead of adding another progress report. Link to the authoritative specification rather than repeating feature/version/limit lists. Store review output in `verification/`. Earlier work logs and images remain in [Git history](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/README.md). Preserve signing identities, permission/license inventories and test expectations. Run `python3 tools/check-docs.py` after changes.
