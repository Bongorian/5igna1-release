# 開発・検証

[English](DEVELOPMENT.md) · [ビルド要件](building.md)

## 構成

| 項目 | 値 |
|---|---|
| デバッグ版アプリID | `com.bongorian.signa1.debug` |
| debug versionName / versionCode | see `app/build.gradle` (currently 1.4.0-debug / 13) |
| minSdk | 31（Android 12） |
| compileSdk / targetSdk | 36 |
| Android Gradle Plugin | 8.10.1 |
| Kotlin | 2.2.20（JVM 17） |
| Gradle Wrapper | 8.11.1 |
| 推奨JDK | 17（21も使用可能） |

Android SDK Platform 36と、AGPが要求するビルドツールを用意してください。SDKの場所は `ANDROID_HOME` またはGit管理外の `local.properties` の `sdk.dir` で指定します。

aapt2はAGPがホストOSに合うものを自動取得します。Linux ARM64などで独自版が必要な場合だけ、コマンドに `-Pandroid.aapt2FromMavenOverride=/path/to/aapt2` を付けてください。共有設定にローカルパスは書きません。

## macOS（Apple Silicon / Intel）

Android StudioのSDK ManagerでAndroid SDK Platform 36、Build Tools 35.0.0、Platform Toolsを用意します。JDK 17は `brew install openjdk@17` でも導入できます。

```sh
./tools/build.sh
```

このスクリプトは、未指定の場合にHomebrewのJDK 17またはインストール済みJDK 17/21と `~/Library/Android/sdk` を探します。明示した `JAVA_HOME` / `ANDROID_HOME` は優先します。システム既定Javaが25でも変更不要です。Gradle 8.11.1をJava 25で実行しないでください。

Android Studioで開く場合は、このリポジトリを選び、Gradle JDKを17または21に設定してください。これはAndroidアプリです。Mac上ではAndroid Emulatorで実行します。

## ビルド

```sh
./gradlew assembleFdroidDebug
```

初回は依存関係の取得にネット接続が必要です。キャッシュが揃っている場合は `--offline` を付けられます。

APKは `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` に出力します。開発用署名のため、既存インストールへの更新には同じ署名が必要です。署名鍵・APKはリポジトリに含めません。

## 最小限の検証

ビルドと静的検査：

```sh
./tools/build.sh assembleFdroidDebug assembleFdroidDebugAndroidTest lintFdroidDebug test
```

カメラを使わない状態管理・RAW行ずれの短い検査：

```sh
./tools/build.sh testFdroidDebugUnitTest
```

通常の小さな変更ではこれらを基本とし、描画・撮影経路を変えた場合だけ、関連する実機検査を追加します。

## Wi-Fi ADBと実機検査

端末のワイヤレスデバッグに表示される、現在のアドレスとポートを使います。接続用ポートとペア設定用ポートは別です。

```sh
# 未ペアリングの場合だけ。コードはプロンプトに入力する。
adb pair PHONE_IP:PAIR_PORT
adb connect PHONE_IP:DEBUG_PORT
adb devices -l
```

以下の `DEVICE` は `adb devices` に表示された接続先へ置き換えてください。

```sh
adb -s DEVICE install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
adb -s DEVICE install -r app/build/outputs/apk/androidTest/fdroid/debug/app-fdroid-debug-androidTest.apk
adb -s DEVICE shell am instrument -w -e action state \
  com.bongorian.signa1.debug.test/com.bongorian.signa1.DeviceChecks
```

`effects` は13 FAULTのGPU、全名前付き操作値、LEVELゼロ、snapshot再読、因果順、旧ID初期化を検証します。`normal-capture` は通常経路のバッファ解放・JPEG保存・状態保持、`capture-contract` はADVANCED ONで表示済み画像を確保した後にカメラと設定を進め、保存JPEGの画素・時刻が確保した画像と一致することを検証します。`compatibility` はカタログ、VGA写真、動画非対応時の写真プレビュー、前後切替を検証します。`state` は実機カメラを起動し、状態の確定／キャンセル、保存値、GPU描画とFAULTの描画を確認します。テストは撮影設定を一時変更し、終了時に復元します。`metadata` はEXIF・RAW・GPUの検査です。

保存経路を変更したときだけ、短い撮影検査を行います：

