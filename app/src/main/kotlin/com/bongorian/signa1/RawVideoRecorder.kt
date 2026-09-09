package com.bongorian.signa1

import android.content.ContentUris
import android.content.ContentValues
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.location.Location
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import java.io.BufferedOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max

/**
 * Original RAW_SENSOR frames + matching per-frame metadata. All file writes use the engine's
 * worker. At most two copied frames are held; overload drops frames, never grows an unbounded
 * capture queue.
 */
internal class RawVideoRecorder
constructor(
    val engine: GlitchEngine,
    val probe: Boolean = false,
) {
    val characteristics: CameraCharacteristics
    val size: Size
    val fps: Int
    val orientation: Int
    val location: Location?
    val taken: Long = System.currentTimeMillis()
    val name: String
    val inFlight: AtomicInteger = AtomicInteger()
    val pending: TreeMap<Long?, ByteArray?> = TreeMap<Long?, ByteArray?>()
    val results: TreeMap<Long?, TotalCaptureResult?> = TreeMap<Long?, TotalCaptureResult?>()
    var accepting: Boolean = true

    @Volatile var failed: Boolean = false

    @Volatile var written: Int = 0

    @Volatile var dropped: Int = 0
    var acceptedNs: Long = 0
    var uri: Uri? = null
    var zip: ZipOutputStream? = null
    var output: CountingStream? = null
    var segment: Int = 0
    var segmentFrames: Int = 0
    var firstNs: Long = 0
    var lastNs: Long = 0
    var timestamps: StringBuilder = StringBuilder()

    init {
        characteristics = requireNotNull(engine.characteristics)
        size = requireNotNull(engine.rawVideoChoice).size
        fps = requireNotNull(engine.videoChoice).fps
        orientation =
            CameraOrientation.exif(engine.captureRotation, engine.front)
        location = if (engine.settings.location) engine.position.get() else null
        name =
            BuildConfig.APP_NAME +
                "_" +
                SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(taken)) +
                "_RAW"
    }

    fun result(result: TotalCaptureResult) {
        if (!accepting) return
        val stamp = result.get<Long?>(CaptureResult.SENSOR_TIMESTAMP)
        if (stamp == null) return
        results.put(stamp, result)
        while (results.size > 32) results.pollFirstEntry()
        pair(stamp)
    }

    fun image(image: Image) {
        if (!accepting || failed || (probe && acceptedNs != 0L)) return
        val stamp = image.timestamp
        if (acceptedNs != 0L && stamp - acceptedNs < 1000000000L / fps - 1000000L) return
        if (inFlight.get() >= 2) {
            dropped++
            expire(stamp)
            return
        }
        acceptedNs = stamp
        inFlight.incrementAndGet()
        try {
            val plane = image.planes[0]
            val source = plane.buffer
            val w = image.width
            val h = image.height
            val stride = plane.rowStride
            val pixel = plane.pixelStride
            val bytes = ByteArray(w * h * 2)
            for (y in 0..<h) {
                if (pixel == 2) {
                    source.position(y * stride)
                    source.get(bytes, y * w * 2, w * 2)
                } else
                    for (x in 0..<w) {
                        val src = y * stride + x * pixel
                        val dst = (y * w + x) * 2
                        bytes[dst] = source.get(src)
                        bytes[dst + 1] = source.get(src + 1)
                    }
            }
            pending.put(stamp, bytes)
            pair(stamp)
            expire(stamp)
        } catch (error: Exception) {
            inFlight.decrementAndGet()
            fail(error)
        } catch (error: AssertionError) {
            inFlight.decrementAndGet()
            fail(error)
        } catch (error: OutOfMemoryError) {
            inFlight.decrementAndGet()
            fail(error)
        }
    }

    fun expire(stamp: Long) {
        while (!pending.isEmpty() && stamp - pending.firstKey()!! > 1000000000L) {
            pending.pollFirstEntry()
            inFlight.decrementAndGet()
            dropped++
        }
    }

    fun pair(stamp: Long) {
        val bytes = pending.get(stamp)
        val metadata = results.get(stamp)
        if (bytes == null || metadata == null) return
        pending.remove(stamp)
        results.remove(stamp)
        engine.files.execute(
            Runnable@{
                try {
                    if (!failed) write(bytes, stamp, metadata)
                } catch (error: Exception) {
                    fail(error)
                } catch (error: AssertionError) {
                    fail(error)
                } catch (error: OutOfMemoryError) {
                    fail(error)
                } finally {
                    inFlight.decrementAndGet()
                }
            }
        )
    }

    fun fail(error: Throwable?) {
        if (failed) return
        failed = true
        Log.e("Signal", "RAW sequence", error)
        engine.gl.post(
            Runnable@{
                if (engine.rawProbe == this) {
                    engine.failRawSession(
                        engine.context.getString(
                            R.string.ui_required_raw_metadata_for_dng_is_unavailable
                        )
                    )
                    return@Runnable
                }
                if (engine.rawRecorder == this) engine.stopVideo()
                engine.status(
                    engine.context.getString(
                        R.string.ui_raw_recording_stopped_previously_saved_segments_are_kept
                    )
                )
            }
        )
    }

    @Throws(IOException::class)
    fun open() {
        val values = ContentValues()
        values.put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            name + String.format(Locale.US, "_%03d.zip", ++segment),
        )
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/5igna1",
        )
        values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        uri =
            engine.context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) throw IOException("RAW MediaStore insert")
        val stream = engine.context.contentResolver.openOutputStream(uri!!)
        if (stream == null) throw IOException("RAW output stream")
        output = CountingStream(BufferedOutputStream(stream, 256 * 1024))
        zip = ZipOutputStream(output)
        zip!!.setLevel(0)
        segmentFrames = 0
        timestamps = StringBuilder("frame,sensor_timestamp_ns,exposure_ns,iso\n")
        lastNs = 0
        firstNs = lastNs
    }

    @Throws(IOException::class)
    fun write(bytes: ByteArray, stamp: Long, metadata: TotalCaptureResult) {
        if (probe) {
            DngCreator(characteristics, metadata).use { creator ->
                creator.writeByteBuffer(
                    object : OutputStream() {
                        override fun write(value: Int) {}

                        override fun write(bytes: ByteArray?, offset: Int, count: Int) {}
                    },
                    size,
                    ByteBuffer.wrap(bytes),
                    0,
                )
            }
            engine.gl.post(
                Runnable@{
                    if (engine.rawProbe == this) {
                        stop()
                        engine.rawProbe = null
                        engine.rawFrameSeen = true
                        engine.ready(engine.frameSeen && !engine.photoBusy)
                        engine.status(
                            engine.context.getString(
                                R.string.ui_raw_output_and_dng_encoding_verified
                            ) + engine.description()
                        )
                    }
                }
            )
            return
        }
        if (
            StatFs(Environment.getExternalStorageDirectory().path).availableBytes <
                max(
                    256000000L,
                    bytes.size * 4L,
                )
        )
            throw IOException("Low storage")
        if (zip == null) open()
        val frame = String.format(Locale.US, "frame_%08d.dng", written)
        zip!!.putNextEntry(ZipEntry(frame))
        DngCreator(characteristics, metadata).use { creator ->
            creator.setOrientation(orientation)
            creator.setDescription(
                BuildConfig.APP_NAME + " RAW video / original sensor frame / timestamp_ns=" + stamp
            )
            if (location != null) creator.setLocation(location)
            creator.writeByteBuffer(zip!!, size, ByteBuffer.wrap(bytes), 0)
        }
        zip!!.closeEntry()
        if (firstNs == 0L) firstNs = stamp
        lastNs = stamp
        val exposure = metadata.get<Long?>(CaptureResult.SENSOR_EXPOSURE_TIME)
        val iso = metadata.get<Int?>(CaptureResult.SENSOR_SENSITIVITY)
        timestamps
            .append(frame)
            .append(',')
            .append(stamp)
            .append(',')
            .append(if (exposure == null) 0 else exposure)
            .append(',')
            .append(if (iso == null) 0 else iso)
            .append('\n')
        written++
        segmentFrames++
        if (output!!.count >= 3500000000L) finishSegment()
    }

    @Throws(IOException::class)
    fun entry(name: String?, text: String) {
        zip!!.putNextEntry(ZipEntry(name))
        zip!!.write(text.toByteArray(StandardCharsets.UTF_8))
        zip!!.closeEntry()
    }

    @Throws(IOException::class)
    fun finishSegment() {
        if (zip == null) return
        entry("timestamps.csv", timestamps.toString())
        val measured =
            if (segmentFrames > 1 && lastNs > firstNs)
                (segmentFrames - 1) * 1e9 / (lastNs - firstNs)
            else 0.0
        entry(
            "manifest.json",
            String.format(
                Locale.US,
                "{\"format\":\"DNG sequence\",\"width\":%d,\"height\":%d,\"requested_fps\":%d,\"measured_fps\":%.5f,\"frames\":%d,\"session_dropped_frames\":%d,\"audio\":false,\"effects_applied\":false,\"segment\":%d}",
                size.width,
                size.height,
                fps,
                measured,
                segmentFrames,
                dropped,
                segment,
            ),
        )
        entry(
            "README.txt",
            engine.context.getString(R.string.ui_5igna1_raw_video_dng_sequence_extract_the_zip),
        )
        zip!!.close()
        zip = null
        val completed = uri
        uri = null
        val published = ContentValues()
        published.put(MediaStore.MediaColumns.IS_PENDING, 0)
        engine.context.contentResolver.update(completed!!, published, null, null)
        engine.ui.post(Runnable@{ engine.listener.saved(completed, true) })
    }

    fun stop() {
        if (!accepting) return
        accepting = false
        dropped += pending.size
        inFlight.addAndGet(-pending.size)
        pending.clear()
        results.clear()
        if (probe) return
        engine.files.execute(
            Runnable@{
                try {
                    if (!failed && written > 0) finishSegment()
                    else {
                        if (zip != null) zip!!.close()
                        zip = null
                        engine.discard(uri)
                        uri = null
                    }
                } catch (error: Exception) {
                    if (zip != null)
                        try {
                            zip!!.close()
                        } catch (ignored: IOException) {}
                    zip = null
                    engine.discard(uri)
                    uri = null
                    Log.e("Signal", "Finalize RAW sequence", error)
                    failed = true
                } finally {
                    engine.gl.post(
                        Runnable@{
                            engine.photoBusy = false
                            engine.ready(engine.frameSeen && engine.attached)
                            engine.status(
                                if (failed)
                                    engine.context.getString(R.string.ui_could_not_save_raw_video)
                                else if (written == 0)
                                    engine.context.getString(
                                        R.string.ui_no_raw_frames_were_captured
                                    )
                                else
                                    engine.context.getString(R.string.ui_raw_saved) +
                                        written +
                                        engine.context.getString(R.string.ui_frames_dropped) +
                                        dropped
                            )
                        }
                    )
                }
            }
        )
    }

    internal class CountingStream(out: OutputStream?) : FilterOutputStream(out) {
        var count: Long = 0

        @Throws(IOException::class)
        override fun write(b: Int) {
            out.write(b)
            count++
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray?, off: Int, len: Int) {
            out.write(b, off, len)
            count += len.toLong()
        }
    }

    companion object {
        private val recovered = AtomicBoolean()

        fun recoverPending(engine: GlitchEngine) {
            if (!recovered.compareAndSet(false, true)) return
            engine.files.execute(
                Runnable@{
                    val selection =
                        MediaStore.MediaColumns.IS_PENDING +
                            "=1 AND " +
                            MediaStore.MediaColumns.OWNER_PACKAGE_NAME +
                            "=? AND " +
                            MediaStore.MediaColumns.DISPLAY_NAME +
                            " LIKE ?"
                    try {
                        engine.context.contentResolver
                            .query(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                arrayOf<String>(MediaStore.MediaColumns._ID),
                                selection,
                                arrayOf<String>(
                                    engine.context.packageName,
                                    "5igna1_%_RAW_%.zip",
                                ),
                                null,
                            )
                            .use { rows ->
                                if (rows != null)
                                    while (rows.moveToNext()) engine.discard(
                                        ContentUris.withAppendedId(
                                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                            rows.getLong(0),
                                        )
                                    )
                            }
                    } catch (error: Exception) {
                        Log.w("Signal", "RAW draft cleanup", error)
                    }
                }
            )
        }
    }
}
