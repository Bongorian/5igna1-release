package com.bongorian.signa1

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

internal object TapAudioChecks {
    private fun audio(test: DeviceChecks, uri: Uri): Pair<MediaFormat,List<String>> {
        val ex=MediaExtractor()
        try {
            ex.setDataSource(test.targetContext,uri,null)
            val id=(0 until ex.trackCount).first { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
            val format=ex.getTrackFormat(id)
            ex.selectTrack(id)
            val hashes=ArrayList<String>()
            val buffer=ByteBuffer.allocate(1024*1024)
            while(ex.sampleTrackIndex>=0) {
                buffer.clear();val n=ex.readSampleData(buffer,0);if(n<0) break
                val bytes=ByteArray(n);buffer.position(0);buffer.get(bytes)
                hashes.add(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
                ex.advance()
            }
            return format to hashes
        } finally {ex.release()}
    }
    fun run(test: DeviceChecks): String {
        val a=test.activity!!
        val source=File(a.filesDir,"tap-audio-fixture.mp4")
        check(source.exists())
        val sourceUri=Uri.fromFile(source)
        val original=audio(test,sourceUri)
        val image=File(a.cacheDir,"tap-chain-check.jpg")
        val bitmap=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888)
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
        val selected=EffectState.defaults().chain((1 shl Effects.ROW_ERROR) or (1 shl Effects.CFA_ERROR) or (1 shl Effects.CRT))
            .amount(.42f)
        test.runOnMainSync {
            a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals=true;advancedMode=false;expertMode=false;lightMode=false;resolutionAudio=false })
            a.applyFaultConfig(FaultConfig.defaults())
            a.commitEffects(selected)
            a.enterTap(TapInput(Uri.fromFile(image),false))
        }
        test.await("image TAP",{a.ready && a.engine.tapSource?.ready==true},15000)
        check(a.effectState.encode()==selected.encode())
        test.runOnMainSync {
            check(a.selectedRoute.childCount==selected.ids().size)
            check(!a.effectAvailable(Effects.ROW_ERROR) && a.effectAvailable(Effects.CRT))
            check(a.tapPickerIntent().action==android.content.Intent.ACTION_GET_CONTENT)
            check(a.tapPickerIntent().getStringArrayExtra(android.content.Intent.EXTRA_MIME_TYPES)!!.toList()==listOf("image/*","video/*"))
            a.enterTap(TapInput(sourceUri,true))
        }
        test.await("video TAP audio ready",{a.ready && a.engine.tapSource?.hasAudio==true},15000)
        check(a.effectState.encode()==selected.encode())
        val before=a.latest
        test.runOnMainSync {a.shoot()}
        test.await("recording starts playback",{a.engine.recording && a.engine.tapSource!!.playing},10000)
        test.await("source end saves recording",{!a.engine.recording && !a.engine.photoBusy && a.latest!=before},30000)
        check(a.engine.tapSource!!.ended && !a.engine.tapSource!!.playing)
        check(VideoMetadata.read(a,a.latest!!)?.state != null) { "Missing signal after source audio passthrough" }
        val saved=a.latest!!
        val copy=audio(test,saved)
        val directory=File(a.filesDir,"verification").apply {mkdirs()}
        a.contentResolver.openInputStream(saved)!!.use { input -> File(directory,"tap-audio-copy.mp4").outputStream().use {input.copyTo(it)} }
        check(copy.first.getInteger(MediaFormat.KEY_CHANNEL_COUNT)==2)
        check(copy.first.getInteger(MediaFormat.KEY_SAMPLE_RATE)==48000)
        check(copy.second.size>100 && copy.second.all { it in original.second }) { "Source audio: copied=${copy.second.size} original=${original.second.size} unmatched=${copy.second.count { it !in original.second }} format=${copy.first}" }
        MediaMetadataRetriever().use { media ->
            media.setDataSource(a,saved)
            check(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() in 4300..5800)
        }
        val samples=copy.second.size
        // A fresh recording at the end replays from zero; the linked tier converts to mono AAC.
        test.runOnMainSync {a.applySettings(CaptureSettings(a.settings).apply {resolutionAudio=true})}
        test.await("linked TAP ready",{a.ready && a.engine.tapSource?.hasAudio==true},15000)
        val expected=RecordingAudio.supported(a.engine.outW,a.engine.outH,true)
        val beforeLinked=a.latest
        test.runOnMainSync {a.shoot()}
        test.await("linked records",{a.engine.recording && a.engine.tapSource!!.playing},10000)
        SystemClock.sleep(1000)
        test.runOnMainSync {a.tapPlay.performClick()}
        test.await("source pause",{!a.engine.tapSource!!.playing},3000)
        SystemClock.sleep(600)
        test.runOnMainSync {a.tapPlay.performClick()}
        test.await("linked end saved",{!a.engine.recording && !a.engine.photoBusy && a.latest!=beforeLinked},30000)
        check(VideoMetadata.read(a,a.latest!!)?.state != null) { "Missing signal after source audio conversion" }
        val converted=audio(test,a.latest!!)
        a.contentResolver.openInputStream(a.latest!!)!!.use { input -> File(directory,"tap-audio-linked.mp4").outputStream().use {input.copyTo(it)} }
        check(converted.first.getInteger(MediaFormat.KEY_CHANNEL_COUNT)==1)
        check(converted.first.getInteger(MediaFormat.KEY_SAMPLE_RATE)==expected.sampleRate)
        check(converted.first.getString(MediaFormat.KEY_MIME)==MediaFormat.MIMETYPE_AUDIO_AAC)
        check(converted.second.isNotEmpty())
        check(a.effectState.encode()==selected.encode())
        val beforeBackground=a.latest
        test.runOnMainSync {a.shoot()}
        test.await("replay after source end",{a.engine.recording && a.engine.tapSource!!.playing},10000)
        SystemClock.sleep(1200)
        test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME)
        test.await("background audio finalization",{!a.engine.recording && !a.engine.attached && a.latest!=beforeBackground},30000)
        check(audio(test,a.latest!!).second.isNotEmpty())
        test.uiAutomation.executeShellCommand("am start -f 0x20020000 -n ${a.packageName}/${MainActivity::class.java.name}").close()
        test.await("return after finalized audio",{a.resumed && a.ready},15000)
        check(!a.engine.recording)
        image.delete()
        return "PASS background source-audio finalization and replay after EOF; chain/image/video and inactive prefix visibility; media-provider picker intent; source end auto-stop/save; copied $samples original AAC packets (48kHz stereo); linked mono AAC ${expected.sampleRate}Hz/${expected.bitRate}bps with pause/resume"
    }
}
