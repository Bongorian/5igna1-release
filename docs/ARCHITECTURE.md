# Architecture

[Redesign and release audit](design/FAULT_SYSTEM.md) · [日本語](ARCHITECTURE.ja.md)

The application and tests are Kotlin, with the GPU pipeline in GLSL. `CameraScreen` builds and binds the screen; `MainActivity` coordinates lifecycle and user actions. `CaptureStorage` persists completed captures while `GlitchEngine` owns camera and GL resources. Immutable snapshot collections take defensive copies and reject mutation, including through Java. Existing Android views and platform camera APIs keep the dependency surface small.

## Settings, time and pixels

`Effects` owns the thirteen fault IDs, eight causal points, control descriptors, RAW capability and generated shader definitions. `EffectParameters` stores immutable named controls and independent 64-bit identity seeds. The number of controls varies by fault. `EffectState` stores the selected route and user LEVEL macro; schema 3 deliberately resets release 1.0 effect settings.

`FaultModel.advance` runs once for each acquired camera frame. Its double-precision clock, camera timestamp and filtered device measurements feed a pure snapshot compiler. `FaultNode` contains an immutable identity, motion and event, with separate unrestricted maps for device profiles and named fault parameters. The renderer has no common strength/param1/param2/seed ABI. Structural, drift and event random domains are independent; event randomness includes a per-session salt. Snapshot reads do not draw randomness or advance the clock. LIVE only enables measured coupling and does not own intrinsic time evolution.

`FaultInputs` retains the release's foreground-bound motion, audio, thermal, CPU and capture-timing acquisition. Audio recording reuses recorder amplitude and releases the separate analysis microphone. Disabled LIVE coupling contributes zero. Closing the camera resets input integration without changing stored identities.

## Canonical GPU signal and shutter handoff

`GlitchEngine` acquires Camera2's SurfaceTexture once, evaluates one state and processes it at the actual source resolution. `EffectChain` follows causal order using ping-pong intermediates. It specializes/caches shaders per fault and caches uniform locations, so unrelated fault branches and uniforms can be optimized away. All pass buffers and programs are released with their GL context.

A `SignalBuffer` is a camera-derived processed texture plus immutable frame metadata. Preview and encoder blit that same texture; neither evaluates the fault model again. The preview can present fewer frames than a high-fps encoder.

`FrameHistory` bounds the preview handoff to three slots. Each submission carries a monotonic presentation token through EGL, mapped to the original camera timestamp in its immutable payload. TextureView acknowledges that token on the UI thread. Camera clocks are not assumed to be identical to EGL clocks. A shutter synchronously reserves the corresponding slot before queuing GL readback. Until released, later camera frames cannot overwrite it. Unacknowledged submissions and the last acknowledged image are also retained; if the UI stalls, preview applies backpressure. An encoder scratch buffer can continue recording while the screen is behind. Camera lifecycle changes invalidate outstanding leases.

JPEG reads the reserved signal once, with striped readback to bound extra memory, and saves it on the existing file worker. Its resolution is a selected live output, not a later still exposure. EXIF records the displayed timestamp, named controls, identity, event and compiled state. Camera exposure metadata is attached only from the matching bounded capture-result lookup.

## Editing, RAW and recording taps

Draft edits leave committed settings unchanged. Apply commits; dismiss/pause cancels. The draft uses the same running timeline. Draft editing cannot start through the camera UI while recording, and recording refuses an active draft, so preview and encoder keep a common route. Both on-screen and volume-key capture require applying or discarding an active effect or LIVE draft first.

`RawGlitch` adapts the same immutable nodes to RAW16. Readout movement/reuse preserves Bayer parity; CFA ERROR and byte-address errors can intentionally change interpretation. DNG photos use a separate RAW exposure with the latched state and explicitly disclose that difference. Original DNG and original RAW video bypass faults. `RawVideoRecorder` retains its bounded queue, timestamp matching, interruption handling and ZIP packaging.

`Frame.through(Point)` selects a causal prefix for intermediate recording extensions. It cannot reorder a route. Current user-facing taps are final processed RGB (including media/display), processed RAW and original RAW. There is no second still-only or video-only fault generator.

## Migration and distribution

`fault_state_v3` and `fault.v3.*` preferences replace old effect/AUTO values. Camera quality, language, GPS and application ID remain unchanged. No dependencies or proprietary services are added. All code remains in `src/main` for FOSS and Play variants. [Validation](VALIDATION.md).

## Capture workspace and media preview

`SignalSheet.anchoredPick` positions the format choices directly under their trigger. Light, GPS and video-audio controls share the top toolbar; audio remains visible in photo mode. `FaultStateDialog` retains ordered event-meter rows while live values update and reserves space below the camera preview.

`MediaPreview` scans readable image/video entries in the app capture folders and presents a single mixed sequence. It decodes photos on one worker (up to 2048 pixels on the long edge) and uses the system thumbnail for DNG. Only the current video has a MediaPlayer/Surface. A GL queue barrier confirms camera release before video preparation. Dismissal cancels pending UI work, releases playback/audio focus and then reattaches the camera; stale decode and player callbacks are generation-guarded. Backgrounding closes the viewer. External viewing is explicit and uses a separate document task.

The camera watches for unacknowledged preview delivery for six seconds and makes up to three reconnect attempts outside capture or cooling pauses. Error/disconnection callbacks use the same recovery path; leaving the foreground cancels it. Capture settings and effect selection are retained.
