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
        val shifted = time + FaultModel.random(seed xor 0x4e45544cL) * interval
        val phase = shifted - floor(shifted / interval) * interval
        return phase < duration
    }
}

/** Frame cadence follows source timestamps, independent of LIVE speed or pause. */
internal class NetworkDelivery {
    private var last = Long.MIN_VALUE
    private var wasStalled = false

    fun update(now: Long, fps: Float, stalled: Boolean, valid: Boolean): Boolean {
        val resumed = wasStalled && !stalled
        wasStalled = stalled
        val take = !valid || now < last || (!stalled &&
            (resumed || fps <= 0f || (now - last).toDouble() >= 1e9 / fps))
        if (take) last = now
        return take
    }
}
