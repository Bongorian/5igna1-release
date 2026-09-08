package com.bongorian.signa1;

import java.util.*;

/** Complete editable catalog of compiled fault values and their time/event generators. */
final class FaultParameters {
    enum Group { TIME, EVENT, SIGNAL, PROFILE }
    static final class Spec {
        final String key;final float min,max,step;final Group group;
        Spec(String key,float min,float max,float step,Group group){this.key=key;this.min=min;this.max=max;this.step=step;this.group=group;}
        float validate(float value){if(!Float.isFinite(value)||value<min||value>max)throw new IllegalArgumentException(key+" range "+min+"…"+max);return value;}
    }
    private static final Map<Integer,List<Spec>> CATALOG=new LinkedHashMap<>();
    static {
        for(int id:Effects.ORDER)if(id!=0){
            List<Spec> p=new ArrayList<>();CATALOG.put(id,p);
            add(p,Group.TIME,"timeScale",-4,4,.01f,"timeOffset",-3600,3600,.1f,"time",-86400,86400,.01f,"driftSpeed",0,10,.01f,"drift",-1,1,.001f,"phaseSpeed",-40,40,.01f,"phase",-6.283186f,6.283186f,.001f,"identityBias",-1,1,.001f);
            if(incidents(id))add(p,Group.EVENT,"eventPeriod",.03f,60,.01f,"eventDuration",.005f,60,.005f,"eventProbability",0,1,.001f,"eventSerial",-1000000,1000000,1,"eventEnvelope",0,1,.001f,"eventPosition",0,1,.001f,"eventPattern",0,997,.1f);
            add(p,Group.SIGNAL,"identitySeed",0,997,.1f,"eventSeed",0,997,.1f);
            switch(id){
                case Effects.PIXEL_DAMAGE:add(p,Group.SIGNAL,"pixelDensity",0,1,.001f,"columnDensity",0,1,.001f,"hotFraction",0,1,.001f,"hotValue",0,1,.001f,"sensorNoise",0,1,.001f,"grainSeed",0,997,.1f);break;
                case Effects.EXPOSURE:add(p,Group.SIGNAL,"exposureDepth",0,2,.001f,"exposurePhase",-6.283186f,6.283186f,.001f,"scanPhase",0,2000,.1f,"integration",-1,1,.001f);break;
                case Effects.ROW_ERROR:add(p,Group.SIGNAL,"weakRows",0,1,.001f,"rowGroups",1,2048,1,"rowOffset",-1,1,.001f,"readoutShear",-1,1,.001f,"lineLoss",0,1,.001f,"linePosition",0,1,.001f,"lineHeight",0,1,.001f,"lineRetention",0,1,.001f);break;
                case Effects.BIT_ERROR:add(p,Group.SIGNAL,"bitProbability",0,1,.001f,"bitIndex",0,1,.001f,"bitBlock",2,512,1);break;
                case Effects.ADDRESS_ERROR:add(p,Group.SIGNAL,"byteOffset",0,4096,1,"addressRegion",2,4096,2,"addressProbability",0,1,.001f);break;
                case Effects.CFA_ERROR:add(p,Group.SIGNAL,"cfaCoverage",0,1,.001f,"cfaPhase",0,2,1,"cfaRegion",2,1024,2);break;
                case Effects.DEMOSAIC_ERROR:add(p,Group.SIGNAL,"interpolationMix",0,1,.001f,"sampleScale",1,64,1);break;
                case Effects.CHROMA_ERROR:add(p,Group.SIGNAL,"chromaOffset",-1,1,.001f,"chromaAngle",-3.141593f,3.141593f,.001f,"chromaBlock",1,512,1);break;
                case Effects.COLOR_MAP:add(p,Group.SIGNAL,"paletteMix",0,1,.001f,"palettePhase",0,1,.001f,"paletteCycles",.01f,32,.01f);break;
                case Effects.BLOCK_ERROR:add(p,Group.SIGNAL,"quantLevels",2,256,1,"blockColumns",1,512,1,"blockError",0,1,.001f,"blockOffset",-1,1,.001f);break;
                case Effects.STREAM_ERROR:add(p,Group.SIGNAL,"streamLoss",0,1,.001f,"streamColumns",1,256,1,"concealment",0,1,.001f);break;
                case Effects.VHS:
                    add(p,Group.PROFILE,"tapeBandwidth",0,1,.001f);
                    add(p,Group.SIGNAL,"trackingOffset",-.5f,.5f,.001f,"trackingWave",0,.25f,.001f,"trackingPhase",-6.283186f,6.283186f,.001f,"trackingSlip",-.5f,.5f,.001f,"tapeDropout",0,1,.001f,"dropoutPosition",0,1,.001f,"tapeNoise",0,1,.001f,"grainSeed",0,997,.1f);break;
                case Effects.CRT:
                    add(p,Group.PROFILE,"scanDepth",0,1,.001f,"scanLines",1,2160,1,"phosphorMix",0,1,.001f);
                    add(p,Group.SIGNAL,"convergenceOffset",-.5f,.5f,.001f,"syncOffset",-.5f,.5f,.001f);break;
                default:throw new IllegalArgumentException("Fault ID");
            }
            CATALOG.put(id,Collections.unmodifiableList(p));
        }
    }
    static boolean incidents(int id){return id==Effects.ROW_ERROR||id==Effects.BIT_ERROR||id==Effects.ADDRESS_ERROR||id==Effects.BLOCK_ERROR||id==Effects.STREAM_ERROR||id==Effects.VHS;}
    private static void add(List<Spec> p,Group group,Object... values){for(int n=0;n<values.length;n+=4)p.add(new Spec((String)values[n],((Number)values[n+1]).floatValue(),((Number)values[n+2]).floatValue(),((Number)values[n+3]).floatValue(),group));}
    static List<Spec> all(int id){List<Spec> p=CATALOG.get(id);if(p==null)throw new IllegalArgumentException("Fault ID");return p;}
    static Spec spec(int id,String key){for(Spec p:all(id))if(p.key.equals(key))return p;throw new IllegalArgumentException("Unknown internal parameter: "+id+" / "+key);}
}
