package com.bongorian.signa1

import android.view.Gravity
import androidx.appcompat.widget.AppCompatToggleButton

/** App-styled, accessible two-state control without a platform switch track. */
internal class SignalToggle(activity: MainActivity, text: String?, checked: Boolean) :
    AppCompatToggleButton(activity) {
    val a: MainActivity?
    val label: String?

    init {
        a = activity
        label = text
        setAllCaps(false)
        a.typography(this, MainActivity.TEXT_LABEL, true)
        setGravity(Gravity.CENTER_VERTICAL)
        setPadding(a.dp(14f), 0, a.dp(14f), 0)
        setMinHeight(a.dp(48f))
        setTextOn(text + "   · ON")
        setTextOff(text + "   · OFF")
        setChecked(checked)
        paint()
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        if (a != null) paint()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setAlpha(if (enabled) 1f else .35f)
    }

    fun paint() {
        setBackgroundTintList(null)
        setTextColor(if (isChecked()) MainActivity.LIME else MainActivity.MUTED)
        setBackground(a!!.bg(MainActivity.PANEL, if (isChecked()) 0x665F7940 else 0))
        setContentDescription(label)
    }
}
