package com.bongorian.signa1

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/** Ordered GPU passes. Each pass reads the previous result, never its own attachment. */
internal class EffectChain(private val source: String, private val supportsExternal: Boolean) {
    private val programs: MutableMap<Int?, Int?> = HashMap<Int?, Int?>()
    private val locations: MutableMap<Int?, MutableMap<String?, Int?>?> =
        HashMap<Int?, MutableMap<String?, Int?>?>()
    private val attributes: MutableMap<Int?, Int?> = HashMap<Int?, Int?>()
    private val textures = IntArray(2)
    private val fbos = IntArray(2)
    private var width = 0
    private var height = 0
    private val vertices: FloatBuffer =
        ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()

    init {
        vertices.put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0)
        program(Effects.CLEAN, false)
        if (supportsExternal) program(Effects.CLEAN, true)
    }

    private fun program(fault: Int, oes: Boolean): Int {
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
        val result = PhotoRenderer.program(shader)
        programs.put(key, result)
        locations.put(result, HashMap<String?, Int?>())
        attributes.put(result, GLES20.glGetAttribLocation(result, "p"))
        return result
    }

    private fun uniform(program: Int, name: String?): Int {
        return locations.getValue(program)!!.computeIfAbsent(name) { key: kotlin.String? ->
            GLES20.glGetUniformLocation(
                program,
                key,
            )
        }!!
    }

    private fun allocate(w: Int, h: Int) {
        if (width == w && height == h) return
        GLES20.glDeleteTextures(2, textures, 0)
        GLES20.glDeleteFramebuffers(2, fbos, 0)
        height = 0
        width = height
        GLES20.glGenTextures(2, textures, 0)
        GLES20.glGenFramebuffers(2, fbos, 0)
        for (i in 0..1) {
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
    ) {
        val count = frame.nodes.size
        if (count > 1) allocate(w, h)
        var input = texture
        for (i in 0..<max(1, count)) {
            val first = i == 0
            val last = i == max(1, count) - 1
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, if (last) target else fbos[i % 2])
            val node = if (count == 0) null else frame.nodes.get(i)
            GLES20.glViewport(0, 0, w, h)
            val program = program(if (node == null) Effects.CLEAN else node.id, first && oes)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(
                if (first && oes) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D,
                input,
            )
            GLES20.glUniform1i(uniform(program, "cam"), 0)
            GLES20.glUniformMatrix4fv(
                uniform(program, "st"),
                1,
                false,
                if (first) transform else IDENTITY,
                0,
            )
            if (node != null) {
                node.profile.forEach { (key: String?, value: Float?) ->
                    GLES20.glUniform1f(
                        uniform(
                            program,
                            key,
                        ),
                        value!!,
                    )
                }
                node.mechanism.forEach { (key: String?, value: Float?) ->
                    GLES20.glUniform1f(
                        uniform(
                            program,
                            key,
                        ),
                        value!!,
                    )
                }
            }
            GLES20.glUniform2f(uniform(program, "sourceSize"), sourceW.toFloat(), sourceH.toFloat())
            vertices.position(0)
            val p: Int = attributes.get(program)!!
            GLES20.glEnableVertexAttribArray(p)
            GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 0, vertices)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            if (!last) input = textures[i % 2]
        }
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Fault GPU draw failed" }
    }

    fun release() {
        GLES20.glDeleteTextures(2, textures, 0)
        GLES20.glDeleteFramebuffers(2, fbos, 0)
        for (program in programs.values) GLES20.glDeleteProgram(program!!)
        programs.clear()
        locations.clear()
        attributes.clear()
        height = 0
        width = height
    }

    companion object {
        private val IDENTITY =
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }
}
