package com.bongorian.signa1

/** Rotation from sensor coordinates into the current display, before selfie mirroring. */
internal object CameraOrientation {
    fun relative(sensor: Int, display: Int, front: Boolean): Int =
        (sensor + (if (front) display else -display) + 360) % 360

    /** Inverse display rotation in GL's upward-positive texture coordinates. */
    fun textureTransform(display: Int): FloatArray {
        val (c, s) = when ((display % 360 + 360) % 360) {
            90 -> 0f to -1f
            180 -> -1f to 0f
            270 -> 0f to 1f
            else -> 1f to 0f
        }
        return floatArrayOf(c,s,0f,0f, -s,c,0f,0f, 0f,0f,1f,0f,
            .5f-.5f*c+.5f*s, .5f-.5f*s-.5f*c, 0f,1f)
    }

    fun exif(rotation: Int, front: Boolean): Int = when (rotation) {
        90 -> if (front) 7 else 6
        180 -> if (front) 4 else 3
        270 -> if (front) 5 else 8
        else -> if (front) 2 else 1
    }
}
