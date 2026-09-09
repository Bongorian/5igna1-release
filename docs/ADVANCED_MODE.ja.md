# ADVANCED MODE

[ガイド](README.ja.md) · [English](ADVANCED_MODE.md)

設定（歯車）で **ADVANCED MODE** を一括ONにし、選択中のチェーンのチップをタップします。個々のFAULTにモード切り替えはありません。プレビューは編集領域の上に残ります。モードは表示設定なので、OFFにしても固定値は保持します。設定のモード切り替えはその場で保存されます。FAULTの編集内容はチェックで適用し、×・戻る・アプリを離れる操作で下書きを破棄します。

**AUTO**はモデルが生成する現在の内部値です。AUTOをタップして**FIX**にすると固定し、スライダーか数値タップで変更できます。範囲内の有限な数値だけを受け付けます。FIXをタップするとAUTOに戻ります。「すべてAUTO」は対象FAULTの固定値と事故個体の固定を解除します。基本操作はAUTOの値へ作用し、固定値がある項目では固定値を優先します。リセットは対象FAULTの初期操作値へ戻して固定値を解除します。ランダムチェーンは新たに選んだFAULTの固定値を解除し、RESEEDは固定値を保持します。

**SEED**は符号付き64ビットの構造的な個体値です。**EVENT SEED**はROW ERROR・BIT ERROR・ADDRESS ERROR・BLOCK ERROR・STREAM ERROR・VHSにあります。AUTOは起動ごとに事故個体を変え、固定すると同じFAULT時間・入力・設定で事故の系列を再現できます。変化するカメラ画像まで再現するものではありません。SIGNAL内の`identitySeed`／`eventSeed`はRGB描画用に縮約した値で、この整数の個体値とは別です。Bayer RAWでは構造的な個体値も使います。

TIMEは時間の生成器と状態（秒、倍率、drift、phase）、EVENTは事故の周期・継続秒数・確率・通番・強度・位置・パターンです。SIGNALは描画・加工の全内部パラメータ、PROFILEはVHS帯域とCRTの表示特性です。FAULTによっては使わない生成値もあります。下流の値をFIXにした場合、その値は生成器を変更しても変わりません。個数・領域は内部のサンプル格子、変位は原則として正規化値、位相・角度は正規化周期で表す項目を除いてラジアンです。

LIVEが操作するのはFAULT時間で、カメラの撮影時刻は進み続けます。`time`の固定は一つのFAULTの時計を固定します。PAUSE中もカメラは動きますがFAULTの状態は停止し、PAUSE中にHITした状態は解除かリセットまで保持されます。LEVELが0なら全FAULTをバイパスします。撮影時は内部状態を固定したスナップショットを使います。RAWに適用するのはBayer処理に対応する先頭6FAULTのみです。VHS／CRTなどの後段はJPG／MP4を使います。内部値を操作してもRAWにない処理段は追加されません。

以下が編集可能な全項目です。範囲は数値入力にも表示します。All faultsは全FAULT共通、Incident faultsは上記のEVENT SEED対応6種類です。スライダーの刻みと異なり、数値入力は範囲内の有限値を受け付けます。ただしサンプリング処理で離散化される項目があります。

