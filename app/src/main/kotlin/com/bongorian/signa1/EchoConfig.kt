package com.bongorian.signa1

internal data class EchoConfig(val enabled: Boolean = false, val delaySeconds: Int = 4) {
    val delayNs: Long get() = delaySeconds.coerceIn(2, 6) * 1_000_000_000L
}
