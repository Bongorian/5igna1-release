package com.bongorian.signa1

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES20
import android.os.Build
import android.os.SystemClock
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.math.max

/** Small foreground GL probe, cached by shader + GPU/driver/OS identity. No camera/media data. */
internal object GpuCalibration {
    private const val VERSION = 1
    data class Result(val vendor: String, val renderer: String, val driver: String, val key: String,
                      val measurement: GpuRecommendation.Measurement?, val origin: String)
    private val identity = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f)
    fun run(context: Context, shader: String, monitor: ThermalMonitor,
            prefs: SharedPreferences = context.getSharedPreferences("gpu-calibration",0)): Result {
        val vendor = GLES20.glGetString(GLES20.GL_VENDOR).orEmpty()
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER).orEmpty()
        val driver = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
        val key = MessageDigest.getInstance("SHA-256").digest("$VERSION|$vendor|$renderer|$driver|${Build.FINGERPRINT}|$shader".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val cached = runCatching {
          if (prefs.getString("key",null) == key) {
            val cached = GpuRecommendation.Measurement(prefs.getString("overhead",null)?.toDoubleOrNull() ?: Double.NaN,
                prefs.getString("perMp",null)?.toDoubleOrNull() ?: Double.NaN)
            cached.takeIf { it.valid() }
          } else null
        }.getOrNull()
        if (cached != null) return Result(vendor,renderer,driver,key,cached,"cached")
        monitor.sample(SystemClock.elapsedRealtime())
        if (monitor.status >= 2 || monitor.batteryC >= 40f)
            return Result(vendor,renderer,driver,key,null,"deferred-heat")
        val input = SignalBuffer();val out = SignalBuffer()
        var chain: EffectChain? = null
        try {
            val started = SystemClock.elapsedRealtime()
            val rendererChain = EffectChain(shader,false)
            chain = rendererChain
            val parameters = EffectParameters.defaults().override(Effects.CFA_ERROR,"cfaCoverage",1f)
                .override(Effects.DEMOSAIC_ERROR,"interpolationMix",1f)
            val base = EffectState.defaults().edit(true,(1 shl Effects.CFA_ERROR) or (1 shl Effects.DEMOSAIC_ERROR) or (1 shl Effects.CRT),parameters).amount(.7f)
            val model = FaultModel(9981);val config = FaultConfig.defaults().enabled(false)
            model.advance(1.2,FaultModel.Inputs(),config)
            val frame = model.apply(base.snapshot(true,0),config)
            val measured = ArrayList<Pair<Long,Double>>()
            for ((w,h) in listOf(384 to 216,768 to 432)) {
                // Soft time budget: a single driver call cannot be preempted. Do not run a long loop.
                if (SystemClock.elapsedRealtime()-started > 750) break
                input.allocate(w,h);out.allocate(w,h)
                val pixels = ByteBuffer.allocateDirect(w*h*4)
                repeat(w*h) { i -> pixels.put((i*13).toByte());pixels.put((i/7).toByte());pixels.put((i*37).toByte());pixels.put(255.toByte()) }
                pixels.rewind();GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,input.texture)
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels)
                fun draw() { rendererChain.render(input.texture,false,identity,frame,w,h,w,h,out.fbo,out.texture);GLES20.glFinish() }
                repeat(2) { draw() }
                val times = ArrayList<Double>()
                repeat(5) {
                    if (SystemClock.elapsedRealtime()-started <= 750) {
                        val ns = System.nanoTime();draw();times.add((System.nanoTime()-ns)/1e6)
                    }
                }
                if (times.size < 3) break
                times.sort();measured.add(w.toLong()*h to times[times.size/2])
            }
            if (measured.size != 2) return Result(vendor,renderer,driver,key,null,"budget-fallback")
            val (smallPixels,small) = measured[0];val (bigPixels,big) = measured[1]
            val perMp = max(.01,max((big-small)*1_000_000/(bigPixels-smallPixels),big*.35*1_000_000/bigPixels))
            val value = GpuRecommendation.Measurement(max(0.0,small-perMp*smallPixels/1_000_000),perMp)
            monitor.sample(SystemClock.elapsedRealtime())
            if (!value.valid() || monitor.status >= 2 || monitor.batteryC >= 40f)
                return Result(vendor,renderer,driver,key,null,"unstable-fallback")
            prefs.edit().putString("key",key).putString("overhead",value.overheadMs.toString())
                .putString("perMp",value.millisecondsPerMegapixel.toString()).apply()
            return Result(vendor,renderer,driver,key,value,"measured")
        } catch (failure: RuntimeException) {
            return Result(vendor,renderer,driver,key,null,"unavailable")
        } catch (failure: OutOfMemoryError) {
            return Result(vendor,renderer,driver,key,null,"memory-fallback")
        } finally {
            input.release();out.release();chain?.release()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)
        }
    }
}
