# 監査資料の棚卸し

[English・全ファイル一覧](README.md) · [検証範囲と制限](../VALIDATION.ja.md) · [文書一覧](../DOCUMENTATION.ja.md)

2026-09-10時点。公開済み1.6.0と、未公開のPixel 9修正 `742c094` を基準に整理しました。リリースへの収録と、署名済み配布物での再検証は別です。

| 区分 | 記録と扱い |
|---|---|
| 未公開修正 | [Pixel 9 RAW/DNG](PIXEL_9_DNG_SIZES.md)。1.6.0には含まれず、Pixel 9のDEV版で確認 |
| 1.6.0収録 | [画面回転・可変ウィンドウ](ADAPTIVE_WINDOW_FEEDBACK.md)、[UI・DISPLAY](UI_DISPLAY_POLISH.md)、[最終の小型UI・形式切り替え・ガイド](COMPACT_CAPTURE_UI.md)。途中のUI説明より最後の記録を優先 |
| 1.5.1以前 | [録画の終了・保存](FOREGROUND_1_5_1.md)、[TAP・MEDIA](TAP_MEDIA_1_5_0.md)、[撮影推奨・音声](ADAPTIVE_CAPTURE_AUDIO.md)。バックグラウンド録画の説明は1.5.1で失効 |
| 過去の測定 | 1.3.0のRAW／GPU最適化、1.3.1のガイド／発熱／実験機能。測定条件とJSONを対応させて保持 |
| 過去の設計 | [注入の検討](DATA_INJECTION_POINTS.ja.md)はTAP実装前の調査。現在の機能説明として扱わない |
| 維持する台帳 | [素材の出典](ASSETS.md)・[素材ハッシュ](assets.json)・[依存一覧](dependencies.json)。サイト素材は別ブランチで管理 |

既存28ファイル（Markdown 21、JSON 7）に完全一致の重複はありません。英日版や最適化の前半・後半は用途が異なるため残しました。全JSONを読み取れ、対応する説明から参照できます。素材29件のハッシュと依存台帳の整合も確認しました。

検証一覧の1.6.0・Pixel 9修正への参照漏れと、公開後も未公開に見える記述を整理しました。過去の測定値・ハッシュは変更していません。旧素材監査の26件という数字は当時の記録とし、29件の現況を追記しました。

新しい記録には日付・ソース・端末・配布形式・条件・結果・制限を残し、訂正や公開状況は日付付きで追記します。ローカル保存だけのログや撮影結果は、このリポジトリから取得できる証拠と区別します。今回の棚卸しで過去のベンチマークを再実行したわけではありません。

- [GPU詳細調査とLIGHT MODE](GPU_DETAIL_LIGHT.ja.md) / [English](GPU_DETAIL_LIGHT.md)、[測定値](gpu-detail-light-results.json)：未リリースの実装と採用しなかったシェーダー試作。

[1.6.1 release follow-up: GPU defaults and TAP verification](GPU_DETAIL_LIGHT.md).
