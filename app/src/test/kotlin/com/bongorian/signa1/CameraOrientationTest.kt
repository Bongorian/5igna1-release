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
}
