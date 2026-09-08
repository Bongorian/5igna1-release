package com.bongorian.signa1;

import java.util.*;

/** Immutable named UI macros. There is no strength/param1/param2/seed stride or physical ABI. */
final class EffectParameters {
    private final Map<Integer,Map<String,Float>> values;
    private final Map<Integer,Long> identities;
    private EffectParameters(Map<Integer,Map<String,Float>> values,Map<Integer,Long> identities){
        Map<Integer,Map<String,Float>> copy=new LinkedHashMap<>();
        values.forEach((id,p)->copy.put(id,Collections.unmodifiableMap(new LinkedHashMap<>(p))));
        this.values=Collections.unmodifiableMap(copy);this.identities=Collections.unmodifiableMap(new LinkedHashMap<>(identities));
    }
    static float unit(float value){return Float.isFinite(value)?Math.max(0,Math.min(1,value)):0;}
    static EffectParameters defaults(){
        Map<Integer,Map<String,Float>> values=new LinkedHashMap<>();Map<Integer,Long> identities=new LinkedHashMap<>();
        for(int id:Effects.ORDER){Map<String,Float> controls=new LinkedHashMap<>();for(Effects.Control c:Effects.CONTROLS[id])controls.put(c.key,c.initial);values.put(id,controls);identities.put(id,0x51a1L+id*1000003L);}
        return new EffectParameters(values,identities);
    }
    float get(int id,String key){Float v=values.get(id).get(key);if(v==null)throw new IllegalArgumentException("Unknown control: "+id+" / "+key);return v;}
    long identity(int id){return identities.get(id);}
    EffectParameters with(int id,String key,float value){
        get(id,key);Map<Integer,Map<String,Float>> copy=new LinkedHashMap<>(values);Map<String,Float> controls=new LinkedHashMap<>(values.get(id));controls.put(key,unit(value));copy.put(id,controls);return new EffectParameters(copy,identities);
    }
    EffectParameters reseed(int id,long seed){Map<Integer,Long> copy=new LinkedHashMap<>(identities);if(!copy.containsKey(id))throw new IllegalArgumentException("Fault ID");copy.put(id,seed);return new EffectParameters(values,copy);}
    EffectParameters reset(int id){EffectParameters result=this;for(Effects.Control c:Effects.CONTROLS[id])result=result.with(id,c.key,c.initial);return result;}
    String encode(){StringBuilder s=new StringBuilder();for(int id:Effects.ORDER){if(id==0)continue;s.append(';').append(id).append(':').append(identity(id));for(Effects.Control c:Effects.CONTROLS[id])s.append(',').append(c.key).append('=').append(get(id,c.key));}return s.toString();}
    static EffectParameters decode(String text){
        EffectParameters p=defaults();Set<Integer> seen=new HashSet<>();
        for(String group:text.split(";")){if(group.isEmpty())continue;String[] entries=group.split(",");String[] head=entries[0].split(":");if(head.length!=2)throw new IllegalArgumentException("Fault identity");int id=Integer.parseInt(head[0]);
            if(id<=0||id>=Effects.NAMES.length||!seen.add(id))throw new IllegalArgumentException("Fault ID");p=p.reseed(id,Long.parseLong(head[1]));Set<String> keys=new HashSet<>();
            for(int i=1;i<entries.length;i++){String[] pair=entries[i].split("=");if(pair.length!=2||!keys.add(pair[0]))throw new IllegalArgumentException("Fault control");float value=Float.parseFloat(pair[1]);if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Control bounds");p=p.with(id,pair[0],value);}
            if(keys.size()!=Effects.CONTROLS[id].length)throw new IllegalArgumentException("Missing controls");
        }
        if(seen.size()!=Effects.NAMES.length-1)throw new IllegalArgumentException("Missing faults");return p;
    }
    String describe(int[] ids){StringBuilder s=new StringBuilder();for(int id:ids){s.append(" | ").append(Effects.name(id)).append(" identity=").append(identity(id));for(Effects.Control c:Effects.CONTROLS[id])s.append(' ').append(c.key).append('=').append(get(id,c.key));}return s.toString();}
}
