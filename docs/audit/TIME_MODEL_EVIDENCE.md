# Time-model audit evidence

Baseline: dd7f027, 2026-09-12. JVM synthetic inputs; no device/UI access.

- **LIVE OFF evolves**: Exposure phase 1.5334729 → 1.5502875 over one arrival second.
- **HOLD differs from speed 0**: Same fault time 1.0; thermal amplitude HOLD=0.0, speed 0=0.0076757353.
- **Manual time does not pin device response**: Pinned time=0, amplitude 0.0 → 0.0076757353.
- **Near-1 speed switches noise clock**: At identical evaluated fault time, VHS grain seed 502.48193 → 62.313747 when speed selects the warped path.
- **Exposure generic phase is diagnostic only**: phase/phaseSpeed overrides change inspection TIME fields but leave exposure mechanism unchanged.
- **Model selector paths disagree**: Network selected by macro: fps=12.0; selected by internal transportKind: fps=0.0.
- **Displayed Network limit differs from effective limit**: At default FAULT=0.55, nominal 12 fps becomes 33.6 fps; a 30 fps input sees no rate reduction.
- **Past content stamp bypasses Network stall**: Forward timestamp during stall updates=false; older TIME ECHO-like timestamp updates=true.
- **Frame cap is quantized by input cadence**: 300 regularly spaced 30 fps arrivals over 10 s yield 100 deliveries at the 12 fps setting (10 fps).
- **Duration can exceed period**: Period 1 s, duration 2 s, chance 1: envelope 1.0 just before boundary → 0.0 at boundary; requested duration does not imply a 2 s continuous hold.
- **Automatic event identity is session-dependent**: Same stage seed, event identities 4020871 / 4020964; Network schedule uses stage seed without session salt.

## Compiled catalog

Ranges/steps below are declared editable limits, not guaranteed automatic-output bounds or evidence that the active shader consumes a value. Macros are normalized 0–1. See the companion review for active-model dependencies and units.

### MOTION BLUR (ID 14)

Macros: `amount`=0.6, `direction`=0.5, `floor`=0.0.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `blurX` | -0.12 | 0.12 | 0.001 |
| SIGNAL | `blurY` | -0.12 | 0.12 | 0.001 |

### THERMAL NOISE (ID 15)

Macros: `amount`=0.6, `grain`=0.2, `floor`=0.0.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `noiseAmplitude` | 0.0 | 0.5 | 0.001 |
| SIGNAL | `noiseGrain` | 1.0 | 16.0 | 1.0 |
| SIGNAL | `grainSeed` | 0.0 | 997.0 | 0.1 |

### PIXEL DAMAGE (ID 1)

Macros: `density`=0.5, `hot`=0.5, `columns`=0.2.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `pixelDensity` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `columnDensity` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `hotFraction` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `hotValue` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `sensorNoise` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `grainSeed` | 0.0 | 997.0 | 0.1 |

### EXPOSURE (ID 2)

Macros: `depth`=0.6, `rate`=0.35, `bands`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `exposureDepth` | 0.0 | 2.0 | 0.001 |
| SIGNAL | `exposurePhase` | -6.283186 | 6.283186 | 0.001 |
| SIGNAL | `scanPhase` | 0.0 | 2000.0 | 0.1 |
| SIGNAL | `integration` | -1.0 | 1.0 | 0.001 |

### SMEAR (ID 16)

Macros: `amount`=0.6, `length`=0.6, `threshold`=0.75.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `smearAmount` | 0.0 | 2.0 | 0.01 |
| SIGNAL | `smearLength` | 0.0 | 0.4 | 0.001 |
| SIGNAL | `smearThreshold` | 0.0 | 0.99 | 0.001 |

### ROW ERROR (ID 3)

Macros: `displacement`=0.5, `bands`=0.4, `loss`=0.35, `concealment`=0.7.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `weakRows` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `rowGroups` | 1.0 | 2048.0 | 1.0 |
| SIGNAL | `rowOffset` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `readoutShear` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `lineLoss` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `linePosition` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `lineHeight` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `lineRetention` | 0.0 | 1.0 | 0.001 |

### BIT ERROR (ID 4)

Macros: `activity`=0.5, `bit`=0.6, `burst_size`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `bitProbability` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `bitIndex` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `bitBlock` | 2.0 | 512.0 | 1.0 |

### ADDRESS ERROR (ID 5)

Macros: `offset`=0.35, `region`=0.4, `activity`=0.6.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `byteOffset` | 0.0 | 4096.0 | 1.0 |
| SIGNAL | `addressRegion` | 2.0 | 4096.0 | 2.0 |
| SIGNAL | `addressProbability` | 0.0 | 1.0 | 0.001 |

### CFA ERROR (ID 6)

Macros: `coverage`=0.55, `phase`=0.0, `region`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `cfaCoverage` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `cfaPhase` | 0.0 | 2.0 | 1.0 |
| SIGNAL | `cfaRegion` | 2.0 | 1024.0 | 2.0 |

### DEMOSAIC ERROR (ID 7)

Macros: `interpolation`=0.65, `sampling`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `interpolationMix` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `sampleScale` | 1.0 | 64.0 | 1.0 |

### CHROMA ERROR (ID 8)

