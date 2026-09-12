package com.bongorian.signa1

import android.app.Dialog
import android.graphics.Color
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
        root.addView(actions, LinearLayout.LayoutParams(-1, a.dp(ControlSize.PRIMARY + 12f)))
        val cancel = a.button(a.getString(R.string.ui_back))
        cancel.setTextColor(MainActivity.MUTED)
        cancel.setBackgroundColor(Color.TRANSPARENT)
        actions.addView(cancel, LinearLayout.LayoutParams(0, a.dp(ControlSize.TOOLBAR), 1f))
        cancel.setOnClickListener(OnClickListener@{ v: View? -> dialog.dismiss() })
        val done = a.button(a.getString(R.string.ui_apply))
        done.setTextColor(MainActivity.BG)
        done.setBackground(a.bg(MainActivity.LIME, 0))
        actions.addView(done, LinearLayout.LayoutParams(0, a.dp(ControlSize.PRIMARY), 1f))
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
        header.addView(heading, LinearLayout.LayoutParams(0, a.dp(ControlSize.STANDARD), 1f))
        val close = a.button("×")
        close.setTextSize(24f)
        close.setContentDescription(a.getString(R.string.ui_close))
        header.addView(close, LinearLayout.LayoutParams(a.dp(ControlSize.TOOLBAR), a.dp(ControlSize.TOOLBAR)))
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
            val bp = LinearLayout.LayoutParams(-1, a.dp(ControlSize.TOOLBAR))
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
            row.setMinHeight(a.dp(ControlSize.STANDARD))
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
        val p = LinearLayout.LayoutParams(-1, a.dp(ControlSize.STANDARD))
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

    fun placeEditor(a: MainActivity, dialog: Dialog, height: Int): Int {
        val root = a.cameraRoot
        val usable = root.height - root.paddingTop - root.paddingBottom
        val bounds = a.windowManager.currentWindowMetrics.bounds
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        val window = dialog.window!!
        val width: Int
        val editorHeight: Int
        val x: Int
        val y: Int
        if (root.wide) {
            val controls = IntArray(2)
            root.controlsColumn.getLocationOnScreen(controls)
            width = root.controlsColumn.width
            editorHeight = usable
            x = controls[0] - bounds.left
            y = origin[1] + root.paddingTop - bounds.top
        } else {
            val innerWidth = root.width - root.paddingLeft - root.paddingRight
            width = min(innerWidth, a.dp(720f))
            editorHeight = min(height, usable)
            x = origin[0] + root.paddingLeft + (innerWidth-width)/2 - bounds.left
            y = origin[1] + root.height - root.paddingBottom - editorHeight - bounds.top
        }
        // Coordinates already include the app's safe insets. Do not apply them a second time.
        window.setDecorFitsSystemWindows(false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.attributes = window.attributes.apply {
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            gravity = Gravity.TOP or Gravity.LEFT
            this.x = x; this.y = y
            this.width = width; this.height = editorHeight
        }
        return editorHeight
    }

    fun resize(a: MainActivity, dialog: Dialog, fraction: Float) {
        val window = dialog.window
        if (window != null) {
            val h = a.cameraRoot.height - a.cameraRoot.paddingTop - a.cameraRoot.paddingBottom
            window.setLayout(
                min(a.cameraRoot.width - a.dp(16f), a.dp(640f)),
                (h * fraction).toInt(),
            )
        }
    }
}
