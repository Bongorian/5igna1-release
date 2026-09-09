package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class TransportBehaviorTest {
    @Test fun readsLegacyControlsAndPreservesNewSelections() {
        val defaults = EffectParameters.defaults()
        val legacy = defaults.encode().replace(Regex(",(transport|reduce|cable|upconvert)=[^,;]+"), "")
        assertEquals(defaults.encode(), EffectParameters.decode(legacy).encode())
        val chosen = defaults.with(Effects.VHS, "transport", 1f).with(Effects.VHS, "cable", 1f)
            .with(Effects.CRT, "transport", 2f / 3)
        assertEquals(chosen.encode(), EffectParameters.decode(chosen.encode()).encode())
    }

    @Test fun networkStallsUseTimeAndRespectDisabledLive() {
        val model = FaultModel(42)
        val input = FaultModel.Inputs()
        var parameters = EffectParameters.defaults().with(Effects.CRT, "transport", 2f / 3).with(Effects.CRT, "sync", 1f)
        val live = FaultConfig.defaults().enabled(true)
        val states = HashSet<Float>()
        for (n in 0..100) {
            input.sensorNs = 1 + n * 700_000_000L
            model.advance(n * .7, input, live)
            states.add(model.inspect(Effects.CRT, parameters, 1f, live).get("networkStall"))
        }
        assertEquals(setOf(0f, 1f), states)
        assertEquals(0f, model.inspect(Effects.CRT, parameters, 1f, live.enabled(false)).get("networkStall"), 0f)
        parameters = parameters.with(Effects.CRT, "sync", 0f)
        assertEquals(0f, model.inspect(Effects.CRT, parameters, 1f, live).get("networkStall"), 0f)
    }
}
