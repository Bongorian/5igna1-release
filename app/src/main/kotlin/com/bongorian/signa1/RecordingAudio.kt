package com.bongorian.signa1

import android.media.MediaCodecList
import android.media.MediaFormat
import kotlin.math.abs

/** Requested AAC tiers, resolved against the platform's selected encoder before recording. */
internal object RecordingAudio {
    data class Quality(val sampleRate: Int, val bitRate: Int)

    fun requested(width: Int, height: Int, linked: Boolean): Quality {
        if (!linked) return Quality(48000, 192000)
        return when (width.toLong() * height) {
            in 0..76800 -> Quality(8000, 24000)
            in 76801..307200 -> Quality(16000, 48000)
            in 307201..921600 -> Quality(32000, 64000)
            in 921601..2073600 -> Quality(48000, 128000)
            else -> Quality(48000, 192000)
        }
    }

    fun supported(width: Int, height: Int, linked: Boolean): Quality {
        val requested = requested(width, height, linked)
        if (!linked) return requested
        return try {
            resolve(requested)
        } catch (_: RuntimeException) {
            Quality(48000, 192000)
        }
    }

    private fun resolve(requested: Quality): Quality {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        // Resolve a default AAC encoder, then choose a rate/bitrate it actually advertises.
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 1)
        val name = codecs.findEncoderForFormat(format) ?: return Quality(48000, 192000)
        val caps =
            codecs.codecInfos
                .first { it.name == name }
                .getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC)
                .audioCapabilities
        val candidates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000)
        val rate =
            candidates
                .filter { caps.isSampleRateSupported(it) }
                .minByOrNull { abs(it - requested.sampleRate) } ?: 48000
        return Quality(rate, caps.bitrateRange.clamp(requested.bitRate))
    }
}
