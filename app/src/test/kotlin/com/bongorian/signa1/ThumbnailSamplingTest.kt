package com.bongorian.signa1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailSamplingTest {
    @Test fun cameraSizesStayWithinPreviewAndResultBounds() {
        for ((width, height) in listOf(8000 to 6000, 6000 to 8000, 8193 to 4097, 257 to 1, 32 to 32)) {
            for (bound in listOf(256, 1024)) {
                val sample = ThumbnailSampling.sampleSize(width, height, bound)
                assertEquals(0, sample and (sample - 1))
                assertTrue((maxOf(width, height).toLong() + sample - 1) / sample <= bound)
            }
        }
        assertEquals(1, ThumbnailSampling.sampleSize(256, 128, 256))
        assertEquals(4, ThumbnailSampling.sampleSize(2049, 1024, 1024))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidBound() { ThumbnailSampling.sampleSize(8000, 6000, 0) }
}
