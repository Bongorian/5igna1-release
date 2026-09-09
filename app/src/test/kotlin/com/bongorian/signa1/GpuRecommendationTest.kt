package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class GpuRecommendationTest {
    private val ram = DeviceProfile.Budget(false,2073600,2073600)
    private val fast = GpuRecommendation.Measurement(.4,2.0)
    @Test fun measuresHardwareInsteadOfRankingVendorNames() {
        val mali = GpuRecommendation.choose(ram,"ARM","Mali-G715",fast,1080,1440,120f)
        val adreno = GpuRecommendation.choose(ram,"Qualcomm","Adreno",fast,1080,1440,120f)
        assertEquals(mali.photoPixels,adreno.photoPixels)
        assertEquals(1555200L,mali.photoPixels)
        assertEquals(60,mali.previewFps)
        assertEquals(60,GpuRecommendation.fps(mali,fast,1555200,59.94f,60))
        val slow = GpuRecommendation.choose(ram,"ARM","Mali-G715",GpuRecommendation.Measurement(4.0,70.0),1080,1440,120f)
        assertTrue(slow.photoPixels < mali.photoPixels)
        assertTrue(slow.previewFps < mali.previewFps)
    }
    @Test fun respectsSoftwareUnknownDisplaysAndBadMeasurements() {
        val software = GpuRecommendation.choose(ram,"Google","ANGLE SwiftShader",fast,1080,1440,120f)
        assertEquals(GpuRecommendation.Kind.SOFTWARE,software.kind)
        assertTrue(software.photoPixels<=307200);assertTrue(software.previewFps<=15)
        val bad = GpuRecommendation.choose(ram,"?","?",GpuRecommendation.Measurement(Double.NaN,2.0),0,0,Float.NaN)
        assertFalse(bad.measured);assertTrue(bad.photoPixels<=921600);assertTrue(bad.previewFps<=24)
        val display = GpuRecommendation.choose(ram,"ARM","Mali",fast,640,480,24f)
        assertEquals(307200L,display.photoPixels);assertTrue(display.previewFps<=24)
        assertEquals(3,GpuRecommendation.fps(display,fast,307200,60f,3))
    }
    @Test fun measuredCadenceKeepsThermalControlAndExpertOverride() {
        val load = AdaptiveLoad()
        load.recommend(60,2_000_000_000.0)
        load.sample(0,0,30f,Float.NaN,1000000,3,false)
        assertEquals(60,load.previewFps);assertEquals(60,load.cameraFps())
        load.sample(1,2,30f,Float.NaN,1000000,3,false)
        assertEquals(12,load.previewFps)
        load.setExpert(true,120);assertEquals(120,load.previewFps)
        load.setExpert(false,120);assertEquals(60,load.previewFps)
        load.sample(2,4,30f,Float.NaN,1000000,3,false);assertTrue(load.cooling)
    }
    @Test fun tapRecordingFollowsByDefaultAndHonorsExplicitPauseOnce() {
        val p=TapPlayback()
        assertTrue(p.recordingStarted())
        p.manual(false);p.recordingStopped()
        assertTrue(p.recordingStarted())
        p.manual(false)
        assertFalse(p.recordingStarted())
        p.recordingStopped()
        assertTrue(p.recordingStarted())
    }
}
