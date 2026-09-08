package com.bongorian.signa1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Full-resolution still processing on its own worker GL context, with bounded readback memory. */
internal object PhotoRenderer {
    @Throws(IOException::class)
    fun shaderSource(context: Context): String {
        context.resources.openRawResource(R.raw.effect).use { `in` ->
            ByteArrayOutputStream().use { out ->
                val buffer = ByteArray(4096)
                var n: Int
                while ((`in`.read(buffer).also { n = it }) != -1) out.write(buffer, 0, n)
                return out.toString("UTF-8").replace("// EFFECT_IDS", Effects.shaderDefines())
            }
        }
    }

    fun shader(type: Int, source: String?): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, source)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] != 0) { GLES20.glGetShaderInfoLog(s) }
        return s
    }

    fun program(fragment: String?): Int {
        val p = GLES20.glCreateProgram()
        val v =
            shader(
                GLES20.GL_VERTEX_SHADER,
                "attribute vec2 p;varying vec2 uv;void main(){uv=p*.5+.5;gl_Position=vec4(p,0.,1.);}",
            )
        val f = shader(GLES20.GL_FRAGMENT_SHADER, fragment)
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        GLES20.glDeleteShader(v)
        GLES20.glDeleteShader(f)
        check(ok[0] != 0) { GLES20.glGetProgramInfoLog(p) }
        return p
    }

    @Throws(IOException::class)
    fun orient(jpeg: ByteArray, front: Boolean): Bitmap {
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        if (decoded == null) throw IOException("JPEG decode")
        val exif = ExifInterface(ByteArrayInputStream(jpeg))
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        val transform = Matrix()
        when (orientation) {
            2 -> transform.setScale(-1f, 1f)
            3 -> transform.setRotate(180f)
            4 -> transform.setScale(1f, -1f)
            5 -> {
                transform.setRotate(90f)
                transform.postScale(-1f, 1f)
            }

            6 -> transform.setRotate(90f)
            7 -> {
                transform.setRotate(270f)
                transform.postScale(-1f, 1f)
            }

            8 -> transform.setRotate(270f)
        }
        if (front) transform.postScale(-1f, 1f)
        val upright =
            Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                transform,
                false,
            )
        if (upright != decoded) decoded.recycle()
        return upright
    }

    /** Offline/replay adapter. Live JPEG capture reads SignalBuffer and never re-renders. */
    @Throws(IOException::class)
    fun render(
        context: Context,
        jpeg: ByteArray,
        front: Boolean,
        frame: EffectState.Frame,
    ): Bitmap? {
        var source: Bitmap? = orient(jpeg, front)
        var output: Bitmap? = null
        val display = EglLease.acquire()
        var egl = EGL14.EGL_NO_CONTEXT
        var surface = EGL14.EGL_NO_SURFACE
        try {
            val configs = arrayOfNulls<EGLConfig>(1)
            val n = IntArray(1)
            val attrs =
                intArrayOf(
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_ALPHA_SIZE,
                    8,
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                )
            if (
                !EGL14.eglChooseConfig(
                    display,
                    attrs,
                    0,
                    configs,
                    0,
                    1,
                    n,
                    0,
                ) || n[0] == 0
            )
                throw IOException("Photo EGL config")
            egl =
                EGL14.eglCreateContext(
                    display,
                    configs[0],
                    EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                    0,
                )
            surface =
                EGL14.eglCreatePbufferSurface(
                    display,
                    configs[0],
                    intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                    0,
                )
            if (
                !EGL14.eglMakeCurrent(
                    display,
                    surface,
                    surface,
                    egl,
                )
            )
                throw IOException("Photo EGL context")
            val width = source!!.width
            val height = source!!.height
            val textures = IntArray(2)
            val fbo = IntArray(1)
            GLES20.glGenTextures(2, textures, 0)
            for (texture in textures) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
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
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, source, 0)
            source!!.recycle()
            source = null
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null,
            )
            GLES20.glGenFramebuffers(1, fbo, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textures[1],
                0,
            )
            if (
                GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                    GLES20.GL_FRAMEBUFFER_COMPLETE
            )
                throw IOException("Photo GPU memory")
            val matrix =
                floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f)
            val chain = EffectChain(shaderSource(context), false)
            chain.render(textures[0], false, matrix, frame, width, height, width, height, fbo[0])
            output = SignalBuffer.readPixels(width, height)
            chain.release()
            return output
        } catch (e: IOException) {
            if (output != null) output.recycle()
            throw e
        } catch (e: RuntimeException) {
            if (output != null) output.recycle()
            throw e
        } finally {
            if (source != null) source!!.recycle()
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            if (surface !== EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
            if (egl !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, egl)
            EglLease.release()
            EGL14.eglReleaseThread()
        }
    }
}
