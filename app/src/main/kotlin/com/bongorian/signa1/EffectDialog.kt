package com.bongorian.signa1

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.bongorian.signa1.MainActivity.EffectPreview
import java.security.SecureRandom
import java.util.Arrays
import java.util.function.IntPredicate
import kotlin.math.min

/** Draft route editor. Its reserved space keeps every slider below the camera preview. */
internal class EffectDialog(val a: MainActivity, single: Boolean) {
    val edit: EffectPreview
    val base: EffectState
    val ids: IntArray
    var draft: EffectParameters
    var mask: Int
    var focused: Int
    var amount: Float
    var chained: Boolean = true
    var tuning: Boolean
    var advancedControls: AdvancedControls? = null
    private var networkSummary: TextView? = null
    var advancedGroup: FaultParameters.Group = FaultParameters.Group.SIGNAL
    var sheet: Dialog? = null
    var auxiliary: Dialog? = null
    private var selectionSummary: TextView? = null
    var route: LinearLayout? = null
    var body: LinearLayout? = null
    var scroll: ScrollView? = null
    val choices: MutableMap<Int?, TextView?> = LinkedHashMap<Int?, TextView?>()

    init {
        edit = a.beginEffectPreview()
        base = edit.base
        draft = base.parameters()
        mask = base.mask
        amount = base.amount
        ids =
            Arrays.stream(Effects.ORDER)
                .filter(IntPredicate { id: Int -> id != Effects.CLEAN && (a.settings.experimentalSignals || !Effects.physical(id)) })
                .toArray()
        focused = base.ids().firstOrNull { it in ids } ?: Effects.CLEAN
        tuning = single && focused != Effects.CLEAN
    }

    fun state(): EffectState {
        return base.edit(chained, mask, draft).amount(amount)
    }

    fun preview() {
        a.previewEffectEdit(edit, state())
    }

    fun action(text: Int): TextView {
        val view = a.button(a.getString(text))
        view.setTextSize(11f)
        return view
    }

