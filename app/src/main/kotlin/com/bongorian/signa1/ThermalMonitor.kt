package com.bongorian.signa1

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

/** Reads thermal signals independently of artistic LIVE inputs. Invoked only in the foreground. */
internal class ThermalMonitor(val context: Context) {
    val power: PowerManager?
    var headroomAt: Long = Long.MIN_VALUE
    var status: Int = -1
    var batteryC: Float = Float.NaN
    var headroom: Float = Float.NaN

    init {
        power = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    }

    fun sample(now: Long) {
        status = -1
        batteryC = Float.NaN
        if (power != null)
            try {
                status = power.currentThermalStatus
            } catch (ignored: RuntimeException) {}
        try {
            val battery =
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (battery != null && battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                val value = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                if (value > 0 && value < 90) batteryC = value
            }
        } catch (ignored: RuntimeException) {}
        if (power != null && (headroomAt == Long.MIN_VALUE || now - headroomAt >= 10000)) {
            headroomAt = now
            try {
                headroom = power.getThermalHeadroom(10)
            } catch (ignored: RuntimeException) {
                headroom = Float.NaN
            }
        }
    }
}
