# Installation and updates

[All guides](README.md) · [Take your first photograph](GETTING_STARTED.md)

## Install from GitHub

1. On your Android device, open the [latest 5igna1 release](https://github.com/Bongorian/5igna1-release/releases/latest).
2. Under **Assets**, download `5igna1-vX.Y.Z.apk`. The `.sha256` file is a checksum, not an installer; GitHub's “Source code” archives are for developers.
3. Open the downloaded APK. If Android asks, allow the browser or file app you are using to install unknown apps, then return to the installer. Wording varies by device.
4. Install and open 5igna1. Allow camera access when prompted.

Requires Android 12 or later. There is one APK for all supported architectures. See [Android's installation help](https://support.google.com/android/answer/9457058?hl=en) for device settings.

## Follow updates with Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) can follow an app's release page and offer new versions. Add this URL as an app source:

```text
https://github.com/Bongorian/5igna1-release
```

5igna1 publishes one APK per release, so no architecture filter is needed. If you need an APK-name filter, use:

```text
^5igna1-v[0-9]+\.[0-9]+\.[0-9]+\.apk$
```

Obtainium tracks public releases. Its source URL should be the repository above, rather than the URL of one downloaded APK. See [Obtainium's documentation](https://github.com/ImranR98/Obtainium#readme) for setup.

## Updating

Install a newer release over the existing app, using the same distribution source. 5igna1 uses a stable app ID and distribution signing certificate. The update installer checks the signing identity.

An app from a different store may have a different signature even if its name and package ID match. Do not uninstall as a first response to a signature error: uninstalling removes app settings. Check the source and [troubleshooting guide](TROUBLESHOOTING.md) first.

The GitHub APK is FOSS. Google Play registration and F-Droid submission are still in progress; no official store listing is available yet.

## Check the download

The release includes `5igna1-vX.Y.Z.apk.sha256` beside the APK. On a computer, put both files in one folder and run the appropriate command for version 1.0.0:

```sh
# Linux
sha256sum -c 5igna1-v1.0.0.apk.sha256
# macOS
shasum -a 256 -c 5igna1-v1.0.0.apk.sha256
```

A checksum detects a mismatched download. The [signing certificate fingerprint](DISTRIBUTION_CERTIFICATE.md) identifies the release key; it is a different value from an APK's file hash. [Android's signing documentation](https://developer.android.com/studio/publish/app-signing) explains update signatures.
