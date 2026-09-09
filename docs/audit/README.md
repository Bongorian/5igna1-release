# Audit inventory

[日本語](README.ja.md) · [Validation and limits](../VALIDATION.md) · [Documentation map](../DOCUMENTATION.md)

Reviewed on 2026-09-10, against published `v1.6.0` and the unreleased Pixel 9 fix `742c094`. Release membership describes where a change shipped; it does not turn a development test into a test of the signed release artifact.

| Status / source scope | Records | Purpose and limits |
|---|---|---|
| Unreleased after 1.6.0 | [Pixel 9 DNG sizes](PIXEL_9_DNG_SIZES.md) | RAW size compatibility; fixed source `742c094`, physical Pixel 9 DEV verification. |
| 1.6.0 | [Adaptive windows](ADAPTIVE_WINDOW_FEEDBACK.md) | Rotation, resizing, feedback drafts; earlier UI geometry is refined by the compact-controls record. |
| 1.6.0 | [UI/display polish](UI_DISPLAY_POLISH.md) | Rotation, SEED and NETWORK/LED rendering; its two-line portrait LIVE description is superseded by compact controls. |
| 1.6.0 | [Compact capture controls](COMPACT_CAPTURE_UI.md) | Final format cycling, RAW video opt-in and guide layout before 1.6.0. |
| 1.5.1 | [Foreground lifecycle](FOREGROUND_1_5_1.md) | Stop/save on exit; supersedes earlier background-recording behavior. |
| 1.5.0 | [TAP and media](TAP_MEDIA_1_5_0.md) | TAP, TIME ECHO, MEDIA/DISPLAY and SEED; background recording is historical. |
| 1.4.0 | [Adaptive capture/audio](ADAPTIVE_CAPTURE_AUDIO.md) | Recommendations and normal/strict capture; publication follow-up is at the end. |
| 1.3.1 | [Guide/thermal](GUIDE_AND_THERMAL.md) / [日本語](GUIDE_AND_THERMAL.ja.md), [JSON](guide-thermal-results.json) | Short thermal/workload observations; not a controlled sustained-temperature comparison. |
| 1.3.1 | [Experimental results](experimental-signals-results.json) | Device input and artifact results; interpretation in [Experimental features](../EXPERIMENTAL_SIGNALS.md). |
| 1.3.0 | [RAW first pass](RAW_OPTIMIZATION.md) / [日本語](RAW_OPTIMIZATION.ja.md), [JSON](raw-optimization-results.json) | PIXEL DAMAGE/ROW ERROR; distinct from the remaining-effects pass. |
| 1.3.0 | [Remaining RAW](RAW_REMAINING_OPTIMIZATION.md) / [日本語](RAW_REMAINING_OPTIMIZATION.ja.md), [JSON](raw-remaining-results.json) | Other RAW stages; differential fixtures and scoped timings. |
| 1.3.0 | [GPU/buffers](FAULT_RENDER_AND_BUFFERS.md) / [日本語](FAULT_RENDER_AND_BUFFERS.ja.md), [JSON](fault-render-and-buffers-results.json) | Renderer comparisons and buffer ownership; historical shader hashes. |
| Through 1.3.0 | [Migration log](VALIDATION_THROUGH_1_3_0.md) / [日本語 summary](VALIDATION_THROUGH_1_3_0.ja.md) | Chronological redesign/tutorial/Kotlin evidence; Japanese edition is a summary. |
| 1.0.0 | [Original validation](VALIDATION_1_0_0.md) | Original release only; does not establish later device coverage. |
| Historical design | [Injection assessment](DATA_INJECTION_POINTS.md) / [日本語](DATA_INJECTION_POINTS.ja.md) | Pre-TAP feasibility, not current feature status or implementation instructions. |
| Maintained inventory | [Asset provenance](ASSETS.md), [hashes](assets.json) | Application/store assets only; GitHub Pages assets have a separate site-branch inventory. |
| Maintained inventory | [Dependencies](dependencies.json) | Reviewed dependency graph; compare with resolved build output, not historical benchmark data. |

## Stocktake findings

The review covered the 28 pre-existing files: 21 Markdown records and 7 JSON files. No byte-identical duplicates were found. English/Japanese editions and separate optimization passes serve different purposes and are retained. All JSON files parse and have explanatory references. The 29 asset entries match their recorded hashes; the resolved dependency inventory matches the reviewed graph.

The validation index had omitted 1.6.0 UI/display records and the unreleased Pixel 9 fix. Some 1.6.0 records still read as unshipped work without a release follow-up. Those navigation/status gaps are corrected without rewriting the original measurements. The 26-entry count in the earlier asset review is historical; the current count is recorded in its dated follow-up.

## Maintenance

- Preserve measurements, hashes, original test conditions and historical publication statements. Append a dated correction or release follow-up instead of replacing evidence with newer results.
- Keep raw JSON alongside the report that explains it. Do not merge independent runs solely because their topic overlaps.
- Record source commit/tag, date, device, build flavor, workload, result and limits in new verification records. State whether evidence is checked-in, externally linked or only local.
- Update this inventory and the validation index when adding an audit. Link every new file from the documentation map and run the documentation check.
- These records do not establish universal device coverage. Local test logs/captures mentioned in reports are not automatically available from a clean checkout; this stocktake did not rerun historical benchmarks.
