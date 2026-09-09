package com.bongorian.signa1

import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import android.widget.TextView

internal object ResponsiveUiChecks {
    fun run(test: DeviceChecks): String {
        for (rotation in intArrayOf(0, 1, 3)) {
            val monitor = test.addMonitor(MainActivity::class.java.name, null, false)
            test.uiAutomation.setRotation(rotation)
            val deadline = SystemClock.elapsedRealtime() + 25000
            while (SystemClock.elapsedRealtime() < deadline) {
                val next = monitor.waitForActivityWithTimeout(250) as? MainActivity
                if (next != null) test.activity = next
                val a = test.activity!!
                if (!a.isDestroyed && a.resumed && a.ready && a.windowManager.defaultDisplay.rotation == rotation) break
            }
            test.removeMonitor(monitor)
            val a = test.activity!!
            check(a.ready && !a.isDestroyed)
            test.runOnMainSync {
                a.settings.experimentalSignals = true
                a.renderCaptureMode()
                a.cameraRoot.controlsScroll.scrollTo(0,0)
            }
            SystemClock.sleep(700)
            test.runOnMainSync {
                val root = a.cameraRoot
                for (mode in listOf(a.photoTab,a.videoTab,a.tapTab)) check(mode.width>=a.dp(48f))
                check(a.formatButton.height == a.photoTab.height)
                check(kotlin.math.abs(a.photoTab.height-a.photoTab.paddingTop-a.photoTab.paddingBottom - a.dp(20f)) <= 1)
                val label = a.faultSwitch
                if (!root.wide) {
                    check(label.maxLines==1 && !label.text.contains('\n'))
                    check(label.paint.measureText("LIVE OFF") <= label.width+1) { "LIVE label clipped: ${label.width}" }
                }
                val parent = a.formatButton.parent as View
                val live = label.parent as View
                check(live.right <= parent.width) { "Capture controls overflow" }
            }
            test.languageScreenshot("responsive-$rotation-camera")
            lateinit var editor: EffectDialog
            test.runOnMainSync { editor=EffectDialog(a,false); editor.show() }
            SystemClock.sleep(500)
            test.runOnMainSync {
                val root = a.cameraRoot
                val decor = editor.sheet!!.window!!.decorView
                val actual = IntArray(2); decor.getLocationOnScreen(actual)
                val origin = IntArray(2); root.getLocationOnScreen(origin)
                val right = origin[0]+root.width-root.paddingRight
                if(root.wide) check(kotlin.math.abs(actual[0]+decor.width-right)<=1) { "Unused right edge: ${actual[0]+decor.width} / $right" }
                check(actual[1] >= origin[1]+root.paddingTop-1)
                check(actual[1]+decor.height <= origin[1]+root.height-root.paddingBottom+1) { "Editor bottom=${actual[1]+decor.height}, safe=${origin[1]+root.height-root.paddingBottom}, y=${actual[1]}, requestedY=${editor.sheet!!.window!!.attributes.y}" }
            }
            test.languageScreenshot("responsive-$rotation-chain")
            test.runOnMainSync { editor.sheet!!.dismiss(); a.showTutorial() }
            for(page in 0 until TutorialDialog.PAGE_COUNT) {
                SystemClock.sleep(150)
                test.runOnMainSync {
                    val guide=a.tutorial!!
                    val decor=guide.dialog.window!!.decorView
                    val card=decor.findViewWithTag<View>("tutorial-card")
                    check(card.width>0 && card.height>0)
                    val rect=Rect(); card.getHitRect(rect)
                    if(!guide.targetBounds.isEmpty) check(!RectF.intersects(RectF(rect),guide.targetBounds)) { "Guide overlaps highlighted control on page $page" }
                    if(page==4) {
                        val expected=Rect(); check(a.formatButton.getGlobalVisibleRect(expected)) { "Format control hidden during guide" }
                        val actual=Rect(); check(decor.findViewWithTag<View>("tutorial-target").getGlobalVisibleRect(actual)) { "Format highlight hidden during guide" }
                        check(kotlin.math.abs(expected.exactCenterX()-actual.exactCenterX())<=1f &&
                            kotlin.math.abs(expected.exactCenterY()-actual.exactCenterY())<=1f) { "Guide highlight does not track the format button: rotation=$rotation expected=$expected actual=$actual bounds=${guide.targetBounds}" }
                        val format=a.settings.photoFormat
                        decor.findViewWithTag<View>("tutorial-practice").performClick()
                        check(decor.findViewWithTag<TextView>("tutorial-practice").text.contains("RAW"))
                        check(a.settings.photoFormat==format)
                    }
                }
                SystemClock.sleep(100)
                if(page==4 || page==7) test.languageScreenshot("responsive-$rotation-guide-$page")
                test.tutorialClick("tutorial-next")
            }
            test.await("camera after responsive guide", { a.ready },20000)
        }
        return "PASS three orientations, icon-relative sizing, single-line portrait LIVE, right-edge editor alignment, non-overlapping guide highlights and isolated format practice"
    }
}
