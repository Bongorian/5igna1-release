package com.bongorian.signa1

import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

/** Fault timeline first; optional modulation and measured device inputs. */
internal object FaultDialog {
    fun styles(a: MainActivity): Array<String> {
        return arrayOf<String>(
            a.getString(R.string.live_natural),
            a.getString(R.string.live_pulse),
            a.getString(R.string.live_swell),
            a.getString(R.string.live_burst),
            a.getString(R.string.live_cascade),
        )
    }

    fun clocks(a: MainActivity): Array<String> {
        return arrayOf<String>(
            a.getString(R.string.live_free),
            a.getString(R.string.live_loop),
            a.getString(R.string.live_ping_pong),
            a.getString(R.string.live_step),
        )
    }

    fun show(a: MainActivity): Dialog? {
        if (a.recording) {
            Toast.makeText(a, R.string.fault_edit_after_recording, Toast.LENGTH_SHORT).show()
            return null
        }
        a.cancelEffectPreview()
        if (a.liveEditor != null) a.liveEditor!!.dialog!!.dismiss()
        val editor = Editor(a)
        a.liveEditor = editor
        return editor.show()
    }

    fun timeButtons(a: MainActivity, row: LinearLayout, vararg buttons: TextView?) {
        timeButtons(a, row, buttons[0], buttons[1], buttons[2], 40)
    }

    fun timeButtons(
        a: MainActivity,
        row: LinearLayout,
        first: TextView?,
        second: TextView?,
        third: TextView?,
        height: Int,
    ) {
        val buttons = arrayOf<TextView>(first!!, second!!, third!!)
        for (n in buttons.indices) {
            val button = buttons[n]
            button.setTextSize(11f)
            button.setSingleLine(true)
            button.setPadding(a.dp(4f), 0, a.dp(4f), 0)
            val p = LinearLayout.LayoutParams(0, a.dp(height.toFloat()), 1f)
            if (n < 2) p.rightMargin = a.dp(8f)
            row.addView(button, p)
        }
    }

    internal class Editor(val a: MainActivity) {
        var enabled: Boolean
        var motion: Boolean
        var audio: Boolean
        var timing: Boolean
        var thermal: Boolean
        var cpu: Boolean
        var sources: Boolean = false
        var page: Int = 0
        var sensitivity: Float
        var mains: Int
        var echo: EchoConfig
        var performance: LivePerformance
        var dialog: Dialog? = null
        var child: Dialog? = null
        var body: LinearLayout? = null
        var pages: LinearLayout? = null
        var status: TextView? = null
        var finished: Boolean = false

        fun state(): FaultConfig {
            return FaultConfig(
                enabled,
                motion,
                audio,
                timing,
                thermal,
                cpu,
                sensitivity,
                mains,
                performance,
                echo = echo,
            )
        }

        fun preview() {
            if (!finished) a.engine.previewFaultConfig(state())
        }

        fun show(): Dialog? {
            pages = a.row()
            body = LinearLayout(a)
            body!!.setOrientation(LinearLayout.VERTICAL)
            render()
            dialog =
                SignalSheet.show(
                    a,
                    a.getString(R.string.fault_live_title),
                    a.getString(R.string.live_editor_hint),
                    body!!,
                    Runnable {
                        a.engine.finishFaultPreview(true)
                        a.requestFaultConfig(state())
                    },
                    .64f,
                    pages,
                )
            val usable = a.cameraRoot.height - a.cameraRoot.paddingTop - a.cameraRoot.paddingBottom
            val height = min(a.dp(540f), Math.round(usable * .64f))
            dialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog!!.window!!.setLayout(a.cameraRoot.width - a.dp(16f), height)
            a.reserveEffectEditor(this, height)
            dialog!!.setOnDismissListener(
                OnDismissListener@{ d: DialogInterface? ->
                    finished = true
                    if (child != null) child!!.dismiss()
                    if (a.liveEditor == this) a.liveEditor = null
                    a.engine.finishFaultPreview(false)
                    a.restoreEffectEditor(this)
                    a.renderEffects()
                    a.handler.removeCallbacks(update)
                }
            )
            a.handler.post(update)
            preview()
            return dialog
        }

