package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class AnalogFpvTest {
    private val id = Effects.STREAM_ERROR
    private val cfg = FaultConfig.defaults().enabled(true)
    private fun analog() = EffectParameters.defaults().with(id, "streamModel", 1f)
    private fun state(p: EffectParameters = analog()) =
        EffectState.defaults().single(id).amount(1f).edit(false, 1 shl id, p)

    @Test fun releasedSettingsRemainDigitalAndNewSettingsRoundTrip() {
        val p = analog().with(id, "fpvQuality", .23f).override(id, "fpvBandCenter", .8f)
        assertEquals(p.encode(), EffectParameters.decode(p.encode()).encode())
        assertEquals(state(p).encode(), EffectState.decode(state(p).encode()).encode())
        val old = EffectParameters.defaults().encode().replace(Regex(",(?:fpv[^=,;]*|streamModel)=[^,;]+"), "")
        val loaded = EffectParameters.decode(old)
        assertFalse(loaded.analogFpv(id))
        assertEquals(.5f, loaded.get(id, "loss"), 0f)
        assertEquals(17, Effects.NAMES.size)
        assertFalse(Effects.choices(false, false).contains(17))
    }

    @Test fun modelsKeepSeparateControlsAndReseedingKeepsTheirValues() {
        val p = analog().with(id, "loss", .27f).with(id, "fpvQuality", .8f)
        val digital = p.with(id, "streamModel", 0f)
        assertEquals(.8f, digital.get(id, "fpvQuality"), 0f)
        assertEquals(.27f, digital.get(id, "loss"), 0f)
        val model = FaultModel()
        assertFalse(model.inspect(id, digital, 1f, cfg).mechanism.containsKey("fpvNoise"))
        assertFalse(model.inspect(id, p, 1f, cfg).mechanism.containsKey("streamLoss"))
        val active = FaultCapabilities.active(id, p, false).map { it.key }
        assertTrue("fpvColorScale" in active)
        assertFalse("eventPeriod" in active || "streamLoss" in active)
        assertFalse(FaultCapabilities.active(id, digital, false).any { it.key.startsWith("fpv") })
        val reseeded = p.reseed(id, 837L)
        for (c in Effects.CONTROLS[id]) assertEquals(p.get(id, c.key), reseeded.get(id, c.key), 0f)
        assertNotEquals(model.inspect(id, p, 1f, cfg).mechanism, model.inspect(id, reseeded, 1f, cfg).mechanism)
    }

    @Test fun transmissionOrderingAndRawExclusion() {
        assertEquals(Effects.Point.STREAM, Effects.point(id))
        assertEquals(11, id)
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
        val fixed = analog().override(id, "time", 1f)
        assertEquals(first, model.apply(state(fixed).snapshot(false, 0), cfg).nodes.single().mechanism)
    }

    @Test fun cleanReceptionAndZeroAmountBypassAndParametersStayBounded() {
        val p = analog().with(id, "fpvQuality", 1f).with(id, "fpvInterference", 0f)
        val n = FaultModel().inspect(id, p, 1f, cfg)
        for (key in listOf("fpvNoise", "fpvBurst", "fpvShift", "fpvSoftness")) assertEquals(0f, n.get(key), 0f)
        assertTrue(FaultModel().apply(state().amount(0f).snapshot(false, 0), cfg).nodes.isEmpty())
        for (value in listOf(0f, 1f)) {
            var extreme = analog()
            for (c in Effects.CONTROLS[id]) extreme = extreme.with(id, c.key, value)
            extreme = extreme.with(id, "streamModel", 1f)
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
