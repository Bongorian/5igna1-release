package com.bongorian.signa1

import org.json.JSONObject

internal object GpuDefaultsChecks {
    fun run(test: DeviceChecks): String {
        val a = test.activity!!
        val prefs = a.getSharedPreferences("gpu-defaults-check",0)
        try {
            prefs.edit().clear().commit()
            check(CaptureSettings.load(prefs,true).lightMode)
            check(!CaptureSettings.load(prefs,false).lightMode)
            CaptureSettings.load(prefs,true).save(prefs)
            check(CaptureSettings.load(prefs,false).lightMode)
            prefs.edit().clear().putBoolean("lightMode",false).commit()
            check(!CaptureSettings.load(prefs,true).lightMode)
            prefs.edit().clear().putString("photoSize","1920x1080").putBoolean("advancedMode",true).commit()
            val saved = CaptureSettings.load(prefs,true)
            check(!saved.lightMode && saved.advancedMode && saved.photoSize == "1920x1080")
            val profile = a.engine.deviceProfile
            val first = profile.gpu!!
            if (first.measurement != null) {
                check(first.measurement.valid())
                test.glSync { profile.calibrate(a,PhotoRenderer.shaderSource(a),a.engine.thermalMonitor) }
                check(profile.gpu!!.origin == "cached" && profile.gpu!!.measurement == first.measurement)
            } else check(first.origin in listOf("deferred-heat","budget-fallback","unstable-fallback","unavailable","memory-fallback"))
            val choice = profile.recommendation!!
            check(choice.photoPixels > 0 && choice.videoPixels > 0) // Startup view can resize after camera configuration.
            return "PASS fresh constrained LIGHT, saved mode/size preservation, GPU measurement/fallback policy; " + JSONObject()
                .put("renderer",first.renderer).put("origin",first.origin)
                .put("overheadMs",first.measurement?.overheadMs ?: JSONObject.NULL)
                .put("msPerMp",first.measurement?.millisecondsPerMegapixel ?: JSONObject.NULL)
                .put("photoPixels",choice.photoPixels).put("videoPixels",choice.videoPixels)
                .put("recommendedFps",choice.previewFps).put("actualPreviewFps",a.engine.previewFps).toString()
        } finally { prefs.edit().clear().commit() }
    }
}
