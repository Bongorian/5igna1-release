package com.bongorian.signa1;

/** Bounded preview work with fast load shedding and slow recovery. No fault state is changed. */
final class AdaptiveLoad {
    static final int[] RATES={6,8,12,15,20,24};
    int previewFps=24,thermalLevel;boolean cooling,expert;double renderMillis;long relaxedSince=-1,coolSince=-1,nextPreviewNs;
    void setExpert(boolean enabled,int cameraFps){if(expert!=enabled){resetClock();relaxedSince=coolSince=-1;previewFps=24;}expert=enabled;if(enabled){previewFps=Math.max(1,cameraFps);cooling=false;}}
    void sample(long nowMillis,int status,float batteryC,float headroom,long pixels,int passes,boolean constrained){
        int level=Math.max(0,status);
        if(Float.isFinite(batteryC)&&batteryC>0&&batteryC<90)level=Math.max(level,batteryC>=48?4:batteryC>=45?3:batteryC>=42?2:batteryC>=40?1:0);
        if(Float.isFinite(headroom)&&headroom>=0)level=Math.max(level,headroom>=1?3:headroom>=.8f?2:headroom>=.6f?1:0);
        thermalLevel=level;if(expert){cooling=false;coolSince=relaxedSince=-1;return;}
        if(level>=4){cooling=true;coolSince=-1;}else if(cooling){if(level<=1){if(coolSince<0)coolSince=nowMillis;if(nowMillis-coolSince>=30000){cooling=false;coolSince=-1;}}else coolSince=-1;}
        double budget=constrained?90_000_000.0:180_000_000.0;
        int cap=Math.min(constrained?20:24,(int)(budget/Math.max(1,pixels*Math.max(1L,passes))));
        if(renderMillis>0)cap=Math.min(cap,(int)(650/renderMillis));
        cap=Math.min(cap,level>=3?6:level==2?12:level==1?20:24);
        int target=6;for(int rate:RATES)if(rate<=cap)target=rate;
        if(target<previewFps){previewFps=target;relaxedSince=-1;}else if(target>previewFps){if(relaxedSince<0)relaxedSince=nowMillis;if(nowMillis-relaxedSince>=15000){for(int rate:RATES)if(rate>previewFps){previewFps=rate;break;}relaxedSince=nowMillis;}}else relaxedSince=-1;
    }
    void rendered(double milliseconds){if(Double.isFinite(milliseconds)&&milliseconds>0)renderMillis=renderMillis==0?milliseconds:renderMillis*.9+Math.min(milliseconds,500)*.1;}
    boolean due(long cameraNs){return expert||nextPreviewNs==0||cameraNs>=nextPreviewNs;}
    void presented(long cameraNs){long interval=intervalNs();nextPreviewNs=nextPreviewNs==0||cameraNs-nextPreviewNs>interval?cameraNs+interval:nextPreviewNs+interval;}
    void resetClock(){nextPreviewNs=0;}
    long intervalNs(){return 1_000_000_000L/previewFps;}
    int cameraFps(){return expert?previewFps:previewFps<=15?15:30;}
}
