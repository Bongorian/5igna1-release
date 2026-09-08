package com.bongorian.signa1;

import android.content.res.ColorStateList;
import android.widget.*;
import java.util.*;

/** Every catalog value can follow the model or be fixed explicitly in the immutable draft. */
final class AdvancedControls {
    final EffectDialog editor;final MainActivity a;final int id;
    FaultNode reference;final Map<String,TextView> values=new LinkedHashMap<>();
    AdvancedControls(EffectDialog editor,int id){this.editor=editor;a=editor.a;this.id=id;if(FaultParameters.all(id).stream().noneMatch(spec->spec.group==editor.advancedGroup))editor.advancedGroup=FaultParameters.Group.SIGNAL;reference=new FaultModel(0).inspect(id,editor.draft,editor.amount,a.faultConfig);if(a.shownLiveFrame!=null)update(a.shownLiveFrame);}
    void update(EffectState.Frame frame){for(FaultNode node:frame.nodes)if(node.id==id){reference=node;break;}Map<String,Float> current=reference.inspect();values.forEach((key,view)->{if(!editor.draft.manual(id,key))view.setText(SignalControls.value(current.get(key)));});}
    float current(FaultParameters.Spec spec){return editor.draft.resolved(id,spec.key,reference.inspect().get(spec.key));}
    void refresh(){int y=editor.scroll.getScrollY();editor.renderBody();editor.scroll.post(()->editor.scroll.scrollTo(0,y));}
    void show(LinearLayout body){
        TextView hint=a.text(a.getString(R.string.ui_advanced_hint),11,MainActivity.MUTED);hint.setPadding(0,a.dp(8),0,a.dp(8));body.addView(hint);
        TextView seed=SignalControls.field(a,body,"SEED · "+editor.draft.identity(id));seed.setTag("identity-seed");seed.setTextSize(11);seed.setOnClickListener(v->editor.auxiliary=SignalSheet.number(a,"SEED",a.getString(R.string.ui_seed_hint),Long.toString(editor.draft.identity(id)),true,text->{editor.draft=editor.draft.reseed(id,Long.parseLong(text));editor.preview();refresh();return true;}));
        if(FaultParameters.incidents(id)){
            LinearLayout eventRow=a.row();TextView eventSeed=a.button("EVENT SEED · "+(editor.draft.fixedEventIdentity(id)?Long.toString(editor.draft.eventIdentity(id,0)):"AUTO")),automatic=a.button("AUTO");eventSeed.setTag("event-identity");eventSeed.setTextSize(10);eventRow.addView(eventSeed,new LinearLayout.LayoutParams(0,a.dp(44),1));eventRow.addView(automatic,new LinearLayout.LayoutParams(a.dp(64),a.dp(44)));body.addView(eventRow);
            eventSeed.setOnClickListener(v->editor.auxiliary=SignalSheet.number(a,"EVENT SEED",a.getString(R.string.ui_event_seed_hint),Long.toString(editor.draft.eventIdentity(id,reference.event.identity)),true,text->{editor.draft=editor.draft.eventIdentity(id,Long.valueOf(text));editor.preview();refresh();return true;}));automatic.setOnClickListener(v->{editor.draft=editor.draft.eventIdentity(id,(Long)null);editor.preview();refresh();});
        }
        HorizontalScrollView groups=new HorizontalScrollView(a);groups.setHorizontalScrollBarEnabled(false);LinearLayout row=a.row();groups.addView(row);body.addView(groups,new LinearLayout.LayoutParams(-1,a.dp(48)));
        for(FaultParameters.Group group:FaultParameters.Group.values()){
            if(FaultParameters.all(id).stream().noneMatch(spec->spec.group==group))continue;
            TextView button=a.button(a.getString(groupLabel(group)));button.setTag("group-"+group.name());button.setTextSize(10);button.setTextColor(editor.advancedGroup==group?MainActivity.LIME:MainActivity.MUTED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,a.dp(42));p.rightMargin=a.dp(5);row.addView(button,p);button.setOnClickListener(v->{editor.advancedGroup=group;editor.renderBody();});
        }
        for(FaultParameters.Spec spec:FaultParameters.all(id))if(spec.group==editor.advancedGroup)parameter(body,spec);
        TextView clear=SignalControls.field(a,body,a.getString(R.string.ui_advanced_auto_all));clear.setTag("advanced-auto-all");clear.setOnClickListener(v->{editor.draft=editor.draft.clearOverrides(id);editor.preview();refresh();});
    }
    void parameter(LinearLayout body,FaultParameters.Spec spec){
        boolean manual=editor.draft.manual(id,spec.key);LinearLayout row=a.row();TextView name=a.text(spec.key,11,MainActivity.WHITE),value=a.button(SignalControls.value(current(spec))),mode=a.button(manual?"FIX":"AUTO");
        row.addView(name,new LinearLayout.LayoutParams(0,a.dp(44),1));value.setTextSize(11);value.setTextColor(MainActivity.LIME);value.setBackgroundColor(android.graphics.Color.TRANSPARENT);row.addView(value,new LinearLayout.LayoutParams(a.dp(90),a.dp(44)));values.put(spec.key,value);mode.setTextSize(10);mode.setTextColor(manual?MainActivity.LIME:MainActivity.MUTED);mode.setTag("auto-"+spec.key);row.addView(mode,new LinearLayout.LayoutParams(a.dp(58),a.dp(40)));body.addView(row);
        mode.setContentDescription(a.getString(manual?R.string.ui_advanced_restore_auto:R.string.ui_advanced_fix)+" · "+spec.key);mode.setOnClickListener(v->{editor.draft=manual?editor.draft.automatic(id,spec.key):editor.draft.override(id,spec.key,Math.max(spec.min,Math.min(spec.max,current(spec))));editor.preview();refresh();});
        value.setTag("value-"+spec.key);value.setOnClickListener(v->editor.auxiliary=SignalSheet.number(a,spec.key,SignalControls.value(spec.min)+" … "+SignalControls.value(spec.max),SignalControls.value(current(spec)),spec.step>=1,text->{editor.draft=editor.draft.override(id,spec.key,Float.parseFloat(text));editor.preview();refresh();return true;}));
        if(manual){SeekBar slider=new SeekBar(a);slider.setTag("advanced-"+spec.key);slider.setContentDescription(spec.key);slider.setMax(1000);slider.setProgress(Math.round((current(spec)-spec.min)/(spec.max-spec.min)*1000));slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));body.addView(slider,new LinearLayout.LayoutParams(-1,a.dp(40)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){if(user){float next=spec.min+(spec.max-spec.min)*n/1000f;next=Math.max(spec.min,Math.min(spec.max,spec.min+Math.round((next-spec.min)/spec.step)*spec.step));editor.draft=editor.draft.override(id,spec.key,next);value.setText(SignalControls.value(next));editor.preview();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}
    }
    static int groupLabel(FaultParameters.Group group){switch(group){case TIME:return R.string.ui_advanced_time;case EVENT:return R.string.ui_advanced_event;case PROFILE:return R.string.ui_advanced_profile;default:return R.string.ui_advanced_signal;}}
}
