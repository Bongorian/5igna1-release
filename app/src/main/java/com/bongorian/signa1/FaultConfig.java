package com.bongorian.signa1;

/** Immutable LIVE direction and optional measured-input coupling. */
final class FaultConfig {
    final boolean enabled,motion,audio,timing,thermal,cpu;final float sensitivity;final int mains;final LivePerformance performance;
    FaultConfig(boolean enabled,boolean motion,boolean audio,boolean timing,boolean thermal,boolean cpu,float sensitivity,int mains){this(enabled,motion,audio,timing,thermal,cpu,sensitivity,mains,LivePerformance.defaults());}
    FaultConfig(boolean enabled,boolean motion,boolean audio,boolean timing,boolean thermal,boolean cpu,float sensitivity,int mains,LivePerformance performance){this.performance=performance;this.enabled=enabled;this.motion=motion;this.audio=audio;this.timing=timing;this.thermal=thermal;this.cpu=cpu;this.sensitivity=EffectParameters.unit(sensitivity);this.mains=mains==60?60:50;}
    static FaultConfig defaults(){return new FaultConfig(false,true,false,true,true,true,.5f,50);}
    FaultConfig enabled(boolean value){return new FaultConfig(value,motion,audio,timing,thermal,cpu,sensitivity,mains,performance);}
    FaultConfig audio(boolean value){return new FaultConfig(enabled,motion,value,timing,thermal,cpu,sensitivity,mains,performance);}
    FaultConfig performance(LivePerformance value){return new FaultConfig(enabled,motion,audio,timing,thermal,cpu,sensitivity,mains,value);}
}
