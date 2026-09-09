package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class PreviewSizingTest {
    @Test fun lightIsExplicitAndMutuallyExclusive() {
        val s = CaptureSettings()
        assertFalse(s.lightMode)
        s.advancedMode = true; s.expertMode = true; s.lightMode = true
        assertTrue(s.lightMode); assertFalse(s.advancedMode); assertFalse(s.expertMode)
        assertTrue(CaptureSettings(s).lightMode)
        s.advancedMode = true
        assertFalse(s.lightMode); assertTrue(s.advancedMode)
        s.lightMode = true; s.expertMode = true
        assertFalse(s.lightMode); assertTrue(s.expertMode); assertFalse(s.advancedMode)
    }
    @Test fun capsInPhysicalViewPixelsWithoutUpscalingOrChangingAspect() {
        assertEquals(PreviewSizing.Size(985,1309), PreviewSizing.choose(3072,4080,986,1309,true,false,false))
        assertEquals(PreviewSizing.Size(1309,985), PreviewSizing.choose(4080,3072,1309,986,true,false,false))
        assertEquals(PreviewSizing.Size(640,480), PreviewSizing.choose(640,480,1000,1000,true,false,false))
        val window = PreviewSizing.choose(3840,2160,600,1000,true,false,false)
        assertEquals(600,window.width); assertEquals(337,window.height)
    }
    @Test fun preservesRecordingHistoryAndOtherModes() {
        val full = PreviewSizing.Size(3072,4080)
        assertEquals(full,PreviewSizing.choose(3072,4080,986,1309,false,false,false))
        assertEquals(full,PreviewSizing.choose(3072,4080,986,1309,true,true,false))
        assertEquals(full,PreviewSizing.choose(3072,4080,986,1309,true,false,true))
        assertEquals(full,PreviewSizing.choose(3072,4080,0,0,true,false,false))
    }
}
