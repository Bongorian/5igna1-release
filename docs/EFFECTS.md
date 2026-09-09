# Fault points and controls

This development branch adds [experimental device response](EXPERIMENTAL_SIGNALS.md), disabled by default: five input sensitivities per stage, plus motion blur, thermal noise and smear. It is not part of published 1.3.0.

[All guides](README.md) · [日本語](EFFECTS.ja.md)

Choose a damaged system, then watch what happens inside it. CLEAN is an empty route. The thirteen faults are applied in signal order, regardless of the order in which you select them.

| Fault point | Fault | Compact controls |
|---|---|---|
| SENSOR | PIXEL DAMAGE | Site density, hot/dark balance, column defects |
| SENSOR | EXPOSURE | Loss depth, phase speed, bands |
| READOUT | ROW ERROR | Displacement, bands, loss incidents, acquired-sample reuse |
| DATA | BIT ERROR | Activity, bit position, burst region |
| DATA | ADDRESS ERROR | Byte offset, region, activity |
| CFA / RECONSTRUCTION | CFA ERROR | Coverage, Bayer phase, region |
| CFA / RECONSTRUCTION | DEMOSAIC ERROR | Wrong-neighbor contribution, sampling |
| COLOR | CHROMA ERROR | Chroma separation, sampling, direction |
| COLOR | COLOR MAP | Palette phase, cycles, mixture |
| CODEC / STREAM | BLOCK ERROR | Quantization loss, block size, wrong-block incidents |
| CODEC / STREAM | STREAM ERROR | Loss incidents, region, acquired-sample reuse |
| MEDIA | VHS | Bandwidth profile, tracking, dropout, noise |
| DISPLAY | CRT | Scan and phosphor profiles, convergence and sync faults |

LEVEL is a compact artistic macro. It changes fault-specific quantities such as defect density, readout distance, exposure attenuation, incident impact or display mixture. It is not a common physical strength multiplied into every stage. The internal model uses named parameters of arbitrary count; the controls above intentionally hide that detail.

## A character that persists

IDENTITY describes fixed defect sites and biases. MOTION is a continuous drift or phase. EVENT is a short accident with a location, pattern and envelope. Some faults only need one or two of these. PIXEL DAMAGE keeps its sites as hot-pixel activity changes; ROW ERROR keeps weak bands as their displacement moves; VHS keeps its tracking bias between slips and dropout. CFA ERROR, DEMOSAIC ERROR and COLOR MAP are structurally stable under time alone.

RANDOM CHAIN creates 2–5 faults available in the current format, randomizes their controls, and sets a new overall level. Hold the main random button for RESEED. RESEED changes the identities of the selected faults and preserves the route and controls. COLOR MAP has no random identity to change. Reset restores a fault's control values without reseeding it. Intrinsic time evolution is defined by each fault. [LIVE FAULT](LIVE_FAULT.md) additionally couples device measurements into those states.

## What the models actually do

The camera supplies every scene sample. No semantic inference, object synthesis or content completion is used. Noise, black samples, misread bytes and reuse of acquired samples are part of the fault model.

- PIXEL DAMAGE through CFA ERROR have RAW16 adapters. Row displacements and row reuse preserve Bayer parity; CFA ERROR deliberately changes it. ADDRESS ERROR can intentionally break byte and component alignment.
- RGB CFA/reconstruction faults remosaic the already processed camera RGB. They do not access or reconstruct the sensor's actual RAW pipeline.
- CHROMA ERROR operates through an explicit luma/chroma conversion. COLOR MAP maps camera luminance to a false-color transfer function.
- BLOCK ERROR simulates decoded-image block quantization and address errors. STREAM ERROR erases decoded regions or conceals them with acquired samples from the same frame. Neither edits an actual codec bitstream or network packet. There is no previous-frame datamoshing in this implementation.
- VHS first supplies a tape bandwidth/chroma profile, then tracking drift/slips, dropout and noise. CRT separates scan/phosphor appearance from convergence and sync faults. Green monochrome is a CRT profile, not a separate TERMINAL effect.

These are causal artistic models, not a complete engineering simulation. Their ranges deliberately exceed normal hardware failure magnitudes. RAW, camera RGB, luma/chroma and the outputs of media/display models are different representations along a route; none is the sole true image.

## Recording the route

JPEG saves the UI-acknowledged, processed camera signal, including VHS and CRT when selected. Ordinary video uses that same processing timeline and canonical images. STREAM ERROR is available for both photos and videos. Processed RAW supports the six faults from PIXEL DAMAGE through CFA ERROR and uses a separate RAW exposure with the latched state; it is not the RGB viewfinder image. [Formats and capture limits](FORMATS.md).

Use LIVE to set how the current fault state evolves, with second-based periods and update intervals. [Time controls](LIVE_FAULT.md). Enable [ADVANCED MODE](ADVANCED_MODE.md) in Settings to directly fix model values.
