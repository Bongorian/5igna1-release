package com.bongorian.signa1

import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.min

internal class CaptureSettings {
    // 0: JPG; 2: sensor-processed RAW. Legacy original-RAW preference 1 migrates to 2.
    var photoFormat: Int = 0
    var jpegQuality: Int = 95
    var videoQuality: Int = 1
    var photoSize: String = "recommended"
    var videoKey: String = "recommended"
    var codec: String = "video/avc"
    var location: Boolean = false
    var rawVideo: Boolean = false
    var expertMode: Boolean = false
    var experimentalSignals: Boolean = false
    var rawVideoSize: String = ""
    var rawVideoFps: Int = 12

    constructor()

    constructor(other: CaptureSettings) {
        experimentalSignals = other.experimentalSignals
        expertMode = other.expertMode
        photoFormat = other.photoFormat
        jpegQuality = other.jpegQuality
        videoQuality = other.videoQuality
        photoSize = other.photoSize
        videoKey = other.videoKey
        codec = other.codec
        location = other.location
        rawVideo = other.rawVideo
        rawVideoSize = other.rawVideoSize
        rawVideoFps = other.rawVideoFps
    }

    fun save(p: SharedPreferences) {
        p.edit()
            .putBoolean("experimentalSignals", experimentalSignals)
            .putBoolean("expertMode", expertMode)
            .putBoolean("loadRecommendationsV1", true)
            .putInt("photoFormat", photoFormat)
            .putInt("jpegQuality", jpegQuality)
            .putInt("videoQuality", videoQuality)
            .putString("photoSize", photoSize)
            .putString("videoKey", videoKey)
            .putString("codec", codec)
            .putBoolean("location", location)
            .putBoolean("rawVideo", rawVideo)
            .putString("rawVideoSize", rawVideoSize)
            .putInt("rawVideoFps", rawVideoFps)
            .apply()
    }

    companion object {
        fun load(p: SharedPreferences): CaptureSettings {
            val s = CaptureSettings()
            s.experimentalSignals = p.getBoolean("experimentalSignals", false)
            s.expertMode = p.getBoolean("expertMode", false)
            s.photoFormat = p.getInt("photoFormat", 0)
            if (s.photoFormat == 1) s.photoFormat = 2
            s.jpegQuality = p.getInt("jpegQuality", 95)
            s.videoQuality = p.getInt("videoQuality", 1)
            s.photoSize = p.getString("photoSize", "recommended") ?: "recommended"
            s.videoKey = p.getString("videoKey", "recommended") ?: "recommended"
            if (!p.getBoolean("loadRecommendationsV1", false) && "max" == s.photoSize)
                s.photoSize = "recommended"
            if (s.videoKey!!.isEmpty()) s.videoKey = "recommended"
            s.codec = p.getString("codec", "video/avc") ?: "video/avc"
            s.location = p.getBoolean("location", false)
            s.rawVideo = p.getBoolean("rawVideo", false)
            s.rawVideoSize = p.getString("rawVideoSize", "") ?: ""
            s.rawVideoFps = max(1, min(30, p.getInt("rawVideoFps", 12)))
            return s
        }
    }
}