```sh
adb -s DEVICE shell am instrument -w -e action photo -e power 70 \
  com.bongorian.signa1.debug.test/com.bongorian.signa1.DeviceChecks
adb -s DEVICE shell am instrument -w -e action video \
  -e videoKey 1920x1080@30 -e bitrate 0 -e power 70 -e seconds 4 \
  com.bongorian.signa1.debug.test/com.bongorian.signa1.DeviceChecks
```

`photo`、`video`、`raw`、`raw-original`、`segment` は実際にメディアを作成します。結果の `failure` がなく、`result=PASS ...` と `INSTRUMENTATION_CODE: -1` が返ることを確認してください。

検証用コピーは端末内のアプリ領域 `files/verification/` にも置かれます。結果に表示されたファイル名を使って取得できます。

```sh
adb -s DEVICE exec-out run-as com.bongorian.signa1.debug \
  cat files/verification/OUTPUT_FILENAME > OUTPUT_FILENAME
```

## 保存メディアの検証ツール

- `tools/check-media.py`：ffprobeとffmpegでJPEGの寸法、短いMP4の時刻・音声同期・フレーム順・全デコードを検査。短い検証動画を対象とします。
- `tools/check-dng.py`：OSのLibRaw共有ライブラリを自動検出して利用（macOSは `brew install libraw`、必要なら `LIBRAW_LIBRARY` で指定）してDNGを開き、展開・現像できることを検査。
- `tools/RawGlitchCheck.kt`：従来のRAW検査補助。現在の通常の入口は `EffectStateCheck` です。

```sh
python3 tools/check-media.py test.jpg short-test.mp4
# 15fps設定・低速エミュレーターでは最低fpsを明示する（既定は20）。
python3 tools/check-media.py --min-fps 10 slow-test.mp4
python3 tools/check-dng.py test.dng
```

転送が中断したファイルを、端末側の保存失敗と取り違えないでください。必要に応じて端末側と取得側のチェックサムを比較します。

## Git管理範囲

ソース、Gradle Wrapper、ドキュメント、検証ツールを管理します。`verification/` は撮影データ・設定ダンプ・ログ・バックアップを含むローカル作業領域、`dist/` はローカルAPK置き場で、どちらも除外しています。

SDKパス、認証情報、署名鍵、端末アドレス・ペアリングコードはリポジトリへ追加しません。共有する検証結果は、個人データを除いた要約を [VALIDATION.md](VALIDATION.md) に記載します。

## 対応端末の方針

Android 12以降、Camera2プレビュー/JPEGとOpenGL ES 2.0が必要です。メーカー・モデル・CPU ABIで制限しません。マイク、GPS、AF、背面カメラは任意です。

- 初期設定はRAM・CPU数と対応出力に合わせた推奨JPEG、AVC/H.264、VGA／HD／FHDの30fps以下を優先。対応しなければ実際の候補から選びます。
- RAW非対応カメラではJPEGへ切り替えます。HEVC非対応ならAVCへ切り替えます。動画候補がなくても写真を起動でき、動画操作を無効にします。
- VGAなど小さい写真・動画サイズも候補です。ソフトウェアエンコーダーも利用できますが、実時間処理速度は端末次第です。
- セッション構成が拒否された場合、低解像度の設定で1回再試行します。
- 最大解像度・RAW・多段エフェクトの負荷や、同時出力できる組み合わせは端末に依存します。対応OSを満たしていても全機種での動作保証ではありません。

カメラの組み合わせは [CameraDeviceの公式仕様](https://developer.android.com/reference/android/hardware/camera2/CameraDevice)、ビルド要件は [AGP 8.10の公式仕様](https://developer.android.com/build/releases/past-releases/agp-8-10-0-release-notes)を参照してください。

`raw-echo` は接続端末でTIME ECHOとRAW切り替えを検証する追加のInstrumentationアクションです。実際のJPEG・DNG・MP4を保存します。

## 1.5.0 checks

- `action seed`: SEED field refresh, exact integer input, Apply/Cancel, EVENT SEED and recording guard.
- `action transport`: model UI, settings order, pixel identity, intermediate resolution, network hold/resume and LED gaps.
- `action tap`: 写真・動画素材、入力位置、再生／録画、ホーム移動時の終了・保存を確認します。安全なロックを設定していないエミュレーターでは`-e leave lock`で画面ロックも確認できます。DEV領域の`files/tap-fixture.mp4`が必要です。
