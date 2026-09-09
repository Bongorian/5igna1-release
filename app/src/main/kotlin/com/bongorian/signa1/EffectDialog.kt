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
    var advancedGroup: FaultParameters.Group = FaultParameters.Group.SIGNAL
    var sheet: Dialog? = null
    var auxiliary: Dialog? = null
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
        root.addView(toolbar, LinearLayout.LayoutParams(-1, a.dp(48f)))
        val cancel = action(R.string.ui_back)
        cancel.setText("×")
        cancel.setTextSize(24f)
        toolbar.addView(cancel, LinearLayout.LayoutParams(a.dp(44f), a.dp(44f)))
        cancel.setOnClickListener(OnClickListener@{ v: View? -> sheet!!.dismiss() })
        val heading = a.title(a.getString(R.string.fault_chain_editor))
        heading.setGravity(Gravity.CENTER)
        toolbar.addView(heading, LinearLayout.LayoutParams(0, -1, 1f))
        val random = a.iconButton(R.drawable.ic_shuffle, a.getString(R.string.ui_random_chain))
        random.setTag("random-chain")
        toolbar.addView(random, LinearLayout.LayoutParams(a.dp(48f), a.dp(48f)))
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
        toolbar.addView(apply, LinearLayout.LayoutParams(a.dp(44f), a.dp(44f)))
        apply.setOnClickListener(
            OnClickListener@{ v: View? ->
                a.finishEffectEdit(edit, state(), true)
                sheet!!.dismiss()
            }
        )
        val routeRow = a.row()
        root.addView(routeRow, LinearLayout.LayoutParams(-1, a.dp(50f)))
        val path = HorizontalScrollView(a)
        path.setHorizontalScrollBarEnabled(false)
        route = a.row()
        path.addView(route)
        routeRow.addView(path, LinearLayout.LayoutParams(0, -1, 1f))
        val add = a.button("＋")
        add.setTextSize(22f)
        add.setContentDescription(a.getString(R.string.fault_add_remove))
        routeRow.addView(add, LinearLayout.LayoutParams(a.dp(44f), a.dp(44f)))
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
        sheet!!.window!!.setLayout(a.cameraRoot.width - a.dp(16f), height)
        a.reserveEffectEditor(this, height)
    }

    fun renderRoute() {
        route!!.removeAllViews()
        for (id in Effects.ordered(mask, false).filter { a.settings.experimentalSignals || !Effects.physical(it) }) {
            val chip = a.button(Effects.name(id))
            chip.setTextSize(10f)
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
            val p = LinearLayout.LayoutParams(-2, a.dp(44f))
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
        if (tuning && focused != Effects.CLEAN) {
            val id = focused
            val title = a.row()
            val name =
                a.text(
                    Effects.name(id) + " · " + (if (Effects.physical(id)) a.getString(R.string.physical_artifact) else Effects.stage(id)),
                    10,
                    MainActivity.MUTED,
                )
            title.addView(name, LinearLayout.LayoutParams(0, a.dp(40f), 1f))
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
            if (!a.effectAvailable(id)) {
                val hint =
                    a.text(
                        a.getString(R.string.fault_requires_rgb),
                        13,
                        MainActivity.MUTED,
                    )
                hint.setPadding(0, a.dp(12f), 0, a.dp(16f))
                body!!.addView(hint)
                val convert =
                    action(
                        if (a.videoMode) R.string.fault_switch_mp4 else R.string.fault_switch_jpeg
                    )
                convert.setTag("switch-format")
                body!!.addView(convert, LinearLayout.LayoutParams(-1, a.dp(48f)))
                convert.setOnClickListener(OnClickListener@{ v: View? -> switchFormat(id) })
            } else {
                if (a.advancedMode) {
                    advancedControls = AdvancedControls(this, id)
                    advancedControls!!.show(requireNotNull(body))
                } else for (control in Effects.CONTROLS[id]) slider(id, control)
                val actions = a.row()
                val reset = action(R.string.ui_reset)
                val reseed = action(R.string.ui_reseed)
                actions.addView(reset, LinearLayout.LayoutParams(0, a.dp(44f), 1f))
                actions.addView(reseed, LinearLayout.LayoutParams(0, a.dp(44f), 1f))
                body!!.addView(actions)
                reset.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        draft = draft.reset(id)
                        renderBody()
                        preview()
                    }
                )
                reseed.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        draft = draft.reseed(id, SecureRandom().nextLong())
                        preview()
                    }
                )
            }
        } else {
            val hint =
                a.text(
                    a.getString(R.string.fault_catalog_hint) + (if (a.settings.experimentalSignals) "\n" + a.getString(R.string.physical_artifact_hint) else ""),
                    MainActivity.TEXT_BODY,
                    MainActivity.MUTED,
                )
            hint.setPadding(0, a.dp(4f), 0, a.dp(8f))
            body!!.addView(hint)
            var row: LinearLayout? = null
            for (i in ids.indices) {
                if (i % 2 == 0) {
                    row = a.row()
                    body!!.addView(row, LinearLayout.LayoutParams(-1, a.dp(65f)))
                }
                val id = ids[i]
                val choice = a.button("")
                choice.setTextSize(11f)
                choice.setPadding(a.dp(6f), a.dp(4f), a.dp(6f), a.dp(4f))
                choices.put(id, choice)
                val p = LinearLayout.LayoutParams(0, a.dp(60f), 1f)
                p.setMargins(a.dp(2f), 0, a.dp(2f), 0)
                row!!.addView(choice, p)
                paintChoice(id)
                choice.setOnClickListener(
                    OnClickListener@{ v: View? ->
                        if (!a.effectAvailable(id)) {
                            focused = id
                            tuning = true
                            renderRoute()
                            renderBody()
                            return@OnClickListener
                        }
                        mask = mask xor (1 shl id)
                        if ((mask and (1 shl id)) != 0) focused = id
                        else if (focused == id)
                            focused = if (mask == 0) 0 else Effects.ordered(mask, false)[0]
                        paintChoice(id)
                        renderRoute()
                        preview()
                        v!!.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                )
            }
            if (ids.size % 2 == 0) {
                row = a.row()
                body!!.addView(row, LinearLayout.LayoutParams(-1, a.dp(65f)))
            }
            val clear = action(R.string.ui_clear_selection)
            clear.setTextSize(10f)
            val clearP = LinearLayout.LayoutParams(0, a.dp(60f), 1f)
            clearP.setMargins(a.dp(2f), 0, a.dp(2f), 0)
            row!!.addView(clear, clearP)
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
        view!!.setText(
            (if (selected) "✓ " else "") +
                Effects.name(id) +
                "\n" +
                (if (Effects.physical(id)) a.getString(R.string.physical_artifact) else stage) +
                (if (available) "" else " · " + (if (a.videoMode) "MP4" else "JPG"))
        )
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
            Effects.name(id) +
                " · " +
                stage +
                (if (available) "" else " · " + a.getString(R.string.fault_requires_rgb))
        )
    }

    fun slider(id: Int, control: Effects.Control) {
        val row = a.row()
        val name = a.text(a.getString(controlLabel(control.key)), 12, MainActivity.MUTED)
        val value = a.text("", 12, MainActivity.LIME)
        row.addView(name, LinearLayout.LayoutParams(0, a.dp(24f), 1f))
        row.addView(value)
        body!!.addView(row)
        val slider = SeekBar(a)
        slider.setContentDescription(a.getString(controlLabel(control.key)))
        slider.setTag(control.key)
        slider.setMax(100)
        slider.setProgress(Math.round(draft.get(id, control.key) * 100))
        value.setText(slider.progress.toString() + "%")
        slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME))
        slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME))
        body!!.addView(slider, LinearLayout.LayoutParams(-1, a.dp(42f)))
        slider.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, n: Int, user: Boolean) {
                    if (user) {
                        draft = draft.with(id, control.key, n / 100f)
                        value.setText(n.toString() + "%")
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
