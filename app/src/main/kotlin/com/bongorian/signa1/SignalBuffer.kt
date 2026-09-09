package com.bongorian.signa1

import android.graphics.Bitmap
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/** GL-owned acquired/processed signal, retained until a successful subsequent presentation. */
internal class SignalBuffer {
    var texture: Int = 0
    var fbo: Int = 0
    var width: Int = 0
    var height: Int = 0
    var frame: EffectState.Frame? = null
    var presentedAt: Long = 0

    fun allocate(w: Int, h: Int) {
        if (width == w && height == h && texture != 0) return
        release()
        require(!(w <= 0 || h <= 0)) { "Signal size" }
        val id = IntArray(1)
        GLES20.glGenTextures(1, id, 0)
        texture = id[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
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
        GLES20.glGenFramebuffers(1, id, 0)
        fbo = id[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texture,
            0,
        )
        if (
            GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE || GLES20.glGetError() != GLES20.GL_NO_ERROR
        ) {
            release()
            throw IllegalStateException("Signal GPU memory: lower resolution")
        }
        width = w
        height = h
    }

    fun read(): Bitmap {
        checkNotNull(frame) { "No presented signal" }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        return readPixels(width, height)
    }

    fun release() {
        if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        if (fbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        height = 0
        width = height
        fbo = width
        texture = fbo
        frame = null
    }

    companion object {
        fun readPixels(width: Int, height: Int): Bitmap {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val stripe = 32
                val bytes =
                    ByteBuffer.allocateDirect(width * stripe * 4).order(ByteOrder.LITTLE_ENDIAN)
                val pixels = IntArray(width * stripe)
                var y = 0
                while (y < height) {
                    val rows = min(stripe, height - y)
                    bytes.clear()
                    GLES20.glReadPixels(
                        0,
                        y,
                        width,
                        rows,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        bytes,
                    )
                    bytes.rewind()
                    for (r in 0..<rows) for (x in 0..<width) {
                        val rgba = bytes.int
                        pixels[(rows - r - 1) * width + x] =
                            (rgba and -0xff0100) or
                                ((rgba and 255) shl 16) or
                                ((rgba ushr 16) and 255)
                    }
                    output.setPixels(pixels, 0, width, 0, height - y - rows, width, rows)
                    y += stripe
                }
                check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Signal readback" }
                return output
            } catch (error: RuntimeException) {
                output.recycle()
                throw error
            } catch (error: OutOfMemoryError) {
                output.recycle()
                throw error
            }
        }
    }
}
