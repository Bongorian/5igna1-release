package com.bongorian.signa1

import android.opengl.EGL14
import android.opengl.EGLDisplay

internal object EglLease {
    var display: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    var users: Int = 0

    @Synchronized
    fun acquire(): EGLDisplay? {
        if (users == 0) {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val v = IntArray(2)
            check(EGL14.eglInitialize(display, v, 0, v, 1)) { "EGL initialize" }
        }
        users++
        return display
    }

    @Synchronized
    fun release() {
        if (--users == 0) {
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
    }
}
