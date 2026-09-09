package com.bongorian.signa1

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/** Every catalog value can follow the model or be fixed explicitly in the immutable draft. */
internal class AdvancedControls(val editor: EffectDialog, val id: Int) {
    val a: MainActivity
    var reference: FaultNode
    val values: MutableMap<String, TextView> = LinkedHashMap<String, TextView>()

    init {
        a = editor.a
        if (
            FaultParameters.all(id).stream().noneMatch { spec: FaultParameters.Spec? ->
                spec!!.group == editor.advancedGroup
            }
        )
            editor.advancedGroup = FaultParameters.Group.SIGNAL
        reference = FaultModel(0).inspect(id, editor.draft, editor.amount, a.faultConfig)
        a.shownLiveFrame?.let(::update)
    }

    fun update(frame: EffectState.Frame) {
        for (node in frame.nodes) if (node.id == id) {
            reference = node
            break
        }
        val current = reference.inspect()
        values.forEach { (key: String, view: TextView?) ->
            if (
                !editor.draft.manual(
                    id,
                    key,
                )
            )
                view!!.setText(SignalControls.value(current.get(key)!!))
        }
    }

    fun current(spec: FaultParameters.Spec): Float {
        return editor.draft.resolved(id, spec.key, reference.inspect().get(spec.key)!!)
    }

    fun refresh() {
        val y = editor.scroll!!.scrollY
        editor.renderBody()
        editor.scroll!!.post(Runnable@{ editor.scroll!!.scrollTo(0, y) })
    }

    fun show(body: LinearLayout) {
        val hint = a.text(a.getString(R.string.ui_advanced_hint), 11, MainActivity.MUTED)
        hint.setPadding(0, a.dp(8f), 0, a.dp(8f))
        body.addView(hint)
        val seed = SignalControls.field(a, body, "SEED · " + editor.draft.identity(id))
        seed.setTag("identity-seed")
        seed.setTextSize(11f)
        seed.setOnClickListener(
            OnClickListener@{ v: View? ->
                editor.auxiliary =
                    SignalSheet.number(
                        a,
                        "SEED",
                        a.getString(R.string.ui_seed_hint),
                        editor.draft.identity(id).toString(),
                        true,
                        Predicate@{ text: String ->
                            editor.draft = editor.draft.reseed(id, text!!.toLong())
                            editor.preview()
                            refresh()
                            true
                        },
                    )
            }
        )
        if (FaultParameters.incidents(id)) {
            val eventRow = a.row()
            val eventSeed =
                a.button(
                    "EVENT SEED · " +
                        (if (editor.draft.fixedEventIdentity(id))
                            editor.draft
                                .eventIdentity(
                                    id,
                                    0,
                                )
                                .toString()
                        else "AUTO")
                )
            val automatic = a.button("AUTO")
            eventSeed.setTag("event-identity")
            eventSeed.setTextSize(10f)
            eventRow.addView(eventSeed, LinearLayout.LayoutParams(0, a.dp(44f), 1f))
            eventRow.addView(automatic, LinearLayout.LayoutParams(a.dp(64f), a.dp(44f)))
            body.addView(eventRow)
            eventSeed.setOnClickListener(
                OnClickListener@{ v: View? ->
                    editor.auxiliary =
                        SignalSheet.number(
                            a,
                            "EVENT SEED",
                            a.getString(R.string.ui_event_seed_hint),
                            editor.draft.eventIdentity(id, reference.event.identity).toString(),
                            true,
                            Predicate@{ text: String ->
                                editor.draft = editor.draft.withEventIdentity(id, text!!.toLong())
                                editor.preview()
                                refresh()
                                true
                            },
                        )
                }
            )
            automatic.setOnClickListener(
                OnClickListener@{ v: View? ->
                    editor.draft = editor.draft.withEventIdentity(id, null as Long?)
                    editor.preview()
                    refresh()
                }
            )
        }
        val groups = HorizontalScrollView(a)
        groups.setHorizontalScrollBarEnabled(false)
        val row = a.row()
        groups.addView(row)
        body.addView(groups, LinearLayout.LayoutParams(-1, a.dp(48f)))
        for (group in FaultParameters.Group.entries) {
            if (
                FaultParameters.all(id).stream().noneMatch { spec: FaultParameters.Spec? ->
                    spec!!.group == group
                }
            )
                continue
            val button = a.button(a.getString(groupLabel(group)))
            button.setTag("group-" + group.name)
            button.setTextSize(10f)
            button.setTextColor(
                if (editor.advancedGroup == group) MainActivity.LIME else MainActivity.MUTED
            )
            val p = LinearLayout.LayoutParams(-2, a.dp(42f))
            p.rightMargin = a.dp(5f)
            row.addView(button, p)
            button.setOnClickListener(
                OnClickListener@{ v: View? ->
                    editor.advancedGroup = group
                    editor.renderBody()
                }
            )
        }
        for (spec in FaultParameters.all(id)) if (spec.group == editor.advancedGroup)
            parameter(
                body,
                spec,
            )
        val clear = SignalControls.field(a, body, a.getString(R.string.ui_advanced_auto_all))
        clear.setTag("advanced-auto-all")
        clear.setOnClickListener(
            OnClickListener@{ v: View? ->
                editor.draft = editor.draft.clearOverrides(id)
                editor.preview()
                refresh()
            }
        )
    }

