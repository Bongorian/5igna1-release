package com.bongorian.signa1;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.*;
import android.util.*;
import java.util.*;

final class CameraOptions {
    static final class Photo {
        final Size size; final boolean maximumPixelMode;
        Photo(Size s,boolean m){size=s;maximumPixelMode=m;}
        String key(){return size.toString()+(maximumPixelMode?"@max":"");}
        String label(android.content.Context context){return String.format(Locale.US,"%.1f MP · %d × %d%s",area(size)/1e6,size.getHeight(),size.getWidth(),maximumPixelMode?context.getString(R.string.ui_high_resolution_sensor):"");}
    }
    static final class Video {
        final Size size; final int fps; final boolean highSpeed;
        Video(Size s,int f,boolean high){size=s;fps=f;highSpeed=high;}
        String key(){return size+"@"+fps+(highSpeed?"h":"");}
        String label(android.content.Context context){return (size.getWidth()==3840?"4K · ":size.getWidth()==1920?"FHD · ":size.getWidth()==1280?"HD · ":"")+size.getHeight()+" × "+size.getWidth()+" / "+fps+" fps"+(highSpeed?context.getString(R.string.ui_no_effects):"");}
    }
    static final class RawVideo {
        final Size size;final int maxFps;
        RawVideo(Size size,int fps){this.size=size;maxFps=fps;}
        String label(android.content.Context context){return size+context.getString(R.string.ui_estimated_raw_limit)+maxFps+" fps";}
    }
    final List<RawVideo> rawVideos=new ArrayList<>();volatile String rawVideoFailure;
    RawVideo rawVideo(CaptureSettings s){for(RawVideo v:rawVideos)if(v.size.toString().equals(s.rawVideoSize))return v;return rawVideos.isEmpty()?null:rawVideos.get(0);}
    boolean rawVideoAvailable(){return !rawVideos.isEmpty()&&rawVideoFailure==null;}
    String rawVideoReason(android.content.Context context){return rawVideoFailure!=null?rawVideoFailure:rawVideos.isEmpty()?context.getString(R.string.ui_continuous_raw_output_is_unavailable_on_this_camera):context.getString(R.string.ui_raw_output_available_checked_when_selected);}
    final List<Photo> photos=new ArrayList<>(), raws=new ArrayList<>();
    final List<Video> videos=new ArrayList<>();
    final Map<String,List<MediaCodecInfo.VideoCapabilities>> encoders=new HashMap<>();
    final CameraCharacteristics characteristics;
    final StreamConfigurationMap map;
    final String id;long recommendedPhotoPixels=2_073_600,recommendedVideoPixels=2_073_600;
    CameraOptions(String cameraId,CameraCharacteristics cc,int maxTexture) {
        id=cameraId;characteristics=cc;map=cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(map==null)throw new IllegalArgumentException("Camera has no stream configuration");
        Size[] signals=map.getOutputSizes(SurfaceTexture.class);if(signals!=null)for(Size size:signals)if(Math.max(size.getWidth(),size.getHeight())<=maxTexture)photos.add(new Photo(size,false));
        addPhotos(raws,map,ImageFormat.RAW_SENSOR,false,Integer.MAX_VALUE);
        StreamConfigurationMap full=cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION);
        if(full!=null){addPhotos(raws,full,ImageFormat.RAW_SENSOR,true,Integer.MAX_VALUE);}
        photos.sort((a,b)->Long.compare(area(b.size),area(a.size)));raws.sort((a,b)->Long.compare(area(b.size),area(a.size)));
        for(MediaCodecInfo ci:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if(!ci.isEncoder())continue;
            for(String type:ci.getSupportedTypes()) if(type.equals("video/avc") || type.equals("video/hevc")) {
                try { MediaCodecInfo.CodecCapabilities caps=ci.getCapabilitiesForType(type); boolean surface=false;for(int format:caps.colorFormats)if(format==MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)surface=true;
                    if(surface) { encoders.computeIfAbsent(type,k->new ArrayList<>()).add(caps.getVideoCapabilities()); Log.i("SignalCaps","Encoder "+ci.getName()+" "+type+" "+caps.getVideoCapabilities().getSupportedWidths()+"x"+caps.getVideoCapabilities().getSupportedHeights()+" bitrates="+caps.getVideoCapabilities().getBitrateRange()); }
                } catch(Exception ignored){}
            }
        }
        int normalMax=0; Range<Integer>[] ranges=cc.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if(ranges!=null)for(Range<Integer> r:ranges)normalMax=Math.max(normalMax,r.getUpper());
        if(normalMax==0)normalMax=30;
        Size[] textureSizes=map.getOutputSizes(SurfaceTexture.class);
        if(textureSizes==null||textureSizes.length==0)throw new IllegalArgumentException("Camera has no preview output");
        for(Size s:textureSizes) {
            if(Math.max(s.getWidth(),s.getHeight())>maxTexture)continue;
            long ns=map.getOutputMinFrameDuration(SurfaceTexture.class,s);
            double possible=ns==0?normalMax:1e9/ns;
            for(int fps:new int[]{15,24,30,60})if(fps<=normalMax && possible+1>=fps && anyEncoder(s,fps))videos.add(new Video(s,fps,false));
        }
        int[] capabilities=cc.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);boolean rawCap=false;if(capabilities!=null)for(int capability:capabilities)if(capability==CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)rawCap=true;
        if(rawCap){Size[] rawSizes=map.getOutputSizes(ImageFormat.RAW_SENSOR);if(rawSizes!=null)for(Size size:rawSizes){
            // Bound two queued sensor frames to 128 MiB; maximum-resolution-only still modes are excluded.
            android.graphics.Rect active=cc.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);Size pixels=cc.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            boolean dngSize=(pixels!=null&&pixels.equals(size))||(active!=null&&active.width()==size.getWidth()&&active.height()==size.getHeight());
            if(!dngSize||area(size)*2>64L*1024*1024)continue;
            long duration=map.getOutputMinFrameDuration(ImageFormat.RAW_SENSOR,size),stall=map.getOutputStallDuration(ImageFormat.RAW_SENSOR,size);
            if(duration<=0)continue;int limit=Math.min(30,Math.min(normalMax,(int)(1_000_000_000L/Math.max(1,duration+stall))));if(limit>=1)rawVideos.add(new RawVideo(size,limit));
        }}
        rawVideos.sort((a,b)->Long.compare(area(a.size),area(b.size)));
        for(RawVideo v:rawVideos)Log.i("SignalCaps","RAW VIDEO "+v.size+" / max "+v.maxFps+" fps");
        // This GL processing path uses normal sessions, not constrained high-speed bursts.
        // Offer normal streams whose frame rate this processing path can deliver.
        videos.sort((a,b)->{int compare=Long.compare(area(b.size),area(a.size));return compare==0?Integer.compare(b.fps,a.fps):compare;});
        Log.i("SignalCaps","Camera "+id+" maxPixelMode="+(full!=null)+" JPEG="+labels(photos)+" RAW="+labels(raws));
        for(Video v:videos)Log.i("SignalCaps","Video "+v.key()+" HEVC="+supports(v,"video/hevc")+" AVC="+supports(v,"video/avc"));
    }
    static long area(Size s){return (long)s.getWidth()*s.getHeight();}
    static String labels(List<Photo> choices){StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(5,choices.size());i++)s.append(choices.get(i).key()).append(' ');return s.toString();}
    static void addPhotos(List<Photo> list,StreamConfigurationMap m,int format,boolean full,int maxTexture) {
        if(m==null)return;
        Size[] sizes=m.getOutputSizes(format);if(sizes!=null)for(Size s:sizes)if(Math.max(s.getWidth(),s.getHeight())<=maxTexture)list.add(new Photo(s,full));
        Size[] high=m.getHighResolutionOutputSizes(format);if(high!=null)for(Size s:high)if(Math.max(s.getWidth(),s.getHeight())<=maxTexture && list.stream().noneMatch(p->p.size.equals(s)&&p.maximumPixelMode==full))list.add(new Photo(s,full));
    }
    boolean anyEncoder(Size size,int fps){return supportsSize(size,fps,"video/hevc")||supportsSize(size,fps,"video/avc");}
    boolean supports(Video v,String codec){return supportsSize(v.size,v.fps,codec);}
    boolean supportsSize(Size s,int fps,String codec) {
        for(MediaCodecInfo.VideoCapabilities caps:encoders.getOrDefault(codec,Collections.emptyList()))try{if(caps.areSizeAndRateSupported(s.getHeight(),s.getWidth(),fps))return true;}catch(Exception ignored){}
        return false;
    }
    List<Video> videosFor(String codec){List<Video> out=new ArrayList<>();for(Video v:videos)if(supports(v,codec))out.add(v);return out;}
    Photo photo(CaptureSettings s){List<Photo> list=s.photoFormat==0?photos:raws;if(list.isEmpty())return null;for(Photo p:list)if(p.key().equals(s.photoSize))return p;if("max".equals(s.photoSize))return list.get(0);if("recommended".equals(s.photoSize)&&s.photoFormat==0){Photo fallback=null;for(Photo p:list)if(area(p.size)<=recommendedPhotoPixels){if(fallback==null)fallback=p;long duration=0;try{duration=map.getOutputMinFrameDuration(SurfaceTexture.class,p.size);}catch(IllegalArgumentException ignored){}if(duration==0||duration<=50_000_000L)return p;}return fallback==null?list.get(list.size()-1):fallback;}for(Photo p:list)if(!p.maximumPixelMode&&area(p.size)<=(s.photoFormat==0?2_073_600:12_000_000))return p;return list.get(list.size()-1);}
    Video video(CaptureSettings s){List<Video> list=videosFor(s.codec);if(list.isEmpty())return null;for(Video v:list)if(v.key().equals(s.videoKey))return v;if("recommended".equals(s.videoKey)||s.videoKey.isEmpty()){Video best=null;for(Video v:list)if(!v.highSpeed&&v.fps<=30&&area(v.size)<=recommendedVideoPixels&&(best==null||area(v.size)>area(best.size)||area(v.size)==area(best.size)&&v.fps>best.fps))best=v;if(best!=null)return best;for(Video v:list)if(!v.highSpeed&&v.fps<=30&&(best==null||area(v.size)<area(best.size)))best=v;if(best!=null)return best;return list.get(list.size()-1);}for(Video v:list)if(area(v.size)==area(list.get(0).size)&&v.fps==30)return v;return list.get(0);}
    int bitrate(Video v,CaptureSettings settings) {
        double pixels=area(v.size); double bpp=settings.videoQuality==0?.10:settings.videoQuality==1?.22:.48;
        long desired=(long)(pixels*v.fps*bpp*(settings.codec.equals("video/hevc")?.8:1));
        int ceiling=0,floor=Integer.MAX_VALUE;
        for(MediaCodecInfo.VideoCapabilities c:encoders.getOrDefault(settings.codec,Collections.emptyList()))try{if(c.areSizeAndRateSupported(v.size.getHeight(),v.size.getWidth(),v.fps)){ceiling=Math.max(ceiling,c.getBitrateRange().getUpper());floor=Math.min(floor,c.getBitrateRange().getLower());}}catch(Exception ignored){}
        if(ceiling==0)return 16000000;if(settings.videoQuality>=3)desired=ceiling;return (int)Math.max(floor,Math.min(ceiling,Math.max(4_000_000,desired)));
    }
    Size previewFor(Photo p) {
        Size best=null;double score=Double.MAX_VALUE;double aspect=(double)p.size.getWidth()/p.size.getHeight();
        for(Size s:map.getOutputSizes(SurfaceTexture.class))if(s.getWidth()<=1920&&s.getHeight()<=1080){double d=Math.abs((double)s.getWidth()/s.getHeight()-aspect)*10000+Math.abs(s.getWidth()-1440);if(d<score){score=d;best=s;}}
        return best==null?map.getOutputSizes(SurfaceTexture.class)[0]:best;
    }
}
