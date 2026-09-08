package com.bongorian.signa1;

import android.content.*;
import android.os.*;

/** Reads thermal signals independently of artistic LIVE inputs. Invoked only in the foreground. */
final class ThermalMonitor {
    final Context context;final PowerManager power;long headroomAt=Long.MIN_VALUE;
    int status=-1;float batteryC=Float.NaN,headroom=Float.NaN;
    ThermalMonitor(Context context){this.context=context;power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);}
    void sample(long now){
        status=-1;batteryC=Float.NaN;
        if(power!=null)try{status=power.getCurrentThermalStatus();}catch(RuntimeException ignored){}
        try{Intent battery=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(battery!=null&&battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)){float value=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f;if(value>0&&value<90)batteryC=value;}}catch(RuntimeException ignored){}
        if(power!=null&&(headroomAt==Long.MIN_VALUE||now-headroomAt>=10000)){headroomAt=now;try{headroom=power.getThermalHeadroom(10);}catch(RuntimeException ignored){headroom=Float.NaN;}}
    }
}
