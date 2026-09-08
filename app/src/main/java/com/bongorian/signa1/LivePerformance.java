package com.bongorian.signa1;

/** Continuous fault-time evolution and modulation. Camera/encoder clocks remain untouched. */
final class LivePerformance {
    static final int NATURAL=0,PULSE=1,SWELL=2,BURST=3,CASCADE=4;
    static final int FREE=0,LOOP=1,PING_PONG=2,STEP=3;
    final int style,clock;final float speed,tempo,depth,beats,division,width,chance,periodSeconds,stepSeconds;final boolean hold;
    LivePerformance(int style,int clock,float speed,float tempo,float depth,float beats,float division,float width,float chance,boolean hold){
        this(style,clock,speed,tempo,depth,beats,division,width,chance,hold,60f/tempo*beats,60f/tempo/division);
    }
    LivePerformance(int style,int clock,float speed,float tempo,float depth,float beats,float division,float width,float chance,boolean hold,float periodSeconds,float stepSeconds){
        this.periodSeconds=FaultModel.clamp(periodSeconds,.25f,32);this.stepSeconds=FaultModel.clamp(stepSeconds,.015625f,2);
        this.style=Math.max(0,Math.min(4,style));this.clock=Math.max(0,Math.min(3,clock));this.speed=FaultModel.clamp(speed,-4,4);this.tempo=FaultModel.clamp(tempo,30,240);this.depth=EffectParameters.unit(depth);this.beats=FaultModel.clamp(beats,1,16);this.division=FaultModel.clamp(division,1,16);this.width=FaultModel.clamp(width,.05f,.95f);this.chance=EffectParameters.unit(chance);this.hold=hold;
    }
    static LivePerformance defaults(){return new LivePerformance(NATURAL,FREE,1,90,.75f,4,4,.3f,.65f,false);}
    LivePerformance held(boolean value){return new LivePerformance(style,clock,speed,tempo,depth,beats,division,width,chance,value,periodSeconds,stepSeconds);}
    LivePerformance with(String key,float value){
        switch(key){
            case "period":return new LivePerformance(style,clock,speed,tempo,depth,beats,division,width,chance,hold,value,stepSeconds);
            case "interval":return new LivePerformance(style,clock,speed,tempo,depth,beats,division,width,chance,hold,periodSeconds,value);
            case "style":return new LivePerformance(Math.round(value),clock,speed,tempo,depth,beats,division,width,chance,hold,periodSeconds,stepSeconds);
            case "clock":return new LivePerformance(style,Math.round(value),speed,tempo,depth,beats,division,width,chance,hold,periodSeconds,stepSeconds);
            case "speed":return new LivePerformance(style,clock,value,tempo,depth,beats,division,width,chance,hold,periodSeconds,stepSeconds);
            case "tempo":return new LivePerformance(style,clock,speed,value,depth,beats,division,width,chance,hold,60f/value*beats,60f/value/division);
            case "depth":return new LivePerformance(style,clock,speed,tempo,value,beats,division,width,chance,hold,periodSeconds,stepSeconds);
            case "beats":return new LivePerformance(style,clock,speed,tempo,depth,value,division,width,chance,hold,60f/tempo*value,stepSeconds);
            case "division":return new LivePerformance(style,clock,speed,tempo,depth,beats,value,width,chance,hold,periodSeconds,60f/tempo/value);
            case "width":return new LivePerformance(style,clock,speed,tempo,depth,beats,division,value,chance,hold,periodSeconds,stepSeconds);
            case "chance":return new LivePerformance(style,clock,speed,tempo,depth,beats,division,width,value,hold,periodSeconds,stepSeconds);
            default:throw new IllegalArgumentException("Performance control");
        }
    }
    static double wrap(double value,double span){return value-Math.floor(value/span)*span;}
    double time(double position){double length=periodSeconds;switch(clock){case LOOP:return wrap(position,length);case PING_PONG:double phase=wrap(position,length*2);return phase<=length?phase:length*2-phase;case STEP:double step=stepSeconds;return Math.floor(position/step)*step;default:return position;}}
    float envelope(double seconds,int rank,int count,long seed){
        double cycle=seconds/periodSeconds,phase=wrap(cycle,1);float envelope;
        switch(style){
            case PULSE:envelope=(float)(.5-.5*Math.cos(phase*Math.PI*2));break;
            case SWELL:envelope=(float)phase;break;
            case BURST:
                if(FaultModel.random(seed^FaultModel.mix((long)Math.floor(cycle)))>=chance||phase>=width)envelope=0;
                else envelope=FaultModel.clamp((float)Math.min(phase/.025,(width-phase)/.075),0,1);break;
            case CASCADE:
                double distance=Math.abs(phase-rank/(double)Math.max(1,count));distance=Math.min(distance,1-distance);envelope=(float)Math.max(0,1-distance*Math.max(1,count));break;
            default:return 1;
        }
        return 1-depth+depth*envelope;
    }
    boolean warped(){return clock!=FREE||speed!=1||hold;}
}
