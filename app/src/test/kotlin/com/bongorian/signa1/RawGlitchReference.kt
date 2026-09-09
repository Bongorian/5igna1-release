package com.bongorian.signa1

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Frozen pre-optimization implementation for byte-exact differential tests. */
internal object RawGlitchReference {
    fun read(data: ByteArray, index: Int): Int {
        val p = index * 2
        return (data[p].toInt() and 255) or ((data[p + 1].toInt() and 255) shl 8)
    }

    fun write(data: ByteArray, index: Int, value: Int) {
        val p = index * 2
        data[p] = value.toByte()
        data[p + 1] = (value ushr 8).toByte()
    }

    fun chain(
        input: ByteArray,
        w: Int,
        h: Int,
        white: Int,
        black: Int,
        frame: EffectState.Frame,
    ): ByteArray {
        require(
            !(w <= 0 ||
                h <= 0 ||
                input.size.toLong() != w.toLong() * h * 2 ||
                white < 1 ||
                white > 65535 ||
                black < 0 ||
                black > white)
        ) {
            "RAW dimensions/levels"
        }
        var output = input.clone()
        for (node in frame.nodes) if (Effects.raw(node.id))
            output = apply(output, w, h, white, black, node)
        return output
    }

    private fun hash(seed: Long, x: Int, y: Int): Float {
        return FaultModel.random(
            seed xor FaultModel.mix((x.toLong() shl 32) xor (y.toLong() and 0xffffffffL))
        )
    }

    fun apply(input: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode): ByteArray {
        val out = input.clone()
        val seed = n.identity.seed
        val bits = 32 - Integer.numberOfLeadingZeros(white)
        for (y in 0..<h) for (x in 0..<w) {
            var sx = x
            var sy = y
            val index = y * w + x
            var gain = 1f
            var value = read(input, index)
            when (n.id) {
                Effects.PIXEL_DAMAGE -> {
                    if (hash(seed, x, 0) < n.get("columnDensity"))
                        value =
                            if (
                                hash(
                                    seed + 71,
                                    x,
                                    0,
                                ) < n.get("hotFraction")
                            )
                                black + Math.round((white - black) * n.get("hotValue"))
                            else black
                    else if (hash(seed, x, y + 1) < n.get("pixelDensity"))
                        value =
                            if (
                                hash(
                                    seed + 71,
                                    x,
                                    y + 1,
                                ) < n.get("hotFraction")
                            )
                                black + Math.round((white - black) * n.get("hotValue"))
                            else black
                    value +=
                        Math.round(
                            (hash(
                                seed xor n.get("grainSeed").toLong(),
                                x,
                                y,
                            ) - .5f) * n.get("sensorNoise") * (white - black)
                        )
                }

                Effects.EXPOSURE -> {
                    gain =
                        1 -
                            n.get("exposureDepth") *
                                (.5f +
                                    .5f *
                                        n.get("integration") *
                                        sin(
                                                (y / h.toFloat() * n.get("scanPhase") +
                                                        n.get("exposurePhase"))
                                                    .toDouble()
                                            )
                                            .toFloat())
                    value = black + Math.round((value - black) * gain)
                }

                Effects.ROW_ERROR -> {
                    val row = ((y and 1.inv()) / h.toFloat() * n.get("rowGroups")).toInt()
                    var displacement =
                        if (hash(seed, row, 0) < n.get("weakRows"))
                            (hash(
                                seed + 17,
                                row,
                                0,
                            ) - .5f) * n.get("rowOffset")
                        else 0f
                    displacement += ((y and 1.inv()) / h.toFloat() - .5f) * n.get("readoutShear")
                    sx += Math.round(displacement * w / 2) * 2
                    value = if (sx < 0 || sx >= w) black else read(input, y * w + sx)
                    val start = (n.get("linePosition") * h / 2).toInt() * 2
                    if (y >= start && y < start + n.get("lineHeight") * h) {
                        val previous = start - 2 + (y and 1)
                        var retained =
                            if (previous < 0 || sx < 0 || sx >= w) black
                            else
                                read(
                                    input,
                                    previous * w + sx,
                                )
                        retained = black + Math.round((retained - black) * n.get("lineRetention"))
                        value =
                            Math.round(
                                value * (1 - n.get("lineLoss")) + retained * n.get("lineLoss")
                            )
                    }
                }

                Effects.BIT_ERROR -> {
                    val block = max(2, n.get("bitBlock").toInt())
                    if (
                        hash(
                            seed xor n.event.pattern.toLong(),
                            x / block,
                            y / max(1, block / 2),
                        ) < n.get("bitProbability")
                    )
                        value = value xor (1 shl Math.round(n.get("bitIndex") * (bits - 1)))
                }

                Effects.ADDRESS_ERROR -> {
                    val bytes = max(2, n.get("addressRegion").toInt())
                    val offset = n.get("byteOffset").toInt()
                    val address = index * 2
                    if (
                        offset > 0 &&
                            hash(
                                seed,
                                address / bytes,
                                0,
                            ) < n.get("addressProbability")
                    ) {
                        val src = address + offset
                        value =
                            if (src + 1 < input.size)
                                (input[src].toInt() and 255) or
                                    ((input[src + 1].toInt() and 255) shl 8)
                            else black
                    }
                }

                Effects.CFA_ERROR -> {
                    val region = max(2, n.get("cfaRegion").toInt())
                    if (hash(seed, x / region, y / region) < n.get("cfaCoverage")) {
                        val phase = n.get("cfaPhase").toInt()
                        sx = if (phase == 1) x else x xor 1
                        sy = if (phase == 0) y else y xor 1
                        if (sx < w && sy < h) value = read(input, sy * w + sx)
                    }
                }

                else -> {}
            }
            write(out, index, max(0, min(white, value)))
        }
        return out
    }
}
