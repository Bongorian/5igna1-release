# 検証範囲と制限

[English](VALIDATION.md) · [ガイド](README.ja.md) · [検証手順](DEVELOPMENT.ja.md)

検証結果は記載したソース・端末・条件に限ります。過去の成功結果を、新しいコードの検証済み根拠にはしません。

| 対象 | 記録 |
|---|---|
| 1.6.0以降・未リリース：LIGHTとGPU詳細 | [調査・実機検証](audit/GPU_DETAIL_LIGHT.ja.md) |
| 1.6.0以降の未公開修正：Pixel 9のRAW/DNGサイズ適合 | [Pixel 9 DEV検証](audit/PIXEL_9_DNG_SIZES.md) |
| 1.6.0：可変ウィンドウ・回転・フィードバック | [画面と操作の検証](audit/ADAPTIVE_WINDOW_FEEDBACK.md) |
| 1.6.0：SEED・NETWORK／LED・小型UIとガイド | [表示改善](audit/UI_DISPLAY_POLISH.md)・[最終UI](audit/COMPACT_CAPTURE_UI.md) |
| 1.5.1：画面離脱時の録画終了・保存、サービス権限の削除 | [ライフサイクル検証](audit/FOREGROUND_1_5_1.md) |
| 1.5.0：TAP・MEDIA／DISPLAY・SEED | [実機検証](audit/TAP_MEDIA_1_5_0.md) |
| 1.4.0の端末判定・通常撮影・解像度連動音声 | [撮影ポリシーの検証](audit/ADAPTIVE_CAPTURE_AUDIO.md) |
| 1.3.1の操作ガイド・設定の即時保存 | [操作ガイドと負荷調査](audit/GUIDE_AND_THERMAL.ja.md) |
| 1.3.1の端末入力感度・追加現象 | [実験機能](EXPERIMENTAL_SIGNALS.ja.md)・[実行結果](audit/experimental-signals-results.json) |
| 1.3.0のRAW・GPU最適化 | [RAW前半](audit/RAW_OPTIMIZATION.ja.md)・[RAW後半](audit/RAW_REMAINING_OPTIMIZATION.ja.md)・[GPUとバッファ](audit/FAULT_RENDER_AND_BUFFERS.ja.md) |
| 1.3.0までの再設計・Kotlin移行 | [過去の検証ログ](audit/VALIDATION_THROUGH_1_3_0.ja.md) |
| 1.0.0 | [初期版検証](audit/VALIDATION_1_0_0.md) |

単体テストは移行前の固定ハッシュと独立したRAW参照処理を維持しています。カメラ・GPUの結合確認は別に行います。エミュレーターの成功は、実機の音質・RAW・持続性能・発熱・電池消費の保証ではありません。RAWは別露光・別表現です。1.4.0の表示フレーム固定JPEGはADVANCED ON時に限ります。

数時間の連続録画、広範な機種確認、GPU電力測定、長時間の条件統一した温度比較は行っていません。不具合報告には版・機種・OS・形式・解像度／fps・ADVANCED／EXPERT設定と再現手順を添えてください。


[監査資料の棚卸し](audit/README.ja.md)で公開版に収録した開発検証、未公開修正、過去の測定、維持する台帳を区別しています。Pixel 9修正は公開済み1.6.0には含まれません。

[描画・RAW負荷調査](audit/LOAD_INVESTIGATION.ja.md)：Pixel 9のテスト用試作、出力比較、測定の制限。アプリ本体への適用は未実施です。
