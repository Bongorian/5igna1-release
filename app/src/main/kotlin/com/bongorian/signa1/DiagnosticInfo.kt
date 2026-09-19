package com.bongorian.signa1

import android.os.Build

/** Explicit allowlist: no media paths, location, identifiers, logs or clipboard reads. */
internal object DiagnosticInfo {
    fun snapshot(a: MainActivity): String = buildString {
        appendLine("5igna1 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.FLAVOR}")
        appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        val gpu = a.engine.deviceProfile.gpu
        appendLine("GPU: ${gpu?.vendor ?: "—"} / ${gpu?.renderer ?: "—"}")
        appendLine("Driver: ${gpu?.driver ?: "—"}")
        appendLine("Capture: ${if (a.tapMode) "TAP" else if (a.videoMode) "VIDEO" else "PHOTO"}")
        appendLine("Processing: ${if (a.settings.expertMode) "EXPERT" else if (a.settings.advancedMode) "ADVANCED" else if (a.settings.lightMode) "LIGHT" else "STANDARD"}")
        appendLine("Photo: format=${a.settings.photoFormat}, size=${a.settings.photoSize}")
        appendLine("Video: ${a.settings.videoKey}, RAW=${a.captureRawVideo}, audio=${a.sound}")
        appendLine("Recording: ${a.recording || a.engine.recording}")
        appendLine("Experimental: ${a.settings.experimentalSignals}; LIVE: ${a.faultConfig.enabled}")
        val state = a.effectState
        appendLine("Chain: ${state.ids().joinToString(" → ") { Effects.name(it) }.ifEmpty { "CLEAN" }}")
        for (id in state.ids()) {
            val p = state.parameters()
            val model = when (id) {
                Effects.STREAM_ERROR -> if (p.analogFpv(id)) "Analog FPV" else "Digital stream"
                Effects.VHS -> arrayOf("VHS", "DVD", "Digital thru", "Analog thru")[p.transportKind(id)]
                Effects.CRT -> arrayOf("CRT", "Digital thru", "Network display", "LED display")[p.transportKind(id)]
                else -> continue
            }
            appendLine("${Effects.name(id)}: $model")
        }
    }.trimEnd()
}
