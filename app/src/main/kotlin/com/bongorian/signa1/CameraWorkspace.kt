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
    private lateinit var foldedCapture: TextView

    fun buildFoldRail() {
        foldButton = a.button("›").apply {
            textSize = 22f
            setPadding(0, 0, 0, 0)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                a.workspaceCollapsed = !a.workspaceCollapsed
                requestLayout()
            }
        }
        foldRail.addView(foldButton, LayoutParams(-1, a.dp(40f)))
        foldRail.addView(android.widget.Space(a), LayoutParams(1, 0, 1f))
        foldedCapture = a.button("＋").apply {
            textSize = 24f
            setPadding(0, 0, 0, 0)
            setTextColor(MainActivity.BG)
            background = a.bg(MainActivity.LIME, 0)
            setOnClickListener { a.shoot() }
        }
        foldRail.addView(foldedCapture, LayoutParams(-1, a.dp(48f)))
        foldRail.addView(android.widget.Space(a), LayoutParams(1, 0, 1f))
        addView(foldRail, LayoutParams(a.dp(32f), -1))
    }

    private fun arrangePreviewTools() {
        val target = if (wide) previewTools else a.viewfinder
        for (view in listOf(a.status, a.lensScroll)) {
            if (view.parent === target) continue
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            if (wide) previewTools.addView(view, LayoutParams(-1, a.dp(if (view === a.status) 24f else ControlSize.COMPACT)))
            else a.viewfinder.addView(view, FrameLayout.LayoutParams(if (view === a.status) -1 else -2,
                a.dp(if (view === a.status) 24f else ControlSize.COMPACT), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = a.dp(if (view === a.status) 48f else 12f)
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
        val foldLabel = if (collapsed) "‹" else "›"
        if (foldButton.text.toString() != foldLabel) foldButton.text = foldLabel
        foldButton.contentDescription = a.getString(if (collapsed) R.string.workspace_expand else R.string.workspace_collapse)
        foldedCapture.visibility = if (collapsed) VISIBLE else GONE
        val captureLabel = if (a.recording) "■" else "＋"
        if (foldedCapture.text.toString() != captureLabel) foldedCapture.text = captureLabel
        foldedCapture.contentDescription = a.capture.contentDescription
        size(foldRail, LayoutParams(a.dp(if (collapsed) 48f else 28f), -1))
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
        val controlH = (content.measuredHeight + a.dp(72f + ControlSize.TOOLBAR)).coerceAtMost((h * .55f).toInt())
        size(previewColumn, if (wide) LayoutParams(0, -1, 1f) else LayoutParams(-1, 0, 1f))
        size(controlsColumn, if (wide) LayoutParams(controlW, -1).apply { leftMargin = a.dp(12f) }
            else LayoutParams(-1, controlH))
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
