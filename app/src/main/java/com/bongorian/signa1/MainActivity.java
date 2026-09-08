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
    String[] DESCRIPTIONS;
    GlitchEngine engine;
    TextureView preview;
    FrameLayout viewfinder,previewArea;
    CameraOptions cameraOptions; CaptureSettings settings; GeoTags geo; TextView geoButton; float displayAspect=.75f,measuredFps;
    TextView status, count, effectTitle, description, strengthValue, photoTab, videoTab, micButton, torchButton, zoomButton;
    ImageView flipButton;MediaThumbnail galleryButton;
    TextView[] presets=new TextView[MODES.length];
    SeekBar strength;
    CaptureButton capture;
    Overlay overlay;
    boolean resumed, ready, recording, videoMode, sound=true;
    boolean latestVideo;
    int captureCount;
    EffectState effectState;
    TextView liveChainStatus;AlertDialog liveChainDialog;EffectState.Frame shownLiveFrame;
    FaultConfig faultConfig=FaultConfig.defaults(),pendingFaultConfig;ToggleButton faultSwitch;
    static final class EffectPreview {final EffectState base;final boolean video;final int format;android.app.Dialog dialog;EffectPreview(EffectState s,boolean v,int f){base=s;video=v;format=f;}}
    private EffectPreview effectPreview;
    long start;
    float zoom=1f;
    Uri latest;
    final Handler handler=new Handler(Looper.getMainLooper());
    final Runnable timer=new Runnable(){ public void run(){ if(recording){ long sec=(SystemClock.elapsedRealtime()-start)/1000; status.setText(settings.rawVideo?String.format(Locale.US,"● RAW %02d:%02d  ",sec/60,sec%60)+engine.rawProgress():String.format(Locale.US,"● REC %02d:%02d  %.0f fps",sec/60,sec%60,measuredFps)); handler.postDelayed(this,250); } } };
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    public void onCreate(Bundle b){
        super.onCreate(b);DESCRIPTIONS=getResources().getStringArray(R.array.effect_descriptions);
        getWindow().setDecorFitsSystemWindows(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(0);
        android.content.SharedPreferences prefs=getSharedPreferences("signal",0);
        sound=prefs.getBoolean("sound",true);
        String last=prefs.getString("last",null); if(last!=null)latest=Uri.parse(last); latestVideo=prefs.getBoolean("lastVideo",false);
        if(b!=null){videoMode=b.getBoolean("session.video",false);zoom=b.getFloat("session.zoom",1);captureCount=b.getInt("session.count",0);}
        settings=CaptureSettings.load(prefs); geo=new GeoTags(this,this::renderGeo);geo.enabled=settings.location;
        effectState=EffectStateStore.load(prefs).forContext(videoMode,settings.photoFormat);
        engine=new GlitchEngine(this,this);engine.position=geo::snapshot;engine.configure(settings,videoMode,effectState);if(b!=null){engine.front=b.getBoolean("session.front",false);engine.zoom=zoom;}
        faultConfig=FaultPreferences.load(prefs);if(b!=null)faultConfig=faultConfig.enabled(b.getBoolean("session.live",false));engine.setFaultConfig(faultConfig);
        buildUi();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);savePrefs();out.putInt("session.count",captureCount);out.putBoolean("session.video",videoMode);out.putBoolean("session.front",engine.front);out.putFloat("session.zoom",zoom);out.putBoolean("session.live",faultConfig.enabled);}
    GradientDrawable bg(int color,int radius,int border){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if(border!=0)d.setStroke(dp(1),border); return d; }
    TextView text(String label,int size,int color){ TextView t=new TextView(this); t.setText(label); t.setTextColor(color); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL); t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(false); return t; }
    TextView button(String label){ TextView t=text(label,12,WHITE); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL),Typeface.NORMAL); t.setBackground(bg(PANEL,14,0)); t.setPadding(dp(12),0,dp(12),0); t.setContentDescription(label); return t; }
    ImageView iconButton(int resource,String description){ImageView icon=new ImageView(this);icon.setImageResource(resource);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setPadding(dp(12),dp(12),dp(12),dp(12));icon.setContentDescription(description);icon.setTooltipText(description);icon.setFocusable(true);icon.setClickable(true);return icon;}
    LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(18),dp(8),dp(18),dp(4)); setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            root.setPadding(dp(18)+safe.left,dp(8)+safe.top,dp(18)+safe.right,dp(4)+safe.bottom);
            return insets;
        });root.requestApplyInsets();
        LinearLayout header=row(); root.addView(header,new LinearLayout.LayoutParams(-1,dp(49)));
        TextView logo=text(BuildConfig.APP_NAME,23,WHITE); logo.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); logo.setLetterSpacing(.10f);logo.setContentDescription(getString(R.string.ui_about_5igna1_and_privacy_policy));logo.setOnClickListener(v->AboutDialog.show(this)); header.addView(logo,new LinearLayout.LayoutParams(0,-1,1));
        ImageView settingsButton=iconButton(R.drawable.ic_settings,getString(R.string.ui_capture_and_language_settings));settingsButton.setBackground(bg(PANEL,14,0));settingsButton.setOnClickListener(v->showSettings());header.addView(settingsButton,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout info=row(); root.addView(info,new LinearLayout.LayoutParams(-1,dp(27)));
        status=text("CONNECTING…",10,LIME); status.setTypeface(Typeface.MONOSPACE);status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.END); info.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        count=text("FULL RES",10,MUTED);count.setOnClickListener(v->showSettings()); count.setTypeface(Typeface.MONOSPACE);count.setSingleLine(true);count.setEllipsize(android.text.TextUtils.TruncateAt.END);count.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams countParams=new LinearLayout.LayoutParams(dp(100),-1);countParams.leftMargin=dp(10);info.addView(count,countParams);
        // Fit a true 9:16 preview into the remaining space. Saved media has the same framing.
        previewArea=new FrameLayout(this); root.addView(previewArea,new LinearLayout.LayoutParams(-1,0,1));
        viewfinder=new FrameLayout(this); viewfinder.setBackground(bg(Color.BLACK,18,0)); viewfinder.setClipToOutline(true);
        previewArea.addView(viewfinder,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        previewArea.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int availableW=r-l,availableH=b-t;
            if(availableW<=0 || availableH<=0)return;
            int w=Math.min(availableW,(int)(availableH*displayAspect)),h=(int)(w/displayAspect);
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)viewfinder.getLayoutParams();
            if(p.width!=w || p.height!=h){p.width=w;p.height=h;p.gravity=Gravity.CENTER;viewfinder.setLayoutParams(p);}
        });
        preview=new TextureView(this); preview.setContentDescription(getString(R.string.ui_camera_preview_tap_to_focus)); preview.setOnClickListener(v -> engine.focus()); preview.setSurfaceTextureListener(this); viewfinder.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        overlay=new Overlay(); viewfinder.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        preview.setOnTouchListener(new View.OnTouchListener(){float downX;public boolean onTouch(View v,android.view.MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;} if(e.getAction()==MotionEvent.ACTION_UP){float diff=e.getX()-downX; if(Math.abs(diff)>dp(60)){chooseEffect(nextAvailable(diff<0?1:-1));}else{v.performClick();overlay.focusX=e.getX();overlay.focusY=e.getY();overlay.invalidate();handler.postDelayed(()->{overlay.focusX=-1;overlay.invalidate();},900);}return true;}return true;}});
        TextView liveTag=text("  FX / "+MODES[effectState.selected()]+"  ",10,LIME); liveTag.setTypeface(Typeface.MONOSPACE); liveTag.setBackground(bg(0x99101410,6,0));
        FrameLayout.LayoutParams tagP=new FrameLayout.LayoutParams(-2,dp(26),Gravity.TOP|Gravity.START);tagP.setMargins(dp(12),dp(12),0,0);viewfinder.addView(liveTag,tagP);liveTag.setTag("fx");
        LinearLayout lens=row(); lens.setGravity(Gravity.CENTER); FrameLayout.LayoutParams lensP=new FrameLayout.LayoutParams(dp(56),dp(42),Gravity.TOP|Gravity.END);lensP.topMargin=dp(12);viewfinder.addView(lens,lensP);
        zoomButton=button(zoom==1?"1×":"2×"); zoomButton.setBackground(bg(0xCC121712,24,0));lens.addView(zoomButton,new LinearLayout.LayoutParams(dp(50),dp(38)));zoomButton.setOnClickListener(v->{zoom=zoom==1?2:1;engine.zoom(zoom);zoomButton.setText(zoom==1?"1×":"2×");});
        LinearLayout effects=new LinearLayout(this); effects.setOrientation(LinearLayout.VERTICAL); effects.setPadding(dp(2),dp(10),dp(2),dp(2));
        GradientDrawable shade=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00101410,0xEE0A0C0B,0xFA0A0C0B}); effects.setBackgroundColor(BG);
        FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(effects,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout effectRow=row(); LinearLayout.LayoutParams er=new LinearLayout.LayoutParams(-1,dp(38)); er.topMargin=dp(8);effects.addView(effectRow,er);
        effectTitle=text(MODES[effectState.selected()],18,WHITE);effectTitle.setSingleLine(true);effectTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);effectTitle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));effectTitle.setLetterSpacing(.06f);effectRow.addView(effectTitle,new LinearLayout.LayoutParams(0,-1,1));
        TextView adjust=button(getString(R.string.ui_adjust));adjust.setTextColor(MUTED);adjust.setBackgroundColor(Color.TRANSPARENT);effectRow.addView(adjust,new LinearLayout.LayoutParams(dp(68),dp(40)));adjust.setOnClickListener(v->showParameters());
        TextView chainButton=button(getString(R.string.ui_chain));effectRow.addView(chainButton,new LinearLayout.LayoutParams(dp(96),dp(40)));chainButton.setOnClickListener(v->showChain());

        description=text(DESCRIPTIONS[effectState.selected()],11,MUTED);description.setSingleLine(true);description.setEllipsize(android.text.TextUtils.TruncateAt.END);effects.addView(description,new LinearLayout.LayoutParams(-1,dp(22)));
        liveChainStatus=text("",10,LIME);liveChainStatus.setSingleLine(true);liveChainStatus.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);liveChainStatus.setMarqueeRepeatLimit(-1);liveChainStatus.setSelected(true);liveChainStatus.setVisibility(View.GONE);effects.addView(liveChainStatus,new LinearLayout.LayoutParams(-1,dp(25)));liveChainStatus.setOnClickListener(v->{if(shownLiveFrame!=null)liveChainDialog=new AlertDialog.Builder(this).setTitle(getString(R.string.ui_live_fault_current_chain)).setMessage(liveChainDetails(shownLiveFrame)).setPositiveButton(getString(R.string.ui_close),null).show();});
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(47));sp.topMargin=dp(5);effects.addView(scroll,sp);
        LinearLayout strip=row();scroll.addView(strip);
        for(int rank=0;rank<Effects.ORDER.length;rank++){final int i=Effects.ORDER[rank];final int index=i;TextView p=button(MODES[i]);p.setTextSize(10);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,dp(39));pp.rightMargin=dp(7);strip.addView(p,pp);presets[i]=p;p.setOnClickListener(v->{chooseEffect(index);scroll.smoothScrollTo(p.getLeft()-dp(40),0);});}
        LinearLayout power=row();effects.addView(power,new LinearLayout.LayoutParams(-1,dp(39)));
        TextView strengthLabel=text(getString(R.string.ui_strength),11,MUTED);power.addView(strengthLabel,new LinearLayout.LayoutParams(dp(48),-1));
        strength=new SeekBar(this);strength.setMax(100);strength.setProgress(Math.round(effectState.amount*100));strength.setProgressTintList(ColorStateList.valueOf(LIME));strength.setThumbTintList(ColorStateList.valueOf(LIME));power.addView(strength,new LinearLayout.LayoutParams(0,dp(36),1));
        strengthValue=text(strength.getProgress()+"%",11,LIME);strengthValue.setTypeface(Typeface.MONOSPACE);strengthValue.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);power.addView(strengthValue,new LinearLayout.LayoutParams(dp(43),-1));
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean from){strengthValue.setText(p+"%");if(from)commitEffects(effectState.amount(p/100f));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){savePrefs();}});
        LinearLayout tools=row();LinearLayout.LayoutParams toolP=new LinearLayout.LayoutParams(-1,dp(44));toolP.topMargin=dp(3);toolP.bottomMargin=dp(7);effects.addView(tools,toolP);
        TextView random=button(getString(R.string.ui_random));random.setTextSize(11);random.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);random.setCompoundDrawablePadding(dp(6));random.setContentDescription(getString(R.string.ui_tap_to_randomize_an_effect_hold_to_randomize));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1.5f);rp.rightMargin=dp(8);tools.addView(random,rp);
        random.setOnClickListener(v->reseed());
        LinearLayout live=row();live.setBackground(bg(PANEL,14,0));tools.addView(live,new LinearLayout.LayoutParams(0,-1,1));
        faultSwitch=new ToggleButton(this);faultSwitch.setTextOn("LIVE FAULT · ON");faultSwitch.setTextOff("LIVE FAULT · OFF");faultSwitch.setTextSize(10);faultSwitch.setAllCaps(false);faultSwitch.setBackgroundColor(Color.TRANSPARENT);faultSwitch.setPadding(dp(8),0,0,0);faultSwitch.setChecked(faultConfig.enabled);faultSwitch.setTextColor(faultConfig.enabled?LIME:MUTED);faultSwitch.setContentDescription(getString(R.string.ui_toggle_live_fault));live.addView(faultSwitch,new LinearLayout.LayoutParams(0,-1,1));
        ImageView reactions=iconButton(R.drawable.ic_tune,getString(R.string.ui_live_fault_settings));reactions.setPadding(dp(11),dp(11),dp(11),dp(11));live.addView(reactions,new LinearLayout.LayoutParams(dp(42),-1));reactions.setOnClickListener(v->FaultDialog.show(this));
        faultSwitch.setOnCheckedChangeListener((view,checked)->{if(checked!=faultConfig.enabled)setFaultConfig(faultConfig.enabled(checked));});
        LinearLayout modes=row();modes.setGravity(Gravity.CENTER);root.addView(modes,new LinearLayout.LayoutParams(-1,dp(35)));
        photoTab=button(getString(R.string.ui_photo));videoTab=button(getString(R.string.ui_video));modes.addView(photoTab,new LinearLayout.LayoutParams(dp(74),dp(30)));modes.addView(videoTab,new LinearLayout.LayoutParams(dp(74),dp(30)));photoTab.setOnClickListener(v->setVideo(false));videoTab.setOnClickListener(v->setVideo(true));
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);root.addView(controls,new LinearLayout.LayoutParams(-1,dp(88)));
        galleryButton=new MediaThumbnail(this);controls.addView(galleryButton,new LinearLayout.LayoutParams(dp(54),dp(54)));galleryButton.setOnClickListener(v->openGallery());galleryButton.load(latest,latestVideo);
        Space spacer=new Space(this);controls.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        capture=new CaptureButton();controls.addView(capture,new LinearLayout.LayoutParams(dp(80),dp(80)));capture.setContentDescription(getString(R.string.ui_take_a_photo));capture.setOnClickListener(v->shoot());
        controls.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));flipButton=iconButton(R.drawable.ic_camera_flip,getString(R.string.ui_switch_front_and_rear_cameras));flipButton.setBackground(bg(PANEL,27,0));controls.addView(flipButton,new LinearLayout.LayoutParams(dp(54),dp(54)));flipButton.setOnClickListener(v->{if(!recording){engine.switchCamera();zoom=1;zoomButton.setText("1×");torchButton.setText(getString(R.string.ui_light_off));}});
        LinearLayout footer=row();root.addView(footer,new LinearLayout.LayoutParams(-1,dp(33)));
        torchButton=text(getString(R.string.ui_light_off),10,MUTED);torchButton.setGravity(Gravity.CENTER);footer.addView(torchButton,new LinearLayout.LayoutParams(0,-1,1));torchButton.setOnClickListener(v->{engine.torch();handler.postDelayed(()->torchButton.setText(engine.torch?getString(R.string.ui_light_on):getString(R.string.ui_light_off)),200);});
        geoButton=text(geo.label(),9,MUTED);geoButton.setGravity(Gravity.CENTER);footer.addView(geoButton,new LinearLayout.LayoutParams(0,-1,1));geoButton.setOnClickListener(v->showLocation());
        micButton=text(sound?getString(R.string.ui_audio_on):getString(R.string.ui_audio_off),10,sound?LIME:MUTED);micButton.setGravity(Gravity.CENTER);footer.addView(micButton,new LinearLayout.LayoutParams(0,-1,1));micButton.setOnClickListener(v->{if(!recording){sound=!sound;micButton.setText(sound?getString(R.string.ui_audio_on):getString(R.string.ui_audio_off));micButton.setTextColor(sound?LIME:MUTED);savePrefs();}});
        renderEffects();renderCaptureMode();savePrefs();
    }
    boolean rawOriginal(){return videoMode?settings.rawVideo:settings.photoFormat==1;}
    int[] availableEffects(){return Effects.choices(videoMode,!videoMode&&settings.photoFormat==2);}
    int[] uiEffects(){return rawOriginal()?new int[0]:effectState.snapshot(videoMode,settings.photoFormat).ids();}
    int nextAvailable(int delta){int[] ids=availableEffects();for(int n=0;n<ids.length;n++)if(ids[n]==effectState.selected())return ids[Math.floorMod(n+delta,ids.length)];return Effects.CLEAN;}
    void chooseEffect(int id){commitEffects(effectState.single(id));}
    void commitEffects(EffectState next){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Effect edits require UI thread");
        cancelEffectPreview();effectState=next.forContext(videoMode,settings.photoFormat);engine.setEffects(effectState);renderEffects();savePrefs();
    }
    EffectPreview beginEffectPreview(){cancelEffectPreview();effectPreview=new EffectPreview(effectState,videoMode,settings.photoFormat);return effectPreview;}
    boolean validPreview(EffectPreview edit){return effectPreview==edit&&effectState==edit.base&&videoMode==edit.video&&settings.photoFormat==edit.format;}
    void previewEffectEdit(EffectPreview edit,EffectState draft){if(validPreview(edit)){engine.previewEffects(draft.forContext(videoMode,settings.photoFormat));TextView tag=viewfinder.findViewWithTag("fx");tag.setText(getString(R.string.ui_preview_editing));}}
    void finishEffectEdit(EffectPreview edit,EffectState draft,boolean apply){
        if(effectPreview!=edit)return;boolean valid=validPreview(edit);effectPreview=null;
        if(apply&&valid)commitEffects(draft);else{engine.clearEffectPreview();renderEffects();if(apply)Toast.makeText(this,getString(R.string.ui_edits_discarded_because_the_capture_mode_changed),Toast.LENGTH_SHORT).show();}
    }
    void cancelEffectPreview(){EffectPreview edit=effectPreview;if(edit==null)return;effectPreview=null;engine.clearEffectPreview();if(edit.dialog!=null)edit.dialog.dismiss();}
    void renderEffects(){
        int i=effectState.selected();boolean original=rawOriginal(),chained=effectState.chained;int[] active=uiEffects();
        effectTitle.setText(original?"RAW ORIGINAL":chained?"CHAIN · "+active.length:MODES[i]);
        String label=original?getString(R.string.ui_original_sensor_data_no_effects):chained?Effects.chainName(active):DESCRIPTIONS[i];
        if(!original&&!videoMode&&settings.photoFormat==2)label=getString(R.string.ui_raw_preview)+label;
        description.setText(label);description.setSelected(chained);description.setEllipsize(chained?android.text.TextUtils.TruncateAt.MARQUEE:android.text.TextUtils.TruncateAt.END);description.setMarqueeRepeatLimit(-1);
        TextView tag=viewfinder.findViewWithTag("fx");tag.setText(original?"  RAW / ORIGINAL  ":chained?"  CHAIN / "+active.length+"  ":"  FX / "+MODES[i]+"  ");
        for(int n=0;n<MODES.length;n++){
            boolean visible=Effects.available(n,videoMode,!videoMode&&settings.photoFormat==2);presets[n].setVisibility(visible?View.VISIBLE:View.GONE);
            boolean on=!original&&(effectState.enabled(n)||(n==Effects.CLEAN&&effectState.mask==0));presets[n].setTextColor(on?BG:MUTED);presets[n].setBackground(bg(on?LIME:BG,18,on?0:PANEL));presets[n].setEnabled(!original);presets[n].setAlpha(original?.35f:1);
        }
        strength.setProgress(Math.round(effectState.amount*100));strengthValue.setText(strength.getProgress()+"%");boolean processing=!original&&active.length>0;strength.setEnabled(processing);strength.setAlpha(processing?1:.35f);if(effectPreview!=null)tag.setText(getString(R.string.ui_preview_editing));
    }
    String liveChainDetails(EffectState.Frame frame){StringBuilder text=new StringBuilder(Effects.chainName(frame.ids()));for(FaultNode n:frame.nodes)text.append("\n").append(Effects.name(n.id)).append(" · ").append(getString(R.string.fault_incident)).append(" ").append(Math.round(n.event.envelope*100)).append("%");return text.toString();}
    public void liveFrame(EffectState.Frame frame){
        shownLiveFrame=frame;if(liveChainDialog!=null&&liveChainDialog.isShowing())liveChainDialog.setMessage(liveChainDetails(frame));if(liveChainStatus==null)return;boolean visible=faultConfig.enabled&&!rawOriginal()&&effectPreview==null;
        liveChainStatus.setVisibility(visible?View.VISIBLE:View.GONE);if(!visible)return;
        String label="LIVE · "+frame.ids().length+getString(R.string.ui_stages)+Math.round(frame.amount*100)+"%  /  "+Effects.chainName(frame.ids());
        if(!label.contentEquals(liveChainStatus.getText()))liveChainStatus.setText(label);liveChainStatus.setContentDescription(label+getString(R.string.ui_tap_to_view_the_full_chain));
        TextView tag=viewfinder.findViewWithTag("fx");tag.setText("  LIVE / "+frame.ids().length+" STAGES  ");
        for(int id=0;id<MODES.length;id++){final int candidate=id;boolean active=java.util.Arrays.stream(frame.ids()).anyMatch(v->v==candidate)||(id==0&&frame.ids().length==0);presets[id].setTextColor(active?BG:MUTED);presets[id].setBackground(bg(active?LIME:BG,18,active?0:PANEL));}
    }
    void showChain(){if(rawOriginal()){Toast.makeText(this,getString(R.string.ui_original_raw_is_not_processed),Toast.LENGTH_SHORT).show();return;}if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}new EffectDialog(this,false).show();}
    void showParameters(){if(rawOriginal()||effectState.mask==0){showChain();return;}if(recording){Toast.makeText(this,R.string.fault_edit_after_recording,Toast.LENGTH_SHORT).show();return;}new EffectDialog(this,true).show();}
    void reseed(){
        if(rawOriginal())return;
        EffectParameters p=effectState.parameters();java.security.SecureRandom random=new java.security.SecureRandom();
        for(int id:effectState.ids())if(id!=Effects.COLOR_MAP)p=p.reseed(id,random.nextLong());
        commitEffects(effectState.edit(effectState.chained,effectState.mask,p));effectTitle.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }
    void renderCaptureMode(){boolean value=videoMode;photoTab.setTextColor(!value?LIME:MUTED);videoTab.setTextColor(value?LIME:MUTED);photoTab.setBackground(bg(!value?PANEL:BG,10,0));videoTab.setBackground(bg(value?PANEL:BG,10,0));capture.setContentDescription(value?getString(R.string.ui_start_video_recording):getString(R.string.ui_take_a_photo));capture.invalidate();}
    void setVideo(boolean value){
        if(!ready||recording||engine.photoBusy||videoMode==value)return;
        if(value&&cameraOptions!=null&&cameraOptions.videos.isEmpty()&&!(settings.rawVideo&&cameraOptions.rawVideoAvailable())){Toast.makeText(this,getString(R.string.ui_video_is_unavailable_on_this_camera),Toast.LENGTH_SHORT).show();return;}
        cancelEffectPreview();videoMode=value;effectState=effectState.forContext(value,settings.photoFormat);ready(false);engine.configure(settings,value,effectState);renderEffects();renderCaptureMode();savePrefs();
    }
    void setFaultConfig(FaultConfig value){
        if(value.enabled&&value.audio&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingFaultConfig=value;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},4);return;}
        applyFaultConfig(value);
    }
    void applyFaultConfig(FaultConfig value){faultConfig=value;if(liveChainStatus!=null)liveChainStatus.setVisibility(value.enabled&&!rawOriginal()?View.VISIBLE:View.GONE);renderEffects();engine.setFaultConfig(value);FaultPreferences.save(getSharedPreferences("signal",0),value);if(faultSwitch!=null){faultSwitch.setChecked(value.enabled);faultSwitch.setTextColor(value.enabled?LIME:MUTED);}}
    void savePrefs(){android.content.SharedPreferences.Editor prefs=getSharedPreferences("signal",0).edit().putBoolean("sound",sound);EffectStateStore.write(prefs,effectState);prefs.apply();}
    void shoot(){
        if(effectPreview!=null){if(recording)cancelEffectPreview();else{Toast.makeText(this,getString(R.string.ui_apply_or_discard_your_edits_before_capturing),Toast.LENGTH_SHORT).show();return;}}
        if(!ready){if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);else Toast.makeText(this,getString(R.string.ui_preparing_the_camera),Toast.LENGTH_SHORT).show();return;}
        capture.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        if(videoMode){ if(!recording && sound && !settings.rawVideo && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2);return;}ready(false);engine.toggleVideo(sound&&!settings.rawVideo); }
        else {ready(false);engine.photo(preview.getSurfaceTexture()==null?0:preview.getSurfaceTexture().getTimestamp());overlay.flash=true;overlay.invalidate();handler.postDelayed(()->{overlay.flash=false;overlay.invalidate();},90);}
    }
    void openGallery(){if(recording||engine.photoBusy)return;cancelEffectPreview();if(latest==null){Toast.makeText(this,getString(R.string.ui_open_your_captured_photos_and_videos_here),Toast.LENGTH_SHORT).show();return;}try{Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(latest,getContentResolver().getType(latest)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){Toast.makeText(this,getString(R.string.ui_photos_are_in_pictures_videos_in_movies_and),Toast.LENGTH_LONG).show();}}
    public void status(String s){if(!recording)status.setText(s);if(s.contains(getString(R.string.ui_failed))||s.contains(getString(R.string.ui_could_not))||s.contains(getString(R.string.ui_error)))Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    public void ready(boolean value){ready=value&&resumed;capture.setAlpha(ready?1:.4f);}
    public void recording(boolean value){recording=value;capture.invalidate();capture.setContentDescription(value?getString(R.string.ui_stop_recording):videoMode?getString(R.string.ui_start_video_recording):getString(R.string.ui_take_a_photo));flipButton.setEnabled(!value);flipButton.setAlpha(value?.3f:1);micButton.setAlpha(value?.3f:1);galleryButton.setEnabled(!value);if(value){start=SystemClock.elapsedRealtime();status.setTextColor(RED);handler.post(timer);}else{handler.removeCallbacks(timer);status.setTextColor(LIME);status.setText("LIVE · "+engine.description());}}
    public void saved(Uri uri,boolean video){boolean rawSequence="application/zip".equals(getContentResolver().getType(uri));latest=uri;latestVideo=video;captureCount++;getSharedPreferences("signal",0).edit().putString("last",uri.toString()).putBoolean("lastVideo",video).apply();Toast.makeText(this,rawSequence?getString(R.string.ui_raw_video_saved_to_download_5igna1):video?getString(R.string.ui_video_saved_to_movies_5igna1):getString(R.string.ui_photo_saved_to_pictures_5igna1),Toast.LENGTH_SHORT).show();galleryButton.load(uri,video);}
    protected void onResume(){super.onResume();resumed=true;geo.start();galleryButton.load(latest,latestVideo);if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);else if(preview.isAvailable())engine.attach(preview.getSurfaceTexture(),preview.getWidth(),preview.getHeight());}
    protected void onPause(){cancelEffectPreview();savePrefs();ready(false);resumed=false;geo.stop();engine.detach();handler.removeCallbacks(timer);super.onPause();}
    protected void onDestroy(){galleryButton.dispose();engine.shutdown();super.onDestroy();}
    public void onRequestPermissionsResult(int code,String[] p,int[] results){super.onRequestPermissionsResult(code,p,results);if(code==4&&pendingFaultConfig!=null){FaultConfig requested=pendingFaultConfig;pendingFaultConfig=null;boolean allowed=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;applyFaultConfig(allowed?requested:requested.audio(false));if(!allowed)Toast.makeText(this,getString(R.string.ui_audio_reaction_is_off_live_fault_can_use),Toast.LENGTH_LONG).show();return;}if(code==1){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED && resumed && preview.isAvailable())engine.attach(preview.getSurfaceTexture(),preview.getWidth(),preview.getHeight());else{status.setText(getString(R.string.ui_tap_capture_to_allow_camera_access));new AlertDialog.Builder(this).setMessage(getString(R.string.ui_camera_access_is_required_to_capture_if_the)).setPositiveButton(getString(R.string.ui_settings),(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNegativeButton(getString(R.string.ui_close),null).show();}}if(code==3){if(resumed)geo.start();renderGeo();showLocation();}if(code==2){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)shoot();else Toast.makeText(this,getString(R.string.ui_turn_audio_off_to_record_a_silent_video),Toast.LENGTH_LONG).show();}}
    public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){if(resumed && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)engine.attach(s,w,h);}
    public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){engine.resize(w,h);}
    public boolean onSurfaceTextureDestroyed(SurfaceTexture s){engine.releaseSurface(s);return false;}
    public void onSurfaceTextureUpdated(SurfaceTexture s){engine.previewPresented(s.getTimestamp());}
    void renderGeo(){if(geoButton!=null){geoButton.setText(geo.label());geoButton.setTextColor(geo.enabled&&geo.snapshot()!=null?LIME:MUTED);geoButton.setContentDescription(getString(R.string.ui_capture_location_settings)+geo.label());}}
    void setLocationEnabled(boolean value){settings.location=value;settings.save(getSharedPreferences("signal",0));geo.setEnabled(value);engine.setLocationEnabled(value);renderGeo();}
    void requestLocationAccess(){
        android.content.SharedPreferences prefs=getSharedPreferences("signal",0);
        boolean asked=prefs.getBoolean("locationPermissionAsked",false);
        if(!geo.permitted()&&asked&&!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)&&!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
            new AlertDialog.Builder(this).setTitle(getString(R.string.ui_check_location_permission)).setMessage(getString(R.string.ui_in_app_info_permissions_location_allow_access_while)).setPositiveButton(getString(R.string.ui_app_settings),(d,w)->openAppSettings()).setNegativeButton(getString(R.string.ui_close),null).show();return;
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
        ScrollView locationScroll=new ScrollView(this);locationScroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.ui_capture_location_gps)).setView(locationScroll).setPositiveButton(getString(R.string.ui_close),null).setNeutralButton(geo.enabled?getString(R.string.ui_do_not_save_location):getString(R.string.ui_cancel),(d,w)->{if(geo.enabled)setLocationEnabled(false);}).create();
        enable.setOnClickListener(v->{dialog.dismiss();setLocationEnabled(true);if(!geo.permitted())requestLocationAccess();else if(!geo.servicesEnabled())startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));else{geo.retry();showLocation();}});
        precise.setOnClickListener(v->{dialog.dismiss();requestLocationAccess();});
        dialog.show();dialog.getWindow().setBackgroundDrawable(bg(PANEL,20,0xff394039));dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(LIME);dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(MUTED);Runnable update=new Runnable(){public void run(){if(!dialog.isShowing())return;details.setText(geo.detail());handler.postDelayed(this,1000);}};update.run();dialog.setOnDismissListener(d->handler.removeCallbacks(update));
    }
    void showSettings(){if(recording||engine.photoBusy){Toast.makeText(this,getString(R.string.ui_change_settings_after_capture_finishes),Toast.LENGTH_SHORT).show();return;}new QualityDialog(this).show();}
    void applySettings(CaptureSettings value){if(recording||engine.photoBusy){Toast.makeText(this,getString(R.string.ui_change_settings_after_capture_finishes),Toast.LENGTH_SHORT).show();return;}cancelEffectPreview();settings=new CaptureSettings(value);settings.save(getSharedPreferences("signal",0));geo.setEnabled(settings.location);if(settings.location&&!geo.permitted())requestLocationAccess();effectState=effectState.forContext(videoMode,settings.photoFormat);ready(false);engine.configure(settings,videoMode,effectState);renderEffects();renderCaptureMode();savePrefs();}
    public void configured(CameraOptions choices,CaptureSettings actual,boolean actualVideo,int w,int h,String detail){if(actualVideo!=videoMode)return;if(actual.photoFormat!=settings.photoFormat)cancelEffectPreview();cameraOptions=choices;settings=new CaptureSettings(actual);settings.location=geo.enabled;micButton.setText(videoMode&&settings.rawVideo?getString(R.string.ui_raw_silent):sound?getString(R.string.ui_audio_on):getString(R.string.ui_audio_off));micButton.setEnabled(!(videoMode&&settings.rawVideo));displayAspect=w/(float)h;int aw=previewArea.getWidth(),ah=previewArea.getHeight();measuredFps=0;zoomButton.setEnabled(!rawOriginal()&&(videoMode||settings.photoFormat==0));zoomButton.setAlpha(!rawOriginal()&&(videoMode||settings.photoFormat==0)?1:.4f);if(rawOriginal()||(!videoMode&&settings.photoFormat!=0)){zoom=1;zoomButton.setText("1×");}if(aw>0&&ah>0){int vw=Math.min(aw,(int)(ah*displayAspect));FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)viewfinder.getLayoutParams();p.width=vw;p.height=(int)(vw/displayAspect);p.gravity=Gravity.CENTER;viewfinder.setLayoutParams(p);}count.setText(videoMode?(settings.rawVideo?"RAW · ":"")+engine.videoChoice.fps+" fps / ⚙":String.format(Locale.US,"%.1f MP / ⚙",w*(double)h/1e6));EffectState valid=effectState.forContext(videoMode,settings.photoFormat);if(valid!=effectState)commitEffects(valid);else renderEffects();}
    public void fps(float value){measuredFps=value;}
    public boolean onKeyDown(int key,android.view.KeyEvent event){if(key==KeyEvent.KEYCODE_VOLUME_DOWN || key==KeyEvent.KEYCODE_VOLUME_UP){if(event.getRepeatCount()==0)shoot();return true;}return super.onKeyDown(key,event);}
    public void onBackPressed(){if(recording)engine.toggleVideo(sound&&!settings.rawVideo);else super.onBackPressed();}
    class CaptureButton extends View {Paint p=new Paint(3);CaptureButton(){super(MainActivity.this);setClickable(true);}protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)-dp(5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(recording?RED:WHITE);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);p.setColor(videoMode?RED:LIME);if(recording)c.drawRoundRect(x-dp(13),y-dp(13),x+dp(13),y+dp(13),dp(5),dp(5),p);else c.drawCircle(x,y,r-dp(6),p);if(!videoMode){p.setColor(BG);c.drawRect(x-dp(8),y-dp(2),x+dp(8),y+dp(2),p);c.drawRect(x-dp(2),y-dp(8),x+dp(2),y+dp(8),p);}}}
    class Overlay extends View{Paint p=new Paint(3);boolean flash;float focusX=-1,focusY;Overlay(){super(MainActivity.this);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(0x228C9B83);p.setStrokeWidth(1);for(int i=1;i<3;i++){c.drawLine(w*i/3,0,w*i/3,h,p);c.drawLine(0,h*i/3,w,h*i/3,p);}p.setColor(0x997FFF40);p.setStrokeWidth(dp(1));float cx=w/2,cy=h/2;c.drawLine(cx-dp(6),cy,cx+dp(6),cy,p);c.drawLine(cx,cy-dp(6),cx,cy+dp(6),p);if(focusX>=0){p.setStyle(Paint.Style.STROKE);p.setColor(LIME);c.drawRoundRect(focusX-dp(25),focusY-dp(25),focusX+dp(25),focusY+dp(25),dp(6),dp(6),p);p.setStyle(Paint.Style.FILL);}if(flash)c.drawColor(0xAAFFFFFF);}}
}
