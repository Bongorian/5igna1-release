# Documentation map and maintenance

[Guide index](README.md) · [日本語](DOCUMENTATION.ja.md)

Start with the user guides to learn the app, the implementation references to change it, or the dated audit records to inspect evidence. Current guides describe 1.6.0; documents at earlier release tags describe those versions.

## Current references

| Files (English / Japanese where available) | Role and source of truth |
|---|---|
| [README](README.md) / [日本語](README.ja.md) | Reader navigation and version scope; avoid duplicating detailed specifications |
| [ABOUT](ABOUT.md) / [日本語](ABOUT.ja.md) | Creative intent and limits of the image models |
| [INSTALLATION](INSTALLATION.md) | APK/Obtainium installation, update identity and checksum procedure |
| [GETTING_STARTED](GETTING_STARTED.md) / [日本語](GETTING_STARTED.ja.md) | First capture and the eight-step interactive guide |
| [Recording and permissions](RECORDING.md) / [日本語](RECORDING.ja.md) | Recording lifecycle, saving on exit and permission purposes |
| [SEED guide](SEEDS.md) / [日本語](SEEDS.ja.md) | Identity, event seeds, reseeding and reproducibility |
| [PRO camera workspace](PRO_CAMERA.md) / [日本語](PRO_CAMERA.ja.md) | Unreleased real camera controls, capability rules and layout inventory |
| [USAGE](USAGE.md) / [日本語](USAGE.ja.md) | Controls, immediate Settings, effect/LIVE transactions and media viewer |
| [RECIPES](RECIPES.md) / [日本語](RECIPES.ja.md) | Creative starting points; these are instructions, not built-in presets |
| [EFFECTS](EFFECTS.md) / [日本語](EFFECTS.ja.md) | Thirteen base fault models and causal order; experimental additions link out |
| [LIVE_FAULT](LIVE_FAULT.md) / [日本語](LIVE_FAULT.ja.md) | Time evolution, inputs, permissions and pause behavior |
| [ADVANCED_MODE](ADVANCED_MODE.md) / [日本語](ADVANCED_MODE.ja.md) | Detailed parameter catalog and displayed-frame capture mode |
| [EXPERIMENTAL_SIGNALS](EXPERIMENTAL_SIGNALS.md) / [日本語](EXPERIMENTAL_SIGNALS.ja.md) | Optional stage sensitivities and three image artifacts, with scoped evidence |
| [FORMATS](FORMATS.md) / [日本語](FORMATS.ja.md) | Authoritative JPEG/RAW/MP4 capture contract and resolution-linked audio tiers |
| [RAW_VIDEO](RAW_VIDEO.md) / [日本語](RAW_VIDEO.ja.md) | Silent original DNG sequences, capability checks, queues and dropped frames |
| [PERFORMANCE](PERFORMANCE.md) / [日本語](PERFORMANCE.ja.md) | Initial device budgets, ADVANCED/EXPERT differences and runtime load policy |
| [TROUBLESHOOTING](TROUBLESHOOTING.md) | Symptom-to-action reference and issue-report information |
| [PRIVACY](PRIVACY.md) / [日本語](PRIVACY.ja.md) | Data handling; keep aligned with bundled policies and local HTML copies |
| [ARCHITECTURE](ARCHITECTURE.md) / [日本語](ARCHITECTURE.ja.md) | Current ownership, lifecycle, state and capture architecture |
| [raw-pipeline](raw-pipeline.md) / [日本語](raw-pipeline.ja.md) | RAW representation adapter; avoid duplicating the full format guide |
| [building](building.md) | Toolchain, flavors, output paths and static checks |
| [DEVELOPMENT](DEVELOPMENT.md) / [日本語](DEVELOPMENT.ja.md) | How to run device tests; separate from results |
| [Audit inventory](audit/README.md) / [日本語](audit/README.ja.md) | Audit file roles, release membership, superseded descriptions and stocktake findings |
| [VALIDATION](VALIDATION.md) / [日本語](VALIDATION.ja.md) | Current evidence index and remaining limits; dated results live in audit |
| [RELEASING](RELEASING.md) | Repeatable signing, versioning and publication procedure |
| [LAUNCH_TASKS](LAUNCH_TASKS.md) | Dated channel status and remaining owner actions |
| [PLAY_AUTOMATION](PLAY_AUTOMATION.md) / [日本語](PLAY_AUTOMATION.ja.md) | Configured draft-upload workflow, credentials, guards and retries |
| [DISTRIBUTION_CERTIFICATE](DISTRIBUTION_CERTIFICATE.md) | Stable public signing identity; never private keys |
| [FDROID_READINESS](FDROID_READINESS.md), [candidate YAML](fdroid/com.bongorian.signa1.yml) | Submitted 1.0.0 candidate scope; do not silently repoint to current development |
| [Asset audit](audit/ASSETS.md), [asset hashes](audit/assets.json), [dependency inventory](audit/dependencies.json) | Provenance and reviewed inventory; refresh only against actual files/dependencies |

## Historical evidence and design

