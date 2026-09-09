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
    var rawVideoEnabled: Boolean = false
    var rawVideo: Boolean = false
    var expertMode: Boolean = false
        set(value) { field = value; if (value) lightMode = false }
    var experimentalSignals: Boolean = false
    var resolutionAudio: Boolean = false
    var advancedMode: Boolean = false
        set(value) { field = value; if (value) lightMode = false }
    var lightMode: Boolean = false
        set(value) {
            field = value
            if (value) { advancedMode = false; expertMode = false }
        }
    var rawVideoSize: String = ""
    var rawVideoFps: Int = 12

    constructor()

    constructor(other: CaptureSettings) {
        experimentalSignals = other.experimentalSignals
        resolutionAudio = other.resolutionAudio
        advancedMode = other.advancedMode
        expertMode = other.expertMode
        lightMode = other.lightMode
        photoFormat = other.photoFormat
        jpegQuality = other.jpegQuality
        videoQuality = other.videoQuality
        photoSize = other.photoSize
        videoKey = other.videoKey
        codec = other.codec
        location = other.location
        rawVideoEnabled = other.rawVideoEnabled
        rawVideo = other.rawVideo && rawVideoEnabled
        rawVideoSize = other.rawVideoSize
        rawVideoFps = other.rawVideoFps
    }

    fun save(p: SharedPreferences) {
        p.edit()
            .putBoolean("experimentalSignals", experimentalSignals)
            .putBoolean("resolutionAudio", resolutionAudio)
            .putBoolean("advancedMode", advancedMode)
            .putBoolean("expertMode", expertMode)
            .putBoolean("lightMode", lightMode)
            .putBoolean("loadRecommendationsV1", true)
            .putInt("photoFormat", photoFormat)
            .putInt("jpegQuality", jpegQuality)
            .putInt("videoQuality", videoQuality)
            .putString("photoSize", photoSize)
            .putString("videoKey", videoKey)
            .putString("codec", codec)
            .putBoolean("location", location)
            .putBoolean("rawVideoEnabled", rawVideoEnabled)
            .putBoolean("rawVideo", rawVideo && rawVideoEnabled)
            .putString("rawVideoSize", rawVideoSize)
            .putInt("rawVideoFps", rawVideoFps)
            .apply()
    }

    companion object {
        fun load(p: SharedPreferences): CaptureSettings {
            val s = CaptureSettings()
            s.experimentalSignals = p.getBoolean("experimentalSignals", false)
            s.resolutionAudio = p.getBoolean("resolutionAudio", false)
            s.advancedMode = p.getBoolean("advancedMode", false)
            s.expertMode = p.getBoolean("expertMode", false)
            s.lightMode = p.getBoolean("lightMode", false)
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
            // Preserve an existing RAW-video choice as an explicit opt-in on upgrade.
            s.rawVideoEnabled = p.getBoolean("rawVideoEnabled", p.getBoolean("rawVideo", false))
            s.rawVideo = s.rawVideoEnabled && p.getBoolean("rawVideo", false)
            s.rawVideoSize = p.getString("rawVideoSize", "") ?: ""
            s.rawVideoFps = max(1, min(30, p.getInt("rawVideoFps", 12)))
            return s
        }
    }
}
