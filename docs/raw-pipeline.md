# RAW representation adapter

[Architecture](ARCHITECTURE.md) · [日本語](raw-pipeline.ja.md)

RAW16 is a camera-acquired representation, not the sole true image. `RawGlitch` consumes the same immutable fault nodes as the GPU path. It validates buffer dimensions and levels, clones input ownership, preserves the RAW16 container size and bounds every output sample to the sensor white level.

Supported faults, in causal order: PIXEL DAMAGE, EXPOSURE, ROW ERROR, BIT ERROR, ADDRESS ERROR and CFA ERROR. Row displacement uses even sample offsets; row concealment reuses Bayer row pairs. CFA ERROR deliberately changes phase. ADDRESS ERROR shifts byte addresses and may misread sample/component boundaries. Exposure works relative to the black level. DngCreator writes the original camera metadata and a description identifying the applied state.

A RAW photograph is acquired by a separate still request. Its fault state is latched from the acknowledged preview at shutter time, but its scene samples and camera timestamp belong to that later RAW exposure. RGB previews remosaic processed camera RGB or approximate sensor errors; they are not actual RAW development. RAW and RGB use separate representation adapters, so identical parameters do not promise identical pixels, defect-site coordinates or developed appearance.

RAW photos use sensor processing; original RAW video bypasses processing. The existing DNG-sequence recorder remains bounded and timestamp matched. Future processed RAW recording can use the common fault-node snapshots; it is not currently offered. Color/media/display points require other signal representations and cannot be stored as valid processed Bayer data.
