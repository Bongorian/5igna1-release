package com.bongorian.signa1;

/** Bounded internal modulation, with the previous measured-input model retained for regression checks.
 * State advances exactly once per camera frame. Rendering and still snapshots are pure reads. */
final class FaultModel {
    static final class Inputs {
        float ax,ay,az,tilt,rotation,audio,cpu,heat,jitter;
        long sensorNs,exposureNs,skewNs;
        boolean motionAvailable,timingAvailable;
    }
    static final int[] VARIABLE={Effects.SENSOR_FAIL,Effects.EXPOSURE_BAND,Effects.ROW_SHIFT,Effects.LINE_LOSS,
        Effects.BIT_ROT,Effects.DATA_SHIFT,Effects.CFA_TEAR,Effects.CHROMA,Effects.CORRUPT,Effects.PACKET_LOSS,Effects.VHS,Effects.TERMINAL};
    double lastSeconds=Double.NaN,nextEvent=Double.NaN,eventStart=Double.NaN;
    final java.util.Random random=new java.util.Random();
    float envelope,countFraction;final float[] targets=new float[Effects.NAMES.length*4];int[] autoIds=new int[0];
    void advanceInternal(double now,FaultConfig c){
        if(Double.isNaN(nextEvent))nextEvent=now;
        if(now>=nextEvent){nextEvent=now+c.intervalSeconds();
            if(random.nextFloat()<c.frequency){eventStart=now;for(int n=0;n<targets.length;n++)targets[n]=random.nextFloat();
                java.util.ArrayList<Integer> pool=new java.util.ArrayList<>();for(int id:Effects.ORDER)if(id!=Effects.CLEAN)pool.add(id);
                java.util.Collections.shuffle(pool,random);autoIds=pool.stream().mapToInt(Integer::intValue).toArray();countFraction=random.nextFloat();
            }
        }
        double age=now-eventStart,life=c.durationSeconds();float ramp=Math.min((float)life/2,.02f+c.smoothness*1.5f);
        envelope=Double.isNaN(age)||age<0||age>=life?0:c.smoothness==0?1:clamp((float)Math.min(age/ramp,(life-age)/ramp),0,1);
    }
    static int[] selectChain(int[] base,int[] allowed,int[] shuffled,FaultConfig c,float fraction){
        boolean sensor=java.util.Arrays.stream(base).anyMatch(id->id==Effects.SENSOR_FAIL);
        int fixed=0;for(int id:base)if(c.preserveDisplay&&Effects.display(id))fixed|=1<<id;
        final int retained=fixed;int[] pool=java.util.Arrays.stream(shuffled).filter(id->id!=Effects.CLEAN&&(id!=Effects.SENSOR_FAIL||sensor)&&java.util.Arrays.stream(allowed).anyMatch(v->v==id)&&(!(c.preserveDisplay&&retained!=0&&Effects.display(id))||(retained&(1<<id))!=0)).toArray();
        int target=c.minEffects+(int)(clamp(fraction,0,.999999f)*(c.maxEffects-c.minEffects+1));target=Math.min(pool.length,Math.max(Integer.bitCount(fixed),target));
        int selected=0;for(int id:pool)if((fixed&(1<<id))!=0)selected|=1<<id;
        for(int id:pool){if(Integer.bitCount(selected)>=target)break;selected|=1<<id;}
        final int mask=selected;return java.util.Arrays.stream(Effects.ORDER).filter(id->(mask&(1<<id))!=0).toArray();
    }
    EffectState.Frame internalFrame(EffectState.Frame base,FaultConfig c,float clock,int[] allowed){
        float mix=envelope*c.variation;float[] p=base.parameters.clone();
        for(int n=0;n<p.length;n++)p[n]=clamp(p[n]+(targets[n]-p[n])*mix,0,1);
        int[] ids=base.ids.clone();if(c.allowChain&&mix>0&&base.ids.length>0){ids=selectChain(base.ids,allowed,autoIds,c,countFraction);}
        float amount=clamp(base.amount+(targets[0]-base.amount)*mix,0,1);
        return new EffectState.Frame(ids,amount,p,null,mix>0?clock:0);
    }
    float displacement,velocity,shock,pressure,temperature,audio,readout,tilt,rotation;
    long sensorNs,exposureNs,skewNs;
    boolean timingAvailable,motionAvailable;
    static float finite(float value){return Float.isFinite(value)?value:0;}
    static float clamp(float value,float lo,float hi){return Math.max(lo,Math.min(hi,finite(value)));}
    static float follow(float value,float target,float dt,float tau){return target+(value-target)*(float)Math.exp(-dt/tau);}
    void reset(){nextEvent=eventStart=Double.NaN;envelope=0;lastSeconds=Double.NaN;displacement=velocity=shock=pressure=temperature=audio=readout=tilt=rotation=0;timingAvailable=motionAvailable=false;}
    void advance(double now,Inputs input,FaultConfig config){
        if(!Double.isFinite(now))return;
        if(!config.enabled){reset();return;}
        if(config.internal){advanceInternal(now,config);lastSeconds=now;return;}
        float dt=Double.isNaN(lastSeconds)?1/60f:(float)Math.max(0,Math.min(.25,now-lastSeconds));lastSeconds=now;
        float gain=.25f+1.75f*config.sensitivity;
        motionAvailable=config.motion&&input.motionAvailable;
        float ax=motionAvailable?clamp(input.ax/9.80665f,-4,4):0;
        float acceleration=motionAvailable?(float)Math.sqrt(input.ax*input.ax+input.ay*input.ay+input.az*input.az)/9.80665f:0;
        float hit=clamp((acceleration-.06f)*gain,0,1);
        shock=Math.max(hit,follow(shock,0,dt,.38f));
        tilt=motionAvailable?clamp(input.tilt,-1,1):0;rotation=motionAvailable?clamp(input.rotation,-6,6):0;
        audio=follow(audio,config.audio?clamp(input.audio*gain,0,1):0,dt,.12f);
        temperature=follow(temperature,config.thermal?clamp(input.heat,0,1):0,dt,3f);
        readout=follow(readout,config.timing?clamp(input.jitter*gain,0,1):0,dt,.18f);
        if((!config.timing||input.jitter<=0)&&readout<.001f)readout=0;
        float demand=Math.max(readout,config.cpu?clamp((input.cpu-.28f)*1.4f*gain,0,1):0);
        // An error burst drops out as scheduling recovers; it never removes a chain stage.
        pressure=follow(pressure,demand,dt,demand>pressure?.045f:.45f);
        if(demand==0&&pressure<.001f)pressure=0;
        if(hit==0&&shock<.001f)shock=0;
        // Damped tape/head oscillator: signed inertia from acceleration and angular velocity,
        // orientation changes preload, and sound pressure supplies a small oscillating force.
        float target=tilt*.10f;
        float force=(-ax*.75f+rotation*.045f)*gain+audio*.10f*(float)Math.sin(now*2*Math.PI*37);
        int steps=Math.max(1,(int)Math.ceil(dt*120));float step=dt/steps;
        for(int n=0;n<steps;n++){velocity+=(-100*(displacement-target)-12*velocity+force*80)*step;displacement+=velocity*step;displacement=clamp(displacement,-1,1);velocity=clamp(velocity,-8,8);}
        if(Math.abs(displacement)<.00001f&&Math.abs(velocity)<.00001f&&force==0&&target==0)displacement=velocity=0;
        timingAvailable=config.timing&&input.timingAvailable;sensorNs=input.sensorNs;exposureNs=input.exposureNs;skewNs=input.skewNs;
    }
    EffectState.Frame apply(EffectState.Frame base,FaultConfig config,float clock){return apply(base,config,clock,Effects.choices(true,false));}
    EffectState.Frame apply(EffectState.Frame base,FaultConfig config,float clock,int[] allowed){
        if(!config.enabled)return new EffectState.Frame(base.ids.clone(),base.amount,base.parameters.clone(),null,0);
        if(config.internal)return internalFrame(base,config,clock,allowed);
        float[] p=base.parameters.clone(),live=new float[Effects.NAMES.length*4];
        for(int id:base.ids){int at=id*4;float scale=1;
            switch(id){
                // Existing defects stay at fixed sites; temperature changes their prominence.
                case Effects.SENSOR_FAIL:scale=.65f+.35f*temperature;break;
                case Effects.EXPOSURE_BAND:
                    if(timingAvailable){double hz=config.mains*2.;double cycles=(sensorNs%1_000_000_000L)*1e-9*hz;
                        double integration=Math.PI*exposureNs*1e-9*hz;
                        live[at]=1;live[at+1]=(float)((cycles%1)*Math.PI*2);live[at+2]=(float)(skewNs*1e-9*hz*Math.PI*2);
                        live[at+3]=integration<1e-6?1:(float)(Math.sin(integration)/integration);
                    }break;
                case Effects.ROW_SHIFT:
                    scale=clamp(shock+readout,0,1);
                    if(motionAvailable){live[at]=1;live[at+1]=clamp(displacement*.10f+rotation*(skewNs>0?skewNs*1e-9f:.01f),-.25f,.25f);}
                    break;
                case Effects.LINE_LOSS:case Effects.DATA_SHIFT:case Effects.CFA_TEAR:scale=pressure;break;
                case Effects.BIT_ROT:scale=clamp(temperature*.65f+pressure*.35f,0,1);break;
                case Effects.CORRUPT:case Effects.PACKET_LOSS:scale=pressure;break;
                case Effects.CHROMA:scale=clamp(.15f+Math.abs(displacement)+audio*.4f,0,1);break;
                case Effects.VHS:
                    scale=clamp(.18f+shock*.65f+audio*.25f+Math.abs(displacement)*.35f,0,1);
                    live[at]=1;live[at+1]=displacement*.12f;live[at+2]=shock;break;
                case Effects.TERMINAL:scale=clamp(.3f+audio*.7f,0,1);break;
                default:break; // Fixed CFA phase, interpolation, palette and chroma subsampling.
            }
            p[at]*=scale;
        }
        return new EffectState.Frame(base.ids.clone(),base.amount,p,live,clock);
    }
}
