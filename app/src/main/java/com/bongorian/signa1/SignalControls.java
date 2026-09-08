package com.bongorian.signa1;

import android.content.res.ColorStateList;
import android.widget.*;
import java.util.Locale;
import java.util.function.Consumer;

final class SignalControls {
    static String value(float value){if(Math.abs(value-Math.round(value))<.00001f)return Long.toString(Math.round(value));return String.format(Locale.US,"%.4f",value).replaceAll("0+$","").replaceAll("\\.$","");}
    static TextView field(MainActivity a,LinearLayout body,String text){TextView row=a.button(text);a.typography(row,MainActivity.TEXT_LABEL,true);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(a.dp(14),a.dp(8),a.dp(14),a.dp(8));row.setMinHeight(a.dp(48));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=a.dp(8);body.addView(row,p);return row;}
    static SeekBar slider(MainActivity a,LinearLayout body,String label,float value,float min,float max,float step,String suffix,Consumer<Float> changed){
        LinearLayout row=a.row();TextView title=a.text(label,12,MainActivity.MUTED),number=a.button("");number.setTextColor(MainActivity.LIME);number.setBackgroundColor(android.graphics.Color.TRANSPARENT);row.addView(title,new LinearLayout.LayoutParams(0,a.dp(38),1));row.addView(number,new LinearLayout.LayoutParams(-2,a.dp(38)));body.addView(row);
        SeekBar slider=new SeekBar(a);slider.setContentDescription(label);int count=Math.max(1,Math.min(10000,Math.round((max-min)/step)));slider.setMax(count);slider.setProgress(Math.round((value-min)/(max-min)*count));slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));body.addView(slider,new LinearLayout.LayoutParams(-1,a.dp(42)));number.setText(value(value)+suffix);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar view,int p,boolean user){if(user){float next=Math.max(min,Math.min(max,min+Math.round((p/(float)count*(max-min))/step)*step));number.setText(value(next)+suffix);changed.accept(next);}}public void onStartTrackingTouch(SeekBar view){}public void onStopTrackingTouch(SeekBar view){}});
        number.setOnClickListener(v->SignalSheet.number(a,label,value(min)+" … "+value(max),value(min+(max-min)*slider.getProgress()/count),false,text->{float next=Float.parseFloat(text);if(!Float.isFinite(next)||next<min||next>max)return false;slider.setProgress(Math.round((next-min)/(max-min)*count));number.setText(value(next)+suffix);changed.accept(next);return true;}));return slider;
    }
}
