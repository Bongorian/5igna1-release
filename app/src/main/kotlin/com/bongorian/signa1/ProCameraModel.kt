package com.bongorian.signa1

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import java.util.Locale

/** Camera controls are separate from FAULT, output format and processing-performance modes. */
internal data class ProCameraState(
    val manualExposure: Boolean = false,
    val exposureNs: Long = 10_000_000L,
    val iso: Int = 100,
    val ev: Int = 0,
    val aeLock: Boolean = false,
    val whiteBalance: Int = 1,
    val awbLock: Boolean = false,
    val manualFocus: Boolean = false,
    val focus: Float = 0f,
    val aperture: Float? = null,
    val filterDensity: Float? = null,
    // -1: camera default, 0: off, 1: optical, 2: video electronic.
    val stabilization: Int = -1,
    val antiBanding: Int = -1,
) {
    fun resolve(c: ProCameraCapabilities, context: ProCameraContext): ProCameraState {
        if (context.highSpeed) return ProCameraState()
        val shutterRange = c.exposureRange(context)
        val manual = manualExposure && shutterRange != null && c.iso != null
        val wb = whiteBalance.takeIf { it in c.whiteBalance } ?: c.whiteBalance.firstOrNull() ?: 1
        return copy(
            manualExposure = manual,
            exposureNs = shutterRange?.let { exposureNs.coerceIn(it) } ?: 10_000_000L,
            iso = c.iso?.let { iso.coerceIn(it) } ?: 100,
            ev = if (manual) 0 else c.ev?.let { ev.coerceIn(it) } ?: 0,
            aeLock = !manual && aeLock && c.aeLock,
            whiteBalance = wb,
            awbLock = awbLock && c.awbLock && wb == 1,
            manualFocus = manualFocus && c.focusMax > 0,
            focus = if (focus.isFinite()) focus.coerceIn(0f, c.focusMax.coerceAtLeast(0f)) else 0f,
            aperture = aperture?.takeIf { manual }?.takeIf { it.isFinite() }?.let { value -> c.apertures.minByOrNull { kotlin.math.abs(it - value) } },
            filterDensity = filterDensity?.takeIf { it.isFinite() }?.let { value -> c.filterDensities.minByOrNull { kotlin.math.abs(it - value) } },
            stabilization = stabilization.takeIf { it in c.stabilizations(context) } ?: -1,
            antiBanding = antiBanding.takeIf { !manual && it in c.antiBanding } ?: -1,
        )
    }

    fun encode(): String = listOf(manualExposure, exposureNs, iso, ev, aeLock, whiteBalance,
        awbLock, manualFocus, focus, aperture ?: "", filterDensity ?: "", stabilization, antiBanding).joinToString(";")

    companion object {
        fun decode(value: String?): ProCameraState = runCatching {
            val p = requireNotNull(value).split(';')
            require(p.size == 13)
            ProCameraState(p[0].toBooleanStrict(), p[1].toLong(), p[2].toInt(), p[3].toInt(),
                p[4].toBooleanStrict(), p[5].toInt(), p[6].toBooleanStrict(), p[7].toBooleanStrict(),
                p[8].toFloat(), p[9].toFloatOrNull(), p[10].toFloatOrNull(), p[11].toInt(), p[12].toInt())
        }.getOrDefault(ProCameraState())
    }
}

internal data class ProCameraContext(val video: Boolean = false, val fps: Int = 30,
    val minimumFrameNs: Long = 0L, val highSpeed: Boolean = false) {
    val nominalFrameNs: Long get() = maxOf(1_000_000_000L / fps.coerceAtLeast(1), minimumFrameNs)
}

internal data class ProCameraCapabilities(
    val exposure: LongRange? = null,
    val maxFrameNs: Long = 0,
    val iso: IntRange? = null,
    val ev: IntRange? = null,
    val evStep: Float = 0f,
    val aeLock: Boolean = false,
    val whiteBalance: List<Int> = emptyList(),
    val awbLock: Boolean = false,
    val focusMax: Float = 0f,
    val focusCalibrated: Boolean = false,
    val apertures: List<Float> = emptyList(),
    val filterDensities: List<Float> = emptyList(),
    val opticalStabilization: Boolean = false,
    val videoStabilization: Boolean = false,
    val antiBanding: List<Int> = emptyList(),
) {
    fun exposureRange(context: ProCameraContext): LongRange? {
        if (context.highSpeed || iso == null || maxFrameNs <= 0) return null
        val range = exposure ?: return null
        val upper = minOf(range.last, maxFrameNs,
            if (context.video) 1_000_000_000L / context.fps.coerceAtLeast(1) else Long.MAX_VALUE)
        return if (range.first > 0 && range.first <= upper) range.first..upper else null
    }
    fun stabilizations(context: ProCameraContext): List<Int> = buildList {
        add(-1)
        if (opticalStabilization) add(0)
        if (opticalStabilization) add(1)
        // Electronic stabilization is not offered: preserve the native field of view.
    }
    fun frameDuration(state: ProCameraState, context: ProCameraContext): Long =
        maxOf(context.nominalFrameNs, state.exposureNs).coerceAtMost(maxFrameNs.coerceAtLeast(1))
}

internal data class ProCameraReading(
    val timestampNs: Long = 0,
    val exposureNs: Long? = null,
    val iso: Int? = null,
    val frameNs: Long? = null,
    val focus: Float? = null,
    val aperture: Float? = null,
    val whiteBalance: Int? = null,
    val aeLocked: Boolean? = null,
    val awbLocked: Boolean? = null,
    val opticalStabilization: Int? = null,
    val videoStabilization: Int? = null,
)

internal object ProCameraScale {
    fun value(range: LongRange, progress: Int): Long {
        if (progress <= 0) return range.first
        if (progress >= 1000) return range.last
        return exp(ln(range.first.toDouble()) + (ln(range.last.toDouble()) - ln(range.first.toDouble())) * progress / 1000.0)
            .roundToLong().coerceIn(range)
    }
    fun progress(range: LongRange, value: Long): Int {
        if (range.first == range.last) return 0
        return ((ln(value.coerceIn(range).toDouble()) - ln(range.first.toDouble())) /
            (ln(range.last.toDouble()) - ln(range.first.toDouble())) * 1000).roundToInt().coerceIn(0, 1000)
    }
    fun shutter(ns: Long?): String = when {
        ns == null || ns <= 0 -> "—"
        ns < 250_000_000L -> "1/" + (1e9 / ns).roundToInt()
        else -> String.format(Locale.US, "%.2f s", ns / 1e9)
    }
}
