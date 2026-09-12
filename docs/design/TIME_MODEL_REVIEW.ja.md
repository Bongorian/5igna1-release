# 内部パラメータと時間モデルの棚卸し

> この棚卸し後の実装は[時間モデル2](../TIME_MODEL.ja.md)を参照してください。以下は旧版の観測・提案を保存した記録です。旧検証の実行にはコミット6e0aaf1を使います。

[English](TIME_MODEL_REVIEW.md) · [全パラメータと検証結果](../audit/TIME_MODEL_EVIDENCE.md) · [資料一覧](../DOCUMENTATION.ja.md)

調査日：2026-09-12。対象はNETWORK改善後の **dd7f027**。これは設計検討資料で、今後の挙動を確定する仕様書ではありません。この調査ではアプリ本体やPixelにインストール済みの版を変更していません。

提案の中心は、各パラメータに「FAULTの演出時刻」「入力・到着時刻」「端末入力への応答」「保持している映像」のどれに従うかを明示することです。撮影と録画の時計は単調に進め、SEEDを使った演出は再現条件を整理し、選択中のモデルに効く項目を表示します。特に優先すべきなのは、映像の時刻と配送の時刻の兼用、モデル選択経路の不一致、NETWORKの設定値と実効値の違いです。

## 対象と調べ方

CLEAN以外の16ステージ、MEDIA／DISPLAYそれぞれ4種類、TIME ECHO、保存時のメタデータまで確認しました。[付録](../audit/TIME_MODEL_EVIDENCE.md)には、実際の `Effects` と `FaultParameters` から生成した全マクロ・内部パラメータ・分類・範囲・スライダー刻みを収録しています。設定マクロは0〜1の正規化値です。

コードの依存関係を読み、11項目をJVM上の合成入力で再現しました。F-Droid debugの単体テスト59件が成功し、既存の移行goldenも変更せず通っています。新しい検証は時刻と数値の確認であり、実機の画質・GPU性能・全撮影経路を測ったものではありません。前のNETWORK描画検証は別の証拠として扱います。`./tools/build.sh testFdroidDebugUnitTest --tests com.bongorian.signa1.TimeModelAuditTest`で再現でき、一覧は`app/build/reports/time-model-audit.md`に出力されます。検証はこの版の現状を記録するもので、将来も同じ挙動を維持すべきという意味ではありません。

主な確認箇所：

- [FaultModel](../../app/src/main/kotlin/com/bongorian/signa1/FaultModel.kt)：時刻、入力応答、イベント、モデル別の数値計算。
- [LivePerformance](../../app/src/main/kotlin/com/bongorian/signa1/LivePerformance.kt)：速度、HOLD、LOOP、PING_PONG、STEP、強度変調。
- [FaultParameters](../../app/src/main/kotlin/com/bongorian/signa1/FaultParameters.kt)／[AdvancedControls](../../app/src/main/kotlin/com/bongorian/signa1/AdvancedControls.kt)：登録項目と実際に表示される項目。
- [EffectChain](../../app/src/main/kotlin/com/bongorian/signa1/EffectChain.kt)／[GLSL](../../app/src/main/res/raw/effect.glsl)／[RawGlitch](../../app/src/main/kotlin/com/bongorian/signa1/RawGlitch.kt)：内部値の実際の使用箇所。
- [GlitchEngine](../../app/src/main/kotlin/com/bongorian/signa1/GlitchEngine.kt)／[TimeEcho](../../app/src/main/kotlin/com/bongorian/signa1/TimeEcho.kt)／[EchoSchedule](../../app/src/main/kotlin/com/bongorian/signa1/EchoSchedule.kt)／[NetworkDisplay](../../app/src/main/kotlin/com/bongorian/signa1/NetworkDisplay.kt)：フレームの時刻と履歴。
- [SavedSignal](../../app/src/main/kotlin/com/bongorian/signa1/SavedSignal.kt)／[EffectState](../../app/src/main/kotlin/com/bongorian/signa1/EffectState.kt)：設定再利用と評価済みスナップショット。

## 現在の時計の分担

