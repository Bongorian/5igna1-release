package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class NetworkDisplayTest {
    @Test fun intervalAndDurationAreIndependentAndSeeded() {
        fun count(interval: Int, duration: Float) = (0 until 30000).count {
            NetworkDisplay.stalled(it / 1000.0, 42, interval, duration)
        }
        assertEquals(4200, count(5, .7f))
        assertEquals(8400, count(5, 1.4f))
        assertEquals(2100, count(10, .7f))
        assertEquals(0, count(0, 1.4f))
        assertTrue((0..1000).any { NetworkDisplay.stalled(it / 100.0, 42, 5, .7f) !=
            NetworkDisplay.stalled(it / 100.0, 73, 5, .7f) })
        for (n in -1000..1000) assertEquals(NetworkDisplay.stalled(n / 100.0, 42, 5, .7f),
            NetworkDisplay.stalled(n / 100.0 + 50, 42, 5, .7f))
    }

    @Test fun frameDeliveryCapsUpdatesAndResumesImmediately() {
        val gate = NetworkDelivery()
        assertTrue(gate.update(1, 10f, false, false))
        assertFalse(gate.update(50_000_001, 10f, false, true))
        assertTrue(gate.update(100_000_001, 10f, false, true))
        assertFalse(gate.update(150_000_001, 10f, true, true))
        assertTrue(gate.update(160_000_001, 10f, false, true))
        assertFalse(gate.update(200_000_001, 10f, false, true))
        assertTrue(gate.update(1, 10f, false, true)) // new source / seek backwards
        assertTrue(gate.update(2, 0f, false, true)) // uncapped
        assertTrue(gate.update(3, 10f, true, false)) // first frame must exist during stall
        assertFalse(gate.update(4, 10f, true, true))
    }

    @Test fun savedSettingsAndZeroAmount() {
        val p = EffectParameters.defaults().with(Effects.CRT, "transport", 2f / 3)
        val restored = EffectParameters.decode(p.encode())
        assertEquals(5, NetworkDisplay.interval(restored.get(Effects.CRT, "networkInterval")))
        assertEquals(.7f, NetworkDisplay.duration(restored.get(Effects.CRT, "networkDuration")), 0f)
        assertEquals(12, NetworkDisplay.fps(restored.get(Effects.CRT, "networkRate")))
        assertEquals(.6f, NetworkDisplay.scale(restored.get(Effects.CRT, "networkResolution")), 0f)
        val model = FaultModel(42)
        val live = FaultConfig.defaults().enabled(true)
        val zero = model.inspect(Effects.CRT, p, 0f, live)
        for (key in listOf("networkFps", "networkStall", "transportLoss")) assertEquals(0f, zero.get(key), 0f)
        val full = model.inspect(Effects.CRT, p, 1f, live.enabled(false))
        assertEquals(12f, full.get("networkFps"), 0f)
        assertEquals(.5f, full.get("transportLoss"), .0001f)
        assertEquals(0f, full.get("networkStall"), 0f)
    }
}
