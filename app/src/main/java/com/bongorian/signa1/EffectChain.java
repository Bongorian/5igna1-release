package com.bongorian.signa1;

import android.opengl.*;
import java.nio.*;

/** Ordered GPU passes. Each pass reads the previous result, never its own attachment. */
final class EffectChain {
    private final String source;private final boolean supportsExternal;
    private final java.util.Map<Integer,Integer> programs=new java.util.HashMap<>();
    private final java.util.Map<Integer,java.util.Map<String,Integer>> locations=new java.util.HashMap<>();
    private final java.util.Map<Integer,Integer> attributes=new java.util.HashMap<>();
    private final int[] textures=new int[2],fbos=new int[2];
    private int width,height;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float[] IDENTITY={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    EffectChain(String shader,boolean oes){source=shader;supportsExternal=oes;vertices.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);program(Effects.CLEAN,false);if(oes)program(Effects.CLEAN,true);}
    private int program(int fault,boolean oes){
        if(oes&&!supportsExternal)throw new IllegalArgumentException("External signal unsupported");int key=fault*2+(oes?1:0);
        Integer cached=programs.get(key);if(cached!=null)return cached;
        // Specialization removes unrelated fault uniforms/branches on small ES2 GPUs.
        String shader=source.replace("uniform int mode;","const int mode="+fault+";");
        if(!oes)shader=shader.replace("#extension GL_OES_EGL_image_external : require","").replace("samplerExternalOES","sampler2D");
        int result=PhotoRenderer.program(shader);programs.put(key,result);locations.put(result,new java.util.HashMap<>());attributes.put(result,GLES20.glGetAttribLocation(result,"p"));return result;
    }
    private int uniform(int program,String name){return locations.get(program).computeIfAbsent(name,key->GLES20.glGetUniformLocation(program,key));}
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
    void render(int texture,boolean oes,float[] transform,EffectState.Frame frame,int w,int h,int sourceW,int sourceH,int target){
        int count=frame.nodes.size();
        if(count>1)allocate(w,h);
        int input=texture;
        for(int i=0;i<Math.max(1,count);i++){
            boolean first=i==0,last=i==Math.max(1,count)-1;
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,last?target:fbos[i%2]);
            FaultNode node=count==0?null:frame.nodes.get(i);
            GLES20.glViewport(0,0,w,h);int program=program(node==null?Effects.CLEAN:node.id,first&&oes);GLES20.glUseProgram(program);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(first&&oes?GLES11Ext.GL_TEXTURE_EXTERNAL_OES:GLES20.GL_TEXTURE_2D,input);
            GLES20.glUniform1i(uniform(program,"cam"),0);
            GLES20.glUniformMatrix4fv(uniform(program,"st"),1,false,first?transform:IDENTITY,0);
            if(node!=null){node.profile.forEach((key,value)->GLES20.glUniform1f(uniform(program,key),value));node.mechanism.forEach((key,value)->GLES20.glUniform1f(uniform(program,key),value));}
            GLES20.glUniform2f(uniform(program,"sourceSize"),sourceW,sourceH);
            vertices.position(0);int p=attributes.get(program);GLES20.glEnableVertexAttribArray(p);GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            if(!last)input=textures[i%2];
        }
        if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Fault GPU draw failed");
    }
    void release(){GLES20.glDeleteTextures(2,textures,0);GLES20.glDeleteFramebuffers(2,fbos,0);for(int program:programs.values())GLES20.glDeleteProgram(program);programs.clear();locations.clear();attributes.clear();width=height=0;}
}
