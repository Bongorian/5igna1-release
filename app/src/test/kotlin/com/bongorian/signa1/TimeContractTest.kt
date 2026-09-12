package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class TimeContractTest {
    private val defaults = EffectParameters.defaults()
    private val off = FaultConfig.defaults().experimental(true)
    private val live = off.enabled(true)
    private fun at(model: FaultModel, time: Double, config: FaultConfig = live, stamp: Long = (time * 1e9).toLong() + 1) {
        model.advance(time, FaultModel.Inputs().apply { sensorNs = stamp }, config)
    }

    @Test fun selectingEachVariantByMacroOrInternalKindProducesSameValues() {
        val model = FaultModel(42)
        at(model,0.0);at(model,1.25)
        for (id in listOf(Effects.VHS,Effects.CRT)) for (kind in 0..3) {
            val macro = defaults.with(id,"transport",kind / 3f)
            val direct = defaults.override(id,"transportKind",kind.toFloat())
            val a = model.inspect(id,macro,1f,live)
            val b = model.inspect(id,direct,1f,live)
            // Default classic profiles omit transport fields; explicitly selecting classic adds neutral transport metadata.
            if (kind > 0) {
                assertEquals("profile $id/$kind",a.profile,b.profile)
                assertEquals("mechanism $id/$kind",a.mechanism,b.mechanism)
                assertEquals("generators $id/$kind",a.internal,b.internal)
            }
            assertEquals(FaultCapabilities.active(id,macro,true).map { it.key },FaultCapabilities.active(id,direct,true).map { it.key })
        }
    }

    @Test fun networkUsesPhysicalTargetsAndSeparateSeverity() {
        val p = defaults.with(Effects.CRT,"transport",2f/3)
        val model = FaultModel(42)
        val node = model.inspect(Effects.CRT,p,.55f,live)
        assertEquals(12f,node.get("networkFps"),0f)
        assertEquals(.7f,node.internal.getValue("eventDuration"),0f)
        assertEquals(.55f,node.internal.getValue("eventProbability"),0f)
        assertEquals(.78f,1-node.get("transportLoss")*.8f,.0001f)
        assertEquals(7f,model.inspect(Effects.CRT,p.override(Effects.CRT,"networkFps",7f),.55f,live).get("networkFps"),0f)
        assertEquals(0f,model.inspect(Effects.CRT,p,1f,off).get("networkStall"),0f)
        assertEquals(0f,model.inspect(Effects.CRT,p.with(Effects.CRT,"networkInterval",0f),1f,live).get("networkStall"),0f)
    }

    @Test fun cadenceTracksRequestedAverageAcrossInputRatesAndStalls() {
        for (source in listOf(24,30,60,120)) {
            val delivery = NetworkDelivery()
            var count = 0
            for (i in 0 until source*10) if (delivery.update(1+(i*1e9/source).toLong(),12f,false,i>0)) count++
            assertEquals("$source fps source",120,count)
        }
        val gate = NetworkDelivery()
        assertTrue(gate.update(0,12f,false,false))
        assertFalse(gate.update(500_000_000,12f,true,true))
        assertFalse(gate.update(100_000_000,12f,true,true)) // even a discontinuity cannot release a valid stall
        assertTrue(gate.update(600_000_000,12f,false,true))
        assertFalse(gate.update(600_000_000,12f,false,true))
        assertTrue(gate.update(60_000_000_000,12f,false,true))
        assertFalse(gate.update(60_000_000_001,12f,false,true)) // no catch-up burst
        assertTrue(gate.update(60_000_000_002,12f,true,false)) // new source needs an initial frame
    }

    @Test fun contentAgeNeverChangesDeliveryClockAndProvenanceIsImmutable() {
        val model = FaultModel(42)
        at(model,0.0);at(model,5.0,stamp=5_123_000_000)
        val frame = model.apply(EffectState.defaults().single(Effects.CRT).snapshot(true,0),live)
        val echo = frame.withContentTimestamp(2_000_000_000)
        assertEquals(frame.deliveryNs,echo.deliveryNs)
        assertEquals(frame.sourceEpoch,echo.sourceEpoch)
        assertEquals(2_000_000_000,echo.cameraNs)
        assertEquals(echo.deliveryNs,echo.afterReadout().deliveryNs)
        assertEquals(echo.deliveryNs,echo.through(Effects.Point.DISPLAY).deliveryNs)
        at(model,6.0)
        val next = model.apply(EffectState.defaults().single(Effects.BLOCK_ERROR).snapshot(true,0),live)
        val delivered = echo.deliveredAt(next)
        assertEquals(echo.parameters.encode(),delivered.parameters.encode())
        assertEquals(echo.cameraNs,delivered.cameraNs)
        assertEquals(echo.time,delivered.time,0.0)
        assertEquals(next.deliveryNs,delivered.deliveryNs)
        assertEquals(FaultClock.VERSION,delivered.clockVersion)
        model.reset();at(model,7.0)
        assertNotEquals(frame.sourceEpoch,model.apply(EffectState.defaults().snapshot(true,0),live).sourceEpoch)
    }

    @Test fun noiseUsesFaultTimeAcrossSpeedFrameRateReverseAndReset() {
        val model = FaultModel(42)
        at(model,0.0);at(model,1.0)
        val near = live.performance(live.performance.with("speed",.999999f))
        for (id in listOf(Effects.PIXEL_DAMAGE,Effects.VHS,Effects.THERMAL_NOISE)) {
            assertEquals(model.inspect(id,defaults,1f,live).get("grainSeed"), model.inspect(id,defaults,1f,near).get("grainSeed"),0f)
            val pinned = defaults.override(id,"time",-.001f)
            val a=model.inspect(id,pinned,1f,live).get("grainSeed")
            val other=FaultModel(73);at(other,0.0);at(other,3.0,stamp=9_123_456_789)
            assertEquals(a,other.inspect(id,pinned,1f,live).get("grainSeed"),0f)
        }
        assertEquals(-1,FaultClock.tick(-.001))
        assertEquals(-2,FaultClock.tick(-.017))
        val initial=FaultModel(42).inspect(Effects.VHS,defaults,1f,live).get("grainSeed")
        model.rewind()
        assertEquals(initial,model.inspect(Effects.VHS,defaults,1f,live).get("grainSeed"),0f)
        for (fps in listOf(24,30,60,120)) {
            val sampled=FaultModel(42)
            for(i in 0..fps) at(sampled,i.toDouble()/fps,stamp=1000+i.toLong())
            assertEquals(model.inspect(Effects.VHS,defaults.override(Effects.VHS,"time",1f),1f,live).get("grainSeed"),
                sampled.inspect(Effects.VHS,defaults,1f,live).get("grainSeed"),0f)
        }
    }

    @Test fun sharedEventsClampDurationReachPeakAndRepeatAtNegativeTime() {
        val middle=IncidentSchedule.sample(.5,1.0,2.0,0.0,1f,0f)
        assertEquals(1.0,middle.duration,0.0)
        assertEquals(1f,middle.envelope,0f)
        val short=IncidentSchedule.sample(.005,1.0,.01,0.0,1f,0f)
        assertEquals(1f,short.envelope,0f)
        assertFalse(IncidentSchedule.sample(.02,1.0,.01,0.0,1f,0f).active)
        assertFalse(IncidentSchedule.sample(.005,1.0,.01,0.0,0f,0f).active)
        for (n in -100..100) {
            val t=n*.013
            assertEquals(IncidentSchedule.sample(t,1.0,.2,.37,1f,0f).active,
                IncidentSchedule.sample(t+10,1.0,.2,.37,1f,0f).active)
        }
    }

    @Test fun stageAndPerformanceSeedsAreRepeatableAcrossSessions() {
        val state=EffectState.defaults().chain((1 shl Effects.BIT_ERROR) or (1 shl Effects.VHS)).amount(.6f)
        val config=live.performance(live.performance.with("style",LivePerformance.BURST.toFloat()).with("depth",1f))
        val a=FaultModel(42);val b=FaultModel(73)
        for (i in 0..100) {
            at(a,i*.1,config);at(b,i*.1,config)
            assertEquals(a.apply(state.snapshot(true,0),config).describe(),b.apply(state.snapshot(true,0),config).describe())
        }
        val p=defaults.withEventIdentity(Effects.BIT_ERROR,99)
        assertEquals(99,a.inspect(Effects.BIT_ERROR,p,1f,live).event.identity)
        assertNotEquals(a.inspect(Effects.BIT_ERROR,defaults,1f,live).event.pattern,
            a.inspect(Effects.BIT_ERROR,defaults.reseed(Effects.BIT_ERROR,77),1f,live).event.pattern)
    }

    @Test fun capabilitiesExposeConsumersAndPreserveInactiveValues() {
        fun keys(id:Int,p:EffectParameters=defaults)=FaultCapabilities.active(id,p,true).map { it.key }.toSet()
        assertFalse("phase" in keys(Effects.EXPOSURE))
        assertTrue("exposureRate" in keys(Effects.EXPOSURE))
        assertFalse("time" in keys(Effects.CFA_ERROR))
        assertEquals(setOf("transportKind"),keys(Effects.VHS,defaults.with(Effects.VHS,"transport",2f/3)))
        val network=defaults.with(Effects.CRT,"transport",2f/3).override(Effects.CRT,"networkSeed",73f)
        assertTrue(setOf("eventPeriod","eventDuration","eventProbability","eventPhase","networkFps").all { it in keys(Effects.CRT,network) })
        assertFalse("networkSeed" in keys(Effects.CRT,network))
        assertFalse("transportDamage" in keys(Effects.CRT,network))
        assertEquals(73f,EffectParameters.decode(network.encode()).overrides(Effects.CRT).getValue("networkSeed"),0f)
        assertTrue("refreshRate" in keys(Effects.CRT,defaults.with(Effects.CRT,"transport",1f)))
        assertFalse("syncOffset" in keys(Effects.CRT,defaults.with(Effects.CRT,"transport",1f)))
        for(id in Effects.ORDER.filter { it!=0 }) assertTrue(FaultCapabilities.active(id,defaults,false).none { it.group==FaultParameters.Group.INPUT })
    }

    @Test fun exposureAndLedHaveActualTimeGenerators() {
        val model=FaultModel(42);at(model,0.0);at(model,1.0)
        val p=defaults.override(Effects.EXPOSURE,"exposureClock",0f).override(Effects.EXPOSURE,"exposurePhaseOffset",0f)
        val a=model.inspect(Effects.EXPOSURE,p.override(Effects.EXPOSURE,"exposureRate",1f),1f,live)
        val b=model.inspect(Effects.EXPOSURE,p.override(Effects.EXPOSURE,"exposureRate",2f),1f,live)
        assertEquals(1f,a.get("exposurePhase"),.0001f);assertEquals(2f,b.get("exposurePhase"),.0001f)
        val led=defaults.with(Effects.CRT,"transport",1f).override(Effects.CRT,"refreshRate",2f)
        fun seed(t:Float)=model.inspect(Effects.CRT,led.override(Effects.CRT,"time",t),1f,live).get("refreshSeed")
        assertEquals(seed(0f),seed(.49f),0f);assertNotEquals(seed(0f),seed(.5f))
    }
    @Test fun unrelatedStaticMechanismsMatchTheFrozenModel() {
        val before=LegacyFaultModelReference(42)
        val after=FaultModel(42)
        before.advance(0.0,LegacyFaultModelReference.Inputs(),off);at(after,0.0,off)
        before.advance(1.25,LegacyFaultModelReference.Inputs(),off);at(after,1.25,off)
        for(id in listOf(Effects.CFA_ERROR,Effects.DEMOSAIC_ERROR,Effects.CHROMA_ERROR,Effects.COLOR_MAP,Effects.CRT)) {
            for(level in listOf(.25f,.55f,1f)) {
                val old=before.inspect(id,defaults,level,off)
                val current=after.inspect(id,defaults,level,off)
                for((key,value) in old.profile) assertEquals("profile $id/$key",value,current.profile.getValue(key),0f)
                for((key,value) in old.mechanism) assertEquals("mechanism $id/$key",value,current.mechanism.getValue(key),0f)
            }
        }
    }

    @Test fun triggerAndResetUseTheSameNetworkIncidentGate() {
        val model=FaultModel(42)
        val p=defaults.with(Effects.CRT,"transport",2f/3).with(Effects.CRT,"networkInterval",0f)
        assertEquals(0f,model.inspect(Effects.CRT,p,1f,live).get("networkStall"),0f)
        model.hit()
        assertEquals(1f,model.inspect(Effects.CRT,p,1f,live).get("networkStall"),0f)
        model.rewind()
        assertEquals(0f,model.inspect(Effects.CRT,p,1f,live).get("networkStall"),0f)
    }

}
