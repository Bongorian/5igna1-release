# Validation and limits

[Guides](README.md) · [日本語](VALIDATION.ja.md) · [Run checks](DEVELOPMENT.md)

The 1.7.2 release passed lint, 75 unit tests in each of four variants, signed FOSS packaging and debug builds. Subsequent main changes fix PRO/shared-sheet close controls and saved-media viewer icons. Their checks cover PRO sheets in both orientations and existing saved-signal behavior; these changes are not a new published release.

Use [UI verification](UI_REVIEW.md) for current reproduction steps. The JVM suite retains independent RAW fixtures and pre-Kotlin golden hashes in the test sources. [Retained measurement data](audit/README.md) are historical comparisons with their original conditions, not current benchmark claims.

Emulator checks do not certify hardware RAW, microphone quality, sustained speed, heat or battery use. No broad device matrix or multi-hour recording/energy study is claimed. Include version, device/OS, format, resolution/fps, processing mode and reproduction steps with an issue.

[Earlier validation records](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/VALIDATION.md) remain available in Git history. Read them at their named source scope.
