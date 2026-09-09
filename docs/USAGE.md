# Controls reference

On the first launch of a build with the tutorial, a five-page quick start appears before the camera permission prompt. Use Next / Back, or Skip at any time. Finishing, skipping or closing it prevents automatic display on later launches. Open Settings → Quick start to read it again without applying or discarding your pending settings edits. The guide is available in English, Japanese and Simplified Chinese, works offline, and keeps its page when the activity is recreated. Existing users see it once when upgrading to the tutorial build.

[All guides](README.md) · [日本語](USAGE.ja.md)

Photo/Video changes capture mode. The center button or a volume key captures a photo or starts/stops recording. Tap the viewfinder to focus; swipe to select a single fault. Zoom offers 1×/2×; RAW uses 1×. The camera switch selects front/back, and the light is available where supported.

The selected chain sits below the preview. Tap + to add faults, or tap a selected fault to adjust it. Removing every fault gives a clean image. Swiping the preview selects a single fault and clears the multi-fault route. LEVEL maps to their individual mechanisms. Each adjustment card has a small number of named controls; their count differs by fault.

The editor reserves its own area below the live preview. The + button opens all 13 faults in a two-column grid; the selected route stays visible above it in causal order. Tap a selected fault to adjust one set of controls at a time. The checkmark saves the draft; ×, Back, outside dismissal or leaving the app cancels it. Apply or discard edits before capturing.

The shuffle icon generates 2–5 available faults, their control values and a new overall level. On the main screen it applies immediately; inside the editor it remains a draft. Hold the main random button to reseed the current route without changing its controls. Reset restores the focused fault's controls. Draft editing and random chain generation are unavailable during recording.

LIVE controls fault-time progression, speed/reverse, second-based loops and update intervals, and the variation pattern. PAUSE freezes fault evolution while the camera continues; TRIGGER creates a temporary fault. Device inputs remain optional. LIVE starts off on cold launch. [LIVE settings](LIVE_FAULT.md).

The format badge at the top switches JPG or RAW (MP4 / RAW ZIP in video mode). The catalog keeps VHS / MEDIA and CRT / DISPLAY visible in RAW, with an explicit format-switch button for effects that require processed images. Switching formats preserves the selected route and controls; RAW applies only supported faults. Switching format in the editor keeps the draft open; cancelling afterwards discards effect edits but keeps the explicitly selected capture format.

The gear selects language, format, resolution, quality and optional GPS. JPEG saves the displayed signal at a supported live resolution (the default is a conservative device recommendation near 1–2 MP; manual sizes and Maximum remain available). RAW DNG uses a separate exposure and may differ from the RGB preview. JPEG quality is 85/95/100. [Formats](FORMATS.md).

Normal MP4 recording uses H.264/H.265, device-supported resolution/fps, four quality levels and optional audio. Preview and encoder use the same processed images; the screen can present fewer frames than the encoder at high frame rates. MP4 splits around 3.5 GB and stops on low storage, camera interruption or leaving the foreground. Long multi-hour sessions are not validated. Experimental original RAW video saves silent DNG sequences in bounded queues and ZIP segments. [RAW video](RAW_VIDEO.md).

The bottom-left thumbnail opens a shared photo/video preview. Swipe sideways to browse captures, pinch to enlarge a photo, and swipe down or tap × to return to the camera. Videos support play/pause and seeking. The ↗ button opens the current item in another app. RAW DNG previews use the available system thumbnail; RAW ZIP opens externally. JPEG/DNG photos are in `DCIM/5igna1`, MP4 in `DCIM/5igna1`, and RAW sequence ZIPs in `Download/5igna1`.

GPS starts off. Approximate and precise location are supported; if no recent fix is available, capture proceeds without GPS. JPEG records the displayed camera timestamp and fault snapshot in EXIF. Exposure/ISO fields are included only when the matching camera result is available. RAW has its own exposure metadata. There is no Internet permission, tracking or account registration. [Privacy](PRIVACY.md).

Enable **ADVANCED MODE** in Settings, then tap a selected fault to inspect or fix its internal values. Turning the mode off hides the advanced controls but preserves fixed values. [Full parameter reference](ADVANCED_MODE.md).

New JPG, DNG and MP4 captures share **DCIM/5igna1**. Existing files stay in their original folders. Experimental RAW ZIP sequences remain in **Download/5igna1**.

The development build includes conservative device recommendations and automatic preview workload reduction. [Heat and workload](PERFORMANCE.md).

Settings groups ADVANCED and EXPERT under Modes. ADVANCED exposes internal values; EXPERT removes app-level preview workload and cooling limits for maximum available speed. [Performance modes](PERFORMANCE.md).

The top toolbar contains the format menu, light and GPS icons, an always-visible video-audio icon, and settings. Light/GPS icons turn lime when enabled; a long press reveals their status. The format menu opens directly below its button. Branding is kept out of the capture workspace; app information is in Settings. FAULT STATE shows time and transient event meters beneath an unobscured preview. A 0% event meter does not mean that intrinsic motion or fixed processing is off.

If camera access is interrupted or no preview is presented for six seconds, the app makes up to three reconnect attempts while in the foreground, outside capture and cooling pauses. Tap the preview to retry after those attempts. Reconnecting does not restart a recording. The last thumbnail is retained across a return from saved-media viewing when its URI is unchanged.
