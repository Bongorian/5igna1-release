# Validation and limits

[Guides](README.md) · [日本語](VALIDATION.ja.md) · [Run checks](DEVELOPMENT.md)

The 1.7.3 release passed lint, 80 unit tests in each of four variants (320 total), FOSS dependency checks, debug builds and unsigned FOSS release assembly. Release packaging additionally verifies the existing distribution certificate, application ID and version. Locale, dependency inventory, release metadata, repository and documentation checks passed.

Analog FPV GPU checks on the connected development device before release preparation covered neutral output, deterministic replay, smoothly moving broad color patches, localized interference and all six controls. PRO sheets in both orientations and saved-signal behavior were checked during the preceding UI fixes. After USB reconnection, the final DEV build was installed successfully on the connected 25060RK16C; Android reports 1.7.3-debug / versionCode 22.

Play update availability requires an eligible Play-installed release and account; actual store update availability has not been verified end to end. DEV explains its package limitation. The update SDK is confined to Play and does not add application permissions.

Use [UI verification](UI_REVIEW.md) for current reproduction steps. The JVM suite retains independent RAW fixtures and pre-Kotlin golden hashes in the test sources. [Retained measurement data](audit/README.md) are historical comparisons with their original conditions, not current benchmark claims.

Emulator checks do not certify hardware RAW, microphone quality, sustained speed, heat or battery use. No broad device matrix or multi-hour recording/energy study is claimed. Include version, device/OS, format, resolution/fps, processing mode and reproduction steps with an issue.

[Earlier validation records](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/VALIDATION.md) remain available in Git history. Read them at their named source scope.
