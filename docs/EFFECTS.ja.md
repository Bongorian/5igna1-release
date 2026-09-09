# FAULT POINTと操作値

[English](EFFECTS.md) · [ガイド](README.ja.md)

同じ壊れた系の個性を保ち、その中で起こる一瞬の事故を撮ります。CLEANはFAULTが選択されていない状態です。選んだ順序にかかわらず、信号の因果順で適用します。

| FAULT POINT | FAULT | 操作値 |
|---|---|---|
| SENSOR | PIXEL DAMAGE | 密度、暗点〜輝点、列の故障 |
| SENSOR | EXPOSURE | 露光欠落、位相速度、帯 |
| READOUT | ROW ERROR | ずれ、帯、欠落、取得済みサンプルの再利用 |
| DATA | BIT ERROR | 活動量、bit位置、burst領域 |
| DATA | ADDRESS ERROR | byte offset、領域、活動量 |
| CFA / RECONSTRUCTION | CFA ERROR | 範囲、位相、領域 |
| CFA / RECONSTRUCTION | DEMOSAIC ERROR | 誤った近傍からの補間、サンプリング |
| COLOR | CHROMA ERROR | 色差のずれ、サンプリング、方向 |
| COLOR | COLOR MAP | パレット位相、周期、混合 |
| CODEC / STREAM | BLOCK ERROR | 量子化の欠落、ブロックサイズ、誤読事故 |
| CODEC / STREAM | STREAM ERROR | 欠落事故、領域、サンプル再利用 |
| MEDIA | VHS | 帯域特性、tracking、dropout、noise |
| DISPLAY | CRT | 走査・蛍光体の特性、convergence・syncの故障 |

LEVELは各FAULT固有の密度・距離・露光・事故の影響などへ変換する操作値です。全FAULT共通の物理的なstrengthではありません。内部パラメータの項目数は自由です。

IDENTITYは故障位置やbias、MOTIONは連続的なdriftや位相、EVENTは一時的な事故です。時間が進んでもChainと個体seedを変えません。「ランダムチェーン」は現在の形式で使える効果を2〜5個選び、操作値と全体LEVELを生成します。長押しでRESEEDになります。RESEEDは選択中の故障個体だけを変更し、操作値は保持します。COLOR MAPは個体seedを使いません。リセットは操作値だけを初期化します。

画像の起点はすべて実カメラです。被写体を意味推論で生成・補完しません。RGBのCFA／DEMOSAICは再モザイクによる近似です。CHROMA ERRORは輝度・色差を分けて扱います。BLOCK／STREAMは復号画像の故障モデルで、実bitstream／packetは破壊しません。STREAMの再利用元は同じフレーム内の取得済みサンプルで、過去フレームのdatamoshは未実装です。

VHS／CRTの媒体・表示特性と、その内部の故障は別項目です。TERMINALはCRTの緑色蛍光体profileに統合しました。RAWを唯一の正解とは扱いません。各経路は異なる信号表現です。

STREAM ERRORは静止画・動画共通です。加工RAWに適用できるのはPIXEL DAMAGE〜CFA ERRORの6種類で、RGBプレビューとは別露光・別表現です。[記録形式](FORMATS.ja.md) · [LIVE FAULT](LIVE_FAULT.ja.md)

今のFAULTがどう変化するかはLIVEで設定します。周期・更新間隔は秒単位です。[時間変化の操作](LIVE_FAULT.ja.md)。内部値を直接固定するには、設定で [ADVANCED MODE](ADVANCED_MODE.ja.md) を一括ONにします。

実験的な端末連動がONなら、加工RAWでもブレ・熱雑音・スミアを追加できます。対応範囲と近似の制限は[実験機能](EXPERIMENTAL_SIGNALS.ja.md)を参照してください。
