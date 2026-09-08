# Google Play 1.1.0 closed-test update

Submitted on 2026-09-09 (Japan time). Application: `com.bongorian.signa1`. Version: **1.1.0 / code 9**. App source: `f4c3e0c09cd3e0ba42350bfeb3f229f1c5b2a7cd`.

## Scope and state

The existing **closed-test Alpha** track was updated. Console confirmed 1.0.0 / code 8 already published to testers. Tester groups, country selections, the internal-test track and managed publishing were not changed. Production access is not yet enabled.

The Console accepted submission of five changes: the Alpha release, Japanese description/screenshots, and English description/screenshots. They appeared under **In review**, with the automatic pre-review quick check still running. Checks then feed Google review. **Managed publishing is on:** after approval, a separate Publish changes action is required before testers receive 1.1.0. This record does not claim approval or delivery.

## Artifact validation

- Signed Play AAB built successfully with the existing upload key; its signing certificate matched the submitted 1.0.0 AAB. No keys were regenerated or changed.
- AAB SHA-256: `1c8ae8e9452fa1b4eb9a80a075ef526c73e37abd18b734a97636a95668e9ebce`.
- Google Play accepted code 9, version 1.1.0, minimum API 31 and target API 36. No previously supported devices became unsupported.
- One informational warning requested a deobfuscation file. This build has `minifyEnabled false`, so no R8/ProGuard mapping is produced.
- Ubuntu and macOS CI passed for the app source. Local tests, lint, both distribution builds and dependency checks also passed; see [Validation](VALIDATION.md).

## Store materials

Japanese/English descriptions reflect 13 causal faults, random chains, ADVANCED, workload recommendations/EXPERT and the mixed photo/video viewer. Each language has four actual 1.1.0 UI screenshots, captured on the API 37 emulator using the AOSP test scene. All eight images were visually checked. Feature artwork alone was declared AI-generated in Console; screenshot pixels were not synthesized or edited.

The existing [Japanese privacy page](https://bongorian.github.io/5igna1-release/) and its English/Chinese editions were updated for optional LIVE inputs, independent workload measurements, DCIM storage and internal media preview. All three live HTML bodies were verified against the bundled policy copies.

Original code-8 artifacts, signing keys and v1.0.0 remain intact. F-Droid submission and GitHub release publication are outside this Play update.
