package com.bongorian.signa1

import android.hardware.camera2.CameraCharacteristics
import android.util.Size

/** RAW stream support alone does not guarantee that Android can serialize it as DNG. */
internal object DngSizes {
    fun accepts(cc: CameraCharacteristics, size: Size, maximum: Boolean): Boolean {
        val active = cc.get(if (maximum)
            CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION
            else CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE) ?: return false
        val pixels = cc.get(if (maximum)
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION
            else CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) ?: return false
        return matches(size.width, size.height, active.width(), active.height(), pixels.width, pixels.height)
    }

    fun matches(w: Int, h: Int, activeW: Int, activeH: Int, pixelW: Int, pixelH: Int): Boolean =
        w > 0 && h > 0 && ((w == activeW && h == activeH) || (w == pixelW && h == pixelH))
}
