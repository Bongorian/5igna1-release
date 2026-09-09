# Building 5igna1

Requires JDK 17, Android SDK Platform 36, and Build Tools 35.0.0. The repository pins Gradle Wrapper 8.11.1 and Android Gradle Plugin 8.10.1, and Kotlin 2.2.20 (JVM target 17). Application and test sources are Kotlin; GLSL remains the GPU shader language.

On macOS, `./tools/build.sh` detects the JDK and SDK and builds fdroidDebug by default. Set `ANDROID_HOME` or an ignored `local.properties` for other setups. Windows users can use `gradlew.bat`.

```sh
./gradlew clean
./gradlew lint
./gradlew test
./gradlew assembleFdroidDebug
./gradlew assembleFdroidRelease
./gradlew assemblePlayDebug
```

| Task | Output | Signing |
|---|---|---|
| assembleFdroidDebug | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` | Development key |
| assembleFdroidRelease | `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` | Unsigned by default |
| assemblePlayDebug | `app/build/outputs/apk/play/debug/app-play-debug.apk` | Development key |
| bundlePlayRelease | `app/build/outputs/bundle/playRelease/app-play-release.aab` | Play upload key, when configured |

An unsigned APK cannot be installed as a normal release. GitHub distribution uses explicit signing configuration described in [Releasing](RELEASING.md). A Play upload key is not used for the FOSS release implicitly.

Both release flavors use `com.bongorian.signa1`; both debug flavors use `com.bongorian.signa1.debug`. Debug can coexist with a normal release. The two debug flavors replace each other.

## Verification

`./gradlew test` runs nine JVM tests for each flavor/build-type combination. The fixtures cover immutable capture state, bounded frame handoff, RAW row/byte/CFA operations, LIVE behavior, advanced controls, adaptive load, and random chains. A migration fixture compares evaluated effects and RAW bytes with fixed SHA-256 values captured from the Java implementation.

```sh
python3 tools/check-locales.py
python3 tools/release.py check
python3 tools/check-repository.py
./gradlew dependencyInventory verifyFossDependencies
python3 tools/audit-dependencies.py --check
```

`verifyFossDependencies` checks for known prohibited SDK groups. It supplements a license review; it does not replace one. The inventory check compares resolved versions and artifact hashes with the reviewed snapshot.

See [device verification procedures (Japanese)](DEVELOPMENT.md) and [tested scope](VALIDATION.md). Use an emulator or a device whose camera and storage you intend to test; capture tests write media.

## Kotlin source layout

Application code lives in `app/src/main/kotlin`; JVM tests are in `app/src/test/kotlin` and `tools/*.kt`, and emulator checks are in `app/src/androidTest/kotlin`. Gradle installs the pinned Kotlin compiler automatically; a separate Kotlin installation is not required.

Format Kotlin with [ktfmt](https://github.com/Kotlin/ktfmt), version 0.64, using `--kotlinlang-style`. The optional formatter is a development tool and is not packaged in the application. See [architecture](ARCHITECTURE.md) for ownership and immutable capture contracts.
