# UI verification

[Development checks](DEVELOPMENT.md) · [日本語](UI_REVIEW.ja.md)

Use the dedicated `Signal_Review_1280_480` emulator (1280×2772, 480 dpi, API 35) for repeatable layout checks. It matches the reviewed phone’s effective density, not its OS skin or camera hardware.

1. Run `python3 tools/review-emulator.py` and start the printed emulator command.
2. Build and install the DEV and instrumentation APKs using [Development checks](DEVELOPMENT.md).
3. Run `DeviceChecks` with `-e action workspace-review -e language ja` (or `en`) for capture layouts and recording; `-e action chain-review` for chain editing; add `-e proSheets true` to workspace review for all four PRO sheets in portrait and landscape.
4. Run `python3 tools/collect-workspace-review.py --suite workspace` or `--suite chain`. Captures and hash manifests go to ignored `verification/` folders. PRO-sheet captures remain in the app’s verification files and can be read with `adb exec-out run-as`.

Inspect screenshots as well as test results. Checks cover shutter size and clipping, collapse behavior, status placement, icon targets and headings, and editor apply/cancel or dismissal. Viewer coverage uses `-e action saved-signal` and its `language-saved-signal-footer.png` capture.

Do not add every transient capture to the reference documentation. Preserve only evidence needed to explain an unresolved issue; use Git history for completed visual reviews.

[Archived workspace and chain screenshots](https://github.com/Bongorian/5igna1-release/blob/87d36c4acf2f4079c1d84562485d9a4fc2a8894b/docs/UI_REVIEW.md) retain original hashes and reproduction details. They show earlier UI, not the current placement of every control.
