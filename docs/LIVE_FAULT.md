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

Unavailable measurements do not invent readings. The expanded section shows input availability and measured values. Temperature and CPU are proxies, not direct measurements of an imaginary broken sensor or network link. Audio permission is requested only when applying enabled audio coupling; denial leaves other sources usable. During audio recording, the existing recorder supplies amplitude instead of opening a second microphone. Leaving the foreground stops recording and releases all inputs.

Preview and ordinary video share the same processed image for each camera timestamp. JPG snapshots the current processed image; ADVANCED ON pins the displayed image; RAW uses a separate exposure and the applicable sensor-domain faults. Original RAW video ZIPs remain unprocessed. [Formats](FORMATS.md).

## TIME ECHO (experimental)

Enable **Experimental features** in Settings, then open LIVE → **TIME ECHO**. Apply its switch and **Probability** setting. Past images interrupt unpredictably: history length, starting point, replay duration and opportunities vary internally. The interface exposes no time or frequency settings. Probability 0 disables automatic events; the separate ECHO button can still trigger a randomized replay manually.

Only the image comes from the past. The current FAULT timeline and measured inputs process it anew. FAULT pause/reverse do not rewind the media clock; recorded microphone audio stays in the present. JPEG and ordinary MP4 include replay. RAW photos and RAW ZIP bypass it. In TAP mode, the historical imported image still enters after READOUT.

History stays in memory in a bounded, reduced-resolution pool (at most 33 RGBA frames, under 30 MiB). Disabling, closing or reconfiguring the input clears it. Allocation failure stops replay for the session. The editor retains Apply/Cancel behavior. LIVE settings use separate Time, Inputs and TIME ECHO pages.
