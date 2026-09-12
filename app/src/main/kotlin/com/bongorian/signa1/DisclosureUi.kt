package com.bongorian.signa1

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.TextView

/** Consistent disclosure styling: explicit action text and a drawn directional chevron. */
internal object DisclosureUi {
    fun bind(a: MainActivity, view: TextView, expanded: Boolean, horizontal: Boolean, label: String) {
        view.isFocusable = true
        if (view.text.toString() != label) view.text = label
        view.stateDescription = a.getString(if (expanded) R.string.disclosure_expanded else R.string.disclosure_collapsed)
        val direction = if (horizontal) { if (expanded) 0 else 1 } else { if (expanded) 2 else 3 }
        val existing = view.compoundDrawablesRelative[2] as? Chevron
        if (existing?.direction != direction) {
            val icon = Chevron(direction).apply { setBounds(0, 0, a.dp(14f), a.dp(14f)) }
            view.setCompoundDrawablesRelative(null, null, icon, null)
            view.compoundDrawablePadding = a.dp(4f)
        }
    }
    private class Chevron(val direction: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MainActivity.MUTED; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
            paint.strokeWidth = w / 8f
            val p = Path()
            when (direction) {
                0 -> { p.moveTo(w*.35f,h*.2f);p.lineTo(w*.65f,h*.5f);p.lineTo(w*.35f,h*.8f) }
                1 -> { p.moveTo(w*.65f,h*.2f);p.lineTo(w*.35f,h*.5f);p.lineTo(w*.65f,h*.8f) }
                2 -> { p.moveTo(w*.2f,h*.65f);p.lineTo(w*.5f,h*.35f);p.lineTo(w*.8f,h*.65f) }
                else -> { p.moveTo(w*.2f,h*.35f);p.lineTo(w*.5f,h*.65f);p.lineTo(w*.8f,h*.35f) }
            }
            canvas.drawPath(p, paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
        @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
