# Internal parameters and time-model review

[日本語](TIME_MODEL_REVIEW.ja.md) · [Generated catalog and evidence](../audit/TIME_MODEL_EVIDENCE.md) · [Documentation map](../DOCUMENTATION.md)

Review date: 2026-09-12. Application baseline: **dd7f027**, after the Network display changes. This is a design review, not a new behavior specification or release. Production code and the Pixel installation are unchanged by this review.

The recommended direction is to give each parameter an explicit dependency on fault time, input/arrival time, measured response, or retained content. Keep capture and encoding monotonic, make seeded fault evolution predictable, and show which controls actually affect the selected model. The most urgent problems are the overloaded content timestamp, inconsistent model selection, and nominal versus effective Network values.

## What was inspected

All 16 non-clean stage IDs, including all four MEDIA and all four DISPLAY variants, plus TIME ECHO and the capture/metadata boundary. The generated appendix lists every normalized macro and every compiled internal parameter with its declared group, limits and UI increment. It is generated from the real `Effects` and `FaultParameters` catalog, rather than a separately maintained list.

Source paths below are relative to `app/src/main/kotlin/com/bongorian/signa1/` unless stated otherwise:

- [FaultModel](../../app/src/main/kotlin/com/bongorian/signa1/FaultModel.kt): arrival updates, local time, inputs, events, profiles and override precedence.
- [LivePerformance](../../app/src/main/kotlin/com/bongorian/signa1/LivePerformance.kt): speed, HOLD, loop, ping-pong, step and strength modulation.
- [FaultParameters](../../app/src/main/kotlin/com/bongorian/signa1/FaultParameters.kt), [AdvancedControls](../../app/src/main/kotlin/com/bongorian/signa1/AdvancedControls.kt): declared versus visible parameters.
- [EffectChain](../../app/src/main/kotlin/com/bongorian/signa1/EffectChain.kt), [shader](../../app/src/main/res/raw/effect.glsl), [RawGlitch](../../app/src/main/kotlin/com/bongorian/signa1/RawGlitch.kt): actual parameter consumers.
- [GlitchEngine](../../app/src/main/kotlin/com/bongorian/signa1/GlitchEngine.kt), [TimeEcho](../../app/src/main/kotlin/com/bongorian/signa1/TimeEcho.kt), [EchoSchedule](../../app/src/main/kotlin/com/bongorian/signa1/EchoSchedule.kt), [NetworkDisplay](../../app/src/main/kotlin/com/bongorian/signa1/NetworkDisplay.kt): source clocks and retained frames.
- [SavedSignal](../../app/src/main/kotlin/com/bongorian/signa1/SavedSignal.kt), [EffectState](../../app/src/main/kotlin/com/bongorian/signa1/EffectState.kt): saved settings and immutable evaluated snapshots.

The evidence uses eleven synthetic JVM characterizations. The full F-Droid debug unit suite passed with 59 tests, including the unchanged migration goldens. Source reading establishes shader consumption; these new probes do not measure image quality, GPU performance, or every camera/codec path. Earlier Network GPU verification remains separate evidence. Reproduce with `./tools/build.sh testFdroidDebugUnitTest --tests com.bongorian.signa1.TimeModelAuditTest`; the catalog is written to `app/build/reports/time-model-audit.md`. The characterization intentionally describes this baseline, not the desired future behavior.

## Existing clock ownership

