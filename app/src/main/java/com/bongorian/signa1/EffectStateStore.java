package com.bongorian.signa1;

import android.content.SharedPreferences;

final class EffectStateStore {
    static final String KEY="effect_state_v2";
    static EffectState load(SharedPreferences prefs){
        // v6 reorders IDs. Never interpret older masks as the new pipeline.
        try{return EffectState.decode(prefs.getString(KEY,""));}
        catch(RuntimeException invalid){return EffectState.defaults();}
    }
    static void write(SharedPreferences.Editor prefs,EffectState state){
        prefs.putString(KEY,state.encode()).remove("effect_state_v1").remove("effect").remove("chainMask").remove("amount");
        for(int n=0;n<Effects.NAMES.length*EffectParameters.STRIDE;n++)prefs.remove("parameter_"+n);
    }
}
