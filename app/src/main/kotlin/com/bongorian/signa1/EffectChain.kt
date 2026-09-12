package com.bongorian.signa1

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/** Ordered GPU passes. Each pass reads the previous result, never its own attachment. */
internal class EffectChain(private val source: String, private val supportsExternal: Boolean) {
    private class Scalar(val location: Int) {
        var initialized = false
        var bits = 0
    }

    private class Program(val id: Int) {
        val position = GLES20.glGetAttribLocation(id, "p")
        val sampler = GLES20.glGetUniformLocation(id, "cam")
        val matrix = GLES20.glGetUniformLocation(id, "st")
        val sourceSize = GLES20.glGetUniformLocation(id, "sourceSize")
        val scalars = HashMap<String, Scalar>()
        var initialized = false
        val transform = FloatArray(16)
        var sourceW = 0
        var sourceH = 0

        private fun bindScalars(values: Map<String, Float>) {
            for ((name, value) in values) {
                val scalar = scalars.getOrPut(name) { Scalar(GLES20.glGetUniformLocation(id, name)) }
                if (scalar.location < 0) continue
                val bits = value.toRawBits()
                if (!scalar.initialized || scalar.bits != bits) {
                    GLES20.glUniform1f(scalar.location, value)
                    scalar.bits = bits
                    scalar.initialized = true
                }
            }
        }

        fun bind(node: FaultNode?, matrixValue: FloatArray, w: Int, h: Int) {
            // Uniform values belong to this private GL program and survive program switches.
            if (!initialized) GLES20.glUniform1i(sampler, 0)
            if (!initialized || transform.indices.any {
                    transform[it].toRawBits() != matrixValue[it].toRawBits()
                }) {
                GLES20.glUniformMatrix4fv(matrix, 1, false, matrixValue, 0)
                matrixValue.copyInto(transform, endIndex = 16)
            }
            if (!initialized || sourceW != w || sourceH != h) {
                GLES20.glUniform2f(sourceSize, w.toFloat(), h.toFloat())
                sourceW = w
                sourceH = h
            }
            if (node != null) {
                // Retain profile-then-mechanism override order, including overlapping names.
                if (node.id == Effects.VHS || node.id == Effects.CRT) bindScalars(mapOf("transportKind" to (node.profile["transportKind"] ?: 0f)))
                bindScalars(node.profile)
                bindScalars(node.mechanism)
            }
            initialized = true
        }
    }

    private val programs = HashMap<Int, Program>()
    private val textures = IntArray(2)
    private val fbos = IntArray(2)
    private var bufferCount = 0
    private var width = 0
    private var height = 0
    private val vertices: FloatBuffer =
        ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()