| 時計・状態 | 使われ方 | 速度・停止・リセットとの関係 |
|---|---|---|
| フレーム到着からの経過時間 | `advance(arrivalSeconds)` と `elapsed`。入力の平滑化、ばね運動、音声による振動、TRIGGERの減衰 | 速度では変わらない。HOLDでは入力応答とTRIGGERの更新前に戻る。RESETは入力応答の履歴を消さない。 |
| FAULTの再生位置 | `performancePosition += delta × speed` | 逆方向にも進む。HOLDで停止し、RESETでゼロになる。LIVEをONにすると経過時間に位置を合わせ直す。 |
| 全体のFAULT時刻 | FREE、LOOP、PING_PONG、STEPで再生位置を変換 | 全体の強度パターンもこれを使う。LIVE OFFでは経過時間が使われ、内部変化は続く。 |
| 各ステージのFAULT時刻 | 全体時刻×`timeScale`＋`timeOffset`、または固定した`time` | ドリフトと個別イベントに使う。固定しても全体の強度変化、TRIGGER、端末入力は固定されない。 |
| ノイズ・露光のサンプル時計 | 通常は`sensorNs`。時間変換時はFAULT時刻を60 Hzに量子化。通常速度のHOLDでは`heldSignalNs` | 速度を1からわずかに変えるだけでも参照する時計が切り替わる。負時刻の整数化はゼロ方向への切り捨て。 |
| 映像内容のタイムスタンプ | カメラのフレーム時刻。TAPでは現在の経過時間で、読み込んだ動画のPTSではない | TIME ECHOは`Frame.cameraNs`を過去画像の時刻へ置き換える。NETWORKの受信間隔にもこの値が使われている。 |
| TIME ECHOの発生時計 | 現在の入力・到着時刻と専用の状態を持つ乱数 | LIVE／実験機能のONに従うが、FAULT速度やHOLDには従わない。 |
| 表示・録画時刻 | 単調に進む表示用トークンとエンコーダー時刻 | FAULTの逆再生や過去映像の挿入があっても前へ進める必要がある。 |

このため、**HOLDと速度0は同じ動作ではありません**。HOLDは端末入力の評価状態とTRIGGERの減衰も保持し、速度0はFAULT位置だけを止めます。この違いは有用ですが、UIで意味を明確にする必要があります。どちらも入力映像全体の停止を意味しません。ただしNETWORKがフリーズ中なら、その停止状態を保持し続ける場合があります。

## パラメータの種類と単位

| 分類 | 現在の項目 | 整理すべき意味 |
|---|---|---|
| 固有パターン | 64bitのSEED、`identitySeed`、`identityBias` | 元の個体SEEDと、内部の浮動小数点乱数値を区別する。静的なbiasがTIMEに置かれている。 |
| 時刻変換 | `timeScale`、`timeOffset`、`time` | 倍率とFAULT秒。全体の強度パターンはこの局所変換の外側にある。 |
| 連続変化 | `driftSpeed`、`drift`、`phaseSpeed`、`phase` | ドリフト速度・符号付き変位・rad/FAULT秒・rad。全モデルで使われるわけではない。 |
| イベント生成 | `eventPeriod`、`eventDuration`、`eventProbability`、`eventSerial` | 周期・継続時間・発生機会ごとの確率・周期の識別。継続時間が周期より長くても入力可能。 |
| イベント結果 | `eventEnvelope`、`eventPosition`、`eventPattern`、`eventSeed` | 強度包絡・位置・パターン。結果を固定すると上流の生成設定は効かなくなる。Serial固定だけでは周期内の時刻は止まらない。 |
| 端末入力 | 5種類の`*Sensitivity`、0〜4 | グローバルの許可・有効状態と実験機能に依存。過去方向へ再生する時計ではなく到着時間で応答する。 |
| 画質・故障機構 | 下表のモデル固有項目 | 混合率、画像に対する相対変位、ピクセル／サンプル数、角度、バイト位置、量子化段数など。PROFILEもFAULT強度で変わる場合がある。 |
| フレーム受信 | `networkFps`、`networkStall`、停止間隔／長さ／解像度マクロ | 受信の実時間と停止演出のFAULT秒が混在。停止間隔と長さに相当するADVANCED生成パラメータがない。 |
| 保持映像 | NETWORKの処理済みフレーム、ECHOの処理前履歴 | ピクセル・時刻・寿命を持つ状態。SEEDと時刻だけでは再現できない。 |

イベントには固定25 msの立ち上がりと90 msの立ち下がりもあるため、短いイベントは最大強度に達しません。時間やSerialの登録範囲は手入力の制限であり、長時間動作する自動値の上限を保証するものではありません。

## 各モデルの棚卸し

