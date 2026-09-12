# Time and parameter contract (unreleased)

[日本語](TIME_MODEL.ja.md) · [LIVE](LIVE_FAULT.md) · [Internal controls](ADVANCED_MODE.md) · [SEED](SEEDS.md)

This development implementation uses **clock model 2**. Existing released builds and saved descriptions without `clockModel=2` retain their historical meaning. Old controls and FIX keys remain readable, but replaying those settings uses the new generators; it does not reproduce an earlier release's exact noise or incident sequence.

## Clocks and controls

| Operation | Fault generators | Measured input response | Source / delivery |
|---|---|---|---|
| LIVE OFF | Intrinsic fault time continues | Coupling disabled | Continue forward |
| Speed 0 | Local motion, incidents and artistic noise stop unless local overrides specify otherwise | Continues | Continue forward |
| HOLD | Fault evolution and current evaluated response are held; TRIGGER remains held too | Held | Source and delivery continue; an active Network stall can keep the old image |
| Reverse / LOOP / PING_PONG / STEP | Change evaluation along fault time | Measured response remains in arrival time | Camera/encoding do not reverse |
| Fixed stage `time` | Pins that stage's generators | Global strength, TRIGGER and inputs may still change its result | Continue forward |
| RESET TIME | Resets fault position and clears TRIGGER | Does not erase response history | Does not clear image history or restart source playback |

Every frame distinguishes monotonic `deliveryNs`, the source/content `cameraNs`, and `sourceEpoch`. Source replacement/reconfiguration advances the epoch; inserting an ECHO frame changes content age only. Capture and encoding timestamps remain forward-moving. TAP's source stamp is its current arrival time, not the imported clip's presentation timestamp.

Artistic grain in PIXEL DAMAGE, VHS and THERMAL NOISE uses a fixed 60 Hz tick of **local fault time**, including natural speed and LIVE OFF. Changing speed from 1 to a nearby value no longer switches to a different noise clock. Negative times use floor-based ticks. EXPOSURE offers `exposureClock`: **0** uses its fault oscillator alone; **1** also uses measured exposure/readout timing when available. Actual timing is not reversed. `exposureRate` is radians per fault second; `exposurePhaseOffset` is radians. The final `exposurePhase` FIX wins over those generators.

## Incidents and SEED

Ordinary incident faults and Network share a pure non-overlapping schedule with period, duration, phase, probability and event identity. Network exposes these in ADVANCED as well as its ordinary timing controls. A Network period of zero disables automatic opportunities.

Duration is capped at period. Ordinary incident envelopes use an attack of min(25 ms, duration/4) and a release of min(90 ms, duration/4), so short events can reach full strength. Network uses the rectangular active window to hold the received frame. Phase is a fraction of the period. Probability compares against one seeded draw per opportunity; LIVE/input modulation can change the current probability during that opportunity. Fixed `eventSerial` selects the random cycle identity without freezing within-cycle time.

AUTO EVENT SEED derives from the structural SEED and a stable domain constant, rather than a new session. A fixed EVENT SEED remains independent of structural reseeding. LIVE BURST's random decisions derive from the ordered chain and its structural seeds. Fixed inputs, times, route and seeds give the same generated states across sessions. TIME ECHO retains its independent randomized history schedule; actual camera/audio/temperature inputs and retained pixels are not recorded for full replay.

TRIGGER retains its existing real-time decay and opens the Network incident gate too. HOLD keeps it active until resume/reset. A final `networkStall` FIX takes precedence. LEVEL zero bypasses the stage even when an incident or manual value is active.

Periods/durations are **fault seconds**. In FREE at 2× speed, a 0.7 s window spans about 0.35 real seconds before input sampling and modulation. LOOP or STEP can revisit or skip windows. Short windows can be missed at low source frame rates; this model does not generate missing source frames.

## Network delivery

- The default delivery target is **12 fps**, including at initial FAULT 0.55. Frame rate and nominal freeze duration are no longer interpolated from an unrelated 60 fps baseline by LEVEL.
- FAULT controls automatic freeze probability and resolution loss. At initial FAULT 0.55, the default 60%-per-side target becomes **78% per side**, the freeze window is **0.7 fault seconds**, and automatic chance is **55%** at opportunities **5 fault seconds** apart. Current LIVE modulation/manual overrides can change effective values; the editor displays them.
- Delivery uses accumulated deadlines on the monotonic clock. This targets the requested average cadence instead of rounding every interval down to an input-frame divisor. It cannot exceed available input frames. Late arrivals deliver once without a catch-up burst; leaving a stall delivers the latest available frame immediately.
- ECHO's older content timestamp does not release a stall. Changing resolution waits for the next delivery. A new source epoch, output-size change, structural RESEED or released buffers initializes fresh history. Bypassing/removing Network clears its retained frame.
- Held processed pixels retain the immutable settings, fault time and content timestamp that created them. Their metadata has a newer delivery timestamp when shown again. Ordinary MP4 metadata still records recording-start settings; it is not a per-frame event log.
- Network changes actual intermediate image resolution and frame delivery. It adds no synthetic noise overlay and does not change selected file dimensions or encoder/audio clocks.

## Model-specific controls

The effective MEDIA/DISPLAY variant is resolved before deriving its mechanisms. Ordinary model selection and internal `transportKind` selection now agree. Choosing the ordinary model/connection/upconversion option replaces the matching fixed selector; other final FIX values remain deliberate overrides.

ADVANCED shows only the selected model's active parameter families. Inactive stored FIX values remain in the saved state with a notice; AUTO ALL clears them. Static CFA and palette phase are not presented as temporal oscillators. Generic phase/drift controls with no consumer are hidden. Network's old `networkSeed` and `transportDamage` values remain decodable but are not active Network controls. Final FIX values override upstream generators.

LED has an explicit **refresh frequency** (ordinary range 0–30 Hz, ADVANCED 0–60 Hz) and a derived `refreshSeed`, both evaluated in fault time. Zero Hz holds its flicker pattern. Module damage and flicker depth remain separate. CRT phosphor, MOTION BLUR and SMEAR keep their current spatial approximations; this change does not introduce temporal afterglow or multi-frame blur.

Per-input sensitivity defaults now follow the active variant. For example, the CRT movement route is not silently inherited by Network or LED. Explicit experimental gains on otherwise unused inputs can still modulate stage strength. Digital pass-through exposes no input/fault generators.

## Validation and compatibility

The legacy Java migration hashes remain unchanged and run against the frozen pre-change model. Current semantic tests separately check unchanged static mechanisms, selected-variant parity, frame provenance, source epochs, 24/30/60/120 fps delivery, noise ticks, seeds, short/reversed event windows, Exposure/LED generators and retained inactive settings. Offscreen GPU checks cover all 16 MEDIA/DISPLAY combinations, ECHO-safe holds, resolution changes and held-frame metadata. No screenshot or UI automation is required for these checks.

The [dated audit](design/TIME_MODEL_REVIEW.md) and [original observations](audit/TIME_MODEL_EVIDENCE.md) describe dd7f027, not this implementation. Their historical reproduction command belongs to audit commit 6e0aaf1. Current regression coverage is `TimeContractTest` and the `network-render` instrumentation action.
