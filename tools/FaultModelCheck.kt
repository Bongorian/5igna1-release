package com.bongorian.signa1

import java.util.*

object FaultModelCheck {
    internal fun check(ok: Boolean, message: String) {
        if (!ok) throw AssertionError(message)
    }

    internal fun node(f: EffectState.Frame, id: Int): FaultNode {
        return f.nodes.stream().filter({ n -> n.id == id }).findFirst().orElseThrow()
    }

    internal fun sample(
        model: FaultModel,
        state: EffectState,
        t: Double,
        config: FaultConfig,
        input: FaultModel.Inputs,
    ): EffectState.Frame {
        input.sensorNs = (t * 1e9).toLong() + 1
        model.advance(t, input, config)
        return model.apply(state.snapshot(true, 0), config)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val off = FaultConfig.defaults()
        val live = FaultConfig(true, true, true, true, true, true, .5f, 50)
        val state = EffectState.defaults().chain(-1)
        val m = FaultModel(7)
        val input = FaultModel.Inputs()
        val start = sample(m, state, 0.0, off, input)
        val startDescription = start.describe()
        var incident = false
        var recovered = false
        var changed = false
        var old = 0f
        for (i in 1..1800) {
            val frame = sample(m, state, i / 60.0, off, input)
            check(Arrays.equals(frame.ids(), state.ids()), "time never shuffles route")
            for (n in frame.nodes) {
                check(
                    n.identity.seed == state.parameters().identity(n.id),
                    "structural identity persistence",
                )
                for (p in n.mechanism.values) check(
                    java.lang.Float.isFinite(p),
                    "finite physical parameter",
                )
            }
            val tape = node(frame, Effects.VHS)
            changed =
                changed or
                    (Math.abs(tape.motion.drift - node(start, Effects.VHS).motion.drift) > .001)
            if (i > 1) check(Math.abs(tape.motion.drift - old) < .025, "continuous slow drift")
            old = tape.motion.drift
            val event = node(frame, Effects.STREAM_ERROR).event.envelope
            if (event > 0) incident = true else if (incident) recovered = true
            check(
                frame.describe() == m.apply(state.snapshot(true, 0), off).describe(),
                "read-only frame snapshot, no random consumption",
            )
        }
        check(
            node(start, Effects.VHS).profile.containsKey("tapeBandwidth") &&
                !node(start, Effects.VHS).mechanism.containsKey("tapeBandwidth"),
            "media profile distinct from tracking/dropout faults",
        )
        check(
            node(start, Effects.CRT).profile.containsKey("phosphorMix") &&
                node(start, Effects.CRT).mechanism.containsKey("syncOffset"),
            "display profile distinct from sync faults",
        )
        check(
            incident && recovered && changed,
            "intrinsic motion and temporary incidents exist without LIVE",
        )
        check(start.describe() == startDescription, "capture snapshot survives continued recording")
        val reseeded =
            m.apply(
                state
                    .edit(true, state.mask, state.parameters().reseed(Effects.VHS, 123))
                    .snapshot(true, 0),
                off,
            )
        val before = m.apply(state.snapshot(true, 0), off)
        check(
            node(reseeded, Effects.CRT).get("convergenceOffset") ==
                node(before, Effects.CRT).get("convergenceOffset"),
            "reseed does not affect other faults",
        )
        check(
            node(reseeded, Effects.VHS).identity.seed != node(before, Effects.VHS).identity.seed,
            "new fault individual",
        )
        check(
            node(before, Effects.PIXEL_DAMAGE).get("pixelDensity") ==
                node(start, Effects.PIXEL_DAMAGE).get("pixelDensity"),
            "sensor sites do not wander",
        )
        try {
            (before.nodes as MutableList<FaultNode>).clear()
            throw AssertionError("mutable nodes")
        } catch (expected: UnsupportedOperationException) {}

        try {
            (before.nodes[0].mechanism as MutableMap<String, Float>)["pixelDensity"] = 1f
            throw AssertionError("mutable mechanism")
        } catch (expected: UnsupportedOperationException) {}

        val a = FaultModel(55)
        val b = FaultModel(55)
        sample(a, state, 0.0, off, input)
        sample(b, state, 0.0, off, input)
        var af = sample(a, state, 10.0, off, input)
        val bf = sample(b, state, 10.0, off, input)
        check(af.describe() == bf.describe(), "explicit session replay")
        input.timingAvailable = true
        input.motionAvailable = input.timingAvailable
        input.ax = 9f
        input.rotation = 2f
        input.heat = 1f
        input.cpu = 1f
        input.audio = .8f
        input.jitter = .8f
        input.exposureNs = 4000000
        input.skewNs = 12000000
        for (i in 1..600) af = sample(a, state, 10 + i / 60.0, live, input)
        check(node(af, Effects.PIXEL_DAMAGE).get("sensorNoise") > 0, "thermal sensor activity")
        check(
            node(af, Effects.PIXEL_DAMAGE).get("pixelDensity") ==
                node(start, Effects.PIXEL_DAMAGE).get("pixelDensity"),
            "heat changes activity, not sites",
        )
        check(
            a.pressure > .1 && Math.abs(node(af, Effects.ROW_ERROR).get("readoutShear")) > .001,
            "timing/CPU and motion coupling",
        )
        val disabled = sample(a, state, 21.0, off, input)
        check(
            node(disabled, Effects.PIXEL_DAMAGE).get("sensorNoise") == 0f,
            "LIVE off disconnects measured inputs",
        )
        check(disabled.cameraNs == input.sensorNs, "camera timestamp belongs to snapshot")
        check(
            sample(a, state.amount(0f), 22.0, off, input).nodes.isEmpty(),
            "LEVEL zero has no processing nodes",
        )
        check(
            FaultModel.drift(9, 999.9999) - FaultModel.drift(9, 1000.0001) < .001,
            "no clock rollover",
        )
        println(
            "PASS independent identity/motion/events, intrinsic evolution, pure snapshots, reseed isolation and measured coupling"
        )
    }
}
