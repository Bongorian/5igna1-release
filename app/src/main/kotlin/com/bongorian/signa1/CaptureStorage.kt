package com.bongorian.signa1

import android.content.ContentValues
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.DngCreator
import android.hardware.camera2.params.BlackLevelPattern
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Persists completed captures; camera and GL resources remain owned by the engine. */
internal fun GlitchEngine.savePhoto(shot: GlitchEngine.PendingPhoto) {
    var uri: Uri? = null
    var temp: File? = null
    var bitmap: Bitmap? = null
    try {
        foregroundWork.await()
        val raw = shot.settings.photoFormat != 0
        val w: Int
        val h: Int
        if (!raw) {
            bitmap = shot.signal
            shot.signal = null
            w = bitmap!!.width
            h = bitmap.height
            temp = File.createTempFile("signal-photo-", ".jpg", context.cacheDir)
            FileOutputStream(temp).use { out ->
                if (
                    !bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        shot.settings.jpegQuality,
                        foregroundWork.output(out),
                    )
                )
                    throw IOException("JPEG compress")
            }
            bitmap.recycle()
            bitmap = null
            foregroundWork.await()
            PhotoMetadata.write(
                temp,
                shot.bytes,
                shot.result,
                shot.taken,
                w,
                h,
                shot.location,
                shot.description(),
            )
            uri = createMedia("jpg", shot.taken)
            FileInputStream(temp).use { `in` ->
                context.contentResolver.openOutputStream(uri).use { out ->
                    GlitchEngine.copy(`in`, foregroundWork.output(out!!))
                }
            }
        } else {
            w = shot.choice!!.size.width
            h = shot.choice.size.height
            require(DngSizes.accepts(requireNotNull(shot.cameraInfo), shot.choice.size, shot.choice.maximumPixelMode)) {
                "RAW dimensions are incompatible with DNG metadata: ${w}x${h}"
            }
            val white = shot.cameraInfo!!.get<Int?>(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
            val blacks =
                shot.cameraInfo.get<BlackLevelPattern?>(
                    CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN
                )
            val black = if (blacks == null) 0 else blacks.getOffsetForIndex(0, 0)
            val data =
                (if (shot.settings.photoFormat == 2)
                    RawGlitch.chain(
                        requireNotNull(shot.bytes),
                        w,
                        h,
                        if (white == null) 65535 else white,
                        black,
                        shot.frame,
                        foregroundWork::await,
                    )
                else shot.bytes)!!
            uri = createMedia("dng", shot.taken)
            DngCreator(shot.cameraInfo, shot.result!!).use { dng ->
                context.contentResolver.openOutputStream(uri).use { out ->
                    dng.setDescription(shot.description())
                    dng.setOrientation(
                        CameraOrientation.exif(shot.rotation, shot.front)
                    )
                    if (shot.location != null) dng.setLocation(shot.location)
                    // DngCreator also accepts array-backed buffers, as in the RAW video writer.
                    dng.writeByteBuffer(foregroundWork.output(out!!), Size(w, h), ByteBuffer.wrap(data), 0)
                }
            }
        }
        foregroundWork.await()
        publish(uri, false, shot.taken)
        Log.i(
            "Signal",
            "Photo saved format=" +
                (if (raw) "DNG" else "JPEG") +
                " " +
                w +
                "x" +
                h +
                " GPS=" +
                (shot.location != null) +
                " effect=" +
                Effects.chainName(shot.frame.ids()) +
                " LEVEL=" +
                shot.frame.amount,
        )
        if (shot.settings.location && shot.location == null)
            status(context.getString(R.string.ui_no_location_fix_saved_without_gps))
    } catch (e: Exception) {
        discard(uri)
        captureFailed()
        error(context.getString(R.string.ui_could_not_save_the_photo), e)
    } catch (e: AssertionError) {
        discard(uri)
        captureFailed()
        error(context.getString(R.string.ui_could_not_save_the_photo), IllegalStateException("DNG writer rejected the capture", e))
    } catch (e: OutOfMemoryError) {
        discard(uri)
        captureFailed()
        status(context.getString(R.string.ui_not_enough_memory_to_process_the_photo_lower))
    } finally {
        if (bitmap != null) bitmap.recycle()
        if (temp != null) temp.delete()
        gl.post(
            Runnable@{
                photoBusy = false
                ready(frameSeen && attached)
            }
        )
    }
}

internal fun GlitchEngine.createMedia(extension: String, taken: Long): Uri {
    externalSession?.let { return it.create(extension) }
    val video = extension == "mp4"
    val values = ContentValues()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(taken))
    values.put(
        MediaStore.MediaColumns.DISPLAY_NAME,
        BuildConfig.APP_NAME + "_" + stamp + "." + extension,
    )
    values.put(
        MediaStore.MediaColumns.MIME_TYPE,
        if (video) "video/mp4" else if (extension == "dng") "image/x-adobe-dng" else "image/jpeg",
    )
    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/5igna1")
    values.put(MediaStore.MediaColumns.IS_PENDING, 1)
    values.put(MediaStore.MediaColumns.DATE_TAKEN, taken)
    val result: Uri =
        context.contentResolver.insert(
            if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        )!!
    checkNotNull(result) { "MediaStore insert" }
    return result
}

internal fun GlitchEngine.publish(uri: Uri, video: Boolean, taken: Long) {
    if (externalSession != null) {
        ui.post { listener.saved(uri, video) }
        return
    }
    val values = ContentValues()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    values.put(MediaStore.MediaColumns.DATE_TAKEN, taken)
    context.contentResolver.update(uri, values, null, null)
    Log.i("Signal", "Saved " + uri + " video=" + video)
    ui.post(Runnable@{ listener.saved(uri, video) })
}

internal fun GlitchEngine.discard(uri: Uri?) {
    externalSession?.let { it.discard(uri); return }
    if (uri != null)
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            Log.w("Signal", "Cleanup", e)
        }
}
