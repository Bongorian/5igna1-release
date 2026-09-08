package com.bongorian.signa1;

import androidx.appcompat.widget.AppCompatToggleButton;

/** App-styled, accessible two-state control without a platform switch track. */
final class SignalToggle extends AppCompatToggleButton {
    final MainActivity a;final String label;
    SignalToggle(MainActivity activity,String text,boolean checked){
        super(activity);a=activity;label=text;setAllCaps(false);a.typography(this,MainActivity.TEXT_LABEL,true);setGravity(android.view.Gravity.CENTER_VERTICAL);setPadding(a.dp(14),0,a.dp(14),0);setMinHeight(a.dp(48));setTextOn(text+"   · ON");setTextOff(text+"   · OFF");setChecked(checked);paint();
    }
    @Override public void setChecked(boolean checked){super.setChecked(checked);if(a!=null)paint();}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);setAlpha(enabled?1:.35f);}
    void paint(){setBackgroundTintList(null);setTextColor(isChecked()?MainActivity.LIME:MainActivity.MUTED);setBackground(a.bg(MainActivity.PANEL,isChecked()?0x665F7940:0));setContentDescription(label);}
}
