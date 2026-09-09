package com.bongorian.signa1

import android.app.Dialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** A sandbox over the actual controls. Camera and location work are suspended by the host. */
internal class TutorialDialog(val activity: MainActivity, page: Int, private val host: Dialog? = null) {
    val dialog = Dialog(activity)
    var page = page.coerceIn(0, PAGE_COUNT - 1)
    private lateinit var root: FrameLayout
    private lateinit var card: LinearLayout
    private lateinit var heading: TextView
    private lateinit var body: TextView
    private lateinit var progress: TextView
    private lateinit var back: TextView
    private lateinit var next: TextView
    private lateinit var scroll: ScrollView
    private lateinit var demo: SignalDemo
    private lateinit var practice: TextView
    private lateinit var slider: SeekBar
    private lateinit var targetButton: TextView
    private lateinit var spotlight: Spotlight
    private var changed = false
    private var level = 55
    private var restoreHost = true
    val targetBounds = RectF()

    fun show() {
        val a = activity
        host?.hide()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        root = FrameLayout(a).apply { tag = "tutorial" }
        spotlight = Spotlight()
        root.addView(spotlight, FrameLayout.LayoutParams(-1, -1))
        targetButton = TextView(a).apply {
            tag = "tutorial-target"
            isClickable = true
            isFocusable = true
            setOnClickListener { practice() }
        }
        root.addView(targetButton)
        card = LinearLayout(a).apply {
            tag = "tutorial-card"
            orientation = LinearLayout.VERTICAL
            setPadding(a.dp(18f), a.dp(14f), a.dp(18f), a.dp(8f))
            background = a.bg(MainActivity.BG, MainActivity.LIME)
        }
        progress = a.text("", 12, MainActivity.LIME).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        card.addView(progress)
        scroll = ScrollView(a)
        val content = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        heading = a.text("", 23, MainActivity.WHITE).apply {
            tag = "tutorial-heading"
            isAccessibilityHeading = true
            setPadding(0, a.dp(8f), 0, a.dp(8f))
        }
        content.addView(heading)
        body = a.text("", 15, MainActivity.WHITE).apply {
            setLineSpacing(a.dp(3f).toFloat(), 1f)
        }
        content.addView(body)
        demo = SignalDemo().apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        content.addView(demo, LinearLayout.LayoutParams(-1, a.dp(74f)).apply { topMargin = a.dp(12f) })
        slider = SeekBar(a).apply {
            tag = "tutorial-level"
            max = 100
            progress = level
            contentDescription = a.getString(R.string.ui_strength)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, user: Boolean) {
                    level = value
                    demo.invalidate()
                    if (user) updatePractice()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        content.addView(slider, LinearLayout.LayoutParams(-1, a.dp(48f)))
        practice = a.button("").apply {
            tag = "tutorial-practice"
            minHeight = a.dp(48f)
            setTextColor(MainActivity.LIME)
            setOnClickListener { practice() }
        }
        content.addView(practice, LinearLayout.LayoutParams(-1, -2).apply { topMargin = a.dp(12f) })
        scroll.addView(content)
        card.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = a.row()
        back = a.button(a.getString(R.string.ui_back)).apply {
            tag = "tutorial-back"
            minHeight = a.dp(48f)
            setOnClickListener { if (page > 0) { page--; render() } }
        }
        next = a.button("").apply {
            tag = "tutorial-next"
            minHeight = a.dp(48f)
            setTextColor(MainActivity.BG)
            background = a.bg(MainActivity.LIME, 0)
            setOnClickListener {
                if (page == PAGE_COUNT - 1) dialog.dismiss() else { page++; render() }
            }
        }
        actions.addView(back, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(a.button(a.getString(R.string.tutorial_skip)).apply {
            tag = "tutorial-skip"
            minHeight = a.dp(48f)
            setTextColor(MainActivity.MUTED)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(a.dp(76f), -2))
        actions.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(actions)
        ButtonSpacing.apply(a, card)
        root.addView(card)
        root.setOnApplyWindowInsetsListener { _, insets -> root.post { position() }; insets }
        root.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (r - l != or - ol || b - t != ob - ot) position()
        }
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            if (restoreHost && !a.isFinishing && !a.isDestroyed) host?.show()
            a.tutorialClosed()
        }
        dialog.window!!.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDecorFitsSystemWindows(false)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            attributes = attributes.apply {
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            setGravity(Gravity.TOP or Gravity.LEFT)
        }
        dialog.show()
        dialog.window!!.setLayout(-1, -1)
        render()
    }

    private fun target(): View? = when (page) {
        1 -> activity.cameraRoot.findViewWithTag("guide-add")
        2 -> activity.strength
        3 -> activity.faultSwitch
        4 -> activity.formatButton
        5 -> activity.capture
        6 -> activity.galleryButton
        7 -> activity.cameraRoot.findViewWithTag("guide-settings")
        else -> null
    }

    private fun position() {
        if (root.width == 0 || root.height == 0) return
        val a = activity
        val margin = a.dp(12f)
        val safe = root.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.systemBars() or
            android.view.WindowInsets.Type.displayCutout()) ?: android.graphics.Insets.NONE
        val left = safe.left + margin
        val right = root.width - safe.right - margin
        val top = safe.top + margin
        val bottom = root.height - safe.bottom - margin
        targetBounds.setEmpty()
        target()?.let { view ->
            val rect = Rect()
            val origin = IntArray(2)
            root.getLocationOnScreen(origin)
            if (view.getGlobalVisibleRect(rect)) {
                rect.offset(-origin[0], -origin[1])
                targetBounds.set(rect)
                targetBounds.inset(-a.dp(5f).toFloat(), -a.dp(5f).toFloat())
            }
        }
        targetButton.visibility = if (targetBounds.isEmpty) View.GONE else View.VISIBLE
        targetButton.contentDescription = a.getString(R.string.guide_try) + ": " + heading.text
        targetButton.layoutParams = FrameLayout.LayoutParams(
            targetBounds.width().toInt().coerceAtLeast(1), targetBounds.height().toInt().coerceAtLeast(1)
        ).apply { leftMargin = targetBounds.left.toInt(); topMargin = targetBounds.top.toInt() }
        if (root.width > root.height && !targetBounds.isEmpty) {
            val leftRoom = targetBounds.left.toInt() - left - margin
            val rightRoom = right - targetBounds.right.toInt() - margin
            val width = minOf(a.dp(480f), maxOf(leftRoom, rightRoom))
            if (width >= a.dp(260f)) {
                val height = minOf(a.dp(470f), bottom - top)
                card.layoutParams = FrameLayout.LayoutParams(width, height).apply {
                    leftMargin = if (leftRoom >= rightRoom) left else right - width
                    topMargin = top + (bottom - top - height) / 2
                }
                spotlight.invalidate()
                return
            }
        }
        val above = !targetBounds.isEmpty && targetBounds.centerY() > (top + bottom) / 2f
        val available = if (targetBounds.isEmpty) bottom - top
            else if (above) targetBounds.top.toInt() - top - margin
            else bottom - targetBounds.bottom.toInt() - margin
        val height = minOf(a.dp(470f), available.coerceAtLeast(1))
        card.layoutParams = FrameLayout.LayoutParams(right - left, height).apply {
            leftMargin = left
            topMargin = if (targetBounds.isEmpty) top + (bottom - top - height) / 2
                else if (above) top else bottom - height
        }
        spotlight.invalidate()
    }

