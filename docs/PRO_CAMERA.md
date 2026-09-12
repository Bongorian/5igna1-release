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

| Area | Controls and behavior | Reason for the placement |
|---|---|---|
| Top bar | Torch, save format, output resolution/fps, AUTO/PRO, Settings | Capture setup stays above the image, without mixing it with FAULT |
| Status line | Readiness, errors and recording time | Short user-facing status; the processing engine still retains diagnostics |
| Preview | Uncropped fit, compact FAULT indicator, centered focal-length buttons at the bottom | Preserve framing; lens selection remains close to the image |
| Top row | Location and microphone controls in the single top row, including OFF state | Show relevant capture state without permanent inactive icons |
| PRO strip | Actual shutter/ISO and shortcuts to supported Exposure, WB, Focus and Lens controls | Adjust real camera behavior while retaining a visible preview |
| FAULT bar | Selected/applied count, LEVEL, expand/collapse; LIVE toggle and settings | Keep creative effects available without always occupying the capture area |
| Expanded FAULT panel | Chain, add/remove, random/reseed, LEVEL, live state and transport, optional ECHO | Existing behavior grouped into one place |
| Mode rail | PHOTO, VIDEO, optional TAP in every app language | Stable position directly above capture |
| Shutter row | Saved media on the left, capture in the center, front/rear on the right | Capture remains centered regardless of source, PRO or expanded panels |

Portrait uses the order above. Wide windows put capture tools in the right column and reserve the remaining area for preview. The controls area scrolls if needed, while the mode rail and shutter stay outside that scroll. The PRO and FAULT editors use a bottom/side sheet with preview space reserved. Closing PRO retains adjustments; FAULT still uses Apply/Cancel. The quick-start guide temporarily expands FAULT to reveal the controls it explains, then restores the previous expansion state.

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

Review AUTO/PRO, collapsed/expanded FAULT, Photo/Video, TAP opt-in, portrait and landscape, and changing between lenses with different capabilities. Confirm that the capture controls remain easy to find and that measured values are distinct from settings. Physical-device visual review is pending under the current no-computer-use instruction.

Automated coverage includes sensor/video range constraints, AE/AWB dependencies, unsupported/high-speed behavior, per-lens serialization, and logarithmic shutter controls. A separate no-Activity Camera2 check uses an offscreen YUV sink: it verifies manual shutter/ISO, WB and restoration of AUTO requests and results. It does not create capture files or inspect the screen. Hardware behavior beyond the tested emulator remains device-dependent.

Verification on 2026-09-12: 75 unit tests passed; F-Droid debug lint, both debug variants and the instrumentation APK built successfully; 616 localized strings passed the locale check. Android 35 emulator Camera2 returned exposure 8,000,000 ns, ISO 200 and daylight WB (5), then restored AUTO request and result values. UI instrumentation was compiled but not run; no screenshots or visual inspection were performed.

USB review follow-up: shared control heights distinguish primary actions (48 dp), ordinary actions (40 dp), toolbars (32 dp) and compact choices (28 dp). The shutter remains larger. Compact labels use 12 sp, editor choices 14 sp and adjustment values 18 sp, with 4/8/12 dp spacing. Focal buttons use approximate 35 mm equivalents when sensor dimensions are available; tooltip details retain the actual focal length. Light, location, audio, format, resolution, AUTO/PRO and Settings share one top row. Status/recording text sits above the focal buttons inside the preview. Audio is available before switching to VIDEO.

USB verification on 2026-09-12 (25060RK16C): pinned physical:0:2 and physical:0:3 both returned manual exposure 8,000,000 ns, ISO 200 and daylight WB (5), and restored AUTO request/result values. These checks use offscreen YUV and never open an Activity or save images. This fixes the earlier mistaken interpretation of physical override keys as the complete set of supported controls.

Landscape review update: PHOTO/VIDEO/TAP are text tabs without button backgrounds. The collapsed FAULT row is a compact transparent summary. In wide windows, status and focal choices move below the right-hand controls, and the preview badge is hidden. The edge chevron collapses that column to a slim rail with capture/stop and an expand action; selected camera and fault values are retained. Portrait keeps its preview controls.
