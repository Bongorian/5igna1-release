# Capture formats

[Guides](README.md) · [日本語](FORMATS.ja.md)

| Format | What is recorded | Relation to the viewfinder |
|---|---|---|
| JPEG | Last UI-acknowledged processed camera signal | Same acquired image and fault state, with JPEG compression |
| MP4 | Interval of canonical processed camera images | Same state progression; display refresh may show fewer frames |
| Original DNG | A separate camera RAW exposure | RGB viewfinder is an approximation; no fault processing |
| Processed DNG | Separate RAW exposure with latched fault nodes | Same fault state, different signal/representation and exposure |
| Experimental RAW video ZIP | Original, silent DNG sequence | Camera-timestamped RAW tap, not rendered RGB video |

For JPEG, the shutter pins the displayed texture before posting GL work. A later camera callback cannot replace it. A three-slot history retains UI-acknowledged and pending images; if presentation falls behind, it applies backpressure instead of overwriting a displayed image. JPEG is read once from the retained signal, not re-rendered from a future exposure. Its resolution is a supported live camera output: standard targets up to 2 MP, maximum choices are bounded to 8 MP for memory use. Large selections can reduce frame rate.

JPEG includes every selected point, including VHS and CRT. Quality is selectable at 85/95/100. Lossy JPEG and video encoding can change individual pixels; the captured source image/state is shared.

Processed RAW supports PIXEL DAMAGE, EXPOSURE, ROW ERROR, BIT ERROR, ADDRESS ERROR and CFA ERROR. Other stages require reconstructed/component/media/display representations and are not written into Bayer DNG. RAW16 is not a privileged true image. Its developed appearance depends on the RAW developer and differs from the camera's RGB processing. The file description distinguishes the latched preview state from the separate RAW exposure timestamp.

Normal video supports H.264/H.265 and optional sound, subject to camera/encoder capabilities. [RAW sequence details](RAW_VIDEO.md). The internal frame model supports causal prefixes ending at a FAULT POINT for future intermediate-signal recording; the initial UI records the final selected RGB route or the existing RAW taps.
