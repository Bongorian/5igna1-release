package com.bongorian.signa1;

import java.util.*;

public final class FaultModelCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static FaultNode node(EffectState.Frame f,int id){return f.nodes.stream().filter(n->n.id==id).findFirst().orElseThrow();}
    static EffectState.Frame sample(FaultModel model,EffectState state,double t,FaultConfig config,FaultModel.Inputs input){input.sensorNs=(long)(t*1e9)+1;model.advance(t,input,config);return model.apply(state.snapshot(true,0),config);}
    public static void main(String[] args){
        FaultConfig off=FaultConfig.defaults(),live=new FaultConfig(true,true,true,true,true,true,.5f,50);
        EffectState state=EffectState.defaults().chain(-1);FaultModel m=new FaultModel(7);FaultModel.Inputs input=new FaultModel.Inputs();
        EffectState.Frame start=sample(m,state,0,off,input);String startDescription=start.describe();
        boolean incident=false,recovered=false,changed=false;float old=0;
        for(int i=1;i<=1800;i++){
            EffectState.Frame frame=sample(m,state,i/60.,off,input);check(Arrays.equals(frame.ids(),state.ids()),"time never shuffles route");
            for(FaultNode n:frame.nodes){check(n.identity.seed==state.parameters().identity(n.id),"structural identity persistence");for(float p:n.mechanism.values())check(Float.isFinite(p),"finite physical parameter");}
            FaultNode tape=node(frame,Effects.VHS);changed|=Math.abs(tape.motion.drift-node(start,Effects.VHS).motion.drift)>.001;
            if(i>1)check(Math.abs(tape.motion.drift-old)<.025,"continuous slow drift");old=tape.motion.drift;
            float event=node(frame,Effects.STREAM_ERROR).event.envelope;if(event>0)incident=true;else if(incident)recovered=true;
            check(frame.describe().equals(m.apply(state.snapshot(true,0),off).describe()),"read-only frame snapshot, no random consumption");
        }
        check(node(start,Effects.VHS).profile.containsKey("tapeBandwidth")&&!node(start,Effects.VHS).mechanism.containsKey("tapeBandwidth"),"media profile distinct from tracking/dropout faults");
        check(node(start,Effects.CRT).profile.containsKey("phosphorMix")&&node(start,Effects.CRT).mechanism.containsKey("syncOffset"),"display profile distinct from sync faults");
        check(incident&&recovered&&changed,"intrinsic motion and temporary incidents exist without LIVE");check(start.describe().equals(startDescription),"capture snapshot survives continued recording");
        EffectState.Frame reseeded=m.apply(state.edit(true,state.mask,state.parameters().reseed(Effects.VHS,123)).snapshot(true,0),off);
        EffectState.Frame before=m.apply(state.snapshot(true,0),off);
        check(node(reseeded,Effects.CRT).get("convergenceOffset")==node(before,Effects.CRT).get("convergenceOffset"),"reseed does not affect other faults");
        check(node(reseeded,Effects.VHS).identity.seed!=node(before,Effects.VHS).identity.seed,"new fault individual");
        check(node(before,Effects.PIXEL_DAMAGE).get("pixelDensity")==node(start,Effects.PIXEL_DAMAGE).get("pixelDensity"),"sensor sites do not wander");
        try{before.nodes.clear();throw new AssertionError("mutable nodes");}catch(UnsupportedOperationException expected){}
        try{before.nodes.get(0).mechanism.put("pixelDensity",1f);throw new AssertionError("mutable mechanism");}catch(UnsupportedOperationException expected){}
        FaultModel a=new FaultModel(55),b=new FaultModel(55);sample(a,state,0,off,input);sample(b,state,0,off,input);
        EffectState.Frame af=sample(a,state,10,off,input),bf=sample(b,state,10,off,input);check(af.describe().equals(bf.describe()),"explicit session replay");
        input.motionAvailable=input.timingAvailable=true;input.ax=9;input.rotation=2;input.heat=1;input.cpu=1;input.audio=.8f;input.jitter=.8f;input.exposureNs=4_000_000;input.skewNs=12_000_000;
        for(int i=1;i<=600;i++)af=sample(a,state,10+i/60.,live,input);
        check(node(af,Effects.PIXEL_DAMAGE).get("sensorNoise")>0,"thermal sensor activity");check(node(af,Effects.PIXEL_DAMAGE).get("pixelDensity")==node(start,Effects.PIXEL_DAMAGE).get("pixelDensity"),"heat changes activity, not sites");
        check(a.pressure>.1&&Math.abs(node(af,Effects.ROW_ERROR).get("readoutShear"))>.001,"timing/CPU and motion coupling");
        EffectState.Frame disabled=sample(a,state,21,off,input);check(node(disabled,Effects.PIXEL_DAMAGE).get("sensorNoise")==0,"LIVE off disconnects measured inputs");
        check(disabled.cameraNs==input.sensorNs,"camera timestamp belongs to snapshot");
        check(sample(a,state.amount(0),22,off,input).nodes.isEmpty(),"LEVEL zero has no processing nodes");
        check(FaultModel.drift(9,999.9999)-FaultModel.drift(9,1000.0001)<.001,"no clock rollover");
        System.out.println("PASS independent identity/motion/events, intrinsic evolution, pure snapshots, reseed isolation and measured coupling");
    }
}
