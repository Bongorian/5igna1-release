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
