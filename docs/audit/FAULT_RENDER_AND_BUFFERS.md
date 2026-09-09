# FAULT rendering and image-buffer reuse — 2026-09-09

[Japanese edition](FAULT_RENDER_AND_BUFFERS.ja.md)

Unreleased work on `codex/kotlin-cleanup`, based on `0695a57`. The GPU shader source, processing order, RGBA8 intermediate rounding and RAW arithmetic are unchanged. This pass reduces CPU submission overhead and reuses image storage.

## Changes and ownership

- Each private GL program caches its uniform locations and last uploaded values. Float values and transforms are compared by raw bits; profile-then-mechanism precedence remains unchanged. Unused uniform locations are skipped. Viewport, texture-unit selection and repeated identical vertex attributes are bound once per chain where possible.
- A caller with a privately owned, unpublished RGBA8 target of exactly the render dimensions can lend that texture to intermediate stages. The chain alternates between it and one private scratch texture. The final stage always lands in the requested target. All passes and quantization boundaries remain separate. Unknown targets, default framebuffer targets and source/target aliases retain the private-buffer fallback. Camera presentation and capture pinning keep their existing ownership rules.
- RAW chains use at most two full-size output/scratch arrays per invocation. Every stage writes every sample and reads an unchanged previous-stage input. Empty chains still return an owned copy; separate invocations never share scratch arrays. The standalone `apply` adapter retains its original copy semantics, including any trailing bytes outside its requested image area.
- DNG photo saving uses `ByteBuffer.wrap(data)`, the same array-backed adapter already used by RAW video. It removes the app's explicit direct-buffer allocation and full-image copy before the framework call. This does not make a claim about copies inside the framework.

## Full-size image storage

| Path | Before | After |
|---|---|---|
| Six-stage RAW chain, cumulative full-size arrays allocated per invocation, including returned output | 7 | 2 |
| Opted-in multi-stage GPU chain, private intermediate textures | 2 | 1 |
| DNG photo adapter, explicit additional full-size direct buffer | 1 | 0 |

At 4096×3072 RAW16, six-stage array allocation drops from 168 MiB to 48 MiB cumulatively per invocation; this is not a measured process peak. The DNG adapter removes an explicit 24 MiB allocation at that size. At 1920×1080 RGBA8, one fewer scratch texture is about 7.91 MiB of pixel storage, excluding driver alignment and bookkeeping. Required camera-buffer lifetime copies, per-stage writes, screen presentation and bounded CPU readback remain.

## Exactness and integration

- All 12 F-Droid debug unit-test methods pass, including 2,274 RAW differential cases, an added capture-isolation/ownership check, and unchanged Java migration golden hashes.
- `EffectChainReference` freezes the renderer from before this pass. On the physical GPU, all 501 comparisons match every RGBA byte: all 13 effects, control endpoints, LEVEL zero, fixed/LIVE state, 2D and external-OES input, transforms mutated in place, changing source/output dimensions, chain lengths 0–13, source/output aliases, fallback targets, and renderer recreation. Tests also assert one private intermediate for opted-in chains.
- The same DngCreator and camera metadata encoded a fixed 4096×3072 synthetic RAW buffer via direct and wrapped adapters. Both 25167440-byte DNG streams have SHA-256 `9c3ca3105c2a1328a9664863db45681a97393e8085735358f21dd8ea74b9c303`; the input array remains unchanged. No photograph was saved by this adapter comparison.
- `assembleFdroidDebug`, `assembleFdroidDebugAndroidTest`, `lintFdroidDebug` (0 errors, 136 existing warnings and one hint), and whitespace checks pass. The physical JPEG capture-contract check passes, including a pinned three-effect frame, later camera frames, exact saved JPEG pixels and snapshot metadata. Actual 4096×3072 RAW saving passes, including all six RAW effects; the resulting DNG opens, unpacks and develops with LibRaw. Its description contains all six effects. The optimized development app is installed and open; the temporary benchmark helper was removed.

## Timings