    private fun render() {
        changed = false
        progress.text = "5IGNA1 / " + activity.getString(R.string.tutorial_progress, page + 1, PAGE_COUNT)
        heading.setText(HEADINGS[page])
        body.setText(BODIES[page])
        back.isEnabled = page > 0
        back.alpha = if (page > 0) 1f else .3f
        next.setText(if (page == PAGE_COUNT - 1) R.string.tutorial_done else R.string.tutorial_next)
        demo.visibility = if (page == 4) View.GONE else View.VISIBLE
        slider.visibility = if (page == 2) View.VISIBLE else View.GONE
        updatePractice()
        demo.invalidate()
        scroll.scrollTo(0, 0)
        target()?.let { view ->
            view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
        }
        root.post { position() }
    }

    private fun practice() {
        changed = !changed
        if (page == 2) slider.progress = if (changed) 90 else 20
        demo.invalidate()
        updatePractice()
    }

    private fun updatePractice() {
        practice.text = when {
            page == 4 -> activity.getString(R.string.guide_format_sample, if (changed) "RAW" else "JPG")
            page == 2 -> activity.getString(R.string.guide_level, level)
            changed -> activity.getString(R.string.guide_tried)
            else -> activity.getString(R.string.guide_try)
        }
    }

    fun dispose() {
        restoreHost = false
        dialog.setOnDismissListener(null)
        dialog.dismiss()
        host?.dismiss()
    }