F＝局所FAULT時刻、E＝F上のイベント、S＝サンプル／ノイズ時計、R＝端末入力の応答、A＝到着・受信時間、I＝入力・保持映像。全ステージに共通するSEED項目は必要箇所以外省略しています。「静的」は、強度と入力を固定したときに固有の時間変化がないという意味です。全体の強度パターン、TRIGGER、実験機能の入力による強度増加は別に働きます。

| モデル | 画像処理に使う主な内部値 | 時間との関係・論点 |
|---|---|---|
| CLEAN | なし | 入力を通す。 |
| PIXEL DAMAGE | `pixelDensity`、`columnDensity`、`hotFraction`、`hotValue`、`sensorNoise`、`grainSeed` | 故障位置はSEEDで固定。輝度はF/R、ノイズはS/R。共通phaseは未使用。 |
| EXPOSURE | `exposureDepth`、`exposurePhase`、`scanPhase`、`integration` | Fの発振と、S・実際の露光／読出時間。共通`phase`／`phaseSpeed`は露光位相を動かさず、別の位相系になっている。 |
| ROW ERROR | `weakRows`、`rowGroups`、`rowOffset`、`readoutShear`、`lineLoss`、`linePosition`、`lineHeight`、`lineRetention` | Fのドリフト、Eの欠落・位置、Rのせん断。保持は同じフレーム内の別行で、過去映像ではない。 |
| BIT ERROR | `bitProbability`、`bitIndex`、`bitBlock`、`eventSeed` | Eの強度・パターンとRの温度／負荷。連続ドリフト・phaseは未使用。 |
| ADDRESS ERROR | `byteOffset`、`addressRegion`、`addressProbability`、`identitySeed` | Eは確率を変えるが、領域はidentityで決まる。イベントのpattern／position、共通drift／phaseはこのシェーダーには効かない。RGBバイト列とRAW16の表現は異なる。 |
| CFA ERROR | `cfaCoverage`、`cfaPhase`、`cfaRegion`、`identitySeed` | 静的な配置。cfaPhaseはBayer配置の選択で、時間の角度ではない。強度一定なら共通TIME項目は画面に効かない。 |
| DEMOSAIC ERROR | `interpolationMix`、`sampleScale`、`identitySeed` | 静的な復元格子・近傍選択。共通TIME生成器の直接の用途はない。RGBでの近似。 |
| CHROMA ERROR | `chromaOffset`、`chromaAngle`、`chromaBlock` | Fのドリフトで分離量が変わる。Angleは画像内の向き。 |
| COLOR MAP | `paletteMix`、`palettePhase`、`paletteCycles` | 輝度に対応する色の静的変換。phase／cyclesは色空間の座標で時間発振ではない。SEED対象外。 |
| BLOCK ERROR | `quantLevels`、`blockColumns`、`blockError`、`blockOffset` | Eのアドレス故障、Fとidentity biasの変位。量子化パターンは空間的。 |
| STREAM ERROR | `streamLoss`、`streamColumns`、`concealment`、`eventSeed` | Eの欠落と領域。補間元は同じ画像内で、前フレームではない。 |
| MEDIA / VHS | `tapeBandwidth`、`trackingOffset`、`trackingWave`、`trackingPhase`、`trackingSlip`、`tapeDropout`、`dropoutPosition`、`tapeNoise`、`grainSeed` | Fのドリフト・位相、Eの欠落、Sの粒子、Rの位置・音声応答。最も多くの時間要素がある。 |
| MEDIA / DVD | `mediaReduce`、`transportDamage`、`transportLoss`、identity/event seed | Eで欠落が変わる。損傷・量子化は強度依存。VHSのイベント周期とtracking／dropout由来のactivityを継承している。VHSのphaseとgrainはDVD画像には効かない。 |
| MEDIA / Digital thru | 経路選択のみ | 通過処理。継承したVHSのTIME／EVENT／画像項目はADVANCEDに残る。 |
| MEDIA / Analog thru | `cableKind`、`transportDamage`、`transportLoss`、`transportNoise`、`trackingPhase`、`dropoutPosition`、`grainSeed` | Fの位相、Eの欠落・位置、Sの干渉。解像度は接続モデルで決まる。イベント周期はVHS由来。 |
| DISPLAY / CRT | `scanDepth`、`scanLines`、`phosphorMix`、`convergenceOffset`、`syncOffset` | FのドリフトとRの動き。走査線は空間模様で、phosphorは色の混合。残光の履歴モデルではない。共通phaseは未使用。 |
| DISPLAY / Digital thru | アナログMEDIA後の`upconvert` | 静的な拡大方法。ほかのDISPLAY項目もADVANCEDには表示される。 |
| DISPLAY / Network | バッファ寸法用の`transportLoss`、`networkFps`、`networkStall` | 停止予定はF、受信間隔は現在は映像内容の時刻、保持する画像はI。ノイズは重ねない。`transportDamage`／`networkSeed`／CRT用の値はNetwork画像には効かない。 |
| DISPLAY / LED | `transportLoss`、`transportDamage`、`refreshBand`、`syncOffset`、`identitySeed` | 故障モジュールはSEEDで決定。ちらつきは`floor(syncOffset × 100)`で切り替わり、F/Rに間接依存。明示的な更新周波数はない。 |
| MOTION BLUR | `blurX`、`blurY` | Rの角速度×露光時間と固定のfloor。現在画像の空間的な多点サンプリングで、過去フレームを積算しない。共通Fの生成器は未使用。 |
| THERMAL NOISE | `noiseAmplitude`、`noiseGrain`、`grainSeed` | Rの温度・露光とSの粒子。局所time固定で粒子を固定しても温度応答は続く。 |
| SMEAR | `smearAmount`、`smearLength`、`smearThreshold` | Rの読出情報。現在画像のハイライトを伸ばす空間処理で、時間的な尾を保存しない。 |
| TIME ECHO（別枠） | 発生確率、内部の保持時間・遅延・継続時間・抽選間隔、処理前履歴 | A/Iと状態を持つ乱数。過去映像に現在のFAULTを掛ける。250 ms間隔で最大33枚を低解像度で保持。 |

