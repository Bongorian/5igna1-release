# Controls reference

[Guides](README.md) · [日本語](USAGE.ja.md) · [First photograph](GETTING_STARTED.md)

## Capture a photo or video

Choose Photo or Video, then use the center button or a volume key to capture or start/stop recording. Tap the viewfinder to focus. Digital zoom is disabled in 1.7.0. On the current development branch, the lower-right camera icon switches front/rear. Focal-length buttons in the viewfinder select lenses on the current side, using approximate 35 mm equivalents when available. When individual sensors are available, selection pins a physical sensor instead of using the combined logical route. The last successfully used camera is remembered. A lens that exposes multiple controllable optical focal lengths gets an optical adjustment control; it selects only advertised focal lengths, with no digital interpolation. Fixed-focal-length lenses have no zoom control. Available sizes and RAW support follow the selected sensor. Selection is disabled during capture and in TAP. Some manufacturers do not expose every built-in camera to other apps.

In the unreleased PRO workspace, the top toolbar contains light, save format, resolution/fps, AUTO/PRO and Settings. Location and audio controls occupy the single top toolbar and remain accessible when OFF. Readiness and output settings appear immediately below it, outside the preview. In landscape, capture controls sit to the right of the preview. Audio starts OFF; experimental RAW video is always silent. [PRO controls and layout inventory](PRO_CAMERA.md).

The unreleased workspace places labeled capture modes above the centered shutter, with saved media on the left and front/rear switching on the right. LIVE sits in the collapsible FAULT bar. Tap the format label to cycle JPG/RAW on supported cameras. Video stays MP4 until RAW video switching is enabled in Settings; it can then cycle MP4/RAW ZIP where supported. Switching back to MP4 keeps that opt-in; disabling it in Settings returns recording format to MP4. TAP exports JPG or MP4 according to the imported media. Changing format preserves the selected fault route and its controls. RAW applies only supported faults; MEDIA and DISPLAY remain visible in the catalog with a format-switch action when needed. [Formats and capture limits](FORMATS.md).

Keep the capture screen active while recording. Switching apps, going Home or locking the device stops recording and saves the recorded portion. Returning opens preview without restarting recording. This applies to camera, TAP and RAW recording. [Recording and permissions](RECORDING.md).

## Build and adjust a fault route

Expand the FAULT bar below the preview to see selected faults in processing order. Tap **+** to add a fault, then tap its chip to adjust it. Removing every fault gives a clean image. In the unreleased workspace, horizontal swipes switch Photo/Video while preserving the fault route. LEVEL changes the selected model's mechanisms; individual control names describe their effects.

The editor keeps the live preview above its controls. The checkmark keeps an edit; ×, Back, outside dismissal or leaving the app cancels it. Apply or discard edits before capturing. If you explicitly change capture format inside the editor, cancelling effect edits does not undo that format choice.

The shuffle button creates a route of 2–5 available faults, their controls and an overall LEVEL. On the capture screen it applies immediately; in the editor it remains a draft. Hold the capture screen's shuffle button to reseed the route without changing its controls. Reset restores the focused fault's controls. Draft editing and random route generation are unavailable during recording. [SEED and RESEED](SEEDS.md).

MEDIA contains VHS, DVD, Digital thru and Analog thru models. DISPLAY contains CRT, Digital thru, Network and LED models. Their controls depend on the chosen model. [Effect models](EFFECTS.md).

## Control time and inputs

LIVE sets fault-time progression, speed/reverse, loops, update intervals and variation patterns. PAUSE freezes fault evolution while the camera continues; TRIGGER creates a temporary fault. FAULT STATE shows time and event meters below the preview. An event meter at 0% does not imply that fixed image processing or intrinsic motion is off.

Motion, audio and other device inputs are optional. LIVE starts OFF on cold launch. TIME ECHO introduces past frames using one probability control. [LIVE settings](LIVE_FAULT.md).

## Use an imported image or video

