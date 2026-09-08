package com.bongorian.signa1;

import android.app.ActivityManager;
import android.content.Context;

/** Conservative starting budget, refined by supported camera modes and runtime render/heat feedback. */
final class DeviceProfile {
    final boolean constrained;
    DeviceProfile(Context context){ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();if(manager!=null)manager.getMemoryInfo(memory);constrained=manager!=null&&(manager.isLowRamDevice()||memory.totalMem>0&&memory.totalMem<5L*1024*1024*1024);}
    long photoPixels(){return constrained?1_000_000:2_073_600;}
    long videoPixels(){return constrained?921_600:2_073_600;}
}
