# Releasing 5igna1

Canonical source: **https://github.com/Bongorian/5igna1-release**. Version 1.0.0 / code 8 is the first public release. Current source is 1.3.1 / code 12, adding the interactive guide and optional experimental device response. GitHub 1.1.0 / code 9 retains its original tagged source. Existing 1.0.0 submissions, artifacts and tags remain unchanged. [Launch checklist](LAUNCH_TASKS.md)

## Distribution and identity

| Source | Build | Signing |
|---|---|---|
| GitHub Releases / Obtainium | fdroidRelease APK | Long-term distribution key |
| F-Droid | fdroidRelease from source | Normally built and signed by F-Droid |
| Google Play | playRelease AAB | AAB uses upload key; installed APKs use the Play app signing key |

All release flavors use `com.bongorian.signa1`. Core functions remain shared. Matching package IDs do not make different signing certificates interchangeable. Decide Play app signing before enrollment if cross-source updates are wanted. F-Droid signature copying additionally requires an accepted reproducible-build setup, which is not verified here.

[Android signing](https://developer.android.com/studio/publish/app-signing) · [F-Droid reproducible builds](https://f-droid.org/en/docs/Reproducible_Builds/)

## Version rules

Keep literal `versionName 'X.Y.Z'` and an increasing `versionCode` in app/build.gradle. Tags are `vX.Y.Z`. Update CHANGELOG and both fastlane `changelogs/<versionCode>.txt` files. Published tags and APKs are immutable: a fix gets a new version/code.

```sh
python3 tools/release.py check
```

## Local FOSS signing

Without an opt-in, `assembleFdroidRelease` needs no credentials and produces an unsigned APK for F-Droid builds.

For GitHub, use the existing dedicated distribution key and an ignored `fdroid-signing.properties`, following `fdroid-signing.properties.example`. Do not generate a new key for each release. The Play upload configuration is not reused implicitly.

```sh
./tools/build.sh assembleFdroidRelease -PsignFdroidRelease=true
python3 tools/release.py package --certificate-sha256 YOUR_CONFIRMED_CERTIFICATE_SHA256
```

Replace the fingerprint with the adopted release certificate, recorded in [DISTRIBUTION_CERTIFICATE.md](DISTRIBUTION_CERTIFICATE.md). The packager verifies the signature, certificate, ID, version, non-debug mode, and fdroidRelease metadata. It rejects an unsigned/debug/unexpected certificate or an attempt to replace a different file under an existing name.

For the 1.3.1 publication, attach these files after signing and verification:

```text
5igna1-v1.3.1.apk
5igna1-v1.3.1.apk.sha256
```

## Initial or manual release

Run the required checks and push the reviewed commit. Create a draft targeting its full commit SHA, attach the verified APK and checksum, and write user-facing release notes. Check the draft before publishing it. Publishing may create its missing tag; do not create a second conflicting tag or overwrite an existing public release.

Verify the public release API, APK/checksum downloads, and tagged source without authentication. Obtainium's source is the repository URL, and the stable APK filter is `^5igna1-v[0-9]+\.[0-9]+\.[0-9]+\.apk$`.

## Optional GitHub signing automation

The tag workflow prepares a draft after validating source, both flavors, dependencies, and version consistency. It needs a `release` environment with:

| Name | Kind |
|---|---|
| FDROID_KEYSTORE_BASE64 | Secret: the adopted keystore, Base64 encoded |
| FDROID_STORE_PASSWORD | Secret |
| FDROID_KEY_ALIAS | Secret |
| FDROID_KEY_PASSWORD | Secret |
| FDROID_CERTIFICATE_SHA256 | Variable: public certificate fingerprint |

Secrets are not configured automatically. Configure them before pushing a `v*` tag to use this workflow. It restores the key only in the runner's temporary directory and removes it on exit. Do not upload keys or passwords as artifacts or paste them into logs/issues.

The normal push/PR workflow runs without release credentials on Ubuntu and macOS. A local signed/manual release does not require GitHub signing Secrets.

## F-Droid

The [metadata candidate](fdroid/com.bongorian.signa1.yml) uses this repository, the fdroid flavor, and v1.0.0. Run `fdroid lint` and `fdroid build --test com.bongorian.signa1` in the F-Droid setup, then follow [the official submission procedure](https://f-droid.org/en/docs/Inclusion_How-To/). Do not submit private signing keys. [Readiness](FDROID_READINESS.md)

## Google Play

Publishing a stable GitHub release now triggers a signed AAB build and upload to an Alpha draft. See [automatic Play uploads](PLAY_AUTOMATION.md) ([日本語](PLAY_AUTOMATION.ja.md)) for setup, verification and retries. Review submission and publication remain owner actions in Play Console.

Play 1.1.0 / code 9 is published to Alpha testers. Version 1.3.0 / code 11 was submitted on 2026-09-09, skipping 1.2.0 on Play. Managed publishing is on; approval and publication are pending. Preserve existing submitted candidates. See the [1.3.0 submission record](PLAY_1_3_0.md). The existing ignored `signing.properties` applies only to Play upload signing. Build with `./tools/build.sh bundlePlayRelease`; see [Play submission materials](../store/google-play/README.md). Billing/Supporter Pack is not implemented, so no product creation is needed for launch.

## Owner checklist

The up-to-date owner checklist is [LAUNCH_TASKS.md](LAUNCH_TASKS.md): offline backup, signing-service setup if adopted, Play registration/testing, F-Droid submission, and actual listing URLs.
