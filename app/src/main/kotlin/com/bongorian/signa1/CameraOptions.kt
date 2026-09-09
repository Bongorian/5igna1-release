package com.bongorian.signa1

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecInfo.VideoCapabilities
import android.media.MediaCodecList
import android.util.Log
import android.util.Range
import android.util.Size
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class CameraOptions(val id: String?, cc: CameraCharacteristics, maxTexture: Int) {
    internal class Photo(val size: Size, val maximumPixelMode: Boolean) {
        fun key(): String {
            return size.toString() + (if (maximumPixelMode) "@max" else "")
        }

        fun label(context: Context): String {
            return String.format(
                Locale.US,
                "%.1f MP · %d × %d%s",
                area(size) / 1e6,
                size.height,
                size.width,
                if (maximumPixelMode) context.getString(R.string.ui_high_resolution_sensor) else "",
            )
        }
    }

    internal class Video(val size: Size, val fps: Int, val highSpeed: Boolean) {
        fun key(): String {
            return size.toString() + "@" + fps + (if (highSpeed) "h" else "")
        }

        fun label(context: Context): String {
            return (if (size.width == 3840) "4K · "
            else if (size.width == 1920) "FHD · " else if (size.width == 1280) "HD · " else "") +
                size.height +
                " × " +
                size.width +
                " / " +
                fps +
                " fps" +
                (if (highSpeed) context.getString(R.string.ui_no_effects) else "")
        }
    }

    internal class RawVideo(val size: Size, val maxFps: Int) {
        fun label(context: Context): String {
            return size.toString() +
                context.getString(R.string.ui_estimated_raw_limit) +
                maxFps +
                " fps"
        }
    }

    val rawVideos: MutableList<RawVideo> = ArrayList<RawVideo>()

    @Volatile var rawVideoFailure: String? = null

    fun rawVideo(s: CaptureSettings): RawVideo? {
        for (v in rawVideos) if (v.size.toString() == s.rawVideoSize) return v
        return if (rawVideos.isEmpty()) null else rawVideos.get(0)
    }

    fun rawVideoAvailable(): Boolean {
        return !rawVideos.isEmpty() && rawVideoFailure == null
    }

    fun rawVideoReason(context: Context): String? {
        return if (rawVideoFailure != null) rawVideoFailure
        else if (rawVideos.isEmpty())
            context.getString(R.string.ui_continuous_raw_output_is_unavailable_on_this_camera)
        else context.getString(R.string.ui_raw_output_available_checked_when_selected)
    }

    val photos: MutableList<Photo> = ArrayList<Photo>()
    val raws: MutableList<Photo> = ArrayList<Photo>()
    val videos: MutableList<Video> = ArrayList<Video>()
    val encoders: MutableMap<String?, MutableList<VideoCapabilities>?> =
        HashMap<String?, MutableList<VideoCapabilities>?>()
    val characteristics: CameraCharacteristics?
    val map: StreamConfigurationMap
    var recommendedPhotoPixels: Long = 2073600
    var recommendedVideoPixels: Long = 2073600

    init {
        characteristics = cc
        map =
            requireNotNull(cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)) {
                "Camera has no stream configuration"
            }
        val signals = map.getOutputSizes<SurfaceTexture?>(SurfaceTexture::class.java)
        if (signals != null)
            for (size in signals) if (
                max(
                    size.width,
                    size.height,
                ) <= maxTexture
            )
                photos.add(Photo(size, false))
        addPhotos(raws, map, ImageFormat.RAW_SENSOR, false, Int.MAX_VALUE)
        val full =
            cc.get<StreamConfigurationMap?>(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
            )
        if (full != null) {
            addPhotos(raws, full, ImageFormat.RAW_SENSOR, true, Int.MAX_VALUE)
        }
        photos.sortWith(
            Comparator { a: Photo?, b: Photo? ->
                java.lang.Long.compare(
                    area(b!!.size),
                    area(a!!.size),
                )
            }
        )
        raws.sortWith(
            Comparator { a: Photo?, b: Photo? ->
                java.lang.Long.compare(
                    area(b!!.size),
                    area(a!!.size),
                )
            }
        )
        for (ci in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) {
            if (!ci.isEncoder()) continue
            for (type in ci.supportedTypes) if (type == "video/avc" || type == "video/hevc") {
                try {
                    val caps = ci.getCapabilitiesForType(type)
                    var surface = false
                    for (format in caps.colorFormats) if (
                        format == CodecCapabilities.COLOR_FormatSurface
                    )
                        surface = true
                    val videoCaps = caps.videoCapabilities
                    if (surface && videoCaps != null) {
                        encoders
                            .computeIfAbsent(type) { k: String? ->
                                ArrayList<VideoCapabilities>()
                            }!!
                            .add(videoCaps)
                        Log.i(
                            "SignalCaps",
                            "Encoder " +
                                ci.name +
                                " " +
                                type +
                                " " +
                                videoCaps.supportedWidths +
                                "x" +
                                videoCaps.supportedHeights +
                                " bitrates=" +
                                videoCaps.bitrateRange,
                        )
                    }
                } catch (ignored: Exception) {}
            }
        }
        var normalMax = 0
        val ranges: Array<Range<Int>>? =
            cc.get<Array<Range<Int>>?>(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        if (ranges != null) for (r in ranges) normalMax = max(normalMax, r.upper!!)
        if (normalMax == 0) normalMax = 30
        val textureSizes = map.getOutputSizes<SurfaceTexture?>(SurfaceTexture::class.java)
        require(!(textureSizes == null || textureSizes.size == 0)) {
            "Camera has no preview output"
        }
        for (s in textureSizes) {
            if (max(s.width, s.height) > maxTexture) continue
            val ns = map.getOutputMinFrameDuration<SurfaceTexture?>(SurfaceTexture::class.java, s)
            val possible = if (ns == 0L) normalMax.toDouble() else 1e9 / ns
            for (fps in
                intArrayOf(
                    15,
                    24,
                    30,
                    60,
                )) if (
                fps <= normalMax &&
                    possible + 1 >= fps &&
                    anyEncoder(
                        s,
                        fps,
                    )
            )
                videos.add(Video(s, fps, false))
        }
        val capabilities = cc.get<IntArray?>(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        var rawCap = false
        if (capabilities != null)
            for (capability in capabilities) if (
                capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
                rawCap = true
        if (rawCap) {
            val rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR)
            if (rawSizes != null)
                for (size in rawSizes) {
                    // Bound two queued sensor frames to 128 MiB; maximum-resolution-only still
                    // modes are excluded.
                    val active =
                        cc.get<Rect?>(
                            CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE
                        )
                    val pixels = cc.get<Size?>(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    val dngSize =
                        (pixels != null && pixels == size) ||
                            (active != null &&
                                active.width() == size.width &&
                                active.height() == size.height)
                    if (!dngSize || area(size) * 2 > 64L * 1024 * 1024) continue
                    val duration = map.getOutputMinFrameDuration(ImageFormat.RAW_SENSOR, size)
                    val stall = map.getOutputStallDuration(ImageFormat.RAW_SENSOR, size)
                    if (duration <= 0) continue
                    val limit =
                        min(30, min(normalMax, (1000000000L / max(1, duration + stall)).toInt()))
                    if (limit >= 1) rawVideos.add(RawVideo(size, limit))
                }
        }
        rawVideos.sortWith(
            Comparator { a: RawVideo?, b: RawVideo? ->
                java.lang.Long.compare(
                    area(a!!.size),
                    area(b!!.size),
                )
            }
        )
        for (v in rawVideos) Log.i(
            "SignalCaps",
            "RAW VIDEO " + v.size + " / max " + v.maxFps + " fps",
        )
        // This GL processing path uses normal sessions, not constrained high-speed bursts.
        // Offer normal streams whose frame rate this processing path can deliver.
        videos.sortWith(
            Comparator { a: Video?, b: Video? ->
                val compare = java.lang.Long.compare(area(b!!.size), area(a!!.size))
                if (compare == 0) Integer.compare(b.fps, a.fps) else compare
            }
        )
        Log.i(
            "SignalCaps",
            "Camera " +
                id +
                " maxPixelMode=" +
                (full != null) +
                " JPEG=" +
                labels(photos) +
                " RAW=" +
                labels(raws),
        )
        for (v in videos) Log.i(
            "SignalCaps",
            "Video " +
                v.key() +
                " HEVC=" +
                supports(v, "video/hevc") +
                " AVC=" +
                supports(
                    v,
                    "video/avc",
                ),
        )
    }

    fun anyEncoder(size: Size, fps: Int): Boolean {
        return supportsSize(size, fps, "video/hevc") || supportsSize(size, fps, "video/avc")
    }

    fun supports(v: Video, codec: String?): Boolean {
        return supportsSize(v.size, v.fps, codec)
    }

    fun supportsSize(s: Size, fps: Int, codec: String?): Boolean {
        for (caps in encoders.getOrDefault(codec, mutableListOf<VideoCapabilities>())!!) try {
            if (
                caps!!.areSizeAndRateSupported(
                    s.height,
                    s.width,
                    fps.toDouble(),
                )
            )
                return true
        } catch (ignored: Exception) {}
        return false
    }

    fun videosFor(codec: String?): MutableList<Video> {
        val out: MutableList<Video> = ArrayList<Video>()
        for (v in videos) if (supports(v, codec)) out.add(v)
        return out
    }

    fun photo(s: CaptureSettings): Photo? {
        val list = if (s.photoFormat == 0) photos else raws
        if (list.isEmpty()) return null
        for (p in list) if (p.key() == s.photoSize) return p
        if ("max" == s.photoSize) return list.get(0)
        if ("recommended" == s.photoSize && s.photoFormat == 0) {
            var fallback: Photo? = null
            for (p in list) if (area(p.size) <= recommendedPhotoPixels) {
                if (fallback == null) fallback = p
                var duration: Long = 0
                try {
                    duration =
                        map!!.getOutputMinFrameDuration<SurfaceTexture?>(
                            SurfaceTexture::class.java,
                            p.size,
                        )
                } catch (ignored: IllegalArgumentException) {}
                if (duration == 0L || duration <= 50000000L) return p
            }
            return if (fallback == null) list.get(list.size - 1) else fallback
        }
        for (p in list) if (
            !p.maximumPixelMode && area(p.size) <= (if (s.photoFormat == 0) 2073600 else 12000000)
        )
            return p
        return list.get(list.size - 1)
    }

    fun video(s: CaptureSettings): Video? {
        val list = videosFor(s.codec)
        if (list.isEmpty()) return null
        for (v in list) if (v.key() == s.videoKey) return v
        if ("recommended" == s.videoKey || s.videoKey.isEmpty()) {
            var best: Video? = null
            for (v in list) if (
                !v.highSpeed &&
                    v.fps <= 30 &&
                    area(v.size) <= recommendedVideoPixels &&
                    (best == null ||
                        area(v.size) > area(best.size) ||
                        area(v.size) == area(best.size) && v.fps > best.fps)
            )
                best = v
            if (best != null) return best
            for (v in list) if (
                !v.highSpeed && v.fps <= 30 && (best == null || area(v.size) < area(best.size))
            )
                best = v
            if (best != null) return best
            return list.get(list.size - 1)
        }
        for (v in list) if (area(v.size) == area(list.get(0).size) && v.fps == 30) return v
        return list.get(0)
    }

    fun bitrate(v: Video, settings: CaptureSettings): Int {
        val pixels = area(v.size).toDouble()
        val bpp =
            if (settings.videoQuality == 0) .10 else if (settings.videoQuality == 1) .22 else .48
        var desired =
            (pixels * v.fps * bpp * (if (settings.codec == "video/hevc") .8 else 1.0)).toLong()
        var ceiling = 0
        var floor = Int.MAX_VALUE
        for (c in
            encoders.getOrDefault(
                settings.codec,
                mutableListOf<VideoCapabilities>(),
            )!!) try {
            if (
                c!!.areSizeAndRateSupported(
                    v.size.height,
                    v.size.width,
                    v.fps.toDouble(),
                )
            ) {
                ceiling = max(ceiling, c.bitrateRange.upper)
                floor = min(floor, c.bitrateRange.lower)
            }
        } catch (ignored: Exception) {}
        if (ceiling == 0) return 16000000
        if (settings.videoQuality >= 3) desired = ceiling.toLong()
        return max(floor.toLong(), min(ceiling.toLong(), max(4000000, desired))).toInt()
    }

    fun previewFor(p: Photo): Size {
        var best: Size? = null
        var score = Double.MAX_VALUE
        val aspect = p.size.width.toDouble() / p.size.height
        for (s in map!!.getOutputSizes<SurfaceTexture?>(SurfaceTexture::class.java)) if (
            s.width <= 1920 && s.height <= 1080
        ) {
            val d = abs(s.width.toDouble() / s.height - aspect) * 10000 + abs(s.width - 1440)
            if (d < score) {
                score = d
                best = s
            }
        }
        return if (best == null) map.getOutputSizes<SurfaceTexture?>(SurfaceTexture::class.java)[0]
        else best
    }

    companion object {
        fun area(s: Size): Long {
            return s.width.toLong() * s.height
        }

        fun labels(choices: MutableList<Photo>): String {
            val s = StringBuilder()
            for (i in 0..<min(5, choices.size)) s.append(choices.get(i).key()).append(' ')
            return s.toString()
        }

        fun addPhotos(
            list: MutableList<Photo>,
            m: StreamConfigurationMap?,
            format: Int,
            full: Boolean,
            maxTexture: Int,
        ) {
            if (m == null) return
            val sizes = m.getOutputSizes(format)
            if (sizes != null)
                for (s in sizes) if (
                    max(
                        s.width,
                        s.height,
                    ) <= maxTexture
                )
                    list.add(Photo(s, full))
            val high = m.getHighResolutionOutputSizes(format)
            if (high != null)
                for (s in high) if (
                    max(
                        s.width,
                        s.height,
                    ) <= maxTexture &&
                        list.stream().noneMatch { p: Photo? ->
                            p!!.size == s && p.maximumPixelMode == full
                        }
                )
                    list.add(Photo(s, full))
        }
    }
}
