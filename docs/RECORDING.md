# Recording, app visibility and permissions

[Guides](README.md) · [日本語](RECORDING.ja.md) · [Formats](FORMATS.md)

Keep the capture screen active while recording. In 1.5.1 and later, switching apps, going Home or locking the screen ends recording and saves the recorded portion. This applies to camera MP4, TAP output MP4 and experimental RAW ZIP sequences. Returning opens the preview; press Record to begin a new file.

## What happens when the screen closes

The app stops accepting new frames and audio, finalizes the current recording, and releases camera, microphone, source playback and rendering resources. File finalization can take a short time after leaving the screen, especially for queued RAW frames. It is completion of the current save, not continued recording. A recording that is too short for the device encoder, storage failure or forced process termination may prevent a usable file from being saved.

Ordinary preview, LIVE measurements and imported-video playback do not run in the background. Photo processing waits while the app is hidden and resumes on return. There is no recording service or recording notification. Android can still retain an idle app process in memory; that does not mean capture is running.

## Permissions used

| Permission | Purpose | When needed |
|---|---|---|
| Camera | Camera preview, photos and videos | Camera capture |
| Microphone (`RECORD_AUDIO`) | Video sound or optional LIVE audio-level input | When that feature is enabled |
| Approximate/precise location | Optional capture location metadata | When location saving is enabled |

TAP reads selected media through the Android picker without requesting access to the entire photo library. The app does not request Internet access, foreground-service permissions, wake-lock permission or notification permission.

## Updating from 1.5.0

Version 1.5.0 allowed an active recording to continue after leaving the app. Version 1.5.1 removes that behavior and its service permissions. Existing captures and saved settings are retained. No permission declaration needs to be completed by app users.

TAP source-audio finalization can continue after capture stops; it combines already recorded video and selected source audio, without background capture.


Development update: every MP4 stores its recording-start signal settings for inspection and reuse in the saved-media viewer. Automatic segments share the initial settings of the recording; later edits and LIVE timing are not recorded. This applies to camera and TAP recordings, with or without audio. Metadata is attached after source-audio processing without re-encoding the video.
