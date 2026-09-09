# 5igna1 Privacy Policy
Last updated: September 9, 2026

Provider: Bongorian
Contact: dennosamurai@gmail.com
App: 5igna1 (com.bongorian.signa1)

## 1. Processing on your device
5igna1 is a camera app that applies effects simulating imaging faults to photos and videos. Images are processed and saved on your device. The app has no developer-operated server, ads, analytics SDK or account registration. It does not automatically send photos, videos, audio or location to the developer.

## 2. Camera and microphone
Camera permission is used for previews and capture. Microphone permission is requested for video with sound or when you enable the optional LIVE audio input. You can record silently with audio off. Only a user-started output recording may continue when you leave the app. A dedicated foreground service supports that session; notification permission enables its recording notification and Stop action. Ordinary preview and imported-video playback stop while hidden.

LIVE FAULT can use optional motion, audio level, camera timing, temperature and process-load inputs to vary fault state. Microphone samples used for LIVE are reduced to levels on the device and are not saved as an audio recording unless video sound is enabled. Derived fault state may be included in capture metadata. Independently of LIVE, while the camera is active the app reads thermal status/headroom, battery temperature, memory capacity, available CPU core count and rendering time to reduce workload. These measurements are processed locally and are not sent to the developer.

## 3. Optional location
Saving location is off by default. If you enable it and grant device permission, an available capture location is written to photo GPS metadata and video location tags. Location is requested only while you use the app. Location services are provided by the operating system and are subject to that provider's settings and policies.

## 4. Saved data and other apps
Photos are saved in DCIM/5igna1 and videos in DCIM/5igna1. Photo metadata includes the date, device model, exposure, ISO and effects. Location is also included when enabled and available. Saved photos and videos are previewed inside the app, using readable captures in the 5igna1 folders. Choosing Open in another app, or opening a RAW ZIP, lets your chosen viewer access that item. If gallery or operating-system backup and synchronization are enabled, the media may be synchronized to those services. Before sharing, check metadata such as location and the receiving app's settings.

## 5. Retention and deletion
Captured media remains in shared device storage until you delete it using your photo or file app. Shared media may remain after uninstalling 5igna1. Settings such as effects, quality, location preferences and the reference to the last saved item are stored in app storage. You can remove them by clearing app data in device settings or uninstalling the app. Processing files are removed when processing finishes. Caches left after interruption are managed by the operating system or removed by clearing app data.

## 6. Data protection
The app uses operating-system permissions and app-storage protection. It does not communicate over external networks. Other apps with access granted on your device may be able to view shared media. You control the device lock and permission settings.

## 7. Contact and changes
If you contact us by email, your message and email address are used to reply and provide support. Support records are deleted when no longer needed. This policy will be updated if data handling changes, with an in-app notice when appropriate.

RAW video saves original, silent DNG sequences and timestamps as ZIP files in Download/5igna1. If location is enabled, an available location is included in the DNG files. Compatibility checks process one RAW image on the device without saving that test image as a file.

[All guides](README.md) · [日本語](PRIVACY.ja.md)

Experimental TAP reads only media you select with the system picker; it does not upload or modify the originals. Preview and source playback stop when the app is hidden. Only a user-started output recording may continue in the background; a foreground service and optional notification support that session. Photo processing pauses while hidden.