RAWは基本6種類のセンサー／読出／データ段と、有効時の実験的な3種類を使います。下流のRGBモデルは迂回します。時計の定義は共通化できますが、RGBとBayerの出力が同じになることを意味しません。

## 見つかった不整合と優先順位

| 優先度 | 現象・具体例 | 改善方針 |
|---|---|---|
| 最優先 | `cameraNs`が「画像の古さ」と「次の受信期限」を兼用。ECHOの過去時刻が来ると、NETWORKの停止中でも更新が許可される。 | 単調な受信時刻、内容の時刻、入力元の世代を分離する。古い画像の挿入と入力元の交換を区別する。 |
| 最優先 | 内部`transportKind`の上書きが通常マクロからの計算後に適用される。Networkを通常選択すると12 fps、内部値で選ぶと0＝制限なしになる。 | 有効なモデルを先に一度だけ決め、そのモデルから初期値・時間処理・表示・描画を作る。 |
| 最優先 | 「12 fps」の内部値が`60 + (12 − 60) × level`。初期FAULT 0.55では**33.6 fps**になり、30 fps入力では減速しない。停止時間と解像度も実効強度に依存する。 | 設定値と実効値を表示する。物理的な目標値と劣化強度の対応を定義し、保存値の意味を変える前に移行方法を決める。 |
| 次 | 受信した瞬間から次の期限を計算するため、30 fps入力で12 fps指定でも実測の更新回数は10 fps。上限は守るが、期待する周期より遅い。 | 次回期限を累積する方式を検討。平均fpsか厳密な最小間隔かを決め、遅延時の連続追いつき更新は避ける。 |
| 次 | 各モデルで無効な項目がADVANCEDに残る。旧`networkSeed`の使用先がない。Networkの停止間隔・長さマクロはADVANCEDから触れない。 | 項目に対象モデルと使用先を登録し、有効項目を表示する。古い保存キーは互換性のため保持する。 |
| 次 | 通常の粒子時計と時間変換時の粒子時計が異なる。速度1→0.999999でも、同じFAULT時刻の粒子が変わる。 | ノイズが従う時計を明示する。演出用粒子は安定したFAULT tick、実測露光は明示的なセンサー時刻モードへ整理する。見た目が変わるので比較検証と版管理が必要。 |
| 次 | イベント継続時間を周期より長くできるが、周期境界でリセットされる。短いイベントは固定の立ち上がり／立ち下がりで弱くなる。 | 重複可否と包絡線を定義する。確率・継続時間・強度を別の量として扱う。 |
| 仕様整理 | HOLD、速度0、固定`time`、RESET、LIVE OFFの範囲が違う。 | 「局所時刻の固定」と「ステージ状態全体の固定」を区別する。RESETで何を戻すかを明記する。 |
| 仕様整理 | 一般イベントはステージSEED＋セッション乱数、NetworkはステージSEEDのみ、LIVE BURSTはセッション乱数、ECHOは専用乱数。 | パターン・イベント・全体演出・履歴のSEEDと再現範囲を分ける。SEED単独での完全再現を約束しない。 |
| 仕様整理 | Networkの保持画像は以前の処理結果でも、記録される`rendered.frame`は現在の設定と時刻。強度ゼロでステージが外れると保持画像も破棄される。 | 現在の指示状態と受信済み画像の由来を別に持つ。迂回、RESEED、入力交換、編集プレビュー、撮影、RESET時の履歴寿命を決める。 |

