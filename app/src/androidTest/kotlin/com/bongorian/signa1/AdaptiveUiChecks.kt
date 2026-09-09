package com.bongorian.signa1

import android.app.Dialog
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.view.View
import android.widget.CheckBox
import android.widget.EditText

internal object AdaptiveUiChecks {
    fun run(test: DeviceChecks): String {
        val a = test.activity!!
        test.runOnMainSync {
            a.window.decorView.findViewWithTag<View>("deviceCheckOverlay")?.visibility = View.GONE
            a.setVideo(false)
            a.settings.experimentalSignals = true
            a.renderCaptureMode()
        }
        SystemClock.sleep(800)
        test.runOnMainSync {
            val root = a.cameraRoot
            check(root.previewColumn.width > a.dp(100f) && a.previewArea.height > a.dp(100f))
            check(root.controlsColumn.bottom <= root.height - root.paddingBottom)
            if (root.wide) check(root.previewColumn.right < root.controlsColumn.left)
            else check(root.previewColumn.bottom <= root.controlsColumn.top)
            check(root.utilityBlock.parent === if (root.wide) root.controlsColumn else root.previewColumn)
            val formatParent = a.formatButton.parent as android.view.ViewGroup
            check(formatParent.indexOfChild(a.formatButton) > formatParent.indexOfChild(a.photoTab.parent as View))
            check(a.formatButton.width >= a.dp(48f))
            check(a.faultSwitch.width >= a.dp(48f)) { "LIVE label squeezed" }
            val modes = listOf(a.photoTab, a.videoTab, a.tapTab)
            for ((video, tap) in listOf(false to false, true to false, false to true)) {
                a.videoMode = video; a.tapMode = tap; a.renderCaptureMode()
                check(modes.count { it.isChecked() } == 1)
                check(modes.all { it.width >= a.dp(48f) && it.height >= a.dp(48f) })
                val selected = modes.single { it.isChecked() }
                check(selected.createAccessibilityNodeInfo().isChecked)
            }
            a.tapMode = false; a.videoMode = false; a.renderCaptureMode()
        }
        test.languageScreenshot("adaptive-camera")
        lateinit var editor: EffectDialog
        test.runOnMainSync { editor = EffectDialog(a, true); editor.show() }
        SystemClock.sleep(500)
        test.runOnMainSync {
            check(a.effectEditorSpace != null)
            check(a.cameraRoot.controlsColumn.visibility == if (a.cameraRoot.wide) View.INVISIBLE else View.GONE)
            check(a.previewArea.height > a.dp(80f))
            if (a.cameraRoot.wide) check(editor.sheet!!.window!!.attributes.width == a.cameraRoot.controlsColumn.width)
        }
        test.languageScreenshot("adaptive-editor")
        test.runOnMainSync { editor.sheet!!.dismiss() }
        lateinit var feedback: Dialog
        val filter = IntentFilter(Intent.ACTION_SENDTO).apply { addDataScheme("mailto") }
        val monitor = test.addMonitor(filter, Instrumentation.ActivityResult(0, null), true)
        try {
            test.runOnMainSync {
                feedback = FeedbackDialog.show(a)
                val root = feedback.window!!.decorView
                check(!root.findViewWithTag<CheckBox>("feedback-device-info").isChecked)
                root.findViewWithTag<EditText>("feedback-message").setText("Reproduction steps & expected result 日本語")
                val intent = FeedbackDialog.intent("Bug & feedback", "text 日本語")
                check(android.net.Uri.decode(intent.data.toString().substringAfter("&body=")) == "text 日本語")
                check(intent.data!!.scheme == "mailto" && intent.action == Intent.ACTION_SENDTO)
                root.findViewWithTag<View>("feedback-email").performClick()
            }
            check(test.checkMonitorHit(monitor, 1)) { "Email intent was not intercepted" }
            test.languageScreenshot("adaptive-feedback")
        } finally {
            test.removeMonitor(monitor)
            test.runOnMainSync { feedback.dismiss() }
        }
        return "PASS current-window layout, 48dp exclusive accessible icon modes, preview-preserving editor, opt-in device details and intercepted email draft; wide=${a.cameraRoot.wide}, size=${a.cameraRoot.width}x${a.cameraRoot.height}"
    }
}
