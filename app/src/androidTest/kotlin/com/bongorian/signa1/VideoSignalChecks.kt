package com.bongorian.signa1

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.security.MessageDigest

internal object VideoSignalChecks {
    fun run(test: DeviceChecks): String {
        val a=test.activity!!
        val initial=EffectState.defaults().single(Effects.CRT).amount(.37f)
            .edit(false,1 shl Effects.CRT,EffectParameters.defaults().reseed(Effects.CRT,7654321L).override(Effects.CRT,"phase",.25f))
        test.runOnMainSync {
            a.applySettings(CaptureSettings(a.settings).apply {experimentalSignals=false;advancedMode=false;expertMode=false;lightMode=false;rawVideo=false})
            a.applyFaultConfig(FaultConfig.defaults())
            a.commitEffects(initial)
            a.sound=false
        }
        test.await("camera ready",{a.ready && !a.tapMode},15000)
        test.runOnMainSync {a.setVideo(true)}
        test.await("video ready",{a.ready && a.videoMode},15000)
        val before=a.latest
        test.runOnMainSync {a.shoot()}
        test.await("recording",{a.engine.recording},10000)
        SystemClock.sleep(1000)
        test.runOnMainSync {a.commitEffects(initial.amount(.89f))}
        SystemClock.sleep(1000)
        test.runOnMainSync {a.shoot()}
        test.await("video saved",{!a.engine.recording && !a.engine.photoBusy && a.latest!=before},20000)
        val uri=a.latest!!
        val stored=VideoMetadata.read(a,uri)!!
        check(stored.state!!.encode()==initial.encode()) { "Recording-start state changed: ${stored.description}" }
        fun digest(): List<Byte> = a.contentResolver.openInputStream(uri)!!.use { input ->
            val hash=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536)
            while(true) {val n=input.read(buffer);if(n<0) break;hash.update(buffer,0,n)}
            hash.digest().toList()
        }
        val original=digest()
        var viewer: MediaPreview?=null
        fun find(v: View,label: String): TextView? {
            if(v is TextView && v.text.toString()==label) return v
            if(v is ViewGroup) for(i in 0 until v.childCount) find(v.getChildAt(i),label)?.let {return it}
            return null
        }
        try {
            test.runOnMainSync {viewer=MediaPreview(a,uri,true);viewer!!.show()}
            test.await("video signal metadata",{viewer!!.savedSignal?.state!=null},15000)
            test.await("video playback",{viewer!!.videoFrameSeen},15000)
            check(a.effectState.amount==.89f)
            test.runOnMainSync {viewer!!.signal.performClick()}
            test.languageScreenshot("video-signal-details")
            test.runOnMainSync {find(viewer!!.signalDialog!!.window!!.decorView,a.getString(R.string.saved_signal_use))!!.performClick()}
            check(a.effectState.encode()==initial.encode())
            check(EffectStateStore.load(a.getSharedPreferences("signal",0)).encode()==initial.encode())
            check(original==digest()) { "Viewer modified the video" }
        } finally {test.runOnMainSync {viewer?.dismiss()}}
        return "PASS camera MP4 recording-start chain/LEVEL/seed/overrides despite mid-recording edit; video playback, metadata display, explicit apply and persistence; original video unchanged"
    }
}
