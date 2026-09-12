package com.bongorian.signa1

import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

internal fun MainActivity.buildProStrip(): LinearLayout {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
    proReadingLabel = text("", 12, MainActivity.MUTED).apply {
        setPadding(dp(4f), dp(8f), dp(4f), dp(8f))
        setSingleLine(true)
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    root.addView(proReadingLabel, LinearLayout.LayoutParams(-1, -2))
    val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
    val fields = row()
    scroll.addView(fields)
    root.addView(scroll, LinearLayout.LayoutParams(-1, dp(ControlSize.PRIMARY)))
    listOf("exposure" to R.string.pro_exposure, "wb" to R.string.pro_white_balance,
        "focus" to R.string.pro_focus, "lens" to R.string.pro_lens).forEach { (key, title) ->
        val field = button(getString(title)).apply {
            tag = "pro-field:" + key
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            setOnClickListener { showProCamera(key) }
        }
        fields.addView(field, LinearLayout.LayoutParams(dp(98f), -1).apply { rightMargin = dp(8f) })
    }
    return root
}

internal fun MainActivity.renderProCamera() {
    val allowed = !tapMode && externalCapture == null
    proModeButton.visibility = if (allowed) View.VISIBLE else View.GONE
    proModeButton.text = if (engine.proMode) "PRO" else "AUTO"
    proModeButton.setTextColor(if (engine.proMode) MainActivity.LIME else MainActivity.WHITE)
    proModeButton.contentDescription = getString(R.string.pro_mode) + " · " + proModeButton.text
    proModeButton.isEnabled = allowed && !recording && !engine.photoBusy
    proStrip.visibility = if (allowed && engine.proMode) View.VISIBLE else View.GONE
    if (!allowed || !engine.proMode) return
    val context = engine.proContext
    val caps = engine.proControls?.capabilities
    val value = engine.proEffective
    val reading = engine.proReading
    val actual = if (context.highSpeed) getString(R.string.pro_high_speed)
        else if (caps == null) getString(R.string.pro_waiting)
        else getString(R.string.pro_measured, ProCameraScale.shutter(reading.exposureNs), reading.iso?.toString() ?: "—")
    if (proReadingLabel.text.toString() != actual) proReadingLabel.text = actual
    fun field(key: String, text: String, supported: Boolean) {
        val view = proStrip.findViewWithTag<TextView>("pro-field:" + key)
        if (view.text.toString() != text) view.text = text
        view.visibility = if (supported) View.VISIBLE else View.GONE
        view.isEnabled = !recording && !engine.photoBusy && !context.highSpeed
        view.alpha = if (view.isEnabled) 1f else .45f
    }
    field("exposure", getString(R.string.pro_exposure) + "\n" +
        if (value.manualExposure) ProCameraScale.shutter(value.exposureNs) + " · " + value.iso
        else "AUTO" + if (value.aeLock) " · L" else "",
        caps?.let { it.exposureRange(context) != null || it.ev != null || it.aeLock } == true)
    field("wb", "WB\n" + proWhiteBalanceLabel(value.whiteBalance), caps?.whiteBalance?.isNotEmpty() == true || caps?.awbLock == true)
    field("focus", getString(R.string.pro_focus) + "\n" + if (value.manualFocus) "MF" else "AF", (caps?.focusMax ?: 0f) > 0f)
    field("lens", getString(R.string.pro_lens) + "\n" + (value.aperture?.let { "ƒ/$it" } ?: "AUTO"),
        caps?.let { it.apertures.isNotEmpty() || it.filterDensities.isNotEmpty() || it.opticalStabilization || it.antiBanding.size > 1 } == true)
}

internal fun MainActivity.showProCamera(group: String) {
    if (recording || engine.photoBusy || tapMode || externalCapture != null || !engine.proMode || engine.proContext.highSpeed) return
    cancelEffectPreview()
    liveChainDialog?.dismiss()
    proCameraDialog?.dialog?.dismiss()
    proCameraDialog = ProCameraDialog(this, group).also { it.show() }
}

internal fun MainActivity.proWhiteBalanceLabel(mode: Int): String = getString(when (mode) {
    2 -> R.string.pro_wb_incandescent
    3 -> R.string.pro_wb_fluorescent
    4 -> R.string.pro_wb_warm
    5 -> R.string.pro_wb_daylight
    6 -> R.string.pro_wb_cloudy
    7 -> R.string.pro_wb_twilight
    8 -> R.string.pro_wb_shade
    else -> R.string.pro_auto
})
