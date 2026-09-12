package com.bongorian.signa1

import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatTextView

/** Labeled shooting modes share one centered rail above the shutter. */
internal class CaptureModeButton(a: MainActivity, @Suppress("UNUSED_PARAMETER") icon: Int, label: String) : AppCompatTextView(a), Checkable {
    init {
        text = label
        a.typography(this, 12, true)
        gravity = Gravity.CENTER
        setSingleLine(true)
        setPadding(a.dp(12f), 0, a.dp(12f), 0)
        contentDescription = label
        tooltipText = label
        isClickable = true
        isFocusable = true
        setChecked(false)
    }
    override fun isChecked() = isSelected
    override fun toggle() { performClick() }
    override fun setChecked(checked: Boolean) {
        isSelected = checked
        val a = context as MainActivity
        background = a.bg(if (checked) MainActivity.PANEL else MainActivity.BG, 0)
        setTextColor(if (checked) MainActivity.LIME else MainActivity.MUTED)
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RadioButton::class.java.name
        info.isCheckable = true
        info.isChecked = isChecked()
    }
}
