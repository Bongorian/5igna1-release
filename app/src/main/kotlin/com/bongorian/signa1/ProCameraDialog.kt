package com.bongorian.signa1

import android.app.Dialog
import android.content.res.ColorStateList
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale

/** Immediate camera adjustments in a focused sheet; FAULT drafts are never changed here. */
internal class ProCameraDialog(private val a: MainActivity, private val group: String) {
    private val key = a.engine.cameraSelection
    private val caps = a.engine.proControls?.capabilities ?: ProCameraCapabilities()
    private val context = a.engine.proContext
    private var desired = a.engine.proDesired
    private val current get() = desired.resolve(caps, context)
    private val body = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
    private var measured: TextView? = null
    var dialog: Dialog? = null
        private set

    fun show() {
        render()
        dialog = SignalSheet.content(a, "PRO · " + a.getString(when (group) {
            "wb" -> R.string.pro_white_balance
            "focus" -> R.string.pro_focus
            "lens" -> R.string.pro_lens
            else -> R.string.pro_exposure
        }), body, R.string.pro_reset_camera, Runnable {
            a.engine.setProState(key, ProCameraState())
        }, .57f)
        dialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        a.reserveEffectEditor(this, SignalSheet.placeEditor(a, dialog!!, dialog!!.window!!.attributes.height))
        dialog!!.setOnDismissListener {
            a.restoreEffectEditor(this)
            if (a.proCameraDialog === this) a.proCameraDialog = null
        }
    }

    fun update() {
        if (key != a.engine.cameraSelection || !a.engine.proMode || a.tapMode) { dialog?.dismiss(); return }
        val r = a.engine.proReading
        val text = when (group) {
            "wb" -> a.getString(R.string.pro_measured_value, r.whiteBalance?.let { a.proWhiteBalanceLabel(it) } ?: "—") + if (r.awbLocked == true) " · WB-L" else ""
            "focus" -> a.getString(R.string.pro_measured_value, r.focus?.let {
                if (it == 0f) "∞" else if (caps.focusCalibrated) String.format(Locale.US, "≈ %.2f m", 1 / it)
                else a.getString(R.string.pro_focus_position, (it / caps.focusMax.coerceAtLeast(.001f) * 100).toInt())
            } ?: "—")
            "lens" -> a.getString(R.string.pro_measured_value, (r.aperture?.let { "ƒ/$it" } ?: "—") +
                (r.opticalStabilization?.let { " · OIS " + if (it == 1) a.getString(R.string.pro_on) else a.getString(R.string.pro_off) } ?: ""))
            else -> a.getString(R.string.pro_measured, ProCameraScale.shutter(r.exposureNs), r.iso?.toString() ?: "—") +
                (if (r.aeLocked == true) " · AE-L" else "")
        }
        if (measured?.text?.toString() != text) measured?.text = text
    }

