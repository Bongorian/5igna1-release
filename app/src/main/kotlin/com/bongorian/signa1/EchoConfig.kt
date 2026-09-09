package com.bongorian.signa1

internal data class EchoConfig(val enabled: Boolean = false, val probability: Float = .35f) {
    val chance: Float get() = if (probability.isFinite()) probability.coerceIn(0f, 1f) else .35f
}
