# Third-party licenses

監査日: 2026-09-09。対象はKotlin移行開発版のPlay / F-Droid構成です。

アプリ本体は[Apache-2.0](LICENSE)。第三者の著作権・ライセンスはそのまま維持します。依存コードを本プロジェクトの著作物として再ライセンスするものではありません。

## Runtime and compile dependencies

`implementation` はAndroidX AppCompat 1.7.1、Startup 1.1.1（manifestで参照）、Kotlin標準ライブラリ 2.2.20です。両flavorのrelease runtimeグラフは一致します。46モジュール（BOM・メタデータを含む）を選択し、44アーティファクトを解決しました。下表はruntimeアーティファクトのあるモジュールです。compile-only/platformも含む全194件の座標・POMライセンス宣言・親POM・アーティファクトSHA-256は[依存一覧](docs/audit/dependencies.json)に記録しています。

| Component | Version | Declared license |
|---|---|---|
| [androidx.activity:activity](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.8.0/activity-1.8.0.pom) | 1.8.0 | Apache-2.0 |
| [androidx.annotation:annotation-experimental](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-experimental/1.4.0/annotation-experimental-1.4.0.pom) | 1.4.0 | Apache-2.0 |
| [androidx.annotation:annotation-jvm](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-jvm/1.6.0/annotation-jvm-1.6.0.pom) | 1.6.0 | Apache-2.0 |
| [androidx.appcompat:appcompat-resources](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat-resources/1.7.1/appcompat-resources-1.7.1.pom) | 1.7.1 | Apache-2.0 |
| [androidx.appcompat:appcompat](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/1.7.1/appcompat-1.7.1.pom) | 1.7.1 | Apache-2.0 |
| [androidx.arch.core:core-common](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-common/2.2.0/core-common-2.2.0.pom) | 2.2.0 | Apache-2.0 |
| [androidx.arch.core:core-runtime](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-runtime/2.2.0/core-runtime-2.2.0.pom) | 2.2.0 | Apache-2.0 |
| [androidx.collection:collection](https://dl.google.com/dl/android/maven2/androidx/collection/collection/1.1.0/collection-1.1.0.pom) | 1.1.0 | Apache-2.0 |
| [androidx.concurrent:concurrent-futures](https://dl.google.com/dl/android/maven2/androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.pom) | 1.1.0 | Apache-2.0 |
| [androidx.core:core-ktx](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.13.0/core-ktx-1.13.0.pom) | 1.13.0 | Apache-2.0 |
| [androidx.core:core](https://dl.google.com/dl/android/maven2/androidx/core/core/1.13.0/core-1.13.0.pom) | 1.13.0 | Apache-2.0 |
| [androidx.cursoradapter:cursoradapter](https://dl.google.com/dl/android/maven2/androidx/cursoradapter/cursoradapter/1.0.0/cursoradapter-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.customview:customview](https://dl.google.com/dl/android/maven2/androidx/customview/customview/1.0.0/customview-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.drawerlayout:drawerlayout](https://dl.google.com/dl/android/maven2/androidx/drawerlayout/drawerlayout/1.0.0/drawerlayout-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.emoji2:emoji2-views-helper](https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2-views-helper/1.3.0/emoji2-views-helper-1.3.0.pom) | 1.3.0 | Apache-2.0 |
| [androidx.emoji2:emoji2](https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2/1.3.0/emoji2-1.3.0.pom) | 1.3.0 | Apache-2.0 |
| [androidx.fragment:fragment](https://dl.google.com/dl/android/maven2/androidx/fragment/fragment/1.5.4/fragment-1.5.4.pom) | 1.5.4 | Apache-2.0 |
| [androidx.interpolator:interpolator](https://dl.google.com/dl/android/maven2/androidx/interpolator/interpolator/1.0.0/interpolator-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-common](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common/2.6.2/lifecycle-common-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-livedata-core](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata-core/2.6.2/lifecycle-livedata-core-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-livedata](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata/2.6.2/lifecycle-livedata-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-process](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-process/2.6.2/lifecycle-process-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-runtime](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime/2.6.2/lifecycle-runtime-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel-savedstate](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/2.6.2/lifecycle-viewmodel-savedstate-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.lifecycle:lifecycle-viewmodel](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel/2.6.2/lifecycle-viewmodel-2.6.2.pom) | 2.6.2 | Apache-2.0 |
| [androidx.loader:loader](https://dl.google.com/dl/android/maven2/androidx/loader/loader/1.0.0/loader-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.profileinstaller:profileinstaller](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/1.3.1/profileinstaller-1.3.1.pom) | 1.3.1 | Apache-2.0 |
| [androidx.resourceinspection:resourceinspection-annotation](https://dl.google.com/dl/android/maven2/androidx/resourceinspection/resourceinspection-annotation/1.0.1/resourceinspection-annotation-1.0.1.pom) | 1.0.1 | Apache-2.0 |
| [androidx.savedstate:savedstate](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate/1.2.1/savedstate-1.2.1.pom) | 1.2.1 | Apache-2.0 |
| [androidx.startup:startup-runtime](https://dl.google.com/dl/android/maven2/androidx/startup/startup-runtime/1.1.1/startup-runtime-1.1.1.pom) | 1.1.1 | Apache-2.0 |
| [androidx.tracing:tracing](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/1.0.0/tracing-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [androidx.vectordrawable:vectordrawable-animated](https://dl.google.com/dl/android/maven2/androidx/vectordrawable/vectordrawable-animated/1.1.0/vectordrawable-animated-1.1.0.pom) | 1.1.0 | Apache-2.0 |
| [androidx.vectordrawable:vectordrawable](https://dl.google.com/dl/android/maven2/androidx/vectordrawable/vectordrawable/1.1.0/vectordrawable-1.1.0.pom) | 1.1.0 | Apache-2.0 |
| [androidx.versionedparcelable:versionedparcelable](https://dl.google.com/dl/android/maven2/androidx/versionedparcelable/versionedparcelable/1.1.1/versionedparcelable-1.1.1.pom) | 1.1.1 | Apache-2.0 |
| [androidx.viewpager:viewpager](https://dl.google.com/dl/android/maven2/androidx/viewpager/viewpager/1.0.0/viewpager-1.0.0.pom) | 1.0.0 | Apache-2.0 |
| [com.google.guava:listenablefuture](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.pom) | 1.0 | Apache-2.0 |
| [org.jetbrains.kotlin:kotlin-stdlib-jdk7](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk7/1.8.0/kotlin-stdlib-jdk7-1.8.0.pom) | 1.8.0 | Apache-2.0 |
| [org.jetbrains.kotlin:kotlin-stdlib-jdk8](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/1.8.0/kotlin-stdlib-jdk8-1.8.0.pom) | 1.8.0 | Apache-2.0 |
| [org.jetbrains.kotlin:kotlin-stdlib](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.2.20/kotlin-stdlib-2.2.20.pom) | 2.2.20 | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-android](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.6.4/kotlinx-coroutines-android-1.6.4.pom) | 1.6.4 | Apache-2.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.6.4/kotlinx-coroutines-core-jvm-1.6.4.pom) | 1.6.4 | Apache-2.0 |
| [org.jetbrains:annotations](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.pom) | 13.0 | Apache-2.0 |

Kotlin標準ライブラリの `MathJVM.kt` にはBoost由来コード（BSL-1.0）が含まれます。[上流の例外一覧](https://github.com/JetBrains/kotlin/blob/v2.2.20/license/README.md)も確認し、[全文](licenses/Kotlin-Boost-1.0.txt)と著作権表示を同梱します。GWT / Guava由来の標準ライブラリ部分はApache-2.0です。時刻処理のThreeTen由来コードはBSD-3-Clauseで、[全文と著作権](licenses/Kotlin-ThreeTen-BSD-3-Clause.txt)を同梱します。POMの単一ライセンス宣言だけで組み込みコード全体を判断しないでください。

`androidx.emoji2` はFOSSです。アプリはその自動初期化を無効にしており、絵文字フォントのダウンロードプロバイダを起動しません。OS標準フォントを利用します。AndroidXやApache-2.0のGuavaはGoogle Play Servicesとは異なります。Billing / Firebase / Analytics / AdMob / GMS SDKは含みません。

## Build and test tools (not packaged in the APK)

| Component | License / scope |
|---|---|
| Gradle Wrapper 8.11.1 | Apache-2.0。公式配布のSHA-256と照合。唯一のGit管理下JAR |
| Android Gradle Plugin 8.10.1 / Android tools | Apache-2.0主体。ビルド用の選択モジュール146件を依存一覧に収録。Android SDKはローカル・CIのビルド環境で使用し、アプリへ同梱しない |
| Kotlin Gradle plugin 2.2.20 | Apache-2.0。コンパイラ・ビルド補助はAPKに含まない。選択座標とPOM宣言は依存一覧に記録 |
| ktfmt 0.64 | Apache-2.0。任意のコード整形ツール。バイナリはリポジトリ・APKに含まない |
| JUnit 4.13.2 | EPL-1.0。`testImplementation` のみ |
| Hamcrest Core 1.3 | BSD-3-Clause。JUnitのテスト用推移依存 |
| JNA 5.6.0 | LGPL-2.1-or-later / Apache-2.0のデュアルライセンス。AGPのビルド用 |
| javax.annotation / javax.activation | CDDL / GPLv2 with Classpath Exceptionの宣言。ビルド用のみ |
| JAXB / StAX / istack | EDL-1.0等。ビルド用のみ |
| Bouncy Castle 1.79 | Bouncy Castle License（MIT型）。ビルド用のみ |
| ASM / protobuf /その他AGP推移依存 | BSD-3-Clause、Apache-2.0、MIT、MPL-1.1等。各バージョンと宣言は依存一覧に記録 |
| Temurin/OpenJDK 17 | GPL-2.0 with Classpath Exception。ビルド環境のみ |
| Python | PSF License。補助スクリプトの実行環境 |
| Pillow | HPND（Pillow/PIL）。任意のPlay素材検査に使用し、APKには含まない |
| FFmpeg / ffprobe | LGPL-2.1-or-later主体、ビルド設定によりGPL。任意のメディア検証コマンドとして起動。バイナリを配布しない |
| LibRaw | LGPL-2.1 / CDDL-1.0の選択。`tools/check-dng.py` がインストール済みライブラリを呼び出す任意の検証専用。APKにはリンク・同梱しない |
| actions/checkout, setup-java, upload-artifact / android-actions/setup-android | MIT。SHAで固定したCI用アクション。APKには含まない |

ビルド用ライブラリにGPL/LGPLという名前が現れることと、アプリ本体にそれらが混入していることは別です。今回、アプリのruntime/compileグラフとアプリ独自ソースにGPL/AGPL/LGPLの同梱依存は見つかっていません。ビルド環境やツール自体を再配布する場合は、それぞれの条件を別途満たしてください。

参照: [Gradle](https://github.com/gradle/gradle/blob/v8.11.1/LICENSE)、[JUnit](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt)、[LibRaw](https://www.libraw.org/about)、[FFmpeg legal](https://ffmpeg.org/legal.html)、[Pillow](https://github.com/python-pillow/Pillow/blob/main/LICENSE)。

## Source and assets

- 初期SIGNALコード・アプリアイコンは、所有者が2026-09-08に自作または本人の依頼で生成したものと確認。現在のソースに外部転載の未処理ライセンス表示やAGPL/GPLヘッダは見つかりませんでした。
- 設定アイコンは[Google Material Design Icons 3.0.1](https://github.com/google/material-design-icons/blob/3.0.1/action/svg/production/ic_settings_24px.svg)由来として、Apache-2.0の出典・変更内容をソースとNOTICEへ明記。
- その他のベクター、生成アート、ストアスクリーンショットは[素材監査](docs/audit/ASSETS.md)を参照。ランタイムに外部フォント・音声・写真・テクスチャ・モデル・プリセットファイルは同梱しません。

## Updating this inventory

```sh
./tools/build.sh dependencyInventory
python3 tools/audit-dependencies.py --write
```

新しい依存のPOM・上流ソース・埋め込み例外を確認してから一覧とNOTICEを更新します。CIは `--check` で解決された座標・ハッシュを比較し、未レビューの変更を失敗にします。POMが示すライセンスは著作者による宣言であり、自動的な法的保証や全ソースの類似性検査ではありません。
