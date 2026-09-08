package com.bongorian.signa1;

import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** App-owned mixed-media viewer. Only the visible item is decoded; close releases playback
 * before camera attachment. No gallery permission, external player, or camera ownership overlap. */
final class MediaPreview {
    static final class Item {
        final Uri uri;final boolean video,raw;final long taken;final String name;
        Item(Uri uri,boolean video,boolean raw,long taken,String name){this.uri=uri;this.video=video;this.raw=raw;this.taken=taken;this.name=name;}
    }
    final MainActivity a;final Uri initial;final boolean initialVideo;
    final Dialog dialog;final LinearLayout panel;final FrameLayout media;
    final TextView title,page,notice,previous,next,play,time,external;final SeekBar seek;
    final ExecutorService worker=Executors.newSingleThreadExecutor();final CancellationSignal scan=new CancellationSignal();
    final List<Item> items=new ArrayList<>();int index;volatile int generation;volatile boolean closed;boolean cameraReleased,prepared,seeking,focusGranted,videoFrameSeen;
    ImageView image;TextureView video;MediaPlayer player;Surface videoSurface;float zoom=1;int videoW,videoH;
    final AudioManager audio;AudioFocusRequest focus;
    MediaPreview(MainActivity activity,Uri uri,boolean isVideo){
        a=activity;initial=uri;initialVideo=isVideo;audio=(AudioManager)a.getSystemService(Context.AUDIO_SERVICE);
        dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackgroundColor(MainActivity.BG);panel.setPadding(a.dp(16),a.dp(8),a.dp(16),a.dp(8));
        LinearLayout header=a.row();TextView close=a.button("×");close.setTextSize(24);close.setBackgroundColor(Color.TRANSPARENT);close.setContentDescription(a.getString(R.string.ui_close));header.addView(close,new LinearLayout.LayoutParams(a.dp(44),a.dp(48)));close.setOnClickListener(v->dismiss());
        title=a.text(a.getString(R.string.media_preview_title),13,MainActivity.WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setGravity(Gravity.CENTER);header.addView(title,new LinearLayout.LayoutParams(0,a.dp(48),1));external=a.button("↗");external.setTextSize(23);external.setContentDescription(a.getString(R.string.media_open_external));external.setTooltipText(external.getContentDescription());external.setBackgroundColor(Color.TRANSPARENT);header.addView(external,new LinearLayout.LayoutParams(a.dp(44),a.dp(48)));external.setOnClickListener(v->{if(!items.isEmpty())a.openExternal(items.get(index).uri);});panel.addView(header);
        media=new FrameLayout(a);media.setClipChildren(true);media.setContentDescription(a.getString(R.string.media_gestures));panel.addView(media,new LinearLayout.LayoutParams(-1,0,1));media.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitVideo());
        notice=a.text(a.getString(R.string.media_loading),13,MainActivity.MUTED);notice.setGravity(Gravity.CENTER);notice.setPadding(a.dp(20),a.dp(20),a.dp(20),a.dp(20));
        LinearLayout footer=new LinearLayout(a);footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(0,a.dp(12),0,0);time=a.text("",11,MainActivity.MUTED);time.setGravity(Gravity.CENTER);footer.addView(time,new LinearLayout.LayoutParams(-1,a.dp(22)));
        seek=new SeekBar(a);seek.setMax(1000);seek.setContentDescription(a.getString(R.string.media_position));seek.setProgressTintList(android.content.res.ColorStateList.valueOf(MainActivity.LIME));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(MainActivity.LIME));footer.addView(seek,new LinearLayout.LayoutParams(-1,a.dp(34)));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){if(prepared&&player!=null)try{player.seekTo((int)(player.getDuration()*s.getProgress()/1000L));}catch(IllegalStateException ignored){}seeking=false;}public void onProgressChanged(SeekBar s,int p,boolean user){}});
        LinearLayout controls=a.row();previous=a.button("‹");previous.setTextSize(28);previous.setContentDescription(a.getString(R.string.media_previous));controls.addView(previous,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));previous.setOnClickListener(v->move(-1));page=a.text("",12,MainActivity.MUTED);page.setGravity(Gravity.CENTER);controls.addView(page,new LinearLayout.LayoutParams(0,a.dp(48),1));play=a.button("Ⅱ");play.setContentDescription(a.getString(R.string.live_pause));controls.addView(play,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));play.setOnClickListener(v->togglePlayback());next=a.button("›");next.setTextSize(28);next.setContentDescription(a.getString(R.string.media_next));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(a.dp(48),a.dp(48));np.leftMargin=a.dp(8);controls.addView(next,np);next.setOnClickListener(v->move(1));footer.addView(controls);panel.addView(footer);
        bindGestures();dialog.setContentView(panel);dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));dialog.getWindow().setDecorFitsSystemWindows(false);panel.setOnApplyWindowInsetsListener((v,insets)->{Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());panel.setPadding(a.dp(16)+safe.left,a.dp(8)+safe.top,a.dp(16)+safe.right,a.dp(8)+safe.bottom);return insets;});dialog.setOnDismissListener(v->dismiss());
    }
    void show(){
        a.mediaPreview=this;a.ready(false);a.handler.removeCallbacks(a.previewAcknowledgement);a.engine.detach();a.engine.gl.post(()->a.handler.post(()->{cameraReleased=true;if(!closed&&video!=null&&video.isAvailable())prepareVideo(generation,video.getSurfaceTexture());}));
        dialog.show();dialog.getWindow().setLayout(-1,-1);panel.requestApplyInsets();media.addView(notice,new FrameLayout.LayoutParams(-1,-1));
        worker.execute(()->{List<Item> found=scan();a.handler.post(()->{if(closed)return;items.addAll(found);index=0;for(int n=0;n<items.size();n++)if(items.get(n).uri.equals(initial)){index=n;break;}showItem();});});
    }
    List<Item> scan(){
        List<Item> found=new ArrayList<>();String[] projection={MediaStore.Files.FileColumns._ID,MediaStore.Files.FileColumns.MEDIA_TYPE,MediaStore.MediaColumns.MIME_TYPE,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.DATE_ADDED,MediaStore.MediaColumns.DATE_TAKEN};
        String selection="("+MediaStore.Files.FileColumns.MEDIA_TYPE+"=1 OR "+MediaStore.Files.FileColumns.MEDIA_TYPE+"=3) AND "+MediaStore.MediaColumns.IS_PENDING+"=0 AND ("+MediaStore.MediaColumns.RELATIVE_PATH+"=? OR "+MediaStore.MediaColumns.RELATIVE_PATH+"=? OR "+MediaStore.MediaColumns.RELATIVE_PATH+"=?)";
        try(Cursor cursor=a.getContentResolver().query(MediaStore.Files.getContentUri("external"),projection,selection,new String[]{"DCIM/5igna1/","Pictures/5igna1/","Movies/5igna1/"},MediaStore.MediaColumns.DATE_ADDED+" DESC, "+MediaStore.Files.FileColumns._ID+" DESC",scan)){
            if(cursor!=null)while(cursor.moveToNext()){boolean movie=cursor.getInt(1)==3;String mime=cursor.getString(2);long taken=cursor.getLong(5);found.add(new Item(ContentUris.withAppendedId(movie?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(0)),movie,mime!=null&&mime.contains("dng"),taken>0?taken:cursor.getLong(4)*1000,cursor.getString(3)));}
        }catch(Exception ignored){}
        if(found.stream().noneMatch(item->item.uri.equals(initial))){String mime=null;try{mime=a.getContentResolver().getType(initial);}catch(Exception ignored){}found.add(0,new Item(initial,initialVideo,mime!=null&&mime.contains("dng"),System.currentTimeMillis(),""));}return found;
    }
    void move(int delta){if(closed||index+delta<0||index+delta>=items.size())return;index+=delta;showItem();}
    void showItem(){
        if(closed||items.isEmpty())return;int ticket=++generation;releasePlayback();media.removeAllViews();image=null;video=null;zoom=1;videoW=videoH=0;prepared=false;seeking=false;videoFrameSeen=false;
        Item item=items.get(index);String date=android.text.format.DateFormat.getDateFormat(a).format(new Date(item.taken))+" · "+android.text.format.DateFormat.getTimeFormat(a).format(new Date(item.taken));title.setText(date);page.setText((item.video?"MP4":item.raw?"RAW":"JPG")+" · "+(index+1)+" / "+items.size());previous.setEnabled(index>0);previous.setAlpha(index>0?1:.25f);next.setEnabled(index+1<items.size());next.setAlpha(index+1<items.size()?1:.25f);play.setVisibility(item.video?View.VISIBLE:View.GONE);play.setEnabled(false);seek.setVisibility(item.video?View.VISIBLE:View.GONE);seek.setEnabled(false);seek.setProgress(0);time.setText(item.video?"0:00 / —":a.getString(R.string.media_gestures));notice.setText(a.getString(R.string.media_loading));notice.setVisibility(View.VISIBLE);
        if(item.video){video=new TextureView(a);media.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture texture,int w,int h){prepareVideo(ticket,texture);}public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){}public boolean onSurfaceTextureDestroyed(SurfaceTexture t){if(ticket==generation)releasePlayback();return true;}public void onSurfaceTextureUpdated(SurfaceTexture t){if(ticket==generation){videoFrameSeen=true;notice.setVisibility(View.GONE);}}});}
        else{image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);media.addView(image,new FrameLayout.LayoutParams(-1,-1));worker.execute(()->{if(closed||ticket!=generation)return;Bitmap bitmap=null;try{if(item.raw)bitmap=a.getContentResolver().loadThumbnail(item.uri,new android.util.Size(1536,1536),scan);else bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(a.getContentResolver(),item.uri),(decoder,info,source)->{android.util.Size size=info.getSize();float scale=Math.min(1,2048f/Math.max(size.getWidth(),size.getHeight()));decoder.setTargetSize(Math.max(1,Math.round(size.getWidth()*scale)),Math.max(1,Math.round(size.getHeight()*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});}catch(Exception|OutOfMemoryError ignored){}Bitmap result=bitmap;a.handler.post(()->{if(closed||ticket!=generation){if(result!=null)result.recycle();return;}if(result==null){notice.setText(a.getString(R.string.media_preview_unavailable));return;}image.setImageBitmap(result);notice.setVisibility(View.GONE);});});}
        media.addView(notice,new FrameLayout.LayoutParams(-1,-1));
    }
    void prepareVideo(int ticket,SurfaceTexture texture){
        if(closed||ticket!=generation||!cameraReleased||player!=null)return;Item item=items.get(index);if(!item.video)return;
        try{MediaPlayer candidate=new MediaPlayer();player=candidate;videoSurface=new Surface(texture);candidate.setSurface(videoSurface);candidate.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build());
        candidate.setOnPreparedListener(mp->{if(closed||ticket!=generation||player!=mp)return;a.handler.removeCallbacks(prepareTimeout);prepared=true;videoW=mp.getVideoWidth();videoH=mp.getVideoHeight();fitVideo();play.setEnabled(true);seek.setEnabled(true);startPlayback();a.handler.post(tick);});candidate.setOnVideoSizeChangedListener((mp,w,h)->{if(player==mp){videoW=w;videoH=h;fitVideo();}});
        candidate.setOnCompletionListener(mp->{if(player==mp){play.setText("▶");play.setContentDescription(a.getString(R.string.media_play));abandonFocus();}});candidate.setOnErrorListener((mp,what,extra)->{if(ticket==generation){releasePlayback();notice.setText(a.getString(R.string.media_preview_unavailable));notice.setVisibility(View.VISIBLE);play.setEnabled(false);seek.setEnabled(false);}return true;});
        candidate.setDataSource(a,item.uri);candidate.prepareAsync();a.handler.postDelayed(prepareTimeout,12000);}catch(Exception error){releasePlayback();notice.setText(a.getString(R.string.media_preview_unavailable));notice.setVisibility(View.VISIBLE);}
    }
    final Runnable prepareTimeout=new Runnable(){public void run(){if(!closed&&!prepared&&player!=null){releasePlayback();notice.setText(a.getString(R.string.media_preview_unavailable));notice.setVisibility(View.VISIBLE);}}};
    void fitVideo(){if(video==null||videoW<=0||videoH<=0||media.getWidth()<=0||media.getHeight()<=0)return;float scale=Math.min(media.getWidth()/(float)videoW,media.getHeight()/(float)videoH);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)video.getLayoutParams();int w=Math.round(videoW*scale),h=Math.round(videoH*scale);if(p.width!=w||p.height!=h){p.width=w;p.height=h;p.gravity=Gravity.CENTER;video.setLayoutParams(p);}}
    void startPlayback(){if(!prepared||player==null)return;try{if(audio!=null&&!focusGranted){int ticket=generation;MediaPlayer focusedPlayer=player;focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(change->{if(change<=0&&ticket==generation&&player==focusedPlayer&&prepared)pausePlayback();},a.handler).build();focusGranted=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!focusGranted){notice.setText(a.getString(R.string.media_play));play.setText("▶");play.setContentDescription(a.getString(R.string.media_play));return;}}player.start();play.setText("Ⅱ");play.setContentDescription(a.getString(R.string.live_pause));}catch(IllegalStateException ignored){}}
    void pausePlayback(){if(!prepared||player==null)return;try{player.pause();}catch(IllegalStateException ignored){}play.setText("▶");play.setContentDescription(a.getString(R.string.media_play));abandonFocus();}
    void togglePlayback(){if(!prepared||player==null)return;try{if(player.isPlaying())pausePlayback();else startPlayback();}catch(IllegalStateException ignored){}}
    void abandonFocus(){if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);focus=null;focusGranted=false;}
    final Runnable tick=new Runnable(){public void run(){if(closed||!prepared||player==null)return;try{int current=player.getCurrentPosition(),duration=player.getDuration();time.setText(clock(current)+" / "+clock(duration));if(!seeking)seek.setProgress(duration<=0?0:(int)(current*1000L/duration));}catch(IllegalStateException ignored){}a.handler.postDelayed(this,250);}};
    static String clock(int millis){int seconds=Math.max(0,millis/1000);return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);}
    void releasePlayback(){a.handler.removeCallbacks(tick);a.handler.removeCallbacks(prepareTimeout);prepared=false;abandonFocus();MediaPlayer old=player;player=null;if(old!=null){try{old.setOnPreparedListener(null);old.setOnCompletionListener(null);old.setOnErrorListener(null);}catch(RuntimeException ignored){}try{old.release();}catch(RuntimeException ignored){}}if(videoSurface!=null){videoSurface.release();videoSurface=null;}}
    void bindGestures(){
        ScaleGestureDetector scale=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector detector){if(image==null)return false;zoom=Math.max(1,Math.min(5,zoom*detector.getScaleFactor()));image.setScaleX(zoom);image.setScaleY(zoom);clampImage();return true;}});
        media.setOnClickListener(v->{if(image!=null&&zoom>1){zoom=1;image.setScaleX(1);image.setScaleY(1);image.setTranslationX(0);image.setTranslationY(0);}else togglePlayback();});
        media.setOnTouchListener(new View.OnTouchListener(){float x,y,lastX,lastY;boolean multiple;public boolean onTouch(View v,android.view.MotionEvent event){scale.onTouchEvent(event);float rawX=event.getRawX(),rawY=event.getRawY();switch(event.getActionMasked()){
            case android.view.MotionEvent.ACTION_DOWN:x=lastX=rawX;y=lastY=rawY;multiple=false;panel.animate().cancel();return true;
            case android.view.MotionEvent.ACTION_POINTER_DOWN:multiple=true;panel.setTranslationY(0);return true;
            case android.view.MotionEvent.ACTION_MOVE:if(scale.isInProgress())return true;if(image!=null&&zoom>1){image.setTranslationX(image.getTranslationX()+rawX-lastX);image.setTranslationY(image.getTranslationY()+rawY-lastY);clampImage();}else if(!multiple&&rawY-y>0&&rawY-y>Math.abs(rawX-x)*1.2f)panel.setTranslationY((rawY-y)*.75f);lastX=rawX;lastY=rawY;return true;
            case android.view.MotionEvent.ACTION_UP:float dx=rawX-x,dy=rawY-y;if(!multiple&&zoom<=1&&dy>a.dp(80)&&dy>Math.abs(dx)*1.2f){dismiss();return true;}if(!multiple&&zoom<=1&&Math.abs(dx)>a.dp(64)&&Math.abs(dx)>Math.abs(dy)*1.2f)move(dx<0?1:-1);else if(!multiple&&Math.abs(dx)<a.dp(8)&&Math.abs(dy)<a.dp(8))v.performClick();panel.animate().translationY(0).setDuration(140).start();return true;
            case android.view.MotionEvent.ACTION_CANCEL:panel.animate().translationY(0).setDuration(140).start();return true;
        }return true;}});
    }
    void clampImage(){if(image==null||image.getDrawable()==null)return;float ratio=image.getDrawable().getIntrinsicWidth()/(float)Math.max(1,image.getDrawable().getIntrinsicHeight());float w=Math.min(media.getWidth(),media.getHeight()*ratio),h=w/ratio;float maxX=Math.max(0,(w*zoom-media.getWidth())/2),maxY=Math.max(0,(h*zoom-media.getHeight())/2);image.setTranslationX(Math.max(-maxX,Math.min(maxX,image.getTranslationX())));image.setTranslationY(Math.max(-maxY,Math.min(maxY,image.getTranslationY())));}
    void dismiss(){if(closed)return;closed=true;generation++;scan.cancel();panel.animate().cancel();releasePlayback();worker.shutdownNow();if(image!=null)image.setImageDrawable(null);dialog.dismiss();if(a.mediaPreview==this){a.mediaPreview=null;a.resumeCameraPreview();}}
}
