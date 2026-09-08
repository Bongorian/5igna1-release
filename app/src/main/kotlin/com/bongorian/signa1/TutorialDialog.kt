package com.bongorian.signa1

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/** Read-only guide: never changes the capture settings or editor drafts. */
internal class TutorialDialog(val activity: MainActivity, page: Int) {
    val dialog: Dialog
    var page: Int
    private var progress: TextView? = null
    private var heading: TextView? = null
    private var body: TextView? = null
    private var back: TextView? = null
    private var next: TextView? = null
    private var scroll: ScrollView? = null

    init {
        this.page = max(0, min(page, HEADINGS.size - 1))
        dialog = Dialog(activity)
    }

    fun show() {
        val a = activity
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(a)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setPadding(a.dp(20f), a.dp(20f), a.dp(20f), a.dp(16f))
        root.setBackground(a.bg(MainActivity.BG, MainActivity.PANEL))
        root.setTag("tutorial")
        val title = a.title(a.getString(R.string.tutorial_title))
        root.addView(title)
        progress = a.text("", 13, MainActivity.LIME)
        progress!!.setPadding(0, a.dp(14f), 0, a.dp(14f))
        progress!!.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE)
        root.addView(progress)
        scroll = ScrollView(a)
        scroll!!.setFillViewport(false)
        val content = LinearLayout(a)
        content.setOrientation(LinearLayout.VERTICAL)
        heading = a.text("", 24, MainActivity.WHITE)
        heading!!.setAccessibilityHeading(true)
        heading!!.setTag("tutorial-heading")
        content.addView(heading)
        body = a.text("", 16, MainActivity.WHITE)
        body!!.setPadding(0, a.dp(20f), 0, a.dp(24f))
        body!!.setLineSpacing(a.dp(6f).toFloat(), 1f)
        content.addView(body)
        scroll!!.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = a.row()
        back = a.button(a.getString(R.string.ui_back))
        back!!.setTag("tutorial-back")
        next = a.button("")
        next!!.setTag("tutorial-next")
        next!!.setTextColor(MainActivity.BG)
        next!!.setBackground(a.bg(MainActivity.LIME, 0))
        val button = LinearLayout.LayoutParams(0, -2, 1f)
        button.setMarginEnd(a.dp(8f))
        back!!.setMinHeight(a.dp(52f))
        next!!.setMinHeight(a.dp(52f))
        actions.addView(back, button)
        actions.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        val skip = a.button(a.getString(R.string.tutorial_skip))
        skip.setTag("tutorial-skip")
        skip.setTextColor(MainActivity.MUTED)
        skip.setMinHeight(a.dp(48f))
        root.addView(skip, LinearLayout.LayoutParams(-1, -2))
        back!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (page > 0) {
                    page--
                    render()
                }
            }
        )
        next!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (page == HEADINGS.size - 1) dialog.dismiss()
                else {
                    page++
                    render()
                }
            }
        )
        skip.setOnClickListener(OnClickListener@{ v: View? -> dialog.dismiss() })
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener(OnDismissListener@{ d: DialogInterface? -> a.tutorialClosed() })
        val window = dialog.window
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.CENTER)
        dialog.show()
        var height = a.window.decorView.height
        if (height <= 0) height = a.resources.displayMetrics.heightPixels
        window.setLayout(
            a.resources.displayMetrics.widthPixels - a.dp(16f),
            (height * .86f).toInt(),
        )
        render()
    }

    private fun render() {
        progress!!.setText(activity.getString(R.string.tutorial_progress, page + 1, HEADINGS.size))
        heading!!.setText(HEADINGS[page])
        body!!.setText(BODIES[page])
        back!!.setEnabled(page > 0)
        back!!.setAlpha(if (page > 0) 1f else .3f)
        next!!.setText(
            if (page == HEADINGS.size - 1) R.string.tutorial_done else R.string.tutorial_next
        )
        next!!.setContentDescription(next!!.text)
        scroll!!.scrollTo(0, 0)
    }

    fun dispose() {
        dialog.setOnDismissListener(null)
        dialog.dismiss()
    }

    companion object {
        const val SEEN: String = "tutorial.seen"
        private val HEADINGS =
            intArrayOf(
                R.string.tutorial_heading_0,
                R.string.tutorial_heading_1,
                R.string.tutorial_heading_2,
                R.string.tutorial_heading_3,
                R.string.tutorial_heading_4,
            )
        private val BODIES =
            intArrayOf(
                R.string.tutorial_body_0,
                R.string.tutorial_body_1,
                R.string.tutorial_body_2,
                R.string.tutorial_body_3,
                R.string.tutorial_body_4,
            )
    }
}
