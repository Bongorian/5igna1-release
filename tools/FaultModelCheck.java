package com.bongorian.signa1;

import java.util.Arrays;
/** Behavior checks for the physics envelope, immutable chain and shared frame snapshot. */
public final class FaultModelCheck {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static FaultConfig on(){return new FaultConfig(true,true,true,true,true,true,.5f,50);}
    static FaultModel.Inputs quiet(){FaultModel.Inputs in=new FaultModel.Inputs();in.motionAvailable=true;in.timingAvailable=true;in.sensorNs=123_456_789;in.exposureNs=10_000_000;in.skewNs=12_000_000;return in;}
    public static void main(String[] args){
        int mask=0;for(int id:Effects.ORDER)if(id!=0)mask|=1<<id;
        EffectState base=EffectState.defaults().chain(mask).amount(.8f);String encoded=base.encode();EffectState.Frame frame=base.snapshot(true,0);
        FaultModel model=new FaultModel();FaultConfig config=on();FaultModel.Inputs input=quiet();
        for(int n=0;n<120;n++)model.advance(n/60.,input,config);
        EffectState.Frame stable=model.apply(frame,config,2);
        require(stable.parameters[Effects.PACKET_LOSS*4]==0,"Stable transport must recover to zero");
        require(Arrays.equals(frame.ids,stable.ids),"Zero-strength stages must remain selected");
        input.ax=15;input.jitter=1;input.cpu=1;input.audio=.8f;
        model.advance(2.01,input,config);float hit=model.displacement;
        EffectState.Frame disturbed=model.apply(frame,config,2.01f);
        require(disturbed.parameters[Effects.PACKET_LOSS*4]>0,"Delivery/load must create a transfer burst");
        require(hit<0,"Positive acceleration must produce opposite signed inertia");
        require(Arrays.equals(disturbed.parameters,model.apply(frame,config,2.01f).parameters),"Preview/encoder reads must not advance model");
        require(Arrays.equals(disturbed.live,model.apply(frame,config,2.01f).live),"Per-frame physical snapshot must match");
        for(int id:new int[]{Effects.CFA_OFFSET,Effects.DEMOSAIC,Effects.SPECTRUM,Effects.CHROMA_LOSS})
            for(int slot=0;slot<4;slot++)require(frame.parameters[id*4+slot]==disturbed.parameters[id*4+slot],"Fixed stage changed");
        require(Math.abs(disturbed.live[Effects.EXPOSURE_BAND*4+3])<.00001,"Full flicker cycle exposure should integrate modulation away");
        input=quiet();for(int n=1;n<=900;n++)model.advance(2.01+n/60.,input,config);
        EffectState.Frame recovered=model.apply(frame,config,17);
        require(recovered.parameters[Effects.PACKET_LOSS*4]==0,"Transient fault failed to recover fully");
        require(Math.abs(model.displacement)<.0001&&model.shock==0,"Shock did not decay");
        require(encoded.equals(base.encode()),"Modulation changed persisted manual settings");
        EffectState.Frame disabled=model.apply(frame,config.enabled(false),50);
        require(disabled.time==0&&disabled.live==null&&Arrays.equals(frame.parameters,disabled.parameters),"OFF must restore fixed manual values");
        for(float v:disturbed.parameters)require(Float.isFinite(v)&&v>=0&&v<=1,"Unbounded parameter");
        FaultModel a=new FaultModel(),b=new FaultModel();input=quiet();input.tilt=.5f;input.ax=2;
        for(int n=0;n<180;n++)a.advance(n/60.,input,config);
        for(int n=0;n<90;n++)b.advance(n/30.,input,config);
        require(Math.abs(a.displacement-b.displacement)<.01,"Physics must be independent of preview frame rate");
        EffectState.Frame raw=model.apply(base.forContext(false,2).snapshot(false,2),config,2);
        byte[] data=new byte[16*16*2];Arrays.fill(data,(byte)63);
        byte[] output=RawGlitch.chain(data,16,16,16383,64,raw.ids,raw.amount,2000,raw.parameters,raw.live);
        require(output.length==data.length,"RAW container size changed");
        for(int n=0;n<256;n++)require(RawGlitch.read(output,n)<=16383,"RAW sample escaped white level");
        FaultConfig auto=new FaultConfig(true,false,false,false,false,false,.5f,50,true,false,1,1,.2f,1,0);
        FaultModel generated=new FaultModel();generated.random.setSeed(42);generated.advance(0,quiet(),auto);
        EffectState.Frame changed=generated.apply(frame,auto,0);
        require(!Arrays.equals(changed.parameters,frame.parameters),"Quiet device must still animate parameters");
        require(Arrays.equals(changed.ids,frame.ids),"Chain changed without permission");
        require(Arrays.equals(changed.parameters,generated.apply(frame,auto,0).parameters),"Snapshot reads changed event");
        generated.advance(2,quiet(),auto);require(Arrays.equals(frame.parameters,generated.apply(frame,auto,2).parameters),"Burst failed to return to manual settings");
        FaultConfig chainAuto=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0);
        generated.reset();generated.advance(0,quiet(),chainAuto);EffectState.Frame switched=generated.apply(frame,chainAuto,0);
        require(switched.ids.length>=2&&switched.ids.length<=4,"Allowed chain switching did not select 2-4 stages");
        FaultConfig never=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,0,0,.2f,1,0);
        generated.reset();for(int n=0;n<100;n++){generated.advance(n,quiet(),never);require(Arrays.equals(frame.parameters,generated.apply(frame,never,n).parameters),"Zero frequency generated a burst");}
        require(!FaultConfig.defaults().enabled&&FaultConfig.defaults().internal,"Default must be internal and OFF");
        require(encoded.equals(base.encode()),"Automatic changes mutated manual state");
        int[] ranked=java.util.Arrays.stream(Effects.ORDER).filter(id->id!=Effects.CLEAN).toArray();
        FaultConfig range=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0,true,1,1);
        int[] chosen=FaultModel.selectChain(new int[]{Effects.VHS},ranked,ranked,range,0);
        require(Arrays.equals(chosen,new int[]{Effects.VHS}),"Preserved display must occupy the requested single slot");
        range=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0,false,16,16);
        chosen=FaultModel.selectChain(new int[]{Effects.CHROMA},ranked,ranked,range,0);
        require(chosen.length==15&&Arrays.stream(chosen).noneMatch(id->id==Effects.SENSOR_FAIL),"SENSOR FAIL entered a chain that did not contain it");
        chosen=FaultModel.selectChain(new int[]{Effects.SENSOR_FAIL},ranked,ranked,range,0);
        require(chosen.length==16&&chosen[0]==Effects.SENSOR_FAIL,"Explicit sensor failure must remain eligible");
        range=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0,true,1,1);
        chosen=FaultModel.selectChain(new int[]{Effects.VHS,Effects.TERMINAL},ranked,ranked,range,0);
        require(chosen.length==2,"Requested upper bound erased a preserved display stage");
        range=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0,false,3,6);
        require(FaultModel.selectChain(new int[]{Effects.CHROMA},ranked,ranked,range,0).length==3,"Minimum range ignored");
        require(FaultModel.selectChain(new int[]{Effects.CHROMA},ranked,ranked,range,.999f).length==6,"Maximum range ignored");
        require(FaultModel.selectChain(new int[]{Effects.CHROMA},Effects.choices(false,true),ranked,range,.999f).length==6,"Selection did not use format-supported candidates before choosing count");
        range=new FaultConfig(true,false,false,false,false,false,.5f,50,true,true,1,1,.2f,1,0,false,1,1);
        chosen=FaultModel.selectChain(new int[]{Effects.VHS},ranked,ranked,range,0);
        require(chosen.length==1&&chosen[0]!=Effects.VHS,"Released display remained pinned");
        System.out.println("PASS sensor exclusion, display retention/release, configurable count range, internal modulation, frequency zero, burst recovery, chain permission, causal inertia, decay/recovery, exposure integration, immutable chain, identical frame reads, fixed stages, RAW bounds and frame-rate independence");
    }
}
