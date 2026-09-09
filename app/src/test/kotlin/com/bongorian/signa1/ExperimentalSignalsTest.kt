package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class ExperimentalSignalsTest {
    private val config = FaultConfig(true, true, true, true, true, true, .5f, 50).experimental(true)
    private fun inputs() = FaultModel.Inputs().apply {
        motionAvailable = true; timingAvailable = true
        ax = 5f; rotation = 1f; angularSpeed = 2f
        audio = .7f; cpu = .9f; jitter = .4f; heat = .8f
        exposureNs = 20_000_000; skewNs = 12_000_000
    }
    private fun model(input: FaultModel.Inputs, c: FaultConfig = config): FaultModel {
        val model = FaultModel(123)
        repeat(240) { i -> input.sensorNs = 1L + i * 16_666_667; model.advance(i / 60.0, input, c) }
        return model
    }
    private fun node(id: Int, p: EffectParameters = EffectParameters.defaults(), input: FaultModel.Inputs = inputs(), c: FaultConfig = config) =
        model(input, c).inspect(id, p, .5f, c)

    @Test fun gateBypassesNewStagesAndSensitivityWithoutDeletingParameters() {
        val p = EffectParameters.defaults().override(Effects.PIXEL_DAMAGE, "thermalSensitivity", 4f)
        val state = EffectState.defaults().amount(.5f).chain((1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.MOTION_BLUR)).edit(true, (1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.MOTION_BLUR), p)
        val m = model(inputs())
        val off = m.apply(state.snapshot(false, 0), config.experimental(false))
        assertArrayEquals(intArrayOf(Effects.PIXEL_DAMAGE), off.ids())
        assertEquals(node(Effects.PIXEL_DAMAGE).get("sensorNoise"), off.nodes.single().get("sensorNoise"), 0f)
        assertEquals(4f, off.parameters.overrides(Effects.PIXEL_DAMAGE)["thermalSensitivity"]!!, 0f)
        assertTrue(off.describe().contains("experimental=false"))
        val on = m.apply(state.snapshot(false, 0), config)
        assertEquals(2, on.nodes.size)
        assertTrue(on.experimental)
    }

    @Test fun zeroGainsDisconnectEveryMeasuredSourceForEveryStage() {
        var p = EffectParameters.defaults()
        for (id in Effects.ORDER.filter { it != 0 }) for (key in FaultSensitivity.keys) p = p.override(id, key, 0f)
        val quiet = FaultModel.Inputs().apply { exposureNs = 20_000_000; skewNs = 12_000_000 }
        for (id in Effects.ORDER.filter { it != 0 }) {
            val loudNode = node(id, p)
            val quietNode = node(id, p, quiet)
            assertEquals("input leak at ${Effects.name(id)}", quietNode.mechanism, loudNode.mechanism)
            assertEquals(quietNode.profile, loudNode.profile)
        }
    }

    @Test fun sourceGainsAreStageLocalAndNewRoutesDrivePreviouslyUnconnectedStages() {
        val defaults = EffectParameters.defaults()
        val gain = defaults.override(Effects.PIXEL_DAMAGE, "thermalSensitivity", 3f)
        assertEquals(node(Effects.PIXEL_DAMAGE).get("sensorNoise") * 3, node(Effects.PIXEL_DAMAGE, gain).get("sensorNoise"), .000001f)
        assertEquals(node(Effects.VHS).describe(), node(Effects.VHS, gain).describe())
        val cfa = defaults.override(Effects.CFA_ERROR, "motionSensitivity", 2f)
        assertTrue(node(Effects.CFA_ERROR, cfa).get("cfaCoverage") > node(Effects.CFA_ERROR).get("cfaCoverage"))
    }

    @Test fun cpuAndTimingCanBeDisconnectedIndependently() {
        val onlyCpu = FaultModel.Inputs().apply { cpu = 1f }
        val onlyTiming = FaultModel.Inputs().apply { jitter = 1f; timingAvailable = true }
        val zero = FaultModel.Inputs()
        val noCpu = EffectParameters.defaults().override(Effects.BIT_ERROR, "cpuSensitivity", 0f)
        val noTiming = EffectParameters.defaults().override(Effects.BIT_ERROR, "timingSensitivity", 0f)
        assertEquals(node(Effects.BIT_ERROR, noCpu, zero).event.envelope, node(Effects.BIT_ERROR, noCpu, onlyCpu).event.envelope, 0f)
        assertEquals(node(Effects.BIT_ERROR, noCpu, zero).inspect()["eventProbability"], node(Effects.BIT_ERROR, noCpu, onlyCpu).inspect()["eventProbability"])
        assertEquals(node(Effects.BIT_ERROR, noTiming, zero).inspect()["eventProbability"], node(Effects.BIT_ERROR, noTiming, onlyTiming).inspect()["eventProbability"])
        assertTrue(node(Effects.BIT_ERROR, input = onlyCpu).inspect().getValue("eventProbability") > node(Effects.BIT_ERROR, input = zero).inspect().getValue("eventProbability"))
    }

    @Test fun physicalArtifactsFollowInputsAndLiveSwitch() {
        assertTrue(kotlin.math.abs(node(Effects.MOTION_BLUR).get("blurX")) > 0)
        assertTrue(node(Effects.THERMAL_NOISE).get("noiseAmplitude") > 0)
        assertTrue(node(Effects.SMEAR).get("smearAmount") > 0)
        assertEquals(0f, node(Effects.MOTION_BLUR, c = config.enabled(false)).get("blurX"), 0f)
        assertEquals(0f, node(Effects.THERMAL_NOISE, c = config.enabled(false)).get("noiseAmplitude"), 0f)
        assertEquals(0f, node(Effects.SMEAR, c = config.enabled(false)).get("smearAmount"), 0f)
        val more = EffectParameters.defaults().override(Effects.EXPOSURE, "timingSensitivity", 2f)
        assertNotEquals(node(Effects.EXPOSURE).get("scanPhase"), node(Effects.EXPOSURE, more).get("scanPhase"))
    }

    @Test fun controlsRoundTripAndLegacySettingsKeepTheirValues() {
        val p = EffectParameters.defaults().override(Effects.VHS, "audioSensitivity", 2.5f).override(Effects.SMEAR, "timingSensitivity", 0f)
        assertEquals(p.encode(), EffectParameters.decode(p.encode()).encode())
        val legacy = EffectParameters.defaults().with(Effects.VHS, "noise", .91f).encode().split(';').filter { it.isNotEmpty() && it.substringBefore(':').toInt() <= Effects.CRT }.joinToString(";")
        val restored = EffectParameters.decode(legacy)
        assertEquals(.91f, restored.get(Effects.VHS, "noise"), 0f)
        assertEquals(.6f, restored.get(Effects.SMEAR, "amount"), 0f)
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 4.01f)) {
            assertThrows(IllegalArgumentException::class.java) { p.override(Effects.VHS, "audioSensitivity", bad) }
        }
    }

    @Test fun causalOrderIncludesSensorArtifactsBeforeDigitalFaults() {
        val frame = model(inputs()).apply(EffectState.defaults().chain(-1).snapshot(false, 0), config)
        assertArrayEquals(Effects.ORDER.filter { it != 0 }.toIntArray(), frame.ids())
        assertTrue(frame.through(Effects.Point.SENSOR).ids().contains(Effects.MOTION_BLUR))
        assertFalse(frame.through(Effects.Point.SENSOR).ids().contains(Effects.SMEAR))
        assertTrue(frame.through(Effects.Point.SENSOR).experimental)
    }

    @Test fun rawArtifactsAreBoundedDeterministicAndBlurPreservesCfaPhase() {
        for ((w, h) in listOf(1 to 1, 1 to 17, 19 to 1, 3 to 5, 32 to 24)) {
            val input = ByteArray(w * h * 2)
            repeat(w * h) { i -> RawGlitch.write(input, i, intArrayOf(128, 700, 1400, 3000)[((i / w) and 1) * 2 + ((i % w) and 1)]) }
            val before = input.clone()
            val blur = EffectParameters.defaults().override(Effects.MOTION_BLUR, "blurX", .12f).override(Effects.MOTION_BLUR, "blurY", -.12f)
            assertArrayEquals(input, RawGlitch.apply(input, w, h, 4095, 64, node(Effects.MOTION_BLUR, blur)))
            for (id in intArrayOf(Effects.MOTION_BLUR, Effects.THERMAL_NOISE, Effects.SMEAR)) {
                val n = node(id)
                val first = RawGlitch.apply(input, w, h, 4095, 64, n)
                assertArrayEquals(first, RawGlitch.apply(input, w, h, 4095, 64, n))
                repeat(w * h) { assertTrue(RawGlitch.read(first, it) in 0..4095) }
                assertNotSame(input, first)
            }
            assertArrayEquals(before, input)
        }
    }

    @Test fun precomputedRawCoordinatesMatchDirectSampling() {
        val w = 37; val h = 29; val white = 4095; val black = 64
        val input = ByteArray(w * h * 2)
        repeat(w * h) { RawGlitch.write(input, it, (it * 71 + 173) % 4096) }
        fun coordinate(p: Int, delta: Float, limit: Int): Int =
            (p + 2 * kotlin.math.round(delta / 2f).toInt()).coerceIn(p and 1, limit - 1 - ((limit - 1 - p) and 1))
        for (length in floatArrayOf(0f, .017f, .12f)) {
            for (id in intArrayOf(Effects.MOTION_BLUR, Effects.SMEAR)) {
                val params = EffectParameters.defaults().override(id,
                    if (id == Effects.MOTION_BLUR) "blurX" else "smearLength", length)
                    .override(id, if (id == Effects.MOTION_BLUR) "blurY" else "smearAmount",
                        if (id == Effects.MOTION_BLUR) -length else 1.3f)
                val n = node(id, params)
                val expected = ByteArray(input.size)
                for (y in 0 until h) for (x in 0 until w) {
                    var sum = 0f
                    val original = RawGlitch.read(input, y * w + x).toFloat()
                    val value = if (id == Effects.MOTION_BLUR) {
                        for (tap in 0..8) {
                            val t = tap / 8f - .5f
                            sum += RawGlitch.read(input, coordinate(y, -length * h * t, h) * w + coordinate(x, length * w * t, w))
                        }
                        sum / 9f
                    } else {
                        val threshold = n.mechanism.getValue("smearThreshold")
                        if (length > 0) for (tap in 0..7) {
                            val sy = coordinate(y, (tap / 7f - .5f) * length * h, h)
                            val light = ((RawGlitch.read(input, sy * w + x) - black) / (white - black).toFloat()).coerceIn(0f, 1f)
                            sum += light * (light - threshold).coerceAtLeast(0f) / (1 - threshold).coerceAtLeast(.01f)
                        }
                        original + sum * 1.3f * (white - black).toFloat() / 8f
                    }
                    RawGlitch.write(expected, y * w + x, kotlin.math.round(value).toInt().coerceIn(0, white))
                }
                assertArrayEquals(expected, RawGlitch.apply(input, w, h, white, black, n))
            }
        }
    }

    @Test fun copiedAndHeldStatesFreezeInputDrivenArtifacts() {
        val m = model(inputs())
        val held = config.performance(config.performance.held(true))
        val state = EffectState.defaults().chain(-1)
        val copy = m.copy()
        val a = m.apply(state.snapshot(false, 0), held)
        val b = copy.apply(state.snapshot(false, 0), held)
        assertEquals(a.describe(), b.describe())
        val input = inputs().apply { heat = 0f; angularSpeed = 10f; sensorNs += 100 }
        m.advance(5.0, input, held)
        val c = m.apply(state.snapshot(false, 0), held)
        assertEquals(a.nodes.map { it.describe() }, c.nodes.map { it.describe() })
    }
}
