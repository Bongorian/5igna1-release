package com.bongorian.signa1

/** Immutable LIVE direction and optional measured-input coupling. */
internal class FaultConfig
constructor(
    val enabled: Boolean,
    val motion: Boolean,
    val audio: Boolean,
    val timing: Boolean,
    val thermal: Boolean,
    val cpu: Boolean,
    sensitivity: Float,
    mains: Int,
    val performance: LivePerformance = LivePerformance.defaults(),
) {
    val sensitivity: Float
    val mains: Int

    init {
        this.sensitivity = EffectParameters.unit(sensitivity)
        this.mains = if (mains == 60) 60 else 50
    }

    fun enabled(value: Boolean): FaultConfig {
        return FaultConfig(
            value,
            motion,
            audio,
            timing,
            thermal,
            cpu,
            sensitivity,
            mains,
            performance,
        )
    }

    fun audio(value: Boolean): FaultConfig {
        return FaultConfig(
            enabled,
            motion,
            value,
            timing,
            thermal,
            cpu,
            sensitivity,
            mains,
            performance,
        )
    }

    fun performance(value: LivePerformance): FaultConfig {
        return FaultConfig(enabled, motion, audio, timing, thermal, cpu, sensitivity, mains, value)
    }

    companion object {
        fun defaults(): FaultConfig {
            return FaultConfig(false, true, false, true, true, true, .5f, 50)
        }
    }
}