    fun show() {
        sheet = Dialog(a)
        sheet!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(a)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setPadding(a.dp(12f), a.dp(6f), a.dp(12f), a.dp(8f))
        root.setBackground(a.bg(MainActivity.BG, MainActivity.PANEL))
        val toolbar = a.row()
        root.addView(toolbar, LinearLayout.LayoutParams(-1, a.dp(ControlSize.PRIMARY)))
        val cancel = action(R.string.ui_back)
        cancel.setText("×")
        cancel.setTextSize(24f)
        toolbar.addView(cancel, LinearLayout.LayoutParams(a.dp(ControlSize.TOOLBAR), a.dp(ControlSize.TOOLBAR)))
        cancel.setOnClickListener(OnClickListener@{ v: View? -> sheet!!.dismiss() })
        val heading = a.title(a.getString(R.string.fault_chain_editor))
        heading.setGravity(Gravity.CENTER)
        toolbar.addView(heading, LinearLayout.LayoutParams(0, -1, 1f))
        val random = a.iconButton(R.drawable.ic_shuffle, a.getString(R.string.ui_random_chain))
        random.setTag("random-chain")
        toolbar.addView(random, LinearLayout.LayoutParams(a.dp(ControlSize.TOOLBAR), a.dp(ControlSize.TOOLBAR)))
        random.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (a.rawOriginal()) return@OnClickListener
                val next = EffectRandomizer.chain(state(), a.availableEffects(), SecureRandom())
                mask = next.mask
                amount = next.amount
                draft = next.parameters()
                focused = next.selected()
                renderRoute()
                renderBody()
                preview()
            }
        )
        random.setEnabled(!a.rawOriginal())
        random.setAlpha(if (a.rawOriginal()) .35f else 1f)
        val apply = action(R.string.ui_apply)
        apply.setTag("apply")
        apply.setText("✓")
        apply.setTextSize(22f)
        apply.setTextColor(MainActivity.BG)
        apply.setBackground(a.bg(MainActivity.LIME, 0))
        toolbar.addView(apply, LinearLayout.LayoutParams(a.dp(ControlSize.PRIMARY), a.dp(ControlSize.PRIMARY)))
        apply.setOnClickListener(
            OnClickListener@{ v: View? ->
                a.finishEffectEdit(edit, state(), true)
                sheet!!.dismiss()
            }
        )
        selectionSummary = a.text("", 12, MainActivity.LIME)
        root.addView(selectionSummary, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = a.dp(6f) })
        val routeRow = a.row()
        root.addView(routeRow, LinearLayout.LayoutParams(-1, a.dp(50f)))
        val path = HorizontalScrollView(a)
        path.setHorizontalScrollBarEnabled(false)
        route = a.row()
        path.addView(route)
        routeRow.addView(path, LinearLayout.LayoutParams(0, -1, 1f))
        val add = a.button(a.getString(R.string.chain_choose))
        add.setTextSize(12f)
        add.setContentDescription(a.getString(R.string.fault_add_remove))
        routeRow.addView(add, LinearLayout.LayoutParams(a.dp(76f), a.dp(44f)))
        add.setOnClickListener(
            OnClickListener@{ v: View? ->
                tuning = false
                renderRoute()
                renderBody()
            }
        )
        scroll = ScrollView(a)
        scroll!!.setFillViewport(false)
        body = LinearLayout(a)
        body!!.setOrientation(LinearLayout.VERTICAL)
        scroll!!.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        ButtonSpacing.apply(a, root)
        sheet!!.setContentView(root)
        sheet!!.setCanceledOnTouchOutside(true)
        val window = sheet!!.window
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setGravity(Gravity.BOTTOM)
        sheet!!.setOnDismissListener(
            OnDismissListener@{ d: DialogInterface? ->
                if (auxiliary != null) auxiliary!!.dismiss()
                a.restoreEffectEditor(this)
                a.finishEffectEdit(edit, base, false)
            }
        )
        edit.dialog = sheet
        renderRoute()
        renderBody()
        sheet!!.show()
        resize()
        preview()
    }

    fun resize() {
        if (sheet == null || !sheet!!.isShowing()) return
        val usable = a.cameraRoot.height - a.cameraRoot.paddingTop - a.cameraRoot.paddingBottom
        val desired =
            if (a.advancedMode) a.dp(460f)
            else if (tuning && a.effectAvailable(focused))
                a.dp((198 + Effects.CONTROLS[focused].size * 66).toFloat())
            else a.dp(425f)
        val height = min(desired, Math.round(usable * .53f))
        SignalSheet.placeEditor(a, sheet!!, height)
        a.reserveEffectEditor(this, height)
    }

    private fun refreshSeedControls() {
        body?.findViewWithTag<TextView>("identity-seed")?.apply {
            text = "SEED\n" + draft.identity(focused)
            contentDescription = "SEED " + draft.identity(focused)
        }
        preview()
    }

    private fun seedControls(id: Int) {
        if (!EffectRandomizer.supportsSeed(id, draft)) return
        val row = a.row()
        val seed = a.button("SEED\n" + draft.identity(id))
        seed.tag = "identity-seed"
        seed.textSize = 12f
        seed.contentDescription = "SEED " + draft.identity(id)
        row.addView(seed, LinearLayout.LayoutParams(0, a.dp(56f), 1f))
        val reroll = action(R.string.ui_reseed)
        reroll.tag = "reseed"
        reroll.textSize = 12f
        row.addView(reroll, LinearLayout.LayoutParams(a.dp(96f), a.dp(56f)))
        body!!.addView(row)
        seed.setOnClickListener {
            auxiliary = SignalSheet.number(a, "SEED", a.getString(R.string.ui_seed_hint), draft.identity(id).toString(), true) { value ->
                draft = draft.reseed(id, value.toLong())
                refreshSeedControls()
                true
            }
        }
        reroll.setOnClickListener {
            draft = EffectRandomizer.reseed(state(), intArrayOf(id), SecureRandom()).parameters()
            refreshSeedControls()
        }
        if (draft.overrides(id).isNotEmpty() || draft.fixedEventIdentity(id)) {
            val hint = a.text(a.getString(R.string.seed_fixed_hint), 11, MainActivity.MUTED)
            hint.setPadding(0, a.dp(6f), 0, a.dp(8f))
            body!!.addView(hint)
        }
    }

    private fun transportControls(id: Int) {
        if (id != Effects.VHS && id != Effects.CRT) return
        fun choice(key: String, title: Int, labels: Array<String>) {
            val resolvedKey = when (key) { "transport" -> "transportKind"; "cable" -> "cableKind"; else -> key }
            val selected = if (key == "transport") draft.transportKind(id) else
                kotlin.math.round(draft.resolved(id, resolvedKey, draft.get(id, key) * (labels.size - 1))).toInt()
            val button = a.button(a.getString(title) + " · " + labels[selected])
            button.tag = "transport-$key"
            body!!.addView(button, LinearLayout.LayoutParams(-1, a.dp(48f)))
            button.setOnClickListener {
                SignalSheet.pick(a, a.getString(title), labels, selected, { n ->
                    draft = draft.with(id, key, n.toFloat() / (labels.size - 1))
                    draft = draft.automatic(id, resolvedKey)
                    renderBody(); preview()
                })
            }
        }
        choice("transport", R.string.transport_model, if (id == Effects.VHS)
            arrayOf("VHS", "DVD", "Digital thru", "Analog thru")
            else arrayOf("CRT", "Digital thru", "Network display", "LED display"))
        val kind = draft.transportKind(id)
        if (id == Effects.VHS && kind <= 1) {
            val toggle = SignalToggle(a, a.getString(R.string.transport_reduce), draft.resolved(id, "mediaReduce", draft.get(id, "reduce")) >= .5f)
            body!!.addView(toggle, LinearLayout.LayoutParams(-1, a.dp(48f)))
            toggle.setOnCheckedChangeListener { _, checked -> draft = draft.with(id, "reduce", if (checked) 1f else 0f).automatic(id, "mediaReduce"); preview() }
        }
        if (id == Effects.VHS && kind == 3) choice("cable", R.string.transport_cable, arrayOf("Composite", "Component"))
        if (id == Effects.CRT && kind == 1) choice("upconvert", R.string.transport_upconvert,
            arrayOf(a.getString(R.string.transport_nearest), a.getString(R.string.transport_linear)))
        val hint = a.text(a.getString((if (id == Effects.VHS) intArrayOf(R.string.transport_vhs_hint, R.string.transport_dvd_hint, R.string.transport_digital_media_hint, R.string.transport_analog_hint) else intArrayOf(R.string.transport_crt_hint, R.string.transport_digital_display_hint, R.string.transport_network_hint, R.string.transport_led_hint))[kind]), 12, MainActivity.MUTED)
        hint.setPadding(0, a.dp(8f), 0, a.dp(8f))
        body!!.addView(hint)
        if (id == Effects.CRT && kind == 2) {
            networkSummary = a.text("", 12, MainActivity.LIME)
            networkSummary!!.tag = "network-effective"
            body!!.addView(networkSummary)
            updateNetwork(null)
        }
    }

    fun updateNetwork(frame: EffectState.Frame?) {
        val summary = networkSummary ?: return
        if (frame == null && amount <= 0f) { summary.text = a.getString(R.string.network_bypassed); return }
        val node = if (frame == null) FaultModel(0).inspect(Effects.CRT, draft, amount, a.faultConfig)
            else frame.nodes.firstOrNull { it.id == Effects.CRT && Math.round(it.profile["transportKind"] ?: 0f) == 2 }
        if (node == null) { summary.text = a.getString(R.string.network_bypassed); return }
        val fps = node.mechanism["networkFps"] ?: 0f
        val rate = if (fps <= 0) a.getString(R.string.network_source_rate) else a.getString(R.string.network_actual_fps, fps)
        val current = node.inspect()
        summary.text = (if ((current["eventPeriod"] ?: 0f) <= 0f) a.getString(R.string.network_effective_off, rate,
            Math.round((1 - node.get("transportLoss") * .8f) * 100)) else a.getString(R.string.network_effective, rate,
            Math.round((1 - node.get("transportLoss") * .8f) * 100),
            current["eventPeriod"] ?: 0f,
            minOf(current["eventDuration"] ?: 0f, current["eventPeriod"] ?: 0f),
            Math.round((current["eventProbability"] ?: 0f) * 100))) +
            if (draft.manual(Effects.CRT, "networkStall")) a.getString(R.string.network_manual_stall)
            else if (!a.faultConfig.enabled) a.getString(R.string.network_live_required) else ""
    }

    fun renderRoute() {
        val selectedIds = Effects.ordered(mask, false)
        selectionSummary?.text = a.getString(R.string.chain_counts, selectedIds.size, selectedIds.count { a.effectAvailable(it) })
        route!!.removeAllViews()
        for (id in Effects.ordered(mask, false).filter { a.settings.experimentalSignals || !Effects.physical(it) }) {
            val chip = a.button(Effects.label(id))
            chip.setTextSize(12f)
            val focus = tuning && id == focused
            chip.setTextColor(
                if (focus) MainActivity.BG
                else if (a.effectAvailable(id)) MainActivity.WHITE else MainActivity.MUTED
            )
            chip.setBackground(
                a.bg(
                    if (focus) MainActivity.LIME else MainActivity.PANEL,
                    0,
                )
            )
            val p = LinearLayout.LayoutParams(-2, a.dp(ControlSize.COMPACT))
            p.rightMargin = a.dp(6f)
            route!!.addView(chip, p)
            chip.setOnClickListener(
                OnClickListener@{ v: View? ->
                    focused = id
                    tuning = true
                    renderRoute()
                    renderBody()
                }
            )
        }
    }

    fun renderBody() {
        body!!.removeAllViews()
        choices.clear()
        advancedControls = null
        networkSummary = null
        if (tuning && focused != Effects.CLEAN) {
            val id = focused
            val title = a.row()
            val name =
                a.text(
                    Effects.label(id) + " · " + (if (Effects.physical(id)) a.getString(R.string.physical_artifact) else Effects.stage(id)),
                    10,
                    MainActivity.MUTED,
                )
            title.addView(name, LinearLayout.LayoutParams(0, a.dp(40f), 1f))
            if ((mask and (1 shl id)) == 0 && a.effectAvailable(id)) {
                val add = action(R.string.chain_add)
                title.addView(add, LinearLayout.LayoutParams(-2, a.dp(ControlSize.COMPACT)))
                add.setOnClickListener {
                    mask = mask or (1 shl id)
                    renderRoute()
                    renderBody()
                    preview()
                }
            }
            if ((mask and (1 shl id)) != 0) {
                val remove = action(R.string.fault_remove)
                title.addView(remove, LinearLayout.LayoutParams(-2, a.dp(40f)))
                remove.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        mask = mask and (1 shl id).inv()
                        focused = if (mask == 0) 0 else Effects.ordered(mask, false)[0]
                        if (mask == 0) tuning = false
                        renderRoute()
                        renderBody()
                        preview()
                    }
                )
            }
            body!!.addView(title)
            body!!.addView(a.text(FaultPresentation.description(a, id), 13, MainActivity.MUTED).apply {
                setPadding(0, a.dp(4f), 0, a.dp(16f))
            })
            if (!a.effectAvailable(id)) {
                val hint =
                    a.text(
                        a.getString(if (a.tapBypasses(id)) R.string.tap_bypass_hint else R.string.fault_requires_rgb),
                        13,
                        MainActivity.MUTED,
                    )
                hint.setPadding(0, a.dp(12f), 0, a.dp(16f))
                body!!.addView(hint)
                if (!a.tapBypasses(id)) {
                val convert =
                    action(
                        if (a.videoMode) R.string.fault_switch_mp4 else R.string.fault_switch_jpeg
                    )
                convert.setTag("switch-format")
                body!!.addView(convert, LinearLayout.LayoutParams(-1, a.dp(48f)))
                convert.setOnClickListener(OnClickListener@{ v: View? -> switchFormat(id) })
                }
            } else {
                transportControls(id)
                seedControls(id)
                if (a.advancedMode) {
                    advancedControls = AdvancedControls(this, id)
                    advancedControls!!.show(requireNotNull(body))
                } else for (control in Effects.CONTROLS[id].filter { it.key !in setOf("transport", "reduce", "cable", "upconvert") }) {
                    val kind = if (id == Effects.VHS || id == Effects.CRT) draft.transportKind(id) else 0
                    if (id == Effects.VHS && (kind == 2 || (kind == 1 && control.key in setOf("bandwidth", "noise")) || (kind == 3 && control.key == "bandwidth"))) continue
                    if (id == Effects.CRT && (kind == 1 ||
                        (kind == 2 && control.key !in NetworkDisplay.keys) ||
                        (kind != 2 && control.key in NetworkDisplay.keys) ||
                        (control.key == "ledRate" && kind != 3) ||
                        (kind == 3 && control.key == "phosphor"))) continue
                    slider(id, control)
                }
                val actions = a.row()
                val reset = action(R.string.ui_reset)
                actions.addView(reset, LinearLayout.LayoutParams(0, a.dp(ControlSize.TOOLBAR), 1f))
                body!!.addView(actions)
                reset.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        draft = draft.reset(id)
                        renderBody()
                        preview()
                    }
                )

            }
        } else {
            val hint =
                a.text(
                    a.getString(R.string.fault_catalog_hint),
                    MainActivity.TEXT_BODY,
                    MainActivity.MUTED,
                )
            hint.setPadding(0, a.dp(4f), 0, a.dp(8f))
            body!!.addView(hint)
            var previous: Effects.Point? = null
            for (id in ids) {
                val point = Effects.point(id)
                if (point != previous) {
                    body!!.addView(a.text(Effects.stage(id), 11, MainActivity.MUTED).apply {
                        setPadding(a.dp(4f), a.dp(14f), 0, a.dp(6f))
                    })
                    previous = point
                }
                val row = a.row()
                val choice = a.button("")
                choice.textSize = 14f
                choice.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                choice.setPadding(a.dp(12f), a.dp(12f), a.dp(12f), a.dp(12f))
                choice.minHeight = a.dp(80f)
                choices[id] = choice
                row.addView(choice, LinearLayout.LayoutParams(0, -2, 1f))
                val detail = a.button(a.getString(R.string.chain_details))
                detail.textSize = 12f
                detail.contentDescription = Effects.label(id) + " · " + a.getString(R.string.chain_details)
                row.addView(detail, LinearLayout.LayoutParams(a.dp(72f), -1).apply { leftMargin = a.dp(6f) })
                detail.setOnClickListener {
                    focused = id
                    tuning = true
                    renderRoute()
                    renderBody()
                }
                body!!.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = a.dp(8f) })
                paintChoice(id)
                choice.setOnClickListener {
                    // An unavailable selected stage may be removed, but importing never removes it.
                    if (!a.effectAvailable(id) && mask and (1 shl id) == 0) {
                        focused = id
                        tuning = true
                        renderRoute()
                        renderBody()
                    } else {
                        mask = mask xor (1 shl id)
                        paintChoice(id)
                        renderRoute()
                        preview()
                        a.confirmHaptic(choice, HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
            val clear = action(R.string.ui_clear_selection)
            body!!.addView(clear, LinearLayout.LayoutParams(-1, a.dp(48f)))
            clear.setOnClickListener(
                OnClickListener@{ v: View? ->
                    mask = 0
                    focused = 0
                    renderRoute()
                    renderBody()
                    preview()
                }
            )
        }
        scroll!!.scrollTo(0, 0)
        ButtonSpacing.apply(a, body!!)
        ButtonSpacing.apply(a, route!!)
        sheet?.findViewById<View>(android.R.id.content)?.let { ButtonSpacing.apply(a, it) }
        resize()
    }

    fun switchFormat(id: Int) {
        if (a.recording || a.engine.photoBusy) return
        val candidate = state().chain(mask or (1 shl id))
        sheet!!.dismiss()
        val next = CaptureSettings(a.settings)
        if (a.videoMode) next.rawVideo = false
        else {
            next.photoFormat = 0
            next.photoSize = "recommended"
        }
        a.applySettings(next)
        a.handler.post(
            Runnable@{
                if (!a.resumed) return@Runnable
                val replacement = EffectDialog(a, true)
                replacement.mask = candidate.mask
                replacement.amount = candidate.amount
                replacement.draft = candidate.parameters()
                replacement.focused = id
                replacement.tuning = true
                replacement.show()
            }
        )
    }

    fun paintChoice(id: Int) {
        val view = choices.get(id)
        val selected = (mask and (1 shl id)) != 0
        val available = a.effectAvailable(id)
        var stage = Effects.stage(id)
        stage = stage.substring(stage.indexOf(" / ") + 3)
        val label = (if (selected) "✓  " else "＋  ") + Effects.label(id)
        val copy = label + "\n" + FaultPresentation.description(a, id) +
            (if (available) "" else "\n" + a.getString(if (a.tapBypasses(id)) R.string.tap_bypassed else R.string.fault_requires_rgb))
        view!!.text = android.text.SpannableString(copy).apply {
            setSpan(android.text.style.RelativeSizeSpan(.86f), label.length + 1, length, 0)
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), label.length + 1, length, 0)
        }
        view.setTextColor(
            if (selected) MainActivity.BG
            else if (available) MainActivity.WHITE else MainActivity.MUTED
        )
        view.setBackground(
            a.bg(
                if (selected) MainActivity.LIME else MainActivity.PANEL,
                0,
            )
        )
        view.setSelected(selected)
        view.setContentDescription(
            Effects.label(id) +
                " · " +
                stage +
                " · " + a.getString(if (selected) R.string.chain_selected else R.string.chain_unselected) +
                (if (available) "" else " · " + a.getString(if (a.tapBypasses(id)) R.string.tap_bypassed else R.string.fault_requires_rgb))
        )
    }

    private fun transportControlLabel(id: Int, key: String): Int {
        val kind = if (id == Effects.VHS || id == Effects.CRT) draft.transportKind(id) else 0
        if (id == Effects.VHS && kind == 1) return if (key == "tracking") R.string.transport_block_damage else R.string.transport_block_loss
        if (id == Effects.VHS && kind == 3) return when (key) {
            "tracking" -> R.string.transport_sync_loss
            "dropout" -> R.string.transport_contact
            else -> R.string.transport_interference
        }
        if (id == Effects.CRT && kind == 2) return controlLabel(key)
        if (key == "ledRate") return R.string.fault_control_ledRate
        if (id == Effects.CRT && kind == 3) return when (key) {
            "scan" -> R.string.transport_pitch
            "convergence" -> R.string.transport_module_loss
            else -> R.string.transport_refresh
        }
        return controlLabel(key)
    }

    private fun controlValue(key: String, value: Float): String = when (key) {
        "ledRate" -> a.getString(R.string.led_refresh_hz, value * 30)
        "networkInterval" -> NetworkDisplay.interval(value).let { if (it == 0) a.getString(R.string.network_off) else a.getString(R.string.network_every_seconds, it) }
        "networkDuration" -> a.getString(R.string.network_seconds, NetworkDisplay.duration(value))
        "networkRate" -> NetworkDisplay.fps(value).let { if (it == 0) a.getString(R.string.network_source_rate) else a.getString(R.string.network_fps, it) }
        "networkResolution" -> a.getString(R.string.network_percent, Math.round(NetworkDisplay.scale(value) * 100))
        else -> Math.round(value * 100).toString() + "%"
    }

    fun slider(id: Int, control: Effects.Control) {
        val row = a.row()
        val name = a.text(a.getString(transportControlLabel(id, control.key)), 12, MainActivity.MUTED)
        val value = a.text("", 12, MainActivity.LIME)
        row.addView(name, LinearLayout.LayoutParams(0, a.dp(24f), 1f))
        row.addView(value)
        body!!.addView(row)
        val slider = SeekBar(a)
        slider.setContentDescription(a.getString(transportControlLabel(id, control.key)))
        slider.setTag(control.key)
        slider.setMax(100)
        slider.setProgress(Math.round(draft.get(id, control.key) * 100))
        value.setText(controlValue(control.key, draft.get(id, control.key)))
        slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME))
        slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME))
        body!!.addView(slider, LinearLayout.LayoutParams(-1, a.dp(42f)))
        slider.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, n: Int, user: Boolean) {
                    if (user) {
                        draft = draft.with(id, control.key, n / 100f)
                        value.setText(controlValue(control.key, n / 100f))
                        updateNetwork(null)
                        preview()
                    }
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}

                override fun onStopTrackingTouch(s: SeekBar?) {}
            }
        )
    }

    companion object {
        fun controlLabel(key: String): Int {
            when (key) {
                "ledRate" -> return R.string.fault_control_ledRate
                "networkInterval" -> return R.string.fault_control_networkInterval
                "networkDuration" -> return R.string.fault_control_networkDuration
                "networkRate" -> return R.string.fault_control_networkRate
                "networkResolution" -> return R.string.fault_control_networkResolution
                "transport" -> return R.string.fault_control_transport
                "reduce" -> return R.string.fault_control_reduce
                "cable" -> return R.string.fault_control_cable
                "upconvert" -> return R.string.fault_control_upconvert
                "amount" -> return R.string.fault_control_amount
                "floor" -> return R.string.fault_control_floor
                "grain" -> return R.string.fault_control_grain
                "length" -> return R.string.fault_control_length
                "threshold" -> return R.string.fault_control_threshold
                "density" -> return R.string.fault_control_density
                "hot" -> return R.string.fault_control_hot
                "columns" -> return R.string.fault_control_columns
                "depth" -> return R.string.fault_control_depth
                "rate" -> return R.string.fault_control_rate
                "bands" -> return R.string.fault_control_bands
                "displacement" -> return R.string.fault_control_displacement
                "loss" -> return R.string.fault_control_loss
                "concealment" -> return R.string.fault_control_concealment
                "activity" -> return R.string.fault_control_activity
                "bit" -> return R.string.fault_control_bit
                "burst_size" -> return R.string.fault_control_burst_size
                "offset" -> return R.string.fault_control_offset
                "region" -> return R.string.fault_control_region
                "coverage" -> return R.string.fault_control_coverage
                "phase" -> return R.string.fault_control_phase
                "interpolation" -> return R.string.fault_control_interpolation
                "sampling" -> return R.string.fault_control_sampling
                "separation" -> return R.string.fault_control_separation
                "direction" -> return R.string.fault_control_direction
                "palette" -> return R.string.fault_control_palette
                "cycles" -> return R.string.fault_control_cycles
                "mix" -> return R.string.fault_control_mix
                "quantization" -> return R.string.fault_control_quantization
                "block_size" -> return R.string.fault_control_block_size
                "misaddress" -> return R.string.fault_control_misaddress
                "bandwidth" -> return R.string.fault_control_bandwidth
                "tracking" -> return R.string.fault_control_tracking
                "dropout" -> return R.string.fault_control_dropout
                "noise" -> return R.string.fault_control_noise
                "scan" -> return R.string.fault_control_scan
                "phosphor" -> return R.string.fault_control_phosphor
                "convergence" -> return R.string.fault_control_convergence
                "sync" -> return R.string.fault_control_sync
                else -> throw IllegalArgumentException("Unknown fault control " + key)
            }
        }
    }
}
