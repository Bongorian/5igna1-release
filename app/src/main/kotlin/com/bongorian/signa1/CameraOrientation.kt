package com.bongorian.signa1

/** Rotation from sensor coordinates into the current display, before selfie mirroring. */
internal object CameraOrientation {
    fun relative(sensor: Int, display: Int, front: Boolean): Int =
        (sensor + (if (front) display else -display) + 360) % 360

    fun exif(rotation: Int, front: Boolean): Int = when (rotation) {
        90 -> if (front) 7 else 6
        180 -> if (front) 4 else 3
        270 -> if (front) 5 else 8
        else -> if (front) 2 else 1
    }
}
