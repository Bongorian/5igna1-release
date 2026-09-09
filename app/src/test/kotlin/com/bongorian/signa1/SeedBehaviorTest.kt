package com.bongorian.signa1

import java.util.Random
import org.junit.Assert.*
import org.junit.Test

class SeedBehaviorTest {
    @Test fun reseedPreservesControlsLocksAndInactiveIdentities() {
        val p = EffectParameters.defaults().override(Effects.PIXEL_DAMAGE, "identitySeed", 17f)
            .withEventIdentity(Effects.ROW_ERROR, Long.MIN_VALUE)
        val base = EffectState.defaults().edit(true, (1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.ROW_ERROR) or (1 shl Effects.COLOR_MAP), p)
        val next = EffectRandomizer.reseed(base, intArrayOf(Effects.PIXEL_DAMAGE, Effects.COLOR_MAP), Random(42))
        assertNotEquals(p.identity(Effects.PIXEL_DAMAGE), next.parameters().identity(Effects.PIXEL_DAMAGE))
        assertEquals(p.identity(Effects.ROW_ERROR), next.parameters().identity(Effects.ROW_ERROR))
        assertEquals(p.identity(Effects.COLOR_MAP), next.parameters().identity(Effects.COLOR_MAP))
        assertEquals(p.overrides(Effects.PIXEL_DAMAGE), next.parameters().overrides(Effects.PIXEL_DAMAGE))
        assertEquals(Long.MIN_VALUE, next.parameters().eventIdentity(Effects.ROW_ERROR, 0))
        assertEquals(base.mask, next.mask)
        for (c in Effects.CONTROLS[Effects.PIXEL_DAMAGE]) assertEquals(p.get(Effects.PIXEL_DAMAGE, c.key), next.parameters().get(Effects.PIXEL_DAMAGE, c.key), 0f)
    }

    @Test fun exactSignedSeedRoundTripAndRepeatableRawPattern() {
        for (seed in longArrayOf(Long.MIN_VALUE, -1, 0, Long.MAX_VALUE)) {
            val p = EffectParameters.defaults().reseed(Effects.PIXEL_DAMAGE, seed)
            assertEquals(seed, EffectParameters.decode(p.encode()).identity(Effects.PIXEL_DAMAGE))
        }
        fun pixels(seed: Long): ByteArray {
            val p = EffectParameters.defaults().reseed(Effects.PIXEL_DAMAGE, seed)
            val state = EffectState.defaults().edit(false, 1 shl Effects.PIXEL_DAMAGE, p).amount(1f)
            val frame = FaultModel(73).apply(state.snapshot(false, 2), FaultConfig.defaults())
            val input = ByteArray(64 * 64 * 2)
            for (n in 0 until 64 * 64) RawGlitch.write(input, n, 1000)
            return RawGlitch.chain(input, 64, 64, 4095, 256, frame)
        }
        assertArrayEquals(pixels(42), pixels(42))
        assertFalse(pixels(42).contentEquals(pixels(43)))
    }

    @Test fun passThroughAndDeterministicStagesDoNotOfferReseed() {
        val p = EffectParameters.defaults()
        for (id in intArrayOf(Effects.COLOR_MAP, Effects.MOTION_BLUR, Effects.SMEAR)) assertFalse(EffectRandomizer.supportsSeed(id, p))
        assertFalse(EffectRandomizer.supportsSeed(Effects.VHS, p.with(Effects.VHS, "transport", 2f / 3)))
        assertFalse(EffectRandomizer.supportsSeed(Effects.CRT, p.with(Effects.CRT, "transport", 1f / 3)))
    }
}
