package com.bongorian.signa1;
import android.opengl.*;
final class EglLease {
    static EGLDisplay display=EGL14.EGL_NO_DISPLAY;static int users;
    static synchronized EGLDisplay acquire(){if(users==0){display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] v=new int[2];if(!EGL14.eglInitialize(display,v,0,v,1))throw new IllegalStateException("EGL initialize");}users++;return display;}
    static synchronized void release(){if(--users==0){EGL14.eglTerminate(display);display=EGL14.EGL_NO_DISPLAY;}}
}
