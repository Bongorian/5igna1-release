# Creative recipes

Treat these as starting points. Light, movement, and the subject change what a setting becomes. Watch the preview and adjust one stage at a time.

[All guides](README.md) · [Effect models](EFFECTS.md) · [日本語](RECIPES.ja.md)

## Unravel an outline

**ROW SHIFT → CHROMA** / processed JPEG or normal video

Select both effects in the chain. ROW SHIFT displaces rows; CHROMA separates color boundaries. Keep CHROMA subtle at first, and tune ROW SHIFT until the subject is still legible. Vertical lines make it easier to see the displacement.

For processed RAW, only ROW SHIFT in this recipe is available.

## Layer an unstable picture

**LINE LOSS → VHS** / processed JPEG or normal video

Remove or repeat rows with LINE LOSS, then add tracking disturbance and noise with VHS. Increase the line-band height gradually so it does not hide more of the subject than you intend.

For video, turn LIVE FAULT on with chain switching disabled. The two selected stages remain in place while their settings vary. Turn LIVE FAULT off to return to the manual state.

## Develop a damaged signal

**DATA SHIFT → CFA OFFSET** / a camera that supports processed RAW

Choose processed RAW and select both stages. DATA SHIFT displaces the underlying byte stream; CFA OFFSET changes the color-array phase. Start gently, save one DNG, and open it in a RAW developer before adjusting further.

The RAW preview is an approximation. Judge the developed DNG as well as the screen. If you want an original for comparison, switch to original RAW and take a separate photograph; this release does not save original and processed RAW simultaneously.

## Meet something unexpected

Long-press **Random** to get a 2–4-stage chain. When a combination interests you, open each stage and reduce its strength to discover what it is doing.

Named preset saving is not available in this release. Keep a note of the chain and values if you want to return to them. The app remembers per-effect adjustments, but changing modes removes effects that the new mode cannot use.

## Nothing seems to change?

Check the overall strength and the stage strength. Effective strength is **overall × stage**. An off stage, zero strength, CLEAN, or original RAW can explain an unchanged image. [Troubleshooting](TROUBLESHOOTING.md)
