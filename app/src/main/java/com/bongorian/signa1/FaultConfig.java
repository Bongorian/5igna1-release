package com.bongorian.signa1;

/** Immutable preferences; modulation never writes effect selections or manual parameters. */
final class FaultConfig {
    final boolean enabled,motion,audio,timing,thermal,cpu;
    final float sensitivity;final int mains;
    final boolean preserveDisplay;final int minEffects,maxEffects;
    final boolean internal,allowChain;final float interval,frequency,duration,variation,smoothness;
    FaultConfig(boolean enabled,boolean motion,boolean audio,boolean timing,boolean thermal,boolean cpu,float sensitivity,int mains){
        this(enabled,motion,audio,timing,thermal,cpu,sensitivity,mains,false,false,.35f,.8f,.4f,.8f,.5f);
    }
    FaultConfig(boolean enabled,boolean motion,boolean audio,boolean timing,boolean thermal,boolean cpu,float sensitivity,int mains,boolean internal,boolean allowChain,float interval,float frequency,float duration,float variation,float smoothness){
        this(enabled,motion,audio,timing,thermal,cpu,sensitivity,mains,internal,allowChain,interval,frequency,duration,variation,smoothness,true,2,4);
    }
    FaultConfig(boolean enabled,boolean motion,boolean audio,boolean timing,boolean thermal,boolean cpu,float sensitivity,int mains,boolean internal,boolean allowChain,float interval,float frequency,float duration,float variation,float smoothness,boolean preserveDisplay,int minEffects,int maxEffects){
        this.preserveDisplay=preserveDisplay;this.minEffects=Math.max(1,Math.min(Effects.NAMES.length-1,minEffects));this.maxEffects=Math.max(this.minEffects,Math.min(Effects.NAMES.length-1,maxEffects));
        this.internal=internal;this.allowChain=allowChain;this.interval=EffectParameters.unit(interval);this.frequency=EffectParameters.unit(frequency);this.duration=EffectParameters.unit(duration);this.variation=EffectParameters.unit(variation);this.smoothness=EffectParameters.unit(smoothness);
        this.enabled=enabled;this.motion=motion;this.audio=audio;this.timing=timing;this.thermal=thermal;this.cpu=cpu;
        this.sensitivity=EffectParameters.unit(sensitivity);this.mains=mains==60?60:50;
    }
    static FaultConfig defaults(){return new FaultConfig(false,false,false,false,false,false,.5f,50,true,false,.35f,.8f,.4f,.8f,.5f);}
    FaultConfig enabled(boolean value){return new FaultConfig(value,motion,audio,timing,thermal,cpu,sensitivity,mains,internal,allowChain,interval,frequency,duration,variation,smoothness,preserveDisplay,minEffects,maxEffects);}
    FaultConfig audio(boolean value){return new FaultConfig(enabled,motion,value,timing,thermal,cpu,sensitivity,mains,internal,allowChain,interval,frequency,duration,variation,smoothness,preserveDisplay,minEffects,maxEffects);}
    float intervalSeconds(){return .5f+interval*9.5f;}
    float durationSeconds(){return .15f+duration*4.85f;}
}
