# Adaptive windows and feedback — development verification

Scope update — 2026-09-10: this development change shipped in [1.6.0](../../CHANGELOG.md). Original test conditions below remain historical. For final capture controls and guide behavior, see [compact controls](COMPACT_CAPTURE_UI.md). See the [audit inventory](README.md) for the separate unreleased Pixel 9 fix.

Date: 2026-09-09. Development branch: `codex/adaptive-window-modes`, based on 1.5.1. This is not a published release.

The camera activity allows rotation and resizing. Layout follows the current available window: wide landscape windows use preview and controls side by side; portrait and narrow windows stack them. Controls scroll independently and capture stays accessible. Effect editors occupy the control side in wide windows. Camera dimensions and RAW orientation follow the display; TAP keeps the selected media orientation. Configuration changes recreate the activity and can stop/save an active recording, following the foreground-only capture policy; recording never resumes automatically.

Photo, video and experimental TAP use one group of 48dp icon controls with a single highlighted selection and accessible radio-button labels. TAP remains behind Experimental signals. Photo/video long presses still open the resolution picker.

Settings feedback opens a local editable email draft. Device details are optional and default off. Only app version/flavor, Android version, manufacturer and model are offered. The message and opt-in selection survive activity recreation. No media, location, stable device identifiers or logs are attached. Sending remains an action in the email app; Copy draft is available without an email handler. Both flavors share this implementation, with no added dependency, SDK, permission or network client.

## Android Vitals

In Play Console, open **Monitor and improve → Android vitals → Overview** (or **Crashes and ANRs**). Basic crash/ANR reporting uses Android diagnostics and needs no added app SDK. Reports depend on users opting into usage/diagnostic sharing and installing the app through Google Play; data may be absent when the sample is too small. GitHub/F-Droid installs are outside these Play metrics. Optional anomaly notifications are configured in Console Settings → Notifications. See [Google’s official instructions](https://support.google.com/googleplay/android-developer/answer/9844486?hl=en).

This implementation preserves the tracking-free F-Droid flavor and adds no proprietary component. See [F-Droid inclusion policy](https://f-droid.org/docs/Inclusion_Policy/). Repository checks do not replace F-Droid review.

## Verification

The `adaptive` instrumentation check passed on:

| Platform | Window / display | Result |
|---|---|---|
| Android 16 / API 36 tablet emulator | 2560×1600 landscape | Layout, icons, editor and feedback passed |
| Android 16 / API 36 tablet emulator | 1600×2560 portrait | Same checks passed |
| Android 16 / API 36 tablet emulator | Display resized to 1000×1600 | Same checks passed |
| Android 15 / API 35 phone emulator | 1080×2400 portrait | Same checks passed |
| Android 15 / API 35 phone emulator | 2400×1080 landscape | Same checks passed |
| Android 16 / API 36 physical device, model 25060RK16C | 1280×2772 portrait | Same checks passed, DEV application only |

On the Android 16 tablet, actual freeform task bounds were changed from (100,100)–(1800,1200) to (160,80)–(1000,1500). Visual inspection confirmed side-by-side versus stacked controls, accessible capture controls and an effect editor aligned to the control column. This tests a resizable task; no physical foldable or hinge-occlusion coverage is claimed.

The checks assert 48dp mode targets, one selected radio state, a visible preview during editing, device details off by default and an intercepted email intent. No email was sent. Emulator screenshots and raw check outputs are retained locally in `verification/adaptive-window-20260909/` and are not published store screenshots.

The `adaptive-rotation` regression test passed on the Android 15 phone and Android 16 tablet. It exercises all four display rotations, output dimension swaps, 180-degree updates, TAP source/mode restoration without opening the camera, and feedback draft restoration. It exposed and led to a fix for TAP startup without a previous camera capability catalog.

Short silent MP4 capture also passed on the Android 15 phone emulator: 480×640 in portrait and 640×480 in landscape, saved to DCIM/5igna1. Emulator frame rates are not device performance guarantees.

Build verification includes the four flavor/build-type JVM suites (35 tests each), existing golden fixtures, lint, F-Droid debug/release, Play debug, translation placeholders, documentation links and repository checks. No release, store submission or signing-key change is part of this work.
