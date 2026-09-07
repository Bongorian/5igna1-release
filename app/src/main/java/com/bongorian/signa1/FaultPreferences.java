package com.bongorian.signa1;
import android.content.SharedPreferences;
final class FaultPreferences {
    static FaultConfig load(SharedPreferences p){return new FaultConfig(false,false,false,false,false,false,.5f,50,true,p.getBoolean("fault.auto.chain",false),p.getFloat("fault.auto.interval",.35f),p.getFloat("fault.auto.frequency",.8f),p.getFloat("fault.auto.duration",.4f),p.getFloat("fault.auto.variation",.8f),p.getFloat("fault.auto.smoothness",.5f),p.getBoolean("fault.auto.preserveDisplay",true),p.getInt("fault.auto.min",2),p.getInt("fault.auto.max",4));}
    static void save(SharedPreferences p,FaultConfig c){p.edit().putBoolean("fault.auto.chain",c.allowChain).putFloat("fault.auto.interval",c.interval).putFloat("fault.auto.frequency",c.frequency).putFloat("fault.auto.duration",c.duration).putFloat("fault.auto.variation",c.variation).putFloat("fault.auto.smoothness",c.smoothness).putBoolean("fault.auto.preserveDisplay",c.preserveDisplay).putInt("fault.auto.min",c.minEffects).putInt("fault.auto.max",c.maxEffects).apply();}
}
