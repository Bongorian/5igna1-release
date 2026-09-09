package com.bongorian.signa1

import android.app.ActivityManager
import android.content.Context

/** Starting limits only: camera capabilities and measured render/thermal feedback refine them. */
internal class DeviceProfile(context: Context) {
    val constrained: Boolean
    private val budget: Budget
    var gpu: GpuCalibration.Result? = null
        private set
    var recommendation: GpuRecommendation.Choice? = null
        private set

    init {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val memory = ActivityManager.MemoryInfo()
        manager?.getMemoryInfo(memory)
        budget =
            classify(
                manager?.isLowRamDevice == true,
                memory.totalMem,
                Runtime.getRuntime().availableProcessors(),
            )
        constrained = budget.constrained
    }

    fun calibrate(context: Context, shader: String, monitor: ThermalMonitor) {
        gpu = GpuCalibration.run(context,shader,monitor)
    }

    fun recommend(viewW: Int, viewH: Int, refreshHz: Float): GpuRecommendation.Choice {
        val result = GpuRecommendation.choose(budget,gpu?.vendor.orEmpty(),gpu?.renderer.orEmpty(),
            gpu?.measurement,viewW,viewH,refreshHz)
        recommendation = result
        return result
    }

    fun initialFps(pixels: Long, refreshHz: Float, cameraFps: Int): Int {
        val choice = recommendation ?: recommend(0,0,refreshHz)
        return GpuRecommendation.fps(choice,gpu?.measurement,pixels,refreshHz,cameraFps)
    }

    fun photoPixels(): Long = recommendation?.photoPixels ?: budget.photoPixels

    fun videoPixels(): Long = recommendation?.videoPixels ?: budget.videoPixels

    data class Budget(val constrained: Boolean, val photoPixels: Long, val videoPixels: Long)

    companion object {
        /** CPU count is a conservative hint, not a CPU/GPU benchmark. Unknown RAM starts low. */
        fun classify(lowRam: Boolean, memoryBytes: Long, cores: Int): Budget {
            val gib = 1024L * 1024 * 1024
            return when {
                lowRam || memoryBytes <= 0 || memoryBytes < 5 * gib || cores <= 4 ->
                    Budget(true, 921600, 307200)
                memoryBytes < 8 * gib || cores < 8 -> Budget(false, 1440000, 921600)
                else -> Budget(false, 2073600, 2073600)
            }
        }
    }
}
