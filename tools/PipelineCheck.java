package com.bongorian.signa1;

import java.util.Arrays;

/** Independent fixtures for byte alignment, row identity, CFA phase and v5 rejection. */
public final class PipelineCheck {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static byte[] samples(int... values){byte[] out=new byte[values.length*2];for(int i=0;i<values.length;i++)RawGlitch.write(out,i,values[i]);return out;}
    static float[] controls(int id,float p1,float p2){float[] out=EffectParameters.defaults();out[id*4+1]=p1;out[id*4+2]=p2;return out;}
    static byte[] apply(byte[] in,int w,int h,int id,float p1,float p2){return RawGlitch.apply(in,w,h,65535,0,id,1,73,controls(id,p1,p2));}
    public static void main(String[] args){
        int data=Effects.DATA_SHIFT,line=Effects.LINE_LOSS,cfa=Effects.CFA_OFFSET;
        byte[] bytes=samples(0x3412,0x7856,0xbc9a,0xf0de);
        check(Arrays.equals(apply(bytes,4,1,data,1f/31,1),samples(0x5634,0x9a78,0xdebc,0)),"one-byte boundary shift");
        check(Arrays.equals(apply(bytes,4,1,data,2f/31,1),samples(0x7856,0xbc9a,0xf0de,0)),"one-word shift");
        check(Arrays.equals(apply(bytes,4,1,data,0,1),bytes),"zero-byte bypass");
        byte[] rows=new byte[8*10*2];for(int y=0;y<10;y++)for(int x=0;x<8;x++)RawGlitch.write(rows,y*8+x,100+y*16+x);
        byte[] repeated=apply(rows,8,10,line,0,1);
        for(int y=0;y<10;y++)for(int x=0;x<8;x++)check(RawGlitch.read(repeated,y*8+x)==(y<2?0:RawGlitch.read(rows,(y-2)*8+x)),"line repetition preserves horizontal samples and Bayer row parity");
        check(Arrays.equals(apply(rows,8,10,line,0,0),new byte[rows.length]),"line loss fills black");
        byte[] mosaic=samples(100,200,300,400);
        check(Arrays.equals(apply(mosaic,2,2,cfa,0,0),samples(200,100,400,300)),"CFA X phase");
        check(Arrays.equals(apply(mosaic,2,2,cfa,.5f,0),samples(300,400,100,200)),"CFA Y phase");
        check(Arrays.equals(apply(mosaic,2,2,cfa,1,0),samples(400,300,200,100)),"CFA XY phase");
        for(int[] dimensions:new int[][]{{1,1},{3,5},{128,96}}){
            int w=dimensions[0],h=dimensions[1];byte[] input=new byte[w*h*2];for(int i=0;i<w*h;i++)RawGlitch.write(input,i,256+i%3500);byte[] before=input.clone();
            for(int id:new int[]{data,line,cfa}){
                float[] p=controls(id,.37f,.62f);
                check(Arrays.equals(input,RawGlitch.apply(input,w,h,4095,256,id,0,7,p)),"global zero bypass");
                byte[] out=RawGlitch.apply(input,w,h,4095,256,id,1,7,p);
                check(out.length==input.length&&Arrays.equals(input,before),"container length and source ownership");
                check(Arrays.equals(out,RawGlitch.apply(input,w,h,4095,256,id,1,999,p)),"capture-independent spatial pattern");
                for(int i=0;i<w*h;i++)check(RawGlitch.read(out,i)<=4095,"white-level bounds");
                p[id*4]=0;check(Arrays.equals(input,RawGlitch.apply(input,w,h,4095,256,id,1,7,p)),"stage zero bypass");
            }
        }
        int newMask=(1<<data)|(1<<line)|(1<<cfa)|(1<<Effects.DEMOSAIC);
        EffectState state=EffectState.defaults().chain(newMask);
        check(Arrays.equals(state.ids(),new int[]{line,data,cfa,Effects.DEMOSAIC}),"pipeline stage order");
        check(Arrays.equals(state.forContext(false,2).ids(),new int[]{line,data,cfa}),"RAW excludes ISP processing");
        check(state.snapshot(false,1).ids.length==0,"RAW original bypass");
        check(EffectState.decode(state.encode()).encode().equals(state.encode()),"new state roundtrip");
        String old="1|0|256|0.73";for(int i=0;i<13*4;i++)old+="|0.5";
        try{EffectState.decode(old);throw new AssertionError("v5 state must not be reinterpreted");}catch(IllegalArgumentException expected){}
        check(Effects.CLEAN==0&&Effects.DEMOSAIC==9&&Effects.TERMINAL==16,"v6 pipeline IDs");
        check(Arrays.equals(RawGlitch.chain(rows,8,10,65535,0,new int[]{Effects.DEMOSAIC},1,7),rows),"ISP never modifies RAW");
        System.out.println("PASS DATA SHIFT byte/word fixtures, LINE LOSS row identity, CFA phases, RAW bounds, v5 rejection and pipeline contexts");
    }
}
