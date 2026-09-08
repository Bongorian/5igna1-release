package com.bongorian.signa1

import android.app.ActivityManager
import android.content.Context

/**
 * Conservative starting budget, refined by supported camera modes and runtime render/heat feedback.
 */
internal class DeviceProfile(context: Context) {
    val constrained: Boolean

    init {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val memory = ActivityManager.MemoryInfo()
        if (manager != null) manager.getMemoryInfo(memory)
        constrained =
            manager != null &&
                (manager.isLowRamDevice() ||
                    memory.totalMem > 0 && memory.totalMem < 5L * 1024 * 1024 * 1024)
    }

    fun photoPixels(): Long {
        return (if (constrained) 1000000 else 2073600).toLong()
    }

    fun videoPixels(): Long {
        return (if (constrained) 921600 else 2073600).toLong()
    }
}
