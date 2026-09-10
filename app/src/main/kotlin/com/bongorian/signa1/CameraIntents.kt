package com.bongorian.signa1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Public Android camera contracts. Secure/lock-screen capture is deliberately not advertised. */
internal object CameraIntents {
    fun launchVideo(action: String?): Boolean? = when (action) {
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, MediaStore.ACTION_IMAGE_CAPTURE -> false
        MediaStore.INTENT_ACTION_VIDEO_CAMERA, MediaStore.ACTION_VIDEO_CAPTURE -> true
        else -> null
    }

    fun returnsCapture(action: String?) = action == MediaStore.ACTION_IMAGE_CAPTURE || action == MediaStore.ACTION_VIDEO_CAPTURE

    @Suppress("DEPRECATION")
    fun request(intent: Intent): CameraRequest {
        val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        require(output == null || output.scheme == "content") { "Expected a content URI" }
        val duration = intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0)
        val bytes = intent.getLongExtra(MediaStore.EXTRA_SIZE_LIMIT, 0L)
        val quality = if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) else null
        require(duration >= 0 && bytes >= 0 && (quality == null || quality in 0..1)) { "Invalid capture limits" }
        return CameraRequest(intent.action == MediaStore.ACTION_VIDEO_CAPTURE, output,
            (duration.toLong() * 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), bytes, quality)
    }
}

internal data class CameraRequest(val video: Boolean, val output: Uri?, val durationMs: Int,
    val sizeLimit: Long, val quality: Int?) {
    fun constrain(settings: CaptureSettings) = CaptureSettings(settings).apply {
        photoFormat = 0
        rawVideo = false
        rawVideoEnabled = false
        // Caller permissions must not be bypassed by the camera's saved location preference.
        location = false
        codec = "video/avc"
        if (quality != null) {
            videoQuality = if (quality == 0) 0 else 2
            videoKey = "recommended"
        }
    }
}

/** Private staging; normal DCIM storage is used only by ordinary launches. */
internal class CaptureSession(context: Context, restored: Bundle?) {
    private val app = context.applicationContext
    val id = restored?.getString("session")?.takeIf { runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false) }
        ?: UUID.randomUUID().toString()
    val directory = File(app.cacheDir, "camera-requests/$id").apply { mkdirs() }
    @Volatile private var closed = false

    @Synchronized fun create(extension: String): Uri {
        check(!closed)
        require(extension == "jpg" || extension == "mp4")
        return uri(File.createTempFile("capture-", ".$extension", directory))
    }

    private fun uri(file: File) = FileProvider.getUriForFile(app, app.packageName + ".camera-files", file)
    fun file(uri: Uri): File? {
        if (uri.scheme != "content" || uri.authority != app.packageName + ".camera-files") return null
        val name = uri.lastPathSegment ?: return null
        val file = File(directory, name)
        return file.takeIf { it.parentFile == directory && it.isFile && this.uri(it) == uri }
    }
    fun discard(uri: Uri?) { uri?.let { file(it)?.delete() } }
    @Synchronized fun close() { closed = true; directory.deleteRecursively() }
}
