package com.bongorian.signa1;

import android.opengl.*;
import java.nio.*;

/** Ordered GPU passes. Each pass reads the previous result, never its own attachment. */
final class EffectChain {
    private final int external,regular;
    private final int[] textures=new int[2],fbos=new int[2];
    private int width,height;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float[] IDENTITY={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    EffectChain(String shader,boolean oes){external=oes?PhotoRenderer.program(shader):0;regular=PhotoRenderer.program(shader.replace("#extension GL_OES_EGL_image_external : require","").replace("samplerExternalOES","sampler2D"));vertices.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);}
    private void allocate(int w,int h){
        if(width==w&&height==h)return;
        GLES20.glDeleteTextures(2,textures,0);GLES20.glDeleteFramebuffers(2,fbos,0);
        width=height=0;GLES20.glGenTextures(2,textures,0);GLES20.glGenFramebuffers(2,fbos,0);
        for(int i=0;i<2;i++){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbos[i]);GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,textures[i],0);
            if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Chain GPU memory: lower resolution");
        }
        width=w;height=h;
    }
    void render(int texture,boolean oes,float[] transform,int[] modes,float amount,float time,int w,int h,int sourceW,int sourceH,int target,float[] parameters){
        render(texture,oes,transform,modes,amount,time,w,h,sourceW,sourceH,target,parameters,null);
    }
    void render(int texture,boolean oes,float[] transform,int[] modes,float amount,float time,int w,int h,int sourceW,int sourceH,int target,float[] parameters,float[] live){
        int count=amount==0?0:modes.length;
        if(count>1)allocate(w,h);
        int input=texture;
        for(int i=0;i<Math.max(1,count);i++){
            boolean first=i==0,last=i==Math.max(1,count)-1;
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,last?target:fbos[i%2]);
            GLES20.glViewport(0,0,w,h);int program=first&&oes?external:regular;GLES20.glUseProgram(program);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(first&&oes?GLES11Ext.GL_TEXTURE_EXTERNAL_OES:GLES20.GL_TEXTURE_2D,input);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"cam"),0);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"st"),1,false,first?transform:IDENTITY,0);
            int id=count==0?Effects.CLEAN:modes[i];
            int at=id*4;GLES20.glUniform4f(GLES20.glGetUniformLocation(program,"live"),live==null?0:live[at],live==null?0:live[at+1],live==null?0:live[at+2],live==null?0:live[at+3]);
            GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"detail"),EffectParameters.get(parameters,id,1),EffectParameters.get(parameters,id,2),EffectParameters.get(parameters,id,3));
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"a"),EffectParameters.unit(amount)*EffectParameters.get(parameters,id,0));GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"t"),time);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"mode"),count==0?Effects.CLEAN:modes[i]);GLES20.glUniform2f(GLES20.glGetUniformLocation(program,"sourceSize"),sourceW,sourceH);
            vertices.position(0);int p=GLES20.glGetAttribLocation(program,"p");GLES20.glEnableVertexAttribArray(p);GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            if(!last)input=textures[i%2];
        }
        if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Chain GPU draw failed");
    }
}
