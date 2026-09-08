# 素材監査

監査日: 2026-09-08。現在のツリーと、初期取り込みからのGit履歴の作成記録を確認しました。[ファイル別SHA-256](assets.json)を併記しています。

| 対象 | 出典・作成方法 | ライセンス / 判定 |
|---|---|---|
| `app/src/main/res/drawable/icon*.xml` / adaptive icon | 初期アプリのロゴを継承。所有者は自作または本人の依頼による生成と確認 | 所有者の権利範囲でApache-2.0。問題なし |
| `ic_camera_flip.xml`, `ic_gallery.xml`, `ic_shuffle.xml`, `ic_tune.xml` | プロジェクトで記述した単純なAndroidベクター | Apache-2.0。問題なし |
| `ic_settings.xml` | Google Material Design Icons 3.0.1のsettings形状をAndroid vectorへ変換・簡略化・配色変更 | Apache-2.0。Googleの帰属と変更をソース・NOTICEへ記載 |
| 各言語の `images/icon.png` | 上記のロゴベクターから512×512 PNGへ書き出し | 元ロゴと同じ。問題なし |
| 各言語の `images/featureGraphic.png` と `store/google-play/source/feature-graphic.png` | 2026-09-07に所有者の依頼でImageGen生成。元プロンプトを保存。1024×500への出力版と生成元 | 所有者が持つ権利についてApache-2.0。生成物の利用条件は下記。生成モデル・学習素材のライセンスをアプリへ持ち込むSDK依存はない |
| 各言語の `images/phoneScreenshots/*.png`（現在の版） | 2026-09-08、Android Emulator API 37、`-camera-back emulated`、1080×1920。現在のfdroidDebug UIを日本語/英語で実行して画面をそのまま取得 | プロジェクトUI＋Apache-2.0のAOSPテストシーン。UI合成・画像の描き足しなし。問題なし |
| 外部フォント、音声、写真、テクスチャ、機械学習モデル、presetデータ | アプリの追跡ファイルとAPKを点検。該当なし | 同梱なし。フォントはOS標準を利用 |

## 現在のスクリーンショット

01 CLEAN、02 ROW SHIFT、03 ROW SHIFT→CHROMA→VHS、04 調整ダイアログを収録。撮影対象は写真素材ではなく、Androidのfake cameraが生成する20×20の家のパターンです。対応する[Scene.cpp](https://android.googlesource.com/device/generic/goldfish/+/refs/heads/android10-gsi/camera/fake-pipeline2/Scene.cpp)のパターンとApache-2.0ヘッダ（Copyright 2012 AOSP）を確認し、NOTICEへ帰属を記載しました。撮影時のエミュレーターの色・移動は入力自体の特徴で、実機のカメラ品質を示すものではありません。

再生成は `DeviceChecks` の `action=screenshots` を、個人情報のないエミュレーターで実行します。実機で実行すると現在カメラが写すものを保存するため、公開対象の被写体を先に確認してください。READMEとfastlaneは同じ画像を参照します。

## 生成広告画像

生成元と編集プロンプトは[作成記録](../../store/google-play/asset-provenance.md)に保存しています。外部の写真・人物・ブランド画像を入力した記録はありません。[OpenAIの利用規約](https://openai.com/policies/row-terms-of-use/)は当事者間のOutputの権利について定めています。画像を配布する権利の範囲で本プロジェクトのApache-2.0を適用します。自動生成物の著作権成立や第三者の権利まで保証する記載はしません。公開者は素材と生成履歴を最終確認してください。

## 公開履歴の開始点

このリポジトリは2026-09-08の現行ソースから新しいGit履歴を開始します。開発用リポジトリの過去コミット・旧Toren1BD背景を含むスクリーンショットは取り込んでいません。現在の画像13ファイルとベクター/adaptive iconは `assets.json` に記録しています。

所有者から提示された [Android Emulator Internal Book](https://github.com/aospbooks/android-emulator-internal-book/blob/main/index.md) のApache-2.0表記は書籍への許諾として確認しました。Toren1BD素材自体の許諾をこの記載だけで確定せず、旧素材を含まない現行スナップショットを公開する方針へ変更しています。旧素材はこの公開リポジトリのライセンス監査対象ファイルに含まれません。

アプリに同梱する素材で、非FOSSまたはライセンス不明として残ったものはありません。READMEのタイトルアートは作品紹介用であることを明示し、実機・実UIの撮影サンプルとして扱いません。

### Development UI utility icons — 2026-09-08

`ic_flash.xml`, `ic_location.xml`, `ic_mic.xml` and `ic_mic_off.xml` are original vector paths created for this project with Codex, under the project Apache-2.0 license. Input: compact 24-unit outline lightning bolt, location pin and microphone (including a muted variant), 1.6-unit stroke, matching the existing controls. No external source image or icon library was used.
