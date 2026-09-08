package com.bongorian.signa1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;

/** Shared dark, rounded editor surface for capture and effect settings. */
final class SignalSheet {
    static Dialog show(MainActivity a,String title,String subtitle,View content,Runnable apply){return show(a,title,subtitle,content,apply,.87f);}
    static Dialog show(MainActivity a,String title,String subtitle,View content,Runnable apply,float fraction){
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(18),a.dp(10),a.dp(18),a.dp(12));root.setBackground(a.bg(MainActivity.BG,MainActivity.PANEL));
        View handle=new View(a);handle.setBackground(a.detailBg(MainActivity.MUTED,0));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(a.dp(30),a.dp(3));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=a.dp(20);root.addView(handle,hp);
        TextView heading=a.title(title);heading.setMinHeight(a.dp(34));root.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView note=a.text(subtitle,MainActivity.TEXT_BODY,MainActivity.MUTED);note.setPadding(0,0,0,a.dp(12));root.addView(note,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=a.row();actions.setPadding(0,a.dp(12),0,0);root.addView(actions,new LinearLayout.LayoutParams(-1,a.dp(60)));
        TextView cancel=a.button(a.getString(R.string.ui_back));cancel.setTextColor(MainActivity.MUTED);cancel.setBackgroundColor(Color.TRANSPARENT);actions.addView(cancel,new LinearLayout.LayoutParams(0,-1,1));cancel.setOnClickListener(v->dialog.dismiss());
        TextView done=a.button(a.getString(R.string.ui_apply));done.setTextColor(MainActivity.BG);done.setBackground(a.bg(MainActivity.LIME,0));actions.addView(done,new LinearLayout.LayoutParams(0,-1,1));done.setOnClickListener(v->{apply.run();dialog.dismiss();});
        dialog.setContentView(root);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.65f;window.setAttributes(lp);window.setGravity(Gravity.BOTTOM);}
        dialog.show();resize(a,dialog,fraction);return dialog;
    }
    static Dialog content(MainActivity a,String title,View content,int secondary,Runnable action,float fraction){
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(18),a.dp(14),a.dp(18),a.dp(14));root.setBackground(a.bg(MainActivity.BG,MainActivity.PANEL));
        LinearLayout header=a.row();TextView heading=a.title(title);header.addView(heading,new LinearLayout.LayoutParams(0,a.dp(48),1));TextView close=a.button("×");close.setTextSize(24);close.setContentDescription(a.getString(R.string.ui_close));header.addView(close,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));close.setOnClickListener(v->dialog.dismiss());root.addView(header);
        ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(content);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);sp.topMargin=a.dp(12);root.addView(scroll,sp);
        if(secondary!=0){TextView button=a.button(a.getString(secondary));button.setTextColor(MainActivity.LIME);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,a.dp(48));bp.topMargin=a.dp(12);root.addView(button,bp);button.setOnClickListener(v->{dialog.dismiss();if(action!=null)action.run();});}
        dialog.setContentView(root);Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setGravity(Gravity.BOTTOM);WindowManager.LayoutParams wp=window.getAttributes();wp.dimAmount=.3f;window.setAttributes(wp);dialog.setCanceledOnTouchOutside(true);dialog.show();resize(a,dialog,fraction);return dialog;
    }
    static Dialog message(MainActivity a,String title,String message,int actionLabel,Runnable action){TextView text=a.text(message,14,MainActivity.WHITE);text.setTag("message");text.setLineSpacing(a.dp(5),1);return content(a,title,text,actionLabel,action,.55f);}
    static Dialog pick(MainActivity a,String title,String[] labels,int selected,java.util.function.IntConsumer chosen){
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);Dialog[] dialog=new Dialog[1];
        for(int n=0;n<labels.length;n++){final int index=n;TextView row=a.text(labels[n]+(n==selected?"   ✓":""),14,n==selected?MainActivity.LIME:MainActivity.WHITE);row.setTag("choice-"+n);row.setPadding(a.dp(16),a.dp(14),a.dp(16),a.dp(14));row.setMinHeight(a.dp(56));row.setBackground(a.bg(MainActivity.PANEL,n==selected?0x665F7940:0));row.setSelected(n==selected);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=a.dp(8);list.addView(row,p);row.setOnClickListener(v->{dialog[0].dismiss();chosen.accept(index);});}
        int height=a.getWindow().getDecorView().getHeight();float fraction=Math.min(.8f,a.dp(110+labels.length*68)/(float)Math.max(height,1));dialog[0]=content(a,title,list,0,null,fraction);return dialog[0];
    }
    static Dialog anchoredPick(MainActivity a,View anchor,String title,String[] labels,int selected,java.util.function.IntConsumer chosen){
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(8));list.setBackground(a.bg(MainActivity.BG,MainActivity.PANEL));
        TextView heading=a.text(title,11,MainActivity.MUTED);heading.setPadding(a.dp(12),a.dp(8),a.dp(12),a.dp(12));list.addView(heading);
        for(int n=0;n<labels.length;n++){final int index=n;TextView option=a.button(labels[n]+(n==selected?"   ✓":""));option.setTag("choice-"+n);option.setSelected(n==selected);option.setGravity(Gravity.CENTER_VERTICAL);option.setTextColor(n==selected?MainActivity.LIME:MainActivity.WHITE);option.setBackground(n==selected?a.bg(MainActivity.PANEL,0):new ColorDrawable(Color.TRANSPARENT));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(48));if(n>0)p.topMargin=a.dp(4);list.addView(option,p);option.setOnClickListener(v->{dialog.dismiss();chosen.accept(index);});}
        dialog.setContentView(list);Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setGravity(Gravity.TOP|Gravity.LEFT);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.18f;window.setAttributes(lp);dialog.setCanceledOnTouchOutside(true);dialog.show();
        int[] position=new int[2];anchor.getLocationOnScreen(position);android.graphics.Rect visible=new android.graphics.Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);lp=window.getAttributes();lp.width=a.dp(212);lp.height=-2;lp.x=Math.max(a.dp(8),Math.min(position[0],a.getResources().getDisplayMetrics().widthPixels-lp.width-a.dp(8)));lp.y=Math.max(0,position[1]+anchor.getHeight()+a.dp(6)-visible.top);window.setAttributes(lp);return dialog;
    }
    static Dialog number(MainActivity a,String title,String range,String initial,boolean integer,java.util.function.Predicate<String> changed){
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);TextView note=a.text(range,12,MainActivity.MUTED);body.addView(note,new LinearLayout.LayoutParams(-1,a.dp(30)));
        EditText input=new EditText(a);input.setTag("number-input");input.setSingleLine(true);input.setTextColor(MainActivity.WHITE);a.typography(input,MainActivity.TEXT_TITLE,false);input.setPadding(a.dp(14),0,a.dp(14),0);input.setBackground(a.bg(MainActivity.PANEL,0));input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|(integer?0:android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL));input.setText(initial);body.addView(input,new LinearLayout.LayoutParams(-1,a.dp(52)));
        TextView error=a.text(a.getString(R.string.ui_invalid_number),12,MainActivity.RED);error.setVisibility(View.GONE);body.addView(error,new LinearLayout.LayoutParams(-1,a.dp(30)));
        TextView apply=a.button(a.getString(R.string.ui_apply));apply.setTag("number-apply");apply.setTextColor(MainActivity.BG);apply.setBackground(a.bg(MainActivity.LIME,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(48));p.topMargin=a.dp(12);body.addView(apply,p);
        Dialog dialog=content(a,title,body,0,null,.4f);if(a.liveEditor!=null)a.liveEditor.child=dialog;else if(a.effectEditorOwner instanceof EffectDialog)((EffectDialog)a.effectEditorOwner).auxiliary=dialog;apply.setOnClickListener(v->{try{if(changed.test(input.getText().toString().trim())){dialog.dismiss();return;}}catch(IllegalArgumentException ignored){}error.setVisibility(View.VISIBLE);});return dialog;
    }
    static void resize(MainActivity a,Dialog dialog,float fraction){Window window=dialog.getWindow();if(window!=null){int h=a.getWindow().getDecorView().getHeight();window.setLayout(a.getResources().getDisplayMetrics().widthPixels-a.dp(16),(int)(h*fraction));}}
}
