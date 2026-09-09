# Capture formats

Recording stops and saves when you leave the capture screen or lock the device. [Recording and permissions](RECORDING.md).

[Guides](README.md) · [日本語](FORMATS.ja.md)

| Format | What is recorded | Relation to the viewfinder |
|---|---|---|
| JPG | Current processed camera signal; displayed signal with ADVANCED ON | OFF may capture a later processed frame; ON pins the displayed frame, with JPEG compression |
| MP4 | Interval of canonical processed camera images | Same state progression; display refresh may show fewer frames |
| RAW (DNG) | Separate RAW exposure with latched fault nodes | Same fault state, different signal/representation and exposure |
| Experimental RAW video ZIP | Original, silent DNG sequence | Camera-timestamped RAW tap, not rendered RGB video |

JPEG uses one processed output buffer with ADVANCED OFF. The GL thread snapshots its current image/state when it handles the shutter, before asynchronous saving. ADVANCED ON instead pins the UI-acknowledged texture synchronously and retains a three-slot display history. Both preserve all selected FAULT passes and immutable saved state. Resolution is an actual supported live output; [device recommendations](PERFORMANCE.md) use RAM, available CPU cores and camera/encoder capabilities. Manual sizes and Maximum remain available.

JPEG includes every selected point, including VHS and CRT. Quality is selectable at 85/95/100. Lossy JPEG and video encoding can change individual pixels; the captured source image/state is shared.

Processed RAW supports PIXEL DAMAGE, EXPOSURE, ROW ERROR, BIT ERROR, ADDRESS ERROR and CFA ERROR. Other stages require reconstructed/component/media/display representations and are not written into Bayer DNG. RAW16 is not a privileged true image. Its developed appearance depends on the RAW developer and differs from the camera's RGB processing. The file description distinguishes the latched preview state from the separate RAW exposure timestamp.

Normal video supports H.264/H.265 and optional sound, subject to camera/encoder capabilities. [RAW sequence details](RAW_VIDEO.md). The internal frame model supports causal prefixes ending at a FAULT POINT for future intermediate-signal recording; the initial UI records the final selected RGB route or the existing RAW taps.

New photographs (JPG/DNG) and ordinary videos (MP4) all save to **DCIM/5igna1**, a shared camera album. Existing files are not moved. Experimental RAW sequence ZIPs save to **Download/5igna1**.

The development build includes conservative device recommendations and automatic preview workload reduction. [Heat and workload](PERFORMANCE.md).

## Resolution-linked audio (experimental)

This independent Settings switch starts OFF and only affects ordinary video with audio ON. Requested mono AAC tiers use actual output pixel area: up to QVGA 8 kHz / 24 kbps; VGA 16 kHz / 48 kbps; HD 32 kHz / 64 kbps; Full HD 48 kHz / 128 kbps; above Full HD 48 kHz / 192 kbps. With the switch OFF, audio remains 48 kHz / 192 kbps. Supported sample rates and bitrates are checked against the platform AAC encoder; the platform may adjust actual encoding. It does not enable the microphone, change LIVE sensitivity, or affect silent RAW sequences.

With experimental device response enabled, processed RAW also supports MOTION BLUR, THERMAL NOISE and SMEAR. These additions and their approximation limits are described in [Experimental features](EXPERIMENTAL_SIGNALS.md).
