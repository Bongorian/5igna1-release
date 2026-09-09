package com.bongorian.signa1

import java.util.Random

/** Random opportunities and history windows; independent of display FPS and the FAULT clock. */
internal class EchoSchedule(private val random: Random = Random()) {
    var retentionNs = duration(2.5, 8.0)
        private set
    var ready = false
        private set
    var active = false
        private set
    var bursts = 0
        private set
    private var next = 0L
    private var until = 0L
    private var delay = 0L

    private fun duration(low: Double, high: Double) =
        ((low + random.nextDouble() * (high - low)) * 1e9).toLong()

    fun reset() {
        retentionNs = duration(2.5, 8.0)
        next = 0L
        until = 0L
        delay = 0L
        ready = false
        active = false
    }

    fun frame(now: Long, oldest: Long, manual: Boolean, chance: Float): Long? {
        if (active && now >= until) {
            active = false
            retentionNs = duration(2.5, 8.0)
            next = now + duration(.8, 3.0)
        }
        val available = if (oldest > 0) minOf(now - oldest, retentionNs) else 0L
        ready = available >= 800_000_000L
        if (next == 0L) next = now + duration(.8, 3.0)
        if (!active && ready && (manual || now >= next)) {
            next = now + duration(.8, 3.0)
            if (manual || random.nextDouble() < chance.coerceIn(0f, 1f)) {
                delay = duration(.65, available * 1e-9)
                until = now + duration(.4, 2.6)
                active = true
                bursts++
            }
        }
        return if (active) now - delay else null
    }
}