| Clock/state | Current source and consumers | Speed / HOLD / RESET |
|---|---|---|
| Arrival elapsed time | `advance(arrivalSeconds)`; `elapsed`; measured-input smoothing, spring integration, audio forcing and manual HIT decay | Speed does not scale this. HOLD returns before response/cue integration. RESET does not reset response history. |
| Global fault position | `performancePosition += delta * speed`; LIVE ON entry aligns to elapsed time | Speed can reverse it. HOLD freezes it. RESET sets position to zero. |
| Global evaluated fault time | FREE position, LOOP wrap, PING_PONG fold, or STEP quantization | Also drives global strength envelope. LIVE OFF uses elapsed time; it is not a static-image mode. |
| Per-stage fault time | Global time × `timeScale` + `timeOffset`, or fixed `time` | Drives drift and stage incidents; fixing this time does not fix the global envelope, HIT or inputs. |
| Noise/exposure sample clock | Normally `sensorNs`; under time warp, `(time * 60).toLong() * 16666667`; natural HOLD uses `heldSignalNs` | Changing speed from 1 to almost 1 switches clock domains. Negative conversion truncates toward zero. |
| Content timestamp | Camera texture timestamp; TAP assigns current elapsed-realtime nanoseconds, not imported-video PTS | TIME ECHO replaces `Frame.cameraNs` with an older retained image timestamp. Network currently uses the same field for delivery cadence. |
| History opportunities | TIME ECHO uses current source/arrival timestamps and its own mutable RNG | Enabled by LIVE/experimental gating; not controlled by FAULT speed or HOLD. |
| Presentation / encoding | Separate monotonic presentation tokens and encoder timing | Must continue forward despite fault reverse/loop or old content. |

HOLD and speed 0 therefore have different meanings today: HOLD freezes evaluated input response and HIT age, whereas speed 0 stops fault position while inputs and HIT decay continue. That distinction is useful if presented explicitly. Neither guarantees that the source image itself stops. Network can retain an already-stalled image while its fault schedule is held.

## Parameter families and units

| Family | Current keys | Meaning / inconsistency to resolve |
|---|---|---|
| Stage identity | Serialized 64-bit identity; `identitySeed`, `identityBias` | Pattern identity versus editable derived float seeds. Bias is under TIME although it is static identity data. |
| Time transform | `timeScale`, `timeOffset`, `time` | Dimensionless scale and fault seconds; current global-strength envelope remains outside this local transform. |
| Continuous motion | `driftSpeed`, `drift`, `phaseSpeed`, `phase` | Drift sampling speed, signed drift, radians per fault second and radians. Not every model consumes these generic generators. |
| Incident schedule | `eventPeriod`, `eventDuration`, `eventProbability`, `eventSerial` | Fault seconds, opportunity probability and cycle identity. Duration may exceed period; short duration is also attenuated by fixed 25 ms attack / 90 ms release. |
| Incident sample | `eventEnvelope`, `eventPosition`, `eventPattern`, `eventSeed` | Envelope, normalized position and pattern sample. Editing a generator does not affect a manually fixed downstream value. `eventSerial` fixes cycle identity, not the within-cycle age. |
| Measured inputs | Five `*Sensitivity` values, 0–4 | Input gain; dependent on global input availability and experimental mode. Response integrates real arrival time, not reversed fault time. |
| Profile / image mechanism | Per-model keys in the next table | Dimensionless mixes, normalized image offsets, pixel/sample grids, radians, byte offsets, quantization levels. PROFILE is still often multiplied by global FAULT. |
| Frame delivery | `networkFps`, `networkStall`, Network interval/duration/resolution macros | Mixes physical delivery seconds with fault seconds. A boolean stall is exposed as SIGNAL; interval/duration macros have no corresponding ADVANCED generators. |
| Retained content | Network processed buffer; TIME ECHO pre-FAULT images | Stateful pixels, timestamps and lifecycle; cannot be reconstructed from time/seed alone. |

Automatic values can exceed the editable catalog's time/serial ranges during long sessions. Those ranges are manual-entry limits, not a complete dimensional contract.

## Model-by-model inventory

`F` = local fault time; `E` = seeded incidents in F; `S` = sample/noise clock; `R` = current measured response; `A` = arrival/delivery time; `I` = retained/input image. Every active stage may also receive the global strength envelope, HIT and experimental non-native input strength. “Static” below means no additional intrinsic time dependency at fixed level/inputs.

