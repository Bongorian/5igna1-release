package com.bongorian.signa1

import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Sizes the capture row from its icon targets and the current available width. */
internal class CaptureToolbar(private val a: MainActivity) : LinearLayout(a) {
    lateinit var live: LinearLayout
    lateinit var modes: LinearLayout
    lateinit var tune: ImageView

    init { orientation = HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }

    private fun dimensions(view: View, width: Int, height: Int) {
        val p = view.layoutParams as LayoutParams
        if (p.width != width || p.height != height) {
            p.width = width; p.height = height; view.layoutParams = p
        }
    }

    private fun font(view: TextView, sp: Float) {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
        if (kotlin.math.abs(view.textSize - px) > .1f) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed,l,t,r,b)
        // Center the mode group where the side controls leave room; on narrow windows keep all
        // touch targets accessible rather than allowing the LIVE controls to overlap it.
        val first = a.formatButton.right + a.dp(4f)
        val last = live.left - a.dp(4f) - modes.measuredWidth
        if (last >= first) {
            val x = ((width-modes.measuredWidth)/2).coerceIn(first,last)
            modes.layout(x,modes.top,x+modes.measuredWidth,modes.bottom)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val buttons = listOf(a.photoTab, a.videoTab, a.tapTab).filter { it.visibility != GONE }
        val iconTarget = a.dp(48f)
        val height = iconTarget
        val glyph = a.dp(20f)
        for (button in buttons) {
            dimensions(button, iconTarget, iconTarget)
            val inset = (iconTarget - glyph) / 2
            if (button.paddingLeft != inset) button.setPadding(inset, inset, inset, inset)
        }
        dimensions(modes, -2, height)
        dimensions(a.formatButton, iconTarget, height)
        val tuneWidth = iconTarget - a.dp(8f)
        dimensions(tune, tuneWidth, height)
        tune.scaleType = ImageView.ScaleType.FIT_CENTER
        val tuneInset = (tuneWidth - glyph) / 2
        val tuneVertical = (height-glyph)/2
        if (tune.paddingLeft != tuneInset || tune.paddingTop != tuneVertical)
            tune.setPadding(tuneInset, tuneVertical, tuneInset, tuneVertical)
        val oneLine = !a.cameraRoot.wide
        val on = if (oneLine) "LIVE ON" else "LIVE\nON"
        val off = if (oneLine) "LIVE OFF" else "LIVE\nOFF"
        if (a.faultSwitch.textOn != on || a.faultSwitch.textOff != off) {
            a.faultSwitch.textOn = on; a.faultSwitch.textOff = off
            a.faultSwitch.text = if (a.faultSwitch.isChecked) on else off
        }
        val textSp = 11f
        font(a.formatButton, textSp)
        val availableLabel = (width - buttons.size*iconTarget - a.dp(4f) - a.dp(8f) - iconTarget - tuneWidth).coerceAtLeast(1)
        val measure = android.graphics.Paint(a.faultSwitch.paint).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, resources.displayMetrics)
        }
        val desired = measure.measureText(if (oneLine) off else "LIVE")
        val labelWidth = minOf(availableLabel, maxOf(a.dp(48f), kotlin.math.ceil(desired).toInt()+a.dp(8f)))
        dimensions(a.faultSwitch, labelWidth, height)
        dimensions(live, labelWidth+tuneWidth, height)
        val fittedPx = measure.textSize * minOf(1f, (labelWidth-a.dp(4f)).coerceAtLeast(1) / desired)
        if (kotlin.math.abs(a.faultSwitch.textSize - fittedPx) > .1f)
            a.faultSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, fittedPx)
        val lines = if (oneLine) 1 else 2
        if (a.faultSwitch.maxLines != lines) a.faultSwitch.maxLines = lines
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}
