package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TapBehaviorTest {
    @Test fun echoProbabilityZeroAndRandomHistory() {
        val silent = EchoSchedule(Random(73))
        val active = EchoSchedule(Random(73))
        val delays = HashSet<Long>()
        val windows = HashSet<Long>()
        for (step in 1..2400) {
            val now = 10_000_000_000L + step * 100_000_000L
            assertNull(silent.frame(now, now - 7_000_000_000L, false, 0f))
            val target = active.frame(now, now - 7_000_000_000L, false, 1f)
            if (target != null) {
                assertTrue(target < now)
                assertTrue(now - target <= 8_000_000_000L)
                delays.add(now - target)
            }
            windows.add(active.retentionNs)
        }
        assertTrue(active.bursts > 20)
        assertTrue(delays.size > 10)
        assertTrue(windows.size > 10)
        assertNotNull(silent.frame(300_000_000_000L, 293_000_000_000L, true, 0f))
    }

    @Test fun tapKeepsCurrentNodesOnlyAfterReadout() {
        val base = EffectState.defaults().chain(-1).snapshot(false, 0)
        val injected = base.afterReadout()
        assertTrue(injected.injection)
        assertArrayEquals(base.ids().filter { Effects.point(it).ordinal > Effects.Point.READOUT.ordinal }.toIntArray(), injected.ids())
        assertEquals(base.cameraNs, injected.cameraNs)
        assertEquals(base.time, injected.time, 0.0)
        assertEquals(base.nodes.filter { Effects.point(it.id).ordinal > Effects.Point.READOUT.ordinal }, injected.nodes)
    }

    @Test fun hiddenImageWorkWaitsAndShutdownUnblocksIt() {
        val gate = ForegroundWork()
        gate.pause()
        val entered = CountDownLatch(1)
        val done = CountDownLatch(1)
        val thread = Thread { entered.countDown(); gate.await(); done.countDown() }
        thread.start()
        assertTrue(entered.await(1, TimeUnit.SECONDS))
        assertFalse(done.await(100, TimeUnit.MILLISECONDS))
        gate.resume()
        assertTrue(done.await(1, TimeUnit.SECONDS))
        gate.close()
        assertThrows(InterruptedException::class.java) { gate.await() }
    }
}
