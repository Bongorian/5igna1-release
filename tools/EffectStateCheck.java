package com.bongorian.signa1;

import java.util.Arrays;

public final class EffectStateCheck {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        float[] params=EffectParameters.defaults();params[Effects.SENSOR_FAIL*4+1]=.2f;
        EffectState state=EffectState.create(Effects.SPECTRUM,(1<<Effects.SENSOR_FAIL)|(1<<Effects.ROW_SHIFT)|(1<<Effects.PACKET_LOSS),2,params);
        check(state.amount==1,"strength bounds");params[Effects.SENSOR_FAIL*4+1]=1;
        check(state.parameters()[Effects.SENSOR_FAIL*4+1]==.2f,"input array ownership");
        float[] copy=state.parameters();copy[Effects.SENSOR_FAIL*4+1]=0;check(state.parameters()[Effects.SENSOR_FAIL*4+1]==.2f,"output array ownership");
        EffectState single=state.single(Effects.BIT_ROT);check(!single.chained&&single.mask==(1<<Effects.BIT_ROT),"single clears old chain");
        EffectState clean=state.single(Effects.CLEAN);check(clean.mask==0&&!clean.chained&&clean.ids().length==0,"clean clears all stages");
        EffectState photo=state.forContext(false,0);check(photo.mask==((1<<Effects.SENSOR_FAIL)|(1<<Effects.ROW_SHIFT)),"photo removes packet loss");
        check(!photo.forContext(true,0).enabled(Effects.PACKET_LOSS),"switch back does not resurrect removed stage");
        EffectState raw=state.chain((1<<Effects.SENSOR_FAIL)|(1<<Effects.SPECTRUM)).forContext(false,2);check(raw.mask==(1<<Effects.SENSOR_FAIL),"RAW excludes color processing");
        check(state.snapshot(false,1).ids.length==0,"RAW original bypass");
        check(state.forContext(false,1)==state,"RAW original remembers explicit settings without processing");
        EffectState.Frame frame=state.snapshot(true,0);EffectState after=state.single(Effects.SPECTRUM).amount(.1f);
        check(frame.ids.length==3&&frame.amount==1&&after.mask==(1<<Effects.SPECTRUM),"frame snapshot survives future edits");frame.parameters[0]=0;check(state.parameters()[0]==1,"frame arrays do not mutate state");
        EffectState roundtrip=EffectState.decode(state.encode());check(roundtrip.encode().equals(state.encode()),"persistence roundtrip");
        check(EffectState.create(-1,-1,Float.NaN,null).amount==0,"invalid scalar clamping");
        try{EffectState.decode("garbage");throw new AssertionError("invalid schema accepted");}catch(IllegalArgumentException expected){}
        check(state.chain(1<<Effects.CLEAN).mask==0,"CLEAN bit is never a stage");
        // RAW row movement must not create an accidental Bayer phase shift.
        int w=128,h=96;byte[] pixels=new byte[w*h*2];for(int y=0;y<h;y++)for(int x=0;x<w;x++)RawGlitch.write(pixels,y*w+x,1000+(x%2)*500+(y%2)*1000);
        float[] row=EffectParameters.defaults();row[Effects.ROW_SHIFT*4+2]=0;
        check(Arrays.equals(pixels,RawGlitch.apply(pixels,w,h,4095,0,Effects.ROW_SHIFT,1,1,row)),"RAW width zero bypass");
        row[Effects.ROW_SHIFT*4+2]=1;byte[] shifted=RawGlitch.apply(pixels,w,h,4095,0,Effects.ROW_SHIFT,1,1,row);int missing=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int n=RawGlitch.read(shifted,y*w+x);if(n==0)missing++;else check(n==RawGlitch.read(pixels,y*w+x),"RAW phase preservation");}
        check(missing>0,"RAW missing edge samples");
        check(Arrays.equals(shifted,RawGlitch.apply(pixels,w,h,4095,0,Effects.ROW_SHIFT,1,999,row)),"RAW stable readout pattern");
        for(int i=0;i<1000;i++){int id=i%Effects.NAMES.length;EffectState s=state.single(id).forContext(false,0);check(s.ids().length<=1&&!s.enabled(Effects.PACKET_LOSS),"selection invariant");check(EffectState.decode(s.encode()).encode().equals(s.encode()),"repeated transitions");}
        System.out.println("PASS immutable snapshots, single/CLEAN cleanup, context filtering, serialization, RAW row stability and Bayer phase");
    }
}
