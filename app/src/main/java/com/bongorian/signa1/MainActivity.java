package com.bongorian.signa1;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends androidx.appcompat.app.AppCompatActivity implements GlitchEngine.Listener, TextureView.SurfaceTextureListener {
    static final int BG=Color.rgb(10,12,13), PANEL=Color.rgb(24,27,28), LIME=Color.rgb(208,242,139), WHITE=Color.rgb(239,242,231), MUTED=Color.rgb(142,151,140), RED=Color.rgb(255,74,107);
    static final String[] MODES=Effects.NAMES;
    GlitchEngine engine;
    TextureView preview;
    FrameLayout viewfinder,previewArea;
    CameraOptions cameraOptions; CaptureSettings settings; GeoTags geo; ImageView geoButton,torchButton,micButton; float displayAspect=.75f,measuredFps;
    TextView status, count, strengthValue, photoTab, videoTab, zoomButton;
    ImageView flipButton;MediaThumbnail galleryButton;
    LinearLayout selectedRoute; TextView formatButton;
    SeekBar strength;
    CaptureButton capture;
    Overlay overlay;
    boolean resumed, ready, recording, videoMode, sound=true;
    boolean latestVideo,advancedMode;
    int captureCount;
    EffectState effectState;
    TextView liveChainStatus;Dialog liveChainDialog;FaultStateDialog faultStatePanel;EffectState.Frame shownLiveFrame;
    FaultDialog.Editor liveEditor;LinearLayout liveTransport;TextView liveHold;
    FaultConfig faultConfig=FaultConfig.defaults(),pendingFaultConfig;ToggleButton faultSwitch;
    static final class EffectPreview {final EffectState base;final boolean video;final int format;android.app.Dialog dialog;EffectPreview(EffectState s,boolean v,int f){base=s;video=v;format=f;}}
    private EffectPreview effectPreview;
    long start;
    float zoom=1f;
    Uri latest;MediaPreview mediaPreview;
    final Handler handler=new Handler(Looper.getMainLooper());
    // Some devices consume a TextureView buffer during dialog/resize transitions without
    // delivering its update callback. Read only the UI-consumed timestamp to release the
    // bounded history; never acknowledge a merely submitted GL buffer.
    final Runnable previewAcknowledgement=new Runnable(){public void run(){if(!resumed||mediaPreview!=null)return;if(preview.isAvailable())engine.previewPresented(preview.getSurfaceTexture().getTimestamp());handler.postDelayed(this,100);}};
    final Runnable timer=new Runnable(){ public void run(){ if(recording){ long sec=(SystemClock.elapsedRealtime()-start)/1000; status.setText(settings.rawVideo?String.format(Locale.US,"● RAW %02d:%02d  ",sec/60,sec%60)+engine.rawProgress():String.format(Locale.US,"● REC %02d:%02d  %.0f fps",sec/60,sec%60,measuredFps)); handler.postDelayed(this,250); } } };
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setDecorFitsSystemWindows(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(0);
        android.content.SharedPreferences prefs=getSharedPreferences("signal",0);
        sound=prefs.getBoolean("sound",true);advancedMode=prefs.getBoolean("advancedMode",false);
        String last=prefs.getString("last",null); if(last!=null)latest=Uri.parse(last); latestVideo=prefs.getBoolean("lastVideo",false);
        if(b!=null){videoMode=b.getBoolean("session.video",false);zoom=b.getFloat("session.zoom",1);captureCount=b.getInt("session.count",0);}
        settings=CaptureSettings.load(prefs); geo=new GeoTags(this,this::renderGeo);geo.enabled=settings.location;
        effectState=EffectStateStore.load(prefs);
        engine=new GlitchEngine(this,this);engine.position=geo::snapshot;engine.configure(settings,videoMode,effectState);if(b!=null){engine.front=b.getBoolean("session.front",false);engine.zoom=zoom;}
        faultConfig=FaultPreferences.load(prefs);if(b!=null)faultConfig=faultConfig.enabled(b.getBoolean("session.live",false));engine.setFaultConfig(faultConfig);
        buildUi();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);savePrefs();out.putInt("session.count",captureCount);out.putBoolean("session.video",videoMode);out.putBoolean("session.front",engine.front);out.putFloat("session.zoom",zoom);out.putBoolean("session.live",faultConfig.enabled);}
    // One radius for panels, fields and controls; small labels use a scaled detail radius.
    static final int UI_RADIUS=12, DETAIL_RADIUS=4;
    GradientDrawable bg(int color,int border){return roundedBackground(color,UI_RADIUS,border);}
    GradientDrawable detailBg(int color,int border){return roundedBackground(color,DETAIL_RADIUS,border);}
    private GradientDrawable roundedBackground(int color,int radius,int border){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if(border!=0)d.setStroke(dp(1),border); return d; }
    static final int TEXT_TITLE=18, TEXT_LABEL=13, TEXT_BODY=12;
    void typography(TextView view,int size,boolean medium){view.setTextSize(size);view.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));view.setLetterSpacing(0);view.setFontFeatureSettings("kern,tnum");view.setIncludeFontPadding(false);view.setLineSpacing(dp(2),1);if(android.os.Build.VERSION.SDK_INT>=28)view.setFallbackLineSpacing(true);}
    TextView title(String label){TextView view=text(label,TEXT_TITLE,WHITE);typography(view,TEXT_TITLE,true);return view;}
    TextView text(String label,int size,int color){ TextView t=new TextView(this); t.setText(label); t.setTextColor(color); typography(t,size,false); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    TextView button(String label){ TextView t=text(label,12,WHITE); t.setGravity(Gravity.CENTER); typography(t,TEXT_BODY,true); t.setBackground(bg(PANEL,0)); t.setPadding(dp(12),0,dp(12),0); t.setContentDescription(label); return t; }
    ImageView iconButton(int resource,String description){ImageView icon=new ImageView(this);icon.setImageResource(resource);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setPadding(dp(12),dp(12),dp(12),dp(12));icon.setContentDescription(description);icon.setTooltipText(description);icon.setFocusable(true);icon.setClickable(true);return icon;}
    LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    LinearLayout cameraRoot;
    Space effectEditorSpace;Object effectEditorOwner;
    final java.util.Map<View,Integer> editorHiddenViews=new java.util.LinkedHashMap<>();
    void reserveEffectEditor(Object owner,int pixels){
        effectEditorOwner=owner;
        if(effectEditorSpace==null){
            boolean belowPreview=false;
            for(int i=0;i<cameraRoot.getChildCount();i++){
                View child=cameraRoot.getChildAt(i);
                if(belowPreview){editorHiddenViews.put(child,child.getVisibility());child.setVisibility(View.GONE);}
                if(child==previewArea)belowPreview=true;
            }
            effectEditorSpace=new Space(this);cameraRoot.addView(effectEditorSpace);
        }
        // Dialog ends at the usable window bottom; reserve that same height in the camera layout.
        effectEditorSpace.setLayoutParams(new LinearLayout.LayoutParams(-1,pixels));
    }
    void restoreEffectEditor(Object owner){
        if(effectEditorOwner!=owner)return;effectEditorOwner=null;
        if(effectEditorSpace!=null){cameraRoot.removeView(effectEditorSpace);effectEditorSpace=null;}
        editorHiddenViews.forEach(View::setVisibility);editorHiddenViews.clear();
    }
    void buildUi(){
        LinearLayout root=new LinearLayout(this);cameraRoot=root; root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(18),dp(8),dp(18),dp(4)); setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            root.setPadding(dp(18)+safe.left,dp(8)+safe.top,dp(18)+safe.right,dp(4)+safe.bottom);
            return insets;
        });root.requestApplyInsets();
        LinearLayout header=row(); root.addView(header,new LinearLayout.LayoutParams(-1,dp(49)));
        formatButton=button("");formatButton.setTextSize(12);formatButton.setOnClickListener(v->showFormat());header.addView(formatButton,new LinearLayout.LayoutParams(dp(68),dp(44)));
        header.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        torchButton=iconButton(R.drawable.ic_flash,getString(R.string.ui_light_off));header.addView(torchButton,new LinearLayout.LayoutParams(dp(44),dp(44)));renderTorch();torchButton.setOnClickListener(v->{engine.torch();handler.postDelayed(this::renderTorch,200);});
        geoButton=iconButton(R.drawable.ic_location,getString(R.string.ui_capture_location_settings));header.addView(geoButton,new LinearLayout.LayoutParams(dp(44),dp(44)));geoButton.setOnClickListener(v->showLocation());renderGeo();
        micButton=iconButton(R.drawable.ic_mic,getString(R.string.ui_audio_on));header.addView(micButton,new LinearLayout.LayoutParams(dp(44),dp(44)));renderAudio();micButton.setOnClickListener(v->{if(!recording){sound=!sound;renderAudio();savePrefs();}});
        ImageView settingsButton=iconButton(R.drawable.ic_settings,getString(R.string.ui_capture_and_language_settings));header.addView(settingsButton,new LinearLayout.LayoutParams(dp(44),dp(44)));settingsButton.setOnClickListener(v->showSettings());
        for(int n=1;n<header.getChildCount()-1;n++){View child=header.getChildAt(n);if(child instanceof TextView){child.setBackgroundColor(Color.TRANSPARENT);child.setPadding(dp(2),0,dp(2),0);((TextView)child).setSingleLine(true);}}
        LinearLayout info=row(); root.addView(info,new LinearLayout.LayoutParams(-1,dp(27)));
        status=text("CONNECTING…",10,LIME); status.setTypeface(Typeface.MONOSPACE);status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.END); info.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        count=text("FULL RES",10,MUTED);count.setOnClickListener(v->showSettings()); count.setTypeface(Typeface.MONOSPACE);count.setSingleLine(true);count.setEllipsize(android.text.TextUtils.TruncateAt.END);count.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams countParams=new LinearLayout.LayoutParams(dp(100),-1);countParams.leftMargin=dp(10);count.setVisibility(View.GONE);info.addView(count,countParams);
        // Fit a true 9:16 preview into the remaining space. Saved media has the same framing.
        previewArea=new FrameLayout(this); root.addView(previewArea,new LinearLayout.LayoutParams(-1,0,1));
        viewfinder=new FrameLayout(this); viewfinder.setBackground(bg(Color.BLACK,0)); viewfinder.setClipToOutline(true);
        previewArea.addView(viewfinder,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        previewArea.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int availableW=r-l,availableH=b-t;
            if(availableW<=0 || availableH<=0)return;
            int w=Math.min(availableW,(int)(availableH*displayAspect)),h=(int)(w/displayAspect);
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)viewfinder.getLayoutParams();
            if(p.width!=w || p.height!=h){p.width=w;p.height=h;p.gravity=Gravity.CENTER;viewfinder.setLayoutParams(p);}
        });
        preview=new TextureView(this); preview.setContentDescription(getString(R.string.ui_camera_preview_tap_to_focus)); preview.setOnClickListener(v -> {if(ready)engine.focus();else engine.retryPreview();}); preview.setSurfaceTextureListener(this); viewfinder.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        overlay=new Overlay(); viewfinder.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        preview.setOnTouchListener(new View.OnTouchListener(){float downX;public boolean onTouch(View v,android.view.MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;} if(e.getAction()==MotionEvent.ACTION_UP){float diff=e.getX()-downX; if(Math.abs(diff)>dp(60)){chooseEffect(nextAvailable(diff<0?1:-1));}else{v.performClick();overlay.focusX=e.getX();overlay.focusY=e.getY();overlay.invalidate();handler.postDelayed(()->{overlay.focusX=-1;overlay.invalidate();},900);}return true;}return true;}});
        TextView liveTag=text("  FX / "+MODES[effectState.selected()]+"  ",10,LIME); liveTag.setTypeface(Typeface.MONOSPACE); liveTag.setBackground(detailBg(0x99101410,0));
        FrameLayout.LayoutParams tagP=new FrameLayout.LayoutParams(-2,dp(26),Gravity.TOP|Gravity.START);tagP.setMargins(dp(12),dp(12),0,0);viewfinder.addView(liveTag,tagP);liveTag.setTag("fx");
        LinearLayout lens=row(); lens.setGravity(Gravity.CENTER); FrameLayout.LayoutParams lensP=new FrameLayout.LayoutParams(dp(56),dp(42),Gravity.TOP|Gravity.END);lensP.topMargin=dp(12);viewfinder.addView(lens,lensP);
        zoomButton=button(zoom==1?"1×":"2×"); zoomButton.setBackground(bg(0xCC121712,0));lens.addView(zoomButton,new LinearLayout.LayoutParams(dp(50),dp(38)));zoomButton.setOnClickListener(v->{zoom=zoom==1?2:1;engine.zoom(zoom);zoomButton.setText(zoom==1?"1×":"2×");});
        LinearLayout effects=new LinearLayout(this); effects.setOrientation(LinearLayout.VERTICAL); effects.setPadding(dp(2),dp(10),dp(2),dp(2));
        GradientDrawable shade=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00101410,0xEE0A0C0B,0xFA0A0C0B}); effects.setBackgroundColor(BG);
        FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(effects,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout chainRow=row();effects.addView(chainRow,new LinearLayout.LayoutParams(-1,dp(48)));
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);chainRow.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));selectedRoute=row();scroll.addView(selectedRoute);
        TextView add=button("＋");add.setTextSize(22);add.setContentDescription(getString(R.string.fault_add_remove));chainRow.addView(add,new LinearLayout.LayoutParams(dp(44),dp(44)));add.setOnClickListener(v->showChain());
        ImageView random=iconButton(R.drawable.ic_shuffle,getString(R.string.ui_tap_to_randomize_an_effect_hold_to_randomize));chainRow.addView(random,new LinearLayout.LayoutParams(dp(48),dp(48)));random.setOnClickListener(v->randomChain());random.setOnLongClickListener(v->{reseed();return true;});
        liveChainStatus=text("",10,LIME);liveChainStatus.setSingleLine(true);liveChainStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);liveChainStatus.setVisibility(View.GONE);effects.addView(liveChainStatus,new LinearLayout.LayoutParams(-1,dp(25)));liveChainStatus.setOnClickListener(v->{if(shownLiveFrame!=null){faultStatePanel=new FaultStateDialog(this);liveChainDialog=faultStatePanel.show(shownLiveFrame);}});
        LinearLayout power=row();effects.addView(power,new LinearLayout.LayoutParams(-1,dp(39)));
        TextView strengthLabel=text(getString(R.string.ui_strength),11,MUTED);power.addView(strengthLabel,new LinearLayout.LayoutParams(dp(48),-1));
        strength=new SeekBar(this);strength.setContentDescription(getString(R.string.ui_strength));strength.setMax(100);strength.setProgress(Math.round(effectState.amount*100));strength.setProgressTintList(ColorStateList.valueOf(LIME));strength.setThumbTintList(ColorStateList.valueOf(LIME));power.addView(strength,new LinearLayout.LayoutParams(0,dp(36),1));
        strengthValue=text(strength.getProgress()+"%",11,LIME);strengthValue.setTypeface(Typeface.MONOSPACE);strengthValue.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);power.addView(strengthValue,new LinearLayout.LayoutParams(dp(43),-1));
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean from){strengthValue.setText(p+"%");if(from)commitEffects(effectState.amount(p/100f));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){savePrefs();}});
        LinearLayout tools=row();root.addView(tools,new LinearLayout.LayoutParams(-1,dp(44)));
        photoTab=button(getString(R.string.ui_photo));videoTab=button(getString(R.string.ui_video));tools.addView(photoTab,new LinearLayout.LayoutParams(dp(60),dp(40)));tools.addView(videoTab,new LinearLayout.LayoutParams(dp(60),dp(40)));photoTab.setOnClickListener(v->setVideo(false));videoTab.setOnClickListener(v->setVideo(true));
        tools.addView(new Space(this),new LinearLayout.LayoutParams(dp(8),1));
        LinearLayout live=row();live.setBackground(bg(PANEL,0));tools.addView(live,new LinearLayout.LayoutParams(0,-1,1));
        faultSwitch=new ToggleButton(this);faultSwitch.setTextOn("LIVE · ON");faultSwitch.setTextOff("LIVE · OFF");typography(faultSwitch,10,true);faultSwitch.setAllCaps(false);faultSwitch.setBackgroundColor(Color.TRANSPARENT);faultSwitch.setPadding(dp(8),0,0,0);faultSwitch.setChecked(faultConfig.enabled);faultSwitch.setTextColor(faultConfig.enabled?LIME:MUTED);faultSwitch.setContentDescription(getString(R.string.ui_toggle_live_fault));live.addView(faultSwitch,new LinearLayout.LayoutParams(0,-1,1));
        ImageView reactions=iconButton(R.drawable.ic_tune,getString(R.string.ui_live_fault_settings));reactions.setPadding(dp(11),dp(11),dp(11),dp(11));live.addView(reactions,new LinearLayout.LayoutParams(dp(42),-1));reactions.setOnClickListener(v->FaultDialog.show(this));
        faultSwitch.setOnCheckedChangeListener((view,checked)->{if(checked!=faultConfig.enabled)setFaultConfig(faultConfig.enabled(checked));});
        liveTransport=row();LinearLayout.LayoutParams timeRow=new LinearLayout.LayoutParams(-1,dp(44));timeRow.topMargin=dp(8);timeRow.bottomMargin=dp(4);root.addView(liveTransport,timeRow);
        liveHold=button(getString(R.string.live_pause));TextView hit=button(getString(R.string.live_trigger)),rewind=button(getString(R.string.live_reset));liveHold.setContentDescription(getString(R.string.live_hold_hint));hit.setContentDescription(getString(R.string.live_hit_hint));rewind.setContentDescription(getString(R.string.live_reset_hint));FaultDialog.timeButtons(this,liveTransport,liveHold,hit,rewind);liveHold.setOnClickListener(v->applyFaultConfig(faultConfig.performance(faultConfig.performance.held(!faultConfig.performance.hold))));hit.setOnClickListener(v->{engine.hitFaults();v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);});rewind.setOnClickListener(v->engine.rewindFaults());
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);root.addView(controls,new LinearLayout.LayoutParams(-1,dp(88)));
        galleryButton=new MediaThumbnail(this);controls.addView(galleryButton,new LinearLayout.LayoutParams(dp(54),dp(54)));galleryButton.setOnClickListener(v->openGallery());galleryButton.load(latest,latestVideo);
        Space spacer=new Space(this);controls.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        capture=new CaptureButton();controls.addView(capture,new LinearLayout.LayoutParams(dp(80),dp(80)));capture.setContentDescription(getString(R.string.ui_take_a_photo));capture.setOnClickListener(v->shoot());
        controls.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));flipButton=iconButton(R.drawable.ic_camera_flip,getString(R.string.ui_switch_front_and_rear_cameras));flipButton.setBackground(bg(PANEL,0));controls.addView(flipButton,new LinearLayout.LayoutParams(dp(54),dp(54)));flipButton.setOnClickListener(v->{if(!recording){engine.switchCamera();zoom=1;zoomButton.setText("1×");handler.postDelayed(this::renderTorch,200);}});
        renderEffects();renderCaptureMode();savePrefs();
    }
    boolean rawOriginal(){return videoMode?settings.rawVideo:settings.photoFormat==1;}
    int[] availableEffects(){return Effects.choices(videoMode,!videoMode&&settings.photoFormat==2);}
    int[] uiEffects(){return rawOriginal()?new int[0]:effectState.snapshot(videoMode,settings.photoFormat).ids();}
    int nextAvailable(int delta){int[] ids=availableEffects();for(int n=0;n<ids.length;n++)if(ids[n]==effectState.selected())return ids[Math.floorMod(n+delta,ids.length)];return Effects.CLEAN;}
    void chooseEffect(int id){commitEffects(effectState.single(id));}
    void commitEffects(EffectState next){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Effect edits require UI thread");
        cancelEffectPreview();effectState=next;engine.setEffects(effectState);renderEffects();savePrefs();
    }
    EffectPreview beginEffectPreview(){if(liveEditor!=null)liveEditor.dialog.dismiss();cancelEffectPreview();effectPreview=new EffectPreview(effectState,videoMode,settings.photoFormat);return effectPreview;}
    boolean validPreview(EffectPreview edit){return effectPreview==edit&&effectState==edit.base&&videoMode==edit.video&&settings.photoFormat==edit.format;}
    void previewEffectEdit(EffectPreview edit,EffectState draft){if(validPreview(edit)){engine.previewEffects(draft.forContext(videoMode,settings.photoFormat));TextView tag=viewfinder.findViewWithTag("fx");tag.setText(getString(R.string.ui_preview_editing));}}
    void finishEffectEdit(EffectPreview edit,EffectState draft,boolean apply){
        if(effectPreview!=edit)return;boolean valid=validPreview(edit);effectPreview=null;
        if(apply&&valid)commitEffects(draft);else{engine.clearEffectPreview();renderEffects();if(apply)Toast.makeText(this,getString(R.string.ui_edits_discarded_because_the_capture_mode_changed),Toast.LENGTH_SHORT).show();}
    }
    void cancelEffectPreview(){EffectPreview edit=effectPreview;if(edit==null)return;effectPreview=null;engine.clearEffectPreview();if(edit.dialog!=null)edit.dialog.dismiss();}
    void renderEffects(){
        if(liveTransport!=null){liveTransport.setVisibility(faultConfig.enabled&&!rawOriginal()&&effectPreview==null&&liveEditor==null?View.VISIBLE:View.GONE);liveHold.setText(getString(faultConfig.performance.hold?R.string.live_resume:R.string.live_pause));liveHold.setTextColor(faultConfig.performance.hold?LIME:MUTED);}
        boolean original=rawOriginal();int[] active=uiEffects();
        formatButton.setText(videoMode?(settings.rawVideo?"RAW ZIP ▾":"MP4 ▾"):settings.photoFormat==0?"JPG ▾":"RAW ▾");
        selectedRoute.removeAllViews();
        for(int id:effectState.ids()){
            boolean enabled=effectAvailable(id);TextView chip=button(Effects.name(id)+(enabled?"":" · "+(videoMode?"MP4":"JPG")));chip.setTextSize(10);chip.setTextColor(enabled?LIME:MUTED);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(44));cp.rightMargin=dp(6);selectedRoute.addView(chip,cp);chip.setOnClickListener(v->showEffect(id));
        }
        if(effectState.mask==0){TextView empty=button(getString(R.string.fault_chain_empty));empty.setTextColor(MUTED);selectedRoute.addView(empty,new LinearLayout.LayoutParams(-2,dp(44)));empty.setOnClickListener(v->showChain());}
        TextView tag=viewfinder.findViewWithTag("fx");tag.setText(original?"  RAW / ORIGINAL  ":"  FX / "+active.length+"  ");
        strength.setProgress(Math.round(effectState.amount*100));strengthValue.setText(strength.getProgress()+"%");boolean processing=!original&&active.length>0;strength.setEnabled(processing);strength.setAlpha(processing?1:.35f);if(effectPreview!=null)tag.setText(getString(R.string.ui_preview_editing));
    }
    String liveChainDetails(EffectState.Frame frame){StringBuilder text=new StringBuilder(Effects.chainName(frame.ids()));for(FaultNode n:frame.nodes)text.append("\n").append(Effects.name(n.id)).append(" · ").append(getString(R.string.fault_incident)).append(" ").append(Math.round(n.event.envelope*100)).append("%");return text.toString();}
    public void liveFrame(EffectState.Frame frame){
        shownLiveFrame=frame;if(effectEditorOwner instanceof EffectDialog){AdvancedControls controls=((EffectDialog)effectEditorOwner).advancedControls;if(controls!=null)controls.update(frame);}if(faultStatePanel!=null&&liveChainDialog!=null&&liveChainDialog.isShowing())faultStatePanel.update(frame);if(liveChainStatus==null)return;boolean visible=faultConfig.enabled&&!rawOriginal()&&effectPreview==null;
        liveChainStatus.setVisibility(visible?View.VISIBLE:View.GONE);if(!visible)return;
        String label=getString(R.string.ui_live_fault_current_chain)+"  ·  "+frame.nodes.size()+"  ·  "+FaultDialog.styles(this)[faultConfig.performance.style]+"   ›";
        if(!label.contentEquals(liveChainStatus.getText()))liveChainStatus.setText(label);liveChainStatus.setContentDescription(label+getString(R.string.ui_tap_to_view_the_full_chain));
        TextView tag=viewfinder.findViewWithTag("fx");tag.setText("  LIVE / "+frame.nodes.size()+"/"+frame.ids().length+"  ");

    }
    void showChain(){if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}new EffectDialog(this,false).show();}
    void showParameters(){if(rawOriginal()||effectState.mask==0){showChain();return;}if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}new EffectDialog(this,true).show();}
    boolean effectAvailable(int id){return !rawOriginal()&&Effects.available(id,videoMode,!videoMode&&settings.photoFormat==2);}
    void showEffect(int id){if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}EffectDialog d=new EffectDialog(this,true);d.focused=id;d.tuning=true;d.show();}
    Dialog showFormat(){
        if(recording||engine.photoBusy)return null;
        String[] labels=videoMode?(cameraOptions!=null&&cameraOptions.rawVideoAvailable()?new String[]{"MP4","RAW ZIP"}:new String[]{"MP4"}):(cameraOptions!=null&&!cameraOptions.raws.isEmpty()?new String[]{"JPG","RAW"}:new String[]{"JPG"});
        return SignalSheet.anchoredPick(this,formatButton,getString(R.string.ui_save_format),labels,videoMode?(settings.rawVideo?1:0):(settings.photoFormat==0?0:1),index->{CaptureSettings next=new CaptureSettings(settings);if(videoMode)next.rawVideo=index==1;else{next.photoFormat=index==0?0:2;next.photoSize="recommended";}applySettings(next);});
    }
    void randomChain(){
        if(rawOriginal())return;
        if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}
        commitEffects(EffectRandomizer.chain(effectState,availableEffects(),new java.security.SecureRandom()));
        selectedRoute.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }
    void reseed(){
        if(rawOriginal())return;
        EffectParameters p=effectState.parameters();java.security.SecureRandom random=new java.security.SecureRandom();
        for(int id:effectState.ids())if(id!=Effects.COLOR_MAP)p=p.reseed(id,random.nextLong());
        commitEffects(effectState.edit(effectState.chained,effectState.mask,p));selectedRoute.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }
    void renderCaptureMode(){renderAudio();boolean value=videoMode;photoTab.setTextColor(!value?LIME:MUTED);videoTab.setTextColor(value?LIME:MUTED);photoTab.setBackground(bg(!value?PANEL:BG,0));videoTab.setBackground(bg(value?PANEL:BG,0));capture.setContentDescription(value?getString(R.string.ui_start_video_recording):getString(R.string.ui_take_a_photo));capture.invalidate();}
    void setVideo(boolean value){
        if(!ready||recording||engine.photoBusy||videoMode==value)return;
        if(value&&cameraOptions!=null&&cameraOptions.videos.isEmpty()&&!(settings.rawVideo&&cameraOptions.rawVideoAvailable())){Toast.makeText(this,getString(R.string.ui_video_is_unavailable_on_this_camera),Toast.LENGTH_SHORT).show();return;}
        cancelEffectPreview();videoMode=value;ready(false);engine.configure(settings,value,effectState);renderEffects();renderCaptureMode();savePrefs();
    }
    void setFaultConfig(FaultConfig value){
        if(value.enabled&&value.audio&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingFaultConfig=value;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},4);return;}
        applyFaultConfig(value);
    }
    void applyFaultConfig(FaultConfig value){faultConfig=value;if(liveChainStatus!=null)liveChainStatus.setVisibility(value.enabled&&!rawOriginal()?View.VISIBLE:View.GONE);renderEffects();engine.setFaultConfig(value);FaultPreferences.save(getSharedPreferences("signal",0),value);if(faultSwitch!=null){faultSwitch.setChecked(value.enabled);faultSwitch.setTextColor(value.enabled?LIME:MUTED);}}
    void savePrefs(){android.content.SharedPreferences.Editor prefs=getSharedPreferences("signal",0).edit().putBoolean("sound",sound);EffectStateStore.write(prefs,effectState);prefs.apply();}
    void shoot(){
        if(liveEditor!=null){if(recording)liveEditor.dialog.dismiss();else{Toast.makeText(this,R.string.ui_apply_or_discard_your_edits_before_capturing,Toast.LENGTH_SHORT).show();return;}}
        if(effectPreview!=null){if(recording)cancelEffectPreview();else{Toast.makeText(this,getString(R.string.ui_apply_or_discard_your_edits_before_capturing),Toast.LENGTH_SHORT).show();return;}}
        if(!ready){if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);else Toast.makeText(this,getString(R.string.ui_preparing_the_camera),Toast.LENGTH_SHORT).show();return;}
        capture.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        if(videoMode){ if(!recording && sound && !settings.rawVideo && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2);return;}ready(false);engine.toggleVideo(sound&&!settings.rawVideo); }
        else {ready(false);engine.photo(preview.getSurfaceTexture()==null?0:preview.getSurfaceTexture().getTimestamp());overlay.flash=true;overlay.invalidate();handler.postDelayed(()->{overlay.flash=false;overlay.invalidate();},90);}
    }
    void openGallery(){if(recording||engine.photoBusy||mediaPreview!=null)return;cancelEffectPreview();if(liveEditor!=null)liveEditor.dialog.dismiss();if(liveChainDialog!=null)liveChainDialog.dismiss();if(latest==null){Toast.makeText(this,getString(R.string.ui_open_your_captured_photos_and_videos_here),Toast.LENGTH_SHORT).show();return;}try{if("application/zip".equals(getContentResolver().getType(latest))){openExternal(latest);return;}}catch(Exception ignored){}new MediaPreview(this,latest,latestVideo).show();}
    void openExternal(Uri uri){try{Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,getContentResolver().getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_DOCUMENT|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);startActivity(intent);}catch(Exception e){Toast.makeText(this,getString(R.string.ui_photos_are_in_pictures_videos_in_movies_and),Toast.LENGTH_LONG).show();}}
    void resumeCameraPreview(){if(!resumed||mediaPreview!=null)return;handler.removeCallbacks(previewAcknowledgement);handler.post(previewAcknowledgement);if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&preview.isAvailable())engine.attach(preview.getSurfaceTexture(),preview.getWidth(),preview.getHeight());}
    public void status(String s){if(!recording)status.setText(s);if(s.contains(getString(R.string.ui_failed))||s.contains(getString(R.string.ui_could_not))||s.contains(getString(R.string.ui_error)))Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    public void ready(boolean value){ready=value&&resumed&&mediaPreview==null;capture.setAlpha(ready?1:.4f);}
    public void recording(boolean value){recording=value;capture.invalidate();capture.setContentDescription(value?getString(R.string.ui_stop_recording):videoMode?getString(R.string.ui_start_video_recording):getString(R.string.ui_take_a_photo));flipButton.setEnabled(!value);flipButton.setAlpha(value?.3f:1);micButton.setAlpha(value?.3f:1);renderAudio();galleryButton.setEnabled(!value);if(value){start=SystemClock.elapsedRealtime();status.setTextColor(RED);handler.post(timer);}else{handler.removeCallbacks(timer);status.setTextColor(LIME);status.setText("LIVE · "+engine.description());}}
    public void saved(Uri uri,boolean video){boolean rawSequence="application/zip".equals(getContentResolver().getType(uri));latest=uri;latestVideo=video;captureCount++;getSharedPreferences("signal",0).edit().putString("last",uri.toString()).putBoolean("lastVideo",video).apply();Toast.makeText(this,rawSequence?getString(R.string.ui_raw_video_saved_to_download_5igna1):video?getString(R.string.ui_video_saved_to_movies_5igna1):getString(R.string.ui_photo_saved_to_pictures_5igna1),Toast.LENGTH_SHORT).show();galleryButton.load(uri,video);}
    protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(previewAcknowledgement);handler.post(previewAcknowledgement);geo.start();galleryButton.load(latest,latestVideo);if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);else resumeCameraPreview();}
    protected void onPause(){resumed=false;if(mediaPreview!=null)mediaPreview.dismiss();if(liveChainDialog!=null)liveChainDialog.dismiss();if(liveEditor!=null)liveEditor.dialog.dismiss();cancelEffectPreview();savePrefs();ready(false);resumed=false;handler.removeCallbacks(previewAcknowledgement);geo.stop();engine.detach();handler.removeCallbacks(timer);super.onPause();}
    protected void onDestroy(){galleryButton.dispose();engine.shutdown();super.onDestroy();}
    public void onRequestPermissionsResult(int code,String[] p,int[] results){super.onRequestPermissionsResult(code,p,results);if(code==4&&pendingFaultConfig!=null){FaultConfig requested=pendingFaultConfig;pendingFaultConfig=null;boolean allowed=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;applyFaultConfig(allowed?requested:requested.audio(false));if(!allowed)Toast.makeText(this,getString(R.string.ui_audio_reaction_is_off_live_fault_can_use),Toast.LENGTH_LONG).show();return;}if(code==1){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED && resumed && mediaPreview==null && preview.isAvailable())engine.attach(preview.getSurfaceTexture(),preview.getWidth(),preview.getHeight());else{status.setText(getString(R.string.ui_tap_capture_to_allow_camera_access));SignalSheet.message(this,getString(R.string.ui_settings),getString(R.string.ui_camera_access_is_required_to_capture_if_the),R.string.ui_settings,()->openAppSettings());}}if(code==3){if(resumed)geo.start();renderGeo();showLocation();}if(code==2){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)shoot();else Toast.makeText(this,getString(R.string.ui_turn_audio_off_to_record_a_silent_video),Toast.LENGTH_LONG).show();}}
    public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){if(resumed && mediaPreview==null && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)engine.attach(s,w,h);}
    public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){engine.resize(w,h);}
    public boolean onSurfaceTextureDestroyed(SurfaceTexture s){engine.releaseSurface(s);return false;}
    public void onSurfaceTextureUpdated(SurfaceTexture s){engine.previewPresented(s.getTimestamp());}
    void renderAudio(){if(micButton!=null){boolean silent=videoMode&&settings.rawVideo,enabled=sound&&!silent;micButton.setImageResource(enabled?R.drawable.ic_mic:R.drawable.ic_mic_off);micButton.setColorFilter(enabled?LIME:MUTED);micButton.setSelected(enabled);micButton.setEnabled(!recording&&!silent);String label=silent?getString(R.string.ui_raw_silent):getString(R.string.capture_record_audio)+" · "+getString(sound?R.string.ui_audio_on:R.string.ui_audio_off);micButton.setContentDescription(label);micButton.setTooltipText(label);}}
    void renderTorch(){if(torchButton!=null){torchButton.setColorFilter(engine.torch?LIME:MUTED);torchButton.setSelected(engine.torch);String label=getString(engine.torch?R.string.ui_light_on:R.string.ui_light_off);torchButton.setContentDescription(label);torchButton.setTooltipText(label);}}
    void renderGeo(){if(geoButton!=null){geoButton.setColorFilter(geo.enabled?LIME:MUTED);geoButton.setSelected(geo.enabled);String label=getString(R.string.ui_capture_location_settings)+" · "+geo.label();geoButton.setContentDescription(label);geoButton.setTooltipText(label);}}
    void setLocationEnabled(boolean value){settings.location=value;settings.save(getSharedPreferences("signal",0));geo.setEnabled(value);engine.setLocationEnabled(value);renderGeo();}
    void requestLocationAccess(){
        android.content.SharedPreferences prefs=getSharedPreferences("signal",0);
        boolean asked=prefs.getBoolean("locationPermissionAsked",false);
        if(!geo.permitted()&&asked&&!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)&&!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
            SignalSheet.message(this,getString(R.string.ui_check_location_permission),getString(R.string.ui_in_app_info_permissions_location_allow_access_while),R.string.ui_app_settings,()->openAppSettings());return;
        }
        prefs.edit().putBoolean("locationPermissionAsked",true).apply();
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},3);
    }
    void openAppSettings(){startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    void showLocation(){
        if(recording){Toast.makeText(this,getString(R.string.ui_change_location_settings_after_recording_stops),Toast.LENGTH_SHORT).show();return;}
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(12),dp(22),0);
        TextView details=text(geo.detail(),13,WHITE);details.setLineSpacing(dp(4),1);body.addView(details);
        String primary=!geo.enabled?getString(R.string.ui_enable_capture_location):!geo.permitted()?getString(R.string.ui_allow_location):!geo.servicesEnabled()?getString(R.string.ui_device_location_settings):getString(R.string.ui_get_location_again);
        TextView enable=button(primary);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(16);body.addView(enable,bp);
        TextView precise=button(getString(R.string.ui_allow_precise_location));if(geo.enabled&&geo.permitted()&&!geo.precise()){LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(48));pp.topMargin=dp(8);body.addView(precise,pp);}
        TextView permissions=button(getString(R.string.ui_app_permissions));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(44));ap.topMargin=dp(8);body.addView(permissions,ap);permissions.setOnClickListener(v->openAppSettings());
        Dialog dialog=SignalSheet.content(this,getString(R.string.ui_capture_location_gps),body,geo.enabled?R.string.ui_do_not_save_location:0,()->setLocationEnabled(false),.65f);
        enable.setOnClickListener(v->{dialog.dismiss();setLocationEnabled(true);if(!geo.permitted())requestLocationAccess();else if(!geo.servicesEnabled())startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));else{geo.retry();showLocation();}});
        precise.setOnClickListener(v->{dialog.dismiss();requestLocationAccess();});
        Runnable update=new Runnable(){public void run(){if(!dialog.isShowing())return;details.setText(geo.detail());handler.postDelayed(this,1000);}};update.run();dialog.setOnDismissListener(d->handler.removeCallbacks(update));
    }
    void showSettings(){if(recording||engine.photoBusy){Toast.makeText(this,getString(R.string.ui_change_settings_after_capture_finishes),Toast.LENGTH_SHORT).show();return;}new QualityDialog(this).show();}
    void applySettings(CaptureSettings value){if(recording||engine.photoBusy){Toast.makeText(this,getString(R.string.ui_change_settings_after_capture_finishes),Toast.LENGTH_SHORT).show();return;}cancelEffectPreview();settings=new CaptureSettings(value);if(settings.photoFormat==1)settings.photoFormat=2;settings.save(getSharedPreferences("signal",0));geo.setEnabled(settings.location);if(settings.location&&!geo.permitted())requestLocationAccess();ready(false);engine.configure(settings,videoMode,effectState);renderEffects();renderCaptureMode();savePrefs();}
    public void configured(CameraOptions choices,CaptureSettings actual,boolean actualVideo,int w,int h,String detail){if(actualVideo!=videoMode)return;if(actual.photoFormat!=settings.photoFormat)cancelEffectPreview();cameraOptions=choices;settings=new CaptureSettings(actual);settings.location=geo.enabled;renderAudio();displayAspect=w/(float)h;int aw=previewArea.getWidth(),ah=previewArea.getHeight();measuredFps=0;zoomButton.setEnabled(!rawOriginal()&&(videoMode||settings.photoFormat==0));zoomButton.setAlpha(!rawOriginal()&&(videoMode||settings.photoFormat==0)?1:.4f);if(rawOriginal()||(!videoMode&&settings.photoFormat!=0)){zoom=1;zoomButton.setText("1×");}if(aw>0&&ah>0){int vw=Math.min(aw,(int)(ah*displayAspect));FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)viewfinder.getLayoutParams();p.width=vw;p.height=(int)(vw/displayAspect);p.gravity=Gravity.CENTER;viewfinder.setLayoutParams(p);}count.setText(videoMode?(settings.rawVideo?"RAW · ":"")+engine.videoChoice.fps+" fps / ⚙":String.format(Locale.US,"%.1f MP / ⚙",w*(double)h/1e6));renderEffects();}
    public void fps(float value){measuredFps=value;if(ready&&!recording&&!engine.cooling)status.setText("LIVE · "+engine.description()+" · "+engine.loadSummary());}
    public boolean onKeyDown(int key,android.view.KeyEvent event){if(key==KeyEvent.KEYCODE_VOLUME_DOWN || key==KeyEvent.KEYCODE_VOLUME_UP){if(event.getRepeatCount()==0)shoot();return true;}return super.onKeyDown(key,event);}
    public void onBackPressed(){if(recording)engine.toggleVideo(sound&&!settings.rawVideo);else super.onBackPressed();}
    class CaptureButton extends View {Paint p=new Paint(3);CaptureButton(){super(MainActivity.this);setClickable(true);}protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)-dp(5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(recording?RED:WHITE);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);p.setColor(videoMode?RED:LIME);if(recording)c.drawRoundRect(x-dp(13),y-dp(13),x+dp(13),y+dp(13),dp(5),dp(5),p);else c.drawCircle(x,y,r-dp(6),p);if(!videoMode){p.setColor(BG);c.drawRect(x-dp(8),y-dp(2),x+dp(8),y+dp(2),p);c.drawRect(x-dp(2),y-dp(8),x+dp(2),y+dp(8),p);}}}
    class Overlay extends View{Paint p=new Paint(3);boolean flash;float focusX=-1,focusY;Overlay(){super(MainActivity.this);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(0x228C9B83);p.setStrokeWidth(1);for(int i=1;i<3;i++){c.drawLine(w*i/3,0,w*i/3,h,p);c.drawLine(0,h*i/3,w,h*i/3,p);}p.setColor(0x997FFF40);p.setStrokeWidth(dp(1));float cx=w/2,cy=h/2;c.drawLine(cx-dp(6),cy,cx+dp(6),cy,p);c.drawLine(cx,cy-dp(6),cx,cy+dp(6),p);if(focusX>=0){p.setStyle(Paint.Style.STROKE);p.setColor(LIME);c.drawRoundRect(focusX-dp(25),focusY-dp(25),focusX+dp(25),focusY+dp(25),dp(6),dp(6),p);p.setStyle(Paint.Style.FILL);}if(flash)c.drawColor(0xAAFFFFFF);}}
}
