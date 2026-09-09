package com.bongorian.signa1

import android.content.SharedPreferences

internal object FaultPreferences {
    fun load(p: SharedPreferences): FaultConfig {
        return FaultConfig(
            false,
            p.getBoolean("fault.v3.motion", true),
            p.getBoolean("fault.v3.audio", false),
            p.getBoolean("fault.v3.timing", true),
            p.getBoolean("fault.v3.thermal", true),
            p.getBoolean("fault.v3.cpu", true),
            p.getFloat("fault.v3.sensitivity", .5f),
            p.getInt("fault.v3.mains", 50),
            LivePerformance(
                p.getInt("live.style", 0),
                p.getInt("live.clock", 0),
                p.getFloat("live.speed", 1f),
                p.getFloat("live.tempo", 90f),
                p.getFloat("live.depth", .75f),
                p.getFloat("live.beats", 4f),
                p.getFloat("live.division", 4f),
                p.getFloat("live.width", .3f),
                p.getFloat("live.chance", .65f),
                false,
                p.getFloat(
                    "live.periodSeconds",
                    60f / p.getFloat("live.tempo", 90f) * p.getFloat("live.beats", 4f),
                ),
                p.getFloat(
                    "live.stepSeconds",
                    60f / p.getFloat("live.tempo", 90f) / p.getFloat("live.division", 4f),
                ),
            ),
            echo = EchoConfig(p.getBoolean("live.echo", false), p.getFloat("live.echo.probability", .35f)),
        )
    }

    fun save(p: SharedPreferences, c: FaultConfig) {
        val a = c.performance
        p.edit()
            .putBoolean("live.echo", c.echo.enabled)
            .putFloat("live.echo.probability", c.echo.chance)
            .putFloat("live.periodSeconds", a.periodSeconds)
            .putFloat("live.stepSeconds", a.stepSeconds)
            .putInt("live.style", a.style)
            .putInt("live.clock", a.clock)
            .putFloat("live.speed", a.speed)
            .putFloat("live.tempo", a.tempo)
            .putFloat("live.depth", a.depth)
            .putFloat("live.beats", a.beats)
            .putFloat("live.division", a.division)
            .putFloat("live.width", a.width)
            .putFloat("live.chance", a.chance)
            .putBoolean("fault.v3.motion", c.motion)
            .putBoolean("fault.v3.audio", c.audio)
            .putBoolean("fault.v3.timing", c.timing)
            .putBoolean("fault.v3.thermal", c.thermal)
            .putBoolean("fault.v3.cpu", c.cpu)
            .putFloat("fault.v3.sensitivity", c.sensitivity)
            .putInt("fault.v3.mains", c.mains)
            .apply()
    }
}
