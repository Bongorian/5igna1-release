# Validation and limits

[Guides](README.md) · [日本語](VALIDATION.ja.md) · [Run checks](DEVELOPMENT.md)

The 1.7.4 release passed lint, 82 unit tests in each of four variants (328 total), FOSS dependency checks, unsigned FOSS release assembly and both debug flavors. The new JVM tests check every selectable model's active inspection keys and stage-specific snapshot compatibility. Release packaging verifies the existing distribution certificate, application ID and version.

## State consistency audit

- Advanced editor values now require matching stage macros, identities, overrides, level, experimental mode and camera generation. All visible keys must exist in the evaluated node; missing values are not replaced with invented zeroes.
- STREAM/VHS/CRT model pickers rebuild the editor. Fixed model sliders now rebuild for both `streamKind` and `transportKind`. Network display summaries also reject snapshots from previous settings or camera generations.
- Camera open/configure callbacks already check the generation, capture results check the current session, and PRO sheets dismiss when their lens changes. The queued live-frame callback now checks camera generation as well as configuration revision.
- RAW/photo/video routes are filtered when snapshots are produced; generation/configuration checks protect camera restarts. This is a source audit, not a claim of support for every sensor/format combination.

The original ADVANCED Analog FPV crash was reproduced on the unpatched 1.7.3 DEV build with the actual model picker. The patched development build passed the original FPV GPU checks and three model round trips before this broader audit. The new `editor-transitions` device check exercises model pickers and fixed model sliders, normal/ADVANCED modes, portrait/landscape, all active groups, stale seeds, Apply/Cancel and background/resume. On the connected 25060RK16C, this check passed 80 model selections including fixed model sliders, all active groups, stale seed rejection, normal/ADVANCED, portrait/landscape, Apply/Cancel and background/resume.

The physical lens/format check passed on all three exposed cameras (rear wide, rear ultra-wide, front): matching sensor results and JPEG focal-length metadata, preserved settings, native uncropped framing, a physical-sensor DNG and MP4 recording with the lens-switch guard. The final 1.7.4-debug / code 23 build is installed on the device.

Play's actual update-available UI still requires an eligible Play-installed release and account and has not been verified end to end. [Distribution status](LAUNCH_TASKS.md) separately records upload, review and tester delivery.

Use [UI verification](UI_REVIEW.md) for current reproduction steps. The JVM suite retains independent RAW fixtures and pre-Kotlin golden hashes in the test sources. [Retained measurement data](audit/README.md) are historical comparisons with their original conditions, not current benchmark claims.

Emulator checks do not certify hardware RAW, microphone quality, sustained speed, heat or battery use. No broad device matrix or multi-hour recording/energy study is claimed. Include version, device/OS, format, resolution/fps, processing mode and reproduction steps with an issue.

[Earlier validation records](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/VALIDATION.md) remain available in Git history. Read them at their named source scope.

## Unreleased photo import and diagnostics

FOSS debug lint/build and 83 unit tests passed. A regression covers STREAM photo descriptions that omit default FPV macros. The `photo-import` instrumentation action supplies a selected content URI through the system-picker result contract, checks explicit apply/camera return and unchanged source/capture settings, and checks reviewed diagnostic clipboard content without automatic email insertion. Physical execution is pending installation confirmation on the connected CPH2437; the test's supplied picker result does not certify every third-party provider UI.
