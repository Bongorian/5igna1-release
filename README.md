# 5igna1

**Every glitch is an encounter.**

A camera for finding images in a broken signal. Shift the readout. Tear the color array. Let a fault arrive, and photograph the moment it makes.

[Download](https://github.com/Bongorian/5igna1-release/releases/latest) · [Your first photograph](docs/GETTING_STARTED.md) · [The idea behind 5igna1](docs/ABOUT.md) · [日本語](README.ja.md)

![5igna1 — Glitch Camera. Lime lettering and fragments of displaced light on black. Promotional artwork.](fastlane/metadata/android/en-US/images/featureGraphic.png)

[![Android build](https://github.com/Bongorian/5igna1-release/actions/workflows/android.yml/badge.svg)](https://github.com/Bongorian/5igna1-release/actions/workflows/android.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84.svg)

## One damaged system, fleeting moments

This branch contains the **unreleased FAULT redesign**, based on release 1.0.0 (`7dd83a7`). The download below remains the published release; its controls and screenshots differ from this branch.

The same damaged system keeps its character while readout drift, exposure phase and brief loss incidents keep moving. Choose the fault points, watch the camera signal, and capture an instant. Ordinary time progression never shuffles your route or regenerates its identity.

Every scene sample comes from the camera. There is no semantic scene generation or content completion. RAW, camera RGB, luma/chroma and media/display outputs are representations along a route; RAW is not the sole true image. Mechanisms are causal but their ranges serve glitch expression, not engineering simulation.

## Choose where the signal breaks

| FAULT POINT | Faults |
|---|---|
| SENSOR | PIXEL DAMAGE · EXPOSURE |
| READOUT | ROW ERROR |
| DATA | BIT ERROR · ADDRESS ERROR |
| CFA / RECONSTRUCTION | CFA ERROR · DEMOSAIC ERROR |
| COLOR | CHROMA ERROR · COLOR MAP |
| CODEC / STREAM | BLOCK ERROR · STREAM ERROR |
| MEDIA | VHS |
| DISPLAY | CRT |

CLEAN is an empty route. Each fault has a few named controls mapped to its own internal mechanism parameters. RESEED changes its identity while keeping the settings. LIVE couples motion, timing, heat, audio and processing pressure into meaningful fault states; intrinsic time evolution belongs to the faults themselves. VHS/CRT profiles are distinct from tracking, dropout, convergence and sync faults. STREAM ERROR is an explicit decoded-region loss/reuse model, not actual packet corruption.

[Fault models](docs/EFFECTS.md) · [LIVE inputs](docs/LIVE_FAULT.md) · [Release audit and migration design](docs/design/FAULT_SYSTEM.md)

## Photograph the displayed signal

JPEG pins the UI-acknowledged camera image at shutter time. A later camera frame or settings edit cannot replace it. Preview and ordinary video use the same canonical processed images. JPEG uses a supported live signal resolution, rather than a later full-resolution exposure. RAW photos use separate exposures and cannot be identical to the RGB viewfinder. [Formats and limits](docs/FORMATS.md).

1. Choose Photo and **JPEG · displayed signal**.
2. Select **ROW ERROR**. Adjust displacement and loss, then Apply. Watch the weak rows drift.
3. Press the shutter when the image interests you. Open the saved photograph from the thumbnail.

Add CHROMA ERROR or VHS in FAULT POINTS to explore another part of the signal path. [First photograph](docs/GETTING_STARTED.md) · [Creative starting points](docs/RECIPES.md).

All processing runs on your device. No ads, accounts, analytics or Internet permission. Japanese, English and Simplified Chinese are available, with the same core features in every distribution.

## Download and update

**Android 12 or later**, a Camera2-compatible camera, and OpenGL ES 2.0 are required. RAW, available resolutions, and frame rates depend on the device and camera.

### GitHub Releases / Obtainium

Get `5igna1-vX.Y.Z.apk` from the [latest release](https://github.com/Bongorian/5igna1-release/releases/latest). There is one APK; no architecture selection is needed. See the [installation guide](docs/INSTALLATION.md) if this is your first APK install.

To follow releases in Obtainium, add:

```text
https://github.com/Bongorian/5igna1-release
```

[Updates and signing](docs/INSTALLATION.md#updating) · [Changelog](CHANGELOG.md)

### Google Play

Developer registration is in progress. The official store link will appear here after publication.

### F-Droid

The FOSS build and submission metadata are prepared. The app is not yet listed in the official repository. [Submission status](docs/FDROID_READINESS.md)

## Keep what you make

| Output | Saved to |
|---|---|
| JPEG and DNG photos | `Pictures/5igna1` |
| MP4 video | `Movies/5igna1` |
| Experimental RAW video sequences, as ZIP | `Download/5igna1` |

GPS tagging starts off. Audio recording is optional. DNG needs a RAW developer, and its developed appearance can differ from the preview. Your gallery or OS may separately sync media if you enable those services.

[Choose a format](docs/FORMATS.md) · [Troubleshooting](docs/TROUBLESHOOTING.md) · [Privacy](docs/PRIVACY.md) · [All guides](docs/README.md)

## Build and contribute

5igna1 uses Camera2, OpenGL ES, and RAW16 processing. With JDK 17 and Android SDK 36 / Build Tools 35.0.0:

```sh
./tools/build.sh
```

[Building](docs/building.md) · [Architecture](docs/ARCHITECTURE.md) · [Contributing](CONTRIBUTING.md) · [Report an issue](https://github.com/Bongorian/5igna1-release/issues)

## License

5igna1 by **Bongorian**. Project code is licensed under [Apache License 2.0](LICENSE). See [NOTICE](NOTICE), [third-party licenses](THIRD_PARTY_LICENSES.md), and [asset provenance](docs/audit/ASSETS.md).
