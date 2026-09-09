package com.bongorian.signa1

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView
import android.widget.RadioButton

/** An icon segment; MainActivity's capture state is the single selection source. */
internal class CaptureModeButton(a: MainActivity, icon: Int, label: String) : AppCompatImageView(a), Checkable {
    init {
        setImageResource(icon)
        scaleType = ScaleType.CENTER_INSIDE
        setPadding(a.dp(12f), a.dp(12f), a.dp(12f), a.dp(12f))
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
        background = a.bg(if (checked) MainActivity.LIME else MainActivity.PANEL, 0)
        setColorFilter(if (checked) MainActivity.BG else MainActivity.MUTED)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RadioButton::class.java.name
        info.isCheckable = true
        info.isChecked = isChecked()
    }
}
