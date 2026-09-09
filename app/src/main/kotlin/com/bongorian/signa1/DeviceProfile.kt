package com.bongorian.signa1

import android.app.ActivityManager
import android.content.Context

/** Starting limits only: camera capabilities and measured render/thermal feedback refine them. */
internal class DeviceProfile(context: Context) {
    val constrained: Boolean
    private val budget: Budget

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

    fun photoPixels(): Long = budget.photoPixels

    fun videoPixels(): Long = budget.videoPixels

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
