package com.bongorian.signa1

import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.view.WindowManager
import android.view.View
import android.view.inspector.WindowInspector

/** Exercise real model pickers against deliberately delayed renderer snapshots. */
internal object EditorTransitionChecks {
    fun run(t: DeviceChecks): String {
        fun host() = t.activity!!
        val settings = CaptureSettings(host().settings)
        val state = host().effectState
        val orientation = host().requestedOrientation
        var dialog: EffectDialog? = null
        var selections = 0
        try {
            for (wide in listOf(false, true)) {
                val monitor = t.addMonitor(MainActivity::class.java.name, null, false)
                t.runOnMainSync {
                    host().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    host().requestedOrientation = if (wide) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                val end = SystemClock.elapsedRealtime() + 30000
                while (SystemClock.elapsedRealtime() < end) {
                    (monitor.waitForActivityWithTimeout(250) as? MainActivity)?.let { t.activity = it }
                    if (!host().isDestroyed && host().resumed && host().ready && host().cameraRoot.wide == wide) break
                }
                t.removeMonitor(monitor)
                t.await("orientation", { !host().isDestroyed && host().cameraRoot.wide == wide && host().ready }, 5000)
                t.runOnMainSync { host().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                for (advanced in listOf(false, true)) for (id in listOf(Effects.STREAM_ERROR, Effects.VHS, Effects.CRT)) {
                    t.runOnMainSync {
                        val a = host()
                        a.applySettings(CaptureSettings(a.settings).apply { photoFormat = 0; advancedMode = advanced; expertMode = false; lightMode = false })
                        a.commitEffects(EffectState.defaults().single(id).amount(1f))
                        val original = a.effectState.encode()
                        dialog = EffectDialog(a, true).also { it.show() }
                        val d = dialog!!
                        fun frame(p: EffectParameters) = EffectState.Frame(intArrayOf(id), d.amount, p, 0, 0.0,
                            listOf(FaultModel(0).inspect(id, p, d.amount, a.faultConfig.experimental(a.settings.experimentalSignals))),
                            experimental = a.settings.experimentalSignals, sourceEpoch = a.engine.generation.toLong())
                        val count = if (id == Effects.STREAM_ERROR) 2 else 4
                        for (choice in (0 until count).toList() + (0 until count).reversed()) {
                            val old = frame(d.draft)
                            a.shownLiveFrame = old
                            d.body!!.findViewWithTag<View>(if (id == Effects.STREAM_ERROR) "stream-model" else "transport-transport").performClick()
                            val picker = WindowInspector.getGlobalWindowViews().first { it.findViewWithTag<View>("choice-$choice") != null }
                            picker.findViewWithTag<View>("choice-$choice").performClick()
                            check(if (id == Effects.STREAM_ERROR) d.draft.analogFpv(id) == (choice == 1) else d.draft.transportKind(id) == choice)
                            if (advanced) {
                                for (group in FaultCapabilities.active(id, d.draft, a.settings.experimentalSignals).map { it.group }.distinct()) {
                                    d.body!!.findViewWithTag<View>("group-${group.name}").performClick()
                                    val controls = d.advancedControls!!
                                    controls.update(old)
                                    val fresh = frame(d.draft)
                                    controls.update(fresh)
                                    check(controls.reference === fresh.nodes.single()) { "Matching snapshot rejected" }
                                    val stale = frame(d.draft.reseed(id, d.draft.identity(id) + 1))
                                    controls.update(stale)
                                    check(controls.reference === fresh.nodes.single()) { "Stale seed accepted" }
                                }
                            }
                            if (advanced) {
                                d.body!!.findViewWithTag<View>("group-PROFILE").performClick()
                                val key = if (id == Effects.STREAM_ERROR) "streamKind" else "transportKind"
                                d.body!!.findViewWithTag<View>("auto-$key").performClick()
                                val bar = d.body!!.findViewWithTag<android.widget.SeekBar>("advanced-$key")
                                val args = android.os.Bundle().apply {
                                    putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, if (choice == 0) 1000f else 0f)
                                }
                                check(bar.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, args))
                                val changed = if (id == Effects.STREAM_ERROR) { if (d.draft.analogFpv(id)) 1 else 0 } else d.draft.transportKind(id)
                                check(changed == if (choice == 0) count - 1 else 0) { "Fixed model slider did not switch" }
                                d.advancedControls!!.update(frame(d.draft))
                            }
                            selections++
                        }
                        d.sheet!!.window!!.decorView.findViewWithTag<View>("chain-close").performClick()
                        check(a.effectState.encode() == original) { "Cancel committed draft" }
                        dialog = EffectDialog(a, true).also { it.show() }
                        val apply = dialog!!
                        apply.draft = apply.draft.reseed(id, 734L)
                        val expected = apply.state().encode()
                        apply.sheet!!.window!!.decorView.findViewWithTag<View>("apply").performClick()
                        check(a.effectState.encode() == expected) { "Apply lost draft" }
                    }
                    t.waitForIdleSync()
                }
            }
            val before = host().effectState.encode()
            val resumeMonitor = t.addMonitor(MainActivity::class.java.name, null, false)
            t.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
            t.await("background", { !host().resumed }, 10000)
            t.uiAutomation.executeShellCommand("am start -n ${t.targetContext.packageName}/${MainActivity::class.java.name}").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
            val resumeEnd = SystemClock.elapsedRealtime() + 30000
            while (SystemClock.elapsedRealtime() < resumeEnd) {
                (resumeMonitor.waitForActivityWithTimeout(250) as? MainActivity)?.let { t.activity = it }
                if (!host().isDestroyed && host().resumed && host().ready) break
            }
            t.removeMonitor(resumeMonitor)
            t.await("foreground camera", { !host().isDestroyed && host().resumed && host().ready }, 5000)
            check(host().effectState.encode() == before) { "Resume changed applied settings" }
            return "PASS $selections model selections, all advanced groups, stale seed rejection, normal/advanced, portrait/landscape, apply/cancel, background/resume"
        } finally {
            t.runOnMainSync {
                dialog?.sheet?.dismiss()
                host().commitEffects(state)
                host().applySettings(settings)
                host().requestedOrientation = orientation
            }
        }
    }
}
