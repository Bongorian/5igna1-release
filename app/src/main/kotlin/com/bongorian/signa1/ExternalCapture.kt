package com.bongorian.signa1

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.IOException
import java.util.concurrent.Executors

/** Keeps the pending result and delivery alive across Activity recreation. No caller URI is opened
 * for writing until the user accepts a completed capture. */
internal class ExternalCaptureModel : ViewModel() {
    lateinit var session: CaptureSession
        private set
    lateinit var request: CameraRequest
        private set
    private lateinit var app: Context
    var captureSettings: CaptureSettings? = null
    var effects: EffectState? = null
    var faults: FaultConfig? = null
    var sound: Boolean? = null
    var source: Uri? = null
        private set
    var delivering = false
        private set
    var result: Intent? = null
        private set
    var failed = false
        private set
    @Volatile private var cancelled = false
    var observer: (() -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    fun initialize(context: Context, intent: Intent, restored: Bundle?) {
        if (::session.isInitialized) return
        app = context.applicationContext
        session = CaptureSession(app, restored)
        effects = restored?.getString("effects")?.let { runCatching { EffectState.decode(it) }.getOrNull() }
        sound = restored?.takeIf { it.containsKey("sound") }?.getBoolean("sound")
        try {
            request = CameraIntents.request(intent)
            request.output?.let { output ->
                require(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
                require(output.authority != app.packageName + ".camera-files")
                // A grant is checked independently of our own MediaStore/provider access. A caller
                // cannot nominate one of our existing files and borrow the camera's permissions.
                require(app.checkUriPermission(output, Process.myPid(), Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED)
            }
            source = restored?.getString("source")?.let(Uri::parse)?.takeIf { session.file(it) != null }
        } catch (_: Exception) {
            failed = true
            request = CameraRequest(intent.action == MediaStore.ACTION_VIDEO_CAPTURE, null, 0, 0, null)
        }
    }

    fun snapshot() = Bundle().apply {
        putString("session", session.id)
        putString("effects", effects?.encode())
        sound?.let { putBoolean("sound", it) }
        putString("source", source?.toString())
    }

    fun saved(uri: Uri, video: Boolean) {
        if (cancelled || failed || result != null || source != null || video != request.video) {
            session.discard(uri)
            return
        }
        val file = session.file(uri)
        if (file == null || file.length() == 0L || request.sizeLimit > 0 && file.length() > request.sizeLimit) {
            session.discard(uri)
            fail()
            return
        }
        source = uri
        observer?.invoke()
    }

    fun fail() {
        if (cancelled || result != null) return
        failed = true
        observer?.invoke()
    }

    fun retake() {
        if (delivering) return
        session.discard(source)
        source = null
        observer?.invoke()
    }

    fun accept() {
        val capture = source ?: return
        if (delivering || cancelled || failed || result != null) return
        delivering = true
        observer?.invoke()
        worker.execute {
            var created: Uri? = null
            var callerWriteStarted = false
            var thumbnail: Bitmap? = null
            try {
                val data = Intent()
                val output = request.output
                if (output != null) {
                    callerWriteStarted = true
                    copyTo(capture, output)
                } else if (request.video) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "5igna1_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/5igna1")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    created = requireNotNull(app.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values))
                    copyTo(capture, created!!)
                    check(!cancelled)
                    app.contentResolver.update(created!!, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                    data.setDataAndType(created, "video/mp4")
                    data.clipData = ClipData.newRawUri("capture", created)
                    data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    thumbnail = thumbnail(app, capture, 256)
                    data.putExtra("data", thumbnail)
                }
                check(!cancelled)
                val published = created
                val bitmap = thumbnail
                main.post {
                    delivering = false
                    if (cancelled) {
                        published?.let { runCatching { app.contentResolver.delete(it, null, null) } }
                        bitmap?.recycle()
                    } else {
                        result = data
                        session.discard(source)
                        source = null
                        observer?.invoke()
                    }
                }
            } catch (_: Exception) {
                created?.let { runCatching { app.contentResolver.delete(it, null, null) } }
                if (callerWriteStarted) request.output?.let { output ->
                    // Do not delete a caller-owned document. Best-effort truncate a failed write.
                    runCatching { app.contentResolver.openOutputStream(output, "wt")?.close() }
                }
                thumbnail?.recycle()
                main.post { delivering = false; fail() }
            }
        }
    }

    private fun copyTo(source: Uri, destination: Uri) {
        app.contentResolver.openInputStream(source)!!.use { input ->
            app.contentResolver.openOutputStream(destination, "wt")!!.use { output ->
                val bytes = ByteArray(64 * 1024)
                while (true) {
                    if (cancelled) throw IOException("Capture cancelled")
                    val count = input.read(bytes)
                    if (count < 0) break
                    output.write(bytes, 0, count)
                }
                output.flush()
            }
        }
    }

    fun cancel() { cancelled = true; session.close() }
    override fun onCleared() {
        observer = null
        cancel()
        worker.shutdown()
    }

    companion object {
        fun thumbnail(context: Context, uri: Uri, bound: Int): Bitmap {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options) }
            require(options.outWidth > 0 && options.outHeight > 0)
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > bound) sample *= 2
            options.inSampleSize = sample
            options.inJustDecodeBounds = false
            return context.contentResolver.openInputStream(uri)!!.use {
                requireNotNull(BitmapFactory.decodeStream(it, null, options))
            }
        }
    }
}

