package com.bongorian.signa1

import java.io.File

/** Dated characterization, not the desired future contract. Does not modify application state. */
object TimeModelAuditCheck {
    @JvmStatic fun main(args: Array<String>) {
        val report = StringBuilder("# Time-model audit evidence\n\nBaseline: dd7f027, 2026-09-12. JVM synthetic inputs; no device/UI access.\n\n")
        fun record(name: String, detail: String, verified: Boolean) {
            check(verified) { "Audit baseline changed: $name; refresh the analysis rather than silently replacing evidence" }
            report.append("- **$name**: $detail\n")
        }
        val defaults = EffectParameters.defaults()
        val off = FaultConfig.defaults().experimental(true)
        val live = FaultConfig(true, true, true, true, true, true, .5f, 50,
            LivePerformance.defaults(), true)
        fun advance(model: FaultModel, time: Double, config: FaultConfig, heat: Float = 0f) {
            val input = FaultModel.Inputs().apply { sensorNs = 123_456_789L + (time * 1e9).toLong(); this.heat = heat }
            model.advance(time, input, config)
        }
        val model = FaultModel(42)
        advance(model, 0.0, off)
        val initial = model.inspect(Effects.EXPOSURE, defaults, 1f, off).get("exposurePhase")
        advance(model, 1.0, off)
        val later = model.inspect(Effects.EXPOSURE, defaults, 1f, off).get("exposurePhase")
        record("LIVE OFF evolves", "Exposure phase $initial → $later over one arrival second.", initial != later)

        val reaction = FaultModel(42)
        advance(reaction, 0.0, live); advance(reaction, 1.0, live)
        val heldModel = reaction.copy(); val zeroModel = reaction.copy()
        val held = live.performance(live.performance.held(true))
        val zero = live.performance(live.performance.with("speed", 0f))
        advance(heldModel, 2.0, held, 1f); advance(zeroModel, 2.0, zero, 1f)
        val a = heldModel.inspect(Effects.THERMAL_NOISE, defaults, 1f, held)
        val b = zeroModel.inspect(Effects.THERMAL_NOISE, defaults, 1f, zero)
        record("HOLD differs from speed 0", "Same fault time ${a.motion.seconds}; thermal amplitude HOLD=${a.get("noiseAmplitude")}, speed 0=${b.get("noiseAmplitude")}.",
            a.motion.seconds == b.motion.seconds && a.get("noiseAmplitude") == 0f && b.get("noiseAmplitude") > 0f)

        val pinned = defaults.override(Effects.THERMAL_NOISE, "time", 0f)
        val pinnedFirst = reaction.inspect(Effects.THERMAL_NOISE, pinned, 1f, live)
        advance(reaction, 2.0, live, 1f)
        val pinnedLast = reaction.inspect(Effects.THERMAL_NOISE, pinned, 1f, live)
        record("Manual time does not pin device response", "Pinned time=0, amplitude ${pinnedFirst.get("noiseAmplitude")} → ${pinnedLast.get("noiseAmplitude")}.",
            pinnedFirst.motion.seconds == pinnedLast.motion.seconds && pinnedFirst.get("noiseAmplitude") != pinnedLast.get("noiseAmplitude"))

        val natural = model.inspect(Effects.VHS, defaults, 1f, live).get("grainSeed")
        val warped = model.inspect(Effects.VHS, defaults, 1f,
            live.performance(live.performance.with("speed", .999999f))).get("grainSeed")
        record("Near-1 speed switches noise clock", "At identical evaluated fault time, VHS grain seed $natural → $warped when speed selects the warped path.", natural != warped)

        val exposureA = model.inspect(Effects.EXPOSURE, defaults, 1f, off)
        val exposureB = model.inspect(Effects.EXPOSURE, defaults.override(Effects.EXPOSURE,"phase",2f).override(Effects.EXPOSURE,"phaseSpeed",12f),1f,off)
        record("Exposure generic phase is diagnostic only", "phase/phaseSpeed overrides change inspection TIME fields but leave exposure mechanism unchanged.", exposureA.mechanism == exposureB.mechanism)

        val network = defaults.with(Effects.CRT,"transport",2f/3)
        val macro = model.inspect(Effects.CRT,network,1f,live)
        val direct = model.inspect(Effects.CRT,defaults.override(Effects.CRT,"transportKind",2f),1f,live)
        record("Model selector paths disagree", "Network selected by macro: fps=${macro.get("networkFps")}; selected by internal transportKind: fps=${direct.get("networkFps")}.",
            macro.get("transportKind") == direct.get("transportKind") && macro.get("networkFps") != direct.get("networkFps"))

        val defaultAmount = EffectState.defaults().amount
        val effective = model.inspect(Effects.CRT,network,defaultAmount,live).get("networkFps")
        record("Displayed Network limit differs from effective limit", "At default FAULT=$defaultAmount, nominal 12 fps becomes $effective fps; a 30 fps input sees no rate reduction.", effective > 30f)

        val delivery = NetworkDelivery()
        check(delivery.update(5_000_000_000,12f,false,false))
        val ordinaryStall = delivery.update(5_100_000_000,12f,true,true)
        val pastDuringStall = delivery.update(3_000_000_000,12f,true,true)
        record("Past content stamp bypasses Network stall", "Forward timestamp during stall updates=$ordinaryStall; older TIME ECHO-like timestamp updates=$pastDuringStall.", !ordinaryStall && pastDuringStall)

        val cadence = NetworkDelivery()
        var delivered = 0
        for (i in 0 until 300) if (cadence.update(1 + i * 33_333_334L,12f,false,i > 0)) delivered++
        record("Frame cap is quantized by input cadence", "300 regularly spaced 30 fps arrivals over 10 s yield $delivered deliveries at the 12 fps setting (10 fps).", delivered == 100)

        val longEvent = defaults.override(Effects.BIT_ERROR,"eventPeriod",1f)
            .override(Effects.BIT_ERROR,"eventDuration",2f).override(Effects.BIT_ERROR,"eventProbability",1f)
        val eventModel = FaultModel(42)
        advance(eventModel,0.0,off);advance(eventModel,.999,off)
        val before = eventModel.inspect(Effects.BIT_ERROR,longEvent,1f,off).event.envelope
        advance(eventModel,1.0,off)
        val after = eventModel.inspect(Effects.BIT_ERROR,longEvent,1f,off).event.envelope
        record("Duration can exceed period", "Period 1 s, duration 2 s, chance 1: envelope $before just before boundary → $after at boundary; requested duration does not imply a 2 s continuous hold.", before == 1f && after == 0f)

        val eventParams = defaults.override(Effects.BIT_ERROR,"eventProbability",1f)
        val sessionA = FaultModel(42); val sessionB = FaultModel(73)
        val eventA = sessionA.inspect(Effects.BIT_ERROR,eventParams,1f,off)
        val eventB = sessionB.inspect(Effects.BIT_ERROR,eventParams,1f,off)
        record("Automatic event identity is session-dependent", "Same stage seed, event identities ${eventA.event.identity} / ${eventB.event.identity}; Network schedule uses stage seed without session salt.", eventA.event.identity != eventB.event.identity)

        report.append("\n## Compiled catalog\n\nRanges/steps below are declared editable limits, not guaranteed automatic-output bounds or evidence that the active shader consumes a value. Macros are normalized 0–1. See the companion review for active-model dependencies and units.\n")
        for (id in Effects.ORDER.filter { it != 0 }) {
            report.append("\n### ${Effects.label(id)} (ID $id)\n\nMacros: ")
            report.append(Effects.CONTROLS[id].joinToString { "`${it.key}`=${it.initial}" })
            report.append(".\n\n| Group | Key | Minimum | Maximum | UI step |\n|---|---|---:|---:|---:|\n")
            for (s in FaultParameters.all(id)) report.append("| ${s.group} | `${s.key}` | ${s.min} | ${s.max} | ${s.step} |\n")
        }
        if (args.isNotEmpty()) File(args[0]).apply { parentFile?.mkdirs(); writeText(report.toString()) }
        println("PASS 11 dated time-model characterizations; ${Effects.ORDER.count { it != 0 }} stage catalogs")
    }
}
