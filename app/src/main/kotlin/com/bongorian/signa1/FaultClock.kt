package com.bongorian.signa1

import kotlin.math.floor
import kotlin.math.min

/** Fault seconds are distinct from monotonic delivery time and the age of source pixels. */
internal object FaultClock {
    const val VERSION = 2
    fun tick(seconds: Double, hz: Double = 60.0): Long = floor(seconds * hz + 1e-9).toLong()
}

/** Pure non-overlapping opportunities shared by incident faults and Network delivery stalls. */
internal object IncidentSchedule {
    data class Window(val serial: Long, val active: Boolean, val envelope: Float, val duration: Double)
    fun sample(time: Double, period: Double, requestedDuration: Double, phase: Double,
               chance: Float, random: Float, serial: Long? = null): Window {
        if (period <= 0 || requestedDuration <= 0) return Window(serial ?: 0, false, 0f, 0.0)
        val shifted = time + phase * period
        val cycle = floor((shifted + 1e-9) / period).toLong()
        val age = (shifted - cycle * period).coerceAtLeast(0.0)
        val duration = min(requestedDuration, period)
        val active = random < chance.coerceIn(0f, 1f) && age < duration - 1e-9
        val attack = min(.025, duration * .25)
        val release = min(.09, duration * .25)
        val envelope = if (active) min(age / attack, (duration - age) / release).coerceIn(0.0, 1.0).toFloat() else 0f
        return Window(serial ?: cycle, active, envelope, duration)
    }
    fun serial(time: Double, period: Double, phase: Double): Long =
        if (period <= 0) 0 else floor((time + phase * period + 1e-9) / period).toLong()
}
