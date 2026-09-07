# RAW動画 / DNG連番

5igna1の撮影設定で「RAW動画 / DNG連番（実験機能）」をONにし、動画モードで録画します。初期OFF。選択中のカメラが連続RAWとDNG保存に対応している場合だけ利用できます。

## 保存内容と使い方

- 保存先：`Download / 5igna1`。約3.5GBごとにZIPを分割します。
- ZIP内：未加工の16bit格納RAWセンサーデータを持つDNG連番、`timestamps.csv`、`manifest.json`、説明ファイル。
- 無音。エフェクトとLIVE FAULTは適用しません。プレビューはカメラが現像した表示です。
- ZIPを展開してRAW対応ソフトにDNG連番として読み込みます。通常のMP4プレーヤーでは直接再生できません。CinemaDNGコンテナへの準拠を主張するものではありません。
- 録画を止めるかアプリを離れると、受付を止めて保存待ちのフレームを処理し、ZIPを確定します。
- 位置情報ONでは録画開始時に取得済みの位置を各DNGへ記録します。

## 対応判定と速度

Camera2のRAW capability、通常モードのRAW_SENSORサイズ、DNGメタデータと一致する寸法、最小フレーム時間とstall時間を確認します。最大解像度専用の静止画モードは含めません。1フレームのコピーを64MiB以下、保存待ちを最大2フレームに制限します。

RAWモード選択時に、RAWとプレビューの出力構成を問い合わせて実際に開始し、対応するCaptureResultとRAW画素からDNGを1枚エンコードできるか検証します。この確認画像はファイル保存しません。APIで構成の問い合わせができない端末は実際の開始結果で判定します。検証が失敗した場合は理由を表示して通常動画へ戻し、そのカメラのRAW設定をその起動中は無効にします。

目標fpsは1〜端末が報告する上限（最大30）で指定できます。上限値は実測保証ではありません。ストレージ、DNG処理、熱状態、カメラの動作によって速度が下がります。例えば4096×3072・12fpsはセンサーデータだけで約302MB/秒になります。

保存待ちの上限を超えたフレームや、撮影情報と対応付けられなかったフレームは破棄します。画面とmanifestの欠落数はアプリ内で破棄した数です。カメラがアプリへ渡す前に失ったフレームは含みません。実際の撮影間隔はCSVのセンサー時刻、区間平均fpsはmanifestで確認できます。一定fpsとして読み込む場合は、欠落に応じた時間の扱いを編集ソフト側で決めてください。

空き容量不足・書込みエラーでは停止します。確定済みZIP区間は残し、保存途中の不完全な区間は削除します。プロセス強制終了で残った未公開のRAW ZIPは次の起動時に清掃します。

## 調査した方式

1. Camera2のRAW_SENSORを連続取得し、DngCreatorでDNG連番にする方式。Androidの公開APIで実装でき、フレームごとの撮影情報を保持できます。今回採用しました。
2. RAWフレーム・音声・撮影情報を専用コンテナへ記録し、後からDNG等へ展開する方式。MotionCamのMCRAWが実例で、公式のデコーダーはRAWからDNG、音声からWAVへの抽出例を公開しています。専用コンテナの記録・圧縮や音声同期は今回実装していません。

現在の通常動画は現像済み画像をAVC / HEVCで圧縮するMP4です。RAW記録ではそのエンコーダーを経由せず、センサー画素を保持します。

## 参照した一次資料（2026-09-08）

- [Camera2 RAW capability](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_RAW)
- [StreamConfigurationMap: 寸法・フレーム時間・stall時間](https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap)
- [CameraDevice: 出力構成の対応確認](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#isSessionConfigurationSupported(android.hardware.camera2.params.SessionConfiguration))
- [DngCreator: RAW画素と撮影情報からDNGを保存](https://developer.android.com/reference/android/hardware/camera2/DngCreator)
- [MotionCam公式MCRAW decoder](https://github.com/mirsadm/motioncam-decoder)

## 今回の確認

K80の背面4096×3072と前面2592×1944はAPI上の上限目安12fpsとして列挙。背面は実際に2fps設定でDNG連番2枚を保存し、ZIP・DNG寸法・撮影時刻・manifestを検証しました。12fpsの持続録画は未検証です。AndroidエミュレーターはRAWを公開していますが必要なDNG情報が不足しており、設定を無効化して通常動画へ戻ることを確認しました。
