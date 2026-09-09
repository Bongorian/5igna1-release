# 文書の役割と保守方針

[English・全ファイル一覧](DOCUMENTATION.md) · [ガイド一覧](README.ja.md)

初めて使う場合は利用ガイド、実装を変更する場合は実装資料、検証の根拠を確認する場合は日付付き記録を参照してください。現行ガイドの対象は1.6.0です。過去版の説明は各リリースタグ内にあります。

| 区分 | 入口と役割 |
|---|---|
| 利用ガイド | [README](README.ja.md)から、初回撮影・操作・FAULT・時間・形式・負荷へ移動 |
| 録画と権限 | [RECORDING](RECORDING.ja.md)が画面を離れた際の終了・保存と権限の用途 |
| SEED | [SEEDS](SEEDS.ja.md)が構造・イベントの再現性と再生成 |
| 撮影仕様 | [FORMATS](FORMATS.ja.md)が通常／ADVANCED撮影・RAW・音声の基準 |
| 実装資料 | [ARCHITECTURE](ARCHITECTURE.ja.md)が所有権・状態・ライフサイクル、[RAW](raw-pipeline.ja.md)が表現変換 |
| 開発手順 | [building](building.md)が環境とビルド、[DEVELOPMENT](DEVELOPMENT.ja.md)が端末検証の実行方法 |
| 検証結果 | [VALIDATION](VALIDATION.ja.md)が索引と制限、[auditの棚卸し](audit/README.ja.md)が公開版・未公開修正・過去記録・台帳を分類 |
| 配布運用 | [RELEASING](RELEASING.md)が手順、[LAUNCH_TASKS](LAUNCH_TASKS.md)が日時付きの状態と残作業、[Play自動化](PLAY_AUTOMATION.ja.md)が設定と再試行 |
| 過去の記録 | 旧リリース報告・design・auditは当時の判断と測定の根拠。現在の機能説明として読まない |
| 監査資料 | 素材の出典・ハッシュ、依存一覧、F-Droid提出候補をそれぞれの対象に限定して管理 |

[全ファイル一覧](DOCUMENTATION.md)は英日版とJSON・YAMLも含めて役割を整理しています。INSTALLATIONとTROUBLESHOOTINGは現在英語版を参照します。

仕様を変えたら対応するガイドと日本語版を更新します。過去の測定値・ハッシュ・提出候補は現在の値で上書きしません。版情報はapp/build.gradle、利用者向けの対象範囲はREADME、配布状態はLAUNCH_TASKSへ集約します。リリースと同じ版文字列でも、タグ以降の開発コードは未公開です。

文書を移したら双方のリンクを直し、文書リンク・一覧の網羅性・翻訳・関連するアプリ検証を実行します。アプリ内ポリシーとローカルHTMLの変更は、該当版の公開時にサイト側にも反映する必要があります。このソースの編集だけでサイトは更新されません。