Model 25060RK16C / Android 16, Mali-G925-Immortalis MC12, debug APKs. The paired offscreen benchmark renders a fixed 1920×1080 RGBA input. The legacy and current renderers share one GL context and identical precomputed frames. Each of three rounds warms both renderers for 30 frames, then measures 60 adjacent old/new pairs with alternating order. CPU time covers the rendering thread's command submission, excluding the subsequent `glFinish`; wall time includes completion through `glFinish`. Readback/hashing is outside the timed interval. Per-column values are medians of three run medians, with their minimum–maximum in parentheses, in milliseconds.

| Workload | Submission CPU before | Submission CPU after | Completion before | Completion after |
|---|---:|---:|---:|---:|
| DEMOSAIC ERROR | 0.213 (0.133–0.280) | 0.197 (0.142–0.280) | 3.070 (2.588–3.103) | 2.933 (2.915–3.369) |
| CHROMA ERROR | 0.125 (0.084–0.210) | 0.115 (0.083–0.218) | 2.054 (1.397–2.700) | 2.080 (1.397–2.586) |
| COLOR MAP | 0.246 (0.236–0.282) | 0.259 (0.235–0.292) | 3.075 (2.913–3.094) | 3.060 (3.030–3.091) |
| BLOCK ERROR | 0.270 (0.258–0.277) | 0.295 (0.272–0.298) | 3.075 (2.957–3.145) | 2.936 (2.819–3.112) |
| STREAM ERROR | 0.250 (0.182–0.253) | 0.250 (0.194–0.264) | 2.805 (2.716–3.072) | 2.943 (2.847–3.060) |
| VHS | 0.254 (0.154–0.265) | 0.274 (0.180–0.281) | 3.102 (2.943–3.108) | 2.929 (2.786–3.046) |
| CRT | 0.242 (0.233–0.250) | 0.250 (0.244–0.257) | 3.162 (2.908–3.225) | 3.118 (2.933–3.288) |
| Downstream seven / fixed | 1.674 (1.421–1.927) | 1.476 (1.218–1.536) | 5.404 (5.173–5.475) | 5.593 (5.408–5.757) |
| Downstream seven / LIVE | 1.813 (1.768–1.893) | 1.663 (1.575–1.687) | 5.460 (5.449–5.642) | 5.728 (5.679–5.768) |
| All thirteen / LIVE | 3.053 (2.917–3.105) | 2.630 (2.479–2.643) | 8.204 (8.075–8.261) | 8.390 (8.271–8.533) |

The main observed benefit is reduced intermediate storage and lower submission CPU time for multi-effect chains. Completion time did not consistently improve; some workloads were slower. These CPU measurements must not be presented as equivalent FPS or end-to-end speed improvements. Single-effect differences vary in direction.

The live-camera comparison alternates before → after → after → before → before → after on the same device. Each condition uses 1080×1920 at a 30fps cap, adaptive resolution disabled, LEVEL 55%, LIVE disabled, six seconds of warm-up and ten seconds of measurement. PSS is the median of three memory samples per condition, followed by the median across the three runs. It includes the whole process and shared-page accounting, not just texture memory. CPU here is the whole process's consumed CPU time divided by rendered frames, unlike the narrower offscreen submission measurement above.

| Preview workload / metric | Before | After |
|---|---:|---:|
| Downstream seven / fps | 29.900 (29.900–30.100) | 29.797 (29.797–29.900) |
| Downstream seven / cpuMsPerFrame | 17.686 (17.635–19.458) | 18.662 (17.064–19.248) |
| Downstream seven / pssMiB | 277.110 (253.035–279.775) | 272.636 (267.232–273.443) |
| All thirteen / fps | 29.800 (29.700–30.000) | 30.000 (29.900–30.000) |
| All thirteen / cpuMsPerFrame | 18.283 (17.738–20.067) | 18.347 (17.689–19.133) |
| All thirteen / pssMiB | 291.984 (287.438–292.945) | 283.885 (283.171–283.894) |

Battery temperature across these final runs: 32.4–34.6 °C; reported thermal statuses [0] (0 means no reported throttling). Power connected, CPU/GPU clocks not locked. Foreground activity kept the final benchmark out of device background freezing; camera rendering was detached for offscreen measurements. Earlier exploratory background measurements are excluded. Results are limited to this device/build/workload and do not establish sustained recording or other-device performance.

[Full final measurements, hashes and summaries](fault-render-and-buffers-results.json).
