package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class ProCameraTest {
    private val camera = ProCameraCapabilities(exposure = 100_000L..30_000_000_000L,
        maxFrameNs = 10_000_000_000L, iso = 50..3200, ev = -12..12, evStep = 1f / 3,
        aeLock = true, whiteBalance = listOf(1, 2, 5), awbLock = true,
        focusMax = 8f, apertures = listOf(1.8f, 2.8f), filterDensities = listOf(0f, 2f),
        opticalStabilization = true, videoStabilization = true, antiBanding = listOf(0, 1, 2, 3))

    @Test fun videoExposureFitsCadenceWhilePhotoAllowsLongExposures() {
        val desired = ProCameraState(manualExposure = true, exposureNs = 25_000_000_000L, iso = 6400)
        val still = desired.resolve(camera, ProCameraContext())
        assertEquals(10_000_000_000L, still.exposureNs)
        assertEquals(3200, still.iso)
        for (fps in listOf(24, 30, 60, 120)) {
            val context = ProCameraContext(video = true, fps = fps)
            val video = desired.resolve(camera, context)
            assertEquals(1_000_000_000L / fps, video.exposureNs)
            assertTrue(camera.frameDuration(video, context) >= video.exposureNs)
        }
        assertEquals(25_000_000_000L, desired.exposureNs) // temporary video constraints do not erase photo preference
    }
    @Test fun automaticDependenciesAndLocksRemainCoherent() {
        val manual = ProCameraState(manualExposure = true, ev = 12, aeLock = true, whiteBalance = 5, awbLock = true)
            .resolve(camera, ProCameraContext())
        assertEquals(0, manual.ev)
        assertFalse(manual.aeLock)
        assertFalse(manual.awbLock)
        assertEquals(-1, manual.antiBanding)
        assertNull(ProCameraState(aperture = 2.8f).resolve(camera, ProCameraContext()).aperture)
        val automatic = ProCameraState(ev = -99, aeLock = true, awbLock = true).resolve(camera, ProCameraContext())
        assertEquals(-12, automatic.ev)
        assertTrue(automatic.aeLock && automatic.awbLock)
        assertFalse(ProCameraState(whiteBalance = 999, awbLock = true).resolve(camera.copy(whiteBalance = listOf(5)), ProCameraContext()).awbLock)
    }
    @Test fun missingCapabilitiesAndHighSpeedNeverReceiveManualSettings() {
        val desired = ProCameraState(manualExposure = true, exposureNs = 5_000_000_000L, iso = 800,
            manualFocus = true, focus = 2f, aeLock = true, awbLock = true, aperture = 2.8f, stabilization = 1)
        val absent = desired.resolve(ProCameraCapabilities(), ProCameraContext())
        assertFalse(absent.manualExposure || absent.manualFocus || absent.aeLock || absent.awbLock)
        assertNull(absent.aperture)
        assertEquals(-1, absent.stabilization)
        assertEquals(ProCameraState(), desired.resolve(camera, ProCameraContext(video = true, fps = 120, highSpeed = true)))
        assertNull(camera.copy(exposure = 100_000_000L..1_000_000_000L).exposureRange(ProCameraContext(video = true, fps = 60)))
    }
    @Test fun perLensValuesAreFiniteClampedAndDiscrete() {
        val value = ProCameraState(manualExposure = true, focus = Float.NaN, manualFocus = true, aperture = 2.6f,
            filterDensity = 1.7f, whiteBalance = 9, stabilization = 2, antiBanding = 100)
            .resolve(camera, ProCameraContext(video = true))
        assertEquals(0f, value.focus, 0f)
        assertEquals(2.8f, value.aperture)
        assertEquals(2f, value.filterDensity)
        assertEquals(1, value.whiteBalance)
        assertEquals(-1, value.stabilization) // no digital crop controls
        assertEquals(-1, value.antiBanding)
        assertEquals(8f, ProCameraState(focus = 20f).resolve(camera, ProCameraContext()).focus, 0f)
    }
    @Test fun storedControlsRoundTripAndMalformedDataReturnsAuto() {
        val value = ProCameraState(true, 8_000_000L, 1250, -3, false, 2, false, true, .6f, 2.8f, 2f, 1, 2)
        assertEquals(value, ProCameraState.decode(value.encode()))
        assertEquals(ProCameraState(), ProCameraState.decode(null))
        assertEquals(ProCameraState(), ProCameraState.decode("not a camera state"))
    }
    @Test fun shutterScaleCoversTheFullRangeAndIsMonotonic() {
        val range = camera.exposureRange(ProCameraContext())!!
        assertEquals(range.first, ProCameraScale.value(range, 0))
        assertEquals(range.last, ProCameraScale.value(range, 1000))
        var previous = 0L
        for (p in 0..1000) {
            val value = ProCameraScale.value(range, p)
            assertTrue(value >= previous)
            assertEquals(p, ProCameraScale.progress(range, value))
            previous = value
        }
        assertEquals(0, ProCameraScale.progress(100L..100L, 100L))
        assertEquals("1/125", ProCameraScale.shutter(8_000_000L))
        assertEquals("—", ProCameraScale.shutter(null))
    }
}
