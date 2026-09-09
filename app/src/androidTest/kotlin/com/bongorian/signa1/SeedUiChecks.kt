package com.bongorian.signa1

import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.TextView

internal object SeedUiChecks {
    fun run(test: DeviceChecks): String {
        val a = test.activity!!
        val advanced = a.advancedMode
        val initial = a.effectState
        try {
            for (mode in booleanArrayOf(false, true)) {
                lateinit var editor: EffectDialog
                test.runOnMainSync {
                    a.advancedMode = mode
                    a.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
                    editor = EffectDialog(a, true); editor.show()
                    val before = editor.draft.identity(Effects.PIXEL_DAMAGE)
                    editor.body!!.findViewWithTag<View>("reseed").performClick()
                    val after = editor.draft.identity(Effects.PIXEL_DAMAGE)
                    check(before != after)
                    check(editor.body!!.findViewWithTag<TextView>("identity-seed").text.toString().endsWith(after.toString())) { "Stale SEED field" }
                    check(a.effectState.parameters().identity(Effects.PIXEL_DAMAGE) == before) { "RESEED committed before Apply" }
                    editor.body!!.findViewWithTag<View>("identity-seed").performClick()
                    val dialog = editor.auxiliary!!
                    val content = dialog.findViewById<View>(android.R.id.content)
                    val input = content.findViewWithTag<EditText>("number-input")
                    val apply = content.findViewWithTag<View>("number-apply")
                    input.setText("9223372036854775808"); apply.performClick()
                    check(dialog.isShowing && editor.draft.identity(Effects.PIXEL_DAMAGE) == after)
                    input.setText(Long.MIN_VALUE.toString()); apply.performClick()
                    check(!dialog.isShowing && editor.draft.identity(Effects.PIXEL_DAMAGE) == Long.MIN_VALUE)
                }
                SystemClock.sleep(500)
                test.runOnMainSync {
                    val seed = editor.body!!.findViewWithTag<View>("identity-seed")
                    val reroll = editor.body!!.findViewWithTag<View>("reseed")
                    check(reroll.left - seed.right >= a.dp(8f)) { "Seed buttons touch" }
                    val margin = (reroll.layoutParams as android.view.ViewGroup.MarginLayoutParams).leftMargin
                    ButtonSpacing.apply(a, editor.body!!)
                    check((reroll.layoutParams as android.view.ViewGroup.MarginLayoutParams).leftMargin == margin) { "Spacing accumulates" }
                }
                test.languageScreenshot("seed-$mode")
                test.runOnMainSync { editor.sheet!!.dismiss() }
                check(a.effectState.parameters().identity(Effects.PIXEL_DAMAGE) == EffectParameters.defaults().identity(Effects.PIXEL_DAMAGE)) { "Cancel changed seed" }
            }
            test.runOnMainSync {
                a.advancedMode = true
                a.commitEffects(EffectState.defaults().single(Effects.VHS))
                val editor = EffectDialog(a, true); editor.show()
                editor.body!!.findViewWithTag<View>("event-identity").performClick()
                val content = editor.auxiliary!!.findViewById<View>(android.R.id.content)
                content.findViewWithTag<EditText>("number-input").setText(Long.MAX_VALUE.toString())
                content.findViewWithTag<View>("number-apply").performClick()
                editor.body!!.findViewWithTag<View>("reseed").performClick()
                check(editor.draft.eventIdentity(Effects.VHS, 0) == Long.MAX_VALUE)
                editor.body!!.findViewWithTag<View>("event-auto").performClick()
                check(!editor.draft.fixedEventIdentity(Effects.VHS))
                val chosen = editor.draft.identity(Effects.VHS)
                editor.sheet!!.window!!.decorView.findViewWithTag<View>("apply").performClick()
                check(EffectStateStore.load(a.getSharedPreferences("signal", 0)).parameters().identity(Effects.VHS) == chosen)
                a.recording = true
                val before = a.effectState.encode()
                a.reseed()
                check(a.effectState.encode() == before)
                a.recording = false
            }
            return "PASS SEED/RESEED basic and advanced UI refresh, signed 64-bit input and overflow rejection, draft cancellation, Apply persistence, fixed/AUTO event seed and recording guard"
        } finally {
            test.runOnMainSync {
                (a.effectEditorOwner as? EffectDialog)?.sheet?.dismiss()
                a.recording = false; a.advancedMode = advanced; a.commitEffects(initial)
            }
        }
    }
}
