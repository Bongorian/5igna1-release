package com.bongorian.signa1

import android.view.Gravity
import android.widget.LinearLayout

/** The mode rail remains centered independently of capture tools and FAULT controls. */
internal class CaptureToolbar(private val a: MainActivity) : LinearLayout(a) {
    init { orientation = HORIZONTAL; gravity = Gravity.CENTER }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val buttons = listOf(a.photoTab, a.videoTab, a.tapTab).filter { it.visibility != GONE }
        val width = (MeasureSpec.getSize(widthMeasureSpec) / buttons.size.coerceAtLeast(1)).coerceAtMost(a.dp(104f))
        buttons.forEach { button ->
            val old = button.layoutParams as LayoutParams
            if (old.width != width || old.height != a.dp(48f)) button.layoutParams = LayoutParams(width, a.dp(48f))
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(a.dp(48f), MeasureSpec.EXACTLY))
    }
}
