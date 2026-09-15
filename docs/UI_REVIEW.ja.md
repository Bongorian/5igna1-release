# UIの検証

[開発手順](DEVELOPMENT.ja.md) · [English](UI_REVIEW.md)

`Signal_Review_1280_480`（1280×2772、480 dpi、API 35）で配置を再確認します。確認した端末と実効密度を揃えていますが、OSの外観・カメラ性能を再現するものではありません。

1. `python3 tools/review-emulator.py` を実行し、表示されたコマンドで起動します。
2. [開発手順](DEVELOPMENT.ja.md)に従い、DEV版と検証APKをインストールします。
3. `DeviceChecks` の `-e action workspace-review -e language ja` で撮影画面と録画、`-e action chain-review` でチェーンを確認します。workspace-reviewに `-e proSheets true` を加えると、PROの4画面を縦横で確認します。
4. `python3 tools/collect-workspace-review.py --suite workspace` または `--suite chain` で画像・ハッシュを取得します。出力はGit管理外の `verification/` 内です。PRO画像はアプリ内のverificationファイルから `adb exec-out run-as` で取得します。

テスト結果とスクリーンショットの両方を確認します。シャッター寸法・欠け、折り畳み、ステータス位置、アイコン領域と見出し、編集の適用／取消・閉じる操作を対象にします。保存画像ビューアは `-e action saved-signal` と `language-saved-signal-footer.png` で確認します。

一時的な画像を毎回ガイドへ追加しません。未解決の問題を説明する資料に絞り、完了したレビューはGit履歴を参照します。

[過去の画面レビュー](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/UI_REVIEW.ja.md)には元画像・ハッシュ・再現手順が残っています。現在と配置が異なる部分があります。

アナログFPVはDeviceChecksの `-e action analog-fpv -e language ja` で確認します。無加工時の一致、スナップショット再現、局所的な干渉、6つの調整項目を検証し、DEVアプリの `files/verification/fpv-{source,default,band,controls}.png` に画像を出力します。取得した画像は追跡対象外の `verification/fpv/` に置きます。
