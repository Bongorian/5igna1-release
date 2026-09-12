# PRO camera controls and workspace review

[日本語](PRO_CAMERA.ja.md) · [Usage](USAGE.md) · [Capture formats](FORMATS.md) · [Time model](TIME_MODEL.md)

This is the development implementation on `codex/pro-camera-workspace`, after 1.7.1. It adds real Camera2 adjustments and reorganizes the camera screen. It has not been published as an app release.

## Three independent choices

| Choice | What it changes | Entry |
|---|---|---|
| Photo / Video / TAP | Capture type or imported source | Labeled rail above the shutter; TAP requires Experimental |
| AUTO / PRO | Automatic camera defaults or capability-aware camera controls | Top bar; hidden for TAP and external capture requests |
| Normal / LIGHT / ADVANCED / EXPERT | Processing workload, fault editing and capture behavior | Settings → Processing modes; existing compatibility rules remain |

PRO does not enable Experimental or change FAULT parameters. PRO itself can use automatic exposure, automatic white balance and autofocus; its controls allow each supported function to be adjusted. Returning to AUTO restores the camera request template and automatic focus. Each lens has its own stored settings. AE/WB locks are temporary and are released on camera reconfiguration, leaving PRO, or a new session.

## Layout inventory

| Area | Controls and behavior |
|---|---|
| Top toolbar | One row: light, location, microphone, save format, resolution/fps, AUTO/PRO and Settings |
| Top status | Readiness, output/quality overview, errors and recording time; never overlays the preview |
| Preview | Native uncropped fit; portrait has a small FAULT badge and centered focal-length choices |
| Landscape | FAULT badge hidden; focal choices centered in the controls area, outside the preview |
| PRO strip | Measured shutter/ISO and equally sized Exposure, WB, Focus and Lens entries; unsupported entries omitted |
| FAULT summary | Leading chevron, selected/applied counts, LEVEL, LIVE and its settings; transparent compact row |
| Expanded FAULT | Chain, add/remove, random/reseed, LEVEL, live state/transport and optional ECHO |
| Mode rail | PHOTO / VIDEO / optional TAP as text tabs in every language |
| Capture row | Saved media, centered 80 dp shutter, front/rear switch |
| Landscape disclosure | Panel icon at the upper outer edge; an 88 dp rail retains the same shutter when collapsed, plus recording time above it |

Primary, standard, toolbar and compact controls use 48/40/32/28 dp heights. The 80 dp shutter is deliberately larger. Compact labels use 12 sp, with PRO labels fitting from 10–12 sp; editor choices and adjustment values use 14 and 18 sp. Spacing uses 4/8/12 dp steps. Disclosure icons change direction with their state; Open/Close remains in accessibility labels and tooltips, without visible action words.

The controls scroll when needed; capture tabs and shutter stay outside that scroll. Landscape collapse retains camera, FAULT and recording state. PRO/FAULT sheets reserve preview space; PRO adjustments save immediately, while FAULT keeps Apply/Cancel. The guide temporarily expands FAULT and restores its previous state afterward.

Horizontal swipes over the preview switch Photo/Video and retain the chain. Tapping requests autofocus when AF is active; this implementation does not set a touch-coordinate metering region. The focus indicator is centered, and manual focus is not overridden. Photo preview now requests continuous-picture AF; video requests continuous-video AF where supported.

## Capability and combination rules

The app reads characteristics and writable keys for the selected camera. Shared camera settings use the logical request. Keys advertised for independent physical-camera override use that override; its absence does not mean shared settings are unsupported. The selected sensor supplies control ranges. Unsupported controls are omitted. The camera's returned values are shown separately from requested settings, because requests can take time to settle or be quantized.

| Control | Availability and combination |
|---|---|
| Exposure compensation | Writable AE/compensation keys, AE ON, a nonzero step and range; automatic exposure only |
| AE lock | Advertised lock capability and writable AE/lock keys; automatic exposure only |
| Shutter + ISO | MANUAL_SENSOR, AE OFF, writable exposure/sensitivity/frame-duration keys and valid ranges; set together |
| Photo shutter range | Sensor exposure range bounded by maximum frame duration; long exposures reduce preview cadence and extend camera/capture timeouts |
| Video shutter range | Also bounded by the chosen video fps; temporarily constrained values do not erase the saved photo preference |
| High-speed video | PRO requests are suspended; the camera uses automatic controls |
| White balance | Only advertised automatic/preset modes; AWB lock only in supported automatic mode |
| Manual focus | MANUAL_SENSOR, AF OFF, writable focus key and positive focus range; distance labels only for calibrated lenses |
| Aperture | Multiple advertised values and a writable key; manual exposure is required |
| Optical ND | Multiple advertised filter-density values and a writable key |
| Stabilization | Default/off/optical where supported; explicit optical selection disables writable electronic stabilization to avoid competing controls |
| Antibanding | Only advertised choices, under automatic exposure; separate from FAULT's lighting-frequency model |

The implemented surface does not claim every vendor control: manual Kelvin/gains/color matrices, touch metering regions, scene/HDR extensions, noise reduction and edge processing are not exposed. Electronic stabilization and digital zoom are not offered. Native optical focal selection remains on the lens selector and uses advertised focal lengths only.

Camera2 semantics: [capture requests](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest), [camera characteristics](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics), [request builder](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest.Builder).

## Capture and persistence boundaries

- Changes are stored per lens and sent to the camera. Returned metadata may lag a requested adjustment; inspect the measured values before capture.
- JPEG continues to save a processed camera frame. RAW continues to request a separate exposure. PRO does not introduce a different photo pipeline or bypass the fault chain.
- FAULT EXPOSURE remains image processing, and HOLD remains a fault-clock control. Neither substitutes for real AE lock or a manual shutter setting.
- TAP bypasses PRO. Imported media retains the camera/color processing already present in that file.
- External capture requests use automatic camera defaults and preserve the user's PRO preference for normal use.
- Changes and mode/lens switches are blocked while recording or processing a photo. Confirmation vibration remains suppressed for audio-enabled recording.
- Saved-media “Use these settings” still restores FAULT settings, not real-camera PRO settings.
- If the camera rejects a repeating PRO request, the app returns to AUTO, retains the per-lens preference, and displays the rejection. It does not report the rejected settings as applied.

## Review and verification

[Visual review and original screenshots](UI_REVIEW.md) covers Japanese and English on a dedicated 1280×2772, 480 dpi emulator, including portrait, landscape, PRO, FAULT, collapsed controls and recording stop. The automated review checks an unclipped 80 dp shutter, reuse of the same capture view across collapse, status outside the preview and fully visible PRO entries. UI captures were inspected after fixing clipped PRO cards and the wrapped MP4 label.

Unit tests cover exposure/video ranges, AE/AWB dependencies, unsupported/high-speed behavior, serialization and shutter scaling. On the USB-connected 25060RK16C, offscreen Camera2 checks passed for physical:0:2 and physical:0:3: 8,000,000 ns exposure, ISO 200, daylight WB (5), then restored AUTO requests and results. The emulator matches display size/density, not that device’s camera hardware or system skin.