        val update: Runnable =
            object : Runnable {
                override fun run() {
                    if (finished) return
                    if (status != null) status!!.setText(a.engine.faultStatus)
                    a.handler.postDelayed(this, 500)
                }
            }

        init {
            val c = a.faultConfig
            enabled = c.enabled
            motion = c.motion
            audio = c.audio
            timing = c.timing
            thermal = c.thermal
            cpu = c.cpu
            sensitivity = c.sensitivity
            mains = c.mains
            performance = c.performance
            echo = c.echo
        }

        fun toggle(label: Int, value: Boolean, changed: (Boolean) -> Unit) {
            val control = SignalToggle(a, a.getString(label), value)
            val p = LinearLayout.LayoutParams(-1, a.dp(48f))
            p.bottomMargin = a.dp(8f)
            body!!.addView(control, p)
            control.setOnCheckedChangeListener(
                OnCheckedChangeListener@{ v: CompoundButton?, checked: Boolean ->
                    changed(checked)
                    preview()
                }
            )
        }

        fun setting(label: Int, choices: Array<String>, selected: Int, key: String) {
            val row =
                SignalControls.field(
                    a,
                    body!!,
                    a.getString(label) + " · " + choices[selected] + " ▾",
                )
            row.setTag("live-" + key)
            row.setOnClickListener(
                OnClickListener@{ v: View? ->
                    child =
                        SignalSheet.pick(
                            a,
                            a.getString(label),
                            choices,
                            selected,
                            IntConsumer@{ n: Int ->
                                performance = performance.with(key, n.toFloat())
                                preview()
                                render()
                            },
                        )
                }
            )
        }

        fun slider(
            label: Int,
            key: String,
            value: Float,
            min: Float,
            max: Float,
            step: Float,
            suffix: String?,
        ) {
            SignalControls.slider(
                    a,
                    body!!,
                    a.getString(label),
                    value,
                    min,
                    max,
                    step,
                    suffix,
                    Consumer@{ n: Float ->
                        performance =
                            performance.with(key, (if (key == "depth") n!! / 100 else n)!!)
                        preview()
                    },
                )
                .setTag("live-" + key)
        }

