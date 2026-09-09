# Google Play 1.3.0 closed-test update

Submitted on 2026-09-09 (Japan time). Application: `com.bongorian.signa1`. Version: **1.3.0 / code 11**. App source: `08bdb71295ca1c073d07c1f9a5f4d1c250bb95ee`, whose source tree matches `v1.3.0`.

## Scope and state

Console confirmed **1.1.0 / code 9** was already published on the existing **closed-test Alpha** track. Version 1.2.0 / code 10 was skipped on Play. This update includes its tutorial and the Kotlin/RAW/buffer optimizations in 1.3.0.

The Console accepted one change: Alpha release **11 (1.3.0)**, with rollout set to 100% of the existing track audience. The release appears under **In review**, with the automatic pre-review quick check running. This records acceptance of the submission, not completed Google review or delivery to testers.

**Managed publishing remains on.** After approval, a separate Publish changes action is required before testers receive 1.3.0. Existing tester settings, the two selected countries/regions, the internal-test track, production and store listing materials were not changed.

## Artifact validation

- Signed Play AAB built with the existing upload key; its certificate matches the previously submitted 1.1.0 AAB.
- AAB SHA-256: `d02582e9166ed6b5bb0da1238e5d78c46395d3c018653b56eec0f1c08f8e1a15`.
- Upload certificate SHA-256: `d7f9c8f29340fe4701f826c64f64beebd68e1b4ccb77af096a229d4cb3f12524`.
- JAR signature verification and bundletool validation passed. Manifest checks confirmed the application ID, version 1.3.0, code 11 and non-debuggable release.
- Google Play accepted minimum API 31 and target API 36. No previously supported devices became unsupported.
- One informational warning requests a deobfuscation file. This build has `minifyEnabled false`, so no R8/ProGuard mapping is produced.
- The app source already passed Ubuntu/macOS CI, local release checks and physical-device validation. See [Validation](VALIDATION.md) and [FAULT/buffer verification](audit/FAULT_RENDER_AND_BUFFERS.md).

Existing code-8/code-9 Play artifacts, signing keys and published GitHub tags remain intact.

## Submitted release notes

### Japanese

初回起動時のチュートリアルを追加しました。設定からいつでも再表示できます。
画像の処理結果を保ちながら、RAW全6種類のFAULT処理を高速化しました。
画像処理とDNG保存時のメモリ使用量を削減しました。

### English

Added a first-launch tutorial, available again anytime from Settings.
Sped up all six RAW faults while preserving image-processing results.
Reduced memory use in image processing and DNG saving.
