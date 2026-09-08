# RAW表現のアダプター

[English](raw-pipeline.md) · [内部設計](ARCHITECTURE.ja.md)

RAW16はカメラ由来の表現の一つです。RawGlitchはGPUと同じ不変FaultNodeを受け取り、寸法・黒白レベル・配列長を確認して入力を複製し、出力を白レベル以内へ収めます。

PIXEL DAMAGE、EXPOSURE、ROW ERROR、BIT ERROR、ADDRESS ERROR、CFA ERRORの6種類を信号順に適用します。行のずれは偶数画素単位、行の再利用はBayerの行ペア単位です。CFA ERRORとbyte addressの誤読では意図的に位相や解釈が崩れます。

RAW写真は別のstill requestによる露光です。FAULT状態はシャッター時の表示から固定しますが、画素と露光時刻は後から取得したRAW自身のものです。RGBプレビューは実RAW現像ではありません。表現アダプターも異なるため、同じパラメータでも画素・故障位置・現像結果の一致は保証しません。

RAW写真はセンサー加工を適用し、原本RAW動画は無加工です。DNG連番の有界キューと時刻対応を維持します。加工RAW動画は将来同じ状態モデルを使って拡張できる構造ですが、現在は提供しません。色・媒体・表示の後段をBayer DNGへ書き込みません。
