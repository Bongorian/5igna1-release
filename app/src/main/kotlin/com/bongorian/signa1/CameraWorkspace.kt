package com.bongorian.signa1

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/** Measures against the current window, including split-screen and freeform resizing. */
internal class CameraWorkspace(val a: MainActivity) : LinearLayout(a) {
    lateinit var utilityBlock: LinearLayout
    lateinit var previewColumn: LinearLayout
    lateinit var controlsColumn: LinearLayout
    lateinit var controlsScroll: ScrollView
    private val previewTools = LinearLayout(a).apply { orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
    private val foldRail = LinearLayout(a).apply { orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
    private lateinit var foldButton: TextView
    lateinit var captureHome: FrameLayout
    private val foldedCaptureHost = FrameLayout(a)
    private val foldedClock = a.text("", 11, MainActivity.RED).apply { gravity = Gravity.CENTER; isSingleLine = true; tag = "workspace-recording-clock" }

    fun recordingClock(seconds: Long) {
        foldedClock.text = String.format(java.util.Locale.US, "● %02d:%02d", seconds / 60, seconds % 60)
    }

    fun buildFoldRail() {
        foldButton = a.button("").apply {
            tag = "workspace-toggle"
            textSize = 11f
            setPadding(a.dp(4f), 0, a.dp(4f), 0)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                a.workspaceCollapsed = !a.workspaceCollapsed
                requestLayout()
            }
        }
        foldRail.addView(foldButton, LayoutParams(-1, a.dp(48f)))
        foldRail.addView(foldedClock, LayoutParams(-1, a.dp(24f)))
        foldRail.addView(android.widget.Space(a), LayoutParams(1, 0, 1f))
        foldRail.addView(foldedCaptureHost, LayoutParams(-1, a.dp(88f)))
        foldRail.addView(android.widget.Space(a), LayoutParams(1, 0, 1f))
        addView(foldRail, LayoutParams(a.dp(32f), -1))
    }

    private fun arrangePreviewTools() {
        val target = if (wide) previewTools else a.viewfinder
        for (view in listOf(a.lensScroll)) {
            if (view.parent === target) continue
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            if (wide) previewTools.addView(view, LayoutParams(-1, a.dp(ControlSize.COMPACT)))
            else a.viewfinder.addView(view, FrameLayout.LayoutParams(-2,
                a.dp(ControlSize.COMPACT), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = a.dp(12f)
                leftMargin = a.dp(12f); rightMargin = a.dp(12f)
            })
        }
        if (wide && previewTools.parent == null) controlsColumn.addView(previewTools,
            controlsColumn.indexOfChild(controlsScroll) + 1, LayoutParams(-1, -2))
        previewTools.visibility = if (wide) VISIBLE else GONE
        a.viewfinder.findViewWithTag<View>("fx").visibility = if (wide) GONE else VISIBLE
    }

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
        arrangePreviewTools()
        val collapsed = wide && a.workspaceCollapsed
        controlsColumn.visibility = when {
            a.effectEditorSpace != null -> if (wide) INVISIBLE else GONE
            collapsed -> GONE
            else -> VISIBLE
        }
        foldRail.visibility = if (wide && a.effectEditorSpace == null) VISIBLE else GONE
        val foldLabel = ""
        DisclosureUi.bind(a, foldButton, !collapsed, true, foldLabel)
        foldButton.contentDescription = a.getString(if (collapsed) R.string.workspace_expand else R.string.workspace_collapse)
        foldButton.tooltipText = foldButton.contentDescription
        foldedCaptureHost.visibility = if (collapsed) VISIBLE else GONE
        foldedClock.visibility = if (collapsed && a.recording) VISIBLE else GONE
        val captureParent = if (collapsed) foldedCaptureHost else captureHome
        if (a.capture.parent !== captureParent) {
            (a.capture.parent as? android.view.ViewGroup)?.removeView(a.capture)
            captureParent.addView(a.capture, FrameLayout.LayoutParams(a.dp(80f), a.dp(80f), Gravity.CENTER))
        }
        size(foldRail, LayoutParams(a.dp(if (collapsed) 88f else 36f), -1))
        val utilityParent = if (wide) controlsColumn else previewColumn
        if (utilityBlock.parent !== utilityParent) {
            (utilityBlock.parent as? LinearLayout)?.removeView(utilityBlock)
            utilityParent.addView(utilityBlock, 0, LayoutParams(-1, -2))
        }
        orientation = if (wide) HORIZONTAL else VERTICAL
        val controlW = if (wide) (w * .32f).toInt().coerceIn(a.dp(264f), a.dp(320f)) else w
        val content = controlsScroll.getChildAt(0)
        content.measure(MeasureSpec.makeMeasureSpec(controlW, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val controlH = (content.measuredHeight + a.dp(88f + ControlSize.TOOLBAR)).coerceAtMost((h * .55f).toInt())
        size(previewColumn, if (wide) LayoutParams(0, -1, 1f) else LayoutParams(-1, 0, 1f))
        size(controlsColumn, if (wide) LayoutParams(controlW, -1).apply { leftMargin = a.dp(12f) }
            else LayoutParams(-1, controlH))
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
