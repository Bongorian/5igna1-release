package com.bongorian.signa1

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Continuous fault-time evolution and modulation. Camera/encoder clocks remain untouched. */
internal class LivePerformance
constructor(
    style: Int,
    clock: Int,
    speed: Float,
    tempo: Float,
    depth: Float,
    beats: Float,
    division: Float,
    width: Float,
    chance: Float,
    val hold: Boolean,
    periodSeconds: Float = 60f / tempo * beats,
    stepSeconds: Float = 60f / tempo / division,
) {
    val style: Int
    val clock: Int
    val speed: Float
    val tempo: Float
    val depth: Float
    val beats: Float
    val division: Float
    val width: Float
    val chance: Float
    val periodSeconds: Float
    val stepSeconds: Float

    init {
        this.periodSeconds = FaultModel.clamp(periodSeconds, .25f, 32f)
        this.stepSeconds = FaultModel.clamp(stepSeconds, .015625f, 2f)
        this.style = max(0, min(4, style))
        this.clock = max(0, min(3, clock))
        this.speed = FaultModel.clamp(speed, -4f, 4f)
        this.tempo = FaultModel.clamp(tempo, 30f, 240f)
        this.depth = EffectParameters.unit(depth)
        this.beats = FaultModel.clamp(beats, 1f, 16f)
        this.division = FaultModel.clamp(division, 1f, 16f)
        this.width = FaultModel.clamp(width, .05f, .95f)
        this.chance = EffectParameters.unit(chance)
    }

    fun held(value: Boolean): LivePerformance {
        return LivePerformance(
            style,
            clock,
            speed,
            tempo,
            depth,
            beats,
            division,
            width,
            chance,
            value,
            periodSeconds,
            stepSeconds,
        )
    }

    fun with(key: String, value: Float): LivePerformance {
        when (key) {
            "period" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    value,
                    stepSeconds,
                )

            "interval" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    value,
                )

            "style" ->
                return LivePerformance(
                    Math.round(value),
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            "clock" ->
                return LivePerformance(
                    style,
                    Math.round(value),
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            "speed" ->
                return LivePerformance(
                    style,
                    clock,
                    value,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            "tempo" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    value,
                    depth,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    60f / value * beats,
                    60f / value / division,
                )

            "depth" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    value,
                    beats,
                    division,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            "beats" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    value,
                    division,
                    width,
                    chance,
                    hold,
                    60f / tempo * value,
                    stepSeconds,
                )

            "division" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    value,
                    width,
                    chance,
                    hold,
                    periodSeconds,
                    60f / tempo / value,
                )

            "width" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    value,
                    chance,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            "chance" ->
                return LivePerformance(
                    style,
                    clock,
                    speed,
                    tempo,
                    depth,
                    beats,
                    division,
                    width,
                    value,
                    hold,
                    periodSeconds,
                    stepSeconds,
                )

            else -> throw IllegalArgumentException("Performance control")
        }
    }

    fun time(position: Double): Double {
        val length = periodSeconds.toDouble()
        when (clock) {
            LOOP -> return wrap(position, length)
            PING_PONG -> {
                val phase: Double = wrap(position, length * 2)
                return if (phase <= length) phase else length * 2 - phase
            }

            STEP -> {
                val step = stepSeconds.toDouble()
                return floor(position / step) * step
            }

            else -> return position
        }
    }

    fun envelope(seconds: Double, rank: Int, count: Int, seed: Long): Float {
        val cycle = seconds / periodSeconds
        val phase: Double = wrap(cycle, 1.0)
        val envelope: Float
        when (style) {
            PULSE -> envelope = (.5 - .5 * cos(phase * Math.PI * 2)).toFloat()
            SWELL -> envelope = phase.toFloat()
            BURST ->
                if (
                    FaultModel.random(seed xor FaultModel.mix(floor(cycle).toLong())) >= chance ||
                        phase >= width
                )
                    envelope = 0f
                else
                    envelope =
                        FaultModel.clamp(
                            min(phase / .025, (width - phase) / .075).toFloat(),
                            0f,
                            1f,
                        )

            CASCADE -> {
                var distance = abs(phase - rank / max(1, count).toDouble())
                distance = min(distance, 1 - distance)
                envelope = max(0.0, 1 - distance * max(1, count)).toFloat()
            }

            else -> return 1f
        }
        return 1 - depth + depth * envelope
    }

    fun warped(): Boolean {
        return clock != FREE || speed != 1f || hold
    }

    companion object {
        const val NATURAL: Int = 0
        const val PULSE: Int = 1
        const val SWELL: Int = 2
        const val BURST: Int = 3
        const val CASCADE: Int = 4
        const val FREE: Int = 0
        const val LOOP: Int = 1
        const val PING_PONG: Int = 2
        const val STEP: Int = 3

        fun defaults(): LivePerformance {
            return LivePerformance(NATURAL, FREE, 1f, 90f, .75f, 4f, 4f, .3f, .65f, false)
        }

        fun wrap(value: Double, span: Double): Double {
            return value - floor(value / span) * span
        }
    }
}