        fun render() {
            body!!.removeAllViews()
            status = null
            toggle(
                R.string.live_enabled,
                enabled,
                Consumer@{ value: Boolean -> enabled = value!! },
            )
            val pages = pages!!
            pages.removeAllViews()
            val pageLabels = if (a.settings.experimentalSignals)
                intArrayOf(R.string.live_page_time, R.string.live_page_inputs, R.string.echo_enabled)
                else intArrayOf(R.string.live_page_time, R.string.live_page_inputs)
            for (index in pageLabels.indices) {
                val button = a.button(a.getString(pageLabels[index]))
                button.tag = "live-page-$index"
                button.textSize = 11f
                button.setTextColor(if (page == index) MainActivity.LIME else MainActivity.MUTED)
                val layout = LinearLayout.LayoutParams(0, a.dp(44f), 1f)
                if (index > 0) layout.leftMargin = a.dp(6f)
                pages.addView(button, layout)
                button.setOnClickListener { page = index; render() }
            }
            pages.setPadding(0, 0, 0, a.dp(8f))
            ButtonSpacing.apply(a, pages)
            if (page == 2 && a.settings.experimentalSignals) {
                toggle(R.string.echo_enabled, echo.enabled) { echo = echo.copy(enabled = it) }
                SignalControls.slider(a, body!!, a.getString(R.string.echo_probability),
                    echo.chance * 100, 0f, 100f, 1f, "%") { value ->
                    echo = echo.copy(probability = value / 100f)
                    preview()
                }.tag = "echo-probability"
                body!!.addView(a.text(a.getString(R.string.echo_hint), 12, MainActivity.MUTED))
                ButtonSpacing.apply(a, body!!)
                return
            }
            if (page == 0) {
                setting(R.string.live_clock, clocks(a), performance.clock, "clock")
                slider(R.string.live_speed, "speed", performance.speed, -4f, 4f, .05f, "×")
                setting(R.string.live_style, styles(a), performance.style, "style")
                if (
                    performance.style != LivePerformance.NATURAL ||
                        performance.clock == LivePerformance.LOOP ||
                        performance.clock == LivePerformance.PING_PONG
                )
                    slider(
                        R.string.live_cycle,
                        "period",
                        performance.periodSeconds,
                        .25f,
                        32f,
                        .05f,
                        " s",
                    )
                if (performance.style != LivePerformance.NATURAL)
                    slider(
                        R.string.live_depth,
                        "depth",
                        performance.depth * 100,
                        0f,
                        100f,
                        1f,
                        "%",
                    )
                if (performance.clock == LivePerformance.STEP)
                    slider(
                        R.string.live_division,
                        "interval",
                        performance.stepSeconds,
                        .015625f,
                        2f,
                        .015625f,
                        " s",
                    )
                if (performance.style == LivePerformance.BURST) {
                    slider(R.string.live_burst_width, "width", performance.width, .05f, .95f, .01f, "")
                    slider(R.string.live_chance, "chance", performance.chance, 0f, 1f, .01f, "")
                }
                val transport = a.row()
                body!!.addView(transport, LinearLayout.LayoutParams(-1, a.dp(48f)))
                val hold =
                    a.button(
                        a.getString(if (performance.hold) R.string.live_resume else R.string.live_pause)
                    )
                val hit = a.button(a.getString(R.string.live_trigger))
                val reset = a.button(a.getString(R.string.live_reset))
                hold.setTextColor(if (performance.hold) MainActivity.LIME else MainActivity.MUTED)
                hold.setContentDescription(a.getString(R.string.live_hold_hint))
                hit.setContentDescription(a.getString(R.string.live_hit_hint))
                reset.setContentDescription(a.getString(R.string.live_reset_hint))
                timeButtons(a, transport, hold, hit, reset, 44)
                hold.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        performance = performance.held(!performance.hold)
                        preview()
                        render()
                    }
                )
                hit.setOnClickListener(
                    OnClickListener@{ v: View? -> if (enabled) a.engine.hitFaults() }
                )
                reset.setOnClickListener(OnClickListener@{ v: View? -> a.engine.rewindFaults() })
                return
            }
            sources = true
            if (sources) {
                toggle(
                    R.string.fault_motion,
                    motion,
                    Consumer@{ value: Boolean -> motion = value!! },
                )
                toggle(R.string.fault_audio, audio, Consumer@{ value: Boolean -> audio = value!! })
                toggle(
                    R.string.fault_timing,
                    timing,
                    Consumer@{ value: Boolean -> timing = value!! },
                )
                toggle(
                    R.string.fault_thermal,
                    thermal,
                    Consumer@{ value: Boolean -> thermal = value!! },
                )
                toggle(R.string.fault_cpu, cpu, Consumer@{ value: Boolean -> cpu = value!! })
                SignalControls.slider(
                    a,
                    requireNotNull(body),
                    a.getString(R.string.fault_sensitivity),
                    sensitivity * 100,
                    0f,
                    100f,
                    1f,
                    "%",
                    Consumer@{ value: Float ->
                        sensitivity = value!! / 100
                        preview()
                    },
                )
                val mainsField =
                    SignalControls.field(
                        a,
                        requireNotNull(body),
                        a.getString(R.string.live_mains) + " · " + mains + " Hz ▾",
                    )
                mainsField.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        child =
                            SignalSheet.pick(
                                a,
                                a.getString(R.string.live_mains),
                                arrayOf<String>("50 Hz", "60 Hz"),
                                if (mains == 60) 1 else 0,
                                IntConsumer@{ n: Int ->
                                    mains = if (n == 0) 50 else 60
                                    preview()
                                    render()
                                },
                            )
                    }
                )
                status = a.text(a.engine.faultStatus, 10, MainActivity.MUTED)
                status!!.setPadding(0, a.dp(8f), 0, a.dp(8f))
                body!!.addView(status)
            }
            ButtonSpacing.apply(a, body!!)
        }
    }
}
