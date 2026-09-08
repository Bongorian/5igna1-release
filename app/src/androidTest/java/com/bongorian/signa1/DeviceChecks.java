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
        if(args.getString("action","").startsWith("tutorial"))getTargetContext().getSharedPreferences("signal",0).edit().remove(TutorialDialog.SEEN).commit();
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        try(ParcelFileDescriptor launched=getUiAutomation().executeShellCommand("am start -f 0x10008000 -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}
        activity=(MainActivity)monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);if(activity==null)throw new AssertionError("Activity start timeout");
        if(args.getString("action","").equals("tutorial-permission")){
            await("guide before permission",()->activity.tutorial!=null&&activity.tutorial.dialog.isShowing(),5000);
            if(activity.checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED)throw new AssertionError("Test requires denied camera permission");
            SystemClock.sleep(1200);languageScreenshot("tutorial-before-permission");
            if(!getTargetContext().getPackageName().contentEquals(getUiAutomation().getRootInActiveWindow().getPackageName()))throw new AssertionError("Permission covered tutorial");
            tutorialClick("tutorial-skip");await("camera permission after guide",()->{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();return root!=null&&root.getPackageName().toString().contains("permissioncontroller");},5000);
            result.putString("result","PASS tutorial readable before camera permission; permission requested only after dismissal");return;
        }
        if(args.getString("action","").equals("tutorial")){checkTutorial();result.putString("result","PASS first launch, page restoration, skip/back/completion, settings draft isolation, all locales and camera recovery");return;}
        runOnMainSync(()->{if(activity.tutorial!=null)activity.tutorial.dialog.dismiss();});
        runOnMainSync(()->{android.widget.TextView cover=new android.widget.TextView(activity);cover.setText("撮影テスト中…");cover.setTextColor(0xffc7ff4a);cover.setTextSize(16);cover.setGravity(android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);cover.setPadding(0,activity.dp(35),0,0);cover.setClickable(true);cover.setTag("deviceCheckOverlay");((android.widget.FrameLayout)activity.getWindow().getDecorView()).addView(cover,new android.widget.FrameLayout.LayoutParams(-1,-1));});
        await("camera ready",()->activity.engine.frameSeen&&activity.cameraOptions!=null,20000);
        original=new CaptureSettings(activity.settings);effectsBefore=activity.effectState;faultsBefore=activity.faultConfig;video=activity.videoMode;
        if(args.getString("liveFault","false").equals("true"))runOnMainSync(()->activity.applyFaultConfig(new FaultConfig(true,true,true,true,true,true,.5f,50)));
        String action=args.getString("action","photo");
        if(action.equals("pixel-preview")){checkPixelPreview();result.putString("result","PASS sparse pixel damage at preview and photo resolutions");}
        else if(action.equals("screenshots")){checkStoreScreenshots();result.putString("result","PASS captured current Japanese and English UI screenshots");}
        else if(action.equals("product-ui")){checkProductUi();result.putString("result","PASS anchored format menu, top utilities, live fault meters, camera interruption and stalled-preview recovery");}
        else if(action.equals("expert")){checkExpert(result);}
        else if(action.equals("load-record")){checkLoadRecording(result);}
        else if(action.equals("load")){checkAdaptiveLoad(result);}
        else if(action.equals("advanced")){checkAdvanced();result.putString("result","PASS advanced UI, exact values, cancel/apply, saved overrides, full-range GPU and RAW contracts");}
        else if(action.equals("performance")){checkPerformance();result.putString("result","PASS LIVE preview isolation, style/time controls, apply/cancel, HOLD and HIT");}
        else if(action.equals("format-ui")){checkFormatUi();result.putString("result","PASS JPG/RAW picker, dismiss without change, RAW selection, legacy original migration and custom toggle");}
        else if(action.equals("chain-format")){checkChainFormat();result.putString("result","PASS 13-fault RAW catalog, explicit JPEG switch with draft retention, cancel and format round-trip");}
        else if(action.equals("editor")){checkEditor();result.putString("result","PASS unobscured preview, draft controls, multi-selection, random chain, cancel and apply");}
        else if(action.equals("capture-contract")){checkCaptureContract();result.putString("result","PASS displayed timestamp pin, later camera frames, JPEG pixel equality and snapshot metadata");}
        else if(action.equals("language")){checkLanguage();result.putString("result","PASS Japanese, English, Chinese, system default, locale recreation, camera recovery and retained settings/effects/LIVE/count");}
        else if(action.equals("raw-video-caps")){checkRawCaps(result);}
        else if(action.equals("raw-video")){checkRawVideo(result);}
        else if(action.equals("live-selection")){checkLiveSelection();result.putString("result","PASS stable selected route and LIVE state UI");}
        else if(action.equals("mixed-preview")){checkMixedPreview();result.putString("result","PASS mixed photo/video swipes, playback/pause/seek, six video swipe dismissals, loading/error/background cleanup and fresh camera/capture recovery");}
        else if(action.equals("swipe-return")){checkSwipeReturn();result.putString("result","PASS six downward-swipe dismissals, fresh displayed frames and capture after return");}
        else if(action.equals("return")){checkPreviewReturn();result.putString("result","PASS six external preview round trips (including immediate return), effect/config preservation, camera recovery and capture after return");}
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
            if(recording&&!chosen.videoKey.isEmpty()&&!chosen.videoKey.equals("recommended")&&!activity.engine.videoChoice.key().equals(chosen.videoKey))throw new AssertionError("Requested video mode not selected: "+activity.engine.videoChoice.key());
            if(chosen.location)await("GPS fix",()->activity.geo.snapshot()!=null,15000);
            Uri before=activity.latest;
            if(recording){if(action.equals("segment"))activity.engine.segmentBytes=8_000_000L;activity.engine.toggleVideo(requestedSound);await("recording",()->activity.engine.recording,15000);if(args.getString("liveFault","false").equals("true")){await("audio ownership",()->activity.engine.recorderAudio==requestedSound&&(requestedSound?activity.engine.faultInputs.microphone==null:activity.engine.faultInputs.microphone!=null),5000);}SystemClock.sleep(Integer.parseInt(args.getString("seconds","6"))*1000L);activity.engine.toggleVideo(requestedSound);await("stop",()->!activity.engine.recording&&activity.engine.recorder==null,20000);}
            else{activity.engine.photo();}
            await("saved",()->activity.latest!=null&&!activity.latest.equals(before)&&!activity.engine.photoBusy,40000);SystemClock.sleep(600);
            if(chosen.photoFormat==0||recording)await("saved thumbnail",()->activity.galleryButton.hasThumbnail,10000);
            Uri saved=activity.latest;result.putString("uri",saved.toString());result.putString("mime",getTargetContext().getContentResolver().getType(saved));if(chosen.location&&!recording){try(InputStream in=getTargetContext().getContentResolver().openInputStream(saved)){if(!new ExifInterface(in).getLatLong(new float[2]))throw new AssertionError("Saved JPEG is missing GPS");}}result.putString("result","PASS saved "+action+" / measured "+activity.measuredFps+" fps");
            try(Cursor cursor=getTargetContext().getContentResolver().query(saved,new String[]{MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.WIDTH,MediaStore.MediaColumns.HEIGHT,MediaStore.MediaColumns.RELATIVE_PATH},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){String name=cursor.getString(0);result.putString("name",name);result.putString("dimensions",cursor.getInt(1)+"x"+cursor.getInt(2));String folder=cursor.getString(3);result.putString("folder",folder);if(!"DCIM/5igna1/".equals(folder))throw new AssertionError("Capture outside shared camera folder: "+folder);File dir=new File(getTargetContext().getFilesDir(),"verification");dir.mkdirs();try(InputStream in=getTargetContext().getContentResolver().openInputStream(saved);OutputStream out=new FileOutputStream(new File(dir,name))){GlitchEngine.copy(in,out);}}}
        }

    }catch(Throwable error){result.putString("failure",android.util.Log.getStackTraceString(error));}
    finally{if(activity!=null&&original!=null){CaptureSettings restore=original;EffectState restoreEffects=effectsBefore;boolean restoreVideo=video;FaultConfig restoreFaults=faultsBefore;runOnMainSync(()->{activity.applyFaultConfig(restoreFaults);activity.videoMode=restoreVideo;boolean restoreLocation=restore.location;restore.location=false;activity.applySettings(restore);activity.setLocationEnabled(restoreLocation);activity.commitEffects(restoreEffects);android.view.View cover=activity.getWindow().getDecorView().findViewWithTag("deviceCheckOverlay");if(cover!=null)((android.view.ViewGroup)cover.getParent()).removeView(cover);});getTargetContext().getSharedPreferences("signal",0).edit().commit();}finish(result.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,result);}
    }
    void tutorialClick(String tag){runOnMainSync(()->activity.tutorial.dialog.getWindow().getDecorView().findViewWithTag(tag).performClick());waitForIdleSync();}
    void recreateTutorialActivity()throws Exception{
        MainActivity old=activity;ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(old::recreate);
        activity=(MainActivity)monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);if(activity==null)throw new AssertionError("Recreation timeout");waitForIdleSync();
    }
    void checkTutorial()throws Exception{
        await("first-launch tutorial",()->activity.tutorial!=null&&activity.tutorial.dialog.isShowing(),5000);
        if(getTargetContext().getSharedPreferences("signal",0).getBoolean(TutorialDialog.SEEN,false))throw new AssertionError("Seen before user dismissal");
        tutorialClick("tutorial-next");tutorialClick("tutorial-next");recreateTutorialActivity();
        if(activity.tutorial==null||activity.tutorial.page!=2)throw new AssertionError("Lost tutorial page on recreation");
        tutorialClick("tutorial-back");if(activity.tutorial.page!=1)throw new AssertionError("Back page");tutorialClick("tutorial-skip");
        if(!getTargetContext().getSharedPreferences("signal",0).getBoolean(TutorialDialog.SEEN,false))throw new AssertionError("Skip not persisted");
        recreateTutorialActivity();if(activity.tutorial!=null)throw new AssertionError("Tutorial repeated after skip");
        await("camera after guide",()->activity.ready&&activity.engine.frameSeen,20000);
        String language=AppLanguage.current();
        try{for(String tag:new String[]{"ja","en","zh"}){
            changeLanguage(tag);QualityDialog[] settings={null};boolean before=activity.advancedMode;
            runOnMainSync(()->{settings[0]=new QualityDialog(activity);settings[0].show();settings[0].advanced=!before;settings[0].content.findViewWithTag("settings-tutorial").performClick();});
            for(int page=0;page<5;page++){if(activity.tutorial.page!=page)throw new AssertionError("Unexpected page");SystemClock.sleep(900);languageScreenshot("tutorial-"+tag+"-"+page);tutorialClick("tutorial-next");}
            if(activity.tutorial!=null||!settings[0].sheet.isShowing()||settings[0].advanced==before||activity.advancedMode!=before)throw new AssertionError("Tutorial changed settings draft");
            runOnMainSync(()->settings[0].sheet.dismiss());
        }}finally{changeLanguage(language);}
        runOnMainSync(()->activity.showTutorial());sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();if(activity.tutorial!=null)throw new AssertionError("System back failed");
        recreateTutorialActivity();if(activity.tutorial!=null)throw new AssertionError("Tutorial repeated after completion");await("final preview",()->activity.ready&&activity.engine.frameSeen,20000);
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
    void checkProductUi()throws Exception{
        runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=0;next.photoSize="recommended";next.expertMode=false;activity.videoMode=false;activity.applySettings(next);activity.commitEffects(EffectState.defaults().chain((1<<Effects.VHS)|(1<<Effects.PIXEL_DAMAGE)|(1<<Effects.ROW_ERROR)));activity.applyFaultConfig(FaultConfig.defaults().enabled(true));});await("product preview",()->activity.ready&&activity.shownLiveFrame!=null,20000);
        runOnMainSync(()->{int[] top=new int[2],camera=new int[2];activity.previewArea.getLocationOnScreen(camera);for(android.view.View utility:new android.view.View[]{activity.torchButton,activity.geoButton,activity.micButton}){utility.getLocationOnScreen(top);if(top[1]+utility.getHeight()>camera[1])throw new AssertionError("Utility remains below preview");}});runOnMainSync(()->{boolean sound=activity.sound;if(activity.micButton.getVisibility()!=android.view.View.VISIBLE)throw new AssertionError("Audio control hidden in photo mode");activity.micButton.performClick();if(activity.sound==sound||activity.micButton.isSelected()!=activity.sound)throw new AssertionError("Audio toggle failed");activity.micButton.performClick();if(activity.sound!=sound)throw new AssertionError("Audio restore failed");});SystemClock.sleep(500);saveUi("product-main.png");
        Dialog[] menu=new Dialog[1];runOnMainSync(()->menu[0]=activity.showFormat());waitForIdleSync();SystemClock.sleep(400);
        runOnMainSync(()->{int[] button=new int[2],popup=new int[2];activity.formatButton.getLocationOnScreen(button);menu[0].getWindow().getDecorView().getLocationOnScreen(popup);if(popup[1]<button[1]+activity.formatButton.getHeight()-activity.dp(8)||popup[1]>button[1]+activity.formatButton.getHeight()+activity.dp(44))throw new AssertionError("Format popup is not anchored: "+popup[1]+" / "+button[1]);});saveUi("product-format-menu.png");runOnMainSync(()->menu[0].dismiss());
        runOnMainSync(()->activity.liveChainStatus.performClick());waitForIdleSync();SystemClock.sleep(500);if(activity.faultStatePanel==null||activity.faultStatePanel.items.size()!=3)throw new AssertionError("Missing fault meter rows");saveUi("product-fault-state.png");runOnMainSync(()->activity.liveChainDialog.dismiss());
        String config=activity.effectState.encode();int generation=activity.engine.generation;glSync(()->activity.engine.cameraInterrupted());await("camera interruption recovery",()->activity.ready&&activity.engine.frameSeen&&activity.engine.generation>generation,15000);long stamp=activity.engine.lastFrameNs;await("recovered fresh frame",()->activity.engine.lastFrameNs>stamp,5000);
        int beforeStall=activity.engine.generation;glSync(()->{try{activity.engine.session.stopRepeating();}catch(Exception e){throw new RuntimeException(e);}});await("stalled preview reconnect",()->activity.engine.generation>beforeStall&&activity.ready&&activity.engine.frameSeen,18000);if(!config.equals(activity.effectState.encode()))throw new AssertionError("Recovery changed chain");
    }
    void swipeMedia(boolean horizontal,boolean forward)throws Exception{
        android.graphics.Rect bounds=new android.graphics.Rect();runOnMainSync(()->activity.mediaPreview.media.getGlobalVisibleRect(bounds));int x=bounds.centerX(),y=bounds.centerY();String command=horizontal?"input swipe "+(forward?bounds.right-bounds.width()/6:bounds.left+bounds.width()/6)+" "+y+" "+(forward?bounds.left+bounds.width()/6:bounds.right-bounds.width()/6)+" "+y+" 250":"input swipe "+x+" "+(bounds.top+bounds.height()/3)+" "+x+" "+(bounds.bottom-bounds.height()/8)+" 250";try(ParcelFileDescriptor gesture=getUiAutomation().executeShellCommand(command)){}
    }
    void checkMixedPreview()throws Exception{
        runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=0;next.photoSize="recommended";next.rawVideo=false;next.videoKey="recommended";next.videoQuality=1;activity.videoMode=false;activity.applySettings(next);});await("mixed photo camera",()->activity.ready,20000);
        final int first=activity.captureCount;runOnMainSync(()->activity.shoot());await("mixed photo saved",()->activity.captureCount>first&&!activity.engine.photoBusy,30000);Uri photo=activity.latest;
        runOnMainSync(()->{activity.videoMode=true;activity.applySettings(new CaptureSettings(activity.settings));});await("mixed video camera",()->activity.ready,20000);final int second=activity.captureCount;activity.engine.toggleVideo(false);await("mixed recording",()->activity.recording,10000);SystemClock.sleep(3500);activity.engine.toggleVideo(false);await("mixed video saved",()->activity.captureCount>second&&!activity.recording,20000);Uri movie=activity.latest;String effects=activity.effectState.encode();
        runOnMainSync(()->activity.openGallery());await("internal video plays",()->activity.mediaPreview!=null&&activity.mediaPreview.prepared&&activity.mediaPreview.videoFrameSeen,15000);MediaPreview viewer=activity.mediaPreview;if(activity.engine.attached||!activity.resumed)throw new AssertionError("Camera overlaps internal decoder or viewer left app");if(!viewer.items.get(viewer.index).uri.equals(movie))throw new AssertionError("Wrong initial capture");saveUi("mixed-video.png");
        runOnMainSync(()->{viewer.pausePlayback();viewer.player.seekTo(1000);});SystemClock.sleep(600);int paused=viewer.player.getCurrentPosition();SystemClock.sleep(400);if(viewer.player.isPlaying()||Math.abs(viewer.player.getCurrentPosition()-paused)>150)throw new AssertionError("Video pause failed");runOnMainSync(viewer::startPlayback);await("video time advances",()->viewer.player.getCurrentPosition()>paused+200||viewer.player.getCurrentPosition()<paused,5000);
        swipeMedia(true,true);await("photo after video swipe",()->viewer.image!=null&&viewer.image.getDrawable()!=null,15000);if(!viewer.items.get(viewer.index).uri.equals(photo)||viewer.player!=null)throw new AssertionError("Mixed sequence order or decoder cleanup");saveUi("mixed-photo.png");
        swipeMedia(true,false);await("video after photo swipe",()->viewer.prepared&&viewer.videoFrameSeen,15000);
        for(int n=0;n<6;n++){
            MediaPreview current=activity.mediaPreview;swipeMedia(false,true);await("video swipe dismiss",()->activity.mediaPreview==null&&activity.ready&&activity.engine.frameSeen,15000);if(current.player!=null||current.videoSurface!=null||!current.closed)throw new AssertionError("Video resources retained after dismiss");long token=activity.engine.presentedFrames.acknowledged();await("fresh displayed frame after video",()->activity.engine.presentedFrames.acknowledged()>token,8000);if(n<5){runOnMainSync(()->activity.openGallery());await("video reopen",()->activity.mediaPreview!=null&&activity.mediaPreview.prepared&&activity.mediaPreview.videoFrameSeen,15000);SystemClock.sleep(n%2==0?500:1500);}
        }
        runOnMainSync(()->{activity.openGallery();activity.mediaPreview.dismiss();});await("close during loading",()->activity.mediaPreview==null&&activity.ready,15000);SystemClock.sleep(700);if(activity.mediaPreview!=null||!effects.equals(activity.effectState.encode()))throw new AssertionError("Late callback reopened viewer or changed effects");saveUi("mixed-camera-return.png");
        runOnMainSync(()->new MediaPreview(activity,Uri.parse("content://media/external/video/media/9223372036854775807"),true).show());await("unavailable video handled",()->activity.mediaPreview!=null&&activity.getString(R.string.media_preview_unavailable).contentEquals(activity.mediaPreview.notice.getText()),15000);swipeMedia(false,true);await("camera after unavailable media",()->activity.mediaPreview==null&&activity.ready,15000);
        runOnMainSync(()->activity.openGallery());await("viewer before background",()->activity.mediaPreview!=null&&activity.mediaPreview.prepared,15000);MediaPreview background=activity.mediaPreview;
        try(ParcelFileDescriptor home=getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME")){}await("viewer cleanup on background",()->!activity.resumed&&activity.mediaPreview==null&&background.player==null&&!activity.engine.attached,10000);
        try(ParcelFileDescriptor resume=getUiAutomation().executeShellCommand("am start -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}await("camera after viewer background",()->activity.resumed&&activity.ready,20000);
        final int before=activity.captureCount;activity.engine.toggleVideo(false);await("record after mixed preview",()->activity.recording,10000);SystemClock.sleep(1500);activity.engine.toggleVideo(false);await("save after mixed preview",()->activity.captureCount>before&&!activity.recording,20000);
    }
    void checkSwipeReturn()throws Exception{
        boolean video=args.getString("media","photo").equals("video");
        runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=0;next.photoSize="recommended";next.rawVideo=false;if(!args.getString("quality","recommended").equals("selected")){next.videoKey="recommended";next.videoQuality=1;}activity.videoMode=video;activity.applySettings(next);activity.commitEffects(EffectState.defaults().single(Effects.CLEAN));});await("swipe test camera",()->activity.ready,20000);
        final int initialCount=activity.captureCount;
        if(video){activity.engine.toggleVideo(false);await("swipe fixture recording",()->activity.recording,10000);SystemClock.sleep(4000);activity.engine.toggleVideo(false);}else runOnMainSync(()->activity.shoot());await("swipe fixture saved",()->activity.captureCount>initialCount&&!activity.engine.photoBusy&&!activity.recording,30000);
        int w=getTargetContext().getResources().getDisplayMetrics().widthPixels,h=getTargetContext().getResources().getDisplayMetrics().heightPixels;
        try{for(int n=0;n<6;n++){
            runOnMainSync(()->activity.openGallery());await("viewer opened",()->!activity.resumed,10000);SystemClock.sleep(n%2==0?2500:350);if(n==0)saveUi("swipe-viewer.png");
            try(ParcelFileDescriptor gesture=getUiAutomation().executeShellCommand("input swipe "+w/2+" "+(h*40/100)+" "+w/2+" "+(h*85/100)+" 250")){}
            long limit=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<limit&&(!activity.resumed||!activity.ready))SystemClock.sleep(100);
            if(!activity.resumed||!activity.ready){saveUi("swipe-return-failure.png");throw new AssertionError("Swipe return "+n+": resumed="+activity.resumed+", ready="+activity.ready+", attached="+activity.engine.attached+", cameraFrames="+activity.engine.frameSeen+", ack="+activity.engine.presentedFrames.acknowledged());}
            long token=activity.engine.presentedFrames.acknowledged();await("new displayed frame after swipe",()->activity.engine.presentedFrames.acknowledged()>token,8000);if(n==5)saveUi("swipe-return-final.png");
        }
        final int before=activity.captureCount;if(video){activity.engine.toggleVideo(false);await("recording after swipe return",()->activity.recording,10000);SystemClock.sleep(1500);activity.engine.toggleVideo(false);}else runOnMainSync(()->activity.shoot());await("capture after swipe return",()->activity.captureCount>before&&!activity.engine.photoBusy&&!activity.recording,30000);
        }finally{if(!activity.resumed){try(ParcelFileDescriptor recover=getUiAutomation().executeShellCommand("am start -n "+getTargetContext().getPackageName()+"/"+MainActivity.class.getName())){}await("return to app after swipe test",()->activity.resumed,10000);}}
    }
    void checkPreviewReturn() throws Exception {
        if(activity.latest==null)throw new AssertionError("Capture a photo before return test");
        runOnMainSync(()->{activity.commitEffects(EffectState.defaults().chain((1<<Effects.VHS)|(1<<Effects.CHROMA_ERROR)).amount(.7f));activity.applyFaultConfig(FaultConfig.defaults().enabled(true));});
        String encoded=activity.effectState.encode();Uri saved=activity.latest;
        for(int n=0;n<6;n++){
            runOnMainSync(()->activity.openExternal(activity.latest));await("external preview pause",()->!activity.resumed,10000);
            if(n<3){await("camera closed outside app",()->!activity.engine.attached,10000);SystemClock.sleep(700);}
            try(ParcelFileDescriptor back=getUiAutomation().executeShellCommand("input keyevent KEYCODE_BACK")){}
            await("camera returns",()->activity.resumed&&activity.ready&&activity.engine.frameSeen,20000);
            if(!encoded.equals(activity.effectState.encode())||!saved.equals(activity.latest)||!activity.faultConfig.enabled)throw new AssertionError("Preview round trip changed manual state");
            long timestamp=activity.engine.lastFrameNs;await("fresh resumed frames",()->activity.engine.lastFrameNs>timestamp,5000);
            if(activity.engine.faultInputs.microphone!=null||!activity.engine.faultInputs.active)throw new AssertionError("Configured LIVE inputs did not resume correctly");
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
            if(!"recommended".equals(defaults.photoSize))throw new AssertionError("Default JPEG is not recommended");defaults.photoSize="max";
            for(android.util.Size advertised:catalog.map.getOutputSizes(android.graphics.SurfaceTexture.class))if(Math.max(advertised.getWidth(),advertised.getHeight())<=activity.engine.maxTexture&&CameraOptions.area(advertised)>CameraOptions.area(catalog.photo(defaults).size))throw new AssertionError("Live photo catalog discards maximum stream: "+id);
            if(!catalog.raws.isEmpty()){defaults.photoFormat=2;if(catalog.photo(defaults)!=catalog.raws.get(0))throw new AssertionError("Default RAW is not maximum");defaults.photoFormat=0;}
            java.util.List<CameraOptions.Video> supported=catalog.videosFor(defaults.codec);if(!supported.isEmpty()){CameraOptions.Video recommended=catalog.video(defaults);boolean modest=supported.stream().anyMatch(v->!v.highSpeed&&v.fps<=30&&CameraOptions.area(v.size)<=catalog.recommendedVideoPixels);if(modest&&(recommended.highSpeed||recommended.fps>30||CameraOptions.area(recommended.size)>catalog.recommendedVideoPixels))throw new AssertionError("Video recommendation exceeds budget");}
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
    android.view.View findText(android.view.View root,String text){
        if(root instanceof android.widget.TextView&&text.contentEquals(((android.widget.TextView)root).getText()))return root;
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++){android.view.View found=findText(group.getChildAt(i),text);if(found!=null)return found;}}
        return null;
    }
    EffectState.Frame presented(){FrameHistory.Lease<SignalBuffer> lease=activity.engine.presentedFrames.reserve();if(lease==null)return null;try{return lease.value.frame;}finally{activity.engine.presentedFrames.release(lease);}}
    void checkExpert(Bundle result)throws Exception{
        runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=0;next.photoSize="recommended";next.rawVideo=false;next.expertMode=false;activity.videoMode=false;activity.applySettings(next);activity.applyFaultConfig(FaultConfig.defaults());activity.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE));});await("normal before expert",()->activity.ready&&!activity.settings.expertMode,20000);
        QualityDialog[] panel=new QualityDialog[1];runOnMainSync(()->{panel[0]=new QualityDialog(activity);panel[0].show();panel[0].content.findViewWithTag("expert-mode").performClick();if(activity.settings.expertMode)throw new AssertionError("Expert applied before save");});waitForIdleSync();SystemClock.sleep(400);saveUi("expert-settings-top.png");runOnMainSync(()->((android.widget.ScrollView)panel[0].content.getParent()).fullScroll(android.view.View.FOCUS_DOWN));waitForIdleSync();SystemClock.sleep(900);saveUi("expert-settings-bottom.png");runOnMainSync(()->panel[0].sheet.dismiss());if(activity.settings.expertMode)throw new AssertionError("Expert cancel committed");
        runOnMainSync(()->{panel[0]=new QualityDialog(activity);panel[0].show();panel[0].content.findViewWithTag("expert-mode").performClick();findText(panel[0].sheet.getWindow().getDecorView(),activity.getString(R.string.ui_apply)).performClick();});await("expert enabled",()->activity.ready&&activity.settings.expertMode&&activity.engine.adaptiveLoad.expert,20000);if(!CaptureSettings.load(activity.getSharedPreferences("signal",0)).expertMode)throw new AssertionError("Expert not persisted");
        GlitchEngine engine=activity.engine;String route=activity.effectState.encode();try{
            glSync(()->{engine.gl.removeCallbacks(engine.thermalPoll);engine.adaptiveLoad.renderMillis=500;engine.applyLoadSample(1000,6,60,2);if(engine.cooling||!engine.adaptiveLoad.expert||engine.previewFps!=engine.expertCameraFps())throw new AssertionError("Expert still capped");});long before=engine.renderedFrames;SystemClock.sleep(2000);long rendered=engine.renderedFrames-before;if(rendered<5||engine.cooling||!activity.ready||!route.equals(activity.effectState.encode()))throw new AssertionError("Expert stopped or changed faults");saveUi("expert-preview.png");
            runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.videoKey="recommended";next.videoQuality=1;activity.videoMode=true;activity.applySettings(next);});await("expert video ready",()->activity.ready&&activity.videoMode,20000);Uri previous=activity.latest;engine.toggleVideo(false);await("expert recording",()->engine.recording,15000);SystemClock.sleep(1200);glSync(()->engine.applyLoadSample(2000,6,60,2));SystemClock.sleep(1200);if(!engine.recording||engine.cooling)throw new AssertionError("Expert thermal stop still active");engine.toggleVideo(false);await("expert recording saved",()->!engine.recording&&activity.latest!=null&&!activity.latest.equals(previous),15000);
            runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.expertMode=false;activity.applySettings(next);});await("expert disabled",()->activity.ready&&!engine.adaptiveLoad.expert,20000);glSync(()->{engine.applyLoadSample(3000,4,30,Float.NaN);if(!engine.cooling||engine.previewFps!=6)throw new AssertionError("Normal protection not restored");});result.putString("result","PASS expert UI cancel/apply/persistence, uncapped preview "+rendered+" frames/2s, simulated critical heat ignored during recording, manual stop saved, normal thermal protection restored");
        }finally{glSync(()->{if(engine.recording)engine.stopVideo();engine.adaptiveLoad.cooling=false;engine.cooling=false;engine.adaptiveLoad.renderMillis=0;engine.updateRequest();engine.gl.post(engine::frame);engine.gl.removeCallbacks(engine.thermalPoll);engine.gl.post(engine.thermalPoll);});}
    }
    void checkLoadRecording(Bundle result)throws Exception{
        runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.rawVideo=false;next.videoKey="recommended";next.videoQuality=1;activity.videoMode=true;activity.applySettings(next);activity.applyFaultConfig(FaultConfig.defaults());activity.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE));});await("recommended video",()->activity.ready&&activity.videoMode,20000);GlitchEngine engine=activity.engine;int width=engine.outW,height=engine.outH;Uri before=activity.latest;
        glSync(()->engine.gl.removeCallbacks(engine.thermalPoll));try{engine.toggleVideo(false);await("recording for thermal stop",()->engine.recording,15000);SystemClock.sleep(2200);glSync(()->engine.applyLoadSample(1000,4,30,Float.NaN));await("thermal stop saved",()->!engine.recording&&activity.latest!=null&&!activity.latest.equals(before),15000);if(!engine.cooling||engine.outW!=width||engine.outH!=height)throw new AssertionError("Thermal stop changed resolution or failed to pause");try(Cursor c=activity.getContentResolver().query(activity.latest,new String[]{MediaStore.MediaColumns.RELATIVE_PATH},null,null,null)){if(c==null||!c.moveToFirst()||!"DCIM/5igna1/".equals(c.getString(0)))throw new AssertionError("Thermal stop output not saved to camera folder");}glSync(()->{engine.applyLoadSample(2000,0,30,Float.NaN);engine.applyLoadSample(32000,0,30,Float.NaN);});await("video preview after cooling",()->!engine.cooling&&activity.ready,8000);if(engine.recording)throw new AssertionError("Recording restarted automatically");result.putString("result","PASS recommended video "+width+"x"+height+", critical-heat stop saved MP4 and preview resumed without restarting recording");}finally{glSync(()->{if(engine.recording)engine.stopVideo();engine.adaptiveLoad.cooling=false;engine.cooling=false;engine.adaptiveLoad.relaxedSince=-1;engine.updateRequest();engine.gl.post(engine::frame);engine.gl.removeCallbacks(engine.thermalPoll);engine.gl.post(engine.thermalPoll);});}
    }
    void checkAdaptiveLoad(Bundle result)throws Exception{
        QualityDialog[] panel=new QualityDialog[1];String oldSize=activity.settings.photoSize;runOnMainSync(()->{panel[0]=new QualityDialog(activity);panel[0].show();panel[0].content.findViewWithTag("load-recommend").performClick();if(!panel[0].draft.photoSize.equals("recommended")||!panel[0].draft.videoKey.equals("recommended"))throw new AssertionError("Recommendation button failed");});waitForIdleSync();SystemClock.sleep(400);saveUi("adaptive-settings.png");runOnMainSync(()->panel[0].sheet.dismiss());if(!activity.settings.photoSize.equals(oldSize))throw new AssertionError("Cancelled recommendation changed settings");
        android.content.SharedPreferences migration=activity.getSharedPreferences("loadMigrationTest",0);try{migration.edit().clear().putString("photoSize","max").commit();if(!CaptureSettings.load(migration).photoSize.equals("recommended"))throw new AssertionError("Legacy maximum not migrated");CaptureSettings explicit=CaptureSettings.load(migration);explicit.photoSize="max";explicit.save(migration);if(!CaptureSettings.load(migration).photoSize.equals("max"))throw new AssertionError("Explicit maximum not retained");migration.edit().clear().putString("photoSize","1920x1080").commit();if(!CaptureSettings.load(migration).photoSize.equals("1920x1080"))throw new AssertionError("Explicit size changed");}finally{migration.edit().clear().commit();}
        runOnMainSync(()->{activity.videoMode=false;CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=0;next.photoSize="recommended";next.rawVideo=false;activity.applySettings(next);activity.applyFaultConfig(FaultConfig.defaults());activity.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE));});await("recommended camera",()->activity.ready&&activity.settings.photoSize.equals("recommended"),20000);
        GlitchEngine engine=activity.engine;long budget=engine.deviceProfile.photoPixels();boolean modest=activity.cameraOptions.photos.stream().anyMatch(p->CameraOptions.area(p.size)<=budget);if(modest&&(long)engine.signalW*engine.signalH>budget)throw new AssertionError("Recommendation exceeds pixel budget");String effects=activity.effectState.encode();int width=engine.outW,height=engine.outH;
        glSync(()->{engine.gl.removeCallbacks(engine.thermalPoll);engine.adaptiveLoad.renderMillis=0;engine.adaptiveLoad.previewFps=24;engine.applyLoadSample(0,0,30,Float.NaN);});SystemClock.sleep(400);long normalStart=engine.renderedFrames;SystemClock.sleep(2000);long normal=engine.renderedFrames-normalStart;
        try{
            long transitionStart=engine.renderedFrames;glSync(()->engine.applyLoadSample(1000,3,30,Float.NaN));await("lower-rate camera resumed",()->engine.renderedFrames>=transitionStart+2,10000);SystemClock.sleep(400);long warmStart=engine.renderedFrames,warmCamera=engine.lastFrameNs;SystemClock.sleep(2000);long warm=engine.renderedFrames-warmStart;if(engine.previewFps!=6||warm>15||warm<5||warm>=normal){saveUi("adaptive-timeout.png");throw new AssertionError("Adaptive render count normal="+normal+" warm="+warm+" fps="+engine.previewFps+" cameraDelta="+(engine.lastFrameNs-warmCamera)+" nextDelta="+(engine.adaptiveLoad.nextPreviewNs-engine.lastFrameNs)+" cooling="+engine.cooling+" range="+engine.request.get(android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)+" ack="+engine.presentedFrames.acknowledged()+" surface="+activity.preview.getSurfaceTexture().getTimestamp());}if(engine.outW!=width||engine.outH!=height||!effects.equals(activity.effectState.encode())||activity.faultConfig.enabled)throw new AssertionError("Load control changed capture/effect state");saveUi("adaptive-load.png");
            glSync(()->engine.applyLoadSample(2000,4,30,Float.NaN));await("cooling pause",()->engine.cooling&&!activity.ready,5000);long paused=engine.renderedFrames;SystemClock.sleep(500);if(engine.renderedFrames!=paused)throw new AssertionError("GPU renders while cooling");saveUi("adaptive-cooling.png");glSync(()->{engine.applyLoadSample(3000,0,30,Float.NaN);if(!engine.cooling)throw new AssertionError("Cooling resumed too soon");engine.applyLoadSample(33000,0,30,Float.NaN);});await("cooling recovery",()->!engine.cooling&&activity.ready&&engine.renderedFrames>paused,8000);
            result.putString("result","PASS recommendations, migration, adaptive render count "+normal+" -> "+warm+" per 2s, cooling pause/recovery, effects and capture size retained with LIVE off");result.putString("recommended",width+"x"+height);
        }finally{glSync(()->{engine.adaptiveLoad.cooling=false;engine.cooling=false;engine.adaptiveLoad.relaxedSince=-1;engine.updateRequest();engine.gl.post(engine::frame);engine.gl.removeCallbacks(engine.thermalPoll);engine.gl.post(engine.thermalPoll);});}
    }
    void glSync(Runnable action)throws Exception{java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);Throwable[] error={null};activity.engine.gl.post(()->{try{action.run();}catch(Throwable failure){error[0]=failure;}finally{done.countDown();}});if(!done.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("GL check timeout");if(error[0]!=null)throw new AssertionError("GL check",error[0]);}
    void checkAdvanced()throws Exception{
        boolean advancedBefore=activity.advancedMode;EffectDialog[] dialog=new EffectDialog[1];
        runOnMainSync(()->{activity.videoMode=false;CaptureSettings settings=new CaptureSettings(activity.settings);settings.photoFormat=0;settings.photoSize="auto";activity.applySettings(settings);activity.applyFaultConfig(FaultConfig.defaults());activity.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE));activity.advancedMode=false;});
        await("advanced camera",()->activity.ready&&activity.settings.photoFormat==0,20000);EffectState original=activity.effectState;
        runOnMainSync(()->activity.preview.setSurfaceTextureListener(new android.view.TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture t,int w,int h){activity.onSurfaceTextureAvailable(t,w,h);}public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture t,int w,int h){activity.onSurfaceTextureSizeChanged(t,w,h);}public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture t){return activity.onSurfaceTextureDestroyed(t);}public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture t){/* Simulate the device's missing presentation callback. */}}));
        try{
            QualityDialog[] settingsEditor=new QualityDialog[1];runOnMainSync(()->{settingsEditor[0]=new QualityDialog(activity);settingsEditor[0].show();settingsEditor[0].content.findViewWithTag("advanced-mode").performClick();if(activity.advancedMode)throw new AssertionError("Global mode committed before Apply");});waitForIdleSync();SystemClock.sleep(300);saveUi("advanced-global-setting.png");runOnMainSync(()->settingsEditor[0].sheet.dismiss());if(activity.advancedMode)throw new AssertionError("Cancelled global mode applied");
            runOnMainSync(()->{settingsEditor[0]=new QualityDialog(activity);settingsEditor[0].show();settingsEditor[0].content.findViewWithTag("advanced-mode").performClick();findText(settingsEditor[0].sheet.getWindow().getDecorView(),activity.getString(R.string.ui_apply)).performClick();});await("global mode applied",()->activity.ready&&activity.advancedMode,20000);if(!activity.getSharedPreferences("signal",0).getBoolean("advancedMode",false))throw new AssertionError("Global mode not persisted");
            runOnMainSync(()->{dialog[0]=new EffectDialog(activity,true);dialog[0].show();if(dialog[0].body.findViewWithTag("advanced-mode")!=null)throw new AssertionError("Per-fault mode switch remains");});waitForIdleSync();
            runOnMainSync(()->{dialog[0].body.findViewWithTag("value-pixelDensity").performClick();android.widget.EditText input=dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-input");input.setText("0.25");dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-apply").performClick();});
            try{await("advanced preview value",()->{EffectState.Frame frame=presented();return frame!=null&&!frame.nodes.isEmpty()&&frame.nodes.get(0).get("pixelDensity")==.25f;},5000);}catch(AssertionError error){saveUi("advanced-timeout.png");EffectState.Frame frame=presented();throw new AssertionError("Advanced preview: surface="+activity.preview.getSurfaceTexture().getTimestamp()+" ack="+activity.engine.presentedFrames.acknowledged()+" camera="+activity.engine.lastFrameNs+" ready="+activity.ready+" valid="+activity.validPreview(dialog[0].edit)+" draft="+dialog[0].draft.resolved(Effects.PIXEL_DAMAGE,"pixelDensity",-1)+" frame="+(frame==null?"null":frame.nodes.toString())+" density="+(frame==null||frame.nodes.isEmpty()?-1:frame.nodes.get(0).get("pixelDensity")),error);}
            if(activity.effectState!=original)throw new AssertionError("Advanced value committed before Apply");
            waitForIdleSync();SystemClock.sleep(300);saveUi("advanced-signal.png");
            runOnMainSync(()->{int[] camera=new int[2],panel=new int[2];activity.preview.getLocationOnScreen(camera);dialog[0].sheet.getWindow().getDecorView().getLocationOnScreen(panel);if(camera[1]+activity.preview.getHeight()>panel[1]||activity.preview.getHeight()<activity.dp(140))throw new AssertionError("Advanced controls obscure preview");dialog[0].sheet.dismiss();});waitForIdleSync();if(activity.effectState!=original)throw new AssertionError("Advanced cancel lost original");
            runOnMainSync(()->{dialog[0]=new EffectDialog(activity,true);dialog[0].show();dialog[0].body.findViewWithTag("value-pixelDensity").performClick();android.widget.EditText input=dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-input");input.setText("-1");dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-apply").performClick();if(!dialog[0].auxiliary.isShowing()||!dialog[0].draft.overrides(Effects.PIXEL_DAMAGE).isEmpty())throw new AssertionError("Invalid number applied");input.setText("0.125");dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-apply").performClick();dialog[0].sheet.getWindow().getDecorView().findViewWithTag("apply").performClick();});waitForIdleSync();
            if(EffectStateStore.load(activity.getSharedPreferences("signal",0)).parameters().resolved(Effects.PIXEL_DAMAGE,"pixelDensity",0)!=.125f)throw new AssertionError("Override not persisted");
            runOnMainSync(()->{activity.commitEffects(EffectState.defaults().single(Effects.VHS));dialog[0]=new EffectDialog(activity,true);dialog[0].show();dialog[0].body.findViewWithTag("event-identity").performClick();android.widget.EditText input=dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-input");input.setText(Long.toString(Long.MIN_VALUE));dialog[0].auxiliary.findViewById(android.R.id.content).findViewWithTag("number-apply").performClick();if(dialog[0].draft.eventIdentity(Effects.VHS,0)!=Long.MIN_VALUE)throw new AssertionError("Event seed exact input failed");});waitForIdleSync();SystemClock.sleep(500);saveUi("advanced-events.png");runOnMainSync(()->dialog[0].sheet.getWindow().getDecorView().findViewWithTag("apply").performClick());waitForIdleSync();if(EffectStateStore.load(activity.getSharedPreferences("signal",0)).parameters().eventIdentity(Effects.VHS,0)!=Long.MIN_VALUE)throw new AssertionError("Event seed not persisted");
            byte[] jpeg=fixture();EffectState hot=EffectState.defaults().single(Effects.PIXEL_DAMAGE);EffectParameters p=hot.parameters().override(Effects.PIXEL_DAMAGE,"pixelDensity",1).override(Effects.PIXEL_DAMAGE,"columnDensity",0).override(Effects.PIXEL_DAMAGE,"hotFraction",1).override(Effects.PIXEL_DAMAGE,"hotValue",1).override(Effects.PIXEL_DAMAGE,"sensorNoise",0);Bitmap white=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(hot.edit(false,hot.mask,p),0));int[] pixels=new int[white.getWidth()*white.getHeight()];white.getPixels(pixels,0,white.getWidth(),0,0,white.getWidth(),white.getHeight());white.recycle();for(int pixel:pixels)if(Color.red(pixel)<253||Color.green(pixel)<253||Color.blue(pixel)<253)throw new AssertionError("Direct pixelDensity not bound to GPU");
            for(int id:Effects.ORDER)if(id!=0)for(boolean high:new boolean[]{false,true}){EffectState state=EffectState.defaults().single(id);EffectParameters values=state.parameters();for(FaultParameters.Spec spec:FaultParameters.all(id))if(spec.group==FaultParameters.Group.SIGNAL||spec.group==FaultParameters.Group.PROFILE)values=values.override(id,spec.key,high?spec.max:spec.min);Bitmap result=PhotoRenderer.render(getTargetContext(),jpeg,false,evaluated(state.edit(false,state.mask,values),.1));result.recycle();}
        }finally{runOnMainSync(()->{if(dialog[0]!=null&&dialog[0].sheet.isShowing())dialog[0].sheet.dismiss();activity.preview.setSurfaceTextureListener(activity);activity.advancedMode=advancedBefore;activity.getSharedPreferences("signal",0).edit().putBoolean("advancedMode",advancedBefore).apply();});}
    }
    void checkPerformance()throws Exception{
        FaultConfig base=new FaultConfig(true,false,false,false,false,false,.5f,50);Dialog[] dialog=new Dialog[1];
        runOnMainSync(()->{activity.videoMode=false;CaptureSettings settings=new CaptureSettings(activity.settings);settings.photoFormat=0;settings.photoSize="auto";activity.applySettings(settings);activity.applyFaultConfig(base);activity.commitEffects(EffectState.defaults().chain((1<<Effects.VHS)|(1<<Effects.CRT)));});await("performance camera",()->activity.ready,20000);
        try{
            runOnMainSync(()->dialog[0]=FaultDialog.show(activity));await("performance preview fork",()->activity.engine.previewFaults!=null,5000);
            runOnMainSync(()->{FaultDialog.Editor editor=activity.liveEditor;editor.body.findViewWithTag("live-style").performClick();editor.child.getWindow().getDecorView().findViewWithTag("choice-3").performClick();editor.performance=editor.performance.with("speed",-2).with("clock",LivePerformance.LOOP);editor.preview();editor.render();});
            await("performance preview setting",()->activity.engine.previewFaultConfig!=null&&activity.engine.previewFaultConfig.performance.speed==-2,5000);
            if(activity.faultConfig!=base||activity.engine.faultConfig!=base)throw new AssertionError("LIVE preview committed config");
            runOnMainSync(()->dialog[0].dismiss());await("performance discard",()->activity.engine.previewFaults==null,5000);if(activity.engine.faultConfig!=base)throw new AssertionError("LIVE cancel lost config");
            runOnMainSync(()->{dialog[0]=FaultDialog.show(activity);FaultDialog.Editor editor=activity.liveEditor;editor.performance=editor.performance.with("style",LivePerformance.CASCADE).with("clock",LivePerformance.PING_PONG).with("period",2.5f).with("interval",.2f);editor.preview();editor.render();});waitForIdleSync();SystemClock.sleep(400);runOnMainSync(()->{int[] camera=new int[2],panel=new int[2];activity.preview.getLocationOnScreen(camera);dialog[0].getWindow().getDecorView().getLocationOnScreen(panel);if(camera[1]+activity.preview.getHeight()>panel[1]||activity.preview.getHeight()<activity.dp(140))throw new AssertionError("LIVE editor obscures preview");});saveUi("live-direction.png");
            runOnMainSync(()->findText(dialog[0].getWindow().getDecorView(),activity.getString(R.string.ui_apply)).performClick());await("performance commit",()->activity.engine.previewFaults==null&&activity.engine.faultConfig.performance.clock==LivePerformance.PING_PONG,5000);
            if(FaultPreferences.load(activity.getSharedPreferences("signal",0)).performance.style!=LivePerformance.CASCADE||FaultPreferences.load(activity.getSharedPreferences("signal",0)).performance.periodSeconds!=2.5f||FaultPreferences.load(activity.getSharedPreferences("signal",0)).performance.stepSeconds!=.2f)throw new AssertionError("Performance not saved");
            waitForIdleSync();SystemClock.sleep(300);saveUi("live-time-buttons.png");
            runOnMainSync(()->activity.liveHold.performClick());java.util.concurrent.CountDownLatch barrier=new java.util.concurrent.CountDownLatch(1);long[] beforeHold={0};double[] expectedTime={0};activity.engine.gl.post(()->{beforeHold[0]=activity.engine.lastFrameNs;expectedTime[0]=activity.engine.faults.time(activity.engine.faultConfig);barrier.countDown();});if(!barrier.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("HOLD queue timeout");
            await("first held frame",()->{EffectState.Frame frame=presented();return frame!=null&&frame.cameraNs>beforeHold[0];},5000);EffectState.Frame held=presented();await("camera continues during HOLD",()->{EffectState.Frame frame=presented();return frame!=null&&frame.cameraNs>held.cameraNs;},5000);EffectState.Frame later=presented();if(held.time!=later.time)throw new AssertionError("HOLD time changed: "+held.time+" -> "+later.time+" expected="+expectedTime[0]+" hold="+activity.engine.faultConfig.performance.hold);
            activity.engine.hitFaults();await("manual HIT",()->{EffectState.Frame frame=presented();return frame!=null&&frame.nodes.stream().anyMatch(n->n.id==Effects.VHS&&n.event.envelope==1);},5000);
            activity.engine.rewindFaults();await("timeline restart",()->{EffectState.Frame frame=presented();return frame!=null&&frame.time==0;},5000);
        }finally{runOnMainSync(()->{if(activity.liveEditor!=null)activity.liveEditor.dialog.dismiss();});}
    }
    void checkFormatUi()throws Exception{
        runOnMainSync(()->{activity.videoMode=false;CaptureSettings settings=new CaptureSettings(activity.settings);settings.photoFormat=0;activity.applySettings(settings);});
        await("JPG ready",()->activity.ready&&activity.settings.photoFormat==0,20000);
        EffectState before=activity.effectState;Dialog[] picker=new Dialog[1];
        runOnMainSync(()->{picker[0]=activity.showFormat();if(picker[0].getWindow().getDecorView().findViewWithTag("choice-2")!=null)throw new AssertionError("Original RAW still offered");if(findText(picker[0].getWindow().getDecorView(),"RAW")==null)throw new AssertionError("RAW missing");});
        waitForIdleSync();SystemClock.sleep(200);saveUi("jpg-raw-picker.png");
        runOnMainSync(()->picker[0].dismiss());waitForIdleSync();if(activity.settings.photoFormat!=0)throw new AssertionError("Dismiss changed format");
        runOnMainSync(()->{picker[0]=activity.showFormat();picker[0].getWindow().getDecorView().findViewWithTag("choice-1").performClick();});
        await("RAW ready",()->activity.ready&&activity.settings.photoFormat==2,20000);
        if(!before.encode().equals(activity.effectState.encode()))throw new AssertionError("Format lost effect settings");
        android.content.SharedPreferences fixture=getTargetContext().getSharedPreferences("formatMigrationTest",0);
        try{fixture.edit().putInt("photoFormat",1).commit();if(CaptureSettings.load(fixture).photoFormat!=2)throw new AssertionError("Legacy original RAW not migrated");}finally{fixture.edit().clear().commit();}
        runOnMainSync(()->{CaptureSettings legacy=new CaptureSettings(activity.settings);legacy.photoFormat=1;activity.applySettings(legacy);if(activity.settings.photoFormat!=2)throw new AssertionError("Legacy format applied");SignalToggle toggle=new SignalToggle(activity,"Test",false);int[] changed={0};toggle.setOnCheckedChangeListener((v,checked)->changed[0]++);toggle.performClick();if(!toggle.isChecked()||changed[0]!=1)throw new AssertionError("Custom toggle failed");});
        await("migrated RAW ready",()->activity.ready,20000);
        runOnMainSync(()->picker[0]=FaultDialog.show(activity));waitForIdleSync();SystemClock.sleep(200);saveUi("custom-live-settings.png");runOnMainSync(()->picker[0].dismiss());
        runOnMainSync(()->{QualityDialog quality=new QualityDialog(activity);quality.show();picker[0]=quality.sheet;});waitForIdleSync();SystemClock.sleep(200);saveUi("custom-capture-settings.png");runOnMainSync(()->picker[0].dismiss());

    }
    void saveUi(String name)throws Exception{runOnMainSync(()->{activity.getWindow().getDecorView().invalidate();});waitForIdleSync();SystemClock.sleep(100);Bitmap image=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"verification");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
    void checkChainFormat()throws Exception{
        EffectState selected=EffectState.defaults().chain((1<<Effects.ROW_ERROR)|(1<<Effects.CRT)|(1<<Effects.VHS));
        runOnMainSync(()->{activity.videoMode=false;activity.applyFaultConfig(FaultConfig.defaults());CaptureSettings raw=new CaptureSettings(activity.settings);raw.photoFormat=2;raw.photoSize="max";activity.applySettings(raw);activity.commitEffects(selected);});
        await("RAW ready",()->activity.ready&&activity.settings.photoFormat==2,20000);
        EffectDialog[] dialog=new EffectDialog[1];
        runOnMainSync(()->{
            if(activity.effectState.mask!=selected.mask||activity.uiEffects().length!=1)throw new AssertionError("RAW erased route or enabled RGB faults");
            dialog[0]=new EffectDialog(activity,false);dialog[0].show();
            if(dialog[0].choices.size()!=13||!dialog[0].choices.get(Effects.CRT).getText().toString().contains("DISPLAY"))throw new AssertionError("Missing DISPLAY catalog");
        });waitForIdleSync();SystemClock.sleep(250);saveUi("lean-raw-catalog.png");
        runOnMainSync(()->{
            dialog[0].choices.get(Effects.EXPOSURE).performClick();dialog[0].choices.get(Effects.CRT).performClick();
            if(activity.settings.photoFormat!=2||activity.effectState.mask!=selected.mask)throw new AssertionError("Catalog changed format or committed draft");
            dialog[0].sheet.getWindow().getDecorView().findViewWithTag("switch-format").performClick();
        });
        waitForIdleSync();await("JPEG ready",()->activity.ready&&activity.settings.photoFormat==0,20000);saveUi("lean-crt-tuning.png");
        runOnMainSync(()->{
            EffectDialog replacement=(EffectDialog)activity.effectEditorOwner;dialog[0]=replacement;
            if(replacement==null||!replacement.sheet.isShowing()||replacement.focused!=Effects.CRT||replacement.state().mask!=(selected.mask|(1<<Effects.EXPOSURE)))throw new AssertionError("Conversion lost draft");
            if(activity.effectState.mask!=selected.mask)throw new AssertionError("Conversion committed draft");
            replacement.sheet.dismiss();
        });waitForIdleSync();
        if(activity.effectState.mask!=selected.mask||activity.uiEffects().length!=3)throw new AssertionError("Cancel or JPEG activation failed");
        for(int format:new int[]{2,0}){
            runOnMainSync(()->{CaptureSettings next=new CaptureSettings(activity.settings);next.photoFormat=format;next.photoSize="max";activity.applySettings(next);});
            await("format roundtrip",()->activity.ready&&activity.settings.photoFormat==format,20000);
            if(activity.effectState.mask!=selected.mask||EffectStateStore.load(activity.getSharedPreferences("signal",0)).mask!=selected.mask)throw new AssertionError("Format change lost saved route");
        }
    }
    void checkEditor()throws Exception{
        runOnMainSync(()->{activity.applyFaultConfig(activity.faultConfig.enabled(false));activity.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE));});
        boolean advancedBefore=activity.advancedMode;runOnMainSync(()->activity.advancedMode=false);EffectState original=activity.effectState;EffectDialog[] reference=new EffectDialog[1];
        runOnMainSync(()->{reference[0]=new EffectDialog(activity,false);reference[0].show();});EffectDialog editor=reference[0];
        try{
            await("editor layout",()->editor.sheet.getWindow().getDecorView().getHeight()>0&&activity.effectEditorSpace!=null,5000);SystemClock.sleep(300);
            runOnMainSync(()->{
                int[] camera=new int[2],panel=new int[2];activity.preview.getLocationOnScreen(camera);editor.sheet.getWindow().getDecorView().getLocationOnScreen(panel);
                if(activity.preview.getHeight()<activity.dp(140)||camera[1]+activity.preview.getHeight()>panel[1])throw new AssertionError("Editor covers the preview");
                editor.choices.get(Effects.EXPOSURE).performClick();editor.choices.get(Effects.ROW_ERROR).performClick();
                if(editor.state().ids().length!=3||!editor.state().chained)throw new AssertionError("Multi-selection lost faults");
                if(!original.encode().equals(activity.effectState.encode()))throw new AssertionError("Draft selection committed before Apply");
                editor.focused=Effects.PIXEL_DAMAGE;editor.tuning=true;editor.renderRoute();editor.renderBody();
            });
            SystemClock.sleep(200);
            runOnMainSync(()->{
                android.widget.SeekBar slider=editor.body.findViewWithTag("density");long time=SystemClock.uptimeMillis();
                for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE,android.view.MotionEvent.ACTION_UP}){
                    android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time+10,action,slider.getWidth()*.8f,slider.getHeight()*.5f,0);slider.dispatchTouchEvent(event);event.recycle();
                }
                if(editor.draft.get(Effects.PIXEL_DAMAGE,"density")<=.65f)throw new AssertionError("Slider did not update draft");
                int[] camera=new int[2],panel=new int[2];activity.preview.getLocationOnScreen(camera);editor.sheet.getWindow().getDecorView().getLocationOnScreen(panel);if(camera[1]+activity.preview.getHeight()>panel[1])throw new AssertionError("Tuning covers the preview");
            });
            await("draft preview frame",()->{FrameHistory.Lease<SignalBuffer> lease=activity.engine.presentedFrames.reserve();if(lease==null)return false;try{return lease.value.frame.parameters.get(Effects.PIXEL_DAMAGE,"density")>.65f;}finally{activity.engine.presentedFrames.release(lease);}},5000);
            runOnMainSync(()->editor.sheet.dismiss());
            waitForIdleSync();
            if(!original.encode().equals(activity.effectState.encode())||activity.effectEditorSpace!=null)throw new AssertionError("Cancel did not restore editor layout/state");
            runOnMainSync(()->{reference[0]=new EffectDialog(activity,false);reference[0].show();});EffectDialog second=reference[0];
            runOnMainSync(()->{
                second.sheet.getWindow().getDecorView().findViewWithTag("random-chain").performClick();
                if(second.state().ids().length<2)throw new AssertionError("Random chain missing stages");
                for(int id:second.state().ids())if(!Effects.available(id,activity.videoMode,!activity.videoMode&&activity.settings.photoFormat==2))throw new AssertionError("Unsupported random fault");
                String generated=second.state().encode();second.sheet.getWindow().getDecorView().findViewWithTag("apply").performClick();
                if(!generated.equals(activity.effectState.encode()))throw new AssertionError("Apply did not preserve generated chain");
            });
            waitForIdleSync();if(activity.effectEditorSpace!=null)throw new AssertionError("Apply did not restore layout");
            runOnMainSync(()->{EffectDialog old=new EffectDialog(activity,false);old.show();reference[0]=new EffectDialog(activity,false);reference[0].show();});
            waitForIdleSync();if(activity.effectEditorSpace==null||!reference[0].sheet.isShowing())throw new AssertionError("Stale dismissal erased new editor space");
        }finally{runOnMainSync(()->{activity.advancedMode=advancedBefore;if(reference[0].sheet.isShowing())reference[0].sheet.dismiss();});waitForIdleSync();}
    }
    void checkPixelPreview()throws Exception{
        for(int[] size:new int[][]{{192,256},{1080,1440}}){
            Bitmap input=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);input.eraseColor(Color.rgb(100,100,100));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();input.compress(Bitmap.CompressFormat.JPEG,100,bytes);input.recycle();
            for(long seed:new long[]{4547633322485754723L,0x51a1L,91L}){
                EffectState base=EffectState.defaults().single(Effects.PIXEL_DAMAGE).amount(.88f);
                EffectParameters controls=base.parameters().with(Effects.PIXEL_DAMAGE,"density",1).with(Effects.PIXEL_DAMAGE,"hot",1).with(Effects.PIXEL_DAMAGE,"columns",1).reseed(Effects.PIXEL_DAMAGE,seed);
                Bitmap output=PhotoRenderer.render(getTargetContext(),bytes.toByteArray(),false,evaluated(base.edit(false,base.mask,controls),0));
                int[] pixels=new int[size[0]*size[1]];output.getPixels(pixels,0,size[0],0,0,size[0],size[1]);output.recycle();
                int damaged=0;for(int pixel:pixels)if(Math.abs(Color.red(pixel)-100)>30)damaged++;
                double ratio=damaged/(double)pixels.length;android.util.Log.i("SignalCheck","Pixel damage "+size[0]+"x"+size[1]+" seed="+seed+" damaged="+ratio);
                if(ratio<.005||ratio>.25)throw new AssertionError("Pixel damage density collapsed at "+size[0]+"x"+size[1]+" seed="+seed+": "+ratio);
            }
        }
    }
    void checkMetadata()throws Exception{
        Bitmap b=Bitmap.createBitmap(64,96,Bitmap.Config.ARGB_8888);b.eraseColor(Color.GREEN);File file=new File(getTargetContext().getCacheDir(),"metadata-check.jpg");try(OutputStream out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.JPEG,95,out);}b.recycle();Location fake=new Location("gps");fake.setLatitude(-12.345678);fake.setLongitude(123.456789);fake.setAltitude(42.5);fake.setAccuracy(3);fake.setTime(System.currentTimeMillis());fake.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());PhotoMetadata.write(file,null,null,System.currentTimeMillis(),64,96,fake,"SIGNAL METADATA CHECK");ExifInterface exif=new ExifInterface(file);float[] latlong=new float[2];if(!exif.getLatLong(latlong)||Math.abs(latlong[0]+12.345678)>.00001||Math.abs(latlong[1]-123.456789)>.00002)throw new AssertionError("GPS roundtrip");if(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)==null||exif.getAttribute(ExifInterface.TAG_SOFTWARE)==null||exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,0)!=1)throw new AssertionError("EXIF fields");file.delete();
        int w=128,h=96;byte[] samples=new byte[w*h*2];for(int i=0;i<w*h;i++)RawGlitch.write(samples,i,256+i%3500);
        EffectState.Frame frame=evaluated(EffectState.defaults().chain(-1),.1);
        byte[] first=RawGlitch.chain(samples,w,h,4095,256,frame),same=RawGlitch.chain(samples,w,h,4095,256,frame);
        if(!Arrays.equals(first,same))throw new AssertionError("RAW snapshot replay");for(int i=0;i<w*h;i++)if(RawGlitch.read(first,i)>4095)throw new AssertionError("RAW bounds");
    }
}
