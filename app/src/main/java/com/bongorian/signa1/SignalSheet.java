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
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(18),a.dp(10),a.dp(18),a.dp(12));root.setBackground(a.bg(MainActivity.BG,24,MainActivity.PANEL));
        View handle=new View(a);handle.setBackground(a.bg(MainActivity.MUTED,3,0));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(a.dp(30),a.dp(3));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=a.dp(20);root.addView(handle,hp);
        TextView heading=a.text(title,21,MainActivity.WHITE);heading.setLetterSpacing(.06f);heading.setMinHeight(a.dp(34));root.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView note=a.text(subtitle,11,MainActivity.MUTED);note.setPadding(0,0,0,a.dp(12));root.addView(note,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=a.row();actions.setPadding(0,a.dp(12),0,0);root.addView(actions,new LinearLayout.LayoutParams(-1,a.dp(60)));
        TextView cancel=a.button(a.getString(R.string.ui_back));cancel.setTextColor(MainActivity.MUTED);cancel.setBackgroundColor(Color.TRANSPARENT);actions.addView(cancel,new LinearLayout.LayoutParams(0,-1,1));cancel.setOnClickListener(v->dialog.dismiss());
        TextView done=a.button(a.getString(R.string.ui_apply));done.setTextColor(MainActivity.BG);done.setBackground(a.bg(MainActivity.LIME,16,0));actions.addView(done,new LinearLayout.LayoutParams(0,-1,1));done.setOnClickListener(v->{apply.run();dialog.dismiss();});
        dialog.setContentView(root);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.65f;window.setAttributes(lp);window.setGravity(Gravity.BOTTOM);}
        dialog.show();resize(a,dialog,fraction);return dialog;
    }
    static void resize(MainActivity a,Dialog dialog,float fraction){Window window=dialog.getWindow();if(window!=null){int h=a.getWindow().getDecorView().getHeight();window.setLayout(a.getResources().getDisplayMetrics().widthPixels-a.dp(16),(int)(h*fraction));}}
}
