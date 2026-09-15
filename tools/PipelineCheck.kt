package com.bongorian.signa1

import java.util.*

/* Independent raw sample fixtures. Deliberately supplies physical values, bypassing UI macros. */
object PipelineCheck {
    internal fun check(ok: Boolean, message: String) {
        if (!ok) throw AssertionError(message)
    }

    internal fun samples(vararg v: Int): ByteArray {
        val b = ByteArray(v.size * 2)
        for (i in v.indices) RawGlitch.write(b, i, v[i])
        return b
    }

    internal fun node(id: Int, vararg pairs: Any): FaultNode {
        val p = LinkedHashMap<String, Float>()
        var i = 0
        while (i < pairs.size) {
            p.put(pairs[i] as String, (pairs[i + 1] as Number).toFloat())
            i += 2
        }
        return FaultNode(
            id,
            FaultNode.Identity(19),
            FaultNode.Motion(0.0, 0f, 0f),
            FaultNode.Event(0, 1f, .5f, 10f),
            p,
        )
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val bytes = samples(0x3412, 0x7856, 0xbc9a, 0xf0de)
        val address =
            node(
                Effects.ADDRESS_ERROR,
                "byteOffset",
                1,
                "addressRegion",
                512,
                "addressProbability",
                1,
            )
        check(
            Arrays.equals(
                RawGlitch.apply(bytes, 4, 1, 65535, 0, address),
                samples(0x5634, 0x9a78, 0xdebc, 0),
            ),
            "one-byte component/address misread",
        )
        check(
            Arrays.equals(
                RawGlitch.apply(
                    bytes,
                    4,
                    1,
                    65535,
                    0,
                    node(Effects.BIT_ERROR, "bitBlock", 2, "bitIndex", 0, "bitProbability", 1),
                ),
                samples(0x3413, 0x7857, 0xbc9b, 0xf0df),
            ),
            "low-bit XOR",
        )
        val mosaic = samples(100, 200, 300, 400)
        check(
            Arrays.equals(
                RawGlitch.apply(
                    mosaic,
                    2,
                    2,
                    4095,
                    0,
                    node(Effects.CFA_ERROR, "cfaRegion", 2, "cfaPhase", 0, "cfaCoverage", 1),
                ),
                samples(200, 100, 400, 300),
            ),
            "CFA X phase",
        )
        check(
            Arrays.equals(
                RawGlitch.apply(
                    mosaic,
                    2,
                    2,
                    4095,
                    0,
                    node(Effects.CFA_ERROR, "cfaRegion", 2, "cfaPhase", 1, "cfaCoverage", 1),
                ),
                samples(300, 400, 100, 200),
            ),
            "CFA Y phase",
        )
        check(
            Arrays.equals(
                RawGlitch.apply(
                    mosaic,
                    2,
                    2,
                    4095,
                    0,
                    node(Effects.CFA_ERROR, "cfaRegion", 2, "cfaPhase", 2, "cfaCoverage", 1),
                ),
                samples(400, 300, 200, 100),
            ),
            "CFA XY phase",
        )
        val exposure =
            RawGlitch.apply(
                mosaic,
                2,
                2,
                4095,
                0,
                node(
                    Effects.EXPOSURE,
                    "exposureDepth",
                    1,
                    "integration",
                    0,
                    "scanPhase",
                    0,
                    "exposurePhase",
                    0,
                ),
            )
        check(
            Arrays.equals(exposure, samples(50, 100, 150, 200)),
            "integrated exposure attenuation",
        )
        val w = 128
        val h = 96
        val pattern = ByteArray(w * h * 2)
        for (y in 0 until h) for (x in 0 until w) RawGlitch.write(
            pattern,
            y * w + x,
            1000 + (x % 2) * 500 + (y % 2) * 1000,
        )
        val row =
            node(
                Effects.ROW_ERROR,
                "rowGroups",
                20,
                "weakRows",
                1,
                "rowOffset",
                .8,
                "readoutShear",
                .4,
                "linePosition",
                .5,
                "lineHeight",
                .1,
                "lineRetention",
                1,
                "lineLoss",
                1,
            )
        val shifted = RawGlitch.apply(pattern, w, h, 4095, 0, row)
        var missing = 0
        for (y in 0 until h) for (x in 0 until w) {
            val v = RawGlitch.read(shifted, y * w + x)
            if (v == 0) missing++
            else
                check(
                    v == RawGlitch.read(pattern, y * w + x),
                    "readout and repeated rows preserve Bayer phase",
                )
        }
        check(missing > 0, "missing edge samples")
        val all = EffectState.defaults().chain(-1)
        val model = FaultModel(42)
        val `in` = FaultModel.Inputs()
        `in`.sensorNs = 100
        model.advance(0.0, `in`, FaultConfig.defaults())
        model.advance(.1, `in`, FaultConfig.defaults())
        val frame = model.apply(all.snapshot(true, 0), FaultConfig.defaults())
        for (size in arrayOf<IntArray>(intArrayOf(1, 1), intArrayOf(3, 5), intArrayOf(128, 96))) {
            val count = size[0] * size[1]
            val input = ByteArray(count * 2)
            for (i in 0 until count) RawGlitch.write(input, i, 256 + i % 3500)
            val before = input.clone()
            val out = RawGlitch.chain(input, size[0], size[1], 4095, 256, frame)
            check(Arrays.equals(input, before), "source ownership")
            check(out.size == input.size, "RAW container size")
            for (i in 0 until count) check(RawGlitch.read(out, i) <= 4095, "white-level bound")
            check(
                Arrays.equals(out, RawGlitch.chain(input, size[0], size[1], 4095, 256, frame)),
                "snapshot replay",
            )
        }
        check(
            Arrays.equals(frame.through(Effects.Point.DATA).ids(), intArrayOf(1, 2, 3, 4, 5)),
            "causal recording tap prefix",
        )
        check(frame.through(Effects.Point.MEDIA).nodes.size == 12, "VHS tap excludes CRT")
        check(
            Effects.point(Effects.CFA_ERROR) == Effects.point(Effects.DEMOSAIC_ERROR),
            "reconstruction point",
        )
        println("PASS byte/bit/CFA/exposure fixtures, Bayer row parity, RAW bounds and causal taps")
    }
}
