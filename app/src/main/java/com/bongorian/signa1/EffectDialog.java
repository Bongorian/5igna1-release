package com.bongorian.signa1;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Draft route editor. Its reserved space keeps every slider below the camera preview. */
final class EffectDialog {
    final MainActivity a;final MainActivity.EffectPreview edit;final EffectState base;
    final int[] ids;EffectParameters draft;int mask,focused;float amount;boolean chained,tuning;
    AdvancedControls advancedControls;FaultParameters.Group advancedGroup=FaultParameters.Group.SIGNAL;
    Dialog sheet,auxiliary;LinearLayout route,body;ScrollView scroll;
    final Map<Integer,TextView> choices=new LinkedHashMap<>();
    EffectDialog(MainActivity activity,boolean single){
        a=activity;edit=a.beginEffectPreview();base=edit.base;draft=base.parameters();mask=base.mask;amount=base.amount;
        ids=Arrays.stream(Effects.ORDER).filter(id->id!=Effects.CLEAN).toArray();
        focused=base.selected();chained=true;tuning=single&&focused!=Effects.CLEAN;
    }
    EffectState state(){return base.edit(chained,mask,draft).amount(amount);}
    void preview(){a.previewEffectEdit(edit,state());}
    TextView action(int text){TextView view=a.button(a.getString(text));view.setTextSize(11);return view;}
    void show(){
        sheet=new Dialog(a);sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(8));root.setBackground(a.bg(MainActivity.BG,MainActivity.PANEL));
        LinearLayout toolbar=a.row();root.addView(toolbar,new LinearLayout.LayoutParams(-1,a.dp(48)));
        TextView cancel=action(R.string.ui_back);cancel.setText("×");cancel.setTextSize(24);toolbar.addView(cancel,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));cancel.setOnClickListener(v->sheet.dismiss());
        TextView heading=a.title(a.getString(R.string.fault_chain_editor));heading.setGravity(Gravity.CENTER);toolbar.addView(heading,new LinearLayout.LayoutParams(0,-1,1));
        ImageView random=a.iconButton(R.drawable.ic_shuffle,a.getString(R.string.ui_random_chain));random.setTag("random-chain");toolbar.addView(random,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        random.setOnClickListener(v->{if(a.rawOriginal())return;EffectState next=EffectRandomizer.chain(state(),a.availableEffects(),new java.security.SecureRandom());mask=next.mask;amount=next.amount;draft=next.parameters();focused=next.selected();renderRoute();renderBody();preview();});random.setEnabled(!a.rawOriginal());random.setAlpha(a.rawOriginal()?.35f:1);
        TextView apply=action(R.string.ui_apply);apply.setTag("apply");apply.setText("✓");apply.setTextSize(22);apply.setTextColor(MainActivity.BG);apply.setBackground(a.bg(MainActivity.LIME,0));toolbar.addView(apply,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));apply.setOnClickListener(v->{a.finishEffectEdit(edit,state(),true);sheet.dismiss();});
        LinearLayout routeRow=a.row();root.addView(routeRow,new LinearLayout.LayoutParams(-1,a.dp(50)));
        HorizontalScrollView path=new HorizontalScrollView(a);path.setHorizontalScrollBarEnabled(false);route=a.row();path.addView(route);routeRow.addView(path,new LinearLayout.LayoutParams(0,-1,1));
        TextView add=a.button("＋");add.setTextSize(22);add.setContentDescription(a.getString(R.string.fault_add_remove));routeRow.addView(add,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));add.setOnClickListener(v->{tuning=false;renderRoute();renderBody();});
        scroll=new ScrollView(a);scroll.setFillViewport(false);body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        sheet.setContentView(root);sheet.setCanceledOnTouchOutside(true);Window window=sheet.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setGravity(Gravity.BOTTOM);
        sheet.setOnDismissListener(d->{if(auxiliary!=null)auxiliary.dismiss();a.restoreEffectEditor(this);a.finishEffectEdit(edit,base,false);});edit.dialog=sheet;
        renderRoute();renderBody();sheet.show();resize();preview();
    }
    void resize(){
        if(sheet==null||!sheet.isShowing())return;
        int usable=a.cameraRoot.getHeight()-a.cameraRoot.getPaddingTop()-a.cameraRoot.getPaddingBottom();
        int desired=a.advancedMode?a.dp(460):tuning&&a.effectAvailable(focused)?a.dp(198+Effects.CONTROLS[focused].length*66):a.dp(425);
        int height=Math.min(desired,Math.round(usable*.53f));sheet.getWindow().setLayout(a.cameraRoot.getWidth()-a.dp(16),height);a.reserveEffectEditor(this,height);
    }
    void renderRoute(){
        route.removeAllViews();
        for(int id:Effects.ordered(mask,false)){
            TextView chip=a.button(Effects.name(id));chip.setTextSize(10);boolean focus=tuning&&id==focused;chip.setTextColor(focus?MainActivity.BG:a.effectAvailable(id)?MainActivity.WHITE:MainActivity.MUTED);chip.setBackground(a.bg(focus?MainActivity.LIME:MainActivity.PANEL,0));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,a.dp(44));p.rightMargin=a.dp(6);route.addView(chip,p);
            chip.setOnClickListener(v->{focused=id;tuning=true;renderRoute();renderBody();});
        }

    }
    void renderBody(){
        body.removeAllViews();choices.clear();advancedControls=null;
        if(tuning&&focused!=Effects.CLEAN){
            final int id=focused;
            LinearLayout title=a.row();TextView name=a.text(Effects.name(id)+" · "+Effects.stage(id),10,MainActivity.MUTED);title.addView(name,new LinearLayout.LayoutParams(0,a.dp(40),1));
            if((mask&(1<<id))!=0){TextView remove=action(R.string.fault_remove);title.addView(remove,new LinearLayout.LayoutParams(-2,a.dp(40)));remove.setOnClickListener(v->{mask&=~(1<<id);focused=mask==0?0:Effects.ordered(mask,false)[0];if(mask==0)tuning=false;renderRoute();renderBody();preview();});}body.addView(title);
            if(!a.effectAvailable(id)){
                TextView hint=a.text(a.getString(R.string.fault_requires_rgb),13,MainActivity.MUTED);hint.setPadding(0,a.dp(12),0,a.dp(16));body.addView(hint);
                TextView convert=action(a.videoMode?R.string.fault_switch_mp4:R.string.fault_switch_jpeg);convert.setTag("switch-format");body.addView(convert,new LinearLayout.LayoutParams(-1,a.dp(48)));convert.setOnClickListener(v->switchFormat(id));
            }else{
                if(a.advancedMode){advancedControls=new AdvancedControls(this,id);advancedControls.show(body);}else for(Effects.Control control:Effects.CONTROLS[id])slider(id,control);
                LinearLayout actions=a.row();TextView reset=action(R.string.ui_reset),reseed=action(R.string.ui_reseed);actions.addView(reset,new LinearLayout.LayoutParams(0,a.dp(44),1));actions.addView(reseed,new LinearLayout.LayoutParams(0,a.dp(44),1));body.addView(actions);
                reset.setOnClickListener(v->{draft=draft.reset(id);renderBody();preview();});reseed.setOnClickListener(v->{draft=draft.reseed(id,new java.security.SecureRandom().nextLong());preview();});
            }
        }else{
            TextView hint=a.text(a.getString(R.string.fault_catalog_hint),MainActivity.TEXT_BODY,MainActivity.MUTED);hint.setPadding(0,a.dp(4),0,a.dp(8));body.addView(hint);
            LinearLayout row=null;
            for(int i=0;i<ids.length;i++){
                if(i%2==0){row=a.row();body.addView(row,new LinearLayout.LayoutParams(-1,a.dp(65)));}
                final int id=ids[i];TextView choice=a.button("");choice.setTextSize(11);choice.setPadding(a.dp(6),a.dp(4),a.dp(6),a.dp(4));choices.put(id,choice);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(60),1);p.setMargins(a.dp(2),0,a.dp(2),0);row.addView(choice,p);paintChoice(id);
                choice.setOnClickListener(v->{if(!a.effectAvailable(id)){focused=id;tuning=true;renderRoute();renderBody();return;}mask^=1<<id;if((mask&(1<<id))!=0)focused=id;else if(focused==id)focused=mask==0?0:Effects.ordered(mask,false)[0];paintChoice(id);renderRoute();preview();v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);});
            }
            TextView clear=action(R.string.ui_clear_selection);clear.setTextSize(10);LinearLayout.LayoutParams clearP=new LinearLayout.LayoutParams(0,a.dp(60),1);clearP.setMargins(a.dp(2),0,a.dp(2),0);row.addView(clear,clearP);clear.setOnClickListener(v->{mask=0;focused=0;renderRoute();renderBody();preview();});
        }
        scroll.scrollTo(0,0);resize();
    }
    void switchFormat(int id){
        if(a.recording||a.engine.photoBusy)return;
        EffectState candidate=state().chain(mask|(1<<id));sheet.dismiss();
        CaptureSettings next=new CaptureSettings(a.settings);if(a.videoMode)next.rawVideo=false;else{next.photoFormat=0;next.photoSize="recommended";}a.applySettings(next);
        a.handler.post(()->{if(!a.resumed)return;EffectDialog replacement=new EffectDialog(a,true);replacement.mask=candidate.mask;replacement.amount=candidate.amount;replacement.draft=candidate.parameters();replacement.focused=id;replacement.tuning=true;replacement.show();});
    }
    void paintChoice(int id){TextView view=choices.get(id);boolean selected=(mask&(1<<id))!=0;boolean available=a.effectAvailable(id);String stage=Effects.stage(id);stage=stage.substring(stage.indexOf(" / ")+3);view.setText((selected?"✓ ":"")+Effects.name(id)+"\n"+stage+(available?"":" · "+(a.videoMode?"MP4":"JPG")));view.setTextColor(selected?MainActivity.BG:available?MainActivity.WHITE:MainActivity.MUTED);view.setBackground(a.bg(selected?MainActivity.LIME:MainActivity.PANEL,0));view.setSelected(selected);view.setContentDescription(Effects.name(id)+" · "+stage+(available?"":" · "+a.getString(R.string.fault_requires_rgb)));}
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
    void slider(int id,Effects.Control control){
        LinearLayout row=a.row();TextView name=a.text(a.getString(controlLabel(control.key)),12,MainActivity.MUTED),value=a.text("",12,MainActivity.LIME);row.addView(name,new LinearLayout.LayoutParams(0,a.dp(24),1));row.addView(value);body.addView(row);
        SeekBar slider=new SeekBar(a);slider.setContentDescription(a.getString(controlLabel(control.key)));slider.setTag(control.key);slider.setMax(100);slider.setProgress(Math.round(draft.get(id,control.key)*100));value.setText(slider.getProgress()+"%");slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));body.addView(slider,new LinearLayout.LayoutParams(-1,a.dp(42)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){if(user){draft=draft.with(id,control.key,n/100f);value.setText(n+"%");preview();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    }
}