| Model | Image/profile parameters consumed (common seeds omitted where obvious) | Time dependencies and review |
|---|---|---|
| CLEAN | None | Input passes through; no fault-clock contract required. |
| PIXEL DAMAGE | `pixelDensity`, `columnDensity`, `hotFraction`, `hotValue`, `sensorNoise`, `grainSeed` | Pattern sites are seeded/static; hot value F/R; noise S/R. Generic phase is unused. |
| EXPOSURE | `exposureDepth`, `exposurePhase`, `scanPhase`, `integration` | F oscillator plus S and actual exposure/readout duration when available. Generic `phase`/`phaseSpeed` do not drive `exposurePhase`; two phase systems are exposed. |
| ROW ERROR | `weakRows`, `rowGroups`, `rowOffset`, `readoutShear`, `lineLoss`, `linePosition`, `lineHeight`, `lineRetention` | F drift, E line loss/position, R shear. Retention samples the same frame, not temporal history. |
| BIT ERROR | `bitProbability`, `bitIndex`, `bitBlock`, `eventSeed` | E envelope/pattern and R temperature/pressure. No continuous drift/phase consumer. |
| ADDRESS ERROR | `byteOffset`, `addressRegion`, `addressProbability`, `identitySeed` | E changes probability; affected regions remain identity-based. Event pattern/position and generic drift/phase do not drive this shader. RGB byte addressing and RAW16 are distinct representations. |
| CFA ERROR | `cfaCoverage`, `cfaPhase`, `cfaRegion`, `identitySeed` | Static pattern/phase choice. `cfaPhase` is a discrete Bayer arrangement, not temporal radians. Generic TIME controls have no image effect at fixed level. |
| DEMOSAIC ERROR | `interpolationMix`, `sampleScale`, `identitySeed` | Static reconstruction grid/neighbors; generic TIME generators have no intrinsic image consumer. RGB approximation only. |
| CHROMA ERROR | `chromaOffset`, `chromaAngle`, `chromaBlock` | F drift changes separation. Angle is orientation in radians, not temporal phase. |
| COLOR MAP | `paletteMix`, `palettePhase`, `paletteCycles` | Static luminance-to-palette mapping. Cycles/phase are color-space coordinates, not temporal oscillators. Stage SEED is intentionally unsupported. |
| BLOCK ERROR | `quantLevels`, `blockColumns`, `blockError`, `blockOffset` | E address fault, F drift and identity bias in offset; static spatial quantization pattern. |
| STREAM ERROR | `streamLoss`, `streamColumns`, `concealment`, `eventSeed` | E loss and seeded regions. Concealment is a region in the same input frame, not previous-frame replay. |
| MEDIA / VHS | `tapeBandwidth`, `trackingOffset`, `trackingWave`, `trackingPhase`, `trackingSlip`, `tapeDropout`, `dropoutPosition`, `tapeNoise`, `grainSeed` | F drift/phase, E slip/dropout, S grain, R tracking/audio. Richest existing temporal model. |
| MEDIA / DVD | `mediaReduce`, `transportDamage`, `transportLoss`, identity/event seeds | E changes loss/pattern; damage/quantization is level-controlled. Inherits the VHS incident schedule, including hidden tracking/dropout-derived activity. VHS phase and grain have no DVD image consumer. |
| MEDIA / Digital thru | Routing only | Exact pass-through. Inherited VHS TIME/EVENT/image controls remain in ADVANCED despite no visible result. |
| MEDIA / Analog thru | `cableKind`, `transportDamage`, `transportLoss`, `transportNoise`, `trackingPhase`, `dropoutPosition`, `grainSeed` | F phase, E loss/location, S interference. Fixed media dimensions depend on cable model. Inherits VHS event timing. |
| DISPLAY / CRT | `scanDepth`, `scanLines`, `phosphorMix`, `convergenceOffset`, `syncOffset` | F drift and R movement. Scan lines are a spatial pattern; phosphor is color mixing, not an afterglow/history model. Generic phase unused. |
| DISPLAY / Digital thru | `upconvert` after analog MEDIA | Static enlargement; no intrinsic fault time. Other DISPLAY parameters are still exposed in ADVANCED. |
| DISPLAY / Network | `transportLoss` for buffer size, `networkFps`, `networkStall` | Freeze schedule F, frame delivery currently content timestamp, retained pixels I. No noise shader. `transportDamage`, `networkSeed` and CRT image parameters do not affect Network pixels. |
| DISPLAY / LED | `transportLoss`, `transportDamage`, `refreshBand`, `syncOffset`, `identitySeed` | Seeded modules; flicker is indexed by `floor(syncOffset * 100)`, indirectly F/R. No explicit refresh frequency. `networkSeed` unused. |
| MOTION BLUR | `blurX`, `blurY` | R angular motion × exposure duration, with static floor. Spatial multi-sampling of the current image; not temporal accumulation. No generic F motion consumer. |
| THERMAL NOISE | `noiseAmplitude`, `noiseGrain`, `grainSeed` | R heat/exposure amplitude plus S grain; static floor available. Fixed local time freezes grain selection, not heat response. |
| SMEAR | `smearAmount`, `smearLength`, `smearThreshold` | R readout/exposure metadata; spatial highlight streak in the current image. No temporal trail buffer. |
| TIME ECHO (outside stage catalog) | Probability; internal random retention/delay/duration/opportunity; pre-FAULT history | A/I with mutable RNG. Past pixels are processed by current FAULT parameters. At most 33 reduced-resolution samples, sampled at 250 ms intervals. |

