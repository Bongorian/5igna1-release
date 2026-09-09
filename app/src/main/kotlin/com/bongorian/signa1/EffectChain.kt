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
