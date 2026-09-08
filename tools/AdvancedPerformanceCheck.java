package com.bongorian.signa1;

import java.util.*;

/** Contract fixtures: full internal catalog, immutable overrides and independent performance clocks. */
public final class AdvancedPerformanceCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static FaultNode node(EffectState.Frame frame,int id){return frame.nodes.stream().filter(n->n.id==id).findFirst().orElseThrow();}
    static EffectState.Frame sample(FaultModel m,EffectState s,double t,FaultConfig c){FaultModel.Inputs in=new FaultModel.Inputs();in.sensorNs=(long)(t*1e9)+1;m.advance(t,in,c);return m.apply(s.snapshot(true,0),c);}
    public static void main(String[] args){
        EffectState state=EffectState.defaults().chain(-1);FaultConfig off=FaultConfig.defaults();FaultModel model=new FaultModel(73);sample(model,state,0,off);
        for(int id:state.ids()){
            FaultNode automatic=model.inspect(id,state.parameters(),state.amount,off);
            Set<String> catalog=new LinkedHashSet<>();for(FaultParameters.Spec spec:FaultParameters.all(id))check(catalog.add(spec.key),"duplicate internal key");
            check(catalog.equals(automatic.inspect().keySet()),"complete internal catalog for "+Effects.name(id)+" missing="+automatic.inspect().keySet()+" vs "+catalog);
            for(FaultParameters.Spec spec:FaultParameters.all(id)){
                float value=spec.min+(spec.max-spec.min)*.63f;
                EffectParameters changed=state.parameters().override(id,spec.key,value);
                FaultNode actual=model.inspect(id,changed,state.amount,off);
                check(Math.abs(actual.inspect().get(spec.key)-value)<(spec.key.equals("eventSerial")?1:.002f),"direct control not applied: "+Effects.name(id)+" / "+spec.key);
                EffectState draft=state.edit(true,state.mask,changed);check(EffectState.decode(draft.encode()).encode().equals(draft.encode()),"override roundtrip");
                check(state.parameters().overrides(id).isEmpty(),"base parameters mutated");
                check(changed.automatic(id,spec.key).encode().equals(state.parameters().encode()),"AUTO returns to original mapping");
                for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,spec.max+1,spec.min-1})try{changed.override(id,spec.key,bad);throw new AssertionError("invalid override accepted");}catch(IllegalArgumentException expected){}
            }
        }
        EffectState raw=state.edit(true,state.mask,state.parameters().override(Effects.PIXEL_DAMAGE,"pixelDensity",1).override(Effects.PIXEL_DAMAGE,"columnDensity",0).override(Effects.PIXEL_DAMAGE,"hotFraction",1).override(Effects.PIXEL_DAMAGE,"hotValue",1));
        byte[] input=new byte[16*16*2];for(int i=0;i<256;i++)RawGlitch.write(input,i,1000);byte[] out=RawGlitch.chain(input,16,16,4095,64,model.apply(raw.single(Effects.PIXEL_DAMAGE).snapshot(false,2),off));for(int i=0;i<256;i++)check(RawGlitch.read(out,i)==4095,"RAW direct hot pixel control");
        EffectState fixed=state.edit(true,state.mask,state.parameters().eventIdentity(Effects.VHS,Long.valueOf(Long.MIN_VALUE)));check(EffectState.decode(fixed.encode()).encode().equals(fixed.encode()),"event identity roundtrip");FaultModel eventA=new FaultModel(1),eventB=new FaultModel(2);sample(eventA,fixed,0,off);sample(eventB,fixed,0,off);for(int i=1;i<100;i++){FaultNode left=node(sample(eventA,fixed,i*.1,off),Effects.VHS),right=node(sample(eventB,fixed,i*.1,off),Effects.VHS);check(left.event.serial==right.event.serial&&left.event.envelope==right.event.envelope&&left.event.pattern==right.event.pattern,"fixed event seed must replay across sessions");}
        LivePerformance p=LivePerformance.defaults().with("tempo",120).with("beats",2);
        check(p.with("clock",LivePerformance.LOOP).time(1.25)==.25,"loop wraps");check(p.with("clock",LivePerformance.LOOP).time(-.25)==.75,"reverse loop wraps");check(p.with("clock",LivePerformance.PING_PONG).time(1.25)==.75,"ping-pong reverses");check(p.with("clock",LivePerformance.STEP).time(.19)==.125,"quantized fault clock");
        LivePerformance seconds=p.with("period",2.5f).with("interval",.2f).with("clock",LivePerformance.STEP).held(true);check(seconds.periodSeconds==2.5f&&seconds.stepSeconds==.2f,"seconds retained through controls");check(Math.abs(seconds.time(.51)-.4)<.00001,"interval is measured directly in seconds");check(Math.abs(seconds.with("clock",LivePerformance.LOOP).time(2.8)-.3)<.00001,"period is measured directly in seconds");
        LivePerformance pulse=p.with("style",LivePerformance.PULSE).with("depth",1);check(pulse.envelope(0,0,3,7)==0&&Math.abs(pulse.envelope(.5,0,3,7)-1)<.0001,"pulse envelope");
        LivePerformance cascade=p.with("style",LivePerformance.CASCADE).with("depth",1);check(cascade.envelope(0,0,3,7)==1&&cascade.envelope(0,1,3,7)<.0001,"cascade directs different stages");
        FaultConfig live=off.enabled(true).performance(p);FaultModel m=new FaultModel(55);sample(m,state,0,live);EffectState.Frame start=sample(m,state,1,live);FaultModel baseline=m.copy(),preview=m.copy();sample(preview,state,2,live.performance(p.with("speed",-2)));EffectState.Frame normal=sample(m,state,2,live);check(normal.describe().equals(sample(baseline,state,2,live).describe()),"preview timeline leaked into committed model");
        FaultConfig held=live.performance(p.held(true));EffectState.Frame a=sample(m,state,3,held),b=sample(m,state,4,held);check(a.time==b.time&&a.cameraNs!=b.cameraNs,"HOLD changes fault time only");for(int id:state.ids())check(node(a,id).describe().equals(node(b,id).describe()),"HOLD did not freeze "+Effects.name(id));
        double position=b.time;EffectState.Frame reversed=sample(m,state,5,live.performance(p.with("speed",-1)));check(reversed.time<position,"negative speed reverses fault clock");
        m.rewind();check(m.apply(state.snapshot(true,0),live).time==0,"transport reset");m.hit();check(node(m.apply(state.snapshot(true,0),live),Effects.STREAM_ERROR).event.envelope==1,"HIT opens incident");
        check(m.apply(state.amount(0).snapshot(true,0),live).nodes.isEmpty(),"zero LEVEL remains bypass even with HIT");
        EffectState.Frame snapshot=m.apply(state.snapshot(true,0),live);String saved=snapshot.describe();sample(m,state,6,live);check(saved.equals(snapshot.describe()),"performance mutated captured snapshot");
        System.out.println("PASS full advanced catalog, direct overrides, RAW behavior, bounds, persistence, performance clocks, HOLD/HIT and preview isolation");
    }
}
