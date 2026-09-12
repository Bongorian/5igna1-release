package com.bongorian.signa1

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.TextView

/** Emulator review captures plus regressions for status placement and the shared shutter. */
internal object WorkspaceReviewChecks {
    fun run(test: DeviceChecks): String {
        val locale = test.args?.getString("language") ?: "ja"
        test.changeLanguage(locale)
        fun host() = requireNotNull(test.activity)
        fun settle() {
            test.await("review camera", { host().ready && host().engine.frameSeen && !host().isDestroyed }, 30000)
            test.waitForIdleSync()
            SystemClock.sleep(800)
        }
        fun orient(landscape: Boolean) {
            val monitor = test.addMonitor(MainActivity::class.java.name, null, false)
            test.runOnMainSync { host().requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            val end = SystemClock.elapsedRealtime() + 30000
            while (SystemClock.elapsedRealtime() < end) {
                (monitor.waitForActivityWithTimeout(250) as? MainActivity)?.let { test.activity = it }
                if (!host().isDestroyed && host().resumed && host().ready && host().cameraRoot.wide == landscape) break
            }
            test.removeMonitor(monitor)
            settle()
            check(host().cameraRoot.wide == landscape)
        }
        fun snapshot(name: String) {
            settle()
            test.runOnMainSync {
                val a = host()
                a.window.decorView.findViewWithTag<View>("deviceCheckOverlay")?.visibility = View.GONE
                check(a.formatButton.lineCount == 1) { "Format label wrapped" }
                check(a.status.parent === a.cameraRoot.utilityBlock) { "Status overlays preview" }
                check(a.capture.width == a.dp(80f) && a.capture.height == a.dp(80f)) { "Shutter resized" }
                if (a.engine.proMode && a.proStrip.visibility == View.VISIBLE) {
                    for (key in listOf("exposure", "wb", "focus", "lens")) {
                        val field = a.proStrip.findViewWithTag<TextView>("pro-field:" + key)
                        if (field.visibility != View.VISIBLE) continue
                        val fieldRect = Rect()
                        check(field.getGlobalVisibleRect(fieldRect) && fieldRect.width() == field.width) { "PRO field clipped: $key" }
                    }
                }
                val visible = Rect()
                check(a.capture.getGlobalVisibleRect(visible) && visible.width() == a.capture.width && visible.height() == a.capture.height) { "Shutter clipped" }
                if (a.cameraRoot.wide) {
                    check(a.lensScroll.parent !== a.viewfinder)
                    check(a.viewfinder.findViewWithTag<View>("fx").visibility == View.GONE)
                }
            }
            test.languageScreenshot("workspace-$locale-$name")
        }
        try {
            orient(false)
            test.runOnMainSync {
                val a = host()
                a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals = true })
                a.renderCaptureMode()
                a.chooseEffect(Effects.ROW_ERROR)
                a.faultDeckExpanded = false
                a.renderFaultDeck()
                a.engine.setProMode(false)
            }
            snapshot("01-portrait-auto")
            test.runOnMainSync { host().faultDeckButton.performClick() }
            snapshot("02-portrait-fault")
            test.runOnMainSync { host().faultDeckButton.performClick(); host().engine.setProMode(true) }
            snapshot("03-portrait-pro")
            test.runOnMainSync { host().showProCamera("exposure") }
            SystemClock.sleep(800)
            test.languageScreenshot("workspace-$locale-04-pro-exposure")
            test.runOnMainSync { host().proCameraDialog?.dialog?.dismiss(); host().engine.setProMode(false) }
            orient(true)
            test.runOnMainSync { host().settings.experimentalSignals = true; host().renderCaptureMode() }
            snapshot("05-landscape")
            val shutter = host().capture
            test.runOnMainSync { host().cameraRoot.findViewWithTag<TextView>("workspace-toggle").performClick() }
            snapshot("06-landscape-collapsed")
            check(host().capture === shutter)
            test.runOnMainSync { host().cameraRoot.findViewWithTag<TextView>("workspace-toggle").performClick(); host().setVideo(true) }
            snapshot("07-landscape-video")
            test.runOnMainSync { host().capture.performClick() }
            test.await("recording", { host().recording }, 15000)
            test.runOnMainSync { host().cameraRoot.findViewWithTag<TextView>("workspace-toggle").performClick() }
            SystemClock.sleep(1400)
            snapshot("08-landscape-recording-collapsed")
            check(host().capture === shutter)
            test.runOnMainSync {
                val clock = host().cameraRoot.findViewWithTag<TextView>("workspace-recording-clock")
                check(clock.visibility == View.VISIBLE && clock.text.contains(":"))
            }
            test.runOnMainSync { host().capture.performClick() }
            test.await("recording stopped", { !host().recording && !host().engine.recording }, 20000)
            test.runOnMainSync { host().cameraRoot.findViewWithTag<TextView>("workspace-toggle").performClick() }
            settle()
            test.runOnMainSync { host().setVideo(false) }
            settle()
            orient(false)
            test.runOnMainSync { host().showSettings() }
            SystemClock.sleep(1000)
            test.languageScreenshot("workspace-$locale-09-settings")
            return "PASS $locale: 9 screenshots; status outside preview, 80 dp unclipped shutter, same capture instance across landscape collapse and recording/stop"
        } finally {
            test.runOnMainSync {
                host().proCameraDialog?.dialog?.dismiss()
                host().workspaceCollapsed = false
                host().cameraRoot.requestLayout()
                if (host().recording) host().capture.performClick()
            }
        }
    }
}
