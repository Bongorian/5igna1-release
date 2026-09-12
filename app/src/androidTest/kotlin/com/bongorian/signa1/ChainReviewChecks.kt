package com.bongorian.signa1

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.ImageView

/** Real window captures and hit-target checks for the chain editor. */
internal object ChainReviewChecks {
    fun run(test: DeviceChecks): String {
        val language = test.args?.getString("language") ?: "ja"
        test.changeLanguage(language)
        fun host() = requireNotNull(test.activity)
        fun settle() {
            test.await("chain camera", { host().ready && host().engine.frameSeen && !host().isDestroyed }, 30000)
            test.waitForIdleSync()
            SystemClock.sleep(900)
        }
        fun orient(wide: Boolean) {
            val monitor = test.addMonitor(MainActivity::class.java.name, null, false)
            test.runOnMainSync { host().requestedOrientation = if (wide) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            val until = SystemClock.elapsedRealtime() + 30000
            while (SystemClock.elapsedRealtime() < until) {
                (monitor.waitForActivityWithTimeout(250) as? MainActivity)?.let { test.activity = it }
                if (!host().isDestroyed && host().resumed && host().ready && host().cameraRoot.wide == wide) break
            }
            test.removeMonitor(monitor)
            settle()
            check(host().cameraRoot.wide == wide)
        }
        for (wide in listOf(false, true)) {
            orient(wide)
            test.runOnMainSync {
                val a = host()
                a.chooseEffect(Effects.ROW_ERROR)
                a.faultDeckExpanded = true
                a.renderFaultDeck()
                a.window.decorView.findViewWithTag<View>("deviceCheckOverlay")?.visibility = View.GONE
            }
            settle()
            val prefix = "chain-$language-" + if (wide) "landscape" else "portrait"
            test.languageScreenshot("$prefix-deck")
            val original = host().effectState.mask
            lateinit var editor: EffectDialog
            test.runOnMainSync { editor = EffectDialog(host(), false); editor.show() }
            settle()
            fun verifyToolbar() {
                test.runOnMainSync {
                    val a = host()
                    check(!a.cameraRoot.findViewWithTag<View>("workspace-toggle").isShown) { "Fold action leaks behind editor" }
                    check(!a.cameraRoot.controlsColumn.isShown) { "Capture controls leak behind editor" }
                    val root = editor.sheet!!.window!!.decorView
                    if (wide) {
                        val previewBounds = Rect()
                        a.viewfinder.getGlobalVisibleRect(previewBounds)
                        val editorOrigin = IntArray(2)
                        root.getLocationOnScreen(editorOrigin)
                        check(previewBounds.right <= editorOrigin[0]) { "Editor overlaps camera preview" }
                    }
                    val rects = listOf("chain-close", "random-chain", "apply").map { tag ->
                        val icon = root.findViewWithTag<ImageView>(tag)
                        check(icon.width == a.dp(40f) && icon.height == a.dp(40f)) { "Wrong target size: $tag" }
                        check(icon.width - icon.paddingLeft - icon.paddingRight >= a.dp(20f)) { "Tiny icon: $tag" }
                        Rect().also { check(icon.getGlobalVisibleRect(it) && it.width() == icon.width && it.height() == icon.height) { "Clipped icon: $tag" } }
                    }
                    check(rects.zipWithNext().all { (left, right) -> left.right <= right.left }) { "Overlapping editor actions" }
                }
            }
            verifyToolbar()
            test.languageScreenshot("$prefix-catalog")
            test.runOnMainSync {
                editor.sheet!!.window!!.decorView.findViewWithTag<View>("chain-detail:${Effects.ROW_ERROR}").performClick()
            }
            settle()
            verifyToolbar()
            test.languageScreenshot("$prefix-adjust")
            test.runOnMainSync {
                editor.sheet!!.window!!.decorView.findViewWithTag<View>("random-chain").performClick()
                editor.sheet!!.window!!.decorView.findViewWithTag<View>("chain-close").performClick()
                check(host().effectState.mask == original) { "Cancel committed draft" }
            }
            settle()
            test.runOnMainSync {
                editor = EffectDialog(host(), false); editor.show()
                editor.mask = 1 shl Effects.BIT_ERROR
                editor.preview()
                editor.sheet!!.window!!.decorView.findViewWithTag<View>("apply").performClick()
                check(host().effectState.mask == 1 shl Effects.BIT_ERROR) { "Apply lost draft" }
            }
            settle()
        }
        return "PASS $language: 6 chain screenshots; 40 dp unclipped non-overlapping actions, 20 dp icons, underlying controls hidden, cancel/apply preserved"
    }
}
