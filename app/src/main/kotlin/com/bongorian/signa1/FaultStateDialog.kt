package com.bongorian.signa1

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Live, read-only event envelopes. Rows retain their positions while values change. */
internal class FaultStateDialog(val a: MainActivity) {
    val body: LinearLayout
    val rows: LinearLayout
    val clock: TextView
    val summary: TextView
    val items: MutableMap<Int?, Row> = LinkedHashMap<Int?, Row>()
    var order: String = ""

    init {
        body = LinearLayout(a)
        body.setOrientation(LinearLayout.VERTICAL)
        val overview = a.row()
        clock = a.text("", 28, MainActivity.WHITE)
        a.typography(clock, 28, false)
        clock.setFontFeatureSettings("tnum")
        overview.addView(clock, LinearLayout.LayoutParams(0, a.dp(48f), 1f))
        summary = a.text("", 12, MainActivity.LIME)
        summary.setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
        overview.addView(summary)
        body.addView(overview)
        val caption = a.text(a.getString(R.string.fault_state_hint), 12, MainActivity.MUTED)
        caption.setPadding(0, 0, 0, a.dp(20f))
        body.addView(caption)
        rows = LinearLayout(a)
        rows.setOrientation(LinearLayout.VERTICAL)
        body.addView(rows)
    }

    fun show(frame: EffectState.Frame): Dialog {
        update(frame)
        val dialog =
            SignalSheet.content(
                a,
                a.getString(R.string.ui_live_fault_current_chain),
                body,
                0,
                null,
                .54f,
            )
        a.reserveEffectEditor(this, SignalSheet.placeEditor(a, dialog, dialog.window!!.attributes.height))
        dialog.setOnDismissListener(
            OnDismissListener@{ v: DialogInterface? ->
                a.restoreEffectEditor(this)
                if (a.faultStatePanel == this) a.faultStatePanel = null
            }
        )
        return dialog
    }

    fun update(frame: EffectState.Frame) {
        clock.setText(String.format(Locale.US, "%.1f s", frame.time))
        summary.setText(a.getString(R.string.fault_state_count, frame.nodes.size))
        val next = frame.ids().contentToString()
        if (next != order) {
            order = next
            rows.removeAllViews()
            items.clear()
            var index = 1
            for (id in frame.ids()) {
                val row: Row = Row(id, index++)
                items.put(id, row)
                val p = LinearLayout.LayoutParams(-1, -2)
                p.bottomMargin = a.dp(10f)
                rows.addView(row.root, p)
            }
            if (items.isEmpty()) {
                val empty =
                    a.text(
                        a.getString(R.string.fault_chain_empty),
                        13,
                        MainActivity.MUTED,
                    )
                rows.addView(empty)
            }
        }
        for (row in items.values) row.update(0f)
        for (node in frame.nodes) {
            val row = items.get(node.id)
            if (row != null) row.update(node.event.envelope)
        }
    }

    internal inner class Row(id: Int, index: Int) {
        val root: LinearLayout
        val value: TextView
        val meter: Meter

        init {
            root = LinearLayout(a)
            root.setOrientation(LinearLayout.VERTICAL)
            root.setPadding(a.dp(14f), a.dp(12f), a.dp(14f), a.dp(12f))
            root.setBackground(a.bg(MainActivity.PANEL, 0))
            val line = a.row()
            val name =
                a.text(
                    String.format(Locale.US, "%02d  %s", index, Effects.label(id)),
                    13,
                    MainActivity.WHITE,
                )
            line.addView(name, LinearLayout.LayoutParams(0, a.dp(24f), 1f))
            value = a.text("", 12, MainActivity.LIME)
            value.setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
            line.addView(value, LinearLayout.LayoutParams(a.dp(48f), a.dp(24f)))
            root.addView(line)
            meter = Meter()
            val p = LinearLayout.LayoutParams(-1, a.dp(4f))
            p.topMargin = a.dp(10f)
            root.addView(meter, p)
        }

        fun update(amount: Float) {
            var amount = amount
            amount = max(0f, min(1f, amount))
            value.setText(Math.round(amount * 100).toString() + "%")
            meter.amount = amount
            meter.invalidate()
        }
    }

    internal inner class Meter : View(a) {
        val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var amount: Float = 0f

        init {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
        }

        override fun onDraw(c: Canvas) {
            val radius = getHeight() / 2f
            paint.setColor(-0xc6bfc5)
            c.drawRoundRect(
                0f,
                0f,
                getWidth().toFloat(),
                getHeight().toFloat(),
                radius,
                radius,
                paint,
            )
            paint.setColor(MainActivity.LIME)
            c.drawRoundRect(
                0f,
                0f,
                getWidth() * amount,
                getHeight().toFloat(),
                radius,
                radius,
                paint,
            )
        }
    }
}
