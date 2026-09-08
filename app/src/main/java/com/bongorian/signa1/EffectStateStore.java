package com.bongorian.signa1;

import android.content.SharedPreferences;

final class EffectStateStore {
    static final String KEY="fault_state_v3";
    static EffectState load(SharedPreferences prefs){
        // Fault v3 deliberately resets release effect/AUTO settings; camera preferences survive.
        try{return EffectState.decode(prefs.getString(KEY,""));}
        catch(RuntimeException invalid){return EffectState.defaults();}
    }
    static void write(SharedPreferences.Editor prefs,EffectState state){
        prefs.putString(KEY,state.encode()).remove("effect_state_v2").remove("effect_state_v1").remove("effect").remove("chainMask").remove("amount");
        for(int n=0;n<17*4;n++)prefs.remove("parameter_"+n);
    }
}
