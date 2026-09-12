# Camera app integration — unreleased

[Guides](README.md) · [日本語](CAMERA_INTEGRATION.ja.md)

These contracts are included in 1.7.1. Earlier releases do not implement them.

## Opening the camera

`android.media.action.STILL_IMAGE_CAMERA` opens photo mode and `android.media.action.VIDEO_CAMERA` opens video mode. These launches use ordinary capture settings and ordinary media storage. Secure/lock-screen actions are not advertised.

## Requesting a result

`android.media.action.IMAGE_CAPTURE` and `android.media.action.VIDEO_CAPTURE` open a single-result session. Photo output is JPEG; video output is H.264 MP4 where supported. RAW and TAP are unavailable in this session. The requested photo/video mode stays fixed. The existing FAULT chain is used and can be adjusted. These temporary capture/effect/audio choices do not replace the ordinary saved choices; camera selection can still be remembered.

The captured file is staged in private cache. The user can preview it, choose **Use**, retake it or cancel. The camera preview stops during review. The destination is written only after acceptance. Location is omitted, regardless of the camera's normal preference. Other signal metadata remains in the resulting file.

- With `MediaStore.EXTRA_OUTPUT`, supply a writable `content://` URI, include it in ClipData, and grant write access. The full JPEG/MP4 is copied there before returning `RESULT_OK`. No extra DCIM copy is made. A ClipData-only output URI is also accepted.
- Without an output URI, photo capture returns a small `Bitmap` in the `"data"` extra, with a maximum edge of 256 pixels. It does not save a full photo to the gallery.
- Without an output URI, video capture saves the accepted MP4 in `DCIM/5igna1` and returns its content URI with a temporary read grant. The recipient should copy/import it if it needs its own persistent copy.
- Cancellation, rejected camera permission, invalid destinations and failed delivery return `RESULT_CANCELED`. Retakes and cancelled private staging files are deleted. A failed destination write is truncated where the provider supports that operation; caller-owned documents are never deleted by the camera. Provider failure can prevent cleanup, so recipients must not use output after cancellation.
- Video requests honor a positive `EXTRA_DURATION_LIMIT` in seconds and `EXTRA_SIZE_LIMIT` in bytes. The video ends at the limit rather than starting another segment. A completed file exceeding the requested byte limit is rejected. `EXTRA_VIDEO_QUALITY` accepts 0 (small supported video/low bitrate) or 1 (recommended video/high bitrate). Unsupported or malformed limits are rejected. Frame/encoder timing may slightly affect recorded duration.
- Screen hiding still stops/finalizes video. Acceptance waits until the user returns. Review and asynchronous delivery survive Activity recreation. Interrupted cache left after process termination is subject to Android cache management or app-data deletion.

## Explicit selection and Android restrictions

For callers targeting Android 11 or later, unrestricted `IMAGE_CAPTURE`/`VIDEO_CAPTURE` resolution and candidate searches can be limited to preinstalled system cameras. Adding these intent filters does **not** force 5igna1 into another app's camera picker. The caller can support 5igna1 by specifying its package (`com.bongorian.signa1`, or `com.bongorian.signa1.debug` for DEV) and optionally including that explicit request in its own selection UI. A calling app that inspects package availability must also account for package visibility; a narrow `<queries><package android:name="com.bongorian.signa1" /></queries>` declaration supports checking this package.

For example, after creating an output URI that the caller owns:

```kotlin
val request = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
    setPackage("com.bongorian.signa1")
    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
    clipData = ClipData.newRawUri("Photo output", outputUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}
// Launch with an Activity Result launcher. Consume outputUri only after RESULT_OK.
cameraResultLauncher.launch(request)
```

For MP4, use `MediaStore.ACTION_VIDEO_CAPTURE`. Do not add `FLAG_ACTIVITY_NEW_TASK` when a caller expects an Activity result. Hardware shortcuts and the default camera selection UI remain device-dependent.

[Common camera intents](https://developer.android.com/guide/components/intents-common#Camera) · [Android 11 camera restrictions](https://developer.android.com/about/versions/11/behavior-changes-11#camera) · [Package visibility](https://developer.android.com/training/package-visibility)

## Verification

The opt-in `camera-client` fixture is a separate application/UID with no camera or media permissions. It explicitly selects DEV, grants a private provider destination, and verifies the actual Activity result and readable bytes. It is not included in any release APK.

```sh
./tools/build.sh -PcameraClientChecks=true :app:assembleFdroidDebug :app:assembleFdroidDebugAndroidTest :camera-client:assembleDebug
adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
adb install -r app/build/outputs/apk/androidTest/fdroid/debug/app-fdroid-debug-androidTest.apk
adb install -r test-clients/camera/build/outputs/apk/debug/camera-client-debug.apk
adb shell pm grant com.bongorian.signa1.debug android.permission.CAMERA
adb shell am instrument -w -e action camera-intents com.bongorian.signa1.debug.test/com.bongorian.signa1.DeviceChecks
```

The test path uses instrumentation callbacks and assertions, with no screenshot, accessibility-tree inspection or coordinate input. It covers ordinary mode launch, JPEG copy and thumbnail results, retake, review recreation, cancellation, invalid/read-only destinations, output failure, time-limited MP4 and returned-video URI access. Device-manufacturer shortcut behavior is outside this test.

Verified on 2026-09-10 with a headless Android 15 / API 35 emulator: ordinary and reused photo/video launches, all eight external-result cases above, an adjusted ROW ERROR chain across review recreation, byte-identical output delivery, no GPS tags, and preserved ordinary RAW/location/codec/last-media preferences. The client could resolve the package-specific capture request; its unrestricted capture query did not list DEV. Lint, 220 JVM tests across four variants, FOSS debug/release and Play debug builds passed. No physical-device or visual UI verification was performed for this change.
