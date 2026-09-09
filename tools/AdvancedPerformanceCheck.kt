package com.bongorian.signa1

import java.util.*

/* Contract fixtures: full internal catalog, immutable overrides and independent performance clocks. */
object AdvancedPerformanceCheck {
    internal fun check(ok: Boolean, message: String) {
        if (!ok) throw AssertionError(message)
    }

    internal fun node(frame: EffectState.Frame, id: Int): FaultNode {
        return frame.nodes.stream().filter({ n -> n.id == id }).findFirst().orElseThrow()
    }

    internal fun sample(
        m: FaultModel,
        s: EffectState,
        t: Double,
        c: FaultConfig,
    ): EffectState.Frame {
        val `in` = FaultModel.Inputs()
        `in`.sensorNs = (t * 1e9).toLong() + 1
        m.advance(t, `in`, c)
        return m.apply(s.snapshot(true, 0), c)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val state = EffectState.defaults().chain(-1)
        val off = FaultConfig.defaults().experimental(true)
        val model = FaultModel(73)
        sample(model, state, 0.0, off)
        for (id in state.ids()) {
            val automatic = model.inspect(id, state.parameters(), state.amount, off)
            val catalog = LinkedHashSet<String>()
            for (spec in FaultParameters.all(id)) check(
                catalog.add(spec.key),
                "duplicate internal key",
            )
            check(
                catalog == automatic.inspect().keys,
                "complete internal catalog for " +
                    Effects.name(id) +
                    " missing=" +
                    automatic.inspect().keys +
                    " vs " +
                    catalog,
            )
            for (spec in FaultParameters.all(id)) {
                val value = spec.min + (spec.max - spec.min) * .63f
                val changed = state.parameters().override(id, spec.key, value)
                val actual = model.inspect(id, changed, state.amount, off)
                check(
                    Math.abs(actual.inspect().getValue(spec.key) - value) <
                        (if (spec.key == "eventSerial") 1f else .002f),
                    "direct control not applied: " + Effects.name(id) + " / " + spec.key,
                )
                val draft = state.edit(true, state.mask, changed)
                check(
                    EffectState.decode(draft.encode()).encode() == draft.encode(),
                    "override roundtrip",
                )
                check(state.parameters().overrides(id).isEmpty(), "base parameters mutated")
                check(
                    changed.automatic(id, spec.key).encode() == state.parameters().encode(),
                    "AUTO returns to original mapping",
                )
                for (bad in
                    floatArrayOf(
                        java.lang.Float.NaN,
                        java.lang.Float.POSITIVE_INFINITY,
                        spec.max + 1,
                        spec.min - 1,
                    )) try {
                    changed.override(id, spec.key, bad)
                    throw AssertionError("invalid override accepted")
                } catch (expected: IllegalArgumentException) {}
            }
        }
        val raw =
            state.edit(
                true,
                state.mask,
                state
                    .parameters()
                    .override(Effects.PIXEL_DAMAGE, "pixelDensity", 1f)
                    .override(Effects.PIXEL_DAMAGE, "columnDensity", 0f)
                    .override(Effects.PIXEL_DAMAGE, "hotFraction", 1f)
                    .override(Effects.PIXEL_DAMAGE, "hotValue", 1f),
            )
        val input = ByteArray(16 * 16 * 2)
        for (i in 0..255) RawGlitch.write(input, i, 1000)
        val out =
            RawGlitch.chain(
                input,
                16,
                16,
                4095,
                64,
                model.apply(raw.single(Effects.PIXEL_DAMAGE).snapshot(false, 2), off),
            )
        for (i in 0..255) check(RawGlitch.read(out, i) == 4095, "RAW direct hot pixel control")
        val fixed =
            state.edit(
                true,
                state.mask,
                state.parameters().withEventIdentity(Effects.VHS, Long.MIN_VALUE),
            )
        check(
            EffectState.decode(fixed.encode()).encode() == fixed.encode(),
            "event identity roundtrip",
        )
        val eventA = FaultModel(1)
        val eventB = FaultModel(2)
        sample(eventA, fixed, 0.0, off)
        sample(eventB, fixed, 0.0, off)
        for (i in 1..99) {
            val left = node(sample(eventA, fixed, i * .1, off), Effects.VHS)
            val right = node(sample(eventB, fixed, i * .1, off), Effects.VHS)
            check(
                left.event.serial == right.event.serial &&
                    left.event.envelope == right.event.envelope &&
                    left.event.pattern == right.event.pattern,
                "fixed event seed must replay across sessions",
            )
        }
        val p = LivePerformance.defaults().with("tempo", 120f).with("beats", 2f)
        check(p.with("clock", LivePerformance.LOOP.toFloat()).time(1.25) == .25, "loop wraps")
        check(
            p.with("clock", LivePerformance.LOOP.toFloat()).time(-.25) == .75,
            "reverse loop wraps",
        )
        check(
            p.with("clock", LivePerformance.PING_PONG.toFloat()).time(1.25) == .75,
            "ping-pong reverses",
        )
        check(
            p.with("clock", LivePerformance.STEP.toFloat()).time(.19) == .125,
            "quantized fault clock",
        )
        val seconds =
            p.with("period", 2.5f)
                .with("interval", .2f)
                .with("clock", LivePerformance.STEP.toFloat())
                .held(true)
        check(
            seconds.periodSeconds == 2.5f && seconds.stepSeconds == .2f,
            "seconds retained through controls",
        )
        check(Math.abs(seconds.time(.51) - .4) < .00001, "interval is measured directly in seconds")
        check(
            Math.abs(seconds.with("clock", LivePerformance.LOOP.toFloat()).time(2.8) - .3) < .00001,
            "period is measured directly in seconds",
        )
        val pulse = p.with("style", LivePerformance.PULSE.toFloat()).with("depth", 1f)
        check(
            pulse.envelope(0.0, 0, 3, 7) == 0f && Math.abs(pulse.envelope(.5, 0, 3, 7) - 1) < .0001,
            "pulse envelope",
        )
        val cascade = p.with("style", LivePerformance.CASCADE.toFloat()).with("depth", 1f)
        check(
            cascade.envelope(0.0, 0, 3, 7) == 1f && cascade.envelope(0.0, 1, 3, 7) < .0001,
            "cascade directs different stages",
        )
        val live = off.enabled(true).performance(p)
        val m = FaultModel(55)
        sample(m, state, 0.0, live)
        val start = sample(m, state, 1.0, live)
        val baseline = m.copy()
        val preview = m.copy()
        sample(preview, state, 2.0, live.performance(p.with("speed", -2f)))
        val normal = sample(m, state, 2.0, live)
        check(
            normal.describe() == sample(baseline, state, 2.0, live).describe(),
            "preview timeline leaked into committed model",
        )
        val held = live.performance(p.held(true))
        val a = sample(m, state, 3.0, held)
        val b = sample(m, state, 4.0, held)
        check(a.time == b.time && a.cameraNs != b.cameraNs, "HOLD changes fault time only")
        for (id in state.ids()) check(
            node(a, id).describe() == node(b, id).describe(),
            "HOLD did not freeze " + Effects.name(id),
        )
        val position = b.time
        val reversed = sample(m, state, 5.0, live.performance(p.with("speed", -1f)))
        check(reversed.time < position, "negative speed reverses fault clock")
        m.rewind()
        check(m.apply(state.snapshot(true, 0), live).time == 0.0, "transport reset")
        m.hit()
        check(
            node(m.apply(state.snapshot(true, 0), live), Effects.STREAM_ERROR).event.envelope == 1f,
            "HIT opens incident",
        )
        check(
            m.apply(state.amount(0f).snapshot(true, 0), live).nodes.isEmpty(),
            "zero LEVEL remains bypass even with HIT",
        )
        val snapshot = m.apply(state.snapshot(true, 0), live)
        val saved = snapshot.describe()
        sample(m, state, 6.0, live)
        check(saved == snapshot.describe(), "performance mutated captured snapshot")
        println(
            "PASS full advanced catalog, direct overrides, RAW behavior, bounds, persistence, performance clocks, HOLD/HIT and preview isolation"
        )
    }
}
