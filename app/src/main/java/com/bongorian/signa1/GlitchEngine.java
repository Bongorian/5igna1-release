package com.bongorian.signa1;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.*;
import android.location.Location;
import android.media.*;
import android.net.Uri;
import android.opengl.*;
import android.os.*;
import android.provider.MediaStore;
import android.util.*;
import android.view.Surface;
import java.io.*;
import java.nio.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

final class GlitchEngine {
    interface Listener {
        void status(String text);void ready(boolean value);void recording(boolean value);void saved(Uri uri,boolean video);
        void configured(CameraOptions options,CaptureSettings settings,boolean video,int width,int height,String detail);
        void fps(float fps);default void liveFrame(EffectState.Frame frame){}
    }
    final Activity context;final Listener listener;
    final HandlerThread thread=new HandlerThread("SignalGL");final Handler gl;
    final Handler ui=new Handler(Looper.getMainLooper());final ExecutorService files=Executors.newSingleThreadExecutor();
    CameraDevice camera;CameraCaptureSession session;CaptureRequest.Builder request;CameraCharacteristics characteristics;
    SurfaceTexture displayTarget;
    SurfaceTexture cameraTexture;Surface cameraSurface,displaySurface,encoderSurface;ImageReader stillReader;
    EGLDisplay display=EGL14.EGL_NO_DISPLAY;EGLContext eglContext=EGL14.EGL_NO_CONTEXT;
    EGLSurface window=EGL14.EGL_NO_SURFACE,encoder=EGL14.EGL_NO_SURFACE;EGLConfig eglConfig;
    EffectChain previewChain,blitChain;
    final FrameHistory<SignalBuffer> presentedFrames=new FrameHistory<>(3,SignalBuffer::new);
    final SignalBuffer encoderScratch=new SignalBuffer();
    final EffectState.Frame cleanFrame=EffectState.defaults().snapshot(true,0);
    static final float[] IDENTITY={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    int signalW,signalH;
    final LinkedHashMap<Long,TotalCaptureResult> signalMetadata=new LinkedHashMap<>();
    final FaultModel faults=new FaultModel();final FaultInputs faultInputs;
    FaultConfig faultConfig=FaultConfig.defaults();boolean recorderAudio;
    volatile String faultStatus="LIVE FAULT OFF";
    long faultUiNs,liveUiNs;int liveUiMask=-1;
    void setFaultConfig(FaultConfig next){gl.post(()->{faultConfig=next;faultInputs.configure(next,attached,recorderAudio);});}
    EffectState.Frame faultFrame(EffectState state){return faults.apply(rawVideoMode()?state.snapshot(false,1):state.snapshot(videoMode,settings.photoFormat),faultConfig);}
    final CameraCaptureSession.CaptureCallback timingCallback=new CameraCaptureSession.CaptureCallback(){
        @Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult result){if(s==session){faultInputs.capture(result);Long ns=result.get(CaptureResult.SENSOR_TIMESTAMP);if(ns!=null){signalMetadata.put(ns,result);while(signalMetadata.size()>16)signalMetadata.remove(signalMetadata.keySet().iterator().next());}if(rawRecorder!=null)rawRecorder.result(result);if(rawProbe!=null)rawProbe.result(result);}}
    };
    private EffectState effectState=EffectState.defaults(),previewEffects;
    private final java.util.concurrent.atomic.AtomicLong configRevision=new java.util.concurrent.atomic.AtomicLong();
    private long appliedRevision;
    void setLocationEnabled(boolean value){gl.post(()->settings.location=value);}
    void setEffects(EffectState next){gl.post(()->{effectState=next;previewEffects=null;});}
    void previewEffects(EffectState next){gl.post(()->previewEffects=next);}
    void clearEffectPreview(){gl.post(()->previewEffects=null);}
    int program,texture,width,height,sensorRotation=90,maxTexture=4096;
    volatile int generation;
    volatile int outW=3072,outH=4096;volatile boolean front,torch,frameSeen,recording,photoBusy;
    boolean sessionFallback;
    boolean attached,videoMode;float zoom=1,maxZoom=4;final float[] matrix=new float[16];FloatBuffer vertices;
    int positionLoc,matrixLoc,timeLoc,amountLoc,modeLoc,sizeLoc;
    CameraOptions.RawVideo rawVideoChoice;ImageReader rawReader;RawVideoRecorder rawProbe;volatile RawVideoRecorder rawRecorder;volatile boolean rawFrameSeen;
    String rawProgress(){RawVideoRecorder current=rawRecorder;return current==null?"":current.written+context.getString(R.string.ui_frames_dropped_253)+current.dropped;}
    boolean rawVideoMode(){return videoMode&&settings.rawVideo;}
    CameraOptions options;CameraOptions.Photo photoChoice;CameraOptions.Video videoChoice;
    CaptureSettings settings;Supplier<Location> position=()->null;
    MediaRecorder recorder;ParcelFileDescriptor videoFd,nextFd;Uri videoUri,nextUri;long videoTaken,nextTaken;
    long lastFrameNs,lastPreviewNs,frameCount,fpsStart,encoderTimeOffset=Long.MIN_VALUE;
    long segmentBytes=3_500_000_000L; // bounded files, no total recording time limit
    PendingPhoto pending;final Map<String,CameraOptions> catalogs=new HashMap<>();
    final Runnable storageWatch=new Runnable(){public void run(){if(!recording)return;try{if(new StatFs(Environment.getExternalStorageDirectory().getPath()).getAvailableBytes()<256_000_000L){stopVideo();status(context.getString(R.string.ui_recording_saved_and_stopped_due_to_low_storage));return;}}catch(Exception ignored){}gl.postDelayed(this,5000);}};
    GlitchEngine(Activity activity,Listener l){context=activity;listener=l;settings=CaptureSettings.load(activity.getSharedPreferences("signal",0));thread.start();gl=new Handler(thread.getLooper());faultInputs=new FaultInputs(activity,gl);vertices=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();vertices.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);RawVideoRecorder.recoverPending(this);}
    void status(String text){ui.post(()->listener.status(text));}
    void ready(boolean value){long revision=appliedRevision;int ticket=generation;ui.post(()->{if(revision==configRevision.get()&&(!value||ticket==generation))listener.ready(value);});}
    void error(String text,Exception e){Log.e("Signal",text,e);status(text+" · "+e.getClass().getSimpleName());}
    void attach(SurfaceTexture target,int w,int h){long revision=configRevision.incrementAndGet();gl.post(()->{if(revision!=configRevision.get())return;if(attached&&displayTarget==target){appliedRevision=revision;width=w;height=h;ready(frameSeen);return;}appliedRevision=revision;close();displayTarget=target;width=w;height=h;attached=true;faultInputs.configure(faultConfig,true,recorderAudio);sessionFallback=false;try{initGl(target);openCamera();}catch(Exception e){error(context.getString(R.string.ui_could_not_start_the_camera),e);close();}});}
    void resize(int w,int h){gl.post(()->{width=w;height=h;});}
    void detach(){configRevision.incrementAndGet();gl.post(this::close);}
    void releaseSurface(SurfaceTexture target){gl.post(()->{if(displayTarget==target)close();target.release();});}
    void shutdown(){gl.post(()->{close();files.shutdown();thread.quitSafely();});}
    void configure(CaptureSettings next,boolean video,EffectState effects){
        CaptureSettings copy=new CaptureSettings(next);long revision=configRevision.incrementAndGet();
        gl.post(()->{if(recording||photoBusy)return;appliedRevision=revision;settings=copy;videoMode=video;effectState=effects;previewEffects=null;if(attached)restart();});
    }
    void restart(){sessionFallback=false;try{closeCamera();current(window);if(cameraTexture!=null){cameraTexture.setOnFrameAvailableListener(null);cameraTexture.release();}GLES20.glDeleteTextures(1,new int[]{texture},0);initCameraTexture();status(context.getString(R.string.ui_configuring_camera));openCamera();}catch(Exception e){error(context.getString(R.string.ui_could_not_change_capture_settings),e);}}
    void switchCamera(){gl.post(()->{if(recording||photoBusy||!attached)return;front=!front;torch=false;zoom=1;restart();});}
    void torch(){gl.post(()->{if(characteristics==null)return;if(!Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))){status(context.getString(R.string.ui_this_camera_has_no_light));return;}torch=!torch;updateRequest();});}
    void zoom(float value){gl.post(()->{zoom=Math.max(1,Math.min(maxZoom,value));updateRequest();});}
    void focus(){gl.post(()->{if(request==null||session==null||session instanceof CameraConstrainedHighSpeedCaptureSession)return;try{request.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);session.capture(request.build(),null,gl);request.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);}catch(Exception e){Log.w("Signal","Focus",e);}});}
    void openCamera(){
        if(!attached||context.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){status(context.getString(R.string.ui_allow_camera_access));return;}
        try{
            CameraManager manager=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=null;
            for(String candidate:manager.getCameraIdList()){CameraCharacteristics cc=manager.getCameraCharacteristics(candidate);Integer facing=cc.get(CameraCharacteristics.LENS_FACING);if(cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!=null&&facing!=null&&facing==(front?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK)){id=candidate;characteristics=cc;break;}}
            if(id==null)for(String candidate:manager.getCameraIdList()){
                CameraCharacteristics cc=manager.getCameraCharacteristics(candidate);
                if(cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!=null){id=candidate;characteristics=cc;break;}
            }
            if(id==null)throw new IllegalStateException("No camera");
            front=Integer.valueOf(CameraCharacteristics.LENS_FACING_FRONT).equals(characteristics.get(CameraCharacteristics.LENS_FACING));
            Integer rotation=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);sensorRotation=rotation==null?0:rotation;
            options=catalogs.get(id);if(options==null){options=new CameraOptions(id,characteristics,maxTexture);catalogs.put(id,options);}
            if(options.videosFor(settings.codec).isEmpty())settings.codec=settings.codec.equals("video/hevc")?"video/avc":"video/hevc";
            if(settings.photoFormat!=0&&options.raws.isEmpty()){settings.photoFormat=0;status(context.getString(R.string.ui_raw_is_unavailable_on_this_camera_switched_to));}
            if(rawVideoMode()||(!videoMode&&settings.photoFormat!=0))zoom=1;
            if(settings.rawVideo&&!options.rawVideoAvailable()){settings.rawVideo=false;status(context.getString(R.string.ui_raw_video_is_unavailable_on_this_camera));}
            rawVideoChoice=options.rawVideo(settings);photoChoice=options.photo(settings);videoChoice=rawVideoMode()&&rawVideoChoice!=null?new CameraOptions.Video(rawVideoChoice.size,Math.min(settings.rawVideoFps,rawVideoChoice.maxFps),false):options.video(settings);if(videoMode?videoChoice==null:photoChoice==null)throw new IllegalStateException("No supported output");
            Size stream=rawVideoMode()?options.previewFor(new CameraOptions.Photo(rawVideoChoice.size,false)):videoMode?videoChoice.size:settings.photoFormat==0?photoChoice.size:options.previewFor(photoChoice);
            if(sessionFallback&&!videoMode)for(Size candidate:options.map.getOutputSizes(SurfaceTexture.class))if(CameraOptions.area(candidate)<CameraOptions.area(stream))stream=candidate;Size output=videoMode?videoChoice.size:settings.photoFormat==0?stream:photoChoice.size;
            signalW=stream.getHeight();signalH=stream.getWidth();
            outW=output.getHeight();outH=output.getWidth();cameraTexture.setDefaultBufferSize(stream.getWidth(),stream.getHeight());
            Float mz=characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);maxZoom=mz==null?1:Math.min(4,mz);
            settings.save(context.getSharedPreferences("signal",0));
            CameraOptions catalog=options;CaptureSettings actual=new CaptureSettings(settings);int ow=outW,oh=outH;String detail=description();boolean actualVideo=videoMode;long revision=appliedRevision;final int ticket=++generation;
            ui.post(()->{if(ticket==generation&&revision==configRevision.get())listener.configured(catalog,actual,actualVideo,ow,oh,detail);});
            Log.i("Signal","Camera="+id+" stream="+stream+" output="+outW+"x"+outH+" "+detail);
            manager.openCamera(id,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice c){if(ticket!=generation||!attached){c.close();return;}camera=c;createSession(c,ticket);}
                public void onDisconnected(CameraDevice c){c.close();if(ticket==generation){closeCamera();status(context.getString(R.string.ui_camera_disconnected_select_your_settings_again));}}
                public void onError(CameraDevice c,int code){c.close();if(ticket==generation){closeCamera();status(context.getString(R.string.ui_camera_error)+code+context.getString(R.string.ui_select_your_settings_again));}}
            },gl);
        }catch(Exception e){error(context.getString(R.string.ui_could_not_open_the_camera),e);}
    }
    String description(){if(rawVideoMode())return context.getString(R.string.ui_raw_sequence)+outW+"×"+outH+context.getString(R.string.ui_target)+videoChoice.fps+"fps";if(videoMode)return outW+"×"+outH+" / "+videoChoice.fps+"fps";return String.format(Locale.US,"%.1f MP / %s",outW*(double)outH/1e6,settings.photoFormat==0?"JPEG "+settings.jpegQuality:settings.photoFormat==1?context.getString(R.string.ui_raw_original):context.getString(R.string.ui_processed_raw));}
    void createSession(CameraDevice c,int ticket){try{
        cameraSurface=new Surface(cameraTexture);List<Surface> surfaces=new ArrayList<>();surfaces.add(cameraSurface);
        if(rawVideoMode()){rawFrameSeen=false;rawReader=ImageReader.newInstance(rawVideoChoice.size.getWidth(),rawVideoChoice.size.getHeight(),ImageFormat.RAW_SENSOR,3);rawReader.setOnImageAvailableListener(reader->rawImage(reader,ticket),gl);surfaces.add(rawReader.getSurface());}
        if(!videoMode&&settings.photoFormat!=0){int format=ImageFormat.RAW_SENSOR;stillReader=ImageReader.newInstance(photoChoice.size.getWidth(),photoChoice.size.getHeight(),format,2);stillReader.setOnImageAvailableListener(reader->imageAvailable(reader,ticket),gl);surfaces.add(stillReader.getSurface());}
        CameraCaptureSession.StateCallback callback=new CameraCaptureSession.StateCallback(){
            public void onConfigured(CameraCaptureSession s){if(ticket!=generation||camera!=c){s.close();return;}session=s;try{request=c.createCaptureRequest(videoMode?CameraDevice.TEMPLATE_RECORD:CameraDevice.TEMPLATE_PREVIEW);request.addTarget(cameraSurface);if(rawVideoMode()&&rawReader!=null){request.addTarget(rawReader.getSurface());int[] shading=characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES);if(shading!=null)for(int mode:shading)if(mode==CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON)request.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,mode);rawProbe=new RawVideoRecorder(GlitchEngine.this,true);}updateRequest();if(rawVideoMode())gl.postDelayed(()->{if(ticket==generation&&!rawFrameSeen)failRawSession(context.getString(R.string.ui_could_not_receive_continuous_raw_frames));},8000);}catch(Exception e){error(context.getString(R.string.ui_could_not_configure_the_preview),e);}}
            public void onConfigureFailed(CameraCaptureSession s){s.close();if(ticket==generation)recoverSession();}
        };
        if(rawVideoMode()){
            List<OutputConfiguration> outputs=new ArrayList<>();for(Surface surface:surfaces)outputs.add(new OutputConfiguration(surface));SessionConfiguration configuration=new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,r->gl.post(r),callback);
            try{if(!c.isSessionConfigurationSupported(configuration)){failRawSession(context.getString(R.string.ui_simultaneous_raw_and_preview_output_is_unsupported));return;}}catch(UnsupportedOperationException ignored){}
            c.createCaptureSession(configuration);
        }
        else if(videoMode&&videoChoice.highSpeed)c.createConstrainedHighSpeedCaptureSession(surfaces,callback,gl);
        else if(!videoMode&&photoChoice.maximumPixelMode){List<OutputConfiguration> outputs=new ArrayList<>();outputs.add(new OutputConfiguration(cameraSurface));OutputConfiguration still=new OutputConfiguration(stillReader.getSurface());still.addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);outputs.add(still);c.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,r->gl.post(r),callback));}
        else c.createCaptureSession(surfaces,callback,gl);
    }catch(Exception e){Log.w("Signal","Session configuration",e);if(ticket==generation)recoverSession();}}
    void failRawSession(String reason){if(options!=null)options.rawVideoFailure=reason;settings.rawVideo=false;settings.save(context.getSharedPreferences("signal",0));status(reason+context.getString(R.string.ui_returning_to_standard_video));restart();}
    void recoverSession(){
        if(rawVideoMode()){failRawSession(context.getString(R.string.ui_could_not_start_the_raw_video_session));return;}
        if(sessionFallback){ready(false);status(context.getString(R.string.ui_this_camera_cannot_use_these_capture_settings));return;}
        sessionFallback=true;closeCamera();
        settings.photoFormat=0;settings.photoSize="auto";settings.videoKey="";settings.codec="video/avc";
        if(!videoMode&&!options.photos.isEmpty())for(CameraOptions.Photo p:options.photos)if(!p.maximumPixelMode)settings.photoSize=p.key();
        if(videoMode){List<CameraOptions.Video> choices=options.videosFor(settings.codec);if(!choices.isEmpty())settings.videoKey=choices.get(choices.size()-1).key();}
        status(context.getString(R.string.ui_retrying_with_a_compatible_lower_resolution));
        try{
            current(window);cameraTexture.setOnFrameAvailableListener(null);cameraTexture.release();
            GLES20.glDeleteTextures(1,new int[]{texture},0);initCameraTexture();openCamera();
        }catch(Exception e){error(context.getString(R.string.ui_could_not_reconfigure_the_camera),e);}
    }
    void applyControls(CaptureRequest.Builder builder,boolean still){
        builder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        int[] af=characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);int desired=still?CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE:CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
        if(af!=null)for(int value:af)if(value==desired)builder.set(CaptureRequest.CONTROL_AF_MODE,desired);
        if(Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)))builder.set(CaptureRequest.FLASH_MODE,torch?CaptureRequest.FLASH_MODE_TORCH:CaptureRequest.FLASH_MODE_OFF);
        Rect bounds=characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(bounds==null)return;int w=(int)(bounds.width()/zoom),h=(int)(bounds.height()/zoom);builder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(bounds.centerX()-w/2,bounds.centerY()-h/2,bounds.centerX()+w/2,bounds.centerY()+h/2));
    }
    void updateRequest(){if(session==null||request==null)return;try{
        applyControls(request,false);int fps=videoMode?videoChoice.fps:30;
        if(session instanceof CameraConstrainedHighSpeedCaptureSession){request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,new Range<>(fps,fps));CameraConstrainedHighSpeedCaptureSession high=(CameraConstrainedHighSpeedCaptureSession)session;high.setRepeatingBurst(high.createHighSpeedRequestList(request.build()),timingCallback,gl);}
        else{Range<Integer> best=null;Range<Integer>[] ranges=characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);if(ranges!=null)for(Range<Integer> r:ranges)if(rawVideoMode()?(r.getUpper()>=fps&&(best==null||r.getUpper()<best.getUpper()||(r.getUpper().equals(best.getUpper())&&r.getLower()<best.getLower()))):(r.getUpper()==fps&&(best==null||r.getLower()>best.getLower())))best=r;if(best!=null)request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,best);session.setRepeatingRequest(request.build(),timingCallback,gl);}
    }catch(Exception e){error(context.getString(R.string.ui_could_not_apply_camera_settings),e);}}
    void initGl(SurfaceTexture target)throws IOException{
        display=EglLease.acquire();int[] attrs={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,0x3142,1,EGL14.EGL_NONE};EGLConfig[] configs=new EGLConfig[1];int[] n=new int[1];
        if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,n,0)||n[0]==0)throw new IllegalStateException("EGL config");eglConfig=configs[0];eglContext=EGL14.eglCreateContext(display,eglConfig,EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);displaySurface=new Surface(target);window=windowFor(displaySurface);current(window);
        String shader=PhotoRenderer.shaderSource(context);previewChain=new EffectChain(shader,true);blitChain=new EffectChain(shader,false);
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,n,0);maxTexture=n[0];Log.i("Signal","GPU="+GLES20.glGetString(GLES20.GL_RENDERER)+" maxTexture="+maxTexture);initCameraTexture();
    }
    void initCameraTexture(){int[] ids=new int[1];GLES20.glGenTextures(1,ids,0);texture=ids[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);cameraTexture=new SurfaceTexture(texture);cameraTexture.setOnFrameAvailableListener(st->{if(st==cameraTexture)frame();},gl);}
    EGLSurface windowFor(Surface surface){EGLSurface s=EGL14.eglCreateWindowSurface(display,eglConfig,surface,new int[]{EGL14.EGL_NONE},0);if(s==EGL14.EGL_NO_SURFACE)throw new IllegalStateException("EGL surface "+EGL14.eglGetError());return s;}
    void current(EGLSurface surface){if(!EGL14.eglMakeCurrent(display,surface,surface,eglContext))throw new IllegalStateException("EGL current");}
    void blit(SignalBuffer signal,int w,int h){blitChain.render(signal.texture,false,IDENTITY,cleanFrame,w,h,signal.width,signal.height,0);}
    void previewPresented(long timestamp){if(presentedFrames.acknowledge(timestamp))gl.post(()->{if(frameSeen&&!photoBusy)ready(!rawVideoMode()||rawFrameSeen);});}
    void frame(){if(!attached||cameraTexture==null||camera==null)return;try{
        current(window);cameraTexture.updateTexImage();cameraTexture.getTransformMatrix(matrix);long timestamp=cameraTexture.getTimestamp();if(timestamp<=lastFrameNs)return;lastFrameNs=timestamp;
        long arrival=SystemClock.elapsedRealtimeNanos();faults.advance(arrival*1e-9,faultInputs.frame(timestamp,arrival,recorder),faultConfig);
        EffectState.Frame state=faultFrame(previewEffects==null?effectState:previewEffects);
        boolean show=lastFrameNs-lastPreviewNs>=30_000_000L||!frameSeen;
        if(show||recording&&!rawVideoMode()){
            FrameHistory.Slot<SignalBuffer> slot=show?presentedFrames.acquire():null;
            SignalBuffer rendered=slot==null?encoderScratch:slot.value;
            if(slot!=null||recording&&!rawVideoMode()){
                try{rendered.allocate(signalW,signalH);previewChain.render(texture,true,matrix,state,signalW,signalH,signalW,signalH,rendered.fbo);rendered.frame=state;}
                catch(Exception failure){if(slot!=null)presentedFrames.abandon(slot);throw failure;}
            }
            if(slot!=null){
                try{
                    blit(rendered,width,height);rendered.presentedAt=System.currentTimeMillis();
                    // Camera clocks may differ from EGL's monotonic clock. The presentation token
                    // identifies this buffer; the immutable payload retains the original cameraNs.
                    long presentationNs=System.nanoTime();presentedFrames.publish(slot,presentationNs);
                    if(!EGLExt.eglPresentationTimeANDROID(display,window,presentationNs)||!EGL14.eglSwapBuffers(display,window))throw new IllegalStateException("Preview presentation");
                }catch(Exception failure){presentedFrames.abandon(slot);throw failure;}
                lastPreviewNs=lastFrameNs;
                int shownMask=0;for(int id:state.ids())shownMask|=1<<id;
                if(shownMask!=liveUiMask||arrival-liveUiNs>150_000_000L){liveUiMask=shownMask;liveUiNs=arrival;long revision=appliedRevision;ui.post(()->{if(revision==configRevision.get())listener.liveFrame(state);});}
            }
            if(recording&&!rawVideoMode()){
                current(encoder);blit(rendered,outW,outH);
                if(encoderTimeOffset==Long.MIN_VALUE)encoderTimeOffset=System.nanoTime()-lastFrameNs;
                EGLExt.eglPresentationTimeANDROID(display,encoder,lastFrameNs+encoderTimeOffset);
                if(!EGL14.eglSwapBuffers(display,encoder))throw new IllegalStateException("Encoder surface");current(window);
            }
        }
        if(arrival-faultUiNs>500_000_000L){faultUiNs=arrival;faultStatus=faultConfig.enabled?faultInputs.summary():"LIVE FAULT OFF";}
        if(!frameSeen){frameSeen=true;ready(presentedFrames.acknowledged()>0&&!photoBusy&&(!rawVideoMode()||rawFrameSeen));status("LIVE · "+description());fpsStart=lastFrameNs;frameCount=0;}
        frameCount++;long elapsed=lastFrameNs-fpsStart;if(elapsed>=2_000_000_000L){float measured=frameCount*1e9f/elapsed;ui.post(()->listener.fps(measured));frameCount=0;fpsStart=lastFrameNs;}
    }catch(Exception e){if(recording)stopVideo();error(context.getString(R.string.ui_image_processing_error),e);}}
    static final class PendingPhoto {
        final CameraCharacteristics cameraInfo;final CaptureSettings settings;final CameraOptions.Photo choice;
        final int rotation;final EffectState.Frame frame;final boolean front;final Location location;final long taken;
        byte[] bytes;Bitmap signal;TotalCaptureResult result;boolean dispatched;
        PendingPhoto(GlitchEngine e,SignalBuffer displayed){cameraInfo=e.characteristics;settings=new CaptureSettings(e.settings);choice=e.photoChoice;
            frame=displayed.frame;front=e.front;rotation=e.sensorRotation;location=settings.location?e.position.get():null;
            taken=settings.photoFormat==0?displayed.presentedAt:System.currentTimeMillis();
        }
        String description(){return BuildConfig.APP_NAME+" "+BuildConfig.VERSION_NAME+" | "+(settings.photoFormat==1?"RAW ORIGINAL":Effects.chainName(frame.ids())+" | "+frame.describe())+" | "+(settings.photoFormat==0?"Displayed RGB signal":settings.photoFormat==1?"Separate RAW exposure":"Separate RAW exposure, latched fault state; RGB preview approximate");}
    }
    void photo(){photo(presentedFrames.acknowledged());}
    void photo(long displayedTimestamp){
        FrameHistory.Lease<SignalBuffer> lease=presentedFrames.reserve(displayedTimestamp);
        gl.post(()->{
        if(!frameSeen||photoBusy||videoMode||!presentedFrames.valid(lease)||(settings.photoFormat!=0&&stillReader==null)){presentedFrames.release(lease);ready(frameSeen&&!photoBusy&&presentedFrames.acknowledged()>0);return;}
        photoBusy=true;ready(false);PendingPhoto shot=new PendingPhoto(this,lease.value);pending=shot;
        try{
            if(settings.photoFormat==0){
                current(window);shot.signal=lease.value.read();shot.result=signalMetadata.get(shot.frame.cameraNs);
                shot.dispatched=true;pending=null;status(context.getString(R.string.fault_capture_signal));files.execute(()->savePhoto(shot));return;
            }
            status(context.getString(R.string.ui_capturing_at_full_resolution));
            CaptureRequest.Builder still=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);still.addTarget(stillReader.getSurface());applyControls(still,true);
            if(photoChoice.maximumPixelMode)still.set(CaptureRequest.SENSOR_PIXEL_MODE,CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
            session.capture(still.build(),new CameraCaptureSession.CaptureCallback(){public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult result){if(pending==shot){shot.result=result;dispatchPhoto(shot);}}public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure failure){if(pending==shot){pending=null;photoBusy=false;ready(frameSeen);status(context.getString(R.string.ui_could_not_capture_the_photo_try_lower_settings));}}},gl);
            gl.postDelayed(()->{if(pending==shot&&!shot.dispatched){pending=null;photoBusy=false;ready(frameSeen);status(context.getString(R.string.ui_photo_capture_timed_out));}},20000);
        }catch(Exception e){if(shot.signal!=null)shot.signal.recycle();pending=null;photoBusy=false;ready(frameSeen);error(context.getString(R.string.ui_could_not_capture),e);}
        catch(OutOfMemoryError e){if(shot.signal!=null)shot.signal.recycle();pending=null;photoBusy=false;ready(frameSeen);status(context.getString(R.string.ui_not_enough_memory_to_process_the_photo_lower));}
        finally{presentedFrames.release(lease);}
    });}
    void rawImage(ImageReader reader,int ticket){try(Image image=reader.acquireNextImage()){
        if(image==null||ticket!=generation)return;
        if(rawProbe!=null)rawProbe.image(image);else if(rawRecorder!=null)rawRecorder.image(image);
    }catch(Exception e){if(ticket==generation){if(rawRecorder!=null)rawRecorder.fail(e);else failRawSession(context.getString(R.string.ui_could_not_read_raw_frames));}}}
    void imageAvailable(ImageReader reader,int ticket){try(Image image=reader.acquireNextImage()){
        if(image==null||ticket!=generation||pending==null)return;PendingPhoto shot=pending;Image.Plane plane=image.getPlanes()[0];ByteBuffer source=plane.getBuffer();
        if(image.getFormat()==ImageFormat.JPEG){shot.bytes=new byte[source.remaining()];source.get(shot.bytes);}
        else{int w=image.getWidth(),h=image.getHeight(),rowStride=plane.getRowStride(),pixelStride=plane.getPixelStride();shot.bytes=new byte[w*h*2];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int src=y*rowStride+x*pixelStride,dst=(y*w+x)*2;shot.bytes[dst]=source.get(src);shot.bytes[dst+1]=source.get(src+1);}}
        dispatchPhoto(shot);
    }catch(Exception e){pending=null;photoBusy=false;ready(frameSeen);error(context.getString(R.string.ui_could_not_read_capture_data),e);}}
    void dispatchPhoto(PendingPhoto shot){if(shot.bytes==null||shot.result==null||shot.dispatched)return;shot.dispatched=true;pending=null;status(shot.settings.photoFormat==0?context.getString(R.string.ui_saving_full_resolution_effects):context.getString(R.string.ui_saving_raw_data));files.execute(()->savePhoto(shot));}
    void savePhoto(PendingPhoto shot){Uri uri=null;File temp=null;Bitmap bitmap=null;try{
        boolean raw=shot.settings.photoFormat!=0;int w,h;
        if(!raw){bitmap=shot.signal;shot.signal=null;w=bitmap.getWidth();h=bitmap.getHeight();temp=File.createTempFile("signal-photo-",".jpg",context.getCacheDir());try(OutputStream out=new FileOutputStream(temp)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,shot.settings.jpegQuality,out))throw new IOException("JPEG compress");}bitmap.recycle();bitmap=null;PhotoMetadata.write(temp,shot.bytes,shot.result,shot.taken,w,h,shot.location,shot.description());uri=createMedia("jpg",shot.taken);try(InputStream in=new FileInputStream(temp);OutputStream out=context.getContentResolver().openOutputStream(uri)){copy(in,out);}}
        else{w=shot.choice.size.getWidth();h=shot.choice.size.getHeight();Integer white=shot.cameraInfo.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);BlackLevelPattern blacks=shot.cameraInfo.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);int black=blacks==null?0:blacks.getOffsetForIndex(0,0);byte[] data=shot.settings.photoFormat==2?RawGlitch.chain(shot.bytes,w,h,white==null?65535:white,black,shot.frame):shot.bytes;
            uri=createMedia("dng",shot.taken);try(DngCreator dng=new DngCreator(shot.cameraInfo,shot.result);OutputStream out=context.getContentResolver().openOutputStream(uri)){dng.setDescription(shot.description());dng.setOrientation(shot.front?(shot.rotation==270?5:7):(shot.rotation==90?6:shot.rotation==270?8:1));if(shot.location!=null)dng.setLocation(shot.location);ByteBuffer packed=ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());packed.put(data).flip();dng.writeByteBuffer(out,new Size(w,h),packed,0);}}
        publish(uri,false,shot.taken);Log.i("Signal","Photo saved format="+(raw?"DNG":"JPEG")+" "+w+"x"+h+" GPS="+(shot.location!=null)+" effect="+Effects.chainName(shot.frame.ids())+" LEVEL="+shot.frame.amount);
        if(shot.settings.location&&shot.location==null)status(context.getString(R.string.ui_no_location_fix_saved_without_gps));
    }catch(Exception e){discard(uri);error(context.getString(R.string.ui_could_not_save_the_photo),e);}catch(OutOfMemoryError e){discard(uri);status(context.getString(R.string.ui_not_enough_memory_to_process_the_photo_lower));}
    finally{if(bitmap!=null)bitmap.recycle();if(temp!=null)temp.delete();gl.post(()->{photoBusy=false;ready(frameSeen&&attached);});}}
    static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    Uri createMedia(String extension,long taken){boolean video=extension.equals("mp4");ContentValues values=new ContentValues();String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date(taken));values.put(MediaStore.MediaColumns.DISPLAY_NAME,BuildConfig.APP_NAME+"_"+stamp+"."+extension);values.put(MediaStore.MediaColumns.MIME_TYPE,video?"video/mp4":extension.equals("dng")?"image/x-adobe-dng":"image/jpeg");values.put(MediaStore.MediaColumns.RELATIVE_PATH,(video?Environment.DIRECTORY_MOVIES:Environment.DIRECTORY_PICTURES)+"/5igna1");values.put(MediaStore.MediaColumns.IS_PENDING,1);values.put(MediaStore.MediaColumns.DATE_TAKEN,taken);Uri result=context.getContentResolver().insert(video?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(result==null)throw new IllegalStateException("MediaStore insert");return result;}
    void publish(Uri uri,boolean video,long taken){ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.IS_PENDING,0);values.put(MediaStore.MediaColumns.DATE_TAKEN,taken);context.getContentResolver().update(uri,values,null,null);Log.i("Signal","Saved "+uri+" video="+video);ui.post(()->listener.saved(uri,video));}
    void discard(Uri uri){if(uri!=null)try{context.getContentResolver().delete(uri,null,null);}catch(Exception e){Log.w("Signal","Cleanup",e);}}
    void toggleVideo(boolean sound){gl.post(()->{if(recording)stopVideo();else startVideo(sound);});}
    void startVideo(boolean sound){
        if(previewEffects!=null){status(context.getString(R.string.fault_recording_draft));ready(true);return;}if(!frameSeen||!attached||recording||photoBusy||!videoMode)return;
        if(rawVideoMode()){if(!rawFrameSeen||rawReader==null){status(context.getString(R.string.ui_checking_raw_output));ready(false);return;}rawRecorder=new RawVideoRecorder(this);recording=true;ready(true);ui.post(()->listener.recording(true));gl.post(storageWatch);return;}
        ready(false);try{
        if(sound){faultInputs.stopMic();recorderAudio=true;faultInputs.configure(faultConfig,attached,true);}
        videoTaken=System.currentTimeMillis();videoUri=createMedia("mp4",videoTaken);videoFd=context.getContentResolver().openFileDescriptor(videoUri,"w");recorder=new MediaRecorder(context);
        if(sound)recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setOutputFile(videoFd.getFileDescriptor());recorder.setVideoEncoder(settings.codec.equals("video/hevc")?MediaRecorder.VideoEncoder.HEVC:MediaRecorder.VideoEncoder.H264);recorder.setVideoSize(outW,outH);recorder.setVideoFrameRate(videoChoice.fps);int bitrate=options.bitrate(videoChoice,settings);recorder.setVideoEncodingBitRate(bitrate);recorder.setMaxFileSize(segmentBytes);
        if(sound){recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioSamplingRate(48000);recorder.setAudioEncodingBitRate(192000);recorder.setAudioChannels(1);}
        Location geo=settings.location?position.get():null;if(geo!=null)recorder.setLocation((float)geo.getLatitude(),(float)geo.getLongitude());
        recorder.setOnErrorListener((r,what,extra)->gl.post(()->{if(recording){stopVideo();status(context.getString(R.string.ui_recording_interrupted_check_the_saved_files));}}));
        recorder.setOnInfoListener((r,what,extra)->{if(!recording)return;if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING)prepareNextSegment();else if(what==MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED)advanceSegment();else if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED){stopVideo();status(context.getString(R.string.ui_could_not_start_the_next_file_recording_saved));}});
        recorder.prepare();encoderSurface=recorder.getSurface();encoder=windowFor(encoderSurface);recorder.start();encoderTimeOffset=Long.MIN_VALUE;recording=true;ready(true);ui.post(()->listener.recording(true));gl.post(storageWatch);Log.i("Signal","Recording started "+outW+"x"+outH+" fps="+videoChoice.fps+" codec="+settings.codec+" bitrate="+bitrate+" audio="+sound+" GPS="+(geo!=null));
        if(settings.location&&geo==null)status(context.getString(R.string.ui_no_gps_fix_video_will_have_no_location));
    }catch(Exception e){releaseRecorder();discard(videoUri);videoUri=null;ready(frameSeen);ui.post(()->listener.recording(false));error(context.getString(R.string.ui_could_not_start_recording_try_lower_settings),e);}}
    void prepareNextSegment(){if(nextUri!=null)return;try{nextTaken=System.currentTimeMillis();nextUri=createMedia("mp4",nextTaken);nextFd=context.getContentResolver().openFileDescriptor(nextUri,"w");recorder.setNextOutputFile(nextFd.getFileDescriptor());}catch(Exception e){discard(nextUri);nextUri=null;if(nextFd!=null)try{nextFd.close();}catch(IOException ignored){}nextFd=null;error(context.getString(R.string.ui_could_not_prepare_the_next_recording_file),e);}}
    void advanceSegment(){if(nextUri==null)return;Uri completed=videoUri;long taken=videoTaken;try{videoFd.close();}catch(IOException ignored){}videoUri=nextUri;videoFd=nextFd;videoTaken=nextTaken;nextUri=null;nextFd=null;try{publish(completed,true,taken);Log.i("Signal","Recording continued in next segment");}catch(Exception e){error(context.getString(R.string.ui_could_not_publish_the_recording_segment),e);}}
    void stopVideo(){if(!recording)return;
        if(rawRecorder!=null){recording=false;photoBusy=true;gl.removeCallbacks(storageWatch);ready(false);RawVideoRecorder completed=rawRecorder;rawRecorder=null;completed.stop();ui.post(()->listener.recording(false));status(context.getString(R.string.ui_finalizing_raw_sequence));return;}
        recording=false;gl.removeCallbacks(storageWatch);ready(false);Uri result=videoUri;long taken=videoTaken;boolean ok=false;try{recorder.stop();ok=true;}catch(Exception e){error(context.getString(R.string.ui_recording_was_too_short_or_could_not_be),e);}finally{releaseRecorder();videoUri=null;ready(frameSeen&&attached);ui.post(()->listener.recording(false));}if(ok)try{publish(result,true,taken);}catch(Exception e){discard(result);error(context.getString(R.string.ui_could_not_save_the_video),e);}else discard(result);}
    void releaseRecorder(){if(display!=EGL14.EGL_NO_DISPLAY&&window!=EGL14.EGL_NO_SURFACE)current(window);if(encoder!=EGL14.EGL_NO_SURFACE){EGL14.eglDestroySurface(display,encoder);encoder=EGL14.EGL_NO_SURFACE;}if(encoderSurface!=null){encoderSurface.release();encoderSurface=null;}if(recorder!=null){recorder.release();recorder=null;}if(videoFd!=null){try{videoFd.close();}catch(IOException ignored){}videoFd=null;}if(nextFd!=null){try{nextFd.close();}catch(IOException ignored){}nextFd=null;}discard(nextUri);nextUri=null;recorderAudio=false;faultInputs.configure(faultConfig,attached,false);}
    void closeCamera(){if(rawProbe!=null){rawProbe.stop();rawProbe=null;}rawFrameSeen=false;faultInputs.resetTiming();generation++;presentedFrames.clear();encoderScratch.frame=null;signalMetadata.clear();frameSeen=false;lastFrameNs=0;lastPreviewNs=0;ready(false);if(recording)stopVideo();if(pending!=null){pending=null;photoBusy=false;status(context.getString(R.string.ui_capture_cancelled_because_the_camera_closed_before_completion));}if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}if(cameraSurface!=null){cameraSurface.release();cameraSurface=null;}if(stillReader!=null){stillReader.close();stillReader=null;}if(rawReader!=null){rawReader.close();rawReader=null;}request=null;}
    void close(){displayTarget=null;attached=false;closeCamera();faultInputs.stop();faults.reset();if(cameraTexture!=null){cameraTexture.setOnFrameAvailableListener(null);cameraTexture.release();cameraTexture=null;}if(display!=EGL14.EGL_NO_DISPLAY){if(eglContext!=EGL14.EGL_NO_CONTEXT&&window!=EGL14.EGL_NO_SURFACE){current(window);for(SignalBuffer buffer:presentedFrames.values())buffer.release();encoderScratch.release();if(previewChain!=null)previewChain.release();if(blitChain!=null)blitChain.release();previewChain=blitChain=null;}EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(window!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,window);if(eglContext!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,eglContext);EglLease.release();EGL14.eglReleaseThread();}display=EGL14.EGL_NO_DISPLAY;window=EGL14.EGL_NO_SURFACE;eglContext=EGL14.EGL_NO_CONTEXT;if(displaySurface!=null){displaySurface.release();displaySurface=null;}}
}
