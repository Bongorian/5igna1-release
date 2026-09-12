package com.bongorian.signa1

import kotlin.math.floor
import kotlin.math.roundToInt

/** Delivery settings are independent of CRT/LED controls and persist in the signal snapshot. */
internal object NetworkDisplay {
    val keys = setOf("networkInterval", "networkDuration", "networkRate", "networkResolution")
    fun interval(value: Float): Int = if (value <= 0f) 0 else (2 + 28 * value).roundToInt()
    fun duration(value: Float): Float = (1 + 14 * value).roundToInt() / 10f
    fun fps(value: Float): Int = if (value >= 1f) 0 else (1 + 29 * value).roundToInt()
    fun scale(value: Float): Float = (20 + 80 * value).roundToInt() / 100f

    // Interval measures freeze-start to freeze-start in fault time. Seed offsets the schedule.
    fun stalled(time: Double, seed: Long, interval: Int, duration: Float): Boolean {
        if (interval == 0 || duration <= 0f) return false
        return IncidentSchedule.sample(time, interval.toDouble(), duration.toDouble(),
            FaultModel.random(seed xor 0x4e45544cL).toDouble(), 1f, 0f).active
    }
}

/** Average delivery cadence on a monotonic clock; late input never triggers catch-up bursts. */
internal class NetworkDelivery {
    private var next = Double.NaN
    private var previous = Long.MIN_VALUE
    private var rate = 0f
    private var wasStalled = false

    fun update(now: Long, fps: Float, stalled: Boolean, valid: Boolean): Boolean {
        val resumed = wasStalled && !stalled
        val reset = !valid || now < previous || fps != rate
        wasStalled = stalled
        previous = now
        rate = fps
        if (!valid) { next = if (fps > 0) now + 1e9 / fps else Double.NaN; return true }
        if (stalled) return false
        if (fps <= 0) { next = Double.NaN; return true }
        val interval = 1e9 / fps
        if (reset || resumed || !next.isFinite()) { next = now + interval; return true }
        if (now.toDouble() + 1 < next) return false
        next += (floor((now - next).coerceAtLeast(0.0) / interval) + 1) * interval
        return true
    }
}
