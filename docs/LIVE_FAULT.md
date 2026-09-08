# LIVE FAULT

[Faults](EFFECTS.md) · [日本語](LIVE_FAULT.ja.md)

Each fault has its own time behavior, even when LIVE is off. A weak row can drift and a tape can suffer a brief dropout while their underlying identities stay fixed. LIVE adds a connection to the physical device; it is not a randomization mode.

| Input | Connection |
|---|---|
| Motion / rotation | Readout shear, tape tracking, CRT sync |
| Camera exposure / skew / timing | Exposure phase and integration, readout instability |
| Thermal state | Hot-pixel activity and sensor noise |
| CPU / camera timing pressure | Probability and impact of data/stream incidents |
| Audio amplitude | Tape timebase motion |

Open the control next to LIVE to choose input sources, sensitivity and 50/60 Hz lighting. LIVE starts off on a cold launch. Audio coupling is optional and requests microphone permission; denying it leaves the other sources usable. During audio recording, the existing recorder supplies the amplitude measurement instead of opening another microphone. Inputs are acquired only while the camera is in the foreground. Availability and measured values depend on the device.

The route and manual controls remain unchanged. Fixed identities use structural random seeds; continuous motion uses slow drift; incidents use separate event randomness. A new session can produce different accidents with the same saved character. RESEED makes a different individual with the same controls. There is no ordinary random chain switching or generic parameter interpolation.

Preview and ordinary video are fed from one canonical processed camera image for each camera timestamp. JPEG pins the displayed image at shutter time. RAW is a separate representation and exposure; see [formats](FORMATS.md).
