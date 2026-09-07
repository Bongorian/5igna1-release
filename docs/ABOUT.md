# Every glitch is an encounter

5igna1 is a camera built around the fragile journey from light to an image.

A sensor measures light. Rows are read. Samples become data. Color is reconstructed. An image is encoded and displayed. Each step is an opportunity for something to shift, disappear, repeat, or be interpreted differently.

5igna1 turns those possibilities into controls for making pictures.

[Back to 5igna1](../README.md) · [Try a creative recipe](RECIPES.md)

## Ichigo-ichie

*Ichigo-ichie* is the idea of an encounter that belongs to its moment.

The scene in front of a camera is already changing. In 5igna1, it also meets a fault pattern: a row displaced across a face, a color boundary split by movement, a brief change that arrives while you are recording. LIVE FAULT can vary parameters and strength over time. The shutter chooses a moment from that meeting.

This does not mean every pixel is randomly regenerated for every shot. Some fault patterns stay in place until you change their placement; the same input and fixed state can produce the same result. The encounter comes from the scene, your decisions, and the timing of capture. A saved photograph holds that encounter still.

## Inspired by actual failure behavior

The effects are organized around the imaging pipeline, with controls related to the fault being represented.

| Model | What 5igna1 does |
|---|---|
| Readout displacement | ROW SHIFT moves bands of rows sideways. LINE LOSS removes or repeats row data. |
| Damaged data | BIT ROT flips bits. DATA SHIFT offsets a byte stream and reinterprets samples. |
| Color-array mismatch | CFA TEAR moves local color samples; CFA OFFSET changes their phase while retaining the original array metadata in processed RAW. |
| Reconstruction and display | DEMOSAIC approximates faulty interpolation; VHS and TERMINAL build display-inspired disturbances. |

These are models, not a physical fault simulator for a particular camera. GPU effects operate on an already processed RGB image. Processed RAW effects operate on RAW16 samples, and the DNG is subsequently interpreted by a RAW developer. SPECTRUM and TERMINAL also make deliberate artistic choices beyond literal fault reproduction.

The camera hardware is not modified or damaged. [The effect specification](EFFECTS.md) explains which behaviors are modeled and where the approximations begin.

## How it differs from a preset filter workflow

A preset filter commonly begins with a finished image and applies a chosen look. 5igna1 begins in the viewfinder and asks you to choose a failure mechanism.

| Creative choice | In 5igna1 |
|---|---|
| Where to intervene | Select a stage from sensor response through display. |
| How it behaves | Adjust controls such as row-band height, byte displacement, or color-array phase. |
| How effects meet | Combine stages in a fixed pipeline order, so each processes the previous stage's output. |
| When to keep the result | Watch the live image, apply your settings, and choose the capture moment. |
| How far into the image to work | Use GPU-processed JPEG/video, or supported RAW sample processing. |

5igna1 still uses digital image-processing effects; “glitch” is not a claim that no filtering occurs. The distinction is the creative model and workflow. It is a live camera instrument, with fault mechanisms you can explore, rather than a catalogue of named looks to apply to an imported photo. Import-and-edit is not part of this release.

## A small invitation

Start with one effect. Find what it does to a line, a shadow, or a moving subject. Add another only when you want to change the conversation between them.

You do not have to break the whole image to find something in it.
