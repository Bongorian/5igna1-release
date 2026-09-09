# UI and display polish — development verification, 2026-09-09

Scope update — 2026-09-10: this development change shipped in [1.6.0](../../CHANGELOG.md). Original test conditions below remain historical. For final capture controls and guide behavior, see [compact controls](COMPACT_CAPTURE_UI.md). See the [audit inventory](README.md) for the separate unreleased Pixel 9 fix.

Development branch: `codex/adaptive-window-modes`. This is not a release or store submission.

## Changes

- Capture format now sits between the capture mode icons and a compact two-line LIVE control. RAW ZIP uses two lines. Utilities are grouped above the portrait viewfinder, and move to the top of the right column in wide windows; the viewfinder occupies the left column.
- SEED/RESEED updates the existing seed field without rebuilding the editor or scheduling a scroll restoration. Apply/cancel and fixed overrides retain their existing semantics.
- Display rotation uses the inverse display rotation in GL texture coordinates. SurfaceTexture remains responsible for the producer transform; RAW sensor-relative orientation and selfie metadata retain their existing formulas.
- NETWORK intentionally replaces coarse flat damaged tiles with short noisy scanline runs, chromatic noise and displaced samples. Intrinsic fault time still advances without LIVE; automatic congestion/frame holds require LIVE.
- LED uses square modules in output pixel coordinates, including when preceding MEDIA stages resize the input. Failed modules darken coherently and refresh variation acts per module. These are RGB approximations; no real packets are modified.

## Validation

- `test`: 36 unit tests per variant, zero failures in all four variants.
- F-Droid and Play debug APK builds, F-Droid debug lint and all 497 localized strings passed.
- Physical Android 16 device: SEED/RESEED in basic and advanced editors, signed seed entry, overflow rejection, cancellation and Apply persistence passed. Added same-view and scroll-position checks over subsequent frames after a scrolled RESEED.
- Physical device: adaptive layout and capture controls passed; screenshots inspected in portrait and landscape. Four display rotations, stream dimension swaps, camera recovery, TAP restoration and feedback draft restoration passed. Coordinate unit tests verify inverse quarter turns without adding a reflection; these are geometric checks rather than an optical calibration of a camera target.
- GPU transport checks: all 16 MEDIA/DISPLAY combinations distinct; digital pass-through, media reduction, cable differences, network hold/resume, seeded noise repeatability and temporal change passed. White-field tests at 960×720 and 720×960 verify coherent square LED modules and fine multilevel NETWORK noise.
- Legacy GPU comparison: 573 byte-exact comparisons passed on Mali-G925. The frozen renderer was not changed. New transport/profile controls are excluded from that pre-transport oracle and covered by transport checks; its missing transport state reset otherwise contaminates subsequent legacy comparisons. No migration golden hashes were changed.

New faults intentionally change NETWORK/LED output. Other effect shader branches retain their prior formulas. Release artifacts and submissions were not updated.
