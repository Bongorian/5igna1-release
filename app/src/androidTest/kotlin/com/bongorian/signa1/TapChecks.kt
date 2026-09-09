package com.bongorian.signa1

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object TapChecks {
    fun run(test: DeviceChecks): String {
        val a = test.activity!!
        val image = File(a.cacheDir, "tap-fixture.png")
        val fixture = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        Canvas(fixture).apply {
            drawColor(Color.BLUE)
            drawRect(0f, 0f, 320f, 120f, Paint().apply { color = Color.RED })
        }
        FileOutputStream(image).use { fixture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        fixture.recycle()
        val video = File(a.filesDir, "tap-fixture.mp4")
        check(video.exists()) { "Push tap-fixture.mp4 to files before this check" }
        test.runOnMainSync {
            a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals = true; advancedMode = false })
            a.applyFaultConfig(FaultConfig.defaults().enabled(true))
            a.commitEffects(EffectState.defaults().single(Effects.ROW_ERROR).amount(1f))
            a.enterTap(TapInput(Uri.fromFile(image), false))
        }
        test.await("TAP image ready", { a.ready && a.engine.tapSource?.ready == true }, 15000)
        check(a.engine.camera == null) { "TAP kept camera open" }
        check(a.tapTab.visibility == View.VISIBLE && !a.videoMode)
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        a.engine.gl.post {
            try {
                val rendered = a.engine.encoderScratch
                check(rendered.frame!!.injection && rendered.frame!!.nodes.isEmpty()) { "Upstream READOUT applied to TAP" }
                val pixels = rendered.read()
                val top = pixels.getPixel(pixels.width / 2, pixels.height / 4)
                val bottom = pixels.getPixel(pixels.width / 2, pixels.height * 3 / 4)
                check(Color.red(top) > 240 && Color.blue(top) < 15 && Color.blue(bottom) > 240) { "TAP image orientation/color" }
                pixels.recycle()
            } catch (failure: Throwable) { error = failure }
            finally { latch.countDown() }
        }
        check(latch.await(5, TimeUnit.SECONDS))
        error?.let { throw AssertionError("TAP pixels", it) }
        val beforePhoto = a.latest
        test.runOnMainSync { a.shoot() }
        test.await("TAP JPEG", { a.latest != beforePhoto && !a.engine.photoBusy }, 20000)
        test.runOnMainSync { FaultDialog.show(a) }
        SystemClock.sleep(600)
        test.languageScreenshot("tap-live-time")
        test.runOnMainSync { a.liveEditor!!.page = 1; a.liveEditor!!.render() }
        SystemClock.sleep(400)
        test.languageScreenshot("tap-live-inputs")
        test.runOnMainSync { a.liveEditor!!.page = 2; a.liveEditor!!.render() }
        SystemClock.sleep(400)
        check(a.liveEditor!!.body!!.findViewWithTag<View>("echo-probability") != null)
        test.languageScreenshot("tap-live-echo")
        test.runOnMainSync { a.liveEditor!!.dialog!!.dismiss() }
        test.await("LIVE editor dismissed", { a.liveEditor == null }, 3000)
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "TAP-test-fixture.mp4")
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
        }
        val fixtureUri = a.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
        a.contentResolver.openOutputStream(fixtureUri)!!.use { out -> video.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
        a.contentResolver.update(fixtureUri, values, null, null)
        test.runOnMainSync { a.enterTap(TapInput(fixtureUri, true)) }
        test.await("TAP video ready", { a.ready && a.engine.tapSource?.ready == true && a.videoMode }, 20000)
        check(!a.engine.tapSource!!.playing)
        SystemClock.sleep(500)
        test.runOnMainSync {
            val parent = a.capture.parent as View
            check(kotlin.math.abs(a.capture.left + a.capture.width/2 - parent.width/2) <= 1) { "TAP shutter off center" }
            check(a.flipButton.visibility == View.INVISIBLE)
        }
        test.runOnMainSync { a.shoot() }
        test.await("automatic TAP playback", { a.engine.recording && a.engine.tapSource!!.playing },15000)
        SystemClock.sleep(1800)
        val autoSaved = a.latest
        test.runOnMainSync { a.shoot() }
        test.await("automatic TAP pause/save", { !a.engine.recording && !a.engine.tapSource!!.playing && a.latest != autoSaved },15000)
        test.runOnMainSync { a.tapPlay.performClick() }
        test.await("preview plays without recorder", { a.engine.tapSource!!.playing }, 3000)
        check(!a.engine.recording)
        test.languageScreenshot("tap-video")
        test.runOnMainSync { a.shoot() }
        test.await("TAP output recording", { a.engine.recording }, 15000)
        test.runOnMainSync { a.tapPlay.performClick() }
        test.await("pause independent from recording", { !a.engine.tapSource!!.playing }, 3000)
        check(a.engine.recording)
        test.runOnMainSync { a.tapPlay.performClick() }
        SystemClock.sleep(1800)
        backgroundSave(test, "TAP")
        check(a.engine.tapSource?.playing == false)
        test.runOnMainSync { a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals = false }) }
        test.await("experimental OFF restores camera", { !a.tapMode && a.ready && a.engine.camera != null }, 15000)
        check(a.tapTab.visibility == View.GONE && a.engine.tapInput == null)
        a.contentResolver.delete(fixtureUri, null, null)
        test.runOnMainSync { a.setVideo(true) }
        test.await("camera video ready", { a.ready && a.videoMode && !a.tapMode }, 15000)
        test.runOnMainSync { a.shoot() }
        test.await("camera recording", { a.engine.recording }, 15000)
        SystemClock.sleep(1800)
        backgroundSave(test, "camera")
        // Repeat with audio, and verify that returning never restarts the recording.
        SystemClock.sleep(1000)
        test.await("camera settled before audio", { a.ready && a.engine.frameSeen },15000)
        test.runOnMainSync { a.sound = true; a.shoot() }
        test.await("audio recording", { a.engine.recording && a.engine.recorderAudio }, 15000)
        SystemClock.sleep(1800)
        backgroundSave(test, "camera/audio", audio = true)
        val permissions = a.packageManager.getPackageInfo(a.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        check(permissions.none { it.contains("FOREGROUND_SERVICE") || it.endsWith("WAKE_LOCK") || it.endsWith("POST_NOTIFICATIONS") })
        return "PASS centered TAP shutter, automatic record play/stop, TAP boundary/orientation, image/video import, independent play/record, JPEG and decodable MP4 output; Home/screen-exit stops and saves TAP/camera/audio recording; hidden camera/render/microphone released; return does not restart recording; no FGS/wake/notification permissions"
    }

    private fun backgroundSave(test: DeviceChecks, label: String, audio: Boolean = false) {
        val a = test.activity!!
        val before = a.latest
        val lock = test.args?.getString("leave") == "lock"
        test.sendKeyDownUpSync(if (lock) KeyEvent.KEYCODE_SLEEP else KeyEvent.KEYCODE_HOME)
        test.await("$label stopped and saved", {
            !a.resumed && !a.engine.attached && !a.engine.recording && a.latest != before &&
                a.engine.camera == null && a.engine.tapSource == null && a.engine.faultInputs.microphone == null
        }, 15000)
        val saved = a.latest!!
        check(a.engine.camera == null && a.engine.tapSource == null && a.engine.faultInputs.microphone == null)
        val frames = a.engine.renderedFrames
        SystemClock.sleep(1000)
        check(a.engine.renderedFrames == frames) { "$label renders while hidden" }
        android.media.MediaMetadataRetriever().use { media ->
            media.setDataSource(a, saved)
            check((media.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L) > 0)
            val decoded = media.getFrameAtTime(0)
            check(decoded != null) { "$label saved file cannot decode" }
            decoded.recycle()
            if (audio) check(media.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes")
        }
        if (lock) {
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_WAKEUP)
            test.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        }
        test.uiAutomation.executeShellCommand("am start -f 0x20020000 -n ${a.packageName}/${MainActivity::class.java.name}").close()
        test.await("$label preview return", { a.resumed && a.ready }, 15000)
        check(!a.engine.recording && !a.recording) { "$label restarted recording" }

    }
}
