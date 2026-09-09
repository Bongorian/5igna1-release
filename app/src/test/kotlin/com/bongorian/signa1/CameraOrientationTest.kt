package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class CameraOrientationTest {
    @Test fun portraitAndLandscapeSensorsInEveryDisplayOrientation() {
        assertArrayEquals(intArrayOf(90, 0, 270, 180), IntArray(4) { CameraOrientation.relative(90, it * 90, false) })
        assertArrayEquals(intArrayOf(270, 0, 90, 180), IntArray(4) { CameraOrientation.relative(270, it * 90, true) })
        assertArrayEquals(intArrayOf(0, 270, 180, 90), IntArray(4) { CameraOrientation.relative(0, it * 90, false) })
        assertArrayEquals(intArrayOf(0, 90, 180, 270), IntArray(4) { CameraOrientation.relative(0, it * 90, true) })
    }
    @Test fun rawExifIncludesHalfTurnsAndMirroredSelfies() {
        assertArrayEquals(intArrayOf(1, 6, 3, 8), IntArray(4) { CameraOrientation.exif(it * 90, false) })
        assertArrayEquals(intArrayOf(2, 7, 4, 5), IntArray(4) { CameraOrientation.exif(it * 90, true) })
    }
    @Test fun textureCoordinatesFollowDisplayRotation() {
        val expected = arrayOf(floatArrayOf(0f,0f), floatArrayOf(0f,1f), floatArrayOf(1f,1f), floatArrayOf(1f,0f))
        for (rotation in 0..3) {
            val m = CameraOrientation.textureTransform(rotation*90)
            assertEquals(expected[rotation][0], m[12], 0f)
            assertEquals(expected[rotation][1], m[13], 0f)
            assertEquals(1f, m[0]*m[5]-m[1]*m[4], 0f)
        }
    }
}
