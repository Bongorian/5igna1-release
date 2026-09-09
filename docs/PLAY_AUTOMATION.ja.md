# Playへの自動アップロード

[English](PLAY_AUTOMATION.md) · [リリース手順](RELEASING.md)

安定版の `vX.Y.Z` GitHub Releaseを公開すると、**Upload Play AAB** が実行されます。公開タグのソースを取得し、検査・テスト・署名付きAABのビルド・証明書の検証を行い、日本語と英語の更新内容を付けた **Alphaの下書き** に保存します。審査への送信と公開はPlay Consoleで行います。

## 費用と認証

公開リポジトリの標準Ubuntuランナーは[GitHubの無料対象](https://docs.github.com/en/billing/concepts/product-billing/github-actions)です。この処理はActionsの成果物やキャッシュを保存せず、常設サーバーや有料Cloudリソースも作りません。既存のPlay開発者登録費用は別です。リポジトリの非公開化やランナー変更時は料金を再確認してください。

Googleとの接続には短時間だけ有効な認証を使い、サービスアカウントの秘密鍵JSONは作成しません。既存のPlayアップロード鍵とパスワードはGitHubの暗号化された環境シークレットに保存し、ビルド中だけ復元します。署名鍵の変更はありません。

## 設定

対象は `Bongorian/5igna1-release` の `google-play` 環境です。`main` と `v*` タグを許可しています。リポジトリ変数 `PLAY_UPLOAD_ENABLED=true` で有効、`false` で以後の実行を停止します。

| 設定名 | 種類 |
|---|---|
| PLAY_UPLOAD_KEYSTORE_BASE64 | シークレット：既存のPlayアップロード鍵のBase64 |
| PLAY_UPLOAD_STORE_PASSWORD | シークレット |
| PLAY_UPLOAD_KEY_ALIAS | シークレット |
| PLAY_UPLOAD_KEY_PASSWORD | シークレット |
| PLAY_WORKLOAD_IDENTITY_PROVIDER | 変数：Google認証プロバイダー |
| PLAY_SERVICE_ACCOUNT | 変数：専用アカウント |

Google側はリポジトリと所有者の数値ID、対象ワークフロー、mainまたはバージョンタグに接続元を限定しています。Playの権限は5igna1の閲覧・品質情報の閲覧・テスト版リリースだけです。製品版リリースやアカウント管理の権限はありません。

## 使い方

1. versionCodeを増やした新しい版を検査し、GitHub Releaseを公開します。下書きとプレリリースは対象外です。
2. GitHub → Actions → **Upload Play AAB** で実行結果を確認します。
3. Play Console → クローズドテスト → Alphaで下書きを確認し、適切なタイミングで審査に送信・公開します。

既存の安定版は **Run workflow** で `main` と対象タグを指定して実行できます。`verify_only` を選ぶと署名付きビルドの検証だけを行います。自動化導入前のタグにも使えます。今後のリリースはワークフローを含むmainを基にしてください。

既存のAlpha下書き、より新しい版、同一versionCodeで異なるAAB、APIエラーがあると停止します。Alphaに同一AABがすでにある場合は変更しません。再ビルドでハッシュが変わる可能性があるため、競合時に上書きせず、既存AABを確認してください。変更したビルドには新しい版番号が必要です。

審査中の変更がある場合も、審査を取り消さずに停止します。既存の審査・公開が終わってから再実行してください。審査保護の設定を外して強行しないでください。未確定の一時編集は削除し、削除できない場合は自動失効します。

下書きはユーザーへ配信されません。テスター・国・ストア説明・F-Droid申請・既存のタグやAPKは変更しません。

## 検証と保守

アップロード処理のテストは通常のUbuntu/macOS CIと実行時に走ります。既存リリースの保持、重複や競合、ハッシュ不一致、審査中エラーを検査します。AABはアプリID・版番号・非デバッグ設定・既存の証明書を確認してから送ります。

外部Actionはコミット固定です。検証専用のGoogle bundletool 1.18.3（Apache-2.0）は公式配布物のSHA-256を確認して利用し、アプリの依存関係には追加しません。詳細・公式資料・設定項目は[英語版](PLAY_AUTOMATION.md)を参照してください。

## 初回実行の確認 — 2026-09-09

[GitHubの実行記録](https://github.com/Bongorian/5igna1-release/actions/runs/34316132266)で、`v1.3.1` の署名付きAABビルド・Google認証・Alphaへの保存が成功しました。Play Consoleでも **12 (1.3.1) が未公開の下書き**、**11 (1.3.0) がテスターへ公開済み**と確認しています。最終処理のUbuntu/macOS CIも通過しました。

## 1.4.0の自動アップロード — 2026-09-09

[実行34327243407](https://github.com/Bongorian/5igna1-release/actions/runs/34327243407)で、GitHub公開後に1.4.0／code 13をAlphaの下書きへアップロードできました。AABのSHA-256は`e30842447f1d95f375a61ad5a088dec35bdd69381b426701a4c5241b11d1f64e`です。既存の下書き・審査を守るチェックは有効なままで、競合するリリースの削除や審査取消はしていません。下書きの保存までの確認であり、審査提出・テスターへの配信は別操作です。

## 1.5.0

2026-09-09に1.5.0 / code 14をAlpha下書きへアップロードできました。審査送信やテスターへの公開は別の操作です。 [Run](https://github.com/Bongorian/5igna1-release/actions/runs/34352867100).

## 1.5.1

2026-09-09に1.5.1 / code 15を未公開のAlpha下書きへアップロードしました。AAB SHA-256: `bd937cd0bbc55ea68db3e37ae5c5277e85634f2c6856329605a7b5991df99d5b`。録画用フォアグラウンドサービスと対応する権限を削除したリリースソースから作成しました。審査提出とテスターへの公開はConsoleでの別操作です。 [Run](https://github.com/Bongorian/5igna1-release/actions/runs/34356251537).