RAW consumes the six base sensor/readout/data stages and the three experimental sensor stages when enabled. Downstream RGB models are bypassed. Clock ownership can be shared without requiring byte-identical RGB and Bayer output.

## Findings and priorities

| Priority | Finding and concrete consequence | Proposed treatment |
|---|---|---|
| First | `cameraNs` denotes content age and Network delivery time. An older ECHO stamp makes `NetworkDelivery.update` accept a new frame even during a stall. | Separate monotonic delivery time, content timestamp and source epoch. Rewind old content without pretending a new source was attached. |
| First | Internal `transportKind` overrides apply after compilation from the ordinary `transport` macro. Direct Network selection produces `networkFps=0`, whereas ordinary Network selection produces 12 at full FAULT. | Resolve one effective variant before deriving any defaults or schedule. Use that variant consistently in compile, UI and rendering. |
| First | Network's displayed 12 fps means `60 + (12 - 60) × level`. At default FAULT 0.55 it is **33.6 fps**; a 30 fps input does not slow down. Duration/resolution also depend on effective level. | Show nominal and effective values, including LIVE modulation. Prefer physical rate/duration targets and an explicitly named severity mapping; decide the mapping before changing stored interpretation. |
| Next | The rate limiter restarts the deadline at each delivered frame. Synthetic 30 fps input with a 12 fps cap delivers 10 fps. This respects an upper bound but loses expected cadence. | Use accumulated delivery deadlines; specify whether the setting means average cadence or strict minimum spacing. Handle missed deadlines without catch-up bursts. |
| Next | All variants share one stage catalog. No-op controls remain visible, and Network's former `networkSeed` has no consumer. Its freeze macros disappear behind ADVANCED's raw catalog. | Add active-variant/consumer metadata. Hide or explain inactive controls while preserving serialized legacy keys. Expose useful schedule controls consistently. |
| Next | Normal and warped noise clocks use different origins/quantization. A speed change from 1 to 0.999999 changes grain even at the same evaluated time. | Define explicit stochastic sample clock selection. Give artistic noise a stable fault-time tick; keep sensor-timed exposure an explicit measured mode. Treat this as a visual change requiring versioned comparison. |
| Next | `eventDuration > eventPeriod` is accepted; the event resets at the period boundary anyway. Fixed attack/release also suppress very short events. | Define non-overlap versus overlap policy and duration-relative envelope shape; keep chance separate from amount and duration. |
| Contract | HOLD, speed 0, fixed `time`, RESET and LIVE OFF have different scopes. Global envelopes/HIT/inputs can change a stage with fixed local time. | Label local-time lock versus whole-stage freeze. Preserve a separate measured-response clock; document whether RESET also resets noise/history. |
| Contract | Automatic incidents use stage seed XOR session salt; Network uses stage seed alone; LIVE BURST uses session salt; ECHO uses mutable randomness. | Record separate pattern, event, performance and history seeds where replay is intended. Do not promise full replay from stage SEED alone. |
| Contract | Held Network pixels can originate from an earlier processed snapshot while the current `rendered.frame` records present parameters/time. Zero-level stage gating also discards Network history. | Carry delivered-content provenance separately from current command state; define history behavior on bypass, reseed, source change, editor preview, capture and RESET. |

