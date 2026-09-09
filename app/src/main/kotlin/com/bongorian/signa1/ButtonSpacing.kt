package com.bongorian.signa1

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

/** Minimum separation between adjacent editor controls; repeated refreshes do not add margins. */
internal object ButtonSpacing {
    private fun controls(view: View): Boolean =
        (view is TextView && view.isClickable) ||
            (view is ViewGroup && (0 until view.childCount).any { controls(view.getChildAt(it)) })

    fun apply(a: MainActivity, root: View) {
        if (root !is ViewGroup) return
        for (i in 0 until root.childCount) apply(a, root.getChildAt(i))
        if (root !is LinearLayout) return
        val gap = a.dp(8f)
        var previous: View? = null
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child.visibility == View.GONE) continue
            val before = previous
            if (before != null && controls(before) && controls(child)) {
                val prior = before.layoutParams as? ViewGroup.MarginLayoutParams
                val params = child.layoutParams as? ViewGroup.MarginLayoutParams
                if (prior != null && params != null) {
                    if (root.orientation == LinearLayout.HORIZONTAL)
                        params.leftMargin = max(params.leftMargin, gap - prior.rightMargin)
                    else params.topMargin = max(params.topMargin, gap - prior.bottomMargin)
                    child.layoutParams = params
                }
            }
            previous = child
        }
    }
}
