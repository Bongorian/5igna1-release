package com.bongorian.signa1

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.max
import kotlin.math.min

/** Shared dark, rounded editor surface for capture and effect settings. */
internal object SignalSheet {
    @JvmOverloads
    fun show(
        a: MainActivity,
        title: String?,
        subtitle: String?,
        content: View?,
        apply: Runnable,
        fraction: Float = .87f,
        toolbar: View? = null,
    ): Dialog {
        val dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(a)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setPadding(a.dp(18f), a.dp(10f), a.dp(18f), a.dp(12f))
        root.setBackground(a.bg(MainActivity.BG, MainActivity.PANEL))
        val handle = View(a)
        handle.setBackground(a.detailBg(MainActivity.MUTED, 0))
        val hp = LinearLayout.LayoutParams(a.dp(30f), a.dp(3f))
        hp.gravity = Gravity.CENTER_HORIZONTAL
        hp.bottomMargin = a.dp(20f)
        root.addView(handle, hp)
        val heading = a.title(title)
        heading.setMinHeight(a.dp(34f))
        root.addView(heading, LinearLayout.LayoutParams(-1, -2))
        val note = a.text(subtitle, MainActivity.TEXT_BODY, MainActivity.MUTED)
        note.setPadding(0, 0, 0, a.dp(12f))
        root.addView(note, LinearLayout.LayoutParams(-1, -2))
        if (toolbar != null) root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(a)
        scroll.setFillViewport(false)
        scroll.setVerticalScrollBarEnabled(false)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = a.row()
        actions.setPadding(0, a.dp(12f), 0, 0)
        root.addView(actions, LinearLayout.LayoutParams(-1, a.dp(60f)))
        val cancel = a.button(a.getString(R.string.ui_back))
        cancel.setTextColor(MainActivity.MUTED)
        cancel.setBackgroundColor(Color.TRANSPARENT)
        actions.addView(cancel, LinearLayout.LayoutParams(0, -1, 1f))
        cancel.setOnClickListener(OnClickListener@{ v: View? -> dialog.dismiss() })
        val done = a.button(a.getString(R.string.ui_apply))
        done.setTextColor(MainActivity.BG)
        done.setBackground(a.bg(MainActivity.LIME, 0))
        actions.addView(done, LinearLayout.LayoutParams(0, -1, 1f))
        done.setOnClickListener(
            OnClickListener@{ v: View? ->
                apply.run()
                dialog.dismiss()
            }
        )
        ButtonSpacing.apply(a, root)
        dialog.setContentView(root)
        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val lp = window.attributes
            lp.dimAmount = .65f
            window.setAttributes(lp)
            window.setGravity(Gravity.BOTTOM)
        }
        dialog.show()
        resize(a, dialog, fraction)
        return dialog
    }

    fun content(
        a: MainActivity,
        title: String?,
        content: View?,
        secondary: Int,
        action: Runnable?,
        fraction: Float,
    ): Dialog {
        val dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(a)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setPadding(a.dp(18f), a.dp(14f), a.dp(18f), a.dp(14f))
        root.setBackground(a.bg(MainActivity.BG, MainActivity.PANEL))
        val header = a.row()
        val heading = a.title(title)
        header.addView(heading, LinearLayout.LayoutParams(0, a.dp(48f), 1f))
        val close = a.button("×")
        close.setTextSize(24f)
        close.setContentDescription(a.getString(R.string.ui_close))
        header.addView(close, LinearLayout.LayoutParams(a.dp(44f), a.dp(44f)))
        close.setOnClickListener(OnClickListener@{ v: View? -> dialog.dismiss() })
        root.addView(header)
        val scroll = ScrollView(a)
        scroll.setVerticalScrollBarEnabled(false)
        scroll.addView(content)
        val sp = LinearLayout.LayoutParams(-1, 0, 1f)
        sp.topMargin = a.dp(12f)
        root.addView(scroll, sp)
        if (secondary != 0) {
            val button = a.button(a.getString(secondary))
            button.setTextColor(MainActivity.LIME)
            val bp = LinearLayout.LayoutParams(-1, a.dp(48f))
            bp.topMargin = a.dp(12f)
            root.addView(button, bp)
            button.setOnClickListener(
                OnClickListener@{ v: View? ->
                    dialog.dismiss()
                    if (action != null) action.run()
                }
            )
        }
        ButtonSpacing.apply(a, root)
        dialog.setContentView(root)
        val window = dialog.window
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.BOTTOM)
        val wp = window.attributes
        wp.dimAmount = .3f
        window.setAttributes(wp)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        resize(a, dialog, fraction)
        return dialog
    }

    fun message(
        a: MainActivity,
        title: String?,
        message: String?,
        actionLabel: Int,
        action: Runnable?,
    ): Dialog {
        val text = a.text(message, 14, MainActivity.WHITE)
        text.setTag("message")
        text.setLineSpacing(a.dp(5f).toFloat(), 1f)
        return content(a, title, text, actionLabel, action, .55f)
    }

    fun pick(
        a: MainActivity,
        title: String?,
        labels: Array<String>,
        selected: Int,
        chosen: (Int) -> Unit,
    ): Dialog {
        val list = LinearLayout(a)
        list.setOrientation(LinearLayout.VERTICAL)
        lateinit var dialog: Dialog
        for (n in labels.indices) {
            val index = n
            val row =
                a.text(
                    labels[n] + (if (n == selected) "   ✓" else ""),
                    14,
                    if (n == selected) MainActivity.LIME else MainActivity.WHITE,
                )
            row.setTag("choice-" + n)
            row.setPadding(a.dp(16f), a.dp(14f), a.dp(16f), a.dp(14f))
            row.setMinHeight(a.dp(56f))
            row.setBackground(
                a.bg(
                    MainActivity.PANEL,
                    if (n == selected) 0x665F7940 else 0,
                )
            )
            row.setSelected(n == selected)
            val p = LinearLayout.LayoutParams(-1, -2)
            p.bottomMargin = a.dp(8f)
            list.addView(row, p)
            row.setOnClickListener(
                OnClickListener@{ v: View? ->
                    dialog.dismiss()
                    chosen(index)
                }
            )
        }
        val height = a.window.decorView.height
        val fraction = min(.8f, a.dp((110 + labels.size * 68).toFloat()) / max(height, 1).toFloat())
        dialog = content(a, title, list, 0, null, fraction)
        return dialog
    }

    fun anchoredPick(
        a: MainActivity,
        anchor: View,
        title: String?,
        labels: Array<String>,
        selected: Int,
        chosen: (Int) -> Unit,
    ): Dialog {
        val dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val list = LinearLayout(a)
        list.setOrientation(LinearLayout.VERTICAL)
        list.setPadding(a.dp(8f), a.dp(8f), a.dp(8f), a.dp(8f))
        list.setBackground(a.bg(MainActivity.BG, MainActivity.PANEL))
        val heading = a.text(title, 11, MainActivity.MUTED)
        heading.setPadding(a.dp(12f), a.dp(8f), a.dp(12f), a.dp(12f))
        list.addView(heading)
        for (n in labels.indices) {
            val index = n
            val option = a.button(labels[n] + (if (n == selected) "   ✓" else ""))
            option.setTag("choice-" + n)
            option.setSelected(n == selected)
            option.setGravity(Gravity.CENTER_VERTICAL)
            option.setTextColor(if (n == selected) MainActivity.LIME else MainActivity.WHITE)
            option.setBackground(
                if (n == selected)
                    a.bg(
                        MainActivity.PANEL,
                        0,
                    )
                else ColorDrawable(Color.TRANSPARENT)
            )
            val p = LinearLayout.LayoutParams(-1, a.dp(48f))
            if (n > 0) p.topMargin = a.dp(4f)
            list.addView(option, p)
            option.setOnClickListener(
                OnClickListener@{ v: View? ->
                    dialog.dismiss()
                    chosen(index)
                }
            )
        }
        ButtonSpacing.apply(a, list)
        dialog.setContentView(list)
        val window = dialog.window
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.TOP or Gravity.LEFT)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        var lp = window.attributes
        lp.dimAmount = .18f
        window.setAttributes(lp)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        val position = IntArray(2)
        anchor.getLocationOnScreen(position)
        val visible = Rect()
        a.window.decorView.getWindowVisibleDisplayFrame(visible)
        lp = window.attributes
        lp.width = a.dp(212f)
        lp.height = -2
        lp.x =
            max(
                a.dp(8f),
                min(
                    position[0],
                    a.resources.displayMetrics.widthPixels - lp.width - a.dp(8f),
                ),
            )
        lp.y = max(0, position[1] + anchor.height + a.dp(6f) - visible.top)
        window.setAttributes(lp)
        return dialog
    }

    fun number(
        a: MainActivity,
        title: String?,
        range: String?,
        initial: String?,
        integer: Boolean,
        changed: (String) -> Boolean,
    ): Dialog {
        val body = LinearLayout(a)
        body.setOrientation(LinearLayout.VERTICAL)
        val note = a.text(range, 12, MainActivity.MUTED)
        body.addView(note, LinearLayout.LayoutParams(-1, -2))
        val input = EditText(a)
        input.setTag("number-input")
        input.setSingleLine(true)
        input.setTextColor(MainActivity.WHITE)
        a.typography(input, MainActivity.TEXT_BODY, false)
        input.setPadding(a.dp(14f), 0, a.dp(14f), 0)
        input.setBackground(a.bg(MainActivity.PANEL, 0))
        input.setInputType(
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_SIGNED or
                (if (integer) 0 else InputType.TYPE_NUMBER_FLAG_DECIMAL)
        )
        input.setText(initial)
        body.addView(input, LinearLayout.LayoutParams(-1, a.dp(52f)))
        val error = a.text(a.getString(R.string.ui_invalid_number), 12, MainActivity.RED)
        error.setVisibility(View.GONE)
        body.addView(error, LinearLayout.LayoutParams(-1, a.dp(30f)))
        val apply = a.button(a.getString(R.string.ui_apply))
        apply.setTag("number-apply")
        apply.setTextColor(MainActivity.BG)
        apply.setBackground(a.bg(MainActivity.LIME, 0))
        val p = LinearLayout.LayoutParams(-1, a.dp(48f))
        p.topMargin = a.dp(12f)
        body.addView(apply, p)
        val dialog = content(a, title, body, 0, null, .4f)
        val liveEditor = a.liveEditor
        if (liveEditor != null) liveEditor.child = dialog
        else (a.effectEditorOwner as? EffectDialog)?.auxiliary = dialog
        apply.setOnClickListener(
            OnClickListener@{ v: View? ->
                try {
                    if (changed(input.text.toString().trim { it <= ' ' })) {
                        dialog.dismiss()
                        return@OnClickListener
                    }
                } catch (ignored: IllegalArgumentException) {}
                error.setVisibility(View.VISIBLE)
            }
        )
        return dialog
    }

    fun resize(a: MainActivity, dialog: Dialog, fraction: Float) {
        val window = dialog.window
        if (window != null) {
            val h = a.window.decorView.height
            window.setLayout(
                a.resources.displayMetrics.widthPixels - a.dp(16f),
                (h * fraction).toInt(),
            )
        }
    }
}