最初の3項目は現行コードの具体的な不一致です。一方、複数の時計、速度0での入力応答、セッションごとのランダム性は、それ自体がバグというわけではありません。操作の意味として選び、明示する必要があります。

## 推奨する共通ルール

1. **フレームの時刻情報に名前を付けて分離する。** 到着／受信時刻、映像内容の時刻、入力元の世代、全体FAULT位置、評価済み全体時刻、局所時刻、乱数更新tickを持たせます。入力交換なら履歴をリセットし、ECHOで古い内容を流しただけなら入力交換扱いにしません。
2. **パターン、連続変化、イベント、端末応答、履歴を個別の機能として宣言する。** 各パラメータに単位・依存元・対象モデル・RAW/RGB対応・生成値か最終固定値かを登録します。実際の使用先に基づいてUIを作ります。
3. **FAULTの時計操作と実時間での取得・記録をそれぞれ定義する。** FREE／LOOP／PING_PONG／STEP／逆再生は共通のFAULT位置から作ります。端末応答は実時間に従い、HOLD時に評価状態を保持します。速度0との差は、意図的に変更しない限り維持して説明します。
4. **Networkの停止と通常イベントは同じ予定生成器に揃える。** 周期、長さ、確率、開始位相、SEEDを共通化し、初期仕様は重複なしにします。受信fpsは別の仕組みとして扱います。LEDを周期的にちらつかせるなら、明示的な更新周波数を持たせます。
5. **実効値を見せる。** FREEの速度2倍では、FAULT時間0.7秒のイベントは強度補正前で実時間0.35秒です。LOOP／STEPでは停止区間を再訪したり飛び越えたりするので、単純な実秒数として約束できません。fpsと解像度も現在の適用値を確認できるようにします。
6. **上書き順を固定する。** 有効モデル決定→マクロからの初期値→局所生成器／イベント→端末・全体変調→最終値の手動固定。下流を固定して上流が効かない場合は、その理由を表示します。
7. **再現性を段階に分ける。** 設定の再利用、評価済みの故障状態、SEEDと入力記録を含む時間列、過去の画像バッファを含む完全再生は別のものです。現在の保存設定の再利用は、入力・履歴まで記録する仕組みではありません。

## 進め方と完了条件

**第1段階：設定経路と表示を揃える。** モデル選択を先に解決し、モデル別の有効パラメータを定義します。受信・内容時刻を分離し、Networkの実効値を表示します。通常選択と内部選択の一致、ECHO中のフリーズ、入力交換、同一時刻、サイズ変更、FAULTゼロ、バッファ解放、画像の由来を検証します。古い設定は読み込めるように保ちます。

**第2段階：時間生成器を整理する。** 共通イベント予定とノイズ時計を追加し、重複・短時間イベント・HOLD／速度0／RESET・SEEDの範囲を確定します。入力とSEEDが固定なら同じ局所時刻で同じ結果になること、負時刻、ループ境界、イベントより大きいSTEP間隔、24／30／60／120 fpsでの振る舞いを比較します。既存goldenは旧挙動の基準として残し、新仕様の期待値を別に追加します。

**第3段階：モデルの意味を揃える。** 必要ならLEDに更新周波数を追加し、EXPOSUREの実際の位相生成器を操作できるようにします。色／Bayerのphaseと時間phaseを区別し、無効項目はUIから整理します。数値が`inspect()`に表示されたことだけでなく、最終的に消費される値や画像で検証します。

**第4段階：新しい表現は別の機能として検討する。** 残光、実際の時間積算によるモーションブラー、フレーム破損などには新しい履歴処理が必要です。現行の空間的近似を説明したうえで、必要性を分けて判断します。今回の棚卸しでは実装しません。

この資料と検証コードは、次の実装を判断するための成果物です。アプリの時間処理、過去のgolden、Pixelのインストール内容、公開リリースは変更していません。
