# Google Play 配布準備

2026-09-09に1.3.0 / versionCode 11を既存のクローズドテストAlphaへ審査送信しました。1.1.0 / code 9はテスターへ公開済みで、1.2.0 / code 10はPlayでは配信していません。テスター設定・対象国を維持した更新です。管理対象の公開はオンのため、承認後に公開操作が必要です。[1.3.0提出記録](../../docs/PLAY_1_3_0.md)を参照してください。アプリIDは **com.bongorian.signa1**。F-Droid/GitHubとの署名関係は[共通リリース手順](../../docs/RELEASING.md)を先に確認してください。

## 成果物

```sh
./tools/build.sh bundlePlayRelease
# JDKのjarsignerとPillowが必要
python3 tools/package-play.py
```

Play用署名は未追跡の `signing.properties` または `SIGNAL_PLAY_*` 環境変数から読みます。鍵がなければAABは未署名であり、packagingは失敗します。既存のupload keyを上書き・再生成しないでください。

AABは `app/build/outputs/bundle/playRelease/app-play-release.aab`。包装後は `dist/5igna1-v1.3.0-google-play.zip` です。ZIPにはAAB、日英素材、説明資料、プライバシーHTML、SHA256SUMSを含めます。秘密鍵・署名設定は含めません。GitHubの一般配布APKはこのPlay成果物ではなくfdroidReleaseから作ります。

## 素材

- `fastlane/metadata/android/ja-JP` と `en-US`：名称、短い説明、詳細説明、`changelogs/11.txt`（過去版の記録も保持）。実際に提出した日英リリースノートは[提出記録](../../docs/PLAY_1_3_0.md)を参照。
- アイコン512×512、feature graphic 1024×500、各言語の1.1.0 UIスクリーンショット1080×1920を4枚。
- 広告用feature graphicは生成アートです。実機の撮影結果ではありません。[作成記録](asset-provenance.md)と[素材監査](../../docs/audit/ASSETS.md)参照。
- スクリーンショットはエミュレーターのテストパターンを写した実行画面。2026-09-09に1.1.0の新UIで撮り直し、日英4枚ずつを確認済みです。プライバシーHTMLは現行実装へ更新し、公開サイトでも3言語すべての一致を確認済みです。

## Console側の作業

1. 開発者登録とアプリ作成。公開者・アプリID・署名方針を確認。
2. Play App Signingを設定。upload keyと配信用app signing keyは役割が異なります。
3. 署名済みplayRelease AABをアップロード。
4. 日英ストア情報・画像・連絡先を登録。対象年齢、IARC、データセーフティ等は[申告下書き](console-declarations.md)と実際の質問を照合して回答。
5. `privacy/index.html` を管理するHTTPSサイトへ配置して公開URLをConsoleに登録。ローカルファイルのままでは提出できません。
6. Consoleが要求する内部/クローズドテスト・審査を完了後に公開。適用条件はアカウントと公開時点で確認。
7. 掲載された実URLをREADMEへ追記。

このアプリはBilling、広告、解析SDKを実装していません。主要機能はF-Droid版と同じです。公開のためにBilling商品やSupporter Packを作成する必要はありません。
