# 再設計版の検証

[詳細（English）](VALIDATION.md) · [設計](design/FAULT_SYSTEM.ja.md)

公開release `7dd83a7`を基準にした未公開実装を、2026-09-08に検証しました。

- JVMは4種類のfixture × 4 variant、計16テストが成功。
- F-Droid debug／unsigned release、Play debug、端末テストAPKのビルド成功。lintはfatal／errorなし。警告は残っています。
- 名前付き操作値、個体の固定・RESEED分離、連続drift、一時事故、snapshot再読、経路順、RAW値域・Bayer位相、表示画像の予約と有界保持を検証。
- エミュレーターでは13 shaderと全操作項目、編集の適用／キャンセル、言語切替、カメラ復旧、LIVE入力の後始末、短い動画保存を確認。
- 表示画像を予約後にカメラと設定を進めても、保存JPEGが予約画像を同じ品質で圧縮したものと画素一致。camera timestampとFAULT情報も一致。

JPEGは実ライブ信号の解像度です。RAWは別露光・別表現で、RGBプレビューと同一ではありません。STREAMは復号画像内のサンプル再利用モデルで、実packet破壊や過去フレームdatamoshはありません。

この再設計について、物理端末の温度・センサー応答・RAW保存、長時間／高解像度の持続性能は未検証です。旧releaseの実機結果は[過去の記録](audit/VALIDATION_1_0_0.md)として区別します。
