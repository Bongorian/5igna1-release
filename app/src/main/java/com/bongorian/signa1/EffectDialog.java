package com.bongorian.signa1;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Named controls of arbitrary count; a draft only commits on Apply. */
final class EffectDialog {
    final MainActivity a;final MainActivity.EffectPreview edit;final EffectState base;
    EffectParameters draft;final int[] ids;final boolean[] enabled;final boolean single;
    final TextView[] toggles;final LinearLayout[] cards;final List<List<SeekBar>> sliders=new ArrayList<>();
    Dialog sheet;boolean syncing;
    EffectDialog(MainActivity activity,boolean single){a=activity;this.single=single;edit=a.beginEffectPreview();base=edit.base;draft=base.parameters();ids=single?base.ids():Arrays.stream(a.availableEffects()).filter(id->id!=Effects.CLEAN).toArray();enabled=new boolean[ids.length];toggles=new TextView[ids.length];cards=new LinearLayout[ids.length];for(int i=0;i<ids.length;i++){enabled[i]=base.enabled(ids[i]);sliders.add(new ArrayList<>());}}
    EffectState state(){int mask=0;for(int i=0;i<ids.length;i++)if(enabled[i])mask|=1<<ids[i];return base.edit(single?base.chained:true,mask,draft);}
    void preview(){if(!syncing)a.previewEffectEdit(edit,state());}
    void show(){
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);
        if(!single){TextView clear=a.button(a.getString(R.string.ui_clear_selection));content.addView(clear,new LinearLayout.LayoutParams(-1,a.dp(42)));clear.setOnClickListener(v->{Arrays.fill(enabled,false);for(int i=0;i<ids.length;i++)refresh(i);preview();});}
        String stage="";
        for(int i=0;i<ids.length;i++){
            final int index=i,id=ids[i];String group=Effects.stage(id);
            if(!stage.equals(group)){stage=group;TextView label=a.text(group,11,MainActivity.LIME);label.setLetterSpacing(.10f);content.addView(label,new LinearLayout.LayoutParams(-1,a.dp(46)));}
            LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(8));cards[i]=card;
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=a.dp(8);content.addView(card,cp);
            LinearLayout head=a.row();card.addView(head,new LinearLayout.LayoutParams(-1,a.dp(48)));
            TextView title=a.text(Effects.name(id),14,MainActivity.WHITE);head.addView(title,new LinearLayout.LayoutParams(0,-1,1));
            TextView tune=a.button(a.getString(R.string.ui_adjust));head.addView(tune,new LinearLayout.LayoutParams(a.dp(68),a.dp(42)));
            TextView toggle=a.button("");toggles[i]=toggle;head.addView(toggle,new LinearLayout.LayoutParams(a.dp(48),a.dp(42)));
            View.OnClickListener select=v->{enabled[index]=!enabled[index];refresh(index);preview();v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);};title.setOnClickListener(select);toggle.setOnClickListener(select);
            LinearLayout editor=new LinearLayout(a);editor.setOrientation(LinearLayout.VERTICAL);card.addView(editor);
            TextView hint=a.text(a.DESCRIPTIONS[id],11,MainActivity.MUTED);editor.addView(hint,new LinearLayout.LayoutParams(-1,-2));
            for(Effects.Control control:Effects.CONTROLS[id])slider(editor,index,control);
            LinearLayout actions=a.row();editor.addView(actions,new LinearLayout.LayoutParams(-1,a.dp(42)));
            TextView reset=a.button(a.getString(R.string.ui_reset));actions.addView(reset,new LinearLayout.LayoutParams(0,-1,1));
            reset.setOnClickListener(v->{draft=draft.reset(id);syncing=true;for(int n=0;n<Effects.CONTROLS[id].length;n++)sliders.get(index).get(n).setProgress(Math.round(draft.get(id,Effects.CONTROLS[id][n].key)*100));syncing=false;preview();});
            if(id!=Effects.COLOR_MAP){TextView seed=a.button(a.getString(R.string.ui_reseed));actions.addView(seed,new LinearLayout.LayoutParams(0,-1,1));seed.setOnClickListener(v->{draft=draft.reseed(id,new java.security.SecureRandom().nextLong());preview();v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);});}
            tune.setOnClickListener(v->{boolean open=editor.getVisibility()!=View.VISIBLE;editor.setVisibility(open?View.VISIBLE:View.GONE);if(open&&sheet!=null){revealPreview();card.post(()->card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true));}});
            editor.setVisibility(single&&enabled[i]?View.VISIBLE:View.GONE);refresh(i);
        }
        sheet=SignalSheet.show(a,a.getString(R.string.fault_points),a.getString(R.string.ui_preview_apply_to_save_back_to_discard),content,()->a.finishEffectEdit(edit,state(),true),single?.66f:.87f);
        edit.dialog=sheet;if(single)revealPreview();sheet.setOnDismissListener(d->a.finishEffectEdit(edit,base,false));
    }
    void revealPreview(){SignalSheet.resize(a,sheet,.66f);Window window=sheet.getWindow();if(window!=null){WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.10f;window.setAttributes(lp);}}
    void refresh(int i){if(cards[i]==null)return;cards[i].setBackground(a.bg(MainActivity.PANEL,16,enabled[i]?0x665F7940:0));toggles[i].setText(enabled[i]?"✓":"＋");toggles[i].setTextColor(enabled[i]?MainActivity.BG:MainActivity.MUTED);toggles[i].setBackground(a.bg(enabled[i]?MainActivity.LIME:MainActivity.BG,12,0));}
    static int controlLabel(String key){switch(key){
        case "density":return R.string.fault_control_density;
        case "hot":return R.string.fault_control_hot;
        case "columns":return R.string.fault_control_columns;
        case "depth":return R.string.fault_control_depth;
        case "rate":return R.string.fault_control_rate;
        case "bands":return R.string.fault_control_bands;
        case "displacement":return R.string.fault_control_displacement;
        case "loss":return R.string.fault_control_loss;
        case "concealment":return R.string.fault_control_concealment;
        case "activity":return R.string.fault_control_activity;
        case "bit":return R.string.fault_control_bit;
        case "burst_size":return R.string.fault_control_burst_size;
        case "offset":return R.string.fault_control_offset;
        case "region":return R.string.fault_control_region;
        case "coverage":return R.string.fault_control_coverage;
        case "phase":return R.string.fault_control_phase;
        case "interpolation":return R.string.fault_control_interpolation;
        case "sampling":return R.string.fault_control_sampling;
        case "separation":return R.string.fault_control_separation;
        case "direction":return R.string.fault_control_direction;
        case "palette":return R.string.fault_control_palette;
        case "cycles":return R.string.fault_control_cycles;
        case "mix":return R.string.fault_control_mix;
        case "quantization":return R.string.fault_control_quantization;
        case "block_size":return R.string.fault_control_block_size;
        case "misaddress":return R.string.fault_control_misaddress;
        case "bandwidth":return R.string.fault_control_bandwidth;
        case "tracking":return R.string.fault_control_tracking;
        case "dropout":return R.string.fault_control_dropout;
        case "noise":return R.string.fault_control_noise;
        case "scan":return R.string.fault_control_scan;
        case "phosphor":return R.string.fault_control_phosphor;
        case "convergence":return R.string.fault_control_convergence;
        case "sync":return R.string.fault_control_sync;
        default:throw new IllegalArgumentException("Unknown fault control "+key);
    }}
    void slider(LinearLayout target,int index,Effects.Control control){
        int id=ids[index],resource=controlLabel(control.key);
        LinearLayout row=a.row();TextView name=a.text(a.getString(resource),11,MainActivity.MUTED),value=a.text("",11,MainActivity.LIME);name.setMinHeight(a.dp(26));row.addView(name,new LinearLayout.LayoutParams(0,-2,1));row.addView(value);target.addView(row);
        SeekBar slider=new SeekBar(a);sliders.get(index).add(slider);slider.setMax(100);slider.setProgress(Math.round(draft.get(id,control.key)*100));value.setText(slider.getProgress()+"%");slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));target.addView(slider,new LinearLayout.LayoutParams(-1,a.dp(38)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){draft=draft.with(id,control.key,n/100f);value.setText(n+"%");preview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    }
}
