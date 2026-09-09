# 文書の役割と保守方針

[English・全ファイル一覧](DOCUMENTATION.md) · [ガイド一覧](README.ja.md)

2026-09-09に、この開発ソースと照合して整理しました。ガイドは現在のチェックアウトを説明し、公開済みの基準版と未公開の追加機能はガイド冒頭で区別します。公開版の正確な説明は、そのタグ内の文書を参照してください。

| 区分 | 入口と役割 |
|---|---|
| 利用ガイド | [README](README.ja.md)から、初回撮影・操作・FAULT・時間・形式・負荷へ移動 |
| 撮影仕様 | [FORMATS](FORMATS.ja.md)が通常／ADVANCED撮影・RAW・音声の基準 |
| 実装資料 | [ARCHITECTURE](ARCHITECTURE.ja.md)が所有権・状態・ライフサイクル、[RAW](raw-pipeline.ja.md)が表現変換 |
| 開発手順 | [building](building.md)が環境とビルド、[DEVELOPMENT](DEVELOPMENT.ja.md)が端末検証の実行方法 |
| 検証結果 | [VALIDATION](VALIDATION.ja.md)が索引と制限、audit配下が条件付きの実行記録 |
| 配布運用 | [RELEASING](RELEASING.md)が手順、[LAUNCH_TASKS](LAUNCH_TASKS.md)が日時付きの状態と残作業、[Play自動化](PLAY_AUTOMATION.ja.md)が設定と再試行 |
| 過去の記録 | 旧リリース報告・design・auditは当時の判断と測定の根拠。現在の機能説明として読まない |
| 監査資料 | 素材の出典・ハッシュ、依存一覧、F-Droid提出候補をそれぞれの対象に限定して管理 |

[全ファイル一覧](DOCUMENTATION.md)は英日版とJSON・YAMLも含めて役割を整理しています。INSTALLATIONとTROUBLESHOOTINGは現在英語版を参照します。

今回、5ページ版チュートリアル、設定の適用／取消、常に表示フレームを撮るという説明、端末初期設定、実験的RAW対応、固定のテスト件数、古い配布状態を修正しました。長い時系列検証ログはauditへ移し、[過去の検証](audit/VALIDATION_THROUGH_1_3_0.ja.md)として保存しました。26件の素材ハッシュは現物と一致し、素材自体は変更していません。

仕様を変えたら対応するガイドと日本語版を更新します。過去の測定値・ハッシュ・提出候補は現在の値で上書きしません。版情報はapp/build.gradle、利用者向けの対象範囲はREADME、配布状態はLAUNCH_TASKSへ集約します。リリースと同じ版文字列でも、タグ以降の開発コードは未公開です。

文書を移したら双方のリンクを直し、文書リンク・一覧の網羅性・翻訳・関連するアプリ検証を実行します。アプリ内ポリシーとローカルHTMLの変更は、該当版の公開時にサイト側にも反映する必要があります。このソースの編集だけでサイトは更新されません。
