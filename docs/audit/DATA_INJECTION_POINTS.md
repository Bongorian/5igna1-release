# Signal injection feasibility

[日本語](DATA_INJECTION_POINTS.ja.md)

Scope: code audit on `codex/interactive-guide-thermal-inputs`, 2026-09-09. This is a design assessment; injection is not implemented in this branch.

The pipeline can support images, procedural noise and other frames, but currently accepts one image input per GPU pass. `EffectChain.render` converts the camera's external texture into ordered 2D processing passes; `effect.glsl` samples only `cam`. STREAM ERROR re-samples the current input: it does not retrieve a previous frame. `EffectState.Frame.through(point)` selects a causal prefix of fault nodes, not an injection API.

| Boundary (`Effects.Point`) | Useful injected data | Existing representation / proposed insertion |
| --- | --- | --- |
| Before SENSOR | Replacement image, noise source | External camera texture → 2D input adapter; define orientation and aspect fit once |
| SENSOR | Exposure field, defective-pixel mask | GPU RGB approximation; RAW16 needs sensor-range samples |
| READOUT | Row offsets, line replacement | After ROW ERROR, before DATA; preserve Bayer parity in RAW |
| DATA | Noise, byte/bit masks, displaced image fragments | After BIT/ADDRESS ERROR; promising first experiment |
| RECONSTRUCTION | CFA samples, cross-frame mosaics | RGB simulation differs from RAW CFA data; explicit separate adapters |
| COLOR | Image blending, palette/noise field | After CHROMA/COLOR MAP; simplest intelligible image-blending entry point |
| STREAM | Delayed frame, block replacement | Dedicated previous-frame textures; current STREAM ERROR has no temporal memory |
| MEDIA | Feedback, tape dropout mask | Before/after VHS with explicit ordering |
| DISPLAY | Alternate phosphor/scan image | Before/after CRT, still inside the canonical saved signal |

A boundary must remain addressable even when its ordinary effects are disabled. Scheduling injection only inside the selected-effect loop would silently skip it. Define whether an injection is before or after a stage; do not add it as an arbitrary effect ID that changes existing ordering.

## Recommended first experiment

One optional injection slot at DATA or COLOR, with either a fixed image or seeded procedural noise. Use an immutable specification containing source identity, boundary, blend operation, amount, scale/crop, seed and sampled time. Load an image once, bound its decoded dimensions and upload it once. Blend in a two-input shader; do not read back and re-upload camera pixels each frame. Keep an OFF path identical to the current pipeline. No new library or network service is necessary.

Put injection before the canonical processed buffer is published. `GlitchEngine.frame` shares that buffer with the preview and encoded video, and `photo` reserves an acknowledged buffer for JPEG. Adding an overlay only in the screen blit would make the saved result differ from what the user saw. Asset replacement must retain the old texture/specification until pending captures release it; persist a stable asset reference and sufficient metadata to describe the captured signal.

This costs another texture and usually another full-frame pass. One RGBA8 1920×1080 texture is 7.91 MiB; 3072×4096 is 48 MiB, excluding driver overhead. Account for the extra pass in `AdaptiveLoad` and avoid a full-resolution default. At high resolution this feature can worsen heat; measure GPU time and memory before widening its scope.

## Later phases

1. Add a bounded one- or two-frame GPU history for temporal replacement. Define whether history contains the pre-injection or post-injection signal, timestamps, delay behavior during throttling, and reset on camera/resolution/lifecycle changes. Never reuse `FrameHistory<SignalBuffer>` slots as writable feedback storage: those slots protect displayed/pending JPEG captures. Never sample a texture while rendering into that same texture.
2. Add a RAW16 adapter if sensor-data injection is desired. `RawGlitch.chain` processes little-endian sample arrays with explicit dimensions, white/black levels and bounded scratch buffers. Map source intensity into those levels, respect CFA layout unless intentional CFA corruption is selected, and keep DNG container metadata valid. RGB bytes cannot simply be inserted into a RAW buffer. RGB preview remains an approximation.

## Acceptance checks before implementation ships

OFF output matches existing golden images; deterministic noise repeats with the same immutable inputs; changing an asset during capture preserves the captured frame; preview/JPEG/video use the same injection state; RAW bounds and metadata remain valid; history remains bounded across resize/pause/restart; measured load includes the added pass. Verify image aspect/orientation and missing-source behavior without silently substituting unrelated data.

Code anchors: `Effects.kt:81`, `EffectChain.kt:160`, `EffectState.kt:130`, `GlitchEngine.kt:1356` and `:1528`, `RawGlitch.kt:24`, `res/raw/effect.glsl:5`. Paths are under `app/src/main/kotlin/com/bongorian/signa1/` except the shader.