| Files | Retained purpose |
|---|---|
| [Pixel 9 DNG sizes](audit/PIXEL_9_DNG_SIZES.md) | Unreleased after 1.6.0; shared size filtering and physical DEV results |
| [Adaptive windows](audit/ADAPTIVE_WINDOW_FEEDBACK.md), [display polish](audit/UI_DISPLAY_POLISH.md), [compact controls](audit/COMPACT_CAPTURE_UI.md) | Development evidence for changes included in 1.6.0; final controls supersede intermediate UI descriptions |
| [DISTRIBUTION_REPORT](DISTRIBUTION_REPORT.md) | Initial 1.0.0 launch preparation |
| [PLAY_1_1_0](PLAY_1_1_0.md), [PLAY_1_3_0](PLAY_1_3_0.md) | Dated Play submissions and confirmed follow-up status |
| [1.5.1 lifecycle validation](audit/FOREGROUND_1_5_1.md) | Stop/save, released resources and packaged permissions |
| [1.5.0 validation](audit/TAP_MEDIA_1_5_0.md) | Dated TAP, media/display and seed checks; superseded background behavior |
| [Original validation](audit/VALIDATION_1_0_0.md) | 1.0.0 test scope |
| [Validation through 1.3.0](audit/VALIDATION_THROUGH_1_3_0.md) / [日本語](audit/VALIDATION_THROUGH_1_3_0.ja.md) | Original chronological redesign/tutorial/Kotlin logs, including status at each test date |
| [RAW optimization](audit/RAW_OPTIMIZATION.md) / [日本語](audit/RAW_OPTIMIZATION.ja.md), [results](audit/raw-optimization-results.json) | First RAW differential/benchmark pass |
| [Remaining RAW optimization](audit/RAW_REMAINING_OPTIMIZATION.md) / [日本語](audit/RAW_REMAINING_OPTIMIZATION.ja.md), [results](audit/raw-remaining-results.json) | Remaining RAW differential/benchmark pass |
| [GPU and buffers](audit/FAULT_RENDER_AND_BUFFERS.md) / [日本語](audit/FAULT_RENDER_AND_BUFFERS.ja.md), [results](audit/fault-render-and-buffers-results.json) | Renderer byte comparisons and scoped memory/CPU measurements |
| [Guide and thermal](audit/GUIDE_AND_THERMAL.md) / [日本語](audit/GUIDE_AND_THERMAL.ja.md), [results](audit/guide-thermal-results.json) | 1.3.1 guide suspension and heat investigation |
| [Experimental results](audit/experimental-signals-results.json) | 1.3.1 experimental GPU/RAW/video evidence |
| [Injection assessment](audit/DATA_INJECTION_POINTS.md) / [日本語](audit/DATA_INJECTION_POINTS.ja.md) | Historical feasibility study; the current TAP feature is described in Experimental features |
| [FAULT redesign](design/FAULT_SYSTEM.md) / [日本語](design/FAULT_SYSTEM.ja.md) | Original migration decisions from 1.0.0; superseded as a current architecture reference |
| [Adaptive capture and audio](audit/ADAPTIVE_CAPTURE_AUDIO.md) | 1.4.0 verification and limits |

## Maintenance rules

Update the relevant reference and its Japanese edition when behavior changes. Keep volatile counts in dated test records, versions in the guide scope/release records, and channel status in LAUNCH_TASKS. The application version is authoritative in `app/build.gradle`; a development tree sharing a release’s version string is still unreleased.

Keep historical measurements, hashes and submitted metadata intact. Label the source/date instead of rewriting old results as current coverage. When moving a document, repair inbound and outbound links. Run the local documentation link/coverage check, locale checks and relevant application tests before committing. Policy-copy changes require a separate site update when the corresponding app version is published; editing this application checkout does not deploy the site.

- [Load investigation](audit/LOAD_INVESTIGATION.md) / [日本語](audit/LOAD_INVESTIGATION.ja.md), [measurements](audit/load-investigation-results.json): post-1.6.0 investigation-only prototypes; not shipped changes.

- [GPU detail and LIGHT MODE](audit/GPU_DETAIL_LIGHT.md) / [日本語](audit/GPU_DETAIL_LIGHT.ja.md), [measurements](audit/gpu-detail-light-results.json): unreleased view-sized preview and common standalone LED optimization.

[1.6.1 release follow-up: GPU defaults and TAP verification](audit/GPU_DETAIL_LIGHT.md).

- [Camera integration](CAMERA_INTEGRATION.md) / [日本語](CAMERA_INTEGRATION.ja.md): unreleased camera intents, caller contracts and separate-UID verification.

- [Internal parameters and time-model review](design/TIME_MODEL_REVIEW.md) / [日本語](design/TIME_MODEL_REVIEW.ja.md), [generated catalog and evidence](audit/TIME_MODEL_EVIDENCE.md): 2026-09-12 audit of dd7f027; proposed clock/parameter contract, with no production behavior change.

- [Clock model 2](TIME_MODEL.md) / [日本語](TIME_MODEL.ja.md): unreleased clock ownership, shared incidents, stable seeds, active model parameters and verification scope.
