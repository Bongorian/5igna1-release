package com.bongorian.signa1;

import java.util.*;

/** Immutable evaluated fault. Named mechanism parameters have no shared dimensionality/strength. */
final class FaultNode {
    static final class Identity {
        final long seed;final float spatialSeed,bias;
        Identity(long seed){this(seed,FaultModel.random(seed)*997,FaultModel.random(seed^0x12ab34cdL)*2-1);}
        Identity(long seed,float spatialSeed,float bias){this.seed=seed;this.spatialSeed=spatialSeed;this.bias=bias;}
    }
    static final class Motion {
        final double seconds;final float drift,phase;
        Motion(double seconds,float drift,float phase){this.seconds=seconds;this.drift=drift;this.phase=phase;}
    }
    static final class Event {
        final long serial,identity;final float envelope,position,pattern;
        Event(long serial,float envelope,float position,float pattern){this(serial,envelope,position,pattern,0);}
        Event(long serial,float envelope,float position,float pattern,long identity){this.identity=identity;this.serial=serial;this.envelope=envelope;this.position=position;this.pattern=pattern;}
    }
    final int id;final Identity identity;final Motion motion;final Event event;
    final Map<String,Float> profile,mechanism,internal;
    FaultNode(int id,Identity identity,Motion motion,Event event,Map<String,Float> mechanism){this(id,identity,motion,event,Collections.emptyMap(),mechanism);}
    FaultNode(int id,Identity identity,Motion motion,Event event,Map<String,Float> profile,Map<String,Float> mechanism){this(id,identity,motion,event,profile,mechanism,Collections.emptyMap());}
    FaultNode(int id,Identity identity,Motion motion,Event event,Map<String,Float> profile,Map<String,Float> mechanism,Map<String,Float> internal){this.internal=Collections.unmodifiableMap(new LinkedHashMap<>(internal));this.id=id;this.identity=identity;this.motion=motion;this.event=event;this.profile=Collections.unmodifiableMap(new LinkedHashMap<>(profile));this.mechanism=Collections.unmodifiableMap(new LinkedHashMap<>(mechanism));}
    float get(String key){Float value=mechanism.get(key);if(value==null)value=profile.get(key);if(value==null)throw new IllegalArgumentException("Missing signal parameter "+key);return value;}
    Map<String,Float> inspect(){Map<String,Float> result=new LinkedHashMap<>(internal);result.putAll(profile);result.putAll(mechanism);return Collections.unmodifiableMap(result);}
    String describe(){return Effects.name(id)+" profile="+profile+" eventIdentity="+event.identity+" event="+event.serial+":"+event.envelope+" motion="+motion.seconds+":"+motion.drift+":"+motion.phase+" internal="+internal+" faults="+mechanism;}
}
