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
        for (case in 0..<600) {
            val (w, h) = sizes[case % sizes.size]
            val (white, black) = ranges[(case / sizes.size) % ranges.size]
            val mask = when (case % 4) {
                0 -> 1 shl Effects.PIXEL_DAMAGE
                1 -> 1 shl Effects.ROW_ERROR
                2 -> (1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.ROW_ERROR)
                else -> random.nextInt() and rawMask
            }
            val config = FaultConfig.defaults().enabled((case / 4) % 2 == 0)
            val model = FaultModel(random.nextLong())
            val inputs = FaultModel.Inputs()
            for (tick in 0..<12) {
                inputs.sensorNs = 1 + tick * 16666667L
                model.advance(tick / 60.0, inputs, config)
            }
            val frame = model.apply(
                EffectState.defaults().chain(mask).amount(levels[(case / 4) % levels.size])
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
}
