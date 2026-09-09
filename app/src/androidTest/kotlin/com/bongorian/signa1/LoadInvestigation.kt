package com.bongorian.signa1

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.locks.LockSupport

/** Bounded developer experiments, excluded from application APKs. No thermal-savings claim. */
internal object LoadInvestigation {
    private val identity = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f)
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun thermal(c: Context): JSONObject {
        val b=c.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        return JSONObject().put("status",c.getSystemService(PowerManager::class.java).currentThermalStatus)
            .put("batteryC",b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0)
            .put("plugged",b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0))
    }
    private fun frame(mask: Int, p: EffectParameters = EffectParameters.defaults()): EffectState.Frame {
        val base=EffectState.defaults().edit(true,mask,p).amount(.7f).snapshot(true,0)
        val model=FaultModel(9981)
        val config=FaultConfig.defaults().enabled(false).experimental(true)
        model.advance(1.2,FaultModel.Inputs(),config)
        return model.apply(EffectState.Frame(base.ids(),base.amount,base.parameters,1234567890,1.2,base.nodes,true),config)
    }
    fun gpu(context: Context): JSONObject {
        val source=SignalBuffer();val out=SignalBuffer();val proposed=SignalBuffer()
        val shader=PhotoRenderer.shaderSource(context)
        val current=EffectChain(shader,false);val probe=TransportTargetProbe(shader,false)
        val records=JSONArray()
        val p=EffectParameters.defaults().override(Effects.MOTION_BLUR,"blurX",.04f).override(Effects.MOTION_BLUR,"blurY",.02f)
            .override(Effects.THERMAL_NOISE,"noiseAmplitude",.12f).override(Effects.THERMAL_NOISE,"noiseGrain",8f)
            .override(Effects.SMEAR,"smearLength",.03f).override(Effects.SMEAR,"smearAmount",1f).override(Effects.SMEAR,"smearThreshold",.6f)
        val workloads=(0..16).map { Triple(Effects.name(it),if(it==0)0 else 1 shl it,p) } + listOf(
            Triple("All thirteen",16382,p),
            Triple("LED",1 shl Effects.CRT,p.with(Effects.CRT,"transport",1f)),
            Triple("Network",1 shl Effects.CRT,p.with(Effects.CRT,"transport",2f/3)),
            Triple("Analog to LED",(1 shl Effects.VHS) or (1 shl Effects.CRT),p.with(Effects.VHS,"transport",1f).with(Effects.CRT,"transport",1f)),
            Triple("DVD to CRT",(1 shl Effects.VHS) or (1 shl Effects.CRT),p.with(Effects.VHS,"transport",1f/3)),
        )
        try {
            for ((w,h) in listOf(1280 to 720,1920 to 1080)) {
                source.allocate(w,h);out.allocate(w,h);proposed.allocate(w,h)
                val colors=IntArray(w*h) { i -> -0x1000000 or (((i%w*13+i/w*3)and 255) shl 16) or (((i/w*7+i%w)and 255)shl 8) or ((i/7)and 255) }
                val bmp=Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,source.texture);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);bmp.recycle()
                val bytes=ByteBuffer.allocateDirect(w*h*4)
                fun read(target:SignalBuffer):ByteArray {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,target.fbo);bytes.clear()
                    GLES20.glReadPixels(0,0,w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes)
                    bytes.rewind();return ByteArray(w*h*4).also { bytes.get(it) }
                }
                for ((name,mask,params) in workloads) {
                    val f=frame(mask,params)
                    fun draw(variant:Int) { if(variant==0) current.render(source.texture,false,identity,f,w,h,w,h,out.fbo,out.texture)
                        else probe.render(source.texture,false,identity,f,w,h,w,h,proposed.fbo,proposed.texture) }
                    val variants=if(name in listOf("LED","Analog to LED","DVD to CRT")) 2 else 1
                    repeat(5){for(v in 0 until variants){draw(v);GLES20.glFinish()}}
                    for(round in 0..2) {
                        val walls=Array(variants){JSONArray()};val cpu=Array(variants){JSONArray()}
                        repeat(15){index -> for(pos in 0 until variants){
                            val v=(index+round+pos)%variants
                            val start=System.nanoTime();val thread=Debug.threadCpuTimeNanos();draw(v)
                            cpu[v].put((Debug.threadCpuTimeNanos()-thread)/1e6)
                            GLES20.glFinish();walls[v].put((System.nanoTime()-start)/1e6)
                        }}
                        val expected=read(out)
                        if(variants==2)check(expected.contentEquals(read(proposed))){"Output mismatch $name $w"}
                        for(v in 0 until variants)records.put(JSONObject().put("name",name).put("width",w).put("height",h)
                            .put("round",round).put("variant",if(v==0)"current" else "direct-final-target")
                            .put("wallMs",walls[v]).put("submissionCpuMs",cpu[v]).put("hash",hash(expected)).put("thermal",thermal(context)))
                    }
                }
            }
            check(GLES20.glGetError()==GLES20.GL_NO_ERROR)
            return JSONObject().put("gpu",GLES20.glGetString(GLES20.GL_RENDERER)).put("shaderHash",hash(shader.toByteArray())).put("records",records)
        } finally {source.release();out.release();proposed.release();current.release();probe.release()}
    }

    private fun cachedNoise(input:ByteArray,w:Int,h:Int,white:Int,black:Int,n:FaultNode):ByteArray {
        val out=ByteArray(input.size)
        val grain=(n.mechanism["noiseGrain"]?:1f).coerceAtLeast(1f)
        val seed=(n.mechanism["grainSeed"]?:0f).toRawBits().toLong() xor n.identity.seed
        val amplitude=n.mechanism["noiseAmplitude"]?:0f
        val range=(white-black).coerceAtLeast(1).toFloat()
        val cols=IntArray(w){(it/grain).toInt()};val cache=FloatArray(cols.last()+1)
        var lastRow=-1
        fun noise(x:Int,y:Int):Float {
            fun value(s:Long)=FaultModel.random(s xor FaultModel.mix((x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)))
            return value(seed)+value(seed+19)+value(seed+73)-1.5f
        }
        for(y in 0 until h){val row=(y/grain).toInt()
            if(row!=lastRow){for(x in cache.indices)cache[x]=noise(x,row);lastRow=row}
            for(x in 0 until w){val i=y*w+x
                val value=RawGlitch.read(input,i).toFloat()+cache[cols[x]]*amplitude*range
                RawGlitch.write(out,i,kotlin.math.round(value).toInt().coerceIn(0,white))
            }
        }
        return out
    }

    fun raw(test:DeviceChecks):String {
        val a=test.activity!!;val e=a.engine
        val next=CaptureSettings(a.settings).apply {photoFormat=0;photoSize="recommended";expertMode=true;advancedMode=false}
        val generation=e.generation
        test.runOnMainSync {a.videoMode=false;a.applySettings(next);a.commitEffects(EffectState.defaults().chain(16382).amount(.7f))}
        test.await("fixed preview",{e.generation>generation && a.ready},25000)
        SystemClock.sleep(2500)
        val idleFrames=e.renderedFrames;val idleStart=System.nanoTime()
        SystemClock.sleep(3000)
        val idleFps=(e.renderedFrames-idleFrames)*1e9/(System.nanoTime()-idleStart)
        val w=4080;val h=3072
        val input=ByteArray(w*h*2)
        for(i in 0 until w*h)RawGlitch.write(input,i,256+(i*17%3500))
        val records=JSONArray()
        val rawFrame=frame(126)
        val inputHash=hash(input)
        var expected:String?=null
        // ABBA order; every run has the same input and immutable nodes.
        for(mode in listOf("normal","cooperative","cooperative","normal")) {
            val before=thermal(a)
            val frameStart=e.renderedFrames
            val cpuStart=Debug.threadCpuTimeNanos();val start=System.nanoTime()
            var deadline=start+8_000_000;var waits=0
            val output=RawGlitch.chain(input,w,h,4095,256,rawFrame) {
                e.foregroundWork.await()
                if(mode=="cooperative" && System.nanoTime()>=deadline){LockSupport.parkNanos(2_000_000);waits++;deadline=System.nanoTime()+8_000_000}
            }
            val elapsed=(System.nanoTime()-start)/1e6
            val cpu=(Debug.threadCpuTimeNanos()-cpuStart)/1e6
            val frames=e.renderedFrames-frameStart
            val digest=hash(output)
            if(expected==null)expected=digest else check(expected==digest)
            records.put(JSONObject().put("variant",mode).put("wallMs",elapsed).put("workerCpuMs",cpu)
                .put("previewFrames",frames).put("previewFps",frames*1000.0/elapsed).put("waits",waits)
                .put("hash",digest).put("before",before).put("after",thermal(a)))
            SystemClock.sleep(1200)
        }
        check(inputHash==hash(input))
        val metadata=arrayOfNulls<android.hardware.camera2.TotalCaptureResult>(1)
        val characteristics=arrayOfNulls<android.hardware.camera2.CameraCharacteristics>(1)
        val ready=java.util.concurrent.CountDownLatch(1)
        e.gl.post { metadata[0]=e.signalMetadata.values.lastOrNull();characteristics[0]=e.characteristics;ready.countDown() }
        check(ready.await(10,java.util.concurrent.TimeUnit.SECONDS))
        test.runOnMainSync {e.detach()}
        val closed=java.util.concurrent.CountDownLatch(1);e.gl.post{closed.countDown()};check(closed.await(10,java.util.concurrent.TimeUnit.SECONDS))
        val stages=JSONArray()
        val small=input.copyOf(1920*1080*2)
        val parameters=EffectParameters.defaults().override(Effects.MOTION_BLUR,"blurX",.04f).override(Effects.MOTION_BLUR,"blurY",.02f)
            .override(Effects.THERMAL_NOISE,"noiseAmplitude",.12f).override(Effects.THERMAL_NOISE,"noiseGrain",8f)
            .override(Effects.SMEAR,"smearLength",.03f).override(Effects.SMEAR,"smearAmount",1f).override(Effects.SMEAR,"smearThreshold",.6f)
        for(id in listOf(1,2,3,4,5,6,14,15,16)) {
            val state=frame(1 shl id,parameters)
            repeat(2){RawGlitch.chain(small,1920,1080,4095,256,state)}
            for(round in 0..2){val start=System.nanoTime();val output=RawGlitch.chain(small,1920,1080,4095,256,state)
                val wall=(System.nanoTime()-start)/1e6
                stages.put(JSONObject().put("name",Effects.name(id)).put("round",round).put("wallMs",wall).put("hash",hash(output)))
            }
        }
        val cached=JSONArray()
        for(grain in listOf(1f,8f)) {
            val state=frame(1 shl Effects.THERMAL_NOISE,parameters.override(Effects.THERMAL_NOISE,"noiseGrain",grain))
            val node=state.nodes.single()
            val expected=RawGlitch.chain(small,1920,1080,4095,256,state)
            check(expected.contentEquals(cachedNoise(small,1920,1080,4095,256,node)))
            repeat(2){cachedNoise(small,1920,1080,4095,256,node)}
            for(round in 0..2)for(v in if(round%2==0)listOf(0,1)else listOf(1,0)){
                val start=System.nanoTime()
                val out=if(v==0)RawGlitch.chain(small,1920,1080,4095,256,state)else cachedNoise(small,1920,1080,4095,256,node)
                val wall=(System.nanoTime()-start)/1e6;check(expected.contentEquals(out))
                cached.put(JSONObject().put("grain",grain).put("round",round).put("variant",if(v==0)"current" else "cell-cache").put("wallMs",wall).put("hash",hash(out)))
            }
        }
        val dng=JSONArray()
        for(mode in listOf("discard","file","file","discard")) {
            val target=File(a.cacheDir,"load-synthetic.dng")
            val start=System.nanoTime()
            val stream=if(mode=="file")target.outputStream()else object:java.io.OutputStream(){override fun write(value:Int){};override fun write(b:ByteArray,off:Int,len:Int){}}
            try {stream.use {output -> android.hardware.camera2.DngCreator(requireNotNull(characteristics[0]),requireNotNull(metadata[0])).use {writer ->
                writer.writeByteBuffer(output,android.util.Size(w,h),ByteBuffer.wrap(input),0)
            }}
                dng.put(JSONObject().put("variant",mode).put("wallMs",(System.nanoTime()-start)/1e6).put("bytes",if(target.exists())target.length()else 0))
            } finally {target.delete()}
        }
        val report=JSONObject().put("width",w).put("height",h).put("previewWidth",e.signalW).put("previewHeight",e.signalH)
            .put("idlePreviewFps",idleFps).put("stages1920x1080",stages).put("noiseCache",cached).put("dngIsolated",dng)
            .put("scope","ABBA synthetic RAW alongside real preview; separate isolated stage and DNG tests").put("records",records)
        File(a.filesDir,"verification/load-raw.json").writeText(report.toString(2))
        return report.toString()
    }
}