| FAULT | Group | Parameter | Range |
|---|---|---|---|
| All faults | TIME | `timeScale` | -4 … 4 |
| All faults | TIME | `timeOffset` | -3600 … 3600 |
| All faults | TIME | `time` | -86400 … 86400 |
| All faults | TIME | `driftSpeed` | 0 … 10 |
| All faults | TIME | `drift` | -1 … 1 |
| All faults | TIME | `phaseSpeed` | -40 … 40 |
| All faults | TIME | `phase` | -6.283186 … 6.283186 |
| All faults | TIME | `identityBias` | -1 … 1 |
| Incident faults | EVENT | `eventPeriod` | .03 … 60 |
| Incident faults | EVENT | `eventDuration` | .005 … 60 |
| Incident faults | EVENT | `eventProbability` | 0 … 1 |
| Incident faults | EVENT | `eventSerial` | -1000000 … 1000000 |
| Incident faults | EVENT | `eventEnvelope` | 0 … 1 |
| Incident faults | EVENT | `eventPosition` | 0 … 1 |
| Incident faults | EVENT | `eventPattern` | 0 … 997 |
| All faults | SIGNAL | `identitySeed` | 0 … 997 |
| All faults | SIGNAL | `eventSeed` | 0 … 997 |
| PIXEL_DAMAGE | SIGNAL | `pixelDensity` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `columnDensity` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `hotFraction` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `hotValue` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `sensorNoise` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `grainSeed` | 0 … 997 |
| EXPOSURE | SIGNAL | `exposureDepth` | 0 … 2 |
| EXPOSURE | SIGNAL | `exposurePhase` | -6.283186 … 6.283186 |
| EXPOSURE | SIGNAL | `scanPhase` | 0 … 2000 |
| EXPOSURE | SIGNAL | `integration` | -1 … 1 |
| ROW_ERROR | SIGNAL | `weakRows` | 0 … 1 |
| ROW_ERROR | SIGNAL | `rowGroups` | 1 … 2048 |
| ROW_ERROR | SIGNAL | `rowOffset` | -1 … 1 |
| ROW_ERROR | SIGNAL | `readoutShear` | -1 … 1 |
| ROW_ERROR | SIGNAL | `lineLoss` | 0 … 1 |
| ROW_ERROR | SIGNAL | `linePosition` | 0 … 1 |
| ROW_ERROR | SIGNAL | `lineHeight` | 0 … 1 |
| ROW_ERROR | SIGNAL | `lineRetention` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitProbability` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitIndex` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitBlock` | 2 … 512 |
| ADDRESS_ERROR | SIGNAL | `byteOffset` | 0 … 4096 |
| ADDRESS_ERROR | SIGNAL | `addressRegion` | 2 … 4096 |
| ADDRESS_ERROR | SIGNAL | `addressProbability` | 0 … 1 |
| CFA_ERROR | SIGNAL | `cfaCoverage` | 0 … 1 |
| CFA_ERROR | SIGNAL | `cfaPhase` | 0 … 2 |
| CFA_ERROR | SIGNAL | `cfaRegion` | 2 … 1024 |
| DEMOSAIC_ERROR | SIGNAL | `interpolationMix` | 0 … 1 |
| DEMOSAIC_ERROR | SIGNAL | `sampleScale` | 1 … 64 |
| CHROMA_ERROR | SIGNAL | `chromaOffset` | -1 … 1 |
| CHROMA_ERROR | SIGNAL | `chromaAngle` | -3.141593 … 3.141593 |
| CHROMA_ERROR | SIGNAL | `chromaBlock` | 1 … 512 |
| COLOR_MAP | SIGNAL | `paletteMix` | 0 … 1 |
| COLOR_MAP | SIGNAL | `palettePhase` | 0 … 1 |
| COLOR_MAP | SIGNAL | `paletteCycles` | .01 … 32 |
| BLOCK_ERROR | SIGNAL | `quantLevels` | 2 … 256 |
| BLOCK_ERROR | SIGNAL | `blockColumns` | 1 … 512 |
| BLOCK_ERROR | SIGNAL | `blockError` | 0 … 1 |
| BLOCK_ERROR | SIGNAL | `blockOffset` | -1 … 1 |
| STREAM_ERROR | SIGNAL | `streamLoss` | 0 … 1 |
| STREAM_ERROR | SIGNAL | `streamColumns` | 1 … 256 |
| STREAM_ERROR | SIGNAL | `concealment` | 0 … 1 |
| VHS | PROFILE | `tapeBandwidth` | 0 … 1 |
| VHS | SIGNAL | `trackingOffset` | -.5 … .5 |
| VHS | SIGNAL | `trackingWave` | 0 … .25 |
| VHS | SIGNAL | `trackingPhase` | -6.283186 … 6.283186 |
| VHS | SIGNAL | `trackingSlip` | -.5 … .5 |
| VHS | SIGNAL | `tapeDropout` | 0 … 1 |
| VHS | SIGNAL | `dropoutPosition` | 0 … 1 |
| VHS | SIGNAL | `tapeNoise` | 0 … 1 |
| VHS | SIGNAL | `grainSeed` | 0 … 997 |
| CRT | PROFILE | `scanDepth` | 0 … 1 |
| CRT | PROFILE | `scanLines` | 1 … 2160 |
| CRT | PROFILE | `phosphorMix` | 0 … 1 |
| CRT | SIGNAL | `convergenceOffset` | -.5 … .5 |
| CRT | SIGNAL | `syncOffset` | -.5 … .5 |
