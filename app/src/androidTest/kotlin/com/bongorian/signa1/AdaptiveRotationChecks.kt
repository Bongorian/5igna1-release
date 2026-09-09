package com.bongorian.signa1

import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import java.io.File

internal object AdaptiveRotationChecks {
    fun run(test: DeviceChecks): String {
        for (rotation in intArrayOf(0, 1, 3, 2)) {
            val monitor = test.addMonitor(MainActivity::class.java.name, null, false)
            test.uiAutomation.setRotation(rotation)
            val deadline = SystemClock.elapsedRealtime() + 25000
            var ready = false
            while (SystemClock.elapsedRealtime() < deadline) {
                val next = monitor.waitForActivityWithTimeout(250) as? MainActivity
                if (next != null) test.activity = next
                val current = test.activity!!
                ready = !current.isDestroyed && current.resumed && current.ready && current.engine.captureRotation ==
                    CameraOrientation.relative(current.engine.sensorRotation, rotation * 90, current.engine.front)
                if (ready) break
            }
            test.removeMonitor(monitor)
            check(ready) { "Rotation $rotation preview did not recover" }
            val a = test.activity!!
            val stream = a.engine.photoChoice!!.size
            val swapped = a.engine.captureRotation % 180 != 0
            check(a.engine.outW == if (swapped) stream.height else stream.width)
            check(a.engine.outH == if (swapped) stream.width else stream.height)
            test.languageScreenshot("rotation-$rotation")
        }
        val file = File(test.targetContext.filesDir, "adaptive-tap.png")
        val bitmap = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        test.runOnMainSync {
            val a = test.activity!!
            a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals = true })
            a.enterTap(TapInput(Uri.fromFile(file), false))
        }
        test.await("TAP before recreation", { test.activity!!.ready && test.activity!!.engine.tapSource?.ready == true }, 15000)
        test.recreateTutorialActivity()
        test.await("TAP restored after recreation", { test.activity!!.ready && test.activity!!.engine.tapSource?.ready == true }, 15000)
        check(test.activity!!.tapMode && test.activity!!.tapInput!!.uri == Uri.fromFile(file))
        check(test.activity!!.engine.camera == null)
        check(test.activity!!.engine.outW == 96 && test.activity!!.engine.outH == 64)
        test.runOnMainSync { test.activity!!.setVideo(false) }
        test.await("camera after TAP", { test.activity!!.ready && test.activity!!.engine.camera != null }, 15000)
        file.delete()
        test.runOnMainSync {
            val root = FeedbackDialog.show(test.activity!!).window!!.decorView
            root.findViewWithTag<android.widget.EditText>("feedback-message").setText("Draft survives rotation 日本語")
        }
        test.recreateTutorialActivity()
        test.await("feedback restored", { test.activity!!.feedbackDialog != null }, 5000)
        test.runOnMainSync {
            val window = test.activity!!.feedbackDialog!!.window!!
            check(window.attributes.width > 0 && window.attributes.height > 0) { "Restored feedback has no usable size" }
            val root = window.decorView
            check(root.findViewWithTag<android.widget.EditText>("feedback-message").text.toString() == "Draft survives rotation 日本語")
            check(!root.findViewWithTag<android.widget.CheckBox>("feedback-device-info").isChecked)
            test.activity!!.feedbackDialog!!.dismiss()
        }
        return "PASS all four display rotations, dimension swaps, 180-degree live orientation, camera recovery and TAP URI/mode/dimensions and feedback draft across recreation"
    }
}
