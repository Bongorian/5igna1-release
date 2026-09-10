package com.bongorian.signa1

/** Never interpolates a digital zoom ratio between advertised optical focal lengths. */
internal object OpticalFocal {
    fun values(advertised: FloatArray): List<Float> = advertised.filter { it.isFinite() && it>0 }.distinct().sorted()
    fun supported(values: List<Float>, requested: Float): Float? =
        if (!requested.isFinite()) values.firstOrNull() else values.minByOrNull { kotlin.math.abs(it-requested) }
}
