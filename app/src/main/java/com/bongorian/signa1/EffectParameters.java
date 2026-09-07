package com.bongorian.signa1;

/** Immutable-on-publication settings: per-stage strength, two controls, and pattern seed. */
final class EffectParameters {
    static final int STRIDE=4;

    static float unit(float x){return Float.isNaN(x)?0:Math.max(0,Math.min(1,x));}
    static float[] defaults(){float[] p=new float[Effects.NAMES.length*STRIDE];for(int id=0;id<Effects.NAMES.length;id++){p[id*STRIDE]=1;p[id*STRIDE+1]=.5f;p[id*STRIDE+2]=.5f;p[id*STRIDE+3]=.17f;}p[Effects.CHROMA*STRIDE+1]=0;return p;}
    static float get(float[] p,int id,int slot){return unit(p[id*STRIDE+slot]);}
    static String describe(int[] ids,float[] p){StringBuilder s=new StringBuilder();for(int id:ids)if(id!=Effects.CLEAN){s.append(" | ").append(Effects.name(id));for(int slot=0;slot<STRIDE;slot++)s.append(slot==0?' ':',').append(Math.round(get(p,id,slot)*100));}return s.toString();}
}
