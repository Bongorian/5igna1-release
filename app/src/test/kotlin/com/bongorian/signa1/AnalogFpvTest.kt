package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class AnalogFpvTest {
    private val id = Effects.ANALOG_FPV
    private val cfg = FaultConfig.defaults().enabled(true)
    private fun state(p: EffectParameters = EffectParameters.defaults()) =
        EffectState.defaults().single(id).amount(1f).edit(false, 1 shl id, p)

    @Test fun oldSettingsLoadAndNewSettingsRoundTripWithoutChangingLegacyControls() {
        val p = EffectParameters.defaults().with(id, "fpvQuality", .23f).override(id, "fpvBandCenter", .8f)
        assertEquals(p.encode(), EffectParameters.decode(p.encode()).encode())
        val legacy = p.encode().split(';').filterNot { it.startsWith("17:") }.joinToString(";")
        val loaded = EffectParameters.decode(legacy)
        for (old in 1..16) {
            assertEquals(p.overrides(old), loaded.overrides(old))
            for (control in Effects.CONTROLS[old]) assertEquals(p.get(old, control.key), loaded.get(old, control.key), 0f)
        }
        assertEquals(.65f, loaded.get(id, "fpvQuality"), 0f)
        assertEquals(state(p).encode(), EffectState.decode(state(p).encode()).encode())
    }

    @Test fun transmissionOrderingAndRawExclusion() {
        assertEquals(Effects.Point.STREAM, Effects.point(id))
        assertTrue(Effects.rank(Effects.STREAM_ERROR) < Effects.rank(id))
        assertTrue(Effects.rank(id) < Effects.rank(Effects.VHS))
        assertFalse(Effects.raw(id))
        assertFalse(Effects.choices(false, true).contains(id))
        val frame = FaultModel().apply(state().snapshot(false, 0), cfg.experimental(false))
        assertEquals(id, frame.nodes.single().id)
        assertTrue(frame.through(Effects.Point.COLOR).nodes.isEmpty())
        assertEquals(id, frame.through(Effects.Point.STREAM).nodes.single().id)
    }

    @Test fun heldAndFixedTimeReplayButRunningTimeChangesNoise() {
        val model = FaultModel()
        val input = FaultModel.Inputs()
        model.advance(0.0, input, cfg)
        model.advance(1.0, input, cfg)
        val first = model.apply(state().snapshot(false, 0), cfg).nodes.single().mechanism
        val held = cfg.performance(cfg.performance.held(true))
        model.advance(2.0, input, held)
        assertEquals(first, model.apply(state().snapshot(false, 0), held).nodes.single().mechanism)
        model.advance(3.0, input, cfg)
        assertNotEquals(first, model.apply(state().snapshot(false, 0), cfg).nodes.single().mechanism)
        val fixed = EffectParameters.defaults().override(id, "time", 1f)
        assertEquals(first, model.apply(state(fixed).snapshot(false, 0), cfg).nodes.single().mechanism)
    }

    @Test fun cleanReceptionAndZeroAmountBypassAndParametersStayBounded() {
        val p = EffectParameters.defaults().with(id, "fpvQuality", 1f).with(id, "fpvInterference", 0f)
        val n = FaultModel().inspect(id, p, 1f, cfg)
        for (key in listOf("fpvNoise", "fpvBurst", "fpvShift", "fpvSoftness")) assertEquals(0f, n.get(key), 0f)
        assertTrue(FaultModel().apply(state().amount(0f).snapshot(false, 0), cfg).nodes.isEmpty())
        for (value in listOf(0f, 1f)) {
            var extreme = EffectParameters.defaults()
            for (c in Effects.CONTROLS[id]) extreme = extreme.with(id, c.key, value)
            for (time in listOf(-100f, 0f, 999f)) {
                val node = FaultModel().inspect(id, extreme.override(id, "time", time), 1f, cfg)
                for (spec in FaultCapabilities.active(id, extreme, false).filter { node.mechanism.containsKey(it.key) }) {
                    val v = node.get(spec.key)
                    assertTrue("${spec.key}: $v", v.isFinite() && v >= spec.min && v <= spec.max)
                }
            }
        }
    }
}
