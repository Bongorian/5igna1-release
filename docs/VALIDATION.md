# Validation and limits

[Guides](README.md) · [日本語](VALIDATION.ja.md) · [Run checks](DEVELOPMENT.md)

This page indexes evidence by source scope. Test results apply to the named build, device and workload; historical tests do not establish coverage for new code.

| Scope | Evidence |
|---|---|
| Unreleased after 1.6.0: Pixel 9 RAW/DNG size compatibility | [Pixel 9 DEV verification](audit/PIXEL_9_DNG_SIZES.md) |
| 1.6.0: adaptive windows, rotation and feedback | [Adaptive windows](audit/ADAPTIVE_WINDOW_FEEDBACK.md) |
| 1.6.0: SEED, NETWORK/LED and compact capture/guide UI | [Display polish](audit/UI_DISPLAY_POLISH.md), [compact controls](audit/COMPACT_CAPTURE_UI.md) |
| 1.5.1: recording stops/saves on exit, removed service permissions | [Lifecycle verification](audit/FOREGROUND_1_5_1.md) |
| 1.5.0: TAP, MEDIA/DISPLAY and SEED | [Physical verification](audit/TAP_MEDIA_1_5_0.md) |
| 1.4.0: recommendations, normal capture and linked audio | [Capture policy verification](audit/ADAPTIVE_CAPTURE_AUDIO.md) |
| 1.3.1 interactive guide and immediate Settings | [Guide and thermal investigation](audit/GUIDE_AND_THERMAL.md) |
| 1.3.1 optional input sensitivity and image artifacts | [Experimental features](EXPERIMENTAL_SIGNALS.md), [recorded results](audit/experimental-signals-results.json) |
| 1.3.0 RAW/GPU optimizations | [RAW first pass](audit/RAW_OPTIMIZATION.md), [remaining RAW](audit/RAW_REMAINING_OPTIMIZATION.md), [GPU/buffers](audit/FAULT_RENDER_AND_BUFFERS.md) |
| Redesign, tutorial and Kotlin migration through 1.3.0 | [Historical validation log](audit/VALIDATION_THROUGH_1_3_0.md) |
| Original 1.0.0 | [Original validation](audit/VALIDATION_1_0_0.md) |

The JVM suite retains the pre-Kotlin golden hashes and independent RAW differential fixtures. Device checks exercise actual camera/GL integration separately. Android Emulator results do not certify a phone’s microphone quality, hardware RAW, sustained speed, temperature or battery use. RAW is a separate exposure and representation; exact displayed-frame JPEG capture applies only with ADVANCED ON in 1.4.0.

No multi-hour recording, broad device matrix, GPU energy measurement or controlled long-duration thermal comparison is claimed. For an actionable issue, include app version, device/Android version, capture format, size/fps, ADVANCED/EXPERT switches and steps to reproduce.


[Complete audit inventory](audit/README.md) separates released development evidence, unreleased fixes, historical measurements and maintained inventories. The Pixel 9 fix is not included in published 1.6.0.

[Rendering/RAW workload investigation](audit/LOAD_INVESTIGATION.md): Pixel 9 test-only prototypes, byte comparisons and performance limits; no production change.
