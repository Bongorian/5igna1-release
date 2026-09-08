package com.bongorian.signa1;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.opengl.*;
import java.io.*;
import java.nio.*;

/** Full-resolution still processing on its own worker GL context, with bounded readback memory. */
final class PhotoRenderer {
    static String shaderSource(Context context)throws IOException {try(InputStream in=context.getResources().openRawResource(R.raw.effect);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8").replace("// EFFECT_IDS",Effects.shaderDefines());}}
    static int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
    static int program(String fragment){int p=GLES20.glCreateProgram();int v=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 p;varying vec2 uv;void main(){uv=p*.5+.5;gl_Position=vec4(p,0.,1.);}");int f=shader(GLES20.GL_FRAGMENT_SHADER,fragment);GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}
    static Bitmap orient(byte[] jpeg,boolean front)throws IOException {
        Bitmap decoded=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);if(decoded==null)throw new IOException("JPEG decode");
        ExifInterface exif=new ExifInterface(new ByteArrayInputStream(jpeg));int orientation=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        android.graphics.Matrix transform=new android.graphics.Matrix();
        switch(orientation){case 2:transform.setScale(-1,1);break;case 3:transform.setRotate(180);break;case 4:transform.setScale(1,-1);break;case 5:transform.setRotate(90);transform.postScale(-1,1);break;case 6:transform.setRotate(90);break;case 7:transform.setRotate(270);transform.postScale(-1,1);break;case 8:transform.setRotate(270);break;}
        if(front)transform.postScale(-1,1);
        Bitmap upright=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),transform,false);if(upright!=decoded)decoded.recycle();return upright;
    }
    /** Offline/replay adapter. Live JPEG capture reads SignalBuffer and never re-renders. */
    static Bitmap render(Context context,byte[] jpeg,boolean front,EffectState.Frame frame)throws IOException {
        Bitmap source=orient(jpeg,front);Bitmap output=null;
        EGLDisplay display=EglLease.acquire();EGLContext egl=EGL14.EGL_NO_CONTEXT;EGLSurface surface=EGL14.EGL_NO_SURFACE;
        try {
            EGLConfig[] configs=new EGLConfig[1];int[] n=new int[1];
            int[] attrs={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_NONE};
            if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,n,0)||n[0]==0)throw new IOException("Photo EGL config");
            egl=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface=EGL14.eglCreatePbufferSurface(display,configs[0],new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
            if(!EGL14.eglMakeCurrent(display,surface,surface,egl))throw new IOException("Photo EGL context");
            int width=source.getWidth(),height=source.getHeight();
            int[] textures=new int[2],fbo=new int[1];GLES20.glGenTextures(2,textures,0);
            for(int texture:textures){GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);}
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,source,0);source.recycle();source=null;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[1]);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,width,height,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
            GLES20.glGenFramebuffers(1,fbo,0);GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0]);GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,textures[1],0);
            if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new IOException("Photo GPU memory");
            float[] matrix={1,0,0,0,0,-1,0,0,0,0,1,0,0,1,0,1};
            EffectChain chain=new EffectChain(shaderSource(context),false);
            chain.render(textures[0],false,matrix,frame,width,height,width,height,fbo[0]);
            output=SignalBuffer.readPixels(width,height);chain.release();return output;
        }catch(IOException|RuntimeException e){if(output!=null)output.recycle();throw e;}
        finally{if(source!=null)source.recycle();EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);if(egl!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,egl);EglLease.release();EGL14.eglReleaseThread();}
    }
}
