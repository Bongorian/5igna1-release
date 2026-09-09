# ADVANCED MODE

Version 1.3.1 adds [experimental device response](EXPERIMENTAL_SIGNALS.md), disabled by default: five input sensitivities per stage, plus motion blur, thermal noise and smear. Experimental features, ADVANCED MODE and audio default to OFF; existing saved choices are retained.

[Guides](README.md) · [日本語](ADVANCED_MODE.ja.md)

Open Settings (gear) and enable **ADVANCED MODE**, then tap any selected chain chip. The preview remains above the editor. The mode is a UI preference: turning it off preserves manual values. The Settings mode switch saves immediately. In the fault editor, Apply commits the draft; ×, Back or leaving the app discards it.

**AUTO** follows the fault model and displays its current compiled value. Tap AUTO to FIX the current value, use its slider, or tap the number for precise input. Each value has a finite allowed range. Tap FIX to return that value to AUTO. ALL AUTO clears all fixed values and the event identity for this fault. Basic controls continue to affect values that are automatic; fixed values take precedence. Reset restores the selected fault's defaults and removes overrides. Random chain clears overrides for newly selected faults; reseed preserves fixed values.

**SEED** is the signed 64-bit structural identity. **EVENT SEED** is available for ROW ERROR, BIT ERROR, ADDRESS ERROR, BLOCK ERROR, STREAM ERROR and VHS. AUTO gets a fresh session identity; a fixed seed reproduces incident scheduling given the same fault time, inputs and settings. It does not reproduce a changing camera image. SIGNAL `identitySeed` and `eventSeed` are reduced RGB rendering values, distinct from these full integer identities. Bayer RAW also uses the structural identity.

TIME contains the generators and their resolved states: seconds, time scale, drift and phase. EVENT contains period/duration in seconds, probability, serial, envelope, position and pattern. SIGNAL exposes every compiled mechanism value. PROFILE separates VHS bandwidth and CRT display characteristics from their faults. Some generator values are unused by particular faults; overriding a generator cannot affect a downstream value that is itself fixed. Counts/regions describe implementation sampling grids, offsets are generally normalized, and phases/angles use radians unless represented as normalized cycles.

LIVE manipulates fault time, never camera capture time. Fixed `time` pins one fault's clock. LIVE PAUSE freezes model evolution while the camera continues; a TRIGGER made while held remains held until release or reset. LEVEL zero bypasses all faults. Captures retain immutable fault snapshots. RAW applies only the six sensor/readout/data faults supported by its Bayer backend; use JPG/MP4 for downstream stages such as VHS/CRT. Internal controls cannot add a missing RAW stage.

The following catalog is the complete editable set; range limits also appear in numeric entry. "Incident faults" means the six EVENT SEED faults above. Slider increments are conveniences; precise entry accepts any finite value in range, although sampling operations may discretize it.