    private fun commit(value: ProCameraState, rebuild: Boolean = false) {
        if (key != a.engine.cameraSelection || a.recording || a.engine.photoBusy) return
        desired = value
        a.engine.setProState(key, value)
        if (rebuild) render()
    }
    private fun note(value: String): TextView = a.text(value, 12, MainActivity.MUTED).also {
        it.setPadding(0, a.dp(8f), 0, a.dp(12f))
        body.addView(it, LinearLayout.LayoutParams(-1, -2))
    }
    private fun choices(title: Int, entries: List<Pair<Int, String>>, selected: Int, choose: (Int) -> Unit) {
        if (entries.isEmpty()) return
        body.addView(a.text(a.getString(title), 12, MainActivity.WHITE))
        val scroll = HorizontalScrollView(a).apply { isHorizontalScrollBarEnabled = false }
        val row = a.row()
        entries.forEach { (id, label) ->
            val button = a.button(label).apply {
                setTextColor(if (id == selected) MainActivity.BG else MainActivity.WHITE)
                background = a.bg(if (id == selected) MainActivity.LIME else MainActivity.PANEL, 0)
                isSelected = id == selected
                contentDescription = a.getString(title) + " · " + label
                setOnClickListener { choose(id) }
            }
            row.addView(button, LinearLayout.LayoutParams(-2, a.dp(ControlSize.COMPACT)).apply { rightMargin = a.dp(8f) })
        }
        scroll.addView(row)
        body.addView(scroll, LinearLayout.LayoutParams(-1, a.dp(ControlSize.TOOLBAR)).apply { topMargin = a.dp(8f); bottomMargin = a.dp(8f) })
    }
    private fun toggle(title: Int, checked: Boolean, on: Int = R.string.pro_on, off: Int = R.string.pro_off, choose: (Boolean) -> Unit) {
        choices(title, listOf(0 to a.getString(off), 1 to a.getString(on)), if (checked) 1 else 0) { choose(it == 1) }
    }
    private fun slider(title: Int, progress: Int, value: (Int) -> String, change: (Int) -> Unit) {
        val label = a.text(a.getString(title) + "   " + value(progress), 18, MainActivity.WHITE)
        body.addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = a.dp(8f) })
        val bar = SeekBar(a).apply {
            max = 1000
            this.progress = progress
            progressTintList = ColorStateList.valueOf(MainActivity.LIME)
            thumbTintList = ColorStateList.valueOf(MainActivity.LIME)
            contentDescription = label.text
        }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar?, position: Int, fromUser: Boolean) {
                label.text = a.getString(title) + "   " + value(position)
                bar.contentDescription = label.text
                if (fromUser) change(position)
            }
            override fun onStartTrackingTouch(view: SeekBar?) {}
            override fun onStopTrackingTouch(view: SeekBar?) {}
        })
        body.addView(bar, LinearLayout.LayoutParams(-1, a.dp(48f)))
    }
    private fun render() {
        body.removeAllViews()
        note(a.getString(R.string.pro_immediate))
        measured = note("")
        when (group) {
            "exposure" -> exposure()
            "wb" -> whiteBalance()
            "focus" -> focus()
            "lens" -> lens()
        }
        update()
    }
    private fun exposure() {
        val range = caps.exposureRange(context)
        if (range != null) toggle(R.string.pro_exposure_control, current.manualExposure, R.string.pro_manual, R.string.pro_auto) { manual ->
            val reading = a.engine.proReading
            commit(desired.copy(manualExposure = manual,
                exposureNs = if (manual && !desired.manualExposure) reading.exposureNs ?: desired.exposureNs else desired.exposureNs,
                iso = if (manual && !desired.manualExposure) reading.iso ?: desired.iso else desired.iso), true)
        }
        if (current.manualExposure && range != null) {
            slider(R.string.pro_shutter, ProCameraScale.progress(range, current.exposureNs),
                { ProCameraScale.shutter(ProCameraScale.value(range, it)) }, { commit(desired.copy(exposureNs = ProCameraScale.value(range, it))) })
            val isoRange = caps.iso!!.let { it.first.toLong()..it.last.toLong() }
            slider(R.string.pro_iso, ProCameraScale.progress(isoRange, current.iso.toLong()),
                { ProCameraScale.value(isoRange, it).toString() }, { commit(desired.copy(iso = ProCameraScale.value(isoRange, it).toInt())) })
            note(a.getString(if (context.video) R.string.pro_shutter_video else R.string.pro_shutter_photo))
            note(a.getString(R.string.pro_manual_pair))
        } else {
            caps.ev?.let { range ->
                fun step(p: Int) = range.first + kotlin.math.round((range.last - range.first) * p / 1000f).toInt()
                slider(R.string.pro_ev, ((current.ev - range.first) * 1000 / (range.last - range.first)).coerceIn(0, 1000),
                    { String.format(Locale.US, "%+.1f EV", step(it) * caps.evStep) }, { commit(desired.copy(ev = step(it))) })
            }
            if (caps.aeLock) toggle(R.string.pro_ae_lock, current.aeLock) { commit(desired.copy(aeLock = it), true) }
        }
    }
    private fun whiteBalance() {
        choices(R.string.pro_white_balance, caps.whiteBalance.map { it to a.proWhiteBalanceLabel(it) }, current.whiteBalance) {
            commit(desired.copy(whiteBalance = it, awbLock = false), true)
        }
        if (caps.awbLock && current.whiteBalance == 1) toggle(R.string.pro_awb_lock, current.awbLock) {
            commit(desired.copy(awbLock = it), true)
        }
        note(a.getString(R.string.pro_wb_hint))
    }
    private fun focus() {
        if (caps.focusMax <= 0) { note(a.getString(R.string.pro_unavailable)); return }
        toggle(R.string.pro_focus_control, current.manualFocus, R.string.pro_manual, R.string.pro_auto) {
            commit(desired.copy(manualFocus = it, focus = if (it && !desired.manualFocus) a.engine.proReading.focus ?: desired.focus else desired.focus), true)
        }
        if (current.manualFocus) {
            slider(R.string.pro_focus, (current.focus / caps.focusMax * 1000).toInt(), { p ->
                if (p == 0) "∞" else if (caps.focusCalibrated) String.format(Locale.US, "≈ %.2f m", 1 / (caps.focusMax * p / 1000f))
                    else a.getString(R.string.pro_focus_position, p / 10)
            }, { commit(desired.copy(focus = caps.focusMax * it / 1000f)) })
        }
        note(a.getString(R.string.pro_focus_hint))
    }
    private fun lens() {
        if (caps.apertures.isNotEmpty() && !current.manualExposure) note(a.getString(R.string.pro_aperture_manual))
        if (caps.apertures.isNotEmpty() && current.manualExposure) choices(R.string.pro_aperture,
            listOf(-1 to a.getString(R.string.pro_auto)) + caps.apertures.mapIndexed { i, value -> i to "ƒ/$value" },
            caps.apertures.indexOf(current.aperture)) { commit(desired.copy(aperture = caps.apertures.getOrNull(it)), true) }
        if (caps.filterDensities.isNotEmpty()) choices(R.string.pro_nd,
            listOf(-1 to a.getString(R.string.pro_auto)) + caps.filterDensities.mapIndexed { i, value -> i to "$value EV" },
            caps.filterDensities.indexOf(current.filterDensity)) { commit(desired.copy(filterDensity = caps.filterDensities.getOrNull(it)), true) }
        if (caps.opticalStabilization) choices(R.string.pro_stabilization,
            caps.stabilizations(context).map { it to a.getString(when (it) { 0 -> R.string.pro_off; 1 -> R.string.pro_optical; else -> R.string.pro_auto }) },
            current.stabilization) { commit(desired.copy(stabilization = it), true) }
        if (caps.antiBanding.size > 1 && !current.manualExposure) choices(R.string.pro_antibanding, caps.antiBanding.map {
            it to when (it) { 0 -> a.getString(R.string.pro_off); 1 -> "50 Hz"; 2 -> "60 Hz"; else -> a.getString(R.string.pro_auto) }
        } + listOf(-1 to a.getString(R.string.pro_camera_default)), current.antiBanding) { commit(desired.copy(antiBanding = it), true) }
        note(a.getString(R.string.pro_lens_hint))
    }
}