/** Activity-owned presentation; the model owns storage and result delivery. */
internal class ExternalCapture(private val a: MainActivity, restored: Bundle?) {
    val model = ViewModelProvider(a)[ExternalCaptureModel::class.java]
    private var dialog: Dialog? = null
    private var video: VideoView? = null
    private var bitmap: Bitmap? = null
    private var previewSuspended = false
    private val update: () -> Unit = { render() }
    val blocked get() = model.source != null || model.delivering || model.failed || model.result != null

    init {
        a.setResult(Activity.RESULT_CANCELED)
        model.initialize(a, a.intent, restored)
    }

    fun remember() {
        model.captureSettings = CaptureSettings(a.settings)
        model.effects = a.effectState
        model.faults = a.faultConfig
        model.sound = a.sound
    }
    fun attach() { model.observer = update; render() }
    fun pause() { dismiss() }
    fun destroy() {
        if (model.observer === update) model.observer = null
        dismiss()
    }
    fun cancel() { model.cancel(); a.finish() }

    private fun dismiss() {
        video?.stopPlayback()
        video = null
        dialog?.dismiss()
        dialog = null
        bitmap?.recycle()
        bitmap = null
    }

    fun render() {
        if (!a.resumed || a.isFinishing || a.isDestroyed) return
        if (model.failed) {
            Toast.makeText(a, R.string.external_capture_failed, Toast.LENGTH_LONG).show()
            cancel()
            return
        }
        model.result?.let {
            a.setResult(Activity.RESULT_OK, it)
            a.finish()
            return
        }
        val uri = model.source
        if (uri == null) {
            dismiss()
            if (previewSuspended) { previewSuspended = false; a.resumeCameraPreview() }
            return
        }
        if (!previewSuspended) {
            previewSuspended = true
            a.engine.detach()
        }
        a.ready(false)
        if (dialog != null) return
        val d = Dialog(a)
        dialog = d
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setCancelable(false)
        d.setOnKeyListener { _, key, event ->
            if (key == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.action == android.view.KeyEvent.ACTION_UP && !model.delivering) cancel()
                true
            } else false
        }
        val body = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(a.dp(18f), a.dp(16f), a.dp(18f), a.dp(16f))
            setBackgroundColor(MainActivity.BG)
        }
        body.addView(a.title(a.getString(R.string.external_capture_review)))
        val preview: View = if (model.request.video) {
            VideoView(a).also { view ->
                video = view
                view.setVideoURI(uri)
                view.setMediaController(MediaController(a).apply { setAnchorView(view) })
                view.setOnPreparedListener { view.seekTo(1) }
                view.setOnErrorListener { _, _, _ -> model.fail(); true }
            }
        } else {
            ImageView(a).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                try {
                    bitmap = ExternalCaptureModel.thumbnail(a, uri, 1024)
                    setImageBitmap(bitmap)
                } catch (_: Exception) { a.handler.post { model.fail() } }
            }
        }
        body.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(a.text(a.getString(R.string.external_capture_review_note), 13, MainActivity.MUTED))
        fun action(label: Int, tag: String, run: () -> Unit) = a.button(a.getString(label)).apply {
            this.tag = tag
            isEnabled = !model.delivering
            setOnClickListener { run() }
        }
        body.addView(action(if (model.delivering) R.string.external_capture_returning else R.string.external_capture_use, "external-use") {
            dismiss()
            model.accept()
        }, LinearLayout.LayoutParams(-1, a.dp(48f)))
        val row = a.row()
        row.addView(action(R.string.external_capture_retake, "external-retake") {
            dismiss()
            model.retake()
            a.resumeCameraPreview()
        }, LinearLayout.LayoutParams(0, a.dp(48f), 1f))
        row.addView(action(R.string.ui_cancel, "external-cancel") { cancel() }, LinearLayout.LayoutParams(0, a.dp(48f), 1f))
        body.addView(row)
        ButtonSpacing.apply(a, body)
        d.setContentView(body)
        d.show()
        SignalSheet.resize(a, d, .9f)
    }
}
