package com.bongorian.signa1;

import android.hardware.camera2.*;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class PhotoMetadata {
    static final String[] CAMERA_TAGS={"ExposureTime","FNumber","PhotographicSensitivity","ISOSpeedRatings","FocalLength","FocalLengthIn35mmFilm","ExposureBiasValue","ExposureProgram","MeteringMode","WhiteBalance","Flash","LensMake","LensModel"};
    static String coordinate(double value){value=Math.abs(value);int degrees=(int)value;double minutes=(value-degrees)*60;int wholeMinutes=(int)minutes;long seconds=Math.round((minutes-wholeMinutes)*60*1_000_000);return degrees+"/1,"+wholeMinutes+"/1,"+seconds+"/1000000";}
    static void write(File file,byte[] original,TotalCaptureResult result,long taken,int width,int height,Location location,String description)throws IOException{
        ExifInterface target=new ExifInterface(file);
        if(original!=null){ExifInterface source=new ExifInterface(new ByteArrayInputStream(original));for(String tag:CAMERA_TAGS){String value=source.getAttribute(tag);if(value!=null)target.setAttribute(tag,value);}}
        target.setAttribute(ExifInterface.TAG_MAKE,Build.MANUFACTURER);target.setAttribute(ExifInterface.TAG_MODEL,Build.MODEL);target.setAttribute(ExifInterface.TAG_SOFTWARE,BuildConfig.APP_NAME+" "+BuildConfig.VERSION_NAME);
        String date=new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",Locale.US).format(new Date(taken));
        target.setAttribute(ExifInterface.TAG_DATETIME,date);target.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,date);target.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED,date);
        target.setAttribute("SubSecTimeOriginal",String.format(Locale.US,"%03d",taken%1000));
        String zone=new SimpleDateFormat("XXX",Locale.US).format(new Date(taken));target.setAttribute("OffsetTimeOriginal",zone);
        target.setAttribute(ExifInterface.TAG_ORIENTATION,"1");target.setAttribute(ExifInterface.TAG_IMAGE_WIDTH,Integer.toString(width));target.setAttribute(ExifInterface.TAG_IMAGE_LENGTH,Integer.toString(height));
        target.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION,Integer.toString(width));target.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION,Integer.toString(height));
        target.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION,description);target.setAttribute(ExifInterface.TAG_USER_COMMENT,description);
        if(result!=null){Long exposure=result.get(CaptureResult.SENSOR_EXPOSURE_TIME);Integer iso=result.get(CaptureResult.SENSOR_SENSITIVITY);Float aperture=result.get(CaptureResult.LENS_APERTURE);Float focal=result.get(CaptureResult.LENS_FOCAL_LENGTH);
            if(exposure!=null)target.setAttribute(ExifInterface.TAG_EXPOSURE_TIME,Double.toString(exposure/1e9));if(iso!=null)target.setAttribute("PhotographicSensitivity",Integer.toString(iso));if(aperture!=null)target.setAttribute(ExifInterface.TAG_F_NUMBER,aperture.toString());if(focal!=null)target.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,focal.toString());}
        if(location!=null){
            target.setAttribute(ExifInterface.TAG_GPS_LATITUDE,coordinate(location.getLatitude()));target.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,location.getLatitude()<0?"S":"N");
            target.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,coordinate(location.getLongitude()));target.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,location.getLongitude()<0?"W":"E");
            if(location.hasAltitude()){target.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,Math.round(Math.abs(location.getAltitude())*1000)+"/1000");target.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF,location.getAltitude()<0?"1":"0");}
            Calendar utc=Calendar.getInstance(TimeZone.getTimeZone("UTC"),Locale.US);utc.setTimeInMillis(location.getTime());target.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,String.format(Locale.US,"%04d:%02d:%02d",utc.get(Calendar.YEAR),utc.get(Calendar.MONTH)+1,utc.get(Calendar.DAY_OF_MONTH)));target.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,utc.get(Calendar.HOUR_OF_DAY)+"/1,"+utc.get(Calendar.MINUTE)+"/1,"+utc.get(Calendar.SECOND)+"/1");
            target.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD,"GPS");
        }target.saveAttributes();
    }
}
