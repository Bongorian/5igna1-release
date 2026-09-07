# Controls reference

[All guides](README.md) · [Quick start](GETTING_STARTED.md) · [日本語](USAGE.ja.md)

## Capture and camera controls

Use Photo/Video to change capture mode. The center button or a volume key takes a photo or starts/stops recording. Tap the live image to focus; swipe left/right to select a single effect.

Digital zoom offers 1×/2×. RAW uses 1×. The bottom-right camera control switches front/back cameras, and a light is available where the camera supports it. Selecting a camera without RAW support returns the photo format to JPEG.

The top-right gear opens capture settings. Its first item selects the app language: follow device, Japanese, English, or Simplified Chinese. Unsupported device languages fall back to English. Applying a language change preserves capture settings, effects, and LIVE state.

The bottom-left thumbnail opens the most recently saved item in an external viewer. DNG needs a RAW developer. Use your gallery or file app to browse older captures.

## Effects and chains

CLEAN disables processing. Selecting a single effect, including by swiping, clears the chain.

The chain selector lets you enable several stages in a fixed processing order. Open the adjustment screen for each selected stage to change strength and its parameters. Opening an inactive stage's controls does not enable it.

Effective stage strength is **overall strength × stage strength**. Both controls range from 0–100%. The adjustment screen explains when a stage is off or its effective strength is zero.

## Previewing an edit

While adjusting, the background shows a trial of your changes.

- **Apply** commits and saves the edit. During recording, it affects subsequent recorded frames.
- **Back, tapping outside, or leaving the app** cancels the edit and restores the committed state.
- Photos and recorded frames use committed settings, not unconfirmed trial edits. Close the adjustment screen before taking a photo.
- Reset restores that stage's defaults. Change placement updates the spatial pattern for effects that support it.

Each stage remembers its adjustment values separately from whether it is selected. Mode changes remove incompatible stages, and switching back does not re-enable them automatically. Original RAW keeps settings but bypasses processing.

## Random and LIVE FAULT

Short-press Random for a single random effect. Long-press it for a 2–4-stage chain. These actions also vary parameters; overall strength is chosen between 35% and 85%.

LIVE FAULT adds changes over time. It starts off, and its settings control interval, probability, duration, change amount, smoothing, and optional chain switching. Changes return to the manual state when they end or when you turn LIVE off. [Full LIVE FAULT guide](LIVE_FAULT.md)

## Photo and video settings

Photos can be processed JPEG, original DNG, or processed DNG. JPEG quality choices are 85/95/100. [Choosing a format](FORMATS.md)

Normal video uses AVC/H.264 or HEVC/H.265, with device-supported resolutions and frame rates, four bitrate settings, and optional audio. There is no app-level total recording-duration limit, but recording depends on battery, heat, storage, and camera/OS interruptions. MP4 splits around 3.5 GB; recording stops below 256 MB free space or when the app leaves the foreground/screen turns off. Multi-hour recording has not been validated.

RAW video is an experimental, original and silent DNG sequence saved in ZIP segments. It bypasses effects and LIVE FAULT. [RAW video](RAW_VIDEO.md)

## Location and metadata

GPS tagging starts off. Open GPS, enable capture location, and grant Android location permission. Approximate and precise permission both work. The interface distinguishes permission denied, location services off, acquiring a fix, and an available location. Refresh to request a new fix.

Photos and videos use locations no older than two minutes; video uses the location at recording start. If no fix is available, capture proceeds without GPS. Photos also include metadata such as time, device model, exposure, ISO, focal length, and applied effects.

The app has no Internet permission and requests neither background location nor access to your entire media library. OS location services, galleries, and backup services have their own behavior. [Privacy](PRIVACY.md)
