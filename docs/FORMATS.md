# Choosing a format

**Start with processed JPEG.** Choose DNG when you want to develop the sensor data yourself.

[All guides](README.md) · [Controls reference](USAGE.md) · [日本語](FORMATS.ja.md)

## Photographs

| Format | Use it for | Effects | Open with |
|---|---|---|---|
| Processed JPEG | An image ready to view or share | 15 photo-compatible effects | A normal photo viewer |
| Original RAW / DNG | Developing unmodified sensor data | Bypassed | A RAW developer |
| Processed RAW / DNG | Developing sensor, data, or color-array faults | 8 stages, SENSOR FAIL through CFA OFFSET | A RAW developer |

RAW is available only on cameras that report support. Front and back cameras may differ. Switching to an unsupported camera returns to JPEG. RAW capture uses 1× zoom.

Processed RAW changes the samples in a DNG. Its preview is an approximation on an already processed RGB image, so the developed file can differ in color, orientation, and appearance. This release does not save JPEG plus RAW, or original plus processed RAW, in one capture.

## Normal video

Normal video is saved as **MP4**, with AVC/H.264 or HEVC/H.265. If your playback app cannot open HEVC, try AVC. Audio is optional; enabling it requires microphone permission.

Choose from the resolutions and frame rates offered by the device. If playback or preview is slow, reduce resolution, frame rate, or the number of stages. Keep the app in the foreground with the screen on while recording.

## Experimental RAW video

Supported cameras can save original, silent DNG sequences as a ZIP. Effects and LIVE FAULT are bypassed. Extract the ZIP and use a RAW-compatible workflow to turn the sequence into video; a normal video player cannot play the ZIP directly.

Frames may be dropped when processing or storage cannot keep up. Read [RAW video and its limits](RAW_VIDEO.md) before relying on it for a recording.

## Where the files go

| File | Folder |
|---|---|
| JPEG and DNG photos | `Pictures/5igna1` |
| MP4 videos | `Movies/5igna1` |
| RAW video ZIPs | `Download/5igna1` |

The bottom-left thumbnail opens the last saved item. Use a gallery or file app for earlier captures. A missing DNG thumbnail does not by itself mean that capture failed.

GPS tagging starts off. If you enable it and a location is available, the file may contain the capture location. Check metadata before sharing if that matters for your image. [Privacy policy](PRIVACY.md)
