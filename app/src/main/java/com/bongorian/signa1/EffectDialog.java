package com.bongorian.signa1;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Draft edits can be previewed but only Apply replaces the committed state. */
final class EffectDialog {
    final MainActivity a;final MainActivity.EffectPreview edit;final EffectState base;
    final float[] draft;final int[] ids;final boolean[] enabled;final boolean single;
    final TextView[] toggles,hints;final LinearLayout[] cards,editors;final SeekBar[][] sliders;
    Dialog sheet;boolean syncing;
    EffectDialog(MainActivity activity,boolean single){
        a=activity;this.single=single;edit=a.beginEffectPreview();base=edit.base;draft=base.parameters();
        ids=single?base.ids():Arrays.stream(a.availableEffects()).filter(id->id!=Effects.CLEAN).toArray();
        enabled=new boolean[ids.length];toggles=new TextView[ids.length];hints=new TextView[ids.length];cards=new LinearLayout[ids.length];editors=new LinearLayout[ids.length];sliders=new SeekBar[ids.length][3];
        for(int i=0;i<ids.length;i++)enabled[i]=base.enabled(ids[i]);
    }
    EffectState state(){int mask=0;for(int i=0;i<ids.length;i++)if(enabled[i])mask|=1<<ids[i];return base.edit(single?base.chained:true,mask,draft);}
    void preview(){if(!syncing)a.previewEffectEdit(edit,state());}
    void show(){
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);
        if(!single){TextView clear=a.button(a.getString(R.string.ui_clear_selection));clear.setTextColor(MainActivity.MUTED);content.addView(clear,new LinearLayout.LayoutParams(-1,a.dp(42)));clear.setOnClickListener(v->{Arrays.fill(enabled,false);for(int i=0;i<ids.length;i++)refresh(i);preview();});}
        String stage="";
        for(int i=0;i<ids.length;i++){
            final int index=i,id=ids[i];String group=Effects.stage(id);
            if(!stage.equals(group)){stage=group;TextView label=a.text(group,11,MainActivity.LIME);label.setLetterSpacing(.12f);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,a.dp(38));lp.topMargin=a.dp(8);content.addView(label,lp);}
            LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(8));cards[i]=card;
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=a.dp(8);content.addView(card,cp);
            LinearLayout head=a.row();card.addView(head,new LinearLayout.LayoutParams(-1,a.dp(48)));
            TextView title=a.text(Effects.name(id),14,MainActivity.WHITE);title.setLetterSpacing(.04f);head.addView(title,new LinearLayout.LayoutParams(0,-1,1));
            TextView tune=a.button(a.getString(R.string.ui_adjust));tune.setTextColor(MainActivity.MUTED);head.addView(tune,new LinearLayout.LayoutParams(a.dp(68),a.dp(42)));
            TextView toggle=a.button("");toggles[i]=toggle;LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(a.dp(48),a.dp(42));tp.leftMargin=a.dp(5);head.addView(toggle,tp);
            View.OnClickListener select=v->{enabled[index]=!enabled[index];refresh(index);preview();v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);};title.setOnClickListener(select);toggle.setOnClickListener(select);
            LinearLayout editor=new LinearLayout(a);editor.setOrientation(LinearLayout.VERTICAL);editors[i]=editor;card.addView(editor);
            TextView hint=a.text("",10,MainActivity.MUTED);hints[i]=hint;hint.setMinHeight(a.dp(24));editor.addView(hint,new LinearLayout.LayoutParams(-1,-2));
            slider(editor,i,0,a.getString(R.string.ui_stage_strength));slider(editor,i,1,a.getResources().getStringArray(R.array.parameter_labels)[id*2]);slider(editor,i,2,a.getResources().getStringArray(R.array.parameter_labels)[id*2+1]);
            LinearLayout actions=a.row();editor.addView(actions,new LinearLayout.LayoutParams(-1,a.dp(42)));
            TextView reset=a.button(a.getString(R.string.ui_reset));reset.setTextColor(MainActivity.MUTED);actions.addView(reset,new LinearLayout.LayoutParams(0,-1,1));
            reset.setOnClickListener(v->{float[] defaults=EffectParameters.defaults();syncing=true;for(int slot=0;slot<4;slot++)draft[id*4+slot]=defaults[id*4+slot];for(int slot=0;slot<3;slot++)sliders[index][slot].setProgress(Math.round(draft[id*4+slot]*sliders[index][slot].getMax()));syncing=false;refresh(index);preview();});
            if(id!=Effects.SPECTRUM&&id!=Effects.CHROMA_LOSS){TextView seed=a.button(a.getString(R.string.ui_reseed));seed.setTextColor(MainActivity.MUTED);actions.addView(seed,new LinearLayout.LayoutParams(0,-1,1));seed.setOnClickListener(v->{draft[id*4+3]=(float)Math.random();v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);preview();});}
            tune.setOnClickListener(v->{boolean open=editor.getVisibility()!=View.VISIBLE;editor.setVisibility(open?View.VISIBLE:View.GONE);if(open&&sheet!=null){revealPreview();card.post(()->card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true));}});
            editor.setVisibility(single&&enabled[i]?View.VISIBLE:View.GONE);refresh(i);
        }
        sheet=SignalSheet.show(a,single?a.getString(R.string.ui_effect_adjust):a.getString(R.string.ui_chain_effects),a.recording?a.getString(R.string.ui_preview_applying_also_changes_the_recording):a.getString(R.string.ui_preview_apply_to_save_back_to_discard),content,()->a.finishEffectEdit(edit,state(),true),single?.66f:.87f);
        edit.dialog=sheet;if(single)revealPreview();sheet.setOnDismissListener(d->a.finishEffectEdit(edit,base,false));
    }
    void revealPreview(){SignalSheet.resize(a,sheet,.66f);Window window=sheet.getWindow();if(window!=null){WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.10f;window.setAttributes(lp);}}
    void refresh(int i){
        if(cards[i]==null)return;cards[i].setBackground(a.bg(MainActivity.PANEL,16,enabled[i]?0x665F7940:0));toggles[i].setText(enabled[i]?"✓":"＋");toggles[i].setTextColor(enabled[i]?MainActivity.BG:MainActivity.MUTED);toggles[i].setBackground(a.bg(enabled[i]?MainActivity.LIME:MainActivity.BG,12,0));
        String hint=!enabled[i]?a.getString(R.string.ui_off_enable_to_apply):base.amount==0?a.getString(R.string.ui_global_strength_is_0):draft[ids[i]*4]==0?a.getString(R.string.ui_this_stage_has_0_strength):ids[i]==Effects.ROW_SHIFT&&draft[ids[i]*4+2]==0?a.getString(R.string.ui_shift_is_0_rows_stay_in_place):ids[i]==Effects.DATA_SHIFT&&Math.round(draft[ids[i]*4+1]*31)==0?a.getString(R.string.ui_0_bytes_data_positions_stay_unchanged):ids[i]==Effects.DEMOSAIC&&draft[ids[i]*4+2]==0?a.getString(R.string.ui_false_color_is_0_no_interpolation_error_applied):a.getString(R.string.ui_adjust_while_watching_the_preview);hints[i].setText(hint);
    }
    String parameterLabel(boolean phase,boolean bit,int n,int max){return phase?new String[]{a.getString(R.string.ui_horizontal),a.getString(R.string.ui_vertical),a.getString(R.string.ui_diagonal)}[n]:bit?"bit "+n+(n==0?a.getString(R.string.ui_low):n==max?a.getString(R.string.ui_high):""):n+"%";}
    void slider(LinearLayout target,int index,int slot,String label){
        int id=ids[index];boolean phase=(id==Effects.CFA_TEAR&&slot==2)||(id==Effects.CFA_OFFSET&&slot==1),bit=id==Effects.BIT_ROT&&slot==1;boolean bytes=id==Effects.DATA_SHIFT&&slot==1;int topBit=7;if(bit&&!edit.video&&edit.format==2){Integer white=a.cameraOptions==null?null:a.cameraOptions.characteristics.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);topBit=31-Integer.numberOfLeadingZeros(white==null?65535:Math.max(1,white));}
        LinearLayout row=a.row();TextView name=a.text(label,11,MainActivity.MUTED),value=a.text("",11,MainActivity.LIME);name.setMinHeight(a.dp(26));row.addView(name,new LinearLayout.LayoutParams(0,-2,1));row.addView(value);target.addView(row);
        SeekBar slider=new SeekBar(a);sliders[index][slot]=slider;slider.setMax(phase?2:bytes?31:bit?Math.max(1,topBit):100);slider.setProgress(Math.round(draft[id*4+slot]*slider.getMax()));value.setText(bytes?slider.getProgress()+" byte":parameterLabel(phase,bit,slider.getProgress(),slider.getMax()));slider.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME));slider.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME));target.addView(slider,new LinearLayout.LayoutParams(-1,a.dp(38)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){draft[id*4+slot]=n/(float)s.getMax();value.setText(bytes?n+" byte":parameterLabel(phase,bit,n,s.getMax()));refresh(index);preview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    }
}
