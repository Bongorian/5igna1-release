package com.bongorian.signa1

import android.os.SystemClock
import android.view.View
import android.widget.ScrollView
import android.widget.TextView

internal object TransportUiChecks {
    fun run(test: DeviceChecks) {
        val a = test.activity!!
        val advanced = a.advancedMode
        try {
            test.runOnMainSync { a.advancedMode = false }
            for (id in intArrayOf(Effects.VHS, Effects.CRT)) {
                lateinit var editor: EffectDialog
                test.runOnMainSync {
                    editor = EffectDialog(a, true)
                    editor.mask = 1 shl id; editor.focused = id; editor.tuning = true
                    editor.show()
                }
                for (kind in 0..3) {
                    test.runOnMainSync {
                        editor.draft = editor.draft.with(id, "transport", kind / 3f)
                        editor.renderBody(); editor.preview()
                        check(editor.body!!.findViewWithTag<View>("transport-transport") != null)
                    }
                    SystemClock.sleep(350)
                    test.languageScreenshot("transport-$id-$kind")
                }
                test.runOnMainSync {
                    a.advancedMode = true
                    editor.advancedGroup = FaultParameters.Group.PROFILE
                    editor.renderBody()
                    check(editor.advancedControls != null)
                    editor.sheet!!.dismiss()
                    a.advancedMode = false
                }
            }
            lateinit var quality: QualityDialog
            test.runOnMainSync { quality = QualityDialog(a); quality.show() }
            SystemClock.sleep(350)
            test.runOnMainSync {
                val content = quality.content!!
                fun index(resource: Int) = (0 until content.childCount).first {
                    (content.getChildAt(it) as? TextView)?.text?.toString() == a.getString(resource)
                }
                val metadata = index(R.string.settings_metadata)
                val experiments = index(R.string.settings_experiments)
                val app = index(R.string.settings_app)
                check(metadata < experiments && experiments < app)
                var parent = content.parent
                while (parent != null && parent !is ScrollView) parent = parent.parent
                (parent as? ScrollView)?.scrollTo(0, content.getChildAt(metadata).top)
            }
            SystemClock.sleep(350)
            test.languageScreenshot("transport-settings")
            test.runOnMainSync { quality.sheet!!.dismiss() }
        } finally {
            test.runOnMainSync { (a.effectEditorOwner as? EffectDialog)?.sheet?.dismiss(); a.advancedMode = advanced }
        }
    }
}
