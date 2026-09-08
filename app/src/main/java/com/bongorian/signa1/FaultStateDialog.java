package com.bongorian.signa1;

import android.app.Dialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Live, read-only event envelopes. Rows retain their positions while values change. */
final class FaultStateDialog {
    final MainActivity a;final LinearLayout body,rows;final TextView clock,summary;
    final Map<Integer,Row> items=new LinkedHashMap<>();String order="";
    FaultStateDialog(MainActivity activity){a=activity;body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout overview=a.row();clock=a.text("",28,MainActivity.WHITE);a.typography(clock,28,false);clock.setFontFeatureSettings("tnum");overview.addView(clock,new LinearLayout.LayoutParams(0,a.dp(48),1));summary=a.text("",12,MainActivity.LIME);summary.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);overview.addView(summary);body.addView(overview);
        TextView caption=a.text(a.getString(R.string.fault_state_hint),12,MainActivity.MUTED);caption.setPadding(0,0,0,a.dp(20));body.addView(caption);rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);body.addView(rows);
    }
    Dialog show(EffectState.Frame frame){update(frame);Dialog dialog=SignalSheet.content(a,a.getString(R.string.ui_live_fault_current_chain),body,0,null,.54f);a.reserveEffectEditor(this,dialog.getWindow().getAttributes().height);dialog.setOnDismissListener(v->{a.restoreEffectEditor(this);if(a.faultStatePanel==this)a.faultStatePanel=null;});return dialog;}
    void update(EffectState.Frame frame){clock.setText(String.format(Locale.US,"%.1f s",frame.time));summary.setText(a.getString(R.string.fault_state_count,frame.nodes.size()));String next=Arrays.toString(frame.ids());
        if(!next.equals(order)){order=next;rows.removeAllViews();items.clear();int index=1;for(int id:frame.ids()){Row row=new Row(id,index++);items.put(id,row);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=a.dp(10);rows.addView(row.root,p);}if(items.isEmpty()){TextView empty=a.text(a.getString(R.string.fault_chain_empty),13,MainActivity.MUTED);rows.addView(empty);}}
        for(Row row:items.values())row.update(0);for(FaultNode node:frame.nodes){Row row=items.get(node.id);if(row!=null)row.update(node.event.envelope);}
    }
    final class Row {
        final LinearLayout root;final TextView value;final Meter meter;
        Row(int id,int index){root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(14),a.dp(12),a.dp(14),a.dp(12));root.setBackground(a.bg(MainActivity.PANEL,0));LinearLayout line=a.row();TextView name=a.text(String.format(Locale.US,"%02d  %s",index,Effects.name(id)),13,MainActivity.WHITE);line.addView(name,new LinearLayout.LayoutParams(0,a.dp(24),1));value=a.text("",12,MainActivity.LIME);value.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);line.addView(value,new LinearLayout.LayoutParams(a.dp(48),a.dp(24)));root.addView(line);meter=new Meter();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(4));p.topMargin=a.dp(10);root.addView(meter,p);}
        void update(float amount){amount=Math.max(0,Math.min(1,amount));value.setText(Math.round(amount*100)+"%");meter.amount=amount;meter.invalidate();}
    }
    final class Meter extends View {final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float amount;Meter(){super(a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){float radius=getHeight()/2f;paint.setColor(0xff39403b);c.drawRoundRect(0,0,getWidth(),getHeight(),radius,radius,paint);paint.setColor(MainActivity.LIME);c.drawRoundRect(0,0,getWidth()*amount,getHeight(),radius,radius,paint);}}
}
