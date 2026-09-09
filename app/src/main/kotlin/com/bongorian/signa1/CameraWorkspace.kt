package com.bongorian.signa1

import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/** Measures against the current window, including split-screen and freeform resizing. */
internal class CameraWorkspace(val a: MainActivity) : LinearLayout(a) {
    lateinit var utilityBlock: LinearLayout
    lateinit var previewColumn: LinearLayout
    lateinit var controlsColumn: LinearLayout
    lateinit var controlsScroll: ScrollView
    private var safe = android.graphics.Insets.NONE

    fun applySafeInsets(value: android.graphics.Insets) {
        safe = value
        requestLayout()
    }

    var wide = false
        private set

    private fun size(view: View, next: LayoutParams) {
        val old = view.layoutParams as? LayoutParams
        if (old == null || old.width != next.width || old.height != next.height ||
            old.weight != next.weight || old.leftMargin != next.leftMargin) view.layoutParams = next
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fullWidth = MeasureSpec.getSize(widthMeasureSpec)
        val margin = a.dp(if (fullWidth - safe.left - safe.right < a.dp(400f)) 12f else 18f)
        if (paddingLeft != margin + safe.left || paddingRight != margin + safe.right ||
            paddingTop != a.dp(8f) + safe.top || paddingBottom != a.dp(4f) + safe.bottom)
            setPadding(margin + safe.left, a.dp(8f) + safe.top, margin + safe.right, a.dp(4f) + safe.bottom)
        val w = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(1)
        val h = (MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom).coerceAtLeast(1)
        wide = w >= a.dp(600f) && w > h
        val utilityParent = if (wide) controlsColumn else previewColumn
        if (utilityBlock.parent !== utilityParent) {
            (utilityBlock.parent as? LinearLayout)?.removeView(utilityBlock)
            utilityParent.addView(utilityBlock, 0, LayoutParams(-1, -2))
        }
        orientation = if (wide) HORIZONTAL else VERTICAL
        val controlW = if (wide) (w * .38f).toInt().coerceIn(a.dp(300f), a.dp(400f)) else w
        val content = controlsScroll.getChildAt(0)
        content.measure(MeasureSpec.makeMeasureSpec(controlW, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val controlH = (content.measuredHeight + a.dp(88f)).coerceAtMost((h * .55f).toInt())
        size(previewColumn, if (wide) LayoutParams(0, -1, 1f) else LayoutParams(-1, 0, 1f))
        size(controlsColumn, if (wide) LayoutParams(controlW, -1).apply { leftMargin = a.dp(12f) }
            else LayoutParams(-1, controlH))
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
