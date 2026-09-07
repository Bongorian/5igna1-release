# Effect models and processing order

Each effect describes a kind of interruption in the path from sensor to display. The chain follows the order below; each stage receives the previous stage's result.

[All guides](README.md) · [Creative recipes](RECIPES.md) · [Project statement](ABOUT.md) · [日本語](EFFECTS.ja.md)

## The 16 effects

CLEAN bypasses processing. The table lists the processed stages in order.

| Stage | Effect | First control | Second control | Processed RAW |
|---|---|---|---|---|
| Sensor | SENSOR FAIL | Point-to-column pattern | Dark-to-bright defects | Yes |
| Sensor | EXPOSURE BAND | Band density | Travel speed | Yes |
| Readout | ROW SHIFT | Row-band height | Displacement | Yes |
| Readout | LINE LOSS | Band height, 2–128 rows | Black fill to repetition | Yes |
| Data | BIT ROT | Bit position | Block size | Yes |
| Data | DATA SHIFT | Byte offset, 0–31 | Block size | Yes |
| Color array | CFA TEAR | Tile size | Horizontal/vertical/diagonal phase | Yes |
| Color array | CFA OFFSET | Horizontal/vertical/both phases | Region size | Yes |
| Interpolation | DEMOSAIC | Reconstruction disruption scale | False-color amount | RGB approximation only |
| Color | CHROMA | Horizontal-to-vertical direction | Separation width | No |
| Color | SPECTRUM | Palette period | Hue phase | No |
| Color | CHROMA LOSS | Chroma block width | Vertical coarseness | No |
| Transport | CORRUPT | Block size | Color-data corruption | No |
| Transport | PACKET LOSS | Packet size | Black fill to repetition | No; video-only |
| Display | VHS | Tracking disturbance | Tape noise | No |
| Display | TERMINAL | Scan-line density | Noise | No |

PACKET LOSS is absent from photo choices and photo processing. It models spatial block loss/repetition, not datamoshing from previous frames.

The ordering takes inspiration from fault locations, rather than claiming that one physical camera fails in exactly this sequence. SPECTRUM and TERMINAL include deliberately stylized choices. None of these effects damages camera hardware or its internal ISP.

## Strength and placement

Overall and per-stage strength range from 0–100%; effective strength is their product. A value of 100% means the maximum setting for that effect, not a guarantee that every pixel is damaged. Different effects use strength for mixing, displacement, exposure reduction, or the proportion of affected regions.

DATA SHIFT offsets, BIT ROT bit positions, and CFA phase options are discrete. The number of bit-position choices differs between normal 8-bit images and the RAW bit depth.

Change placement updates the seed for supported spatial patterns. SPECTRUM and CHROMA LOSS do not use a placement seed, so they omit that control. SENSOR FAIL and ROW SHIFT patterns are fixed by their seed. Time alone does not redraw every fault pattern; LIVE FAULT can vary settings separately.

## ROW SHIFT: a displaced readout

Bands move horizontally, with independent height and displacement controls. Zero displacement has no effect. The pattern stays in place until changed; samples leaving the image are treated as missing rather than wrapped around.

RAW shifts use even pixel distances to preserve the Bayer phase and fill missing edges with the black level. A row displacement therefore does not unintentionally become a color-array phase fault. Sensor orientation and RGB preview coordinates can still make RAW development look different from the preview.

## DATA SHIFT: bytes become different samples

Processed RAW treats RAW16 as a little-endian byte stream. It shifts by 0–31 bytes and reinterprets each adjacent pair as a sample. Odd offsets cross word boundaries; even offsets move sample positions. This is different from BIT ROT, which flips bits.

The first control selects the byte offset; 0 bytes is unchanged. The second selects affected block size, 4–512 bytes in RAW. Effective strength controls the proportion of affected blocks. Samples beyond the input end use the black level, and values are bounded by the white level. The DNG header and metadata are not intentionally corrupted.

The GPU approximation treats processed RGB as interleaved RGB8 bytes with row-based blocks. It cannot produce the same result as RAW16 byte displacement.

## LINE LOSS: missing or repeated rows

LINE LOSS blends from black-filled bands to repetition of the preceding rows. It does not move horizontal coordinates. Band height is 2–128 rows in even steps.

RAW repeats the preceding two rows alternately to preserve the Bayer row phase. Repeated samples come from the original input, and the initial band uses the black level. LINE LOSS loses/repeats rows; ROW SHIFT moves them sideways.

## CFA OFFSET: the wrong color-array phase

Within each 2×2 RAW cell, samples are swapped horizontally, vertically, or both, while the DNG retains its original CFA metadata. Relative to RGGB, these correspond to interpretation as GRBG, GBRG, or BGGR. The operation is relative to the actual sensor's array.

The second control sets the affected region size from 2–256 pixels. Samples at an odd image edge that lack a swap partner are retained. CFA TEAR makes local neighboring sample displacements; CFA OFFSET performs the cell-phase permutation.

The GPU version builds a synthetic RGGB mosaic from RGB, then interpolates it with a different phase. It is not live development of the camera's RAW stream.

## DEMOSAIC: unstable reconstruction

DEMOSAIC builds a synthetic RGGB mosaic from RGB, then interpolates using one-sided neighboring samples without following edge direction. The CFA phase remains correct; inconsistent interpolation and coarse sampling create false colors, checker patterns, and disrupted edges.

Its disruption scale spans 1–16 pixels, with a separate false-color amount. Zero false-color amount or zero strength is unchanged. It is available for JPEG, preview, and normal video. DNG contains undeveloped samples, so DEMOSAIC is not a processed RAW stage. A custom live RAW demosaicer is not implemented.

## Implementation map

- [Effects.java](../app/src/main/java/com/bongorian/signa1/Effects.java): names, IDs, order, and mode availability.
- [EffectParameters.java](../app/src/main/java/com/bongorian/signa1/EffectParameters.java): labels, defaults, and normalization.
- [effect.glsl](../app/src/main/res/raw/effect.glsl): GPU interpretations.
- [RawGlitch.java](../app/src/main/java/com/bongorian/signa1/RawGlitch.java): RAW16 operations.

New effects should explain the behavior they model and distinguish RGB approximations from RAW data changes. The project does not claim measured coverage of all real-world camera failures.
