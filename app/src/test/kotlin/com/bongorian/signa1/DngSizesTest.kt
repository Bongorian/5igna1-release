package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class DngSizesTest {
    @Test fun pixelNineCroppedAndBinnedStreamsAreNotDngSizes() {
        assertTrue(DngSizes.matches(4080,3072,4080,3072,4080,3072))
        for ((w,h) in listOf(4080 to 2288,2032 to 1536,2016 to 1136))
            assertFalse(DngSizes.matches(w,h,4080,3072,4080,3072))
    }
    @Test fun acceptsActiveOrFullPixelArrayButNotTransposedOrEmpty() {
        assertTrue(DngSizes.matches(4000,3000,4000,3000,4080,3072))
        assertTrue(DngSizes.matches(4080,3072,4000,3000,4080,3072))
        assertFalse(DngSizes.matches(3072,4080,4000,3000,4080,3072))
        assertFalse(DngSizes.matches(0,0,0,0,0,0))
    }
    @Test fun maximumResolutionUsesItsOwnDimensions() {
        assertTrue(DngSizes.matches(8160,6144,8160,6144,8160,6144))
        assertFalse(DngSizes.matches(4080,3072,8160,6144,8160,6144))
    }
}
