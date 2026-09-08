package com.bongorian.signa1;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import java.nio.*;

/** GL-owned acquired/processed signal, retained until a successful subsequent presentation. */
final class SignalBuffer {
    int texture,fbo,width,height;EffectState.Frame frame;long presentedAt;
    void allocate(int w,int h){
        if(width==w&&height==h&&texture!=0)return;
        release();if(w<=0||h<=0)throw new IllegalArgumentException("Signal size");
        int[] id=new int[1];GLES20.glGenTextures(1,id,0);texture=id[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
        GLES20.glGenFramebuffers(1,id,0);fbo=id[0];GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo);GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,texture,0);
        if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE||GLES20.glGetError()!=GLES20.GL_NO_ERROR){release();throw new IllegalStateException("Signal GPU memory: lower resolution");}
        width=w;height=h;
    }
    Bitmap read(){if(frame==null)throw new IllegalStateException("No presented signal");GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo);return readPixels(width,height);}
    static Bitmap readPixels(int width,int height){
        Bitmap output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        try{int stripe=32;ByteBuffer bytes=ByteBuffer.allocateDirect(width*stripe*4).order(ByteOrder.LITTLE_ENDIAN);int[] pixels=new int[width*stripe];
            for(int y=0;y<height;y+=stripe){int rows=Math.min(stripe,height-y);bytes.clear();GLES20.glReadPixels(0,y,width,rows,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes);bytes.rewind();for(int r=0;r<rows;r++)for(int x=0;x<width;x++){int rgba=bytes.getInt();pixels[(rows-r-1)*width+x]=(rgba&0xff00ff00)|((rgba&255)<<16)|((rgba>>>16)&255);}output.setPixels(pixels,0,width,0,height-y-rows,width,rows);}
            if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Signal readback");return output;
        }catch(RuntimeException|OutOfMemoryError error){output.recycle();throw error;}
    }
    void release(){if(texture!=0)GLES20.glDeleteTextures(1,new int[]{texture},0);if(fbo!=0)GLES20.glDeleteFramebuffers(1,new int[]{fbo},0);texture=fbo=width=height=0;frame=null;}
}
