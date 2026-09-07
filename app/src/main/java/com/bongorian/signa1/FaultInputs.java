package com.bongorian.signa1;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.hardware.camera2.*;
import android.media.*;
import android.os.*;
import java.util.Locale;

/** Sensor ownership is tied to foreground GL attachment. All fields except micPeak are GL-owned. */
final class FaultInputs implements SensorEventListener {
    final Context context;final Handler handler;final SensorManager sensors;
    final FaultModel.Inputs values=new FaultModel.Inputs();
    FaultConfig config=FaultConfig.defaults();boolean active,recorderAudio,hasAccel,hasGyro,gravityReady;
    float gx,gy,gz,batteryC=Float.NaN;int thermalStatus=-1;
    long accelNs,lastCpuWall,lastCpuTime,lastFrame,lastArrival;float framePeriod;
    final long[][] metadata=new long[16][3];int metadataIndex;
    volatile float micPeak;volatile boolean micRunning;volatile String micState="OFF";
    AudioRecord microphone;Thread micThread;
    FaultInputs(Context context,Handler handler){this.context=context;this.handler=handler;sensors=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);}
    void configure(FaultConfig next,boolean foreground,boolean recordingAudio){
        boolean changed=active!=(foreground&&next.enabled&&!next.internal)||config.motion!=next.motion;
        config=next;recorderAudio=recordingAudio;
        if(changed){stopSensors();active=foreground&&next.enabled&&!next.internal;if(active&&config.motion){
            Sensor accel=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor gyro=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if(accel!=null)hasAccel=sensors.registerListener(this,accel,20_000,handler);
            if(gyro!=null)hasGyro=sensors.registerListener(this,gyro,20_000,handler);
        }}
        active=foreground&&next.enabled&&!next.internal;
        handler.removeCallbacks(poll);
        if(active){poll.run();}else{values.cpu=values.heat=values.jitter=0;lastFrame=lastArrival=0;framePeriod=0;}
        boolean wanted=active&&config.audio&&!recorderAudio&&context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        if(wanted&&microphone==null)startMic();else if(!wanted)stopMic();
        if(active&&config.audio&&context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)micState=context.getString(R.string.ui_no_permission);
        else if(active&&config.audio&&recorderAudio)micState=context.getString(R.string.ui_recorded_audio);
    }
    void stopSensors(){if(sensors!=null)sensors.unregisterListener(this);hasAccel=hasGyro=gravityReady=false;accelNs=0;values.ax=values.ay=values.az=values.tilt=values.rotation=0;values.motionAvailable=false;}
    void stop(){configure(config,false,false);lastCpuWall=lastCpuTime=0;}
    @Override public void onSensorChanged(SensorEvent event){
        if(!active||!config.motion)return;
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            float x=event.values[0],y=event.values[1],z=event.values[2];
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z))return;
            if(!gravityReady){gx=x;gy=y;gz=z;gravityReady=true;}
            float dt=accelNs==0?.02f:Math.max(.001f,Math.min(.2f,(event.timestamp-accelNs)*1e-9f));accelNs=event.timestamp;
            float alpha=(float)Math.exp(-dt/.35);gx=alpha*gx+(1-alpha)*x;gy=alpha*gy+(1-alpha)*y;gz=alpha*gz+(1-alpha)*z;
            values.ax=x-gx;values.ay=y-gy;values.az=z-gz;values.tilt=FaultModel.clamp(gx/9.80665f,-1,1);values.motionAvailable=true;
        }else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){values.rotation=FaultModel.clamp(event.values[2],-6,6);values.motionAvailable=true;}
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    final Runnable poll=new Runnable(){public void run(){if(!active)return;
        long wall=SystemClock.elapsedRealtime(),cpu=android.os.Process.getElapsedCpuTime();
        if(lastCpuWall>0&&wall>lastCpuWall)values.cpu=FaultModel.clamp((cpu-lastCpuTime)/(float)(wall-lastCpuWall),0,1);
        lastCpuWall=wall;lastCpuTime=cpu;
        values.heat=0;batteryC=Float.NaN;thermalStatus=-1;
        if(config.thermal){
            Intent battery=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(battery!=null&&battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)){float c=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f;if(c>-20&&c<90){batteryC=c;values.heat=FaultModel.clamp((c-32)/15,0,1);}}
            PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if(power!=null)try{thermalStatus=power.getCurrentThermalStatus();values.heat=Math.max(values.heat,FaultModel.clamp(thermalStatus/4f,0,1));}catch(RuntimeException ignored){}
        }
        handler.postDelayed(this,1000);
    }};
    void resetTiming(){lastFrame=lastArrival=0;framePeriod=0;values.jitter=0;values.timingAvailable=false;for(long[] row:metadata)java.util.Arrays.fill(row,0);}
    void capture(TotalCaptureResult result){
        Long stamp=result.get(CaptureResult.SENSOR_TIMESTAMP),exposure=result.get(CaptureResult.SENSOR_EXPOSURE_TIME),skew=result.get(CaptureResult.SENSOR_ROLLING_SHUTTER_SKEW);
        if(stamp!=null&&exposure!=null&&skew!=null){long[] slot=metadata[metadataIndex++%metadata.length];slot[0]=stamp;slot[1]=exposure;slot[2]=skew;}
    }
    FaultModel.Inputs frame(long stamp,long arrival,MediaRecorder recorder){
        if(lastFrame>0&&stamp>lastFrame){float period=(stamp-lastFrame)*1e-9f;
            if(framePeriod==0)framePeriod=period;
            float gap=Math.max(0,period-framePeriod*1.15f)/Math.max(.001f,framePeriod);
            // Arrival lateness measures this app's delivery cadence, never network packet loss.
            float late=lastArrival>0?Math.max(0,(arrival-lastArrival)*1e-9f-period*1.2f)/Math.max(.001f,period):0;
            values.jitter=FaultModel.clamp(Math.max(gap,late),0,1);
            if(period<framePeriod*1.5f)framePeriod=framePeriod*.98f+period*.02f;
        }
        lastFrame=stamp;lastArrival=arrival;values.sensorNs=stamp;values.timingAvailable=false;
        long best=-1;for(long[] slot:metadata)if(slot[0]>0&&slot[0]<=stamp&&stamp-slot[0]<250_000_000L&&slot[0]>best){best=slot[0];values.exposureNs=slot[1];values.skewNs=slot[2];values.timingAvailable=true;}
        values.audio=0;
        if(active&&config.audio){
            if(recorderAudio&&recorder!=null)try{values.audio=level(recorder.getMaxAmplitude());}catch(RuntimeException ignored){micState=context.getString(R.string.ui_unavailable_220);}
            else values.audio=micPeak;
        }
        return values;
    }
    static float level(int amplitude){return amplitude<=0?0:FaultModel.clamp(((float)(20*Math.log10(amplitude/32768.0))+55)/45,0,1);}
    void startMic(){if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){micState=context.getString(R.string.ui_no_permission);return;}try{
        int size=Math.max(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
        AudioRecord next=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
        microphone=next;if(next.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microphone unavailable");
        next.startRecording();micRunning=true;micState=context.getString(R.string.ui_level_detection);
        micThread=new Thread(()->{short[] buffer=new short[1024];try{while(micRunning){int count=next.read(buffer,0,buffer.length);if(count<=0)break;int peak=0;for(int n=0;n<count;n++)peak=Math.max(peak,Math.abs((int)buffer[n]));micPeak=level(peak);}}catch(RuntimeException ignored){}finally{micPeak=0;if(micRunning)micState=context.getString(R.string.ui_unavailable_220);}},"FaultAudio");micThread.start();
    }catch(RuntimeException error){stopMic();micState=context.getString(R.string.ui_unavailable_220);}}
    void stopMic(){micRunning=false;micPeak=0;if(microphone!=null){try{microphone.stop();}catch(RuntimeException ignored){}if(micThread!=null)try{micThread.join(300);}catch(InterruptedException e){Thread.currentThread().interrupt();}microphone.release();microphone=null;}micThread=null;micState="OFF";}
    String summary(){return (hasAccel||hasGyro?context.getString(R.string.ui_reading_motion):context.getString(R.string.ui_motion))+context.getString(R.string.ui_audio)+micState+"\n"+
        (Float.isNaN(batteryC)?context.getString(R.string.ui_battery_temperature):String.format(Locale.US,context.getString(R.string.ui_battery_1f_c),batteryC))+context.getString(R.string.ui_thermal_state)+(thermalStatus<0?"—":thermalStatus)+"\n"+
        (config.cpu?String.format(Locale.US,context.getString(R.string.ui_app_cpu_0f_one_core),values.cpu*100):"CPU OFF")+" / "+
        (values.timingAvailable?String.format(Locale.US,context.getString(R.string.ui_readout_1f_ms),values.skewNs*1e-6):context.getString(R.string.ui_readout));}
}