    init {
        vertices.put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0)
        program(Effects.CLEAN, false)
        if (supportsExternal) program(Effects.CLEAN, true)
    }

    private fun program(fault: Int, oes: Boolean): Program {
        require(!(oes && !supportsExternal)) { "External signal unsupported" }
        val key = fault * 2 + (if (oes) 1 else 0)
        val cached = programs.get(key)
        if (cached != null) return cached
        // Specialization removes unrelated fault uniforms/branches on small ES2 GPUs.
        var shader = source.replace("uniform int mode;", "const int mode=" + fault + ";")
        if (!oes)
            shader =
                shader
                    .replace("#extension GL_OES_EGL_image_external : require", "")
                    .replace("samplerExternalOES", "sampler2D")
        val result = Program(PhotoRenderer.program(shader))
        programs[key] = result
        return result
    }

    private fun allocate(w: Int, h: Int, count: Int) {
        if (width == w && height == h && bufferCount == count) return
        GLES20.glDeleteTextures(2, textures, 0)
        GLES20.glDeleteFramebuffers(2, fbos, 0)
        textures.fill(0)
        fbos.fill(0)
        bufferCount = 0
        height = 0
        width = height
        GLES20.glGenTextures(count, textures, 0)
        GLES20.glGenFramebuffers(count, fbos, 0)
        for (i in 0..<count) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                w,
                h,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null,
            )
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbos[i])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textures[i],
                0,
            )
            check(
                GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) ==
                    GLES20.GL_FRAMEBUFFER_COMPLETE
            ) {
                "Chain GPU memory: lower resolution"
            }
        }
        width = w
        height = h
        bufferCount = count
    }

    private class StageBuffer {
        var texture = 0
        var fbo = 0
        var w = 0
        var h = 0
        fun allocate(width: Int, height: Int) {
            if (w == width && h == height && texture != 0) return
            release()
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0); texture = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glGenFramebuffers(1, ids, 0); fbo = ids[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0)
            check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) { "Transport GPU memory" }
            w = width; h = height
        }
        fun release() {
            if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            if (fbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            texture = 0; fbo = 0; w = 0; h = 0
        }
    }

    private val transportBuffers = arrayOf(StageBuffer(), StageBuffer())
    private val networkFrame = StageBuffer()
    private var networkValid = false
    private var networkIdentity = 0L
    private val networkDelivery = NetworkDelivery()
    private var networkOutputW = 0
    private var networkOutputH = 0
    private var networkEpoch = 0L
    private var networkSnapshot: EffectState.Frame? = null

    fun renderedFrame(current: EffectState.Frame): EffectState.Frame =
        if (networkValid) networkSnapshot?.deliveredAt(current) ?: current else current

    private fun drawStage(input: Int, external: Boolean, matrix: FloatArray, node: FaultNode?,
                          sourceWidth: Int, sourceHeight: Int, destination: Int, w: Int, h: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, destination)
        GLES20.glViewport(0, 0, w, h)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        val program = program(node?.id ?: Effects.CLEAN, external)
        GLES20.glUseProgram(program.id)
        GLES20.glBindTexture(if (external) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D, input)
        program.bind(node, matrix, sourceWidth, sourceHeight)
        vertices.position(0)
        GLES20.glEnableVertexAttribArray(program.position)
        GLES20.glVertexAttribPointer(program.position, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun renderTransport(texture: Int, oes: Boolean, transform: FloatArray?, frame: EffectState.Frame,
                                w: Int, h: Int, sourceW: Int, sourceH: Int, target: Int, targetTexture: Int) {
        var input = texture
        var iw = sourceW; var ih = sourceH
        var first = true
        var analog = false
        var nearest = false
        var networkUsed = false
        var finalWritten = false
        for (node in frame.nodes) {
            val kind = Math.round(node.profile["transportKind"] ?: 0f)
            // Digital media is an exact pass-through: no render or resampling at this stage.
            if (node.id == Effects.VHS && kind == 2) continue
            if (node.id == Effects.CRT && kind == 1) {
                nearest = analog && node.get("upconvert") < .5f
                continue
            }
            var ow = w; var oh = h
            if (node.id == Effects.VHS) {
                analog = kind == 0 || kind == 3
                if (kind == 3 || (node.profile["mediaReduce"] ?: 0f) >= .5f) {
                    val capW = if (kind == 0 || (kind == 3 && node.get("cableKind") < .5f)) 320 else 720
                    ow = minOf(w, capW); oh = minOf(h, 480)
                }
            }
            val network = node.id == Effects.CRT && kind == 2
            if (network) {
                networkUsed = true
                val factor = 1f - node.get("transportLoss") * .8f
                ow = max(16, (w * factor).toInt()); oh = max(16, (h * factor).toInt())
            }
            // A standalone LED pass already produces w × h RGBA. An owned, unpublished target
            // needs no extra resampling copy. Unknown/aliased targets keep the original path.
            if (frame.nodes.size == 1 && node.id == Effects.CRT && kind == 3 &&
                target != 0 && targetTexture != 0 && targetTexture != texture && targetTexture != input) {
                drawStage(input, first && oes, if (first) requireNotNull(transform) else IDENTITY,
                    node, ow, oh, target, ow, oh)
                finalWritten = true
                break
            }
            val buffer = if (network) networkFrame else transportBuffers.first { it.texture != input }
            if (network && (networkOutputW != w || networkOutputH != h || networkIdentity != node.identity.seed || networkEpoch != frame.sourceEpoch)) networkValid = false
            val now = frame.deliveryNs
            if (!network || networkDelivery.update(now, node.mechanism["networkFps"] ?: 0f,
                    node.get("networkStall") >= .5f, networkValid)) {
                // Allocate only when a frame arrives: resolution edits must not discard a held frame.
                buffer.allocate(ow, oh)
                drawStage(input, first && oes, if (first) requireNotNull(transform) else IDENTITY,
                    node, if (node.id == Effects.CRT && kind == 3) ow else iw,
                    if (node.id == Effects.CRT && kind == 3) oh else ih, buffer.fbo, ow, oh)
                if (network) { networkValid = true; networkIdentity = node.identity.seed; networkOutputW = w; networkOutputH = h; networkEpoch = frame.sourceEpoch; networkSnapshot = frame }
            }
            input = buffer.texture; iw = buffer.w; ih = buffer.h; first = false
        }
        if (!networkUsed) { networkValid = false; networkSnapshot = null; networkFrame.release() }
        if (nearest && !first) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, input)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        }
        if (!finalWritten)
            drawStage(input, first && oes, if (first) requireNotNull(transform) else IDENTITY, null, iw, ih, target, w, h)
        if (nearest && !first) GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Transport GPU draw failed" }
    }

    fun render(
        texture: Int,
        oes: Boolean,
        transform: FloatArray?,
        frame: EffectState.Frame,
        w: Int,
        h: Int,
        sourceW: Int,
        sourceH: Int,
        target: Int,
        targetTexture: Int = 0,
    ) {
        if (frame.nodes.any { (it.profile["transportKind"] ?: 0f) > 0f || (it.profile["mediaReduce"] ?: 0f) > 0f }) {
            renderTransport(texture, oes, transform, frame, w, h, sourceW, sourceH, target, targetTexture)
            return
        }
        if (networkValid) { networkValid = false; networkSnapshot = null; networkFrame.release() }
        transportBuffers.forEach { if (it.texture != 0) it.release() }
        val count = frame.nodes.size
        // Opt in only for an owned RGBA8 target texture of exactly w × h. The callers keep it
        // unpublished until render returns. Unknown targets and source aliases use private buffers.
        val borrowTarget = count > 1 && target != 0 && targetTexture != 0 && targetTexture != texture
        if (count > 1) allocate(w, h, if (borrowTarget) 1 else 2)
        var input = texture
        val passes = max(1, count)
        GLES20.glViewport(0, 0, w, h)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        vertices.position(0)
        var previousPosition = -1
        for (i in 0..<passes) {
            val first = i == 0
            val last = i == passes - 1
            val destination = if (borrowTarget) {
                if ((passes - i) % 2 == 1) target else fbos[0]
            } else if (last) target else fbos[i % 2]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, destination)
            val node = if (count == 0) null else frame.nodes.get(i)
            val program = program(if (node == null) Effects.CLEAN else node.id, first && oes)
            GLES20.glUseProgram(program.id)
            GLES20.glBindTexture(
                if (first && oes) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D,
                input,
            )
            program.bind(node, if (first) requireNotNull(transform) else IDENTITY, sourceW, sourceH)
            if (program.position != previousPosition) {
                GLES20.glEnableVertexAttribArray(program.position)
                GLES20.glVertexAttribPointer(program.position, 2, GLES20.GL_FLOAT, false, 0, vertices)
                previousPosition = program.position
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            if (!last) input = if (borrowTarget) {
                if (destination == target) targetTexture else textures[0]
            } else textures[i % 2]
        }
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Fault GPU draw failed" }
    }

    fun releaseBuffers() {
        transportBuffers.forEach { it.release() }
        networkFrame.release()
        networkValid = false
        networkSnapshot = null
        GLES20.glDeleteTextures(2, textures, 0)
        GLES20.glDeleteFramebuffers(2, fbos, 0)
        textures.fill(0)
        fbos.fill(0)
        bufferCount = 0
        height = 0
        width = height
    }

    fun release() {
        releaseBuffers()
        for (program in programs.values) GLES20.glDeleteProgram(program.id)
        programs.clear()
    }

    companion object {
        private val IDENTITY =
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }
}
