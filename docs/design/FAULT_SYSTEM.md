# Fault system redesign (release baseline)

Baseline: public release source `7dd83a7` (1.0.0). This is an intentional settings and processing break, not an ID-compatible refactor. [Japanese design](FAULT_SYSTEM.ja.md).

## Intent

A persistent damaged system has a recognizable character and fleeting incidents. Every image starts with a real camera sample. No semantic reconstruction, object inference or invented scene content is involved. Noise, erased samples, address mistakes, component mistakes and reuse of acquired samples are allowed. Mechanisms are causal, but their ranges are artistic.

## Baseline audit

The release uses 17 IDs, four floats per effect (strength, two parameters, seed), generated GLSL IDs and ordered ping-pong passes. AUTO interpolates all floats, including identity seeds; optionally it shuffles the chain. Device inputs already collect acceleration, rotation, audio amplitude, battery/thermal state, CPU pressure and capture timing. The older measured-input model is present but the current dialog only configures internal AUTO. The camera advances state once per acquired frame. Still capture takes a later JPEG/RAW exposure and re-renders it: equal settings do not mean the displayed image was captured. Preview and encoding also run separate effect passes at different sizes.

Reuse: Camera2 lifecycle, input acquisition, causal pass order, immutable committed settings, editor transactions, EGL lease, MediaStore/EXIF, bounded original-RAW recording queue, FOSS distribution boundaries. Replace: catalog, parameter storage, generic AUTO and chain switching, fault-state compiler, shaders and RAW adapters, and JPEG capture's later-exposure rendering.

## Breaking catalog migration

| Release 1.0 | New route / decision |
|---|---|
| CLEAN | Empty selection, no processing pass |
| SENSOR FAIL / EXPOSURE BAND | PIXEL DAMAGE / EXPOSURE |
| ROW SHIFT + LINE LOSS | ROW ERROR, with stable weak bands, displacement and transient loss/reuse |
| BIT ROT / DATA SHIFT | BIT ERROR / ADDRESS ERROR, with representation-specific component widths |
| CFA TEAR + CFA OFFSET | CFA ERROR; phase misinterpretation replaces arbitrary channel tearing |
| DEMOSAIC | DEMOSAIC ERROR; wrong-neighbor interpolation retained |
| CHROMA + CHROMA LOSS | CHROMA ERROR; explicit luma/chroma sampling and displacement |
| SPECTRUM | COLOR MAP; a luminance transfer, not a spectral sensor claim |
| CORRUPT | Removed. Byte/component misread belongs to ADDRESS ERROR; block address/quantization belongs to BLOCK ERROR. Arbitrary channel permutations are removed. |
| PACKET LOSS | STREAM ERROR for photos and videos; decoded-region erasure/same-frame concealment, no actual packet mutation |
| VHS | Tape profile is separate from tracking/slip/dropout/noise state |
| TERMINAL | CRT green-phosphor profile, with convergence/sync faults separate |

Stored masks and fixed-stride floats are intentionally not migrated to visually equivalent settings. Only fault preferences reset; camera and other application preferences remain.

## Representation and fault points

| Point | Fault | Mechanism / representation |
|---|---|---|
| SENSOR | PIXEL DAMAGE | Stable dead/hot sample sites; activity changes with heat |
| SENSOR | EXPOSURE | Row exposure attenuation, rolling phase and integration |
| READOUT | ROW ERROR | Persistent weak bands, moving readout displacement, transient missing/repeated row pairs |
| DATA | BIT ERROR | Sample-component bit flips, burst-local bit errors |
| DATA | ADDRESS ERROR | Byte-address misalignment and component misread in RAW16 or RGB8 |
| CFA / RECONSTRUCTION | CFA ERROR | Bayer phase misinterpretation in regions |
| CFA / RECONSTRUCTION | DEMOSAIC ERROR | Wrong neighbor interpolation; RGB path explicitly remosaics camera RGB |
| COLOR | CHROMA ERROR | Chroma displacement and chroma sampling/coarsening in luma/chroma space |
| COLOR | COLOR MAP | Camera luminance mapped through a false-color transfer function |
| CODEC / STREAM | BLOCK ERROR | Decoded-image block quantization and block address error model |
| CODEC / STREAM | STREAM ERROR | Decoded-region erasure / same-frame concealment; no actual packets or compressed bitstream are edited |
| MEDIA | VHS | Bandwidth / chroma response profile, separate tracking drift, tracking slips, dropout and tape noise |
| DISPLAY | CRT | Scan / phosphor profile (color or monochrome), separate convergence bias and sync drift |

