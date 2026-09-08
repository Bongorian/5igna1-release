# Controls reference

[All guides](README.md) · [日本語](USAGE.ja.md)

Photo/Video changes capture mode. The center button or a volume key captures a photo or starts/stops recording. Tap the viewfinder to focus; swipe to select a single fault. Zoom offers 1×/2×; RAW uses 1×. The camera switch selects front/back, and the light is available where supported.

CLEAN selects no faults. A single selection clears the multi-fault route. FAULT POINTS lets you choose several faults in causal signal order. LEVEL maps to their individual mechanisms. Each adjustment card has a small number of named controls; their count differs by fault.

The background previews a draft. Apply saves it; Back, outside dismissal or leaving the app cancels it. Reset restores that fault's controls while keeping its identity. RESEED changes the identities of selected faults while retaining their controls and route. There is no random chain switch. Draft editing is unavailable during recording; direct committed selections affect subsequent frames. A JPEG always captures the displayed image, including a visible draft if a hardware shutter is used.

Time evolution belongs to each fault. LIVE optionally couples motion, audio, thermal, camera timing and processing pressure to that system. It starts off on cold launch. [LIVE settings](LIVE_FAULT.md).

The gear selects language, format, resolution, quality and optional GPS. JPEG saves the displayed signal at a supported live resolution (standard targets up to 2 MP; the largest offered signal is bounded to 8 MP). RAW original and processed DNG use separate exposures and may differ from the RGB preview. JPEG quality is 85/95/100. [Formats](FORMATS.md).

Normal MP4 recording uses H.264/H.265, device-supported resolution/fps, four quality levels and optional audio. Preview and encoder use the same processed images; the screen can present fewer frames than the encoder at high frame rates. MP4 splits around 3.5 GB and stops on low storage, camera interruption or leaving the foreground. Long multi-hour sessions are not validated. Experimental original RAW video saves silent DNG sequences in bounded queues and ZIP segments. [RAW video](RAW_VIDEO.md).

The bottom-left thumbnail opens the latest saved item in an external viewer. JPEG/DNG photos are in `Pictures/5igna1`, MP4 in `Movies/5igna1`, and RAW sequence ZIPs in `Download/5igna1`.

GPS starts off. Approximate and precise location are supported; if no recent fix is available, capture proceeds without GPS. JPEG records the displayed camera timestamp and fault snapshot in EXIF. Exposure/ISO fields are included only when the matching camera result is available. RAW has its own exposure metadata. There is no Internet permission, tracking or account registration. [Privacy](PRIVACY.md).
