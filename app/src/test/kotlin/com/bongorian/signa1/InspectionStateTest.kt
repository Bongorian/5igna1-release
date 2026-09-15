package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class InspectionStateTest {
    @Test fun stageComparisonRejectsChangedInputsButAllowsOtherStagesAndRoundTrips() {
        val id = Effects.STREAM_ERROR
        val p = EffectParameters.defaults()
        assertTrue(p.sameStage(id, EffectParameters.decode(p.encode())))
        assertTrue(p.sameStage(id, p.with(Effects.CRT, "transport", 1f)))
        assertFalse(p.sameStage(id, p.with(id, "loss", .9f)))
        assertFalse(p.sameStage(id, p.with(id, "streamModel", 1f)))
        assertFalse(p.sameStage(id, p.reseed(id, p.identity(id) + 1)))
        assertFalse(p.sameStage(id, p.override(id, "streamLoss", .3f)))
        assertFalse(p.sameStage(id, p.withEventIdentity(id, 42L)))
    }

    @Test fun everySelectableModelProvidesItsActiveInspectionValues() {
        val p = EffectParameters.defaults()
        for (experimental in listOf(false, true)) for (id in Effects.ORDER.filter { it != Effects.CLEAN }) {
            val variants = when (id) {
                Effects.STREAM_ERROR -> listOf(p, p.with(id, "streamModel", 1f))
                Effects.VHS, Effects.CRT -> (0..3).map { p.with(id, "transport", it / 3f) }
                else -> listOf(p)
            }
            for (variant in variants) {
                val values = FaultModel(0).inspect(id, variant, .7f, FaultConfig.defaults().experimental(experimental)).inspect()
                for (spec in FaultCapabilities.active(id, variant, experimental)) {
                    assertTrue("$id ${spec.key}", values[spec.key]?.isFinite() == true)
                }
            }
        }
    }
}
