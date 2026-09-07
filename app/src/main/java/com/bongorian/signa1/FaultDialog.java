package com.bongorian.signa1;
import android.content.res.ColorStateList;
import android.widget.*;
import java.util.Locale;
final class FaultDialog {
    static SeekBar slider(MainActivity a,LinearLayout body,String title,float value,int kind){
        TextView label=a.text("",12,MainActivity.WHITE);label.setPadding(0,a.dp(12),0,0);body.addView(label);
        SeekBar bar=new SeekBar(a);bar.setMax(100);bar.setProgress(Math.round(value*100));bar.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));bar.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));body.addView(bar,new LinearLayout.LayoutParams(-1,a.dp(42)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int v,boolean user){label.setText(title+" · "+(kind==1?String.format(Locale.US,a.getString(R.string.ui_1f_s),.5+v*.095):kind==2?String.format(Locale.US,a.getString(R.string.ui_2f_s),.15+v*.0485):v+"%"));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});bar.setProgress(Math.round(value*100));
        // setProgress does not notify when the value is unchanged.
        label.setText(title+" · "+(kind==1?String.format(Locale.US,a.getString(R.string.ui_1f_s),.5+value*9.5):kind==2?String.format(Locale.US,a.getString(R.string.ui_2f_s),.15+value*4.85):Math.round(value*100)+"%"));return bar;
    }
    static void show(MainActivity a){FaultConfig c=a.faultConfig;LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
        SeekBar interval=slider(a,body,a.getString(R.string.ui_event_interval),c.interval,1),frequency=slider(a,body,a.getString(R.string.ui_occurrence_rate),c.frequency,0),duration=slider(a,body,a.getString(R.string.ui_event_duration),c.duration,2),variation=slider(a,body,a.getString(R.string.ui_parameter_variation),c.variation,0),smoothness=slider(a,body,a.getString(R.string.ui_smoothness),c.smoothness,0);
        Switch chain=new Switch(a);chain.setText(a.getString(R.string.ui_allow_chain_switching));chain.setTextColor(MainActivity.WHITE);chain.setTextSize(13);chain.setChecked(c.allowChain);body.addView(chain);
        LinearLayout range=new LinearLayout(a);range.setOrientation(LinearLayout.HORIZONTAL);body.addView(a.text(a.getString(R.string.ui_random_chain_size_total),12,MainActivity.WHITE));LinearLayout rangeLabels=new LinearLayout(a);for(String label:new String[]{a.getString(R.string.ui_minimum),a.getString(R.string.ui_maximum)}){TextView text=a.text(label,11,MainActivity.MUTED);text.setGravity(android.view.Gravity.CENTER);rangeLabels.addView(text,new LinearLayout.LayoutParams(0,a.dp(24),1));}body.addView(rangeLabels);body.addView(range);
        NumberPicker minimum=new NumberPicker(a),maximum=new NumberPicker(a);for(NumberPicker picker:new NumberPicker[]{minimum,maximum}){picker.setMinValue(1);picker.setMaxValue(Effects.NAMES.length-1);picker.setWrapSelectorWheel(false);picker.setTextColor(MainActivity.WHITE);range.addView(picker,new LinearLayout.LayoutParams(0,a.dp(110),1));}
        minimum.setValue(c.minEffects);maximum.setValue(c.maxEffects);minimum.setContentDescription(a.getString(R.string.ui_minimum_stage_count));maximum.setContentDescription(a.getString(R.string.ui_maximum_stage_count));minimum.setOnValueChangedListener((v,old,next)->{if(maximum.getValue()<next)maximum.setValue(next);});maximum.setOnValueChangedListener((v,old,next)->{if(minimum.getValue()>next)minimum.setValue(next);});
        Switch preserve=new Switch(a);preserve.setText(a.getString(R.string.ui_keep_original_display_stages_vhs_terminal));preserve.setTextColor(MainActivity.WHITE);preserve.setTextSize(13);preserve.setChecked(c.preserveDisplay);body.addView(preserve);
        body.addView(a.text(a.getString(R.string.ui_more_stages_increase_processing_load_the_count_is),11,MainActivity.MUTED));
        SignalSheet.show(a,a.getString(R.string.ui_live_fault_auto_variation),a.getString(R.string.ui_varies_effects_at_the_chosen_interval_and_probability),body,()->a.setFaultConfig(new FaultConfig(a.faultConfig.enabled,false,false,false,false,false,.5f,50,true,chain.isChecked(),interval.getProgress()/100f,frequency.getProgress()/100f,duration.getProgress()/100f,variation.getProgress()/100f,smoothness.getProgress()/100f,preserve.isChecked(),minimum.getValue(),maximum.getValue())));
    }
}