Enable Experimental features in Settings to show **TAP**. Select an image or video with Android's picker. It enters the route after READOUT, before DATA; SENSOR and READOUT faults are bypassed for that source. The source file is not modified.

For a video source, playback and output recording have separate controls. Play/Pause controls the source; the shutter starts/stops the processed MP4 output. A paused source can still be recorded with evolving effects. Leaving the capture screen stops both playback and recording. [Experimental features and TAP](EXPERIMENTAL_SIGNALS.md).

## Change settings

Settings save immediately. They include language, capture format, resolution, quality, optional location metadata and processing modes. The Experimental section is between saved-information options and App information. Experimental features, ADVANCED, EXPERT and linked audio start OFF; saved choices are retained.

ADVANCED exposes internal parameters and retains displayed frames for JPEG capture. With it OFF, capture uses the current processed output and may differ from the last displayed frame. RAW photos use a separate exposure and differ from the RGB preview. [ADVANCED MODE](ADVANCED_MODE.md).

The app recommends a conservative initial size for the device; manual sizes and Maximum remain available. Automatic preview workload reduction limits work under load. EXPERT removes app-level preview and cooling limits. [Performance modes](PERFORMANCE.md).

The eight-step Quick start highlights the actual controls and offers isolated LEVEL and format-switching practice. It covers capture icons and resolution long-press, SEED/RESEED, LIVE timing, TAP and immediately saved Settings. The camera pauses during the guide. Skip, close or finish to dismiss it; replay it from Settings. It works offline in English, Japanese and Simplified Chinese.

## View saved captures

The bottom-left thumbnail opens the photo/video viewer. Swipe sideways to browse, pinch to enlarge a photo, and swipe down or tap × to return to capture. Videos support Play/Pause and seeking. The ↗ button opens the current item in another app. DNG uses an available system thumbnail; RAW ZIP opens externally.

JPG, DNG and MP4 are saved in `DCIM/5igna1`; RAW sequence ZIPs are in `Download/5igna1`. Existing files remain in their original folders. Optional GPS metadata is saved only when enabled and a location is available. [Privacy](PRIVACY.md).

If the camera is interrupted or no preview is presented for six seconds, the app attempts to reconnect up to three times while active, outside capture and cooling pauses. Tap the preview to retry after those attempts. Reconnecting never restarts recording. [Troubleshooting](TROUBLESHOOTING.md).

Settings introduced in 1.6.1: Settings → Modes → LIGHT MODE reduces display-only processing to the view size. Fresh setups on constrained devices start with LIGHT; it excludes ADVANCED/EXPERT; saved dimensions remain selected. [Details and exceptions](PERFORMANCE.md).

## Reuse saved signal settings

In the saved-media viewer, the SIGNAL row shows the current photo or video’s recorded chain. Tap it to inspect the metadata and choose **Use these settings**. This restores the recorded chain, LEVEL, controls, seeds and detailed overrides, plus the Experimental switch when recorded. It leaves capture format, resolution and LIVE settings unchanged. LIVE time, measured inputs and source media cannot be replayed from these records, so the resulting image is not guaranteed to match. MP4 recordings made with 1.7.0 or later store the recording-start settings, including in each automatically split file; later changes are not recorded. Older videos and other media without usable metadata cannot supply settings. Incomplete or unsupported records remain viewable but cannot be applied. The original media is never modified. Video settings are stored in an MP4 user-extension box; other editors may remove this data when exporting.

### Development control update

TAP and its source controls stay hidden unless Experimental features is enabled; playback and file selection appear only while TAP is active. Confirmation vibration is suppressed during audio-enabled recording, including the fault trigger and recording stop.

The chain catalog uses full-width rows with a description and selection state. Tap the row to add/remove; the sliders icon opens explanation and adjustment separately, including an Add action for an unselected available fault. The upper + icon returns to the catalog. The checkmark commits the draft and the close icon cancels it. Selection does not change processing order. The editor and live chain inspection show selected/applied counts; inspection retains bypassed stages with an explanation. Event percentages still refer only to transient events, not overall effect strength.
