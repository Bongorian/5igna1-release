package com.bongorian.signa1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.*;

/** Foreground-only location with independent permission, service and fix states. */
final class GeoTags implements LocationListener {
    final Activity activity;final LocationManager manager;final Runnable changed;
    final Handler handler=new Handler(Looper.getMainLooper());final ArrayList<CancellationSignal> requests=new ArrayList<>();
    volatile Location last;volatile boolean enabled;boolean active,foreground;int generation,permissionLevel;long attempted;
    String error="";
    GeoTags(Activity a,Runnable callback){activity=a;changed=callback;manager=(LocationManager)a.getSystemService(Context.LOCATION_SERVICE);}
    boolean precise(){return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    boolean permitted(){return precise()||activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    boolean servicesEnabled(){return manager!=null&&manager.isLocationEnabled();}
    void setEnabled(boolean value){enabled=value;if(!value){last=null;disconnect();}else if(foreground)connect(false);changed.run();}
    void start(){foreground=true;connect(false);handler.removeCallbacks(heartbeat);handler.postDelayed(heartbeat,1000);changed.run();}
    void retry(){if(foreground)connect(true);changed.run();}
    void connect(boolean force){
        int level=precise()?2:permitted()?1:0;
        if(level<permissionLevel)last=null;
        if(!foreground||!enabled||level==0||!servicesEnabled()){disconnect();permissionLevel=level;return;}
        if(!force&&active&&permissionLevel==level)return;
        disconnect();permissionLevel=level;attempted=SystemClock.elapsedRealtime();error="";int ticket=generation;
        for(String provider:manager.getProviders(true)){
            if(provider.equals(LocationManager.PASSIVE_PROVIDER))continue;
            try{
                Location known=manager.getLastKnownLocation(provider);if(known!=null)onLocationChanged(known);
                manager.requestLocationUpdates(provider,1000,0,this,Looper.getMainLooper());active=true;
                CancellationSignal cancellation=new CancellationSignal();requests.add(cancellation);
                LocationRequest request=new LocationRequest.Builder(1000).setQuality(level==2?LocationRequest.QUALITY_HIGH_ACCURACY:LocationRequest.QUALITY_BALANCED_POWER_ACCURACY).setDurationMillis(30_000).build();
                manager.getCurrentLocation(provider,request,cancellation,activity.getMainExecutor(),location->{if(ticket!=generation||!foreground||!enabled)return;if(location!=null)onLocationChanged(location);else changed.run();});
            }catch(SecurityException denied){error=activity.getString(R.string.ui_check_location_permissions);}
            catch(IllegalArgumentException unavailable){error=activity.getString(R.string.ui_location_service_unavailable);}
        }
        changed.run();
    }
    void disconnect(){generation++;for(CancellationSignal signal:requests)signal.cancel();requests.clear();if(manager!=null)try{manager.removeUpdates(this);}catch(SecurityException ignored){}active=false;}
    void stop(){foreground=false;handler.removeCallbacks(heartbeat);disconnect();}
    final Runnable heartbeat=new Runnable(){public void run(){if(!foreground)return;
        int level=precise()?2:permitted()?1:0;
        if(!enabled||!servicesEnabled()||level!=permissionLevel)connect(false);
        else if(enabled&&!active&&SystemClock.elapsedRealtime()-attempted>10_000)connect(false);
        changed.run();handler.postDelayed(this,1000);
    }};
    static boolean fresh(Location value,long now){
        if(value==null)return false;long age=now-value.getElapsedRealtimeNanos();
        return age>=0&&age<=120_000_000_000L&&Double.isFinite(value.getLatitude())&&Double.isFinite(value.getLongitude())&&Math.abs(value.getLatitude())<=90&&Math.abs(value.getLongitude())<=180;
    }
    Location snapshot(){Location value=last;return !enabled||!permitted()||(!precise()&&permissionLevel==2)||!servicesEnabled()||!fresh(value,SystemClock.elapsedRealtimeNanos())?null:new Location(value);}
    String label(){return !enabled?"GPS OFF":!permitted()?activity.getString(R.string.ui_gps_denied):!servicesEnabled()?activity.getString(R.string.ui_gps_device_off):snapshot()!=null?(precise()?"GPS ON":activity.getString(R.string.ui_gps_approx)):!active?activity.getString(R.string.ui_gps_unavailable):SystemClock.elapsedRealtime()-attempted>30_000?activity.getString(R.string.ui_gps_no_fix):activity.getString(R.string.ui_gps_locating);}
    String detail(){
        String permission=precise()?activity.getString(R.string.ui_precise_location_allowed):permitted()?activity.getString(R.string.ui_approximate_location_allowed):activity.getString(R.string.ui_location_not_allowed);
        String service=servicesEnabled()?activity.getString(R.string.ui_device_location_on):activity.getString(R.string.ui_device_location_off);
        Location value=snapshot();String fix;
        if(!enabled)fix=activity.getString(R.string.ui_saving_capture_location_is_off);
        else if(!permitted())fix=activity.getString(R.string.ui_select_allow_location_and_grant_access_while_using);
        else if(!servicesEnabled())fix=activity.getString(R.string.ui_enable_location_in_the_device_settings);
        else if(value!=null)fix=(value.hasAccuracy()?String.format(Locale.JAPAN,activity.getString(R.string.ui_estimated_accuracy_0f_m),value.getAccuracy()):activity.getString(R.string.ui_location_acquired))+String.format(Locale.JAPAN,activity.getString(R.string.ui_d_s_ago),Math.max(0,(SystemClock.elapsedRealtimeNanos()-value.getElapsedRealtimeNanos())/1_000_000_000L));
        else fix=active?activity.getString(R.string.ui_acquiring_location_gps_reception_may_be_weak_indoors):error.isEmpty()?activity.getString(R.string.ui_no_location_service_is_available_check_your_device):error;
        return permission+"\n"+service+"\n\n"+fix+activity.getString(R.string.ui_an_available_location_is_saved_in_photo_and);
    }
    public void onLocationChanged(Location value){
        if(!foreground||!enabled||!permitted()||(!precise()&&permissionLevel==2)||!fresh(value,SystemClock.elapsedRealtimeNanos()))return;
        Location old=last;long difference=old==null?Long.MAX_VALUE:value.getElapsedRealtimeNanos()-old.getElapsedRealtimeNanos();
        if(!fresh(old,SystemClock.elapsedRealtimeNanos())||difference>15_000_000_000L||difference>=0&&(!old.hasAccuracy()||value.hasAccuracy()&&value.getAccuracy()<=old.getAccuracy()*2))last=new Location(value);
        changed.run();
    }
    public void onProviderEnabled(String provider){handler.post(()->{if(foreground)connect(true);});}
    public void onProviderDisabled(String provider){changed.run();}
    public void onStatusChanged(String provider,int status,Bundle extras){}
}
