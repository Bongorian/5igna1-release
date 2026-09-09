package com.bongorian.signa1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer

internal object LightModeChecks {
    private val identity = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f)
    private fun matrix(e: GlitchEngine) = GlitchEngine::class.java.getDeclaredField("orientedMatrix").apply { isAccessible=true }.get(e) as FloatArray
    private fun read(b: SignalBuffer): ByteArray {
        val data=ByteBuffer.allocateDirect(b.width*b.height*4)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,b.fbo)
        GLES20.glReadPixels(0,0,b.width,b.height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,data)
        return ByteArray(data.capacity()).also{data.rewind();data.get(it)}
    }
    fun run(t: DeviceChecks): JSONObject {
        val a=t.activity!!;val e=a.engine
        val result=JSONObject();val saved=ArrayList<Uri>()
        val beforeLatest=a.latest;val beforeLatestVideo=a.latestVideo
        val prefs=a.getSharedPreferences("signal",0)
        var q:QualityDialog?=null
        try {
            t.runOnMainSync {
                a.videoMode=false
                a.applyFaultConfig(FaultConfig.defaults().enabled(false))
                a.applySettings(CaptureSettings(a.settings).apply {
                    lightMode=false;advancedMode=true;expertMode=true;photoFormat=0;photoSize="max"
                    experimentalSignals=false;rawVideo=false;location=false
                })
                a.commitEffects(EffectState.defaults().single(Effects.CFA_ERROR))
                q=QualityDialog(a).also{it.show()}
                q!!.content!!.findViewWithTag<View>("light-mode").performClick()
                check(a.settings.lightMode&&!a.settings.advancedMode&&!a.settings.expertMode&&!a.advancedMode)
                check(CaptureSettings.load(prefs).lightMode)
                q!!.content!!.findViewWithTag<View>("advanced-mode").performClick()
                check(!a.settings.lightMode&&a.settings.advancedMode)
                q!!.content!!.findViewWithTag<View>("light-mode").performClick()
                q!!.content!!.findViewWithTag<View>("expert-mode").performClick()
                check(!a.settings.lightMode&&a.settings.expertMode)
                q!!.content!!.findViewWithTag<View>("light-mode").performClick()
                check(a.settings.lightMode&&!a.settings.expertMode&&!a.advancedMode)

            }
            t.saveUi("light-mode-settings.png")
            t.runOnMainSync {q!!.sheet!!.dismiss();q=null}
            t.await("LIGHT preview",{a.ready&&e.settings.lightMode&&e.encoderScratch.frame!=null},25000)
            t.glSync {
                val expected=PreviewSizing.choose(e.signalW,e.signalH,e.width,e.height,true,false,false)
                check(e.encoderScratch.width==expected.width&&e.encoderScratch.height==expected.height)
                check(e.encoderScratch.width<=e.width&&e.encoderScratch.height<=e.height)
                check(e.presentedFrames.values().all{it.texture==0})
                result.put("preview",JSONObject().put("width",e.encoderScratch.width).put("height",e.encoderScratch.height)
                    .put("viewWidth",e.width).put("viewHeight",e.height).put("outputWidth",e.outW).put("outputHeight",e.outH))
                // Live external camera input: the one-shot result must match an independent full-size render.
                val shot=SignalBuffer();val reference=SignalBuffer()
                val chain=TransportCopyReference(PhotoRenderer.shaderSource(a),true)
                try {
                    e.renderLightPhoto(shot);reference.allocate(e.outW,e.outH)
                    chain.render(e.texture,true,matrix(e),shot.frame!!,e.outW,e.outH,e.signalW,e.signalH,reference.fbo,reference.texture)
                    check(read(shot).contentEquals(read(reference))) { "LIGHT shot changed full-resolution processing" }
                    result.put("fullResolutionCameraByteMatch",true)
                } finally {shot.release();reference.release();chain.release()}
            }
            // Actual save goes through the full-resolution one-shot path, not bitmap upscaling.
            val before=a.latest;val ow=e.outW;val oh=e.outH
            e.photo()
            t.await("LIGHT JPEG saved",{a.latest!=before&&!e.photoBusy},30000)
            val uri=a.latest!!;saved.add(uri)
            a.contentResolver.openInputStream(uri).use {
                val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeStream(it,null,opts)
                check(opts.outWidth==ow&&opts.outHeight==oh){"Saved LIGHT JPEG ${opts.outWidth}x${opts.outHeight}, expected ${ow}x${oh}"}
                result.put("savedJpeg",JSONObject().put("width",opts.outWidth).put("height",opts.outHeight))
            }
            val vw=e.width;val vh=e.height
            try {
                val frames=e.renderedFrames
                e.resize(vw/2,vh/2)
                t.await("resized LIGHT",{e.renderedFrames>frames&&e.encoderScratch.width<=vw/2&&e.encoderScratch.height<=vh/2},10000)
                check(e.outW==ow&&e.outH==oh)
                result.put("resizePreservesOutput",true)
            } finally {e.resize(vw,vh)}
            result.put("ledComparisons",led(t))
            result.put("ledTiming",ledTiming(t))
            t.runOnMainSync {
                a.commitEffects(EffectState.defaults().edit(true,1 shl Effects.CRT,
                    EffectParameters.defaults().with(Effects.CRT,"transport",2f/3)))
            }
            t.await("NETWORK full retained resolution",{e.encoderScratch.frame?.nodes?.any{it.id==Effects.CRT&&(it.profile["transportKind"]?:0f)==2f}==true&&e.encoderScratch.width==e.signalW},12000)
            result.put("networkFullResolution",true)
            t.runOnMainSync {
                a.videoMode=true
                a.applySettings(CaptureSettings(a.settings).apply{videoKey="recommended";rawVideo=false;lightMode=true})
                a.commitEffects(EffectState.defaults().single(Effects.CRT))
            }
            t.await("LIGHT video ready",{a.ready&&e.videoMode&&e.settings.lightMode},20000)
            val videoW=e.outW;val videoH=e.outH;val videoBefore=a.latest
            e.toggleVideo(false)
            t.await("LIGHT recording full resolution",{e.recording&&e.encoderScratch.width==e.signalW&&e.encoderScratch.height==e.signalH},15000)
            android.os.SystemClock.sleep(1200)
            e.toggleVideo(false)
            t.await("LIGHT video saved",{!e.recording&&a.latest!=videoBefore},20000)
            val video=a.latest!!;saved.add(video)
            MediaMetadataRetriever().use { m ->
                m.setDataSource(a,video)
                val w=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val h=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                check(w==videoW&&h==videoH){"Video dimensions $w x $h, expected $videoW x $videoH"}
                result.put("savedVideo",JSONObject().put("width",w).put("height",h))
            }
            t.await("LIGHT resumes after video",{e.encoderScratch.width<=e.width&&e.encoderScratch.height<=e.height},10000)
            if (a.cameraOptions!!.raws.isNotEmpty()) {
                t.runOnMainSync {
                    a.videoMode=false
                    a.applySettings(CaptureSettings(a.settings).apply{photoFormat=2;photoSize="recommended";lightMode=true})
                    a.commitEffects(EffectState.defaults().single(Effects.CFA_ERROR))
                }
                t.await("LIGHT RAW preview",{a.ready&&!e.videoMode&&e.settings.photoFormat==2},25000)
                val rawBefore=a.latest
                val rawW=e.photoChoice!!.size.width;val rawH=e.photoChoice!!.size.height
                e.photo()
                t.await("LIGHT RAW saved",{a.latest!=rawBefore&&!e.photoBusy},40000)
                val raw=a.latest!!;saved.add(raw)
                a.contentResolver.openInputStream(raw).use {
                    val exif=android.media.ExifInterface(requireNotNull(it))
                    val w=exif.getAttributeInt(android.media.ExifInterface.TAG_IMAGE_WIDTH,0)
                    val h=exif.getAttributeInt(android.media.ExifInterface.TAG_IMAGE_LENGTH,0)
                    check(w==rawW&&h==rawH){"LIGHT RAW dimensions $w x $h, expected $rawW x $rawH"}
                    result.put("savedRaw",JSONObject().put("width",w).put("height",h))
                }
            }
            t.runOnMainSync {
                a.videoMode=false
                a.applySettings(CaptureSettings(a.settings).apply{advancedMode=true})
            }
            t.await("ADVANCED full preview",{a.ready&&!e.settings.lightMode&&e.settings.advancedMode&&e.presentedFrames.acknowledged()>0},25000)
            t.glSync {check(e.presentedFrames.values().any{it.width==e.signalW&&it.height==e.signalH})}
            result.put("advancedPreserved",true)
            return result
        } finally {
            t.runOnMainSync{q?.sheet?.dismiss()}
            if(e.recording){e.toggleVideo(false);t.await("test recording stopped",{!e.recording},15000)}
            saved.forEach{a.contentResolver.delete(it,null,null)}
            t.runOnMainSync {
                a.latest=beforeLatest;a.latestVideo=beforeLatestVideo;a.savePrefs();a.galleryButton.load(beforeLatest,beforeLatestVideo)
            }
        }
    }
    private fun ledTiming(t: DeviceChecks): JSONArray {
        val records=JSONArray();val a=t.activity!!;val e=a.engine
        t.glSync {
            e.current(e.window)
            val current=EffectChain(PhotoRenderer.shaderSource(a),true)
            val legacy=TransportCopyReference(PhotoRenderer.shaderSource(a),true)
            val out=Array(2){SignalBuffer()}
            try {
                out.forEach{it.allocate(1080,1920)}
                val f=e.faultFrame(EffectState.defaults().edit(true,1 shl Effects.CRT,
                    EffectParameters.defaults().with(Effects.CRT,"transport",1f)).amount(.7f))
                fun draw(v:Int) {
                    if(v==0)legacy.render(e.texture,true,matrix(e),f,1080,1920,e.signalW,e.signalH,out[v].fbo,out[v].texture)
                    else current.render(e.texture,true,matrix(e),f,1080,1920,e.signalW,e.signalH,out[v].fbo,out[v].texture)
                }
                repeat(5){for(v in 0..1){draw(v);GLES20.glFinish()}}
                for(round in 0..3) {
                    val times=Array(2){JSONArray()}
                    repeat(20){i->for(pos in 0..1){val v=(i+pos+round)%2
                        val start=System.nanoTime();draw(v);GLES20.glFinish();times[v].put((System.nanoTime()-start)/1e6)
                    }}
                    check(read(out[0]).contentEquals(read(out[1])))
                    for(v in 0..1)records.put(JSONObject().put("round",round).put("variant",if(v==0)"reference-copy" else "current-direct")
                        .put("width",1080).put("height",1920).put("externalCamera",true).put("wallMs",times[v]))
                }
            } finally {current.release();legacy.release();out.forEach{it.release()}}
        }
        return records
    }
    private fun led(t: DeviceChecks): JSONArray {
        val a=t.activity!!;val e=a.engine;val results=JSONArray()
        for (external in listOf(true,false)) for (owned in listOf(true,false)) {
            t.glSync {
                e.current(e.window)
                val current=EffectChain(PhotoRenderer.shaderSource(a),true)
                val legacy=TransportCopyReference(PhotoRenderer.shaderSource(a),true)
                val input=SignalBuffer();val x=SignalBuffer();val y=SignalBuffer()
                try {
                    val w=480;val h=641
                    input.allocate(w,h);x.allocate(w,h);y.allocate(w,h)
                    val bitmap=Bitmap.createBitmap(IntArray(w*h){i->-0x1000000 or ((i*1103515245+12345)and 0xffffff)},w,h,Bitmap.Config.ARGB_8888)
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,input.texture);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);bitmap.recycle()
                    for(case in 0..11) {
                        val p=EffectParameters.defaults().with(Effects.CRT,"transport",1f).reseed(Effects.CRT,101L+case*173)
                            .with(Effects.VHS,"transport",if(case%2==0)1f else 2f/3)
                        val mask=(1 shl Effects.CRT) or (if(case%3==0)1 shl Effects.VHS else 0)
                        val f=e.faultFrame(EffectState.defaults().edit(true,mask,p).amount(case/11f))
                        val transform=if(external)matrix(e) else identity
                        val texture=if(external)e.texture else input.texture
                        val sw=if(external)e.signalW else w;val sh=if(external)e.signalH else h
                        current.render(texture,external,transform,f,w,h,sw,sh,x.fbo,if(owned)x.texture else 0)
                        legacy.render(texture,external,transform,f,w,h,sw,sh,y.fbo,if(owned)y.texture else 0)
                        check(read(x).contentEquals(read(y))){"LED mismatch external=$external owned=$owned case=$case"}
                        results.put(JSONObject().put("external",external).put("ownedTarget",owned).put("case",case).put("equal",true))
                    }
                    if(!external&&owned) {
                        // Aliased source/destination must retain the private intermediate path.
                        val f=e.faultFrame(EffectState.defaults().edit(true,1 shl Effects.CRT,EffectParameters.defaults().with(Effects.CRT,"transport",1f)))
                        val clean=e.cleanFrame;val copy=EffectChain(PhotoRenderer.shaderSource(a),false)
                        try {
                            copy.render(input.texture,false,identity,clean,w,h,w,h,x.fbo,x.texture)
                            copy.render(input.texture,false,identity,clean,w,h,w,h,y.fbo,y.texture)
                            current.render(x.texture,false,identity,f,w,h,w,h,x.fbo,x.texture)
                            legacy.render(y.texture,false,identity,f,w,h,w,h,y.fbo,y.texture)
                            check(read(x).contentEquals(read(y))){"Aliased LED target changed"}
                            results.put(JSONObject().put("alias",true).put("equal",true))
                        }finally{copy.release()}
                    }
                }finally{current.release();legacy.release();input.release();x.release();y.release()}
            }
        }
        return results
    }
}
