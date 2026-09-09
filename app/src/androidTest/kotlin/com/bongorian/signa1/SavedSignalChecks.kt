package com.bongorian.signa1

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File

internal object SavedSignalChecks {
    fun run(test: DeviceChecks): String {
        val a=test.activity!!
        val original=a.effectState
        val settings=CaptureSettings(a.settings)
        val fixture=File(a.cacheDir,"saved-signal-check.jpg")
        val bitmap=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(44,67,89))
        fixture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
        val expected=EffectState.defaults().chain((1 shl Effects.BIT_ERROR) or (1 shl Effects.CRT)).amount(.72f)
            .edit(true,(1 shl Effects.BIT_ERROR) or (1 shl Effects.CRT),EffectParameters.defaults()
                .reseed(Effects.BIT_ERROR,778899L).override(Effects.CRT,"phase",.25f))
        val frame=expected.snapshot(true,0)
        PhotoMetadata.write(fixture,null,null,System.currentTimeMillis(),320,240,null,
            "5igna1 1.6.1 | ${Effects.chainName(frame.ids())} | ${frame.describe()} | Processed RGB capture")
        val bytes=fixture.readBytes()
        val exif=android.media.ExifInterface(fixture)
        val comment=exif.getAttribute(android.media.ExifInterface.TAG_USER_COMMENT)
        val desc=exif.getAttribute(android.media.ExifInterface.TAG_IMAGE_DESCRIPTION)
        check(SavedSignal.read(comment)?.state != null || SavedSignal.read(desc)?.state != null) { "EXIF parse: comment=$comment description=$desc" }
        var viewer: MediaPreview?=null
        fun find(v: View, label: String): TextView? {
            if(v is TextView && v.text.toString()==label) return v
            if(v is ViewGroup) for(i in 0 until v.childCount) find(v.getChildAt(i),label)?.let { return it }
            return null
        }
        try {
            test.runOnMainSync { viewer=MediaPreview(a,Uri.fromFile(fixture),false);viewer!!.show() }
            test.await("saved chain metadata",{viewer!!.savedSignal?.state!=null},15000)
            check(viewer!!.savedSignal!!.state!!.encode()==expected.encode())
            check(a.effectState.encode()==original.encode()) { "Viewing changed settings" }
            SystemClock.sleep(400)
            test.languageScreenshot("saved-signal-footer")
            test.runOnMainSync { viewer!!.signal.performClick() }
            SystemClock.sleep(500)
            test.languageScreenshot("saved-signal-details")
            test.runOnMainSync {
                val apply=find(viewer!!.signalDialog!!.window!!.decorView,a.getString(R.string.saved_signal_use))!!
                apply.performClick()
            }
            check(a.effectState.encode()==expected.encode())
            check(EffectStateStore.load(a.getSharedPreferences("signal",0)).encode()==expected.encode())
            check(a.settings.photoSize==settings.photoSize && a.settings.videoKey==settings.videoKey && a.settings.photoFormat==settings.photoFormat)
            check(fixture.readBytes().contentEquals(bytes)) { "Modified source photo" }
            check(a.mediaPreview===viewer && !a.engine.attached)
            test.runOnMainSync {
                viewer!!.items.add(MediaPreview.Item(Uri.fromFile(File(a.cacheDir,"missing-signal.jpg")),false,false,0,"missing"))
                viewer!!.index=viewer!!.items.lastIndex;viewer!!.showItem()
            }
            test.await("missing metadata",{viewer!!.signal.text.toString()==a.getString(R.string.saved_signal_missing)},10000)
            check(viewer!!.savedSignal==null && !viewer!!.signal.isEnabled)
            return "PASS existing JPEG EXIF chain/LEVEL/seed/overrides, explicit apply and persistence, unchanged photo/format/size, gallery camera remains closed, navigation clears stale metadata"
        } finally {
            test.runOnMainSync { viewer?.dismiss();a.commitEffects(original) }
            fixture.delete()
        }
    }
}
