package com.bongorian.signa1

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import java.io.File

internal object PhotoImportChecks {
    fun run(t: DeviceChecks): String {
        val a = t.activity!!
        val original = a.effectState
        val settings = CaptureSettings(a.settings)
        val file = File(a.cacheDir, "camera-requests/import-check.jpg").apply { parentFile!!.mkdirs() }
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(42, 69, 91))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
        val expected = EffectState.defaults().single(Effects.STREAM_ERROR).edit(false, 1 shl Effects.STREAM_ERROR,
            EffectParameters.defaults().with(Effects.STREAM_ERROR, "streamModel", 1f).reseed(Effects.STREAM_ERROR, 871L))
        val frame = expected.snapshot(false, 0)
        PhotoMetadata.write(file, null, null, 123L, 320, 240, null,
            "5igna1 test | ${Effects.chainName(frame.ids())} | ${frame.describe()} | Processed RGB capture")
        val bytes = file.readBytes()
        val uri = androidx.core.content.FileProvider.getUriForFile(a, a.packageName + ".camera-files", file)
        var monitor: Instrumentation.ActivityMonitor? = null
        try {
            t.runOnMainSync { a.openGallery() }
            t.await("gallery import entry", { a.mediaPreview?.panel?.findViewWithTag<View>("media-open-photo") != null }, 10000)
            monitor = t.addMonitor(IntentFilter(Intent.ACTION_GET_CONTENT).apply { addCategory(Intent.CATEGORY_OPENABLE); addDataType("image/*") },
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)), true)
            t.runOnMainSync { a.mediaPreview!!.panel.findViewWithTag<View>("media-open-photo").performClick() }
            t.await("received photo metadata", { a.mediaPreview?.imported == true && a.mediaPreview?.savedSignal?.state != null }, 15000)
            t.removeMonitor(monitor); monitor = null
            val viewer = a.mediaPreview!!
            check(viewer.items.size == 1)
            check(viewer.savedSignal!!.state!!.encode() == expected.encode())
            check(a.effectState.encode() == original.encode()) { "Opening applied settings" }
            t.runOnMainSync {
                viewer.signal.performClick()
                t.findText(viewer.signalDialog!!.window!!.decorView, a.getString(R.string.photo_use_settings))!!.performClick()
            }
            t.await("return to camera", { a.mediaPreview == null && a.ready }, 15000)
            check(a.effectState.encode() == expected.encode())
            check(a.settings.photoFormat == settings.photoFormat && a.settings.photoSize == settings.photoSize && a.settings.videoKey == settings.videoKey)
            check(file.readBytes().contentEquals(bytes)) { "Source photo was modified" }
            t.runOnMainSync {
                val feedback = FeedbackDialog.show(a)
                val root = feedback.window!!.decorView
                val copy = root.findViewWithTag<View>("feedback-diagnostics-copy")
                check(copy.visibility == View.GONE)
                root.findViewWithTag<View>("feedback-diagnostics").performClick()
                val text = root.findViewWithTag<TextView>("feedback-diagnostics-text").text.toString()
                check("GPU:" in text && "Analog FPV" in text && BuildConfig.VERSION_NAME in text)
                check(!text.contains(a.cacheDir.path) && !text.contains(uri.toString()))
                copy.performClick()
                check((a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString() == text)
                check(root.findViewWithTag<TextView>("feedback-message").text.isEmpty()) { "Diagnostics inserted into email" }
                feedback.dismiss()
            }
            return "PASS viewer import action, content URI result, photo metadata, explicit apply and camera return, unchanged media/capture settings, reviewed diagnostic copy without automatic email insertion"
        } finally {
            monitor?.let(t::removeMonitor)
            t.runOnMainSync { a.mediaPreview?.dialog?.dismiss(); a.feedbackDialog?.dismiss(); a.commitEffects(original); a.applySettings(settings) }
            file.delete()
        }
    }
}
