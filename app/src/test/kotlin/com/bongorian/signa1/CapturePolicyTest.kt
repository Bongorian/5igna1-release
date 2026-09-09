package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class CapturePolicyTest {
    @Test
    fun defaultsAndCopiesPreserveIndependentSwitches() {
        val settings = CaptureSettings()
        assertFalse(settings.advancedMode)
        assertFalse(settings.resolutionAudio)
        settings.resolutionAudio = true
        val copy = CaptureSettings(settings)
        assertTrue(copy.resolutionAudio)
        assertFalse(copy.experimentalSignals)
        settings.advancedMode = true
        assertFalse(copy.advancedMode)
    }

    @Test
    fun conservativeBudgetsCoverUnknownMemoryAndCpuLimits() {
        val gib = 1024L * 1024 * 1024
        assertEquals(307200L, DeviceProfile.classify(false, 0, 8).videoPixels)
        assertTrue(DeviceProfile.classify(true, 12 * gib, 8).constrained)
        assertTrue(DeviceProfile.classify(false, 12 * gib, 4).constrained)
        assertEquals(921600L, DeviceProfile.classify(false, 6 * gib, 8).videoPixels)
        assertEquals(2073600L, DeviceProfile.classify(false, 8 * gib, 8).videoPixels)
    }

    @Test
    fun audioTiersUseActualAreaAndIgnoreOrientation() {
        val sizes = arrayOf(320 to 240, 640 to 480, 1280 to 720, 1920 to 1080, 3840 to 2160)
        val rates = listOf(8000, 16000, 32000, 48000, 48000)
        val bits = listOf(24000, 48000, 64000, 128000, 192000)
        sizes.forEachIndexed { i, (w, h) ->
            val quality = RecordingAudio.requested(w, h, true)
            assertEquals(rates[i], quality.sampleRate)
            assertEquals(bits[i], quality.bitRate)
            assertEquals(quality, RecordingAudio.requested(h, w, true))
            assertEquals(
                RecordingAudio.Quality(48000, 192000),
                RecordingAudio.requested(w, h, false),
            )
        }
        assertEquals(16000, RecordingAudio.requested(321, 240, true).sampleRate)
    }
}
