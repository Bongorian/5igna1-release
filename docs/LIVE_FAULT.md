# LIVE FAULT

[日本語](LIVE_FAULT.ja.md) · [Faults](EFFECTS.md) · [ADVANCED MODE](ADVANCED_MODE.md)

LIVE describes faults evolving at the current moment. Its controls define the fault timeline, the shape of variation and optional influence from device measurements. Intrinsic fault evolution remains available with LIVE off; enabling LIVE applies the selected timeline and modulation. LIVE and pause start off on cold launch; other settings are saved.

Open the adjustment button beside LIVE. The camera remains visible above the panel. Changes preview immediately in a separate draft; Apply retains the draft state and time, while closing discards it. Configure before recording; the three time controls also work during ordinary recording.

| Setting | Role |
|---|---|
| Time progression | Continuous, repeating, forward/backward, or a fixed update interval |
| Evolution speed / reverse | −4× to +4×; negative values reverse fault time and zero stops its clock |
| Variation pattern | Intrinsic evolution, periodic variation, accumulation/release, intermittent faults, or progression along the chain |
| Variation / repeat period | Seconds per cycle, 0.25–32 s; also sets the extent of looping/forward-backward time |
| Update interval | Seconds between changes for the stepped clock, 0.015625–2 s |
| Variation amount | How far strength decreases between peaks |
| Active fraction / probability | Duration within a period and probability per period for intermittent faults |

Timing is specified directly in seconds. Earlier tempo/beat settings migrate to their equivalent durations. Speed acts on fault time, so periods are measured along that timeline. Camera capture and video timestamps continue forward. These controls do not replay earlier camera images. A loop can differ when device inputs change.

**PAUSE / RESUME**, **TRIGGER** and **RESET TIME** share an evenly spaced row. Pause freezes fault state and the evaluated influence of inputs while the camera continues. Trigger creates a temporary fault at the current moment; triggering while paused holds it until resume/reset. Reset returns fault time to zero and clears the manual event. The selected chain remains intact. Zero LEVEL bypasses all faults; advanced fixed values retain precedence while a fault is active.

## Device-input inventory

| Setting | Measurement | Actual connection |
|---|---|---|
| Motion & rotation | Accelerometer/gyroscope, when available | Readout shear, VHS tracking, CRT sync |
| Audio amplitude | Optional microphone or the active audio recorder | Tape timebase movement and tracking wave |
| Camera exposure & readout timing | Camera metadata and delivery cadence | Exposure phase/integration; readout and incident pressure |
| Temperature | Battery temperature and Android thermal status | Hot-pixel level and sensor noise |
| Processing pressure | This app's CPU time relative to elapsed time | Data/stream incident probability and impact |
| Sensitivity | Gain for motion, audio, cadence and CPU coupling | Temperature retains its own mapping |
| Lighting frequency | 50 or 60 Hz | Mains-related exposure phase and integration when metadata is available |

Unavailable measurements do not invent readings. The expanded section shows input availability and measured values. Temperature and CPU are proxies, not direct measurements of an imaginary broken sensor or network link. Audio permission is requested only when applying enabled audio coupling; denial leaves other sources usable. During audio recording, the existing recorder supplies amplitude instead of opening a second microphone. Input ownership ends when the camera leaves the foreground.

Preview and ordinary video share the same processed image for each camera timestamp. JPG snapshots the current processed image; ADVANCED ON pins the displayed image; RAW uses a separate exposure and the applicable sensor-domain faults. Original RAW video ZIPs remain unprocessed. [Formats](FORMATS.md).

## TIME ECHO (experimental)

Enable **Experimental features** in Settings, then enable **TIME ECHO** in the LIVE editor and Apply. With LIVE on, it automatically inserts a two-second replay every 12 seconds; the separate **ECHO** camera button triggers a replay manually. Choose a starting point 2, 4 or 6 seconds in the past. Allow history to accumulate after enabling or changing cameras. A manual replay restarts the 12-second automatic interval; pressing during a replay does not extend it.

Only the camera image comes from the past. The selected FAULT chain, its current timeline, and current measured inputs process each historical image anew. FAULT pause/reverse do not pause/reverse the replay clock. Video audio continues in the present. JPEG and ordinary MP4 include the replay; RAW photos and RAW ZIP bypass it.

History is held only in memory, at up to 480 pixels on the long side and up to 4 frames per second, then scaled to the chosen output size. At most 33 frames use under 30 MiB of RGBA texture memory. History clears when leaving the camera, reconfiguring capture or disabling the feature; allocation failure disables replay for that session. The feature starts off, respects the Experimental switch and retains its editor settings with Apply/Cancel semantics.
