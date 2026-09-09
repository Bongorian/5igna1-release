package com.bongorian.signa1

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** RAW16 representation adapter for the SAME immutable fault nodes as RGB/video. */
internal object RawGlitch {
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
        var current = input
        var first: ByteArray? = null
        var second: ByteArray? = null
        for (node in frame.nodes) if (Effects.raw(node.id)) {
            val output = if (current === first) {
                second ?: ByteArray(input.size).also { second = it }
            } else {
                first ?: ByteArray(input.size).also { first = it }
            }
            applyInto(current, output, w, h, white, black, node)
            current = output
        }
        // Even an empty chain returns owned storage rather than exposing its input.
        return if (current === input) input.clone() else current
    }

    /** Bounded same-CFA-color sampling; these are artistic sensor-domain approximations. */
    private fun physical(input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode) {
        fun coordinate(p: Int, delta: Float, limit: Int): Int {
            val shifted = p + 2 * kotlin.math.round(delta / 2f).toInt()
            return shifted.coerceIn(p and 1, limit - 1 - ((limit - 1 - p) and 1))
        }
        val range = (white - black).coerceAtLeast(1).toFloat()
        val blurX = n.mechanism["blurX"] ?: 0f
        val blurY = n.mechanism["blurY"] ?: 0f
        val grain = (n.mechanism["noiseGrain"] ?: 1f).coerceAtLeast(1f)
        val grainSeed = (n.mechanism["grainSeed"] ?: 0f).toRawBits().toLong() xor n.identity.seed
        val amplitude = n.mechanism["noiseAmplitude"] ?: 0f
        val smearLength = n.mechanism["smearLength"] ?: 0f
        val smearThreshold = n.mechanism["smearThreshold"] ?: 0f
        val smearAmount = n.mechanism["smearAmount"] ?: 0f
        // Coordinates depend on the axis, not on each pixel. Precompute once per pass.
        val blurColumns = if (n.id == Effects.MOTION_BLUR) Array(9) { i ->
            IntArray(w) { x -> coordinate(x, blurX * w * (i / 8f - .5f), w) }
        } else emptyArray()
        val blurRows = if (n.id == Effects.MOTION_BLUR) Array(9) { i ->
            IntArray(h) { y -> coordinate(y, blurY * h * (i / 8f - .5f), h) * w }
        } else emptyArray()
        val smearRows = if (n.id == Effects.SMEAR) Array(8) { i ->
            IntArray(h) { y -> coordinate(y, (i / 7f - .5f) * smearLength * h, h) * w }
        } else emptyArray()
        for (y in 0 until h) for (x in 0 until w) {
            val index = y * w + x
            val original = read(input, index).toFloat()
            val value = when (n.id) {
                Effects.MOTION_BLUR -> {
                    var sum = 0f
                    for (i in 0..8) sum += read(input, blurRows[i][y] + blurColumns[i][x])
                    sum / 9f
                }
                Effects.THERMAL_NOISE -> {
                    val px = (x / grain).toInt(); val py = (y / grain).toInt()
                    val noise = hash(grainSeed, px, py) + hash(grainSeed + 19, px, py) + hash(grainSeed + 73, px, py) - 1.5f
                    original + noise * amplitude * range
                }
                else -> {
                    var sum = 0f
                    if (smearLength > 0f) for (i in 0..7) {
                        val light = ((read(input, smearRows[i][y] + x) - black) / range).coerceIn(0f, 1f)
                        sum += light * (light - smearThreshold).coerceAtLeast(0f) / (1 - smearThreshold).coerceAtLeast(.01f)
                    }
                    original + sum * smearAmount * range / 8f
                }
            }
            write(out, index, kotlin.math.round(value).toInt().coerceIn(0, white))
        }
    }

    private fun hash(seed: Long, x: Int, y: Int): Float {
        return FaultModel.random(
            seed xor FaultModel.mix((x.toLong() shl 32) xor (y.toLong() and 0xffffffffL))
        )
    }

    fun apply(input: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode): ByteArray =
        input.clone().also { applyInto(input, it, w, h, white, black, n) }

    private fun applyInto(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode,
    ) {
        // Every stage writes every output sample; its source remains untouched until it finishes.
        when (n.id) {
            Effects.MOTION_BLUR, Effects.THERMAL_NOISE, Effects.SMEAR -> physical(input, out, w, h, white, black, n)
            Effects.PIXEL_DAMAGE -> pixelDamage(input, out, w, h, white, black, n)
            Effects.EXPOSURE -> exposure(input, out, w, h, white, black, n)
            Effects.ROW_ERROR -> rowError(input, out, w, h, white, black, n)
            Effects.BIT_ERROR -> bitError(input, out, w, h, white, n)
            Effects.ADDRESS_ERROR -> addressError(input, out, w, h, white, black, n)
            Effects.CFA_ERROR -> cfaError(input, out, w, h, white, n)
            else -> for (y in 0..<h) for (x in 0..<w) {
                val index = y * w + x
                write(out, index, max(0, min(white, read(input, index))))
            }
        }
    }

    private fun exposure(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode,
    ) {
        val depth = n.get("exposureDepth")
        val integration = n.get("integration")
        val scanPhase = n.get("scanPhase")
        val exposurePhase = n.get("exposurePhase")
        for (y in 0..<h) {
            // Keep the original Float/Double conversions and multiplication order.
            val gain = 1 - depth * (.5f + .5f * integration *
                sin((y / h.toFloat() * scanPhase + exposurePhase).toDouble()).toFloat())
            for (x in 0..<w) {
                val index = y * w + x
                val value = black + Math.round((read(input, index) - black) * gain)
                write(out, index, max(0, min(white, value)))
            }
        }
    }

    private fun bitError(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, n: FaultNode,
    ) {
        val seed = n.identity.seed xor n.event.pattern.toLong()
        val block = max(2, n.get("bitBlock").toInt())
        val blockHeight = max(1, block / 2)
        val probability = n.get("bitProbability")
        val bits = 32 - Integer.numberOfLeadingZeros(white)
        val flip = 1 shl Math.round(n.get("bitIndex") * (bits - 1))
        val affected = BooleanArray((w - 1) / block + 1)
        var previousGroup = -1
        for (y in 0..<h) {
            val group = y / blockHeight
            if (group != previousGroup) {
                for (bx in affected.indices) affected[bx] = hash(seed, bx, group) < probability
                previousGroup = group
            }
            for (x in 0..<w) {
                val index = y * w + x
                var value = read(input, index)
                if (affected[x / block]) value = value xor flip
                write(out, index, max(0, min(white, value)))
            }
        }
    }

    private fun addressError(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode,
    ) {
        val seed = n.identity.seed
        val bytes = max(2, n.get("addressRegion").toInt())
        val offset = n.get("byteOffset").toInt()
        val probability = n.get("addressProbability")
        var previousRegion = -1
        var affected = false
        for (index in 0..<w * h) {
            val address = index * 2
            if (offset > 0) {
                // Regions are byte-addressed, including odd sizes and row crossings.
                val region = address / bytes
                if (region != previousRegion) {
                    affected = hash(seed, region, 0) < probability
                    previousRegion = region
                }
            }
            var value = read(input, index)
            if (affected) {
                val src = address + offset
                value = if (src + 1 < input.size)
                    (input[src].toInt() and 255) or ((input[src + 1].toInt() and 255) shl 8)
                else black
            }
            write(out, index, max(0, min(white, value)))
        }
    }

    private fun cfaError(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, n: FaultNode,
    ) {
        val seed = n.identity.seed
        val region = max(2, n.get("cfaRegion").toInt())
        val coverage = n.get("cfaCoverage")
        val phase = n.get("cfaPhase").toInt()
        val affected = BooleanArray((w - 1) / region + 1)
        var previousGroup = -1
        for (y in 0..<h) {
            val group = y / region
            if (group != previousGroup) {
                for (bx in affected.indices) affected[bx] = hash(seed, bx, group) < coverage
                previousGroup = group
            }
            for (x in 0..<w) {
                val index = y * w + x
                var value = read(input, index)
                if (affected[x / region]) {
                    val sx = if (phase == 1) x else x xor 1
                    val sy = if (phase == 0) y else y xor 1
                    if (sx < w && sy < h) value = read(input, sy * w + sx)
                }
                write(out, index, max(0, min(white, value)))
            }
        }
    }

    private fun pixelDamage(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode,
    ) {
        val seed = n.identity.seed
        val columnDensity = n.get("columnDensity")
        val pixelDensity = n.get("pixelDensity")
        val hotFraction = n.get("hotFraction")
        val hotValue = black + Math.round((white - black) * n.get("hotValue"))
        val grainSeed = seed xor n.get("grainSeed").toLong()
        val sensorNoise = n.get("sensorNoise")
        // A column fault is identical at every row. Keep its test separate from its value.
        val damagedColumns = BooleanArray(w)
        val columnValues = IntArray(w)
        for (x in 0..<w) {
            damagedColumns[x] = hash(seed, x, 0) < columnDensity
            if (damagedColumns[x])
                columnValues[x] = if (hash(seed + 71, x, 0) < hotFraction) hotValue else black
        }
        for (y in 0..<h) for (x in 0..<w) {
            val index = y * w + x
            var value = read(input, index)
            if (damagedColumns[x]) value = columnValues[x]
            else if (hash(seed, x, y + 1) < pixelDensity)
                value = if (hash(seed + 71, x, y + 1) < hotFraction) hotValue else black
            // Preserve operation order and rounding exactly, including noise before clamping.
            value += Math.round((hash(grainSeed, x, y) - .5f) * sensorNoise * (white - black))
            write(out, index, max(0, min(white, value)))
        }
    }

    private fun rowError(
        input: ByteArray, out: ByteArray, w: Int, h: Int, white: Int, black: Int, n: FaultNode,
    ) {
        val seed = n.identity.seed
        val rowGroups = n.get("rowGroups")
        val weakRows = n.get("weakRows")
        val rowOffset = n.get("rowOffset")
        val readoutShear = n.get("readoutShear")
        val start = (n.get("linePosition") * h / 2).toInt() * 2
        val lineHeight = n.get("lineHeight")
        val lineRetention = n.get("lineRetention")
        val lineLoss = n.get("lineLoss")
        for (y in 0..<h) {
            val row = ((y and 1.inv()) / h.toFloat() * rowGroups).toInt()
            var displacement =
                if (hash(seed, row, 0) < weakRows) (hash(seed + 17, row, 0) - .5f) * rowOffset
                else 0f
            displacement += ((y and 1.inv()) / h.toFloat() - .5f) * readoutShear
            val offset = Math.round(displacement * w / 2) * 2
            val retain = y >= start && y < start + lineHeight * h
            val previous = start - 2 + (y and 1)
            for (x in 0..<w) {
                val sx = x + offset
                var value = if (sx < 0 || sx >= w) black else read(input, y * w + sx)
                if (retain) {
                    var retained =
                        if (previous < 0 || sx < 0 || sx >= w) black
                        else read(input, previous * w + sx)
                    retained = black + Math.round((retained - black) * lineRetention)
                    value = Math.round(value * (1 - lineLoss) + retained * lineLoss)
                }
                write(out, y * w + x, max(0, min(white, value)))
            }
        }
    }

}