| FAULT | Group | Parameter | Range |
|---|---|---|---|
| All faults | TIME | `timeScale` | -4 … 4 |
| All faults | TIME | `timeOffset` | -3600 … 3600 |
| All faults | TIME | `time` | -86400 … 86400 |
| All faults | TIME | `driftSpeed` | 0 … 10 |
| All faults | TIME | `drift` | -1 … 1 |
| All faults | TIME | `phaseSpeed` | -40 … 40 |
| All faults | TIME | `phase` | -6.283186 … 6.283186 |
| All faults | TIME | `identityBias` | -1 … 1 |
| Incident faults | EVENT | `eventPeriod` | .03 … 60 |
| Incident faults | EVENT | `eventDuration` | .005 … 60 |
| Incident faults | EVENT | `eventProbability` | 0 … 1 |
| Incident faults | EVENT | `eventSerial` | -1000000 … 1000000 |
| Incident faults | EVENT | `eventEnvelope` | 0 … 1 |
| Incident faults | EVENT | `eventPosition` | 0 … 1 |
| Incident faults | EVENT | `eventPattern` | 0 … 997 |
| All faults | SIGNAL | `identitySeed` | 0 … 997 |
| All faults | SIGNAL | `eventSeed` | 0 … 997 |
| PIXEL_DAMAGE | SIGNAL | `pixelDensity` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `columnDensity` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `hotFraction` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `hotValue` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `sensorNoise` | 0 … 1 |
| PIXEL_DAMAGE | SIGNAL | `grainSeed` | 0 … 997 |
| EXPOSURE | SIGNAL | `exposureDepth` | 0 … 2 |
| EXPOSURE | SIGNAL | `exposurePhase` | -6.283186 … 6.283186 |
| EXPOSURE | SIGNAL | `scanPhase` | 0 … 2000 |
| EXPOSURE | SIGNAL | `integration` | -1 … 1 |
| ROW_ERROR | SIGNAL | `weakRows` | 0 … 1 |
| ROW_ERROR | SIGNAL | `rowGroups` | 1 … 2048 |
| ROW_ERROR | SIGNAL | `rowOffset` | -1 … 1 |
| ROW_ERROR | SIGNAL | `readoutShear` | -1 … 1 |
| ROW_ERROR | SIGNAL | `lineLoss` | 0 … 1 |
| ROW_ERROR | SIGNAL | `linePosition` | 0 … 1 |
| ROW_ERROR | SIGNAL | `lineHeight` | 0 … 1 |
| ROW_ERROR | SIGNAL | `lineRetention` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitProbability` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitIndex` | 0 … 1 |
| BIT_ERROR | SIGNAL | `bitBlock` | 2 … 512 |
| ADDRESS_ERROR | SIGNAL | `byteOffset` | 0 … 4096 |
| ADDRESS_ERROR | SIGNAL | `addressRegion` | 2 … 4096 |
| ADDRESS_ERROR | SIGNAL | `addressProbability` | 0 … 1 |
| CFA_ERROR | SIGNAL | `cfaCoverage` | 0 … 1 |
| CFA_ERROR | SIGNAL | `cfaPhase` | 0 … 2 |
| CFA_ERROR | SIGNAL | `cfaRegion` | 2 … 1024 |
| DEMOSAIC_ERROR | SIGNAL | `interpolationMix` | 0 … 1 |
| DEMOSAIC_ERROR | SIGNAL | `sampleScale` | 1 … 64 |
| CHROMA_ERROR | SIGNAL | `chromaOffset` | -1 … 1 |
| CHROMA_ERROR | SIGNAL | `chromaAngle` | -3.141593 … 3.141593 |
| CHROMA_ERROR | SIGNAL | `chromaBlock` | 1 … 512 |
| COLOR_MAP | SIGNAL | `paletteMix` | 0 … 1 |
| COLOR_MAP | SIGNAL | `palettePhase` | 0 … 1 |
| COLOR_MAP | SIGNAL | `paletteCycles` | .01 … 32 |
| BLOCK_ERROR | SIGNAL | `quantLevels` | 2 … 256 |
| BLOCK_ERROR | SIGNAL | `blockColumns` | 1 … 512 |
| BLOCK_ERROR | SIGNAL | `blockError` | 0 … 1 |
| BLOCK_ERROR | SIGNAL | `blockOffset` | -1 … 1 |
| STREAM_ERROR | SIGNAL | `streamLoss` | 0 … 1 |
| STREAM_ERROR | SIGNAL | `streamColumns` | 1 … 256 |
| STREAM_ERROR | SIGNAL | `concealment` | 0 … 1 |
| VHS | PROFILE | `tapeBandwidth` | 0 … 1 |
| VHS | SIGNAL | `trackingOffset` | -.5 … .5 |
| VHS | SIGNAL | `trackingWave` | 0 … .25 |
| VHS | SIGNAL | `trackingPhase` | -6.283186 … 6.283186 |
| VHS | SIGNAL | `trackingSlip` | -.5 … .5 |
| VHS | SIGNAL | `tapeDropout` | 0 … 1 |
| VHS | SIGNAL | `dropoutPosition` | 0 … 1 |
| VHS | SIGNAL | `tapeNoise` | 0 … 1 |
| VHS | SIGNAL | `grainSeed` | 0 … 997 |
| CRT | PROFILE | `scanDepth` | 0 … 1 |
| CRT | PROFILE | `scanLines` | 1 … 2160 |
| CRT | PROFILE | `phosphorMix` | 0 … 1 |
| CRT | SIGNAL | `convergenceOffset` | -.5 … .5 |
| CRT | SIGNAL | `syncOffset` | -.5 … .5 |

## Experimental additions

Enabled by the separate experimental Settings switch. INPUT is available on every stage; these additions are outside the original thirteen-fault catalog above.

| Stage | Group | Parameter | Range |
|---|---|---|---|
| All | INPUT | `motionSensitivity`, `audioSensitivity`, `timingSensitivity`, `thermalSensitivity`, `cpuSensitivity` | 0 … 4 |
| MOTION BLUR | SIGNAL | `blurX`, `blurY` | -.12 … .12 |
| THERMAL NOISE | SIGNAL | `noiseAmplitude` | 0 … .5 |
| THERMAL NOISE | SIGNAL | `noiseGrain` | 1 … 16 |
| THERMAL NOISE | SIGNAL | `grainSeed` | 0 … 997 |
| SMEAR | SIGNAL | `smearAmount` | 0 … 2 |
| SMEAR | SIGNAL | `smearLength` | 0 … .4 |
| SMEAR | SIGNAL | `smearThreshold` | 0 … .99 |
