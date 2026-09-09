# Heat and workload

[Guides](README.md) · [日本語](PERFORMANCE.ja.md)

Version 1.4.0 starts with **Recommended for this device**. Camera and encoder capabilities select an actual supported output within these initial pixel budgets, preferring video at up to 30 fps:

| Starting tier | JPEG budget | Video budget |
|---|---:|---:|
| Android low-RAM flag, unknown RAM, less than 5 GiB RAM, or at most 4 available CPU cores | 921,600 pixels | 307,200 pixels (VGA) |
| Otherwise, less than 8 GiB RAM or fewer than 8 available cores | 1,440,000 pixels | 921,600 pixels (HD) |
| Otherwise | 2,073,600 pixels | 2,073,600 pixels (Full HD) |

These are conservative starting hints, not CPU/GPU benchmark scores. Core count does not measure core speed or GPU performance. If no supported mode fits, the catalog supplies a fallback. Existing manual sizes are retained. Measured rendering cost and thermal feedback then adjust preview cadence; they do not silently change a recording resolution.

## Normal capture and ADVANCED MODE

ADVANCED defaults OFF. Normal capture reuses one processed output texture, preserving every selected FAULT pass. The GL thread reads an immutable JPEG snapshot from the current processed image when it handles the shutter; exact agreement with the last displayed frame is not required. Preview and normal video share this output without retaining a display history.

ADVANCED ON retains the three-slot displayed-frame history and pins the displayed image before queuing the shutter. Switching modes releases old output textures and restarts the camera. Both paths retain immutable capture state; RAW remains a separate exposure. Relative to three allocated RGBA8 history textures, one output needs two fewer full-size textures (about 15.8 MiB at 1920×1080, excluding driver and camera memory). This is a storage calculation, not measured total memory or battery savings. FAULT sampling and per-frame processing cost remain unchanged.

Settings → **Workload and recommendations** → **Use recommended photo and video settings** restores these selections and standard video quality. Resolution pickers also offer Recommended alongside manual sizes and Maximum. On the first upgrade to this policy, the previous maximum-photo default becomes Recommended; exact saved sizes are retained. A subsequent explicit Maximum choice is retained. Empty legacy video selections resolve to Recommended. A selected JPG size is also the saved JPG size; automatic frame pacing does not change it mid-session. RAW still uses a separate exposure and supported sensor faults.

## EXPERT MODE

Settings → Modes → **EXPERT MODE** removes app-level thermal pauses, adaptive workload caps and timed preview frame skipping. Photo preview requests the fastest advertised normal capture rate compatible with the chosen stream; video uses its selected fps. Actual throughput remains limited by the camera, GPU, display and retained-frame availability. Resolution choices are preserved, so a smaller manual size can still be faster. EXPERT is independent of ADVANCED (internal parameters and displayed-frame capture), is saved as soon as you toggle it, and starts off by default.

EXPERT skips workload thermal polling and never stops a recording in response to the app's thermal thresholds. Turning it off restores automatic control. Heat and battery use can increase. Android/device thermal protections and supported-format, memory-buffer and storage checks remain in effect. This mode does not overclock the device or bypass Android's controls.

The settings sections are Modes, Workload and recommendations, Photo, Standard video, experimental RAW video, Saved information and App. Language and privacy information are grouped under App.

## Runtime control

With EXPERT off, the app monitors Android thermal status, thermal headroom when available (no faster than once per 10 seconds), battery temperature as a fallback signal, time spent submitting/rendering frames, and image size × chain length. Thermal monitoring operates even when LIVE or its temperature input is off. It does not enable faults, change the chain or fabricate thermal glitches. Missing thermal readings remain unknown; measured rendering cost and conservative budgets still apply.

Preview processing has a 24 fps ceiling and steps down through 20, 15, 12, 8 and 6 fps. Lowering the limit skips both fault compilation and expensive image-processing passes on unused preview frames; fault time continues advancing. The camera request is also reduced toward 15 fps for a slowed photo preview where supported. Rate recovery is gradual, with at least 15 seconds of headroom before each increase. Slow camera delivery can make actual preview rate lower than the displayed limit.

Ordinary recording keeps its chosen dimensions and encoder cadence; reducing preview refresh saves display work, while each recorded camera frame is still processed. Recommended recording resolution is therefore the main reduction for recording workload. No resolution or codec switch is attempted inside a recording.

At Android CRITICAL thermal status or a battery reading of 48°C or higher, the app finishes recording and pauses camera repetition and LIVE input acquisition. It resumes after at least 30 seconds of sufficiently cool readings. The other battery thresholds (40/42/45°C), thermal-status tiers and headroom thresholds are conservative app workload heuristics, not hardware temperature guarantees. Cooling does not start a new recording automatically.

## Validation limits

The controller is tested with simulated heat readings and a real camera, without deliberately heating a phone. A Pixel 6 Pro is not attached, so lower temperature or sustained recording behavior on that model still needs confirmation. Recommended sizes depend on the camera/encoder catalog; thermal APIs also vary by device. The approach follows [Android's thermal workload guidance](https://developer.android.com/games/optimize/adpf/thermal), with rendering cost and battery readings supplementing platform thermal signals.
