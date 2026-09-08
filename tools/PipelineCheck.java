package com.bongorian.signa1;

import java.util.*;

/** Independent raw sample fixtures. Deliberately supplies physical values, bypassing UI macros. */
public final class PipelineCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static byte[] samples(int... v){byte[] b=new byte[v.length*2];for(int i=0;i<v.length;i++)RawGlitch.write(b,i,v[i]);return b;}
    static FaultNode node(int id,Object... pairs){Map<String,Float> p=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)p.put((String)pairs[i],((Number)pairs[i+1]).floatValue());return new FaultNode(id,new FaultNode.Identity(19),new FaultNode.Motion(0,0,0),new FaultNode.Event(0,1,.5f,10),p);}
    public static void main(String[] args){
        byte[] bytes=samples(0x3412,0x7856,0xbc9a,0xf0de);
        FaultNode address=node(Effects.ADDRESS_ERROR,"byteOffset",1,"addressRegion",512,"addressProbability",1);
        check(Arrays.equals(RawGlitch.apply(bytes,4,1,65535,0,address),samples(0x5634,0x9a78,0xdebc,0)),"one-byte component/address misread");
        check(Arrays.equals(RawGlitch.apply(bytes,4,1,65535,0,node(Effects.BIT_ERROR,"bitBlock",2,"bitIndex",0,"bitProbability",1)),samples(0x3413,0x7857,0xbc9b,0xf0df)),"low-bit XOR");
        byte[] mosaic=samples(100,200,300,400);
        check(Arrays.equals(RawGlitch.apply(mosaic,2,2,4095,0,node(Effects.CFA_ERROR,"cfaRegion",2,"cfaPhase",0,"cfaCoverage",1)),samples(200,100,400,300)),"CFA X phase");
        check(Arrays.equals(RawGlitch.apply(mosaic,2,2,4095,0,node(Effects.CFA_ERROR,"cfaRegion",2,"cfaPhase",1,"cfaCoverage",1)),samples(300,400,100,200)),"CFA Y phase");
        check(Arrays.equals(RawGlitch.apply(mosaic,2,2,4095,0,node(Effects.CFA_ERROR,"cfaRegion",2,"cfaPhase",2,"cfaCoverage",1)),samples(400,300,200,100)),"CFA XY phase");
        byte[] exposure=RawGlitch.apply(mosaic,2,2,4095,0,node(Effects.EXPOSURE,"exposureDepth",1,"integration",0,"scanPhase",0,"exposurePhase",0));check(Arrays.equals(exposure,samples(50,100,150,200)),"integrated exposure attenuation");
        int w=128,h=96;byte[] pattern=new byte[w*h*2];for(int y=0;y<h;y++)for(int x=0;x<w;x++)RawGlitch.write(pattern,y*w+x,1000+(x%2)*500+(y%2)*1000);
        FaultNode row=node(Effects.ROW_ERROR,"rowGroups",20,"weakRows",1,"rowOffset",.8,"readoutShear",.4,"linePosition",.5,"lineHeight",.1,"lineRetention",1,"lineLoss",1);
        byte[] shifted=RawGlitch.apply(pattern,w,h,4095,0,row);int missing=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int v=RawGlitch.read(shifted,y*w+x);if(v==0)missing++;else check(v==RawGlitch.read(pattern,y*w+x),"readout and repeated rows preserve Bayer phase");}check(missing>0,"missing edge samples");
        EffectState all=EffectState.defaults().chain(-1);FaultModel model=new FaultModel(42);FaultModel.Inputs in=new FaultModel.Inputs();in.sensorNs=100;model.advance(0,in,FaultConfig.defaults());model.advance(.1,in,FaultConfig.defaults());EffectState.Frame frame=model.apply(all.snapshot(true,0),FaultConfig.defaults());
        for(int[] size:new int[][]{{1,1},{3,5},{128,96}}){int count=size[0]*size[1];byte[] input=new byte[count*2];for(int i=0;i<count;i++)RawGlitch.write(input,i,256+i%3500);byte[] before=input.clone();byte[] out=RawGlitch.chain(input,size[0],size[1],4095,256,frame);check(Arrays.equals(input,before),"source ownership");check(out.length==input.length,"RAW container size");for(int i=0;i<count;i++)check(RawGlitch.read(out,i)<=4095,"white-level bound");check(Arrays.equals(out,RawGlitch.chain(input,size[0],size[1],4095,256,frame)),"snapshot replay");}
        check(Arrays.equals(frame.through(Effects.Point.DATA).ids(),new int[]{1,2,3,4,5}),"causal recording tap prefix");
        check(frame.through(Effects.Point.MEDIA).nodes.size()==12,"VHS tap excludes CRT");
        check(Effects.point(Effects.CFA_ERROR)==Effects.point(Effects.DEMOSAIC_ERROR),"reconstruction point");
        System.out.println("PASS byte/bit/CFA/exposure fixtures, Bayer row parity, RAW bounds and causal taps");
    }
}
