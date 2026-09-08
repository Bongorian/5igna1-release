package com.bongorian.signa1

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object SignalControls {
    fun value(value: Float): String {
        if (abs(value - Math.round(value)) < .00001f) return Math.round(value).toLong().toString()
        return String.format(Locale.US, "%.4f", value)
            .replace("0+$".toRegex(), "")
            .replace("\\.$".toRegex(), "")
    }

    fun field(a: MainActivity, body: LinearLayout, text: String?): TextView {
        val row = a.button(text)
        a.typography(row, MainActivity.TEXT_LABEL, true)
        row.setGravity(Gravity.CENTER_VERTICAL)
        row.setPadding(a.dp(14f), a.dp(8f), a.dp(14f), a.dp(8f))
        row.setMinHeight(a.dp(48f))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.bottomMargin = a.dp(8f)
        body.addView(row, p)
        return row
    }

    fun slider(
        a: MainActivity,
        body: LinearLayout,
        label: String?,
        value: Float,
        min: Float,
        max: Float,
        step: Float,
        suffix: String?,
        changed: (Float) -> Unit,
    ): SeekBar {
        val row = a.row()
        val title = a.text(label, 12, MainActivity.MUTED)
        val number = a.button("")
        number.setTextColor(MainActivity.LIME)
        number.setBackgroundColor(Color.TRANSPARENT)
        row.addView(title, LinearLayout.LayoutParams(0, a.dp(38f), 1f))
        row.addView(number, LinearLayout.LayoutParams(-2, a.dp(38f)))
        body.addView(row)
        val slider = SeekBar(a)
        slider.setContentDescription(label)
        val count = max(1, min(10000, Math.round((max - min) / step)))
        slider.setMax(count)
        slider.setProgress(Math.round((value - min) / (max - min) * count))
        slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME))
        slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME))
        body.addView(slider, LinearLayout.LayoutParams(-1, a.dp(42f)))
        number.setText(value(value) + suffix)
        slider.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(view: SeekBar?, p: Int, user: Boolean) {
                    if (user) {
                        val next =
                            max(
                                min,
                                min(
                                    max,
                                    min +
                                        Math.round((p / count.toFloat() * (max - min)) / step) *
                                            step,
                                ),
                            )
                        number.setText(value(next) + suffix)
                        changed(next)
                    }
                }

                override fun onStartTrackingTouch(view: SeekBar?) {}

                override fun onStopTrackingTouch(view: SeekBar?) {}
            }
        )
        number.setOnClickListener(
            OnClickListener@{ v: View? ->
                SignalSheet.number(
                    a,
                    label,
                    value(min) + " … " + value(max),
                    value(min + (max - min) * slider.progress / count),
                    false,
                    Predicate@{ text: String ->
                        val next = text!!.toFloat()
                        if (!java.lang.Float.isFinite(next) || next < min || next > max)
                            return@Predicate false
                        slider.setProgress(Math.round((next - min) / (max - min) * count))
                        number.setText(value(next) + suffix)
                        changed(next)
                        true
                    },
                )
            }
        )
        return slider
    }
}
