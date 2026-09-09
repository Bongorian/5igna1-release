package com.bongorian.signa1

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** On-device comparison against the frozen renderer, using identical shader source and inputs. */
internal object FaultRenderChecks {
    private val identity = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f)

    private fun frame(state: EffectState, live: Boolean, time: Double): EffectState.Frame {
        val model = FaultModel(9981)
        val config = FaultConfig.defaults().enabled(live)
        val input = FaultModel.Inputs()
        model.advance(0.0, input, config)
        input.sensorNs = 1 + (time * 1e9).toLong()
        model.advance(time, input, config)
        return model.apply(state.snapshot(true, 0), config)
    }

    private fun fixture(w: Int, h: Int): Bitmap {
        val colors = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            (255 shl 24) or (((x * 13 + y * 3) and 255) shl 16) or
                (((y * 7 + x) and 255) shl 8) or (((x / 7 + y / 9) % 2) * 255)
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun pixels(target: SignalBuffer, bytes: ByteBuffer): ByteArray {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.fbo)
        bytes.clear()
        GLES20.glReadPixels(0, 0, target.width, target.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bytes)
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Comparison readback" }
        bytes.rewind()
        return ByteArray(target.width * target.height * 4).also { bytes.get(it) }
    }

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun thermal(context: Context): JSONObject {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        return JSONObject().put("status", context.getSystemService(PowerManager::class.java).currentThermalStatus)
            .put("batteryC", battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0)
            .put("plugged", battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
    }

    fun run(context: Context, benchmark: Boolean, transportOnly: Boolean = false, investigation: Boolean = false, gpuDetail: JSONObject? = null): String {
        val display = EglLease.acquire()
        var egl = EGL14.EGL_NO_CONTEXT
        var surface = EGL14.EGL_NO_SURFACE
        try {
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, intArrayOf(
                EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_NONE
            ),0,configs,0,1,count,0) && count[0] > 0)
            egl = EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            surface = EGL14.eglCreatePbufferSurface(display,configs[0],
                intArrayOf(EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE),0)
            check(EGL14.eglMakeCurrent(display,surface,surface,egl))
            val report = if (gpuDetail != null) GpuDetailInvestigation.run(context, gpuDetail) else if (investigation) LoadInvestigation.gpu(context) else if (transportOnly) transportCompare(context) else compare(context, benchmark)
            val directory = File(context.filesDir, "verification").also { it.mkdirs() }
            File(directory, "fault-render.json").writeText(report.toString(2))
            return report.toString()
        } finally {
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,surface)
            if (egl != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,egl)
            EglLease.release()
            EGL14.eglReleaseThread()
        }
    }

    private fun transportCompare(context: Context): JSONObject {
        val chain = EffectChain(PhotoRenderer.shaderSource(context), false)
        val source = SignalBuffer()
        val target = SignalBuffer()
        val w = 960; val h = 720
        val bytes = ByteBuffer.allocateDirect(w * h * 4)
        source.allocate(w, h); target.allocate(w, h)
        val image = fixture(w, h)
        fun upload(invert: Boolean) {
            val bitmap = if (!invert) image else Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.MAGENTA) }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, source.texture)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            if (invert) bitmap.recycle()
        }
        fun render(parameters: EffectParameters, mask: Int): ByteArray {
            val state = EffectState.defaults().edit(true, mask, parameters).amount(.8f)
            chain.render(source.texture, false, identity, frame(state, false, 0.0), w, h, w, h, target.fbo, target.texture)
            return pixels(target, bytes)
        }
        val defaults = EffectParameters.defaults()
        val media = 1 shl Effects.VHS
        val display = 1 shl Effects.CRT
        try {
            upload(false)
            val clean = render(defaults, 0)
            val digital = defaults.with(Effects.VHS, "transport", 2f / 3)
            check(clean.contentEquals(render(digital, media))) { "Digital media changed pixels" }
            check(clean.contentEquals(render(digital.with(Effects.CRT, "transport", 1f / 3), media or display))) { "Digital chain changed pixels" }
            val hashes = HashSet<String>()
            for (m in 0..3) for (d in 0..3) {
                val parameters = defaults.with(Effects.VHS, "transport", m / 3f).with(Effects.VHS, "reduce", 1f)
                    .with(Effects.CRT, "transport", d / 3f)
                hashes.add(hash(render(parameters, media or display)))
            }
            check(hashes.size >= 12) { "Transport profiles produced indistinguishable frames" }
            for (m in 0..1) {
                val parameters = defaults.with(Effects.VHS, "transport", m / 3f)
                check(!render(parameters, media).contentEquals(render(parameters.with(Effects.VHS, "reduce", 1f), media))) { "Media reduction did not change pixels" }
            }
            val analog = defaults.with(Effects.VHS, "transport", 1f).with(Effects.CRT, "transport", 1f / 3)
            check(!render(analog, media or display).contentEquals(render(analog.with(Effects.VHS, "cable", 1f), media or display))) { "Composite/component match" }
            check(!render(analog, media or display).contentEquals(render(analog.with(Effects.CRT, "upconvert", 0f), media or display))) { "Upsampling choices match" }
            val network = defaults.with(Effects.CRT, "transport", 2f / 3).override(Effects.CRT, "networkStall", 0f)
            val before = render(network, display)
            upload(true)
            check(before.contentEquals(render(network.override(Effects.CRT, "networkStall", 1f), display))) { "Network stall did not retain preceding pixels" }
            check(!before.contentEquals(render(network, display))) { "Network did not resume" }
            val solid = render(defaults, 0)
            val led = defaults.with(Effects.CRT, "transport", 1f).with(Effects.CRT, "scan", 1f)
                .with(Effects.CRT, "convergence", 0f).with(Effects.CRT, "sync", 0f)
            check(solid.contentEquals(render(led, display))) { "LED added element-gap lines" }
            checkTransportFaultGeometry(context)
            upload(false)
            // Reuse the same specialized programs after switching away from transport models.
            chain.releaseBuffers()
            check(clean.contentEquals(render(digital, media)))
            return JSONObject().put("result", "PASS digital identity, 16 media/display combinations, media resolution, cable models, upconversion, network frame hold/resume, noisy payload corruption and LIVE timing, square LED modules in both aspect ratios without black gaps")
                .put("uniqueFrames", hashes.size)
        } finally {
            image.recycle(); chain.release(); source.release(); target.release()
        }
    }

    private fun checkTransportFaultGeometry(context: Context) {
        val chain = EffectChain(PhotoRenderer.shaderSource(context), false)
        val source = SignalBuffer(); val target = SignalBuffer()
        try {
            for ((w,h) in listOf(960 to 720, 720 to 960)) {
                source.allocate(w,h); target.allocate(w,h)
                val bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,source.texture)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0); bitmap.recycle()
                fun render(kind: Float, live: Boolean = false, time: Double = 0.0): ByteArray {
                    val p = EffectParameters.defaults().with(Effects.CRT,"transport",kind)
                        .override(Effects.CRT,"transportDamage",1f).override(Effects.CRT,"transportLoss",0f)
                        .override(Effects.CRT,"refreshBand",0f).override(Effects.CRT,"networkStall",0f)
                    val state=EffectState.defaults().edit(true,1 shl Effects.CRT,p).amount(1f)
                    chain.render(source.texture,false,identity,frame(state,live,time),w,h,w,h,target.fbo,target.texture)
                    return pixels(target,ByteBuffer.allocateDirect(w*h*4))
                }
                val led=render(1f)
                check(led.contentEquals(render(1f))) { "Fixed LED seed changed" }
                val module=(minOf(w,h)/180).coerceAtLeast(1)*8
                var failures=0
                for (y in 0 until h step module) for (x in 0 until w step module) {
                    val value=led[(y*w+x)*4].toInt() and 255
                    if(value<128) failures++
                    for (dy in 0 until minOf(module,h-y)) for (dx in 0 until minOf(module,w-x)) {
                        check(kotlin.math.abs((led[((y+dy)*w+x+dx)*4].toInt() and 255)-value)<=1) { "LED module is not a coherent square at $w x $h" }
                    }
                }
                check(failures>0) { "LED has no failed modules" }
                val network=render(2f/3)
                check(network.contentEquals(render(2f/3))) { "Fixed NETWORK seed changed" }
                check(!render(2f/3, true, .7).contentEquals(render(2f/3, true, 4.2))) { "LIVE ON did not advance NETWORK noise" }
                val values=network.indices.filter { it%4!=3 }.map { network[it].toInt() and 255 }.toSet()
                check(values.size>100) { "NETWORK corruption is flat blocks instead of noise" }
                check(network.count { (it.toInt() and 255)<240 }>w*h/4) { "NETWORK corruption missing" }
            }
        } finally { chain.release(); source.release(); target.release() }
    }

    private fun compare(context: Context, benchmark: Boolean): JSONObject {
        val source = PhotoRenderer.shaderSource(context)
        var old = EffectChainReference(source, true)
        var current = EffectChain(source, true)
        val beforeTarget = SignalBuffer()
        val afterTarget = SignalBuffer()
        val texture = IntArray(2)
        GLES20.glGenTextures(2, texture, 0)
        for (i in 0..1) {
            val target = if (i == 0) GLES20.GL_TEXTURE_2D else GLES11Ext.GL_TEXTURE_EXTERNAL_OES
            GLES20.glBindTexture(target, texture[i])
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        val image = fixture(128, 96)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0)
        val stream = SurfaceTexture(texture[1]).also { it.setDefaultBufferSize(128, 96) }
        val producer = Surface(stream)
        val canvas = producer.lockCanvas(null)
        canvas.drawBitmap(image, 0f, 0f, null)
        producer.unlockCanvasAndPost(canvas)
        stream.updateTexImage()
        val externalMatrix = FloatArray(16).also(stream::getTransformMatrix)
        image.recycle()
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteBuffer.allocateDirect(192 * 144 * 4)
        var comparisons = 0
        val matrix = identity.clone()
        fun checkFrame(state: EffectState, live: Boolean, time: Double, oes: Boolean, label: String) {
            val f = frame(state, live, time)
            // Alternate target allocation, source dimensions and mutations of the same matrix object.
            val w = if (comparisons % 3 == 0) 97 else 128
            val h = if (comparisons % 3 == 0) 65 else 96
            val sw = if (comparisons % 5 == 0) 192 else 128
            val sh = if (comparisons % 5 == 0) 144 else 96
            (if (oes) externalMatrix else identity).copyInto(matrix)
            if (comparisons % 4 == 0) { matrix[0] = -matrix[0]; matrix[12] = 1f - matrix[12] }
            beforeTarget.allocate(w, h)
            afterTarget.allocate(w, h)
            old.render(texture[if (oes) 1 else 0],oes,matrix,f,w,h,sw,sh,beforeTarget.fbo)
            val expected = pixels(beforeTarget, buffer)
            current.render(texture[if (oes) 1 else 0],oes,matrix,f,w,h,sw,sh,afterTarget.fbo,afterTarget.texture)
            val actual = pixels(afterTarget, buffer)
            if (!expected.contentEquals(actual)) {
                val mismatch = expected.indices.first { expected[it] != actual[it] }
                error("GPU bytes differ: $label oes=$oes live=$live time=$time byte=$mismatch")
            }
            if (f.nodes.size > 1) {
                val field = EffectChain::class.java.getDeclaredField("bufferCount").also { it.isAccessible = true }
                check(field.getInt(current) == 1) { "Chain did not reuse its output texture" }
            }
            digest.update(actual)
            comparisons++
        }
        try {
            for (oes in listOf(false, true)) {
                for (id in Effects.ORDER) {
                    for (level in floatArrayOf(0f, .25f, 1f))
                        for (live in listOf(false, true))
                            checkFrame(EffectState.defaults().single(id).amount(level),live,.77,oes,"id=$id level=$level")
                    for (control in Effects.CONTROLS[id]) {
                        // The frozen pre-transport renderer has no media buffers or profile reset.
                        // Profile controls are verified by transportCompare, not this legacy oracle.
                        if ((id == Effects.VHS && control.key in setOf("transport", "reduce", "cable")) ||
                            (id == Effects.CRT && control.key in setOf("transport", "upconvert"))) continue
                        val state = EffectState.defaults().single(id)
                        for (value in floatArrayOf(0f, 1f)) {
                            val changed = state.edit(false,state.mask,state.parameters().with(id,control.key,value))
                            checkFrame(changed,true,2.31,oes,"id=$id ${control.key}=$value")
                        }
                    }
                }
                val masks = (0..13).map { length -> (1 shl (length + 1)) - 2 } +
                    listOf(16256, (1 shl 2) or (1 shl 8) or (1 shl 13))
                for (mask in masks)
                    for (time in doubleArrayOf(0.0,.1,1.2,4.9,.1))
                        checkFrame(EffectState.defaults().chain(mask),true,time,oes,"chain=$mask")
            }
            // Source/output aliasing must fall back to independent intermediates.
            for (mask in intArrayOf(6, 896, 16256, 16382)) {
                val w = 128
                val h = 96
                beforeTarget.allocate(w,h)
                afterTarget.allocate(w,h)
                val original = fixture(w,h)
                for (target in listOf(beforeTarget,afterTarget)) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,target.texture)
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,original,0)
                }
                original.recycle()
                val f = frame(EffectState.defaults().chain(mask),true,1.2)
                old.render(beforeTarget.texture,false,identity,f,w,h,w,h,beforeTarget.fbo)
                val expected = pixels(beforeTarget,buffer)
                current.render(afterTarget.texture,false,identity,f,w,h,w,h,afterTarget.fbo,afterTarget.texture)
                val actual = pixels(afterTarget,buffer)
                check(expected.contentEquals(actual)) { "Aliased input/target changed: $mask" }
                digest.update(actual)
                comparisons++
                // No target-texture opt-in keeps the original private-buffer route available.
                old.render(texture[0],false,identity,f,w,h,w,h,beforeTarget.fbo)
                val separate = pixels(beforeTarget,buffer)
                current.render(texture[0],false,identity,f,w,h,w,h,afterTarget.fbo)
                val fallback = pixels(afterTarget,buffer)
                check(separate.contentEquals(fallback)) { "Private-buffer fallback changed: $mask" }
                digest.update(fallback)
                comparisons++
            }
            // Release/recreate both caches in the same context and verify their first draw again.
            old.release()
            current.release()
            old = EffectChainReference(source, true)
            current = EffectChain(source, true)
            checkFrame(EffectState.defaults().chain(16256),true,1.2,false,"recreated")
            val report = JSONObject().put("comparisons",comparisons)
                .put("comparisonSha256",digest.digest().joinToString("") { "%02x".format(it) })
                .put("shaderSha256",hash(source.toByteArray()))
                .put("vendor",GLES20.glGetString(GLES20.GL_VENDOR))
                .put("renderer",GLES20.glGetString(GLES20.GL_RENDERER))
                .put("version",GLES20.glGetString(GLES20.GL_VERSION))
            if (benchmark) {
                val w = 1920
                val h = 1080
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture[0])
                val large = fixture(w,h)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,large,0)
                large.recycle()
                beforeTarget.allocate(w,h)
                afterTarget.allocate(w,h)
                val readback = ByteBuffer.allocateDirect(w*h*4)
                val records = JSONArray()
                val workloads = (7..13).map { Triple(1 shl it,true,Effects.name(it)) } + listOf(
                    Triple(16256,false,"Downstream seven / fixed"),
                    Triple(16256,true,"Downstream seven / LIVE"),
                    Triple(16382,true,"All thirteen / LIVE"),
                )
                for ((mask, live, name) in workloads) {
                    val state = EffectState.defaults().chain(mask)
                    val frames = (0..<90).map { frame(state,live,if (live) it / 30.0 else 1.2) }
                    var expectedHash: String? = null
                    fun draw(optimized: Boolean, index: Int) {
                        val target = if (optimized) afterTarget else beforeTarget
                        if (optimized) current.render(texture[0],false,identity,frames[index],w,h,w,h,target.fbo,target.texture)
                        else old.render(texture[0],false,identity,frames[index],w,h,w,h,target.fbo)
                    }
                    for (round in 1..3) {
                        for (i in 0..<30) for (optimized in listOf(false,true)) {
                            draw(optimized,i)
                            GLES20.glFinish()
                        }
                        val walls = arrayOf(JSONArray(),JSONArray())
                        val cpus = arrayOf(JSONArray(),JSONArray())
                        for (i in 30..<90) {
                            // Counterbalance each adjacent pair to reduce clock/governor drift.
                            for (position in 0..1) {
                                val variant = (i + round + position) % 2
                                val started = System.nanoTime()
                                val cpuStarted = Debug.threadCpuTimeNanos()
                                draw(variant == 1,i)
                                val cpuElapsed = Debug.threadCpuTimeNanos() - cpuStarted
                                GLES20.glFinish()
                                walls[variant].put((System.nanoTime()-started)/1e6)
                                cpus[variant].put(cpuElapsed/1e6)
                            }
                        }
                        for (variant in 0..1) {
                            val target = if (variant == 1) afterTarget else beforeTarget
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,target.fbo)
                            readback.clear()
                            GLES20.glReadPixels(0,0,w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,readback)
                            readback.rewind()
                            val outputDigest = MessageDigest.getInstance("SHA-256")
                            outputDigest.update(readback)
                            val outputHash = outputDigest.digest().joinToString("") { "%02x".format(it) }
                            if (expectedHash == null) expectedHash = outputHash
                            check(expectedHash == outputHash) { "Benchmark output changed: $name" }
                            records.put(JSONObject().put("name",name).put("mask",mask).put("live",live)
                                .put("round",round).put("variant",if (variant == 1) "after" else "before")
                                .put("wallMillis",walls[variant]).put("submissionCpuMillis",cpus[variant])
                                .put("sha256",outputHash).put("thermal",thermal(context)))
                        }
                    }
                }
                report.put("width",w).put("height",h).put("benchmark",records)
            }
            return report
        } finally {
            beforeTarget.release()
            afterTarget.release()
            old.release()
            current.release()
            producer.release()
            stream.release()
            GLES20.glDeleteTextures(2,texture,0)
        }
    }
}
