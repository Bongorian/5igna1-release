package com.bongorian.signa1;

import android.content.ContentValues;
import android.hardware.camera2.*;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Size;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

/** Original RAW_SENSOR frames + matching per-frame metadata. All file writes use the engine's worker.
 * At most two copied frames are held; overload drops frames, never grows an unbounded capture queue. */
final class RawVideoRecorder {
    private static final java.util.concurrent.atomic.AtomicBoolean recovered=new java.util.concurrent.atomic.AtomicBoolean();
    static void recoverPending(GlitchEngine engine){if(!recovered.compareAndSet(false,true))return;engine.files.execute(()->{
        String selection=MediaStore.MediaColumns.IS_PENDING+"=1 AND "+MediaStore.MediaColumns.OWNER_PACKAGE_NAME+"=? AND "+MediaStore.MediaColumns.DISPLAY_NAME+" LIKE ?";
        try(android.database.Cursor rows=engine.context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,new String[]{MediaStore.MediaColumns._ID},selection,new String[]{engine.context.getPackageName(),"5igna1_%_RAW_%.zip"},null)){if(rows!=null)while(rows.moveToNext())engine.discard(android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,rows.getLong(0)));}catch(Exception error){android.util.Log.w("Signal","RAW draft cleanup",error);}
    });}
    final boolean probe;
    final GlitchEngine engine;final CameraCharacteristics characteristics;final Size size;final int fps,orientation;
    final Location location;final long taken=System.currentTimeMillis();final String name;
    final AtomicInteger inFlight=new AtomicInteger();
    final TreeMap<Long,byte[]> pending=new TreeMap<>();final TreeMap<Long,TotalCaptureResult> results=new TreeMap<>();
    boolean accepting=true;volatile boolean failed;volatile int written,dropped;long acceptedNs;
    Uri uri;ZipOutputStream zip;CountingStream output;int segment=0,segmentFrames;long firstNs,lastNs;
    StringBuilder timestamps=new StringBuilder();
    RawVideoRecorder(GlitchEngine engine){this(engine,false);}
    RawVideoRecorder(GlitchEngine engine,boolean probe){this.probe=probe;this.engine=engine;characteristics=engine.characteristics;size=engine.rawVideoChoice.size;fps=engine.videoChoice.fps;orientation=engine.front?(engine.sensorRotation==270?5:7):(engine.sensorRotation==90?6:engine.sensorRotation==270?8:1);location=engine.settings.location?engine.position.get():null;name=BuildConfig.APP_NAME+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date(taken))+"_RAW";}
    void result(TotalCaptureResult result){if(!accepting)return;Long stamp=result.get(CaptureResult.SENSOR_TIMESTAMP);if(stamp==null)return;results.put(stamp,result);while(results.size()>32)results.pollFirstEntry();pair(stamp);}
    void image(Image image){if(!accepting||failed||(probe&&acceptedNs!=0))return;long stamp=image.getTimestamp();
        if(acceptedNs!=0&&stamp-acceptedNs<1_000_000_000L/fps-1_000_000L)return;
        if(inFlight.get()>=2){dropped++;expire(stamp);return;}acceptedNs=stamp;inFlight.incrementAndGet();
        try{Image.Plane plane=image.getPlanes()[0];ByteBuffer source=plane.getBuffer();int w=image.getWidth(),h=image.getHeight(),stride=plane.getRowStride(),pixel=plane.getPixelStride();byte[] bytes=new byte[w*h*2];
            for(int y=0;y<h;y++){if(pixel==2){source.position(y*stride);source.get(bytes,y*w*2,w*2);}else for(int x=0;x<w;x++){int src=y*stride+x*pixel,dst=(y*w+x)*2;bytes[dst]=source.get(src);bytes[dst+1]=source.get(src+1);}}
            pending.put(stamp,bytes);pair(stamp);expire(stamp);
        }catch(Exception|AssertionError|OutOfMemoryError error){inFlight.decrementAndGet();fail(error);}
    }
    void expire(long stamp){while(!pending.isEmpty()&&stamp-pending.firstKey()>1_000_000_000L){pending.pollFirstEntry();inFlight.decrementAndGet();dropped++;}}
    void pair(long stamp){byte[] bytes=pending.get(stamp);TotalCaptureResult metadata=results.get(stamp);if(bytes==null||metadata==null)return;pending.remove(stamp);results.remove(stamp);engine.files.execute(()->{try{if(!failed)write(bytes,stamp,metadata);}catch(Exception|AssertionError|OutOfMemoryError error){fail(error);}finally{inFlight.decrementAndGet();}});}
    void fail(Throwable error){if(failed)return;failed=true;android.util.Log.e("Signal","RAW sequence",error);engine.gl.post(()->{if(engine.rawProbe==this){engine.failRawSession(engine.context.getString(R.string.ui_required_raw_metadata_for_dng_is_unavailable));return;}if(engine.rawRecorder==this)engine.stopVideo();engine.status(engine.context.getString(R.string.ui_raw_recording_stopped_previously_saved_segments_are_kept));});}
    void open()throws IOException{
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name+String.format(Locale.US,"_%03d.zip",++segment));values.put(MediaStore.MediaColumns.MIME_TYPE,"application/zip");values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/5igna1");values.put(MediaStore.MediaColumns.IS_PENDING,1);
        uri=engine.context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("RAW MediaStore insert");OutputStream stream=engine.context.getContentResolver().openOutputStream(uri);if(stream==null)throw new IOException("RAW output stream");output=new CountingStream(new BufferedOutputStream(stream,256*1024));zip=new ZipOutputStream(output);zip.setLevel(0);segmentFrames=0;timestamps=new StringBuilder("frame,sensor_timestamp_ns,exposure_ns,iso\n");firstNs=lastNs=0;
    }
    void write(byte[] bytes,long stamp,TotalCaptureResult metadata)throws IOException{
        if(probe){try(DngCreator creator=new DngCreator(characteristics,metadata)){creator.writeByteBuffer(new OutputStream(){public void write(int value){}public void write(byte[] bytes,int offset,int count){}},size,ByteBuffer.wrap(bytes),0);}engine.gl.post(()->{if(engine.rawProbe==this){stop();engine.rawProbe=null;engine.rawFrameSeen=true;engine.ready(engine.frameSeen&&!engine.photoBusy);engine.status(engine.context.getString(R.string.ui_raw_output_and_dng_encoding_verified)+engine.description());}});return;}
        if(new StatFs(Environment.getExternalStorageDirectory().getPath()).getAvailableBytes()<Math.max(256_000_000L,bytes.length*4L))throw new IOException("Low storage");
        if(zip==null)open();String frame=String.format(Locale.US,"frame_%08d.dng",written);
        zip.putNextEntry(new ZipEntry(frame));try(DngCreator creator=new DngCreator(characteristics,metadata)){creator.setOrientation(orientation);creator.setDescription(BuildConfig.APP_NAME+" RAW video / original sensor frame / timestamp_ns="+stamp);if(location!=null)creator.setLocation(location);creator.writeByteBuffer(zip,size,ByteBuffer.wrap(bytes),0);}zip.closeEntry();
        if(firstNs==0)firstNs=stamp;lastNs=stamp;Long exposure=metadata.get(CaptureResult.SENSOR_EXPOSURE_TIME);Integer iso=metadata.get(CaptureResult.SENSOR_SENSITIVITY);timestamps.append(frame).append(',').append(stamp).append(',').append(exposure==null?0:exposure).append(',').append(iso==null?0:iso).append('\n');written++;segmentFrames++;
        if(output.count>=3_500_000_000L)finishSegment();
    }
    void entry(String name,String text)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    void finishSegment()throws IOException{if(zip==null)return;
        entry("timestamps.csv",timestamps.toString());double measured=segmentFrames>1&&lastNs>firstNs?(segmentFrames-1)*1e9/(lastNs-firstNs):0;
        entry("manifest.json",String.format(Locale.US,"{\"format\":\"DNG sequence\",\"width\":%d,\"height\":%d,\"requested_fps\":%d,\"measured_fps\":%.5f,\"frames\":%d,\"session_dropped_frames\":%d,\"audio\":false,\"effects_applied\":false,\"segment\":%d}",size.getWidth(),size.getHeight(),fps,measured,segmentFrames,dropped,segment));
        entry("README.txt",engine.context.getString(R.string.ui_5igna1_raw_video_dng_sequence_extract_the_zip));
        zip.close();zip=null;Uri completed=uri;uri=null;ContentValues published=new ContentValues();published.put(MediaStore.MediaColumns.IS_PENDING,0);engine.context.getContentResolver().update(completed,published,null,null);engine.ui.post(()->engine.listener.saved(completed,true));
    }
    void stop(){if(!accepting)return;accepting=false;dropped+=pending.size();inFlight.addAndGet(-pending.size());pending.clear();results.clear();if(probe)return;
        engine.files.execute(()->{try{if(!failed&&written>0)finishSegment();else{if(zip!=null)zip.close();zip=null;engine.discard(uri);uri=null;}}catch(Exception error){if(zip!=null)try{zip.close();}catch(IOException ignored){}zip=null;engine.discard(uri);uri=null;android.util.Log.e("Signal","Finalize RAW sequence",error);failed=true;}finally{engine.gl.post(()->{engine.photoBusy=false;engine.ready(engine.frameSeen&&engine.attached);engine.status(failed?engine.context.getString(R.string.ui_could_not_save_raw_video):written==0?engine.context.getString(R.string.ui_no_raw_frames_were_captured):engine.context.getString(R.string.ui_raw_saved)+written+engine.context.getString(R.string.ui_frames_dropped)+dropped);});}});
    }
    static final class CountingStream extends FilterOutputStream{long count;CountingStream(OutputStream out){super(out);}public void write(int b)throws IOException{out.write(b);count++;}public void write(byte[] b,int off,int len)throws IOException{out.write(b,off,len);count+=len;}}
}
