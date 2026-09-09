# 検証範囲と制限

[English](VALIDATION.md) · [ガイド](README.ja.md) · [検証手順](DEVELOPMENT.ja.md)

検証結果は記載したソース・端末・条件に限ります。過去の成功結果を、新しいコードの検証済み根拠にはしません。

| 対象 | 記録 |
|---|---|
| 1.4.0の端末判定・通常撮影・解像度連動音声 | [今回の検証](audit/ADAPTIVE_CAPTURE_AUDIO.md) |
| 1.3.1の操作ガイド・設定の即時保存 | [操作ガイドと負荷調査](audit/GUIDE_AND_THERMAL.ja.md) |
| 1.3.1の端末入力感度・追加現象 | [実験機能](EXPERIMENTAL_SIGNALS.ja.md)・[実行結果](audit/experimental-signals-results.json) |
| 1.3.0のRAW・GPU最適化 | [RAW前半](audit/RAW_OPTIMIZATION.ja.md)・[RAW後半](audit/RAW_REMAINING_OPTIMIZATION.ja.md)・[GPUとバッファ](audit/FAULT_RENDER_AND_BUFFERS.ja.md) |
| 1.3.0までの再設計・Kotlin移行 | [過去の検証ログ](audit/VALIDATION_THROUGH_1_3_0.ja.md) |
| 1.0.0 | [初期版検証](audit/VALIDATION_1_0_0.md) |

単体テストは移行前の固定ハッシュと独立したRAW参照処理を維持しています。カメラ・GPUの結合確認は別に行います。エミュレーターの成功は、実機の音質・RAW・持続性能・発熱・電池消費の保証ではありません。RAWは別露光・別表現です。1.4.0の表示フレーム固定JPEGはADVANCED ON時に限ります。

数時間の連続録画、広範な機種確認、GPU電力測定、長時間の条件統一した温度比較は行っていません。不具合報告には版・機種・OS・形式・解像度／fps・ADVANCED／EXPERT設定と再現手順を添えてください。
