package com.bongorian.signa1

import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class RawOptimizationTest {
    @Test
    fun matchesOriginalBytesAcrossSeedsLevelsLiveStatesAndChains() {
        val random = Random(0x51a1)
        val sizes = arrayOf(1 to 1, 1 to 17, 19 to 1, 3 to 5, 32 to 24, 63 to 47)
        val levels = floatArrayOf(0f, .01f, .25f, .55f, .8f, 1f)
        val ranges = arrayOf(1 to 0, 255 to 0, 4095 to 256, 65535 to 1024, 4095 to 4095)
        val rawMask = Effects.ORDER.filter { Effects.raw(it) }.fold(0) { mask, id -> mask or (1 shl id) }
        for (case in 0..<1500) {
            val (w, h) = sizes[(case / 8) % sizes.size]
            val (white, black) = ranges[(case / (8 * sizes.size)) % ranges.size]
            val rawIds = Effects.ORDER.filter { Effects.raw(it) }
            val route = case % 8
            val mask = when {
                route < rawIds.size -> 1 shl rawIds[route]
                route == 6 -> rawMask
                else -> random.nextInt() and rawMask
            }
            val config = FaultConfig.defaults().enabled((case / 8) % 2 == 0)
            val model = FaultModel(random.nextLong())
            val inputs = FaultModel.Inputs()
            for (tick in 0..<12) {
                inputs.sensorNs = 1 + tick * 16666667L
                model.advance(tick / 60.0, inputs, config)
            }
            val frame = model.apply(
                EffectState.defaults().chain(mask).amount(levels[(case / 8) % levels.size])
                    .snapshot(false, 2),
                config,
            )
            val input = ByteArray(w * h * 2).also(random::nextBytes)
            val original = input.clone()
            val expected = RawGlitchReference.chain(input, w, h, white, black, frame)
            val actual = RawGlitch.chain(input, w, h, white, black, frame)
            assertArrayEquals("Case $case, ${w}x$h, mask $mask", expected, actual)
            assertArrayEquals("Input changed in case $case", original, input)
            assertNotSame(input, actual)
        }
    }

    @Test
    fun preservesRegionEdgesOddByteOffsetsCfaPhasesAndRounding() {
        val random = Random(731)
        val cases = mutableListOf<Pair<Int, Map<String, Float>>>()
        for (block in intArrayOf(2, 3, 5, 17))
            for (probability in floatArrayOf(0f, .5f, 1f))
                for (bit in floatArrayOf(0f, .5f, 1f))
                    cases += Effects.BIT_ERROR to mapOf(
                        "bitBlock" to block.toFloat(), "bitProbability" to probability, "bitIndex" to bit,
                    )
        for (region in intArrayOf(2, 3, 5, 17))
            for (offset in intArrayOf(0, 1, 3, 20))
                for (probability in floatArrayOf(0f, .5f, 1f))
                    cases += Effects.ADDRESS_ERROR to mapOf(
                        "addressRegion" to region.toFloat(), "byteOffset" to offset.toFloat(),
                        "addressProbability" to probability,
                    )
        for (region in intArrayOf(2, 3, 5, 17))
            for (phase in 0..2)
                for (coverage in floatArrayOf(0f, .5f, 1f))
                    cases += Effects.CFA_ERROR to mapOf(
                        "cfaRegion" to region.toFloat(), "cfaPhase" to phase.toFloat(),
                        "cfaCoverage" to coverage,
                    )
        for (depth in floatArrayOf(0f, .5f, 1f))
            for (phase in floatArrayOf(0f, .1234567f, 3.1415927f))
                cases += Effects.EXPOSURE to mapOf("exposureDepth" to depth, "exposurePhase" to phase)
        for ((id, parameters) in cases) {
            val base = FaultModel(random.nextLong()).apply(
                EffectState.defaults().single(id).amount(1f).snapshot(false, 2),
                FaultConfig.defaults(),
            ).nodes.single()
            val node = FaultNode(id, base.identity, base.motion, base.event,
                base.profile, base.mechanism + parameters, base.internal)
            for ((w, h) in arrayOf(1 to 1, 1 to 17, 19 to 1, 3 to 5, 32 to 24, 63 to 47)) {
                val input = ByteArray(w * h * 2).also(random::nextBytes)
                val original = input.clone()
                assertArrayEquals("Effect $id, ${w}x$h, $parameters",
                    RawGlitchReference.apply(input, w, h, 4095, 256, node),
                    RawGlitch.apply(input, w, h, 4095, 256, node))
                assertArrayEquals(original, input)
            }
        }
    }

}
