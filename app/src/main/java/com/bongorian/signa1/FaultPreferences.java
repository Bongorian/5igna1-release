package com.bongorian.signa1;
import android.content.SharedPreferences;
final class FaultPreferences {
    static FaultConfig load(SharedPreferences p){return new FaultConfig(false,p.getBoolean("fault.v3.motion",true),p.getBoolean("fault.v3.audio",false),p.getBoolean("fault.v3.timing",true),p.getBoolean("fault.v3.thermal",true),p.getBoolean("fault.v3.cpu",true),p.getFloat("fault.v3.sensitivity",.5f),p.getInt("fault.v3.mains",50));}
    static void save(SharedPreferences p,FaultConfig c){p.edit().putBoolean("fault.v3.motion",c.motion).putBoolean("fault.v3.audio",c.audio).putBoolean("fault.v3.timing",c.timing).putBoolean("fault.v3.thermal",c.thermal).putBoolean("fault.v3.cpu",c.cpu).putFloat("fault.v3.sensitivity",c.sensitivity).putInt("fault.v3.mains",c.mains).apply();}
}
