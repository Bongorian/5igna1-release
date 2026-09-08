package com.bongorian.signa1;

import java.util.*;

public final class EffectStateCheck {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        EffectParameters p=EffectParameters.defaults().with(Effects.VHS,"tracking",.2f);
        EffectState state=EffectState.create(Effects.CLEAN,(1<<Effects.PIXEL_DAMAGE)|(1<<Effects.ROW_ERROR)|(1<<Effects.STREAM_ERROR),1,p);
        check(Effects.CONTROLS[Effects.VHS].length!=Effects.CONTROLS[Effects.DEMOSAIC_ERROR].length,"variable control counts");
        EffectParameters changed=p.with(Effects.VHS,"tracking",1);check(p.get(Effects.VHS,"tracking")==.2f&&changed.get(Effects.VHS,"tracking")==1,"immutable controls");
        long identity=p.identity(Effects.ROW_ERROR);EffectParameters other=p.reseed(Effects.ROW_ERROR,Long.MIN_VALUE);
        check(p.identity(Effects.ROW_ERROR)==identity&&other.identity(Effects.ROW_ERROR)==Long.MIN_VALUE,"long identity and immutable reseed");
        check(other.get(Effects.ROW_ERROR,"displacement")==p.get(Effects.ROW_ERROR,"displacement"),"reseed keeps controls");
        check(other.identity(Effects.VHS)==p.identity(Effects.VHS),"reseed isolates faults");
        check(state.single(Effects.CLEAN).ids().length==0,"CLEAN empty route");
        check(state.single(Effects.CRT).ids().length==1,"single selection");
        check(Arrays.equals(state.forContext(false,0).ids(),state.forContext(true,0).ids()),"photo and video share STREAM ERROR");
        check(Arrays.equals(state.forContext(false,2).ids(),new int[]{Effects.PIXEL_DAMAGE,Effects.ROW_ERROR}),"RAW representation capability");
        check(state.snapshot(false,1).ids().length==0&&state.forContext(false,1)==state,"RAW original tap remembers settings");
        EffectState.Frame frame=state.snapshot(true,0);int[] ids=frame.ids();ids[0]=Effects.CRT;check(frame.ids()[0]==Effects.PIXEL_DAMAGE,"immutable snapshot route");
        check(EffectState.decode(state.encode()).encode().equals(state.encode()),"named schema roundtrip");
        check(EffectState.decode(state.edit(true,state.mask,other).encode()).parameters().identity(Effects.ROW_ERROR)==Long.MIN_VALUE,"identity roundtrip");
        for(String invalid:new String[]{"","2|0|0|.5","3|0|0|NaN|"+p.encode(),state.encode().replace("tracking=0.2","tracking=NaN"),state.encode().replace("tracking=0.2","unknown=0.2")})try{EffectState.decode(invalid);throw new AssertionError("invalid settings accepted");}catch(IllegalArgumentException expected){}
        try{p.with(Effects.VHS,"strength",.5f);throw new AssertionError("universal physical strength accepted");}catch(IllegalArgumentException expected){}
        try{new EffectState.Frame(new int[]{Effects.CRT,Effects.VHS},1,p,0,0,Collections.emptyList());throw new AssertionError("Arbitrary stack accepted");}catch(IllegalArgumentException expected){}
        check(state.chain(1).mask==0,"CLEAN cannot become a pass");
        System.out.println("PASS named settings, immutable route, identity isolation, schema reset and shared photo/video capabilities");
    }
}