CLEAN is an empty route. RAW is one representation, not ground truth. RGB reconstruction faults are explicitly approximations, not access to sensor CFA. RAW16 processing supports SENSOR through CFA; downstream profiles cannot be written into a meaningful Bayer DNG. Original RAW recording remains an explicitly unprocessed signal tap.

## Parameters and time

Settings store named, variable-length controls for each fault, plus a long identity seed. The compact controls are UI macros, not a common physical parameter layout. A compiler maps them to unrestricted named mechanism parameters. Media/display profile values occupy a separate immutable map from fault parameters. The global LEVEL is also a macro: it changes densities, offsets, attenuation, event impact or profile mixture depending on the fault; renderers never multiply every fault by a common strength uniform.

A frame contains immutable fault nodes: IDENTITY (structural seed and biases), MOTION (continuous drift and phase), EVENT (incident serial, position and envelope), and compiled mechanism parameters. Random streams are separated by purpose and fault identity. Drift interpolates a slow random field; event slots use a separate session salt. Looking up a snapshot does not consume random numbers. A new session may have different incidents, while the stored identity retains its character. RESEED changes only identities of selected faults. Normal frame progression never changes the route or settings.

Intrinsic motion and incidents belong to the fault and continue without LIVE. LIVE only couples measured inputs; each unavailable or disabled source contributes zero. Motion feeds readout/tracking, timing feeds exposure and readout, heat feeds sensor activity, CPU/timing pressure feeds data/stream incidents, and audio feeds tape drift. No generic parameter interpolation or automatic chain switching remains.

## Capture contract

A camera timestamp and one immutable state produce one canonical processed texture. Preview and ordinary video consume that image; they do not re-run the fault program with independent state. A bounded three-slot history retains pending and UI-acknowledged textures. The shutter pins the acknowledged timestamp synchronously before posting GL work. JPEG reads that texture before the GL queue can overwrite it: pressing the shutter does not request another exposure, advance state or re-roll an event. JPEG resolution is the actual camera preview signal resolution, not an upscaled full-resolution still. UI and metadata must disclose this. Lossy JPEG/video encoding can of course change output pixels.

Draft preview uses the same time coordinate. Editing is disabled during recording to preserve apply/cancel semantics and the shared displayed/recorded image contract. A saved JPEG captures the displayed signal after an active draft has been applied or discarded; capture is blocked during editing. Camera close or reconfiguration invalidates retained frames. Buffers and readback are bounded and released on teardown.

RAW photos require a different exposure and representation; they must never be described as exact copies of the RGB viewfinder. Processed RAW reuses the latched fault snapshot, original RAW bypasses processing. This is an explicit constraint, not a claim of RGB/RAW equality. Original RAW videos retain their existing bounded queue and timestamped DNG sequence. A common node/state representation permits future timestamp-matched processed-RAW recording without a second fault model.

A signal tap is a causal prefix ending at a fault point. Frame snapshots can be restricted to a prefix for processing/recording extensions; no arbitrary reordering is allowed. The initial camera UI records the final selected route, including VHS/CRT, or the existing RAW taps.

## Migration and acceptance

Use a new settings key/schema; reset old effect/AUTO values to CLEAN and default coupling preferences. Preserve camera, quality, language and location settings. Do not change application identity, release version or publish anything as part of this work.

Verify independent fixtures for structural persistence, reseed isolation, continuous drift, temporary events, pure repeated snapshots, no chain changes, named parameter roundtrip, causal tap prefixes, RAW bounds/Bayer phase, and shader output. Build both shared-code variants, run lint/localization checks, and use emulator GPU/capture checks when available. Device sensor fidelity, thermal behavior and real hardware RAW matching need explicit reporting, not invented validation claims.