    fun parameter(body: LinearLayout, spec: FaultParameters.Spec) {
        val manual = editor.draft.manual(id, spec.key)
        val row = a.row()
        val name = a.text(spec.key, 11, MainActivity.WHITE)
        val value = a.button(SignalControls.value(current(spec)))
        val mode = a.button(if (manual) "FIX" else "AUTO")
        row.addView(name, LinearLayout.LayoutParams(0, a.dp(44f), 1f))
        value.setTextSize(11f)
        value.setTextColor(MainActivity.LIME)
        value.setBackgroundColor(Color.TRANSPARENT)
        row.addView(value, LinearLayout.LayoutParams(a.dp(90f), a.dp(44f)))
        values.put(spec.key, value)
        mode.setTextSize(10f)
        mode.setTextColor(if (manual) MainActivity.LIME else MainActivity.MUTED)
        mode.setTag("auto-" + spec.key)
        row.addView(mode, LinearLayout.LayoutParams(a.dp(58f), a.dp(40f)))
        body.addView(row)
        mode.setContentDescription(
            a.getString(
                if (manual) R.string.ui_advanced_restore_auto else R.string.ui_advanced_fix
            ) + " · " + spec.key
        )
        mode.setOnClickListener(
            OnClickListener@{ v: View? ->
                editor.draft =
                    if (manual) editor.draft.automatic(id, spec.key)
                    else
                        editor.draft.override(
                            id,
                            spec.key,
                            max(spec.min, min(spec.max, current(spec))),
                        )
                editor.preview()
                refresh()
            }
        )
        value.setTag("value-" + spec.key)
        value.setOnClickListener(
            OnClickListener@{ v: View? ->
                editor.auxiliary =
                    SignalSheet.number(
                        a,
                        spec.key,
                        SignalControls.value(spec.min) + " … " + SignalControls.value(spec.max),
                        SignalControls.value(current(spec)),
                        spec.step >= 1,
                        Predicate@{ text: String ->
                            editor.draft = editor.draft.override(id, spec.key, text!!.toFloat())
                            editor.preview()
                            refresh()
                            true
                        },
                    )
            }
        )
        if (manual) {
            val slider = SeekBar(a)
            slider.setTag("advanced-" + spec.key)
            slider.setContentDescription(spec.key)
            slider.setMax(1000)
            slider.setProgress(
                Math.round((current(spec) - spec.min) / (spec.max - spec.min) * 1000)
            )
            slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME))
            slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME))
            body.addView(slider, LinearLayout.LayoutParams(-1, a.dp(40f)))
            slider.setOnSeekBarChangeListener(
                object : OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, n: Int, user: Boolean) {
                        if (user) {
                            var next = spec.min + (spec.max - spec.min) * n / 1000f
                            next =
                                max(
                                    spec.min,
                                    min(
                                        spec.max,
                                        spec.min +
                                            Math.round((next - spec.min) / spec.step) * spec.step,
                                    ),
                                )
                            editor.draft = editor.draft.override(id, spec.key, next)
                            value.setText(SignalControls.value(next))
                            editor.preview()
                        }
                    }

                    override fun onStartTrackingTouch(s: SeekBar?) {}

                    override fun onStopTrackingTouch(s: SeekBar?) {}
                }
            )
        }
    }

    companion object {
        fun groupLabel(group: FaultParameters.Group): Int {
            when (group) {
                FaultParameters.Group.TIME -> return R.string.ui_advanced_time
                FaultParameters.Group.EVENT -> return R.string.ui_advanced_event
                FaultParameters.Group.PROFILE -> return R.string.ui_advanced_profile
                else -> return R.string.ui_advanced_signal
            }
        }
    }
}
