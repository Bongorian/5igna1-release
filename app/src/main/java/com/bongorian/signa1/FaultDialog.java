package com.bongorian.signa1;

import android.content.res.ColorStateList;
import android.widget.*;

/** Device coupling only. Intrinsic motion/events are edited on their own fault. */
final class FaultDialog {
    static Switch source(MainActivity a,LinearLayout body,int label,boolean value){Switch s=new Switch(a);s.setText(a.getString(label));s.setTextColor(MainActivity.WHITE);s.setTextSize(13);s.setChecked(value);body.addView(s,new LinearLayout.LayoutParams(-1,a.dp(50)));return s;}
    static void show(MainActivity a){
        FaultConfig c=a.faultConfig;LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
        Switch motion=source(a,body,R.string.fault_motion,c.motion),timing=source(a,body,R.string.fault_timing,c.timing),thermal=source(a,body,R.string.fault_thermal,c.thermal),cpu=source(a,body,R.string.fault_cpu,c.cpu),audio=source(a,body,R.string.fault_audio,c.audio);
        TextView label=a.text(a.getString(R.string.fault_sensitivity),12,MainActivity.WHITE);body.addView(label);
        SeekBar gain=new SeekBar(a);gain.setMax(100);gain.setProgress(Math.round(c.sensitivity*100));gain.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));gain.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));body.addView(gain);
        Switch mains=source(a,body,R.string.fault_mains_60,c.mains==60);
        SignalSheet.show(a,a.getString(R.string.fault_live_title),a.getString(R.string.fault_live_description),body,()->a.setFaultConfig(new FaultConfig(a.faultConfig.enabled,motion.isChecked(),audio.isChecked(),timing.isChecked(),thermal.isChecked(),cpu.isChecked(),gain.getProgress()/100f,mains.isChecked()?60:50)));
    }
}
