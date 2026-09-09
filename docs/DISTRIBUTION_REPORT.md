# Initial 1.0.0 launch record

5igna1 1.0.0 / versionCode 8 starts a new public source history in **Bongorian/5igna1-release**. The previous development repository stays private. Old Toren1BD screenshots and their history are not imported.

## What is prepared

- Shared Play and F-Droid flavors, with all core behavior in src/main.
- A signed FOSS APK and SHA-256 for GitHub Releases and Obtainium.
- A dedicated long-term distribution key, separate from the Play upload key. Both keys and recovery settings have verified local backups under Documents.
- English-first README and project statement describing the creative intent, actual fault models, RGB/RAW distinctions, and live camera workflow.
- Installation, first-use, recipe, format, controls, and troubleshooting guides; linked Japanese material.
- Apache-2.0, third-party notices, asset provenance, reviewed dependency inventory, CI, and F-Droid metadata.

The app's processing code and packaged assets are unchanged by this public documentation pass. The new source snapshot retains the same application ID, version, and signing identity for distribution.

## Verification

See [tested scope](VALIDATION.md) for build, behavior, device, and RAW limits. Repository guards check tracked credentials/blobs and the wrapper; dependency and release checks compare reviewed metadata. Public download verification is recorded in the [launch checklist](LAUNCH_TASKS.md).

## Follow-up at the time of launch

[Launch tasks](LAUNCH_TASKS.md) is the current checklist. GitHub distribution comes first. This report records the initial 1.0.0 publication. The owner subsequently reported Play closed-test review and F-Droid merge pending on 2026-09-08; see the launch checklist for current status. Optional CI signing Secrets and an offline copy of the key backup remain separate work.
