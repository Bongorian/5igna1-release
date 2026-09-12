package com.bongorian.signa1

import android.hardware.camera2.CaptureResult
import android.os.SystemClock
import android.view.View
import android.widget.TextView

internal object CameraLensChecks {
    fun run(test: DeviceChecks): String {
        val a=test.activity!!
        val e=a.engine
        val original=e.cameraSelection
        val report=StringBuilder()
        val preserved=a.effectState.encode()
        try {
            test.runOnMainSync {
                a.applySettings(CaptureSettings(a.settings).apply {
                    photoFormat=0;photoSize="640x480";videoKey="640x480@30";codec="video/avc"
                    experimentalSignals=false;rawVideo=false;advancedMode=false;expertMode=false;lightMode=false
                })
                a.setVideo(false)
            }
            test.await("camera catalog",{a.ready && !a.videoMode},15000)
            val lenses=e.lenses
            check(lenses.isNotEmpty())
            report.append("catalog=").append(lenses.joinToString { "${it.key} ${it.label(a)} ${it.detail(a)}" }).append('\n')
            android.util.Log.i("SignalLensCheck",report.toString())
            test.runOnMainSync {a.showCameras()}
            SystemClock.sleep(500)
            test.languageScreenshot("camera-lenses")
            test.runOnMainSync {a.lensDialog!!.dismiss()}
            for (lens in lenses) {
                if (e.activeLens?.front != lens.front) {
                    test.runOnMainSync { a.flipButton.performClick() }
                    test.await("facing ${lens.front}", { a.ready && e.activeLens?.front == lens.front }, 20000)
                }
                test.runOnMainSync {
                    a.showCameras()
                    a.lensDialog!!.window!!.decorView.findViewWithTag<View>("camera-lens:"+lens.key)!!.performClick()
                }
                test.await("selected ${lens.key}",{e.activeLens?.key==lens.key && a.ready && e.frameSeen && e.session!=null},20000)
                SystemClock.sleep(900)
                check(e.camera!!.id==lens.cameraId)
                check(e.characteristics===e.activeLens!!.characteristics)
                check(a.effectState.encode()==preserved)
                var focal: Float?=null
                var crop: android.graphics.Rect?=null
                val barrier=java.util.concurrent.CountDownLatch(1)
                e.gl.post {
                    val result=e.signalMetadata.values.lastOrNull()
                    focal=result?.get(CaptureResult.LENS_FOCAL_LENGTH)
                    crop=result?.get(CaptureResult.SCALER_CROP_REGION)
                    barrier.countDown()
                }
                check(barrier.await(5,java.util.concurrent.TimeUnit.SECONDS))
                check(focal!=null) { "No sensor metadata for ${lens.key}" }
                if(lens.physicalId!=null) check(kotlin.math.abs(focal!!-lens.focalMm!!)<.1f) { "Wrong lens focal=$focal expected=${lens.focalMm}" }
                val before=a.latest
                test.runOnMainSync {a.shoot()}
                test.await("JPEG ${lens.key}",{a.latest!=before && !e.photoBusy},20000)
                a.contentResolver.openInputStream(a.latest!!)!!.use { input ->
                    val exif=android.media.ExifInterface(input)
                    val saved=exif.getAttributeDouble(android.media.ExifInterface.TAG_FOCAL_LENGTH,0.0)
                    check(saved==0.0 || kotlin.math.abs(saved-focal!!.toDouble())<.1) { "EXIF wrong lens $saved vs $focal" }
                }
                report.append(lens.key).append(" focal=").append(focal).append(" crop=").append(crop).append(" JPEG saved\n")
            }
            val rawLens=lenses.firstOrNull {it.physicalId!=null &&
                it.characteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(android.graphics.ImageFormat.RAW_SENSOR)?.isNotEmpty()==true}
            if(rawLens!=null) {
                test.runOnMainSync {e.selectCamera(rawLens.key)}
                test.await("RAW sensor ready",{a.ready && e.activeLens?.key==rawLens.key},20000)
                val size=e.options!!.raws.minByOrNull {it.size.width.toLong()*it.size.height}
                if(size!=null) {
                    test.runOnMainSync {a.applySettings(CaptureSettings(a.settings).apply {photoFormat=2;photoSize=size.key()})}
                    test.await("physical RAW ready",{a.ready && e.stillReader!=null},20000)
                    val before=a.latest
                    test.runOnMainSync {a.shoot()}
                    test.await("physical DNG saved",{a.latest!=before && !e.photoBusy},30000)
                    a.contentResolver.openInputStream(a.latest!!)!!.use {input ->
                        val exif=android.media.ExifInterface(input)
                        val storedFocal=exif.getAttributeDouble(android.media.ExifInterface.TAG_FOCAL_LENGTH,0.0)
                        check(storedFocal==0.0 || kotlin.math.abs(storedFocal-rawLens.focalMm!!)<.1) { "DNG EXIF=$storedFocal expected=${rawLens.focalMm}" }
                    }
                    a.contentResolver.openInputStream(a.latest!!)!!.use {input ->
                        java.io.File(a.filesDir,"verification/physical-sensor.dng").outputStream().use {input.copyTo(it)}
                    }
                    report.append("physical DNG saved: ").append(rawLens.key).append('\n')
                    test.runOnMainSync {a.applySettings(CaptureSettings(a.settings).apply {photoFormat=0;photoSize="640x480"})}
                    test.await("return to JPEG",{a.ready && e.stillReader==null},15000)
                }
            }
            val wide=lenses.filter {it.physicalId!=null && !it.front}.minByOrNull {it.equivalentMm ?: 999.0} ?: lenses.first()
            test.runOnMainSync {e.selectCamera(wide.key)}
            test.await("wide ready",{a.ready && e.activeLens?.key==wide.key},20000)
            test.await("camera preference persisted",{a.getSharedPreferences("signal",0).getString("cameraSelection","")==wide.key},5000)
            val nativeDone=java.util.concurrent.CountDownLatch(1)
            var nativeCrop: android.graphics.Rect?=null
            e.gl.post {nativeCrop=e.signalMetadata.values.lastOrNull()?.get(CaptureResult.SCALER_CROP_REGION);nativeDone.countDown()}
            check(nativeDone.await(5,java.util.concurrent.TimeUnit.SECONDS))
            val active=wide.characteristics.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
            check(nativeCrop==active) { "Sensor is digitally cropped: $nativeCrop / $active" }
            test.runOnMainSync {a.setVideo(true)}
            test.await("wide video ready",{a.ready && a.videoMode},20000)
            val before=a.latest
            test.runOnMainSync {a.sound=false;a.shoot()}
            test.await("wide video starts",{e.recording},10000)
            test.runOnMainSync {e.selectCamera(lenses.first().key)}
            SystemClock.sleep(1800)
            check(e.activeLens?.key==wide.key) { "Switched during capture" }
            test.runOnMainSync {a.shoot()}
            test.await("wide MP4 saved",{!e.recording && !e.photoBusy && a.latest!=before},20000)
            check(VideoMetadata.read(a,a.latest!!)!=null)
            return "PASS lens selector, sensor result and JPEG EXIF match, settings and camera preference preserved, native uncropped sensor, physical DNG where supported, wide MP4 with capture switch guard\n$report"
        } finally {
            test.runOnMainSync {a.lensDialog?.dismiss();e.selectCamera(original)}
            test.await("restore camera",{e.activeLens?.key==original && a.ready},20000)
        }
    }
}
