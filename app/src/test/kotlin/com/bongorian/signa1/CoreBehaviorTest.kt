package com.bongorian.signa1

import org.junit.Test

/* Independent fixtures exercise both distribution variants with identical expectations. */
class CoreBehaviorTest {
    @Test
    fun adaptivePreviewLoad() {
        val load = AdaptiveLoad()
        load.sample(0, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertEquals(24, load.previewFps.toLong())
        var rendered = 0
        for (n in 1..300) {
            val stamp = n * 33_333_333L
            if (load.due(stamp)) {
                load.presented(stamp)
                rendered++
            }
        }
        org.junit.Assert.assertTrue(rendered >= 238 && rendered <= 242)
        load.sample(1000, 2, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertEquals(12, load.previewFps.toLong())
        load.sample(2000, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertEquals(12, load.previewFps.toLong())
        load.sample(17000, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertEquals(15, load.previewFps.toLong())
        load.sample(18000, 0, 46f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertEquals(6, load.previewFps.toLong())
        org.junit.Assert.assertFalse(load.cooling)
        load.sample(19000, 4, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertTrue(load.cooling)
        load.sample(20000, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        load.sample(49999, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertTrue(load.cooling)
        load.sample(50000, 0, 30f, java.lang.Float.NaN, 2000000, 2, false)
        org.junit.Assert.assertFalse(load.cooling)
        val predictive = AdaptiveLoad()
        predictive.sample(0, -1, java.lang.Float.NaN, .9f, 2000000, 2, false)
        org.junit.Assert.assertEquals(12, predictive.previewFps.toLong())
        val busy = AdaptiveLoad()
        busy.sample(0, 0, java.lang.Float.NaN, java.lang.Float.NaN, 12000000, 13, false)
        org.junit.Assert.assertEquals(6, busy.previewFps.toLong())
        val slow = AdaptiveLoad()
        slow.rendered(70.0)
        slow.sample(0, 0, java.lang.Float.NaN, java.lang.Float.NaN, 1000000, 1, false)
        org.junit.Assert.assertEquals(8, slow.previewFps.toLong())
    }

    @Test
    fun expertBypassesOnlyAdaptivePolicy() {
        val load = AdaptiveLoad()
        load.sample(0, 6, 60f, 2f, 100000000, 13, true)
        org.junit.Assert.assertTrue(load.cooling)
        load.setExpert(true, 120)
        load.rendered(500.0)
        load.sample(1, 6, 60f, 2f, 100000000, 13, true)
        org.junit.Assert.assertFalse(load.cooling)
        org.junit.Assert.assertEquals(120, load.cameraFps().toLong())
        load.presented(1000000000)
        org.junit.Assert.assertTrue(load.due(1000000001))
        load.setExpert(false, 120)
        load.sample(2, 6, 60f, 2f, 100000000, 13, true)
        org.junit.Assert.assertTrue(load.cooling)
        org.junit.Assert.assertEquals(6, load.previewFps.toLong())
    }

    @Test
    fun immutableStateAndRawRows() {
        EffectStateCheck.main(emptyArray<String>())
    }

    @Test
    fun pipelineFixtures() {
        PipelineCheck.main(emptyArray<String>())
    }

    @Test
    fun displayedFrameCapture() {
        FrameHistoryCheck.main(emptyArray<String>())
    }

    @Test
    fun liveFaultBehavior() {
        FaultModelCheck.main(emptyArray<String>())
    }

    @Test
    fun advancedPerformanceContracts() {
        AdvancedPerformanceCheck.main(emptyArray<String>())
    }

    @Test
    fun randomChainsRespectFormatAndKeepOriginal() {
        val original = EffectState.defaults().single(Effects.VHS)
        val saved = original.encode()
        val random = java.util.Random(725)
        val masks = java.util.HashSet<Int>()
        for (i in 0..99) {
            val generated = EffectRandomizer.chain(original, Effects.choices(false, true), random)
            org.junit.Assert.assertTrue(generated.chained)
            org.junit.Assert.assertTrue(generated.ids().size >= 2 && generated.ids().size <= 5)
            var previous = 0
            for (id in generated.ids()) {
                org.junit.Assert.assertTrue(Effects.raw(id) && id > previous)
                previous = id
                for (control in Effects.CONTROLS[id]) {
                    val value = generated.parameters().get(id, control.key)
                    org.junit.Assert.assertTrue(
                        java.lang.Float.isFinite(value) && value >= 0 && value <= 1
                    )
                }
            }
            org.junit.Assert.assertEquals(
                generated.encode(),
                EffectState.decode(generated.encode()).encode(),
            )
            masks.add(generated.mask)
        }
        org.junit.Assert.assertTrue(masks.size > 10)
        org.junit.Assert.assertEquals(saved, original.encode())
        org.junit.Assert.assertEquals(
            EffectRandomizer.chain(original, Effects.ORDER, java.util.Random(42)).encode(),
            EffectRandomizer.chain(original, Effects.ORDER, java.util.Random(42)).encode(),
        )
        org.junit.Assert.assertEquals(
            0,
            EffectRandomizer.chain(original, intArrayOf(0), random).mask.toLong(),
        )
    }
}
