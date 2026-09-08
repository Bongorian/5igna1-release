package com.bongorian.signa1;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Read-only guide: never changes the capture settings or editor drafts. */
final class TutorialDialog {
    static final String SEEN="tutorial.seen";
    private static final int[] HEADINGS={R.string.tutorial_heading_0,R.string.tutorial_heading_1,R.string.tutorial_heading_2,R.string.tutorial_heading_3,R.string.tutorial_heading_4};
    private static final int[] BODIES={R.string.tutorial_body_0,R.string.tutorial_body_1,R.string.tutorial_body_2,R.string.tutorial_body_3,R.string.tutorial_body_4};
    final MainActivity activity;
    final Dialog dialog;
    int page;
    private TextView progress,heading,body,back,next;
    private ScrollView scroll;
    TutorialDialog(MainActivity activity,int page){this.activity=activity;this.page=Math.max(0,Math.min(page,HEADINGS.length-1));dialog=new Dialog(activity);}
    void show(){
        MainActivity a=activity;dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(20),a.dp(20),a.dp(20),a.dp(16));root.setBackground(a.bg(MainActivity.BG,MainActivity.PANEL));root.setTag("tutorial");
        TextView title=a.title(a.getString(R.string.tutorial_title));root.addView(title);
        progress=a.text("",13,MainActivity.LIME);progress.setPadding(0,a.dp(14),0,a.dp(14));progress.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(progress);
        scroll=new ScrollView(a);scroll.setFillViewport(false);LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);
        heading=a.text("",24,MainActivity.WHITE);heading.setAccessibilityHeading(true);heading.setTag("tutorial-heading");content.addView(heading);
        body=a.text("",16,MainActivity.WHITE);body.setPadding(0,a.dp(20),0,a.dp(24));body.setLineSpacing(a.dp(6),1);content.addView(body);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=a.row();back=a.button(a.getString(R.string.ui_back));back.setTag("tutorial-back");next=a.button("");next.setTag("tutorial-next");next.setTextColor(MainActivity.BG);next.setBackground(a.bg(MainActivity.LIME,0));
        LinearLayout.LayoutParams button=new LinearLayout.LayoutParams(0,-2,1);button.setMarginEnd(a.dp(8));back.setMinHeight(a.dp(52));next.setMinHeight(a.dp(52));actions.addView(back,button);actions.addView(next,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
        TextView skip=a.button(a.getString(R.string.tutorial_skip));skip.setTag("tutorial-skip");skip.setTextColor(MainActivity.MUTED);skip.setMinHeight(a.dp(48));root.addView(skip,new LinearLayout.LayoutParams(-1,-2));
        back.setOnClickListener(v->{if(page>0){page--;render();}});next.setOnClickListener(v->{if(page==HEADINGS.length-1)dialog.dismiss();else{page++;render();}});skip.setOnClickListener(v->dialog.dismiss());
        dialog.setContentView(root);dialog.setCanceledOnTouchOutside(false);dialog.setOnDismissListener(d->a.tutorialClosed());Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setGravity(Gravity.CENTER);dialog.show();int height=a.getWindow().getDecorView().getHeight();if(height<=0)height=a.getResources().getDisplayMetrics().heightPixels;window.setLayout(a.getResources().getDisplayMetrics().widthPixels-a.dp(16),(int)(height*.86f));render();
    }
    private void render(){progress.setText(activity.getString(R.string.tutorial_progress,page+1,HEADINGS.length));heading.setText(HEADINGS[page]);body.setText(BODIES[page]);back.setEnabled(page>0);back.setAlpha(page>0?1f:.3f);next.setText(page==HEADINGS.length-1?R.string.tutorial_done:R.string.tutorial_next);next.setContentDescription(next.getText());scroll.scrollTo(0,0);}
    void dispose(){dialog.setOnDismissListener(null);dialog.dismiss();}
}