    private inner class Spotlight : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        override fun onDraw(canvas: Canvas) {
            path.reset()
            path.fillType = Path.FillType.EVEN_ODD
            path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            if (!targetBounds.isEmpty) path.addRoundRect(targetBounds, 12f, 12f, Path.Direction.CW)
            paint.style = Paint.Style.FILL
            paint.color = 0xDB060807.toInt()
            canvas.drawPath(path, paint)
            if (!targetBounds.isEmpty) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = activity.dp(2f).toFloat()
                paint.color = MainActivity.LIME
                canvas.drawRoundRect(targetBounds, 12f, 12f, paint)
            }
        }
    }

    /** Deliberately cheap, deterministic illustration; no camera, timer or background animation. */
    private inner class SignalDemo : View(activity) {
        private val paint = Paint()
        private val colors = intArrayOf(0xffcadf63.toInt(), 0xff6fe0bd.toInt(), 0xff4294ad.toInt(), 0xffee7161.toInt(), 0xffb777c4.toInt(), 0xffd5d6ca.toInt())
        override fun onDraw(canvas: Canvas) {
            val amount = if (page == 2) level / 100f else if (changed) .85f else .15f
            for (row in 0 until 16) for (col in 0 until 6) {
                val shift = if (row % 3 == 0) amount * width * .22f else 0f
                paint.color = colors[(col + if (changed && page == 3) row else 0) % colors.size]
                val x = col * width / 6f + shift
                canvas.drawRect(x, row * height / 16f, x + width / 6f, (row + 1) * height / 16f - 1, paint)
            }
            paint.color = 0xcc080a09.toInt()
            canvas.drawRect(0f, height - activity.dp(23f).toFloat(), width.toFloat(), height.toFloat(), paint)
            paint.color = MainActivity.WHITE
            paint.textSize = activity.dp(10f).toFloat()
            canvas.drawText(activity.getString(R.string.guide_sandbox), activity.dp(8f).toFloat(), height - activity.dp(7f).toFloat(), paint)
        }
    }

    companion object {
        const val SEEN = "tutorial.seen"
        const val PAGE_COUNT = 8
        private val HEADINGS = intArrayOf(R.string.guide_heading_0, R.string.guide_heading_1, R.string.guide_heading_2, R.string.guide_heading_3, R.string.guide_heading_4, R.string.guide_heading_5, R.string.guide_heading_6, R.string.guide_heading_7)
        private val BODIES = intArrayOf(R.string.guide_body_0, R.string.guide_body_1, R.string.guide_body_2, R.string.guide_body_3, R.string.guide_body_4, R.string.guide_body_5, R.string.guide_body_6, R.string.guide_body_7)
    }
}
