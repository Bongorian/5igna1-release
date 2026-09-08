# Troubleshooting

Start with the symptom below. For a first run, processed JPEG with one effect is a useful baseline.

[All guides](README.md) · [Controls reference](USAGE.md)

## The camera is black or will not open

After closing a saved photo or video, the camera resumes automatically. The foreground preview makes up to three reconnect attempts after an interruption or six seconds without a presented frame, outside capture and cooling pauses. Tap the preview to retry if recovery stops.

Allow camera access in Android's app permissions. Close another app that may be using the camera, then reopen 5igna1. Try the other camera or a lower resolution in settings. If it still fails, note the device, Android version, and camera selection when reporting it.

## An effect does nothing

Check that you are not in CLEAN or original RAW. In a chain, the stage must be enabled and LEVEL and its relevant fault controls must be above zero. Some controls also have a no-change value, such as a ADDRESS ERROR offset of 0 bytes. STREAM ERROR can be quiet between incidents.

Choose **Apply** to keep an edit. Back, tapping outside, or leaving the app cancels the previewed changes.

## An effect disappears when I change modes

STREAM ERROR is available for both photos and videos. Processed RAW supports six sensor/readout/data/CFA faults. Switching formats preserves the route and controls. RAW applies only supported stages; the full catalog remains visible, with a format-switch action for RGB-only faults.

## RAW is unavailable

RAW support is camera-specific. The front camera may not support it even when the back camera does. RAW video needs additional capture and DNG checks to succeed. Use JPEG or normal MP4 when the selected camera does not offer the required mode.

## The DNG looks different from the preview

That is an expected limitation. The preview approximates RAW effects on processed RGB; a RAW developer interprets the DNG samples, color array, and metadata separately. Judge the developed file as well as the preview. A DNG also needs a RAW-capable app to open it.

## I cannot find a saved capture

Check `DCIM/5igna1` for photos, `DCIM/5igna1` for MP4, and `Download/5igna1` for RAW video ZIPs. The bottom-left thumbnail opens the latest saved item in the shared photo/video viewer. Swipe sideways to browse readable captures in the current and legacy 5igna1 folders. RAW ZIP opens externally. A gallery may take time to index a file or may not display DNG thumbnails.

## Video has no sound or will not play

Turn audio on and allow microphone access if you want sound. RAW video is always silent. If a player cannot decode HEVC/H.265, try a new recording using AVC/H.264.

A RAW video ZIP is a sequence of DNG files, not a playable MP4. Extract and develop it with a compatible workflow.

## Recording stops or the preview is slow

Keep 5igna1 in the foreground with the screen on. Check free space, reduce resolution or fps, and try fewer effect stages. Device temperature, the camera, encoder, and storage all affect sustained recording. Normal MP4 recording splits around 3.5 GB and stops when free space drops below 256 MB.

## LIVE FAULT does not react to sound or movement

LIVE couples measured input to the selected faults. Turn it on, enable the relevant source and choose a fault that uses it (motion for ROW ERROR, audio for VHS). Audio needs microphone permission; unavailable sensors contribute no input. Intrinsic fault motion continues even with LIVE off. CLEAN and original RAW taps bypass fault processing.

## Android says the app cannot be installed or updated

Confirm Android 12 or later, download the APK again, and check free space. For an update, use the same source and signing identity as the installed app. A similarly named development build is separate; a differently signed release with the same ID cannot update it. [Installation and signatures](INSTALLATION.md)

## Report a problem

Open an [issue](https://github.com/Bongorian/5igna1-release/issues) and include:

- App version and download source.
- Device model, Android version, and front/back camera.
- Photo/video mode, format, resolution, fps, and effect chain.
- Steps to reproduce, expected result, and what happened.

A short example helps. Review images and logs for faces, location metadata, or other personal information before attaching them. [Contributing](../CONTRIBUTING.md)
