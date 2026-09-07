package com.bongorian.signa1;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.util.Linkify;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Privacy information remains available offline and without camera permission. */
final class AboutDialog {
    static void show(MainActivity activity){
        String policy;
        try(InputStream in=activity.getResources().openRawResource(R.raw.privacy_policy);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            policy=out.toString(StandardCharsets.UTF_8.name());
        }catch(Exception error){policy=activity.getString(R.string.ui_could_not_load_the_privacy_policy_contact_dennosamurai);}
        TextView text=activity.text(policy,14,MainActivity.WHITE);text.setTextIsSelectable(true);
        text.setPadding(activity.dp(20),activity.dp(12),activity.dp(20),activity.dp(16));text.setLineSpacing(activity.dp(4),1);
        text.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);text.setLinkTextColor(MainActivity.LIME);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        TextView licenses=activity.button(activity.getString(R.string.ui_open_source_licenses));licenses.setOnClickListener(v->showLicenses(activity));
        content.addView(licenses,new LinearLayout.LayoutParams(-1,activity.dp(48)));content.addView(text);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        new AlertDialog.Builder(activity).setTitle(BuildConfig.APP_NAME+" · "+BuildConfig.VERSION_NAME)
            .setView(scroll).setPositiveButton(activity.getString(R.string.ui_close),null)
            .setNeutralButton(activity.getString(R.string.ui_contact),(dialog,which)->{
                Intent intent=new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:dennosamurai@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT,"5igna1 support");
                try{activity.startActivity(intent);}catch(Exception unavailable){android.widget.Toast.makeText(activity,activity.getString(R.string.ui_contact_dennosamurai_gmail_com),android.widget.Toast.LENGTH_LONG).show();}
            }).show();
    }
    static void showLicenses(MainActivity activity){
        StringBuilder legal=new StringBuilder();
        for(String name:new String[]{"NOTICE","THIRD_PARTY_LICENSES.md","LICENSE","Kotlin-COPYRIGHT.txt","Kotlin-Boost-1.0.txt"}){
            try(InputStream in=activity.getAssets().open("licenses/"+name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                legal.append(name).append("\n\n").append(out.toString(StandardCharsets.UTF_8.name())).append("\n\n");
            }catch(Exception error){legal.append(name).append(": ").append(activity.getString(R.string.ui_unavailable)).append('\n');}
        }
        TextView text=activity.text(legal.toString(),12,MainActivity.WHITE);text.setTextIsSelectable(true);text.setPadding(activity.dp(20),activity.dp(12),activity.dp(20),activity.dp(16));
        ScrollView scroll=new ScrollView(activity);scroll.addView(text);
        new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.ui_open_source_licenses)).setView(scroll).setPositiveButton(activity.getString(R.string.ui_close),null).show();
    }
}
