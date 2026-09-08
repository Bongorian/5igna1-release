package com.bongorian.signa1;

import java.util.*;

/** One camera-driven timeline. Snapshot evaluation is pure and never draws random numbers. */
final class FaultModel {
    static final class Inputs {
        float ax,ay,az,tilt,rotation,audio,cpu,heat,jitter;
        long sensorNs,exposureNs,skewNs;boolean motionAvailable,timingAvailable;
    }
    private final long sessionSalt;
    private double lastSeconds=Double.NaN,elapsed,performancePosition,cueAge=Double.POSITIVE_INFINITY;
    private boolean liveWasEnabled;private long heldSignalNs;
    float displacement,velocity,shock,pressure,temperature,audio,readout,tilt,rotation;
    long sensorNs,exposureNs,skewNs;boolean timingAvailable,motionAvailable;
    FaultModel(){this(new java.security.SecureRandom().nextLong());}
    FaultModel(long sessionSalt){this.sessionSalt=sessionSalt;}
    FaultModel copy(){FaultModel n=new FaultModel(sessionSalt);n.lastSeconds=lastSeconds;n.elapsed=elapsed;n.performancePosition=performancePosition;n.cueAge=cueAge;n.liveWasEnabled=liveWasEnabled;n.heldSignalNs=heldSignalNs;n.displacement=displacement;n.velocity=velocity;n.shock=shock;n.pressure=pressure;n.temperature=temperature;n.audio=audio;n.readout=readout;n.tilt=tilt;n.rotation=rotation;n.sensorNs=sensorNs;n.exposureNs=exposureNs;n.skewNs=skewNs;n.timingAvailable=timingAvailable;n.motionAvailable=motionAvailable;return n;}
    static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    static float random(long seed){return (mix(seed)>>>40)*0x1.0p-24f;}
    static float finite(float x){return Float.isFinite(x)?x:0;}
    static float clamp(float x,float lo,float hi){return Math.max(lo,Math.min(hi,finite(x)));}
    static float follow(float value,float target,float dt,float tau){return target+(value-target)*(float)Math.exp(-dt/tau);}
    static float drift(long seed,double time){long cell=(long)Math.floor(time);float f=(float)(time-cell);f=f*f*(3-2*f);return (random(seed^mix(cell))*(1-f)+random(seed^mix(cell+1))*f)*2-1;}
    void reset(){lastSeconds=Double.NaN;displacement=velocity=shock=pressure=temperature=audio=readout=tilt=rotation=0;timingAvailable=motionAvailable=false;}
    void hit(){cueAge=0;}
    void rewind(){performancePosition=0;cueAge=Double.POSITIVE_INFINITY;}
    double time(FaultConfig config){return config.enabled?config.performance.time(performancePosition):elapsed;}
    float cue(FaultConfig config){return config.enabled?clamp((float)(1-cueAge/.65),0,1):0;}
    void advance(double now,Inputs input,FaultConfig config){
        if(!Double.isFinite(now)||(!Double.isNaN(lastSeconds)&&now<=lastSeconds))return;
        double delta=Double.isNaN(lastSeconds)?0:now-lastSeconds;lastSeconds=now;elapsed+=delta;
        if(config.enabled&&!liveWasEnabled)performancePosition=elapsed-delta;
        liveWasEnabled=config.enabled;sensorNs=input.sensorNs;
        if(config.enabled&&config.performance.hold)return;
        heldSignalNs=input.sensorNs;
        if(config.enabled)performancePosition+=delta*config.performance.speed;
        cueAge+=delta;
        float dt=(float)Math.min(.25,delta);sensorNs=input.sensorNs;exposureNs=input.exposureNs;skewNs=input.skewNs;
        float gain=.25f+1.75f*config.sensitivity;
        motionAvailable=config.enabled&&config.motion&&input.motionAvailable;
        timingAvailable=config.enabled&&config.timing&&input.timingAvailable;
        float ax=motionAvailable?clamp(input.ax/9.80665f,-4,4):0;
        float acceleration=motionAvailable?clamp((float)Math.sqrt(input.ax*input.ax+input.ay*input.ay+input.az*input.az)/9.80665f,0,4):0;
        float hit=clamp((acceleration-.06f)*gain,0,1);shock=Math.max(hit,follow(shock,0,dt,.38f));
        tilt=motionAvailable?clamp(input.tilt,-1,1):0;rotation=motionAvailable?clamp(input.rotation,-6,6):0;
        audio=follow(audio,config.enabled&&config.audio?clamp(input.audio*gain,0,1):0,dt,.12f);
        temperature=follow(temperature,config.enabled&&config.thermal?clamp(input.heat,0,1):0,dt,3);
        readout=follow(readout,config.enabled&&config.timing?clamp(input.jitter*gain,0,1):0,dt,.18f);
        float demand=Math.max(readout,config.enabled&&config.cpu?clamp((input.cpu-.28f)*1.4f*gain,0,1):0);
        pressure=follow(pressure,demand,dt,demand>pressure?.045f:.45f);
        float force=(-ax*.75f+rotation*.045f)*gain+audio*.10f*(float)Math.sin(elapsed*2*Math.PI*37);
        int steps=Math.max(1,(int)Math.ceil(dt*120));float step=dt/steps;
        for(int i=0;i<steps;i++){velocity+=(-100*(displacement-tilt*.10f)-12*velocity+force*80)*step;displacement=clamp(displacement+velocity*step,-1,1);velocity=clamp(velocity,-8,8);}
        // Disabling/unavailable inputs cannot leave a hidden bias coupled into future frames.
        if(!config.enabled){displacement=velocity=shock=pressure=temperature=audio=readout=tilt=rotation=0;}
    }
    private FaultNode.Event event(long identity,int id,double time,double period,double duration,float probability,long serial){
        double age=LivePerformance.wrap(time,period);
        long key=mix(identity^((long)id<<48)^mix(serial)^0x4556454e54L);
        float gate=random(key)<clamp(probability,0,1)&&age<duration?1:0;
        float envelope=gate*clamp((float)Math.min(age/.025,(duration-age)/.09),0,1);
        return new FaultNode.Event(serial,envelope,random(key+1),random(key+2)*997,identity);
    }
    EffectState.Frame apply(EffectState.Frame base,FaultConfig config){
        List<FaultNode> nodes=new ArrayList<>();int[] ids=base.ids();double time=time(config);float cue=cue(config);
        if(base.amount>0)for(int index=0;index<ids.length;index++){
            float directed=config.enabled?config.performance.envelope(time,index,ids.length,sessionSalt):1;
            float level=clamp(base.amount*directed+(1-base.amount*directed)*cue,0,1);
            // A fully gated stage bypasses its profile as well as its mechanism.
            if(level>0)nodes.add(compile(ids[index],base.parameters,level,config));
        }
        return new EffectState.Frame(ids,base.amount,base.parameters,sensorNs,time,nodes);
    }
    FaultNode inspect(int id,EffectParameters controls,float level,FaultConfig config){return compile(id,controls,level,config);}
    private FaultNode compile(int id,EffectParameters controls,float level,FaultConfig config){
        long seed=controls.identity(id);FaultNode.Identity initial=new FaultNode.Identity(seed);
        FaultNode.Identity identity=new FaultNode.Identity(seed,controls.resolved(id,"identitySeed",initial.spatialSeed),controls.resolved(id,"identityBias",initial.bias));
        float timeScale=controls.resolved(id,"timeScale",1),timeOffset=controls.resolved(id,"timeOffset",0);
        double time=controls.manual(id,"time")?controls.resolved(id,"time",0):time(config)*timeScale+timeOffset;
        float speed=controls.resolved(id,"driftSpeed",id==Effects.VHS?.24f:id==Effects.CRT?.16f:id==Effects.EXPOSURE?.8f:.42f);
        boolean moving=id==Effects.PIXEL_DAMAGE||id==Effects.EXPOSURE||id==Effects.ROW_ERROR||id==Effects.CHROMA_ERROR||id==Effects.BLOCK_ERROR||id==Effects.VHS||id==Effects.CRT;
        float drift=controls.resolved(id,"drift",moving?drift(seed^0x4d4f54494f4eL,time*speed):0);
        float phaseSpeed=controls.resolved(id,"phaseSpeed",id==Effects.EXPOSURE?.5f:1.7f);
        float phase=controls.resolved(id,"phase",(float)((time*phaseSpeed+random(seed)*Math.PI*2)%(Math.PI*2)));
        FaultNode.Motion motion=new FaultNode.Motion(time,drift,phase);
        float coupling=config.enabled?1:0;
        float pressure=this.pressure*coupling,heat=temperature*coupling,move=displacement*coupling;
        float period=controls.resolved(id,"eventPeriod",id==Effects.VHS?2.3f:id==Effects.STREAM_ERROR?1.1f:id==Effects.ROW_ERROR?1.7f:id==Effects.BIT_ERROR?.6f:2.7f);
        float duration=controls.resolved(id,"eventDuration",id==Effects.VHS?.48f:id==Effects.STREAM_ERROR?.38f:.19f);
        float activity=id==Effects.ROW_ERROR||id==Effects.STREAM_ERROR?controls.get(id,"loss"):id==Effects.VHS?Math.max(controls.get(id,"dropout"),controls.get(id,"tracking")*.5f):id==Effects.BIT_ERROR||id==Effects.ADDRESS_ERROR?controls.get(id,"activity"):id==Effects.BLOCK_ERROR?controls.get(id,"misaddress"):0;
        boolean incidents=FaultParameters.incidents(id);
        float probability=controls.resolved(id,"eventProbability",clamp(activity*.8f+pressure*.5f,0,1));
        long serial=controls.manual(id,"eventSerial")?Math.round(controls.resolved(id,"eventSerial",0)):(long)Math.floor(time/period);
        FaultNode.Event automatic=incidents?event(controls.eventIdentity(id,seed^sessionSalt),id,time,period,duration,probability,serial):new FaultNode.Event(-1,0,0,0);
        FaultNode.Event event=new FaultNode.Event(automatic.serial,controls.resolved(id,"eventEnvelope",Math.max(automatic.envelope,cue(config))),controls.resolved(id,"eventPosition",automatic.position),controls.resolved(id,"eventPattern",automatic.pattern),automatic.identity);
        Map<String,Float> internal=new LinkedHashMap<>();put(internal,"timeScale",timeScale,"timeOffset",timeOffset,"time",(float)time,"driftSpeed",speed,"drift",drift,"phaseSpeed",phaseSpeed,"phase",phase,"identityBias",identity.bias);
        if(incidents)put(internal,"eventPeriod",period,"eventDuration",duration,"eventProbability",probability,"eventSerial",(float)event.serial,"eventEnvelope",event.envelope,"eventPosition",event.position,"eventPattern",event.pattern);
        boolean warped=config.enabled&&(config.performance.clock!=LivePerformance.FREE||config.performance.speed!=1)||timeScale!=1||timeOffset!=0||controls.manual(id,"time");
        long signalNs=warped?(long)(time*60)*16_666_667L:config.enabled&&config.performance.hold?heldSignalNs:sensorNs;
        Map<String,Float> p=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        // Domain-specific values, in sample/normalized signal units; no universal strength uniform.
        p.put("identitySeed",identity.spatialSeed);p.put("eventSeed",event.envelope>0?event.pattern:identity.spatialSeed);
        switch(id){
            case Effects.PIXEL_DAMAGE:
                put(p,"pixelDensity",level*controls.get(id,"density")*.025f,"columnDensity",level*controls.get(id,"columns")*.07f,
                    "hotFraction",controls.get(id,"hot"),"hotValue",clamp(.8f+drift*.12f+heat*.3f,0,1),"sensorNoise",heat*level*.035f);
                p.put("grainSeed",random(seed^mix(signalNs))*997);break;
            case Effects.EXPOSURE:
                float rate=controls.get(id,"rate"),bands=controls.get(id,"bands");
                float exposurePhase=(float)((time*rate*18+identity.spatialSeed)%(Math.PI*2)),scan=4+bands*160,integrate=1;
                if(timingAvailable){double hz=config.mains*2.;exposurePhase=(float)((((signalNs%1_000_000_000L)*1e-9*hz%1)*Math.PI*2+time*rate*18)%(Math.PI*2));scan=(float)(skewNs*1e-9*hz*Math.PI*2)*(1+bands*8);double x=Math.PI*exposureNs*1e-9*hz;integrate=x<1e-6?1:(float)(Math.sin(x)/x);}
                put(p,"exposureDepth",level*controls.get(id,"depth"),"exposurePhase",exposurePhase,"scanPhase",scan,"integration",integrate);break;
            case Effects.ROW_ERROR:
                float width=controls.get(id,"displacement")*level;
                put(p,"weakRows",.15f+.65f*level,"rowGroups",12+228*(1-controls.get(id,"bands")),"rowOffset",width*(.08f+drift*.20f),
                    "readoutShear",width*(move*.18f+rotation*coupling*.008f+readout*coupling*.1f),"lineLoss",event.envelope*controls.get(id,"loss")*level,
                    "linePosition",event.position,"lineHeight",.005f+controls.get(id,"bands")*.13f,"lineRetention",controls.get(id,"concealment"));break;
            case Effects.BIT_ERROR:
                put(p,"bitProbability",level*controls.get(id,"activity")*(.08f+.8f*event.envelope+heat*.2f),"bitIndex",controls.get(id,"bit"),"bitBlock",2+controls.get(id,"burst_size")*126);break;
            case Effects.ADDRESS_ERROR:
                put(p,"byteOffset",(float)Math.round(controls.get(id,"offset")*31*level),"addressRegion",4+2*(float)Math.round(controls.get(id,"region")*254),"addressProbability",level*controls.get(id,"activity")*(.25f+.75f*event.envelope));break;
            case Effects.CFA_ERROR:
                put(p,"cfaCoverage",level*controls.get(id,"coverage"),"cfaPhase",(float)Math.round(controls.get(id,"phase")*2),"cfaRegion",2+2*(float)Math.round(controls.get(id,"region")*127));break;
            case Effects.DEMOSAIC_ERROR:
                put(p,"interpolationMix",level*controls.get(id,"interpolation"),"sampleScale",1+(float)Math.floor(controls.get(id,"sampling")*15));break;
            case Effects.CHROMA_ERROR:
                put(p,"chromaOffset",level*controls.get(id,"separation")*(.006f+.065f*(.5f+.5f*drift)),"chromaAngle",controls.get(id,"direction")*(float)Math.PI,
                    "chromaBlock",1+level*controls.get(id,"sampling")*95);break;
            case Effects.COLOR_MAP:
                put(p,"paletteMix",level*controls.get(id,"mix"),"palettePhase",controls.get(id,"palette"),"paletteCycles",.5f+controls.get(id,"cycles")*5);break;
            case Effects.BLOCK_ERROR:
                put(p,"quantLevels",256-(float)Math.floor(level*controls.get(id,"quantization")*252),"blockColumns",8+(1-controls.get(id,"block_size"))*72,
                    "blockError",level*controls.get(id,"misaddress")*event.envelope,"blockOffset",(identity.bias+drift)*.25f);break;
            case Effects.STREAM_ERROR:
                put(p,"streamLoss",level*controls.get(id,"loss")*event.envelope,"streamColumns",8+(1-controls.get(id,"region"))*56,"concealment",controls.get(id,"concealment"));break;
            case Effects.VHS:
                // Media characteristics (bandwidth, chroma leakage) and faults are independent.
                float tracking=level*controls.get(id,"tracking");
                profile.put("tapeBandwidth",level*controls.get(id,"bandwidth"));
                put(p,"trackingOffset",tracking*(identity.bias*.012f+drift*.03f+move*.12f),
                    "trackingWave",tracking*(.003f+audio*coupling*.012f),"trackingPhase",phase,"trackingSlip",tracking*event.envelope*.12f,
                    "tapeDropout",level*controls.get(id,"dropout")*event.envelope,"dropoutPosition",event.position,"tapeNoise",level*controls.get(id,"noise")*.22f);
                p.put("grainSeed",random(seed^mix(signalNs)^0x54415045L)*997);break;
            case Effects.CRT:
                float scanLevel=level*controls.get(id,"scan");
                put(profile,"scanDepth",scanLevel*.5f,"scanLines",240+controls.get(id,"scan")*760,"phosphorMix",level*controls.get(id,"phosphor"));
                put(p,"convergenceOffset",level*controls.get(id,"convergence")*(identity.bias*.009f+drift*.003f),"syncOffset",level*controls.get(id,"sync")*(drift*.035f+move*.025f));break;
            default:throw new IllegalArgumentException("Fault ID");
        }
        for(Map.Entry<String,Float> value:controls.overrides(id).entrySet()){if(profile.containsKey(value.getKey()))profile.put(value.getKey(),value.getValue());if(p.containsKey(value.getKey()))p.put(value.getKey(),value.getValue());}
        return new FaultNode(id,identity,motion,event,profile,p,internal);
    }
    private static void put(Map<String,Float> p,Object... pairs){for(int i=0;i<pairs.length;i+=2)p.put((String)pairs[i],((Number)pairs[i+1]).floatValue());}
}
