package com.bongorian.signa1;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class DeviceChecks extends Instrumentation {
    Bundle args;MainActivity activity;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);args=arguments==null?new Bundle():arguments;start();}
    void await(String what,BooleanSupplier condition,long millis){long until=SystemClock.elapsedRealtime()+millis;while(SystemClock.elapsedRealtime()<until){if(condition.getAsBoolean())return;SystemClock.sleep(100);}throw new AssertionError("Timeout: "+what);}
    @Override public void onStart(){Bundle result=new Bundle();CaptureSettings original=null;EffectState effectsBefore=null;FaultConfig faultsBefore=null;boolean video=false;try{
        new Thread(()->{SystemClock.sleep(12000);if(activity==null){for(java.util.Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet())if(entry.getKey().getName().equals("main")||entry.getKey().getName().contains("Signal")||entry.getKey().getName().contains("Instr"))android.util.Log.i("SignalCheck",entry.getKey().getName()+" "+java.util.Arrays.toString(entry.getValue()));}},"CheckWatch").start();
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        try(ParcelFileDescriptor launched=getUiAutomation().executeShellCommand("am start -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}
        activity=(MainActivity)monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);if(activity==null)throw new AssertionError("Activity start timeout");
        runOnMainSync(()->{android.widget.TextView cover=new android.widget.TextView(activity);cover.setText("撮影テスト中…");cover.setTextColor(0xffc7ff4a);cover.setTextSize(16);cover.setGravity(android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);cover.setPadding(0,activity.dp(35),0,0);cover.setClickable(true);cover.setTag("deviceCheckOverlay");((android.widget.FrameLayout)activity.getWindow().getDecorView()).addView(cover,new android.widget.FrameLayout.LayoutParams(-1,-1));});
        await("camera ready",()->activity.engine.frameSeen&&activity.cameraOptions!=null,20000);
        original=new CaptureSettings(activity.settings);effectsBefore=activity.effectState;faultsBefore=activity.faultConfig;video=activity.videoMode;
        if(args.getString("liveFault","false").equals("true"))runOnMainSync(()->activity.applyFaultConfig(new FaultConfig(true,true,true,true,true,true,.5f,50)));
        String action=args.getString("action","photo");
        if(action.equals("screenshots")){checkStoreScreenshots();result.putString("result","PASS captured current Japanese and English UI screenshots");}
        else if(action.equals("capture-contract")){checkCaptureContract();result.putString("result","PASS displayed timestamp pin, later camera frames, JPEG pixel equality and snapshot metadata");}
        else if(action.equals("language")){checkLanguage();result.putString("result","PASS Japanese, English, Chinese, system default, locale recreation, camera recovery and retained settings/effects/LIVE/count");}
        else if(action.equals("raw-video-caps")){checkRawCaps(result);}
        else if(action.equals("raw-video")){checkRawVideo(result);}
        else if(action.equals("live-selection")){checkLiveSelection();result.putString("result","PASS stable selected route and LIVE state UI");}
        else if(action.equals("return")){checkPreviewReturn();result.putString("result","PASS three external preview round trips, effect/config preservation, camera recovery and capture after return");}
        else if(action.equals("faults")){checkFaults();result.putString("result","PASS LIVE FAULT GPU snapshots, recoverable bypass, chain invariance, microphone handoff prerequisites and foreground cleanup");}
        else if(action.equals("geo")){checkGeo();result.putString("result","PASS location checks for the current permission/service state");}
        else if(action.equals("compatibility")){checkCompatibility();result.putString("result","PASS camera catalogs, low-resolution selection, JPEG without encoder, session recovery, front/back startup");}
        else if(action.equals("effects")){checkGpu();checkNewEffects();result.putString("result","PASS all 13 fault shaders, every named control, snapshot replay, bypass and causal composition");}
        else if(action.equals("metadata")){checkMetadata();checkGpu();result.putString("result","PASS JPEG EXIF/GPS, RAW invariants, GPU chain composition/orientation/zero strength");}
        else if(action.equals("state")){checkState();checkGpu();result.putString("result","PASS state commit/cancel/cleanup and fault GPU behavior");}
        else{
            CaptureSettings chosen=new CaptureSettings(original);chosen.rawVideo=false;chosen.location=args.getString("gps","false").equals("true");chosen.jpegQuality=Integer.parseInt(args.getString("quality","100"));chosen.videoQuality=Integer.parseInt(args.getString("bitrate","3"));chosen.codec=args.getString("codec",original.codec);chosen.videoKey=args.getString("videoKey","");chosen.photoSize=args.getString("photoSize","auto");chosen.photoFormat=action.equals("raw")?2:action.equals("raw-original")?1:0;
            boolean recording=action.equals("video")||action.equals("segment");boolean requestedSound=!args.getString("sound","true").equals("false");int preset=Integer.parseInt(args.getString("effect",Integer.toString(chosen.photoFormat==2?Effects.ROW_ERROR:Effects.CLEAN)));int power=Integer.parseInt(args.getString("power","70"));int previous=activity.engine.generation;
            runOnMainSync(()->{activity.videoMode=recording;activity.applySettings(chosen);int mask=Integer.parseInt(args.getString("chainMask","0"));EffectState selected=activity.effectState.single(preset);if(mask!=0)selected=selected.chain(mask);activity.commitEffects(selected.amount(power/100f));});
            await("configured",()->activity.engine.generation>previous&&activity.engine.frameSeen&&activity.ready,20000);SystemClock.sleep(1000);
            if(recording&&!chosen.videoKey.isEmpty()&&!activity.engine.videoChoice.key().equals(chosen.videoKey))throw new AssertionError("Requested video mode not selected: "+activity.engine.videoChoice.key());
            if(chosen.location)await("GPS fix",()->activity.geo.snapshot()!=null,15000);
            Uri before=activity.latest;
            if(recording){if(action.equals("segment"))activity.engine.segmentBytes=8_000_000L;activity.engine.toggleVideo(requestedSound);await("recording",()->activity.engine.recording,15000);if(args.getString("liveFault","false").equals("true")){await("audio ownership",()->activity.engine.recorderAudio==requestedSound&&(requestedSound?activity.engine.faultInputs.microphone==null:activity.engine.faultInputs.microphone!=null),5000);}SystemClock.sleep(Integer.parseInt(args.getString("seconds","6"))*1000L);activity.engine.toggleVideo(requestedSound);await("stop",()->!activity.engine.recording&&activity.engine.recorder==null,20000);}
            else{activity.engine.photo();}
            await("saved",()->activity.latest!=null&&!activity.latest.equals(before)&&!activity.engine.photoBusy,40000);SystemClock.sleep(600);
            if(chosen.photoFormat==0||recording)await("saved thumbnail",()->activity.galleryButton.hasThumbnail,10000);
            Uri saved=activity.latest;result.putString("uri",saved.toString());result.putString("mime",getTargetContext().getContentResolver().getType(saved));if(chosen.location&&!recording){try(InputStream in=getTargetContext().getContentResolver().openInputStream(saved)){if(!new ExifInterface(in).getLatLong(new float[2]))throw new AssertionError("Saved JPEG is missing GPS");}}result.putString("result","PASS saved "+action+" / measured "+activity.measuredFps+" fps");
            try(Cursor cursor=getTargetContext().getContentResolver().query(saved,new String[]{MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.WIDTH,MediaStore.MediaColumns.HEIGHT},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){String name=cursor.getString(0);result.putString("name",name);result.putString("dimensions",cursor.getInt(1)+"x"+cursor.getInt(2));File dir=new File(getTargetContext().getFilesDir(),"verification");dir.mkdirs();try(InputStream in=getTargetContext().getContentResolver().openInputStream(saved);OutputStream out=new FileOutputStream(new File(dir,name))){GlitchEngine.copy(in,out);}}}
        }

    }catch(Throwable error){result.putString("failure",android.util.Log.getStackTraceString(error));}
    finally{if(activity!=null&&original!=null){CaptureSettings restore=original;EffectState restoreEffects=effectsBefore;boolean restoreVideo=video;FaultConfig restoreFaults=faultsBefore;runOnMainSync(()->{activity.applyFaultConfig(restoreFaults);activity.videoMode=restoreVideo;boolean restoreLocation=restore.location;restore.location=false;activity.applySettings(restore);activity.setLocationEnabled(restoreLocation);activity.commitEffects(restoreEffects);android.view.View cover=activity.getWindow().getDecorView().findViewWithTag("deviceCheckOverlay");if(cover!=null)((android.view.ViewGroup)cover.getParent()).removeView(cover);});getTargetContext().getSharedPreferences("signal",0).edit().commit();}finish(result.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,result);}
    }
    void checkStoreScreenshots()throws Exception{
        String locale=AppLanguage.current();
        try{
            runOnMainSync(()->{
                android.view.View cover=activity.getWindow().getDecorView().findViewWithTag("deviceCheckOverlay");if(cover!=null)((android.view.ViewGroup)cover.getParent()).removeView(cover);
                CaptureSettings settings=new CaptureSettings(activity.settings);settings.rawVideo=false;settings.photoFormat=0;settings.photoSize="auto";settings.location=false;
                activity.videoMode=false;activity.applySettings(settings);activity.applyFaultConfig(FaultConfig.defaults());activity.commitEffects(EffectState.defaults());
            });
            await("screenshot camera",()->activity.ready&&activity.engine.frameSeen,20000);
            int before=activity.captureCount;runOnMainSync(()->activity.shoot());await("screenshot thumbnail",()->activity.captureCount>before&&activity.galleryButton.hasThumbnail,30000);
            for(String language:new String[]{"ja","en"}){
                changeLanguage(language);
                runOnMainSync(()->activity.commitEffects(EffectState.defaults()));SystemClock.sleep(2000);languageScreenshot("store-"+language+"-01-camera");
                runOnMainSync(()->activity.commitEffects(EffectState.defaults().single(Effects.ROW_ERROR).amount(.8f)));SystemClock.sleep(2000);languageScreenshot("store-"+language+"-02-row-shift");
                runOnMainSync(()->activity.commitEffects(EffectState.defaults().chain((1<<Effects.ROW_ERROR)|(1<<Effects.CHROMA_ERROR)|(1<<Effects.VHS)).amount(.65f)));SystemClock.sleep(2000);languageScreenshot("store-"+language+"-03-chain");
                runOnMainSync(()->new EffectDialog(activity,true).show());SystemClock.sleep(750);languageScreenshot("store-"+language+"-04-adjust");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                runOnMainSync(()->AboutDialog.showLicenses(activity));SystemClock.sleep(500);languageScreenshot("store-"+language+"-licenses");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            }
        }finally{changeLanguage(locale);}
    }
    void changeLanguage(String tags)throws Exception{
        if(AppLanguage.current().equals(tags))return;
        MainActivity before=activity;ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        runOnMainSync(()->AppLanguage.select(tags));
        MainActivity next=(MainActivity)monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);
        if(next==null||next==before)throw new AssertionError("Locale did not recreate activity: "+tags);
        activity=next;await("localized camera ready",()->activity.resumed&&activity.ready&&activity.engine.frameSeen,25000);
        if(before.engine.attached)throw new AssertionError("Old camera remains attached");
    }
    void languageScreenshot(String name)throws Exception{
        File dir=new File(getTargetContext().getFilesDir(),"verification");dir.mkdirs();Bitmap screen=getUiAutomation().takeScreenshot();
        try(OutputStream out=new FileOutputStream(new File(dir,"language-"+name+".png"))){screen.compress(Bitmap.CompressFormat.PNG,100,out);}finally{screen.recycle();}
    }
    void checkLanguage()throws Exception{
        String originalLocale=AppLanguage.current();int originalCount=activity.captureCount;
        try{
            runOnMainSync(()->{activity.commitEffects(EffectState.defaults().single(Effects.VHS).amount(.63f));activity.applyFaultConfig(FaultConfig.defaults().enabled(true));activity.captureCount=123;});
            String encoded=activity.effectState.encode();int quality=activity.settings.jpegQuality;
            String[] tags={"ja","en","zh",""},titles={"言語","App language","应用语言"},policies={"プライバシーポリシー","Privacy Policy","隐私政策"};
            for(int n=0;n<tags.length;n++){
                changeLanguage(tags[n]);
                if(!encoded.equals(activity.effectState.encode())||activity.settings.jpegQuality!=quality||!activity.faultConfig.enabled||activity.captureCount!=123)throw new AssertionError("Locale changed session state");
                if(n<3){if(!activity.getString(R.string.language_title).contains(titles[n]))throw new AssertionError("Wrong translated title: "+activity.getString(R.string.language_title));
                    try(InputStream in=activity.getResources().openRawResource(R.raw.privacy_policy)){if(!new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains(policies[n]))throw new AssertionError("Wrong policy locale");}
                }else{String expected=Build.VERSION.SDK_INT>=33?activity.getSystemService(android.app.LocaleManager.class).getSystemLocales().get(0).getLanguage():android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();if(!activity.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(expected))throw new AssertionError("System language not followed: expected "+expected+" actual "+activity.getResources().getConfiguration().getLocales());}
                String name=tags[n].isEmpty()?"system":tags[n];languageScreenshot(name+"-main");
                runOnMainSync(()->new QualityDialog(activity).show());SystemClock.sleep(500);languageScreenshot(name+"-settings");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                runOnMainSync(()->FaultDialog.show(activity));SystemClock.sleep(500);languageScreenshot(name+"-live");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            }
        }finally{changeLanguage(originalLocale);runOnMainSync(()->activity.captureCount=originalCount);}
    }
    void checkRawCaps(Bundle result)throws Exception{
        android.hardware.camera2.CameraManager manager=(android.hardware.camera2.CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);StringBuilder caps=new StringBuilder();
        for(String id:manager.getCameraIdList()){CameraOptions options=new CameraOptions(id,manager.getCameraCharacteristics(id),4096);caps.append(id).append(": ");for(CameraOptions.RawVideo raw:options.rawVideos)caps.append(raw.label(activity)).append("; ");if(options.rawVideos.isEmpty())caps.append(options.rawVideoReason(activity));caps.append('\n');}
        result.putString("result","PASS queried RAW capture capabilities");result.putString("rawVideo",caps.toString());
    }
    void checkLiveSelection()throws Exception{
        EffectState selected=EffectState.defaults().chain((1<<Effects.VHS)|(1<<Effects.CHROMA_ERROR));
        runOnMainSync(()->{activity.commitEffects(selected);activity.applyFaultConfig(FaultConfig.defaults().enabled(true));});
        await("live UI frame",()->activity.shownLiveFrame!=null&&Arrays.equals(activity.shownLiveFrame.ids(),selected.ids()),5000);
        SystemClock.sleep(1800);
        runOnMainSync(()->{if(!Arrays.equals(activity.shownLiveFrame.ids(),selected.ids()))throw new AssertionError("Route changed during LIVE");if(!activity.liveChainStatus.getText().toString().contains(Effects.chainName(selected.ids())))throw new AssertionError("Actual route is not displayed");});
    }
    void checkRawVideo(Bundle result)throws Exception{
        if(!activity.cameraOptions.rawVideoAvailable()){result.putString("result","UNAVAILABLE: "+activity.cameraOptions.rawVideoReason(activity));return;}
        CaptureSettings raw=new CaptureSettings(activity.settings);raw.location=false;raw.rawVideo=true;raw.rawVideoFps=2;raw.rawVideoSize=activity.cameraOptions.rawVideos.get(0).size.toString();int generation=activity.engine.generation;
        runOnMainSync(()->{activity.videoMode=true;activity.applySettings(raw);});
        await("RAW session validation",()->activity.engine.generation>generation&&activity.engine.frameSeen&&(activity.engine.rawFrameSeen||!activity.settings.rawVideo),20000);
        if(!activity.settings.rawVideo){result.putString("result","UNAVAILABLE verified: "+activity.cameraOptions.rawVideoReason(activity));return;}
        await("RAW ready",()->activity.ready,5000);int saved=activity.captureCount;activity.engine.toggleVideo(false);await("RAW recording",()->activity.engine.rawRecorder!=null&&activity.engine.recording,5000);
        RawVideoRecorder writer=activity.engine.rawRecorder;boolean background=args.getString("background","false").equals("true");int minimumFrames=background?1:2;await("RAW frames written",()->writer.written>=minimumFrames||writer.failed,20000);if(writer.failed)throw new AssertionError("DNG writer failed");
        if(background){try(ParcelFileDescriptor home=getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME")){}await("RAW pause",()->!activity.resumed&&!activity.engine.attached,5000);}else activity.engine.toggleVideo(false);
        await("RAW ZIP saved",()->!activity.engine.photoBusy&&!activity.engine.recording&&activity.captureCount>saved,20000);
        Uri uri=activity.latest;if(!"application/zip".equals(getTargetContext().getContentResolver().getType(uri)))throw new AssertionError("RAW recording was not a ZIP");
        int frames=0;boolean manifest=false,timestamps=false;try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(getTargetContext().getContentResolver().openInputStream(uri))){java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] block=new byte[65536];int n;while((n=zip.read(block))!=-1)bytes.write(block,0,n);
            if(entry.getName().endsWith(".dng")){frames++;ExifInterface exif=new ExifInterface(new ByteArrayInputStream(bytes.toByteArray()));if(exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,0)!=writer.size.getWidth()||exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,0)!=writer.size.getHeight())throw new AssertionError("DNG dimensions differ from sensor stream");}
            if(entry.getName().equals("manifest.json")){org.json.JSONObject info=new org.json.JSONObject(bytes.toString("UTF-8"));manifest=!info.getBoolean("effects_applied")&&!info.getBoolean("audio")&&info.getInt("frames")>=minimumFrames;}
            if(entry.getName().equals("timestamps.csv"))timestamps=bytes.toString("UTF-8").split("\n").length>=minimumFrames+1;zip.closeEntry();
        }}
        if(frames<minimumFrames||!manifest||!timestamps)throw new AssertionError("Incomplete RAW sequence archive");result.putString("result","PASS RAW session, original DNG sequence, matched metadata, timestamps, manifest and stop/finalize");result.putInt("frames",frames);result.putString("uri",uri.toString());
        if(background){try(ParcelFileDescriptor resume=getUiAutomation().executeShellCommand("am start -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}await("RAW resume validation",()->activity.resumed&&activity.ready&&activity.engine.rawFrameSeen,20000);result.putString("lifecycle","PASS background stop/finalize and resumed RAW capture validation");}
    }
    void checkPreviewReturn() throws Exception {
        if(activity.latest==null)throw new AssertionError("Capture a photo before return test");
        runOnMainSync(()->{activity.commitEffects(EffectState.defaults().chain((1<<Effects.VHS)|(1<<Effects.CHROMA_ERROR)).amount(.7f));activity.applyFaultConfig(FaultConfig.defaults().enabled(true));});
        String encoded=activity.effectState.encode();Uri saved=activity.latest;
        for(int n=0;n<3;n++){
            runOnMainSync(()->activity.openGallery());await("external preview pause",()->!activity.resumed,10000);
            await("camera closed outside app",()->!activity.engine.attached,10000);SystemClock.sleep(700);
            try(ParcelFileDescriptor back=getUiAutomation().executeShellCommand("input keyevent KEYCODE_BACK")){}
            await("camera returns",()->activity.resumed&&activity.ready&&activity.engine.frameSeen,20000);
            if(!encoded.equals(activity.effectState.encode())||!saved.equals(activity.latest)||!activity.faultConfig.enabled)throw new AssertionError("Preview round trip changed manual state");
            long timestamp=activity.engine.lastFrameNs;await("fresh resumed frames",()->activity.engine.lastFrameNs>timestamp,5000);
            if(activity.engine.faultInputs.microphone!=null||activity.engine.faultInputs.active)throw new AssertionError("Internal mode acquired device inputs");
        }
        int count=activity.captureCount;runOnMainSync(()->activity.shoot());await("capture after external preview",()->activity.captureCount>count&&!activity.engine.photoBusy,30000);
        if(FaultPreferences.load(getTargetContext().getSharedPreferences("signal",0)).enabled)throw new AssertionError("Cold startup must be OFF");
    }
    void checkFaults()throws Exception{
        FaultModel model=new FaultModel();FaultConfig config=new FaultConfig(true,true,true,true,true,true,.5f,50);
        FaultModel.Inputs inputs=new FaultModel.Inputs();inputs.motionAvailable=true;inputs.ax=15;inputs.jitter=1;
        model.advance(1,inputs,config);model.advance(1.04,inputs,config);
        EffectState selected=EffectState.defaults().single(Effects.VHS).amount(.8f);
        EffectState.Frame live=model.apply(selected.snapshot(true,0),config);
        byte[] jpeg=fixture();Bitmap first=PhotoRenderer.render(getTargetContext(),jpeg,false,live),same=PhotoRenderer.render(getTargetContext(),jpeg,false,live);
        if(!first.sameAs(same))throw new AssertionError("Snapshot replay changed pixels");first.recycle();same.recycle();
        String before=activity.effectState.encode();
        runOnMainSync(()->activity.applyFaultConfig(config));
        await("fault microphone",()->activity.engine.faultInputs.microphone!=null,5000);
        SystemClock.sleep(500);
        if(!before.equals(activity.effectState.encode()))throw new AssertionError("Sensors changed effect selection");
        try(ParcelFileDescriptor home=getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME")){}
        await("background input cleanup",()->!activity.engine.faultInputs.active&&activity.engine.faultInputs.microphone==null,5000);
        try(ParcelFileDescriptor resume=getUiAutomation().executeShellCommand("am start -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}
        await("foreground input restart",()->activity.engine.faultInputs.active&&activity.engine.faultInputs.microphone!=null&&activity.engine.frameSeen,15000);
        runOnMainSync(()->activity.applyFaultConfig(config.enabled(false)));
        await("fault input cleanup",()->!activity.engine.faultInputs.active&&activity.engine.faultInputs.microphone==null,5000);
    }
    void checkGeo()throws Exception{
        boolean allowed=activity.geo.permitted();
        runOnMainSync(()->activity.setLocationEnabled(true));
        if(!allowed){if(activity.geo.snapshot()!=null||!activity.geo.label().equals(activity.getString(R.string.ui_gps_denied)))throw new AssertionError("Denied permission shown as waiting/fixed");return;}
        if(!activity.geo.servicesEnabled()){if(activity.geo.snapshot()!=null||!activity.geo.label().equals(activity.getString(R.string.ui_gps_device_off)))throw new AssertionError("Disabled location services shown as usable");return;}
        runOnMainSync(()->{
            Location stale=new Location("gps");stale.setLatitude(35);stale.setLongitude(139);stale.setAccuracy(5);stale.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()-121_000_000_000L);
            if(GeoTags.fresh(stale,SystemClock.elapsedRealtimeNanos()))throw new AssertionError("Expired location accepted");
            Location fresh=new Location(stale);fresh.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());activity.geo.onLocationChanged(fresh);
            if(activity.geo.snapshot()==null)throw new AssertionError("Fresh location missing");
            if(!activity.geo.precise()){activity.geo.permissionLevel=2;if(activity.geo.snapshot()!=null)throw new AssertionError("Precise cache survived approximate permission downgrade");activity.geo.retry();activity.geo.onLocationChanged(fresh);}
            activity.geo.start();if(activity.geo.snapshot()==null)throw new AssertionError("Resume erased usable fix");
            activity.geo.retry();if(activity.geo.snapshot()==null)throw new AssertionError("Retry erased usable fix");
            Location copy=activity.geo.snapshot();copy.setLatitude(0);if(activity.geo.snapshot().getLatitude()==0)throw new AssertionError("Snapshot is mutable shared state");
            if(activity.geo.precise()?activity.geo.label().contains(activity.getString(R.string.ui_gps_approx)):!activity.geo.label().contains(activity.getString(R.string.ui_gps_approx)))throw new AssertionError("Precision permission not shown");
            activity.setLocationEnabled(false);if(activity.geo.snapshot()!=null||activity.geo.active)throw new AssertionError("OFF retained location or updates");
        });
    }
    void checkCompatibility()throws Exception{
        android.hardware.camera2.CameraManager manager=(android.hardware.camera2.CameraManager)getTargetContext().getSystemService(Context.CAMERA_SERVICE);
        for(String id:manager.getCameraIdList()){
            CameraOptions catalog=new CameraOptions(id,manager.getCameraCharacteristics(id),activity.engine.maxTexture);
            CaptureSettings defaults=new CaptureSettings();
            if(catalog.photo(defaults)==null)throw new AssertionError("No default JPEG: "+id);
            CameraOptions.Photo tiny=new CameraOptions.Photo(new android.util.Size(640,480),false);
            catalog.photos.clear();catalog.photos.add(tiny);catalog.videos.clear();catalog.encoders.clear();
            if(catalog.photo(defaults)!=tiny||catalog.video(defaults)!=null)throw new AssertionError("Low-resolution photo must not require video");
            if(catalog.previewFor(tiny)==null)throw new AssertionError("No preview for VGA photo");
        }
        // Exercise the actual photo startup path when no video encoder is available.
        CameraOptions live=activity.cameraOptions;
        java.util.List<CameraOptions.Video> videos=new java.util.ArrayList<>(live.videos);
        int before=activity.engine.generation;
        activity.engine.gl.post(()->{live.videos.clear();activity.engine.restart();});
        try{await("photo preview without encoder",()->activity.engine.generation>before&&activity.engine.frameSeen&&activity.ready,20000);}
        finally{activity.engine.gl.post(()->{live.videos.addAll(videos);});}
        int recovery=activity.engine.generation;
        activity.engine.gl.post(()->activity.engine.recoverSession());
        await("low-resolution session recovery",()->activity.engine.generation>recovery&&activity.engine.frameSeen&&activity.ready,20000);
        if(activity.engine.photoChoice.maximumPixelMode||activity.settings.photoFormat!=0)throw new AssertionError("Recovery must use normal JPEG");
        for(int i=0;i<2;i++){
            int previous=activity.engine.generation;activity.engine.switchCamera();
            await("camera switch",()->activity.engine.generation>previous&&activity.engine.frameSeen&&activity.ready,20000);
        }
    }
    void checkState()throws Exception{
        CaptureSettings jpeg=new CaptureSettings(activity.settings);jpeg.photoFormat=0;int previous=activity.engine.generation;
        runOnMainSync(()->{activity.videoMode=false;activity.applySettings(jpeg);});
        await("JPEG context",()->activity.engine.generation>previous&&activity.ready,20000);
        runOnMainSync(()->{
            activity.commitEffects(EffectState.defaults().chain((1<<Effects.PIXEL_DAMAGE)|(1<<Effects.ROW_ERROR)).amount(.7f));EffectState base=activity.effectState;
            String persisted=activity.getSharedPreferences("signal",0).getString(EffectStateStore.KEY,"");
            MainActivity.EffectPreview edit=activity.beginEffectPreview();EffectState draft=base.single(Effects.ROW_ERROR).amount(.2f);activity.previewEffectEdit(edit,draft);
            if(activity.effectState!=base||!persisted.equals(activity.getSharedPreferences("signal",0).getString(EffectStateStore.KEY,"")))throw new AssertionError("Preview leaked to committed state");
            activity.finishEffectEdit(edit,draft,false);if(activity.effectState!=base)throw new AssertionError("Cancel did not restore base");
            edit=activity.beginEffectPreview();activity.previewEffectEdit(edit,draft);activity.finishEffectEdit(edit,draft,true);
            if(activity.effectState.chained||activity.effectState.mask!=(1<<Effects.ROW_ERROR)||activity.effectState.amount!=.2f)throw new AssertionError("Apply was not atomic");
            edit=activity.beginEffectPreview();activity.chooseEffect(Effects.CLEAN);activity.finishEffectEdit(edit,base,true);
            if(activity.effectState.mask!=0||activity.effectState.chained)throw new AssertionError("Stale editor resurrected chain");
            activity.commitEffects(base.chain((1<<Effects.PIXEL_DAMAGE)|(1<<Effects.STREAM_ERROR)));if(activity.effectState.mask!=((1<<Effects.PIXEL_DAMAGE)|(1<<Effects.STREAM_ERROR)))throw new AssertionError("Photo lost stream model");
            String before=activity.effectState.encode();activity.renderEffects();if(!before.equals(activity.effectState.encode()))throw new AssertionError("Redraw mutated effect state");
        });
    }
    void checkCaptureContract()throws Exception{
        CaptureSettings settings=new CaptureSettings(activity.settings);settings.photoFormat=0;settings.rawVideo=false;settings.photoSize="auto";settings.jpegQuality=100;
        int generation=activity.engine.generation;
        runOnMainSync(()->{activity.videoMode=false;activity.applySettings(settings);activity.commitEffects(EffectState.defaults().chain((1<<Effects.ROW_ERROR)|(1<<Effects.VHS)|(1<<Effects.CRT)).amount(.8f));});
        await("live signal",()->activity.engine.generation>generation&&activity.ready&&activity.engine.presentedFrames.acknowledged()>0,20000);
        SystemClock.sleep(500);
        FrameHistory.Lease<SignalBuffer> displayed=activity.engine.presentedFrames.reserve();if(displayed==null)throw new AssertionError("No acknowledged image");
        long capturedCameraNs=displayed.value.frame.cameraNs;
        Bitmap[] reference=new Bitmap[1];Throwable[] problem=new Throwable[1];java.util.concurrent.CountDownLatch read=new java.util.concurrent.CountDownLatch(1);
        activity.engine.gl.post(()->{try{activity.engine.current(activity.engine.window);reference[0]=displayed.value.read();}catch(Throwable failure){problem[0]=failure;}finally{read.countDown();}});
        if(!read.await(5,java.util.concurrent.TimeUnit.SECONDS)||problem[0]!=null)throw new AssertionError("Reference readback",problem[0]);
        // Simulate delayed GL shutter handling while new camera states arrive. The old image is pinned.
        await("later camera signal",()->activity.engine.lastFrameNs>displayed.value.frame.cameraNs+150_000_000L,5000);
        Uri before=activity.latest;activity.engine.photo(displayed.timestamp);activity.engine.presentedFrames.release(displayed);
        runOnMainSync(()->activity.commitEffects(EffectState.defaults().single(Effects.COLOR_MAP)));
        await("latched JPEG",()->activity.latest!=null&&!activity.latest.equals(before)&&!activity.engine.photoBusy,30000);
        Bitmap actual;try(InputStream in=getTargetContext().getContentResolver().openInputStream(activity.latest)){actual=BitmapFactory.decodeStream(in);}
        ByteArrayOutputStream encoded=new ByteArrayOutputStream();reference[0].compress(Bitmap.CompressFormat.JPEG,100,encoded);Bitmap expected=BitmapFactory.decodeByteArray(encoded.toByteArray(),0,encoded.size());reference[0].recycle();
        if(actual==null||!actual.sameAs(expected))throw new AssertionError("Saved JPEG is not the pinned displayed signal");actual.recycle();expected.recycle();
        try(InputStream in=getTargetContext().getContentResolver().openInputStream(activity.latest)){String description=new ExifInterface(in).getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);if(description==null||!description.contains("cameraNs="+capturedCameraNs)||!description.contains("VHS"))throw new AssertionError("Capture lost timestamp/state metadata");}
    }
    void checkNewEffects()throws Exception{
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("effect-schema-check",0);
        prefs.edit().clear().putString("effect_state_v2","2|0|256|0.73").putInt("effect",8).commit();
        if(EffectStateStore.load(prefs).mask!=0)throw new AssertionError("Old IDs reinterpreted");
        android.content.SharedPreferences.Editor editor=prefs.edit();EffectStateStore.write(editor,EffectState.defaults().single(Effects.DEMOSAIC_ERROR));editor.commit();
        if(EffectStateStore.load(prefs).selected()!=Effects.DEMOSAIC_ERROR||prefs.contains("effect_state_v2"))throw new AssertionError("New schema migration");prefs.edit().clear().commit();
    }
    byte[] fixture(){Bitmap input=Bitmap.createBitmap(192,256,Bitmap.Config.ARGB_8888);for(int y=0;y<256;y++)for(int x=0;x<192;x++)input.setPixel(x,y,Color.rgb((x*13+y*3)%256,y,((x/7+y/9)%2)*255));ByteArrayOutputStream bytes=new ByteArrayOutputStream();input.compress(Bitmap.CompressFormat.JPEG,100,bytes);input.recycle();return bytes.toByteArray();}
    EffectState.Frame evaluated(EffectState state,double time){FaultModel model=new FaultModel(5);FaultModel.Inputs input=new FaultModel.Inputs();model.advance(0,input,FaultConfig.defaults());input.sensorNs=(long)(time*1e9);model.advance(time,input,FaultConfig.defaults());return model.apply(state.snapshot(true,0),FaultConfig.defaults());}
    void checkGpu()throws Exception{
        byte[] jpeg=fixture();Bitmap clean=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(EffectState.defaults(),0));
        Bitmap zero=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(EffectState.defaults().chain(-1).amount(0),4));if(!zero.sameAs(clean))throw new AssertionError("LEVEL zero bypass");zero.recycle();
        if(Color.green(clean.getPixel(80,10))>=Color.green(clean.getPixel(80,240)))throw new AssertionError("GPU orientation");
        File dir=new File(getTargetContext().getFilesDir(),"verification");dir.mkdirs();
        for(int id:Effects.ORDER){if(id==0)continue;EffectState selected=EffectState.defaults().single(id).amount(1);EffectState.Frame frame=null;
            for(int i=1;i<200;i++){frame=evaluated(selected,i*.05);if(id!=Effects.STREAM_ERROR||frame.nodes.get(0).event.envelope>.5)break;}
            Bitmap image=PhotoRenderer.render(getTargetContext(),jpeg,false,frame),again=PhotoRenderer.render(getTargetContext(),jpeg,false,frame);
            if(!image.sameAs(again))throw new AssertionError("Snapshot replay: "+Effects.name(id));
            if(image.sameAs(clean))throw new AssertionError("Fault has no visible mechanism: "+Effects.name(id));
            try(OutputStream out=new FileOutputStream(new File(dir,"fault-"+id+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();again.recycle();
            double incidentTime=.08;
            for(int step=1;step<=1200;step++){FaultNode probe=evaluated(selected,step*.05).nodes.get(0);if(probe.event.envelope>.5&&probe.event.position>.03){incidentTime=step*.05;break;}if(probe.event.serial<0)break;}
            for(Effects.Control control:Effects.CONTROLS[id]){
                boolean responds=false;
                for(double time:new double[]{incidentTime,.08,1.18,2.78,5.48,8.18}){
                    EffectParameters p=selected.parameters();
                    Bitmap low=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(selected.edit(false,selected.mask,p.with(id,control.key,0)),time));
                    Bitmap high=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(selected.edit(false,selected.mask,p.with(id,control.key,1)),time));
                    responds=!low.sameAs(high);low.recycle();high.recycle();if(responds)break;
                }
                if(!responds)throw new AssertionError("Control has no visible effect: "+Effects.name(id)+" / "+control.key);
            }
        }
        EffectState chain=EffectState.defaults().chain((1<<Effects.EXPOSURE)|(1<<Effects.CHROMA_ERROR)|(1<<Effects.CRT));
        Bitmap composed=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(chain,3)),last=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(chain.single(Effects.CRT),3));
        if(composed.sameAs(last)||composed.sameAs(clean))throw new AssertionError("Causal chain composition");composed.recycle();last.recycle();clean.recycle();
    }
    void checkMetadata()throws Exception{
        Bitmap b=Bitmap.createBitmap(64,96,Bitmap.Config.ARGB_8888);b.eraseColor(Color.GREEN);File file=new File(getTargetContext().getCacheDir(),"metadata-check.jpg");try(OutputStream out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.JPEG,95,out);}b.recycle();Location fake=new Location("gps");fake.setLatitude(-12.345678);fake.setLongitude(123.456789);fake.setAltitude(42.5);fake.setAccuracy(3);fake.setTime(System.currentTimeMillis());fake.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());PhotoMetadata.write(file,null,null,System.currentTimeMillis(),64,96,fake,"SIGNAL METADATA CHECK");ExifInterface exif=new ExifInterface(file);float[] latlong=new float[2];if(!exif.getLatLong(latlong)||Math.abs(latlong[0]+12.345678)>.00001||Math.abs(latlong[1]-123.456789)>.00002)throw new AssertionError("GPS roundtrip");if(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)==null||exif.getAttribute(ExifInterface.TAG_SOFTWARE)==null||exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,0)!=1)throw new AssertionError("EXIF fields");file.delete();
        int w=128,h=96;byte[] samples=new byte[w*h*2];for(int i=0;i<w*h;i++)RawGlitch.write(samples,i,256+i%3500);
        EffectState.Frame frame=evaluated(EffectState.defaults().chain(-1),.1);
        byte[] first=RawGlitch.chain(samples,w,h,4095,256,frame),same=RawGlitch.chain(samples,w,h,4095,256,frame);
        if(!Arrays.equals(first,same))throw new AssertionError("RAW snapshot replay");for(int i=0;i<w*h;i++)if(RawGlitch.read(first,i)>4095)throw new AssertionError("RAW bounds");
    }
}
