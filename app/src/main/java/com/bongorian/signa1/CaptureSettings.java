package com.bongorian.signa1;

import android.content.SharedPreferences;

final class CaptureSettings {
    // 0: JPG; 2: sensor-processed RAW. Legacy original-RAW preference 1 migrates to 2.
    int photoFormat, jpegQuality=95, videoQuality=1;
    String photoSize="recommended", videoKey="recommended", codec="video/avc";
    boolean location,rawVideo,expertMode;String rawVideoSize="";int rawVideoFps=12;
    CaptureSettings() {}
    CaptureSettings(CaptureSettings other) { expertMode=other.expertMode;photoFormat=other.photoFormat; jpegQuality=other.jpegQuality; videoQuality=other.videoQuality; photoSize=other.photoSize; videoKey=other.videoKey; codec=other.codec; location=other.location;rawVideo=other.rawVideo;rawVideoSize=other.rawVideoSize;rawVideoFps=other.rawVideoFps; }
    static CaptureSettings load(SharedPreferences p) {
        CaptureSettings s=new CaptureSettings(); s.expertMode=p.getBoolean("expertMode",false); s.photoFormat=p.getInt("photoFormat",0);if(s.photoFormat==1)s.photoFormat=2; s.jpegQuality=p.getInt("jpegQuality",95); s.videoQuality=p.getInt("videoQuality",1); s.photoSize=p.getString("photoSize","recommended"); s.videoKey=p.getString("videoKey","recommended"); if(!p.getBoolean("loadRecommendationsV1",false)&&"max".equals(s.photoSize))s.photoSize="recommended";if(s.videoKey.isEmpty())s.videoKey="recommended"; s.codec=p.getString("codec","video/avc"); s.location=p.getBoolean("location",false);s.rawVideo=p.getBoolean("rawVideo",false);s.rawVideoSize=p.getString("rawVideoSize","");s.rawVideoFps=Math.max(1,Math.min(30,p.getInt("rawVideoFps",12)));return s;
    }
    void save(SharedPreferences p) { p.edit().putBoolean("expertMode",expertMode).putBoolean("loadRecommendationsV1",true).putInt("photoFormat",photoFormat).putInt("jpegQuality",jpegQuality).putInt("videoQuality",videoQuality).putString("photoSize",photoSize).putString("videoKey",videoKey).putString("codec",codec).putBoolean("location",location).putBoolean("rawVideo",rawVideo).putString("rawVideoSize",rawVideoSize).putInt("rawVideoFps",rawVideoFps).apply(); }
}
