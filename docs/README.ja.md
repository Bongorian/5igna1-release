# 5igna1 ガイド

[プロジェクト](../README.ja.md) · [English](README.md)

公開版1.7.2と、その後のmainのUI修正を対象にします。初めて使う方は[初回撮影](GETTING_STARTED.ja.md)、操作の確認は[操作ガイド](USAGE.ja.md)、カメラ調整は[PRO撮影](PRO_CAMERA.ja.md)から始めてください。

## 使い方

- [一期一会のグリッチ](ABOUT.ja.md)
- [Installation and updates](INSTALLATION.md)
- [はじめての撮影](GETTING_STARTED.ja.md)
- [操作ガイド](USAGE.ja.md)
- [PRO撮影と撮影画面のレビュー](PRO_CAMERA.ja.md)
- [表現の出発点](RECIPES.ja.md)
- [FAULT POINTと操作値](EFFECTS.ja.md)
- [LIVE FAULT](LIVE_FAULT.ja.md)
- [SEEDとRESEED](SEEDS.ja.md)
- [時間と内部パラメータの共通ルール（1.7.1）](TIME_MODEL.ja.md)
- [ADVANCED MODE](ADVANCED_MODE.ja.md)
- [実験的な端末連動](EXPERIMENTAL_SIGNALS.ja.md)
- [保存形式](FORMATS.ja.md)
- [RAW動画 / DNG連番](RAW_VIDEO.ja.md)
- [録画・画面を離れたときの動作・権限](RECORDING.ja.md)
- [発熱・負荷対策](PERFORMANCE.ja.md)
- [5igna1 プライバシーポリシー](PRIVACY.ja.md)
- [Troubleshooting](TROUBLESHOOTING.md)

## 開発と検証

- [Building 5igna1](building.md)
- [内部設計](ARCHITECTURE.ja.md)
- [RAW表現のアダプター](raw-pipeline.ja.md)
- [開発・検証](DEVELOPMENT.ja.md)
- [UIの検証](UI_REVIEW.ja.md)
- [検証範囲と制限](VALIDATION.ja.md)

## 保守と配布

- [Releasing 5igna1](RELEASING.md)
- [Playへの自動アップロード](PLAY_AUTOMATION.ja.md)
- [Distribution signing certificate](DISTRIBUTION_CERTIFICATE.md)
- [F-Droid readiness — submission awaiting merge](FDROID_READINESS.md)
- [Distribution status and owner checklist](LAUNCH_TASKS.md)
- [Google Play 1.1.0 closed-test update](PLAY_1_1_0.md)
- [Google Play 1.3.0 closed-test update](PLAY_1_3_0.md)

[素材・依存台帳と比較用の測定データ](audit/README.ja.md) · [F-Droid提出候補](fdroid/com.bongorian.signa1.yml)

## 文書の保守

現行仕様は対応するガイドへ反映し、別の進捗報告を増やしません。機能・版・制約を重複記載せず、詳しい資料へリンクします。画面レビューの出力は `verification/` に置きます。古い作業記録・画面は[Git履歴](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/README.ja.md)で確認できます。署名情報・権限・ライセンス台帳とテストの期待値は保持します。変更後は `python3 tools/check-docs.py` を実行してください。