The first three are concrete inconsistencies reproduced or derived from current code. Different clock domains, input response during speed 0 and session-dependent incidents are not automatically defects; they require an explicit product contract.

## Proposed contract

1. **One frame-time record with named fields:** monotonic arrival/delivery nanoseconds, content/source timestamp, source epoch, global fault position, evaluated global time, local stage time and stochastic tick. Source identity changes reset retained delivery; ECHO age changes do not.
2. **Static pattern, continuous motion, incidents, input response and history are separate capabilities.** Every parameter declares unit, upstream dependencies, active variants, backend support and whether it is a generator or final override. Consumers determine which controls appear.
3. **Clock controls manipulate fault evolution.** FREE/LOOP/PING_PONG/STEP/REVERSE use the same underlying position. Physical acquisition/encoding remain monotonic. Measured response stays in arrival time; HOLD samples and retains it. Speed 0 retains the current distinction unless deliberately renamed/changed.
4. **Network freezes and generic incidents use one scheduling primitive.** Support period, duration, chance, phase and seed, with a clearly defined non-overlap default. Network frame cadence remains a separate delivery mechanism. LED receives an explicit refresh rate if periodic refresh is intended.
5. **Show effective values.** A 0.7 s fault-time event at 2× speed lasts 0.35 real seconds in a free clock, before level modulation. LOOP/STEP can revisit or skip the active window, so do not promise a fixed wall-time duration there. Display rate/resolution targets alongside current effective limits.
6. **Override precedence is explicit:** resolve variant → macro-derived defaults → local generators/events → measured/global modulation → final manual overrides. A fixed final value wins; generator rows should explain when downstream overrides make them ineffective.
7. **Reproducibility has levels:** saved settings; evaluated fault snapshot; deterministic fault timeline with fixed seeds and recorded inputs; exact rendered-history replay. The current saved-signal feature restores settings, not a full source/input/history recording.

## Suggested implementation order and acceptance checks

**Phase 1 — clarify and correct routing.** Resolve effective variant first; add a variant-aware parameter catalog; separate delivery/content timestamps and source epoch; display nominal/effective Network values. Verify every ordinary/internal model selection pair, ECHO during a stall, source replacement, repeated timestamps, resizing, LEVEL zero, release/reacquire and metadata provenance. Keep previous settings readable.

**Phase 2 — unify generated time behavior.** Add the shared incident schedule and explicit noise-clock policy; specify overlap, short events, HOLD/zero speed/RESET and seed scope. Verify identical results at the same local time with fixed inputs/seeds, positive/negative boundary behavior, loop seams, step intervals longer than events, and 24/30/60/120 fps sampling. Keep old goldens as legacy fixtures; add new expected behavior separately.

**Phase 3 — normalize model semantics.** Give LED refresh a physical frequency if desired; expose Exposure's real phase generator; separate static color/Bayer phase from temporal phase; remove no-op controls from active UI while retaining compatibility aliases. Verify final consumed values or rendered pixels, not just that `inspect()` echoes a requested override.

**Phase 4 — evaluate richer effects separately.** Afterglow, true temporal motion blur and packet/frame corruption would require new stateful image models. Current names should explain their present spatial approximations. They are outside this audit's implementation scope.

This review adds reproducible observations and documents the proposed order. It does not change the application's time behavior, overwrite migration goldens, install another Pixel build, or publish a release.