Macros: `separation`=0.45, `sampling`=0.35, `direction`=0.0.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `chromaOffset` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `chromaAngle` | -3.141593 | 3.141593 | 0.001 |
| SIGNAL | `chromaBlock` | 1.0 | 512.0 | 1.0 |

### COLOR MAP (ID 9)

Macros: `palette`=0.5, `cycles`=0.4, `mix`=0.8.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `paletteMix` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `palettePhase` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `paletteCycles` | 0.01 | 32.0 | 0.01 |

### BLOCK ERROR (ID 10)

Macros: `quantization`=0.5, `block_size`=0.45, `misaddress`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `quantLevels` | 2.0 | 256.0 | 1.0 |
| SIGNAL | `blockColumns` | 1.0 | 512.0 | 1.0 |
| SIGNAL | `blockError` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `blockOffset` | -1.0 | 1.0 | 0.001 |

### STREAM ERROR (ID 11)

Macros: `loss`=0.5, `region`=0.4, `concealment`=0.7.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `streamLoss` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `streamColumns` | 1.0 | 256.0 | 1.0 |
| SIGNAL | `concealment` | 0.0 | 1.0 | 0.001 |

### MEDIA (ID 12)

Macros: `transport`=0.0, `reduce`=0.0, `cable`=0.0, `bandwidth`=0.6, `tracking`=0.5, `dropout`=0.4, `noise`=0.25.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| EVENT | `eventPeriod` | 0.03 | 60.0 | 0.01 |
| EVENT | `eventDuration` | 0.005 | 60.0 | 0.005 |
| EVENT | `eventProbability` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventSerial` | -1000000.0 | 1000000.0 | 1.0 |
| EVENT | `eventEnvelope` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPosition` | 0.0 | 1.0 | 0.001 |
| EVENT | `eventPattern` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| PROFILE | `tapeBandwidth` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `trackingOffset` | -0.5 | 0.5 | 0.001 |
| SIGNAL | `trackingWave` | 0.0 | 0.25 | 0.001 |
| SIGNAL | `trackingPhase` | -6.283186 | 6.283186 | 0.001 |
| SIGNAL | `trackingSlip` | -0.5 | 0.5 | 0.001 |
| SIGNAL | `tapeDropout` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `dropoutPosition` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `tapeNoise` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `grainSeed` | 0.0 | 997.0 | 0.1 |
| PROFILE | `transportKind` | 0.0 | 3.0 | 1.0 |
| SIGNAL | `transportDamage` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `transportLoss` | 0.0 | 1.0 | 0.001 |
| PROFILE | `mediaReduce` | 0.0 | 1.0 | 1.0 |
| PROFILE | `cableKind` | 0.0 | 1.0 | 1.0 |
| SIGNAL | `transportNoise` | 0.0 | 1.0 | 0.001 |

### DISPLAY (ID 13)

Macros: `transport`=0.0, `upconvert`=1.0, `networkInterval`=0.10714286, `networkDuration`=0.42857143, `networkRate`=0.37931034, `networkResolution`=0.5, `scan`=0.5, `phosphor`=0.0, `convergence`=0.4, `sync`=0.4.

| Group | Key | Minimum | Maximum | UI step |
|---|---|---:|---:|---:|
| INPUT | `motionSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `audioSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `timingSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `thermalSensitivity` | 0.0 | 4.0 | 0.01 |
| INPUT | `cpuSensitivity` | 0.0 | 4.0 | 0.01 |
| TIME | `timeScale` | -4.0 | 4.0 | 0.01 |
| TIME | `timeOffset` | -3600.0 | 3600.0 | 0.1 |
| TIME | `time` | -86400.0 | 86400.0 | 0.01 |
| TIME | `driftSpeed` | 0.0 | 10.0 | 0.01 |
| TIME | `drift` | -1.0 | 1.0 | 0.001 |
| TIME | `phaseSpeed` | -40.0 | 40.0 | 0.01 |
| TIME | `phase` | -6.283186 | 6.283186 | 0.001 |
| TIME | `identityBias` | -1.0 | 1.0 | 0.001 |
| SIGNAL | `identitySeed` | 0.0 | 997.0 | 0.1 |
| SIGNAL | `eventSeed` | 0.0 | 997.0 | 0.1 |
| PROFILE | `scanDepth` | 0.0 | 1.0 | 0.001 |
| PROFILE | `scanLines` | 1.0 | 2160.0 | 1.0 |
| PROFILE | `phosphorMix` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `convergenceOffset` | -0.5 | 0.5 | 0.001 |
| SIGNAL | `syncOffset` | -0.5 | 0.5 | 0.001 |
| PROFILE | `transportKind` | 0.0 | 3.0 | 1.0 |
| SIGNAL | `transportDamage` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `transportLoss` | 0.0 | 1.0 | 0.001 |
| PROFILE | `upconvert` | 0.0 | 1.0 | 1.0 |
| SIGNAL | `networkFps` | 0.0 | 60.0 | 1.0 |
| SIGNAL | `networkStall` | 0.0 | 1.0 | 1.0 |
| SIGNAL | `refreshBand` | 0.0 | 1.0 | 0.001 |
| SIGNAL | `networkSeed` | 0.0 | 997.0 | 0.1 |
