# Experimental RAW video

Recording stops and saves when you leave the capture screen or lock the device. [Recording and permissions](RECORDING.md).

RAW video records **original, silent DNG frames in a ZIP**, rather than a playable MP4. Use it when you want to develop a sequence outside the app.

[All guides](README.md) · [Choosing a format](FORMATS.md) · [日本語](RAW_VIDEO.ja.md)

## Record and open

Enable RAW video / DNG sequence in capture settings, then switch to video mode. It starts off by default and is available only when the selected camera passes continuous RAW and DNG checks.

The app saves to `Download/5igna1`, splitting ZIP segments around 3.5 GB. Each segment contains DNG frames, `timestamps.csv`, `manifest.json`, and a readme. Effects and LIVE FAULT are bypassed; the preview is the camera's developed image.

Extract the ZIP and load the DNG sequence into a compatible RAW workflow. A normal MP4 player cannot open it directly. This format does not claim CinemaDNG-container compliance. Audio and simultaneous processed video are not recorded.

Stopping or leaving the app stops accepting frames, finishes queued frames, and closes the ZIP. If enabled and available, the location at recording start is written into the DNGs.

## Speed and dropped frames

The target is 1 fps up to the camera-reported limit, capped at 30 fps. A reported limit is not a measured sustained rate. DNG encoding, storage, temperature, and camera behavior can reduce throughput.

The app limits a copied frame to 64 MiB and queues at most two frames. Frames that exceed the queue or cannot be paired with capture metadata are dropped. The display and manifest count drops within the app; they cannot count frames lost before the camera delivered them.

Use sensor timestamps in the CSV to inspect real intervals and the manifest for the segment's average rate. If an editor imports at a constant rate, decide there how to handle gaps in the sequence.

Low space or a write error stops recording. Completed segments remain; the incomplete segment is removed. Unpublished RAW ZIPs left after process termination are cleaned up at the next launch.

## Compatibility checks

The app examines Camera2 RAW capability, normal-mode RAW_SENSOR sizes, DNG-compatible dimensions, frame timing, and stall duration. It excludes maximum-resolution-only still modes.

When RAW video is selected, it checks or attempts the RAW-plus-preview session and encodes one DNG from matching image data and capture metadata. The check image is not saved. If the check fails, the app explains the reason, returns to normal video, and disables that camera's RAW video configuration for the current launch.

## Tested scope

On REDMI K80 Ultra, back 4096×3072 and front 2592×1944 modes reported a nominal ceiling of 12 fps. A short back-camera test at a 2 fps target saved two DNG frames, with ZIP contents, dimensions, timestamps, and manifest verified. Sustained 12 fps recording has not been tested.

An Android Emulator exposed RAW but lacked required DNG metadata. The app rejected that configuration and returned to normal video as intended.

## Implementation references

The implementation uses continuous Camera2 RAW_SENSOR capture and Android's DngCreator. Normal MP4 instead compresses developed images through AVC/HEVC.

- [Camera2 RAW capability](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_RAW)
- [StreamConfigurationMap](https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap)
- [DngCreator](https://developer.android.com/reference/android/hardware/camera2/DngCreator)
