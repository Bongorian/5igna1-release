package com.bongorian.signa1;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Size;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

/** Loads only the app's last saved URI. No photo-library permission or main-thread decoding. */
final class MediaThumbnail extends FrameLayout {
    final MainActivity activity;final ImageView image;final TextView badge;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    CancellationSignal cancellation;Uri requested;boolean loading;int revision;boolean disposed;volatile boolean hasThumbnail;
    MediaThumbnail(MainActivity a){super(a);activity=a;setBackground(a.bg(MainActivity.PANEL,0xff394039));setClipToOutline(true);setClickable(true);
        image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);addView(image,new LayoutParams(-1,-1));
        badge=a.text("",8,MainActivity.WHITE);badge.setGravity(Gravity.CENTER);badge.setBackground(a.detailBg(0xCC0A0C0D,0));LayoutParams bp=new LayoutParams(-2,a.dp(17),Gravity.BOTTOM|Gravity.END);bp.setMargins(0,0,a.dp(3),a.dp(3));badge.setPadding(a.dp(4),0,a.dp(4),0);addView(badge,bp);placeholder();
    }
    void placeholder(){hasThumbnail=false;image.setScaleType(ImageView.ScaleType.CENTER);image.setImageResource(R.drawable.ic_gallery);badge.setVisibility(GONE);setContentDescription(activity.getString(R.string.ui_open_saved_photos_and_videos));}
    void load(Uri uri,boolean video){if(disposed)return;if(uri!=null&&uri.equals(requested)&&(loading||hasThumbnail))return;requested=uri;loading=uri!=null;int ticket=++revision;if(cancellation!=null)cancellation.cancel();placeholder();if(uri==null)return;
        setContentDescription(video?activity.getString(R.string.ui_open_the_last_saved_video):activity.getString(R.string.ui_open_the_last_saved_photo));setTooltipText(getContentDescription());
        CancellationSignal signal=new CancellationSignal();cancellation=signal;
        worker.execute(()->{Bitmap bitmap=null;String type=null;try{type=activity.getContentResolver().getType(uri);bitmap=activity.getContentResolver().loadThumbnail(uri,new Size(192,192),signal);}catch(Exception ignored){}
            Bitmap result=bitmap;boolean sequence="application/zip".equals(type);boolean raw=sequence||(type!=null&&type.contains("dng"));activity.handler.post(()->{if(disposed||ticket!=revision){if(result!=null)result.recycle();return;}
                loading=false;if(result==null)placeholder();if(result!=null){hasThumbnail=true;image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageBitmap(result);}
                badge.setText(sequence?"RAW ZIP":video?"▶":raw?"RAW":"");if(sequence)setContentDescription(activity.getString(R.string.ui_open_the_last_saved_raw_video_zip));badge.setVisibility(video||raw?VISIBLE:GONE);
            });
        });
    }
    void dispose(){disposed=true;revision++;if(cancellation!=null)cancellation.cancel();worker.shutdownNow();image.setImageDrawable(null);}
}
