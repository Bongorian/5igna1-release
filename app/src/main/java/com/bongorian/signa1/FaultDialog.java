package com.bongorian.signa1;

import android.app.Dialog;
import android.view.View;
import android.widget.*;
import java.util.function.Consumer;

/** Fault timeline first; optional modulation and measured device inputs. */
final class FaultDialog {
    static String[] styles(MainActivity a){return new String[]{a.getString(R.string.live_natural),a.getString(R.string.live_pulse),a.getString(R.string.live_swell),a.getString(R.string.live_burst),a.getString(R.string.live_cascade)};}
    static String[] clocks(MainActivity a){return new String[]{a.getString(R.string.live_free),a.getString(R.string.live_loop),a.getString(R.string.live_ping_pong),a.getString(R.string.live_step)};}
    static Dialog show(MainActivity a){if(a.recording){Toast.makeText(a,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return null;}a.cancelEffectPreview();if(a.liveEditor!=null)a.liveEditor.dialog.dismiss();Editor editor=new Editor(a);a.liveEditor=editor;return editor.show();}
    static void timeButtons(MainActivity a,LinearLayout row,TextView... buttons){timeButtons(a,row,buttons[0],buttons[1],buttons[2],40);}
    static void timeButtons(MainActivity a,LinearLayout row,TextView first,TextView second,TextView third,int height){TextView[] buttons={first,second,third};for(int n=0;n<buttons.length;n++){TextView button=buttons[n];button.setTextSize(11);button.setSingleLine(true);button.setPadding(a.dp(4),0,a.dp(4),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(height),1);if(n<2)p.rightMargin=a.dp(8);row.addView(button,p);}}
    static final class Editor {
        final MainActivity a;boolean enabled,motion,audio,timing,thermal,cpu,sources;float sensitivity;int mains;LivePerformance performance;
        Dialog dialog,child;LinearLayout body;TextView status;boolean finished;
        Editor(MainActivity a){this.a=a;FaultConfig c=a.faultConfig;enabled=c.enabled;motion=c.motion;audio=c.audio;timing=c.timing;thermal=c.thermal;cpu=c.cpu;sensitivity=c.sensitivity;mains=c.mains;performance=c.performance;}
        FaultConfig state(){return new FaultConfig(enabled,motion,audio,timing,thermal,cpu,sensitivity,mains,performance);}
        void preview(){if(!finished)a.engine.previewFaultConfig(state());}
        Dialog show(){body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);render();dialog=SignalSheet.show(a,a.getString(R.string.fault_live_title),a.getString(R.string.live_editor_hint),body,()->{a.engine.finishFaultPreview(true);a.setFaultConfig(state());},.53f);int usable=a.cameraRoot.getHeight()-a.cameraRoot.getPaddingTop()-a.cameraRoot.getPaddingBottom();int height=Math.min(a.dp(460),Math.round(usable*.53f));dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);dialog.getWindow().setLayout(a.cameraRoot.getWidth()-a.dp(16),height);a.reserveEffectEditor(this,height);dialog.setOnDismissListener(d->{finished=true;if(child!=null)child.dismiss();if(a.liveEditor==this)a.liveEditor=null;a.engine.finishFaultPreview(false);a.restoreEffectEditor(this);a.renderEffects();a.handler.removeCallbacks(update);});a.handler.post(update);preview();return dialog;}
        final Runnable update=new Runnable(){public void run(){if(finished)return;if(status!=null)status.setText(a.engine.faultStatus);a.handler.postDelayed(this,500);}};
        void toggle(int label,boolean value,Consumer<Boolean> changed){SignalToggle control=new SignalToggle(a,a.getString(label),value);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(48));p.bottomMargin=a.dp(8);body.addView(control,p);control.setOnCheckedChangeListener((v,checked)->{changed.accept(checked);preview();});}
        void setting(int label,String[] choices,int selected,String key){TextView row=SignalControls.field(a,body,a.getString(label)+" · "+choices[selected]+" ▾");row.setTag("live-"+key);row.setOnClickListener(v->child=SignalSheet.pick(a,a.getString(label),choices,selected,n->{performance=performance.with(key,n);preview();render();}));}
        void slider(int label,String key,float value,float min,float max,float step,String suffix){SignalControls.slider(a,body,a.getString(label),value,min,max,step,suffix,n->{performance=performance.with(key,key.equals("depth")?n/100:n);preview();}).setTag("live-"+key);}
        void render(){
            body.removeAllViews();status=null;
            toggle(R.string.live_enabled,enabled,value->enabled=value);
            setting(R.string.live_clock,clocks(a),performance.clock,"clock");
            slider(R.string.live_speed,"speed",performance.speed,-4,4,.05f,"×");
            setting(R.string.live_style,styles(a),performance.style,"style");
            if(performance.style!=LivePerformance.NATURAL||performance.clock==LivePerformance.LOOP||performance.clock==LivePerformance.PING_PONG)slider(R.string.live_cycle,"period",performance.periodSeconds,.25f,32,.05f," s");
            if(performance.style!=LivePerformance.NATURAL)slider(R.string.live_depth,"depth",performance.depth*100,0,100,1,"%");
            if(performance.clock==LivePerformance.STEP)slider(R.string.live_division,"interval",performance.stepSeconds,.015625f,2,.015625f," s");
            if(performance.style==LivePerformance.BURST){slider(R.string.live_burst_width,"width",performance.width,.05f,.95f,.01f,"");slider(R.string.live_chance,"chance",performance.chance,0,1,.01f,"");}
            LinearLayout transport=a.row();body.addView(transport,new LinearLayout.LayoutParams(-1,a.dp(48)));
            TextView hold=a.button(a.getString(performance.hold?R.string.live_resume:R.string.live_pause)),hit=a.button(a.getString(R.string.live_trigger)),reset=a.button(a.getString(R.string.live_reset));hold.setTextColor(performance.hold?MainActivity.LIME:MainActivity.MUTED);hold.setContentDescription(a.getString(R.string.live_hold_hint));hit.setContentDescription(a.getString(R.string.live_hit_hint));reset.setContentDescription(a.getString(R.string.live_reset_hint));timeButtons(a,transport,hold,hit,reset,44);hold.setOnClickListener(v->{performance=performance.held(!performance.hold);preview();render();});hit.setOnClickListener(v->{if(enabled)a.engine.hitFaults();});reset.setOnClickListener(v->a.engine.rewindFaults());
            TextView input=SignalControls.field(a,body,a.getString(R.string.live_sources)+(sources?" ▴":" ▾"));input.setTextColor(MainActivity.LIME);input.setTag("live-sources");input.setOnClickListener(v->{sources=!sources;render();});
            if(sources){
                toggle(R.string.fault_motion,motion,value->motion=value);toggle(R.string.fault_audio,audio,value->audio=value);toggle(R.string.fault_timing,timing,value->timing=value);toggle(R.string.fault_thermal,thermal,value->thermal=value);toggle(R.string.fault_cpu,cpu,value->cpu=value);
                SignalControls.slider(a,body,a.getString(R.string.fault_sensitivity),sensitivity*100,0,100,1,"%",value->{sensitivity=value/100;preview();});
                TextView mainsField=SignalControls.field(a,body,a.getString(R.string.live_mains)+" · "+mains+" Hz ▾");mainsField.setOnClickListener(v->child=SignalSheet.pick(a,a.getString(R.string.live_mains),new String[]{"50 Hz","60 Hz"},mains==60?1:0,n->{mains=n==0?50:60;preview();render();}));
                status=a.text(a.engine.faultStatus,10,MainActivity.MUTED);status.setPadding(0,a.dp(8),0,a.dp(8));body.addView(status);
            }
        }
    }
}
