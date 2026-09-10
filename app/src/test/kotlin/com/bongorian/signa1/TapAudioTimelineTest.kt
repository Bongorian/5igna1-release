package com.bongorian.signa1

import org.junit.Assert.*
import org.junit.Test

class TapAudioTimelineTest {
    @Test fun pausesLeaveGapsAndRecordingStartsAtFirstEncodedFrame() {
        val time=TapAudioTimeline()
        time.playback(true,2_000_000,10_000_000)
        time.firstFrame(10_100_000)
        time.playback(false,3_000_000,11_000_000)
        time.playback(true,3_000_000,12_000_000)
        val spans=time.snapshot(13_000_000)
        assertEquals(listOf(TapAudioTimeline.Span(0,900000,2100000),TapAudioTimeline.Span(1900000,2900000,3000000)),spans)
        assertEquals(TapAudioTimeline.Span(0,500000,2500000),spans[0].clipped(400000,1200000))
        assertNull(spans[0].clipped(1000000,1500000))
    }
    @Test fun endingAndReplayDoNotLoopOrExtendPreviousAudio() {
        val time=TapAudioTimeline()
        time.firstFrame(0)
        time.playback(true,0,0)
        time.playback(false,5_000_000,5_000_000)
        assertEquals(listOf(TapAudioTimeline.Span(0,5_000_000,0)),time.snapshot(8_000_000))
        time.playback(true,0,9_000_000)
        assertEquals(TapAudioTimeline.Span(9_000_000,10_000_000,0),time.snapshot(10_000_000).last())
    }
}
