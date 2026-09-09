package com.bongorian.signa1

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Transparent, bounded starting policy; measured workload/heat still control runtime cadence. */
internal object GpuRecommendation {
    enum class Kind { HARDWARE, UNKNOWN, SOFTWARE }
    data class Measurement(val overheadMs: Double, val millisecondsPerMegapixel: Double) {
        fun valid() = overheadMs.isFinite() && overheadMs in 0.0..500.0 &&
            millisecondsPerMegapixel.isFinite() && millisecondsPerMegapixel in .01..5000.0
        fun time(pixels: Long) = overheadMs + millisecondsPerMegapixel * pixels / 1_000_000.0
    }
    data class Choice(val photoPixels: Long, val videoPixels: Long, val previewFps: Int,
                      val pixelWorkBudget: Double?, val kind: Kind, val measured: Boolean)

    fun kind(vendor: String, renderer: String): Kind {
        val name = "$vendor $renderer".lowercase(java.util.Locale.ROOT)
        if (listOf("swiftshader", "llvmpipe", "softpipe", "software rasterizer", "swrast").any { it in name })
            return Kind.SOFTWARE
        return if (listOf("adreno", "mali", "powervr", "xclipse", "apple", "nvidia", "intel", "vivante").any { it in name })
            Kind.HARDWARE else Kind.UNKNOWN
    }

    fun choose(ram: DeviceProfile.Budget, vendor: String, renderer: String, measurement: Measurement?,
               viewW: Int, viewH: Int, refreshHz: Float): Choice {
        val kind = kind(vendor, renderer)
        val measured = measurement?.takeIf { it.valid() }
        val screen = if (viewW > 0 && viewH > 0) viewW.toLong() * viewH else ram.photoPixels
        val displayFps = if (refreshHz.isFinite() && refreshHz >= 6f) refreshHz.roundToInt() else 30
        val familyPixels = if (kind == Kind.SOFTWARE) 307200L else if (kind == Kind.UNKNOWN && measured == null) 921600L else Long.MAX_VALUE
        val ceiling = minOf(60, displayFps, if (kind == Kind.SOFTWARE) 15 else if (measured == null) {
            if (ram.constrained) 20 else 24
        } else 60)
        fun pixels(maximum: Long): Long {
            val limit = minOf(maximum, screen, familyPixels).coerceAtLeast(1)
            if (measured == null) return limit
            // Prefer a useful 30 fps before increasing detail. Catalogs choose actual supported sizes.
            val budget = 650.0 / minOf(30, ceiling).coerceAtLeast(6)
            val capacity = (((budget - measured.overheadMs).coerceAtLeast(0.0) /
                measured.millisecondsPerMegapixel) * 1_000_000).toLong()
            return min(limit, capacity.coerceAtLeast(minOf(307200L, limit)))
        }
        val photo = pixels(ram.photoPixels)
        val video = pixels(ram.videoPixels)
        val cap = if (measured == null) ceiling else min(ceiling, (650 / measured.time(photo)).toInt())
        val fps = rate(cap)
        // Probe: CFA + DEMOSAIC + CRT plus input/output overhead, weighted as 17 work units.
        val work = measured?.let { 650_000_000.0 * 17 / it.millisecondsPerMegapixel }
        return Choice(photo,video,fps,work,kind,measured != null)
    }

    fun fps(choice: Choice, measurement: Measurement?, pixels: Long, refreshHz: Float, cameraFps: Int): Int {
        val display = if (refreshHz.isFinite() && refreshHz >= 6) refreshHz.roundToInt() else 30
        val measured = measurement?.takeIf { it.valid() }
        val predicted = if (measured == null) choice.previewFps else (650 / measured.time(max(1L,pixels))).toInt()
        val family = if (choice.kind == Kind.SOFTWARE) 15 else 60
        return minOf(rate(minOf(predicted, display, max(1,cameraFps), family)), max(1,cameraFps), max(1,display))
    }

    fun rate(cap: Int): Int = AdaptiveLoad.RATES.lastOrNull { it <= cap } ?: 6
    fun workUnits(id: Int): Int = when (id) {
        Effects.CFA_ERROR -> 9
        Effects.DEMOSAIC_ERROR -> 4
        Effects.MOTION_BLUR -> 9
        Effects.SMEAR -> 8
        Effects.VHS, Effects.CRT -> 3
        else -> 1
    }
}
