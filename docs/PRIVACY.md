# 5igna1 Privacy Policy
Last updated: September 12, 2026

Provider: Bongorian
Contact: dennosamurai@gmail.com
App: 5igna1 (com.bongorian.signa1)

## 1. Processing on your device
5igna1 is a camera app that applies effects simulating imaging faults to photos and videos. Images are processed and saved on your device. The app has no developer-operated server, ads, analytics SDK or account registration. It does not automatically send photos, videos, audio or location to the developer.

## 2. Camera and microphone
Camera permission is used for previews and capture. Microphone permission is requested for video with sound or optional LIVE audio input. Silent video does not require the microphone. When you leave the capture screen, switch apps or lock the screen, recording stops and the app finalizes the recorded file. Camera, microphone, preview and source playback are released. Recording does not restart automatically when you return. The app uses no foreground service, wake-lock permission or notification permission. Photo processing waits while the app is hidden and resumes when you return.

LIVE FAULT can use optional motion, audio level, camera timing, temperature and process-load inputs to vary fault state. Microphone samples used for LIVE are reduced to levels on the device and are not saved as an audio recording unless video sound is enabled. Derived fault state may be included in capture metadata. Independently of LIVE, while the camera is active the app reads thermal status/headroom, battery temperature, memory capacity, available CPU core count and rendering time to reduce workload. These measurements are processed locally and are not sent to the developer.

## 3. Optional location
Saving location is off by default. If you enable it and grant device permission, an available capture location is written to photo GPS metadata and video location tags. Location is requested only while you use the app. Location services are provided by the operating system and are subject to that provider's settings and policies.

## 4. Saved data and other apps
Photos are saved in DCIM/5igna1 and videos in DCIM/5igna1. Photo metadata includes the date, device model, exposure, ISO and effects. Location is also included when enabled and available. Saved photos and videos are previewed inside the app, using readable captures in the 5igna1 folders. Choosing Open in another app, or opening a RAW ZIP, lets your chosen viewer access that item. If gallery or operating-system backup and synchronization are enabled, the media may be synchronized to those services. Before sharing, check metadata such as location and the receiving app's settings.

RAW video saves original, silent DNG sequences and timestamps as ZIP files in Download/5igna1. If location is enabled, an available location is included in the DNG files. Compatibility checks process one RAW image on the device without saving that test image as a file.

Experimental TAP reads only media selected with the system picker, without modifying or uploading the originals. Photo processing pauses while the app is hidden.

## 5. Retention and deletion
Captured media remains in shared device storage until you delete it using your photo or file app. Shared media may remain after uninstalling 5igna1. Settings such as effects, quality, location preferences and the reference to the last saved item are stored in app storage. You can remove them by clearing app data in device settings or uninstalling the app. Processing files are removed when processing finishes. Caches left after interruption are managed by the operating system or removed by clearing app data.

## 6. Data protection
The app uses operating-system permissions and app-storage protection. It does not communicate over external networks. Other apps with access granted on your device may be able to view shared media. You control the device lock and permission settings.

## 7. Contact and changes
Settings offers Feedback / report bug. You review an editable message and choose whether to include the displayed app version, Android version, manufacturer and model (off by default). Opening your email app passes this draft to that app; you decide whether to send it. Copy draft writes the displayed recipient, subject and message to the system clipboard. The app does not attach media, location, identifiers or logs, and does not send reports automatically. Your email service handles messages you send under its own policies.

If you contact us by email, your message and email address are used to reply and provide support. Support records are deleted when no longer needed. This policy will be updated if data handling changes, with an in-app notice when appropriate.

[English](PRIVACY.md) · [日本語](PRIVACY.ja.md) · [Guides](README.md)

GPU/driver identity and a short synthetic rendering measurement are stored locally to recommend resolution and preview frame rate. No camera image is used by this measurement and no results are transmitted.

Imported TAP video audio is read only from the selected media and retained in processed recordings without microphone permission. With resolution-linked audio enabled it is converted locally. Temporary conversion files are removed after finalization; clearing app data removes interrupted cache files.

MP4 videos also contain the signal chain and effect settings at recording start. These settings remain in the file when it is shared.

When another app explicitly requests a capture, you review and accept the JPEG or MP4 before it is returned. Only that new capture is handed to the requesting app, with no location tags. Private staging files are deleted after acceptance or cancellation. If a video request supplies no destination, the accepted video remains in shared DCIM/5igna1 storage. The receiving app and its storage provider apply their own policies.
