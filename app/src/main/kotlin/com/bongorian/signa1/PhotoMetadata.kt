package com.bongorian.signa1

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.location.Location
import android.media.ExifInterface
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

internal object PhotoMetadata {
    val CAMERA_TAGS: Array<String> =
        arrayOf<String>(
            "ExposureTime",
            "FNumber",
            "PhotographicSensitivity",
            "ISOSpeedRatings",
            "FocalLength",
            "FocalLengthIn35mmFilm",
            "ExposureBiasValue",
            "ExposureProgram",
            "MeteringMode",
            "WhiteBalance",
            "Flash",
            "LensMake",
            "LensModel",
        )

    fun coordinate(value: Double): String {
        var value = value
        value = abs(value)
        val degrees = value.toInt()
        val minutes = (value - degrees) * 60
        val wholeMinutes = minutes.toInt()
        val seconds = Math.round((minutes - wholeMinutes) * 60 * 1000000)
        return degrees.toString() + "/1," + wholeMinutes + "/1," + seconds + "/1000000"
    }

    @Throws(IOException::class)
    fun write(
        file: File,
        original: ByteArray?,
        result: TotalCaptureResult?,
        taken: Long,
        width: Int,
        height: Int,
        location: Location?,
        description: String?,
    ) {
        val target = ExifInterface(file)
        if (original != null) {
            val source = ExifInterface(ByteArrayInputStream(original))
            for (tag in CAMERA_TAGS) {
                val value = source.getAttribute(tag)
                if (value != null) target.setAttribute(tag, value)
            }
        }
        target.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
        target.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
        target.setAttribute(
            ExifInterface.TAG_SOFTWARE,
            BuildConfig.APP_NAME + " " + BuildConfig.VERSION_NAME,
        )
        val date = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(taken))
        target.setAttribute(ExifInterface.TAG_DATETIME, date)
        target.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, date)
        target.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, date)
        target.setAttribute("SubSecTimeOriginal", String.format(Locale.US, "%03d", taken % 1000))
        val zone = SimpleDateFormat("XXX", Locale.US).format(Date(taken))
        target.setAttribute("OffsetTimeOriginal", zone)
        target.setAttribute(ExifInterface.TAG_ORIENTATION, "1")
        target.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
        target.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
        target.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
        target.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
        target.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description)
        target.setAttribute(ExifInterface.TAG_USER_COMMENT, description)
        if (result != null) {
            val exposure = result.get<Long?>(CaptureResult.SENSOR_EXPOSURE_TIME)
            val iso = result.get<Int?>(CaptureResult.SENSOR_SENSITIVITY)
            val aperture = result.get<Float?>(CaptureResult.LENS_APERTURE)
            val focal = result.get<Float?>(CaptureResult.LENS_FOCAL_LENGTH)
            if (exposure != null)
                target.setAttribute(
                    ExifInterface.TAG_EXPOSURE_TIME,
                    (exposure / 1e9).toString(),
                )
            if (iso != null) target.setAttribute("PhotographicSensitivity", iso.toString())
            if (aperture != null)
                target.setAttribute(
                    ExifInterface.TAG_F_NUMBER,
                    aperture.toString(),
                )
            if (focal != null) target.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focal.toString())
        }
        if (location != null) {
            target.setAttribute(ExifInterface.TAG_GPS_LATITUDE, coordinate(location.latitude))
            target.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (location.latitude < 0) "S" else "N",
            )
            target.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE,
                coordinate(location.longitude),
            )
            target.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (location.longitude < 0) "W" else "E",
            )
            if (location.hasAltitude()) {
                target.setAttribute(
                    ExifInterface.TAG_GPS_ALTITUDE,
                    Math.round(abs(location.altitude) * 1000).toString() + "/1000",
                )
                target.setAttribute(
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    if (location.altitude < 0) "1" else "0",
                )
            }
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
            utc.setTimeInMillis(location.time)
            target.setAttribute(
                ExifInterface.TAG_GPS_DATESTAMP,
                String.format(
                    Locale.US,
                    "%04d:%02d:%02d",
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH) + 1,
                    utc.get(Calendar.DAY_OF_MONTH),
                ),
            )
            target.setAttribute(
                ExifInterface.TAG_GPS_TIMESTAMP,
                utc.get(Calendar.HOUR_OF_DAY).toString() +
                    "/1," +
                    utc.get(Calendar.MINUTE) +
                    "/1," +
                    utc.get(Calendar.SECOND) +
                    "/1",
            )
            target.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS")
        }
        target.saveAttributes()
    }
}
