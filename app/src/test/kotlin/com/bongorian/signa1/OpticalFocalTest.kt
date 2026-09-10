package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class OpticalFocalTest {
    @Test fun onlyAdvertisedOpticalValuesAreSelectable() {
        val values=OpticalFocal.values(floatArrayOf(7f,2f,Float.NaN,-1f,2f,Float.POSITIVE_INFINITY))
        assertEquals(listOf(2f,7f),values)
        assertEquals(2f,OpticalFocal.supported(values,3f))
        assertEquals(7f,OpticalFocal.supported(values,9f))
        assertEquals(2f,OpticalFocal.supported(values,Float.NaN))
        assertNull(OpticalFocal.supported(emptyList(),3f))
    }
    @Test fun fixedFocalLensCannotZoom() {
        assertEquals(1.6f,OpticalFocal.supported(listOf(1.6f),100f))
        assertEquals(1.6f,OpticalFocal.supported(listOf(1.6f),0f))
    }
}
