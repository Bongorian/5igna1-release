package com.bongorian.signa1

import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ImageInfo
import android.graphics.ImageDecoder.OnHeaderDecodedListener
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.net.Uri
import android.os.CancellationSignal
import android.provider.MediaStore
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.View.OnTouchListener
import android.view.Window
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * App-owned mixed-media viewer. Only the visible item is decoded; close releases playback before
 * camera attachment. No gallery permission, external player, or camera ownership overlap.
 */
internal class MediaPreview(val a: MainActivity, val initial: Uri, val initialVideo: Boolean) {
    internal class Item(
        val uri: Uri,
        val video: Boolean,
        val raw: Boolean,
        val taken: Long,
        val name: String?,
    )

    val dialog: Dialog
    val panel: LinearLayout
    val media: FrameLayout
    val title: TextView
    val page: TextView
    val notice: TextView
    val previous: ImageView
    val next: ImageView
    val play: TextView
    val time: TextView
    val external: ImageView
    val signal: TextView
    var savedSignal: SavedSignal? = null
    var signalDialog: Dialog? = null
    val seek: SeekBar
    val worker: ExecutorService = Executors.newSingleThreadExecutor()
    val scan: CancellationSignal = CancellationSignal()
    val items: MutableList<Item> = ArrayList<Item>()
    var index: Int = 0

    @Volatile var generation: Int = 0

    @Volatile var closed: Boolean = false
    var cameraReleased: Boolean = false
    var prepared: Boolean = false
    var seeking: Boolean = false
    var focusGranted: Boolean = false
    var videoFrameSeen: Boolean = false
    var image: ImageView? = null
    var video: TextureView? = null
    var player: MediaPlayer? = null
    var videoSurface: Surface? = null
    var zoom: Float = 1f
    var videoW: Int = 0
    var videoH: Int = 0
    val audio: AudioManager?
    var focus: AudioFocusRequest? = null

    fun show() {
        a.mediaPreview = this
        a.ready(false)
        a.handler.removeCallbacks(a.previewAcknowledgement)
        a.engine.detach()
        a.engine.gl.post(
            Runnable@{
                a.handler.post(
                    Runnable@{
                        cameraReleased = true
                        if (!closed && video != null && video!!.isAvailable())
                            prepareVideo(
                                generation,
                                video!!.surfaceTexture,
                            )
                    }
                )
            }
        )
        dialog.show()
        dialog.window!!.setLayout(-1, -1)
        panel.requestApplyInsets()
        media.addView(notice, FrameLayout.LayoutParams(-1, -1))
        worker.execute(
            Runnable@{
                val found = scan()
                a.handler.post(
                    Runnable@{
                        if (closed) return@Runnable
                        items.addAll(found)
                        index = 0
                        for (n in items.indices) if (items.get(n).uri == initial) {
                            index = n
                            break
                        }
                        showItem()
                    }
                )
            }
        )
    }

    fun scan(): MutableList<Item> {
        val found: MutableList<Item> = ArrayList<Item>()
        val projection =
            arrayOf<String>(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_TAKEN,
            )
        val selection =
            "(" +
                MediaStore.Files.FileColumns.MEDIA_TYPE +
                "=1 OR " +
                MediaStore.Files.FileColumns.MEDIA_TYPE +
                "=3) AND " +
                MediaStore.MediaColumns.IS_PENDING +
                "=0 AND (" +
                MediaStore.MediaColumns.RELATIVE_PATH +
                "=? OR " +
                MediaStore.MediaColumns.RELATIVE_PATH +
                "=? OR " +
                MediaStore.MediaColumns.RELATIVE_PATH +
                "=?)"
        try {
            a.contentResolver
                .query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    arrayOf<String>("DCIM/5igna1/", "Pictures/5igna1/", "Movies/5igna1/"),
                    MediaStore.MediaColumns.DATE_ADDED +
                        " DESC, " +
                        MediaStore.Files.FileColumns._ID +
                        " DESC",
                    scan,
                )
                .use { cursor ->
                    if (cursor != null)
                        while (cursor.moveToNext()) {
                            val movie = cursor.getInt(1) == 3
                            val mime = cursor.getString(2)
                            val taken = cursor.getLong(5)
                            found.add(
                                Item(
                                    ContentUris.withAppendedId(
                                        if (movie) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        cursor.getLong(0),
                                    ),
                                    movie,
                                    mime != null && mime.contains("dng"),
                                    if (taken > 0) taken else cursor.getLong(4) * 1000,
                                    cursor.getString(3),
                                )
                            )
                        }
                }
        } catch (ignored: Exception) {}
        if (found.stream().noneMatch { item: Item? -> item!!.uri == initial }) {
            var mime: String? = null
            try {
                mime = a.contentResolver.getType(initial)
            } catch (ignored: Exception) {}
            found.add(
                0,
                Item(
                    initial,
                    initialVideo,
                    mime != null && mime.contains("dng"),
                    System.currentTimeMillis(),
                    "",
                ),
            )
        }
        return found
    }

    fun move(delta: Int) {
        if (closed || index + delta < 0 || index + delta >= items.size) return
        index += delta
        showItem()
    }

    fun showItem() {
        if (closed || items.isEmpty()) return
        val ticket = ++generation
        signalDialog?.dismiss()
        signalDialog = null
        savedSignal = null
        signal.isEnabled = false
        signal.text = a.getString(R.string.saved_signal_loading)
        releasePlayback()
        media.removeAllViews()
        image = null
        video = null
        zoom = 1f
        videoH = 0
        videoW = videoH
        prepared = false
        seeking = false
        videoFrameSeen = false
        val item = items.get(index)
        val date =
            DateFormat.getDateFormat(a).format(Date(item.taken)) +
                " · " +
                DateFormat.getTimeFormat(a).format(Date(item.taken))
        title.setText(date)
        page.setText(
            (if (item.video) "MP4" else if (item.raw) "RAW" else "JPG") +
                " · " +
                (index + 1) +
                " / " +
                items.size
        )
        previous.setEnabled(index > 0)
        previous.setAlpha(if (index > 0) 1f else .25f)
        next.setEnabled(index + 1 < items.size)
        next.setAlpha(if (index + 1 < items.size) 1f else .25f)
        play.setVisibility(if (item.video) View.VISIBLE else View.GONE)
        play.setEnabled(false)
        seek.setVisibility(if (item.video) View.VISIBLE else View.GONE)
        seek.setEnabled(false)
        seek.setProgress(0)
        time.setText(if (item.video) "0:00 / —" else a.getString(R.string.media_gestures))
        notice.setText(a.getString(R.string.media_loading))
        notice.setVisibility(View.VISIBLE)
        worker.execute {
            val metadata = runCatching {
                if (item.video) VideoMetadata.read(a,item.uri)
                else a.contentResolver.openInputStream(item.uri)?.use { input ->
                    val exif = android.media.ExifInterface(input)
                    val candidates = listOf(android.media.ExifInterface.TAG_USER_COMMENT,
                        android.media.ExifInterface.TAG_IMAGE_DESCRIPTION).mapNotNull { SavedSignal.read(exif.getAttribute(it)) }
                    candidates.firstOrNull { it.state != null } ?: candidates.firstOrNull()
                }
            }.getOrNull()
            a.handler.post {
                if (!closed && ticket == generation) {
                    savedSignal = metadata
                    signal.text = metadata?.let { a.getString(R.string.saved_signal_label, it.chain) }
                        ?: a.getString(R.string.saved_signal_missing)
                    signal.isEnabled = metadata != null
                }
            }
        }
        if (item.video) {
            video = TextureView(a)
            media.addView(video, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
            video!!.setSurfaceTextureListener(
                object : SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        texture: SurfaceTexture,
                        w: Int,
                        h: Int,
                    ) {
                        prepareVideo(ticket, texture)
                    }

                    override fun onSurfaceTextureSizeChanged(t: SurfaceTexture, w: Int, h: Int) {}

                    override fun onSurfaceTextureDestroyed(t: SurfaceTexture): Boolean {
                        if (ticket == generation) releasePlayback()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(t: SurfaceTexture) {
                        if (ticket == generation) {
                            videoFrameSeen = true
                            notice.setVisibility(View.GONE)
                        }
                    }
                }
            )
        } else {
            image = ImageView(a)
            image!!.setScaleType(ImageView.ScaleType.FIT_CENTER)
            media.addView(image, FrameLayout.LayoutParams(-1, -1))
            worker.execute(
                Runnable@{
                    if (closed || ticket != generation) return@Runnable
                    var bitmap: Bitmap? = null
                    try {
                        if (item.raw)
                            bitmap =
                                a.contentResolver.loadThumbnail(item.uri, Size(1536, 1536), scan)
                        else
                            bitmap =
                                ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(
                                        a.contentResolver,
                                        item.uri,
                                    ),
                                    OnHeaderDecodedListener {
                                        decoder: ImageDecoder?,
                                        info: ImageInfo?,
                                        source: ImageDecoder.Source? ->
                                        val size = info!!.size
                                        val scale = min(1f, 2048f / max(size.width, size.height))
                                        decoder!!.setTargetSize(
                                            max(1, Math.round(size.width * scale)),
                                            max(1, Math.round(size.height * scale)),
                                        )
                                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                                    },
                                )
                    } catch (ignored: Exception) {} catch (ignored: OutOfMemoryError) {}
                    val result = bitmap
                    a.handler.post(
                        Runnable@{
                            if (closed || ticket != generation) {
                                if (result != null) result.recycle()
                                return@Runnable
                            }
                            if (result == null) {
                                notice.setText(a.getString(R.string.media_preview_unavailable))
                                return@Runnable
                            }
                            image!!.setImageBitmap(result)
                            notice.setVisibility(View.GONE)
                        }
                    )
                }
            )
        }
        media.addView(notice, FrameLayout.LayoutParams(-1, -1))
    }

    fun prepareVideo(ticket: Int, texture: SurfaceTexture?) {
        if (closed || ticket != generation || !cameraReleased || player != null) return
        val item = items.get(index)
        if (!item.video) return
        try {
            val candidate = MediaPlayer()
            player = candidate
            videoSurface = Surface(texture)
            candidate.setSurface(videoSurface)
            candidate.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            candidate.setOnPreparedListener(
                OnPreparedListener@{ mp: MediaPlayer? ->
                    if (closed || ticket != generation || player !== mp) return@OnPreparedListener
                    a.handler.removeCallbacks(prepareTimeout)
                    prepared = true
                    videoW = mp!!.videoWidth
                    videoH = mp.videoHeight
                    fitVideo()
                    play.setEnabled(true)
                    seek.setEnabled(true)
                    startPlayback()
                    a.handler.post(tick)
                }
            )
            candidate.setOnVideoSizeChangedListener(
                OnVideoSizeChangedListener { mp: MediaPlayer?, w: Int, h: Int ->
                    if (player === mp) {
                        videoW = w
                        videoH = h
                        fitVideo()
                    }
                }
            )
            candidate.setOnCompletionListener(
                OnCompletionListener { mp: MediaPlayer? ->
                    if (player === mp) {
                        play.setText("▶")
                        play.setContentDescription(a.getString(R.string.media_play))
                        abandonFocus()
                    }
                }
            )
            candidate.setOnErrorListener(
                MediaPlayer.OnErrorListener { mp: MediaPlayer?, what: Int, extra: Int ->
                    if (ticket == generation) {
                        releasePlayback()
                        notice.setText(a.getString(R.string.media_preview_unavailable))
                        notice.setVisibility(View.VISIBLE)
                        play.setEnabled(false)
                        seek.setEnabled(false)
                    }
                    true
                }
            )
            candidate.setDataSource(a, item.uri)
            candidate.prepareAsync()
            a.handler.postDelayed(prepareTimeout, 12000)
        } catch (error: Exception) {
            releasePlayback()
            notice.setText(a.getString(R.string.media_preview_unavailable))
            notice.setVisibility(View.VISIBLE)
        }
    }

    val prepareTimeout: Runnable =
        object : Runnable {
            override fun run() {
                if (!closed && !prepared && player != null) {
                    releasePlayback()
                    notice.setText(a.getString(R.string.media_preview_unavailable))
                    notice.setVisibility(View.VISIBLE)
                }
            }
        }

    fun fitVideo() {
        if (video == null || videoW <= 0 || videoH <= 0 || media.width <= 0 || media.height <= 0)
            return
        val scale = min(media.width / videoW.toFloat(), media.height / videoH.toFloat())
        val p = video!!.layoutParams as FrameLayout.LayoutParams
        val w = Math.round(videoW * scale)
        val h = Math.round(videoH * scale)
        if (p.width != w || p.height != h) {
            p.width = w
            p.height = h
            p.gravity = Gravity.CENTER
            video!!.setLayoutParams(p)
        }
    }

    fun startPlayback() {
        if (!prepared || player == null) return
        try {
            if (audio != null && !focusGranted) {
                val ticket = generation
                val focusedPlayer = player
                focus =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
                        )
                        .setOnAudioFocusChangeListener(
                            OnAudioFocusChangeListener { change: Int ->
                                if (
                                    change <= 0 &&
                                        ticket == generation &&
                                        player === focusedPlayer &&
                                        prepared
                                )
                                    pausePlayback()
                            },
                            a.handler,
                        )
                        .build()
                focusGranted =
                    audio.requestAudioFocus(focus!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (!focusGranted) {
                    notice.setText(a.getString(R.string.media_play))
                    play.setText("▶")
                    play.setContentDescription(a.getString(R.string.media_play))
                    return
                }
            }
            player!!.start()
            play.setText("Ⅱ")
            play.setContentDescription(a.getString(R.string.live_pause))
        } catch (ignored: IllegalStateException) {}
    }

    fun pausePlayback() {
        if (!prepared || player == null) return
        try {
            player!!.pause()
        } catch (ignored: IllegalStateException) {}
        play.setText("▶")
        play.setContentDescription(a.getString(R.string.media_play))
        abandonFocus()
    }

    fun togglePlayback() {
        if (!prepared || player == null) return
        try {
            if (player!!.isPlaying()) pausePlayback() else startPlayback()
        } catch (ignored: IllegalStateException) {}
    }

    fun abandonFocus() {
        if (audio != null && focus != null) audio.abandonAudioFocusRequest(focus!!)
        focus = null
        focusGranted = false
    }

    val tick: Runnable =
        object : Runnable {
            override fun run() {
                if (closed || !prepared || player == null) return
                try {
                    val current = player!!.currentPosition
                    val duration = player!!.duration
                    time.setText(clock(current) + " / " + clock(duration))
                    if (!seeking)
                        seek.setProgress(
                            if (duration <= 0) 0 else (current * 1000L / duration).toInt()
                        )
                } catch (ignored: IllegalStateException) {}
                a.handler.postDelayed(this, 250)
            }
        }

    private fun navigationIcon(resource: Int, label: Int, name: String) =
        a.iconButton(resource, a.getString(label)).apply {
            tag = name
            setPadding(a.dp(10f), a.dp(10f), a.dp(10f), a.dp(10f))
            setColorFilter(MainActivity.WHITE)
            background = a.detailBg(MainActivity.PANEL, 0)
        }

    init {
        audio = a.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        dialog = Dialog(a)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        panel = LinearLayout(a)
        panel.setOrientation(LinearLayout.VERTICAL)
        panel.setBackgroundColor(MainActivity.BG)
        panel.setPadding(a.dp(16f), a.dp(8f), a.dp(16f), a.dp(8f))
        val header = a.row()
        val close = navigationIcon(R.drawable.ic_close, R.string.ui_close, "media-close")
        close.setBackgroundColor(Color.TRANSPARENT)
        header.addView(close, LinearLayout.LayoutParams(a.dp(ControlSize.STANDARD), a.dp(ControlSize.STANDARD)))
        close.setOnClickListener(OnClickListener@{ v: View? -> dismiss() })
        title = a.text(a.getString(R.string.media_preview_title), 13, MainActivity.WHITE)
        title.setSingleLine(true)
        title.setEllipsize(TextUtils.TruncateAt.END)
        title.setGravity(Gravity.CENTER)
        header.addView(title, LinearLayout.LayoutParams(0, a.dp(ControlSize.STANDARD), 1f))
        external = navigationIcon(R.drawable.ic_open_external, R.string.media_open_external, "media-external")
        external.setBackgroundColor(Color.TRANSPARENT)
        header.addView(external, LinearLayout.LayoutParams(a.dp(ControlSize.STANDARD), a.dp(ControlSize.STANDARD)))
        external.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (!items.isEmpty()) a.openExternal(items.get(index).uri)
            }
        )
        panel.addView(header)
        media = FrameLayout(a)
        media.setClipChildren(true)
        media.setContentDescription(a.getString(R.string.media_gestures))
        panel.addView(media, LinearLayout.LayoutParams(-1, 0, 1f))
        media.addOnLayoutChangeListener(
            OnLayoutChangeListener@{
                v: View?,
                l: Int,
                t: Int,
                r: Int,
                b: Int,
                ol: Int,
                ot: Int,
                or: Int,
                ob: Int ->
                fitVideo()
            }
        )
        notice = a.text(a.getString(R.string.media_loading), 13, MainActivity.MUTED)
        notice.setGravity(Gravity.CENTER)
        notice.setPadding(a.dp(20f), a.dp(20f), a.dp(20f), a.dp(20f))
        val footer = LinearLayout(a)
        footer.setOrientation(LinearLayout.VERTICAL)
        footer.setPadding(0, a.dp(12f), 0, 0)
        time = a.text("", 11, MainActivity.MUTED)
        time.setGravity(Gravity.CENTER)
        footer.addView(time, LinearLayout.LayoutParams(-1, a.dp(22f)))
        signal = a.button(a.getString(R.string.saved_signal_loading))
        signal.setTextSize(11f)
        signal.setSingleLine(true)
        signal.ellipsize = TextUtils.TruncateAt.MARQUEE
        signal.marqueeRepeatLimit = -1
        signal.isSelected = true
        signal.isHorizontalFadingEdgeEnabled = true
        signal.setTextColor(MainActivity.LIME)
        signal.isEnabled = false
        signal.setOnClickListener { showSignal() }
        seek = SeekBar(a)
        seek.setMax(1000)
        seek.setContentDescription(a.getString(R.string.media_position))
        seek.setProgressTintList(ColorStateList.valueOf(MainActivity.LIME))
        seek.setThumbTintList(ColorStateList.valueOf(MainActivity.LIME))
        footer.addView(seek, LinearLayout.LayoutParams(-1, a.dp(34f)))
        seek.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onStartTrackingTouch(s: SeekBar?) {
                    seeking = true
                }

                override fun onStopTrackingTouch(s: SeekBar) {
                    if (prepared && player != null)
                        try {
                            player!!.seekTo((player!!.duration * s.progress / 1000L).toInt())
                        } catch (ignored: IllegalStateException) {}
                    seeking = false
                }

                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {}
            }
        )
        val controls = a.row()
        previous = navigationIcon(R.drawable.ic_previous, R.string.media_previous, "media-previous")
        controls.addView(previous, LinearLayout.LayoutParams(a.dp(ControlSize.STANDARD), a.dp(ControlSize.STANDARD)))
        previous.setOnClickListener(OnClickListener@{ v: View? -> move(-1) })
        page = a.text("", 12, MainActivity.MUTED)
        page.setGravity(Gravity.CENTER)
        controls.addView(page, LinearLayout.LayoutParams(0, a.dp(ControlSize.STANDARD), 1f))
        play = a.button("Ⅱ")
        play.setContentDescription(a.getString(R.string.live_pause))
        controls.addView(play, LinearLayout.LayoutParams(a.dp(ControlSize.STANDARD), a.dp(ControlSize.STANDARD)))
        play.setOnClickListener(OnClickListener@{ v: View? -> togglePlayback() })
        next = navigationIcon(R.drawable.ic_next, R.string.media_next, "media-next")
        val np = LinearLayout.LayoutParams(a.dp(ControlSize.STANDARD), a.dp(ControlSize.STANDARD))
        np.leftMargin = a.dp(8f)
        controls.addView(next, np)
        next.setOnClickListener(OnClickListener@{ v: View? -> move(1) })
        footer.addView(controls)
        // Keep media navigation together, with a separate full-width signal action below it.
        val signalPosition = LinearLayout.LayoutParams(-1, a.dp(ControlSize.COMPACT))
        signalPosition.topMargin = a.dp(12f)
        footer.addView(signal, signalPosition)
        panel.addView(footer)
        bindGestures()
        dialog.setContentView(panel)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setDecorFitsSystemWindows(false)
        panel.setOnApplyWindowInsetsListener(
            OnApplyWindowInsetsListener@{ v: View?, insets: WindowInsets? ->
                val safe =
                    insets!!.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                    )
                panel.setPadding(
                    a.dp(16f) + safe.left,
                    a.dp(8f) + safe.top,
                    a.dp(16f) + safe.right,
                    a.dp(8f) + safe.bottom,
                )
                insets
            }
        )
        dialog.setOnDismissListener(OnDismissListener@{ v: DialogInterface? -> dismiss() })
    }

    fun showSignal() {
        val selected = savedSignal ?: return
        val ticket = generation
        val body = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        fun block(text: String, size: Int, color: Int, gap: Int, medium: Boolean = false): TextView {
            val label = a.text(text,size,color)
            a.typography(label,size,medium)
            label.setTextIsSelectable(true)
            body.addView(label,LinearLayout.LayoutParams(-1,-2).apply { topMargin = a.dp(gap.toFloat()) })
            return label
        }
        block(a.getString(R.string.saved_signal_heading),10,MainActivity.MUTED,0,true).letterSpacing = .12f
        block(selected.chain,18,MainActivity.WHITE,8,true)
        selected.state?.let { block(a.getString(R.string.saved_signal_level,Math.round(it.amount*100)),12,MainActivity.LIME,12,true) }
        block(a.getString(R.string.saved_signal_scope),11,MainActivity.MUTED,20)
        if (selected.state == null) block(a.getString(R.string.saved_signal_incomplete),11,MainActivity.MUTED,12)
        body.addView(View(a).apply { setBackgroundColor(MainActivity.PANEL) },
            LinearLayout.LayoutParams(-1,a.dp(1f)).apply { topMargin = a.dp(20f); bottomMargin = a.dp(16f) })
        block(a.getString(R.string.saved_signal_metadata),10,MainActivity.MUTED,0,true).letterSpacing = .12f
        val data = block(selected.description.replace(" | ","\n\n"),10,MainActivity.MUTED,10)
        data.typeface = android.graphics.Typeface.MONOSPACE
        data.setLineSpacing(a.dp(3f).toFloat(),1f)
        signalDialog = SignalSheet.content(a,a.getString(R.string.saved_signal_title),body,
            if (selected.state != null) R.string.saved_signal_use else 0,
            Runnable {
                if (!closed && ticket == generation && savedSignal === selected) {
                    if (a.engine.photoBusy || a.recording) {
                        android.widget.Toast.makeText(a,R.string.ui_change_settings_after_capture_finishes,android.widget.Toast.LENGTH_SHORT).show()
                        return@Runnable
                    }
                    a.commitEffects(selected.state!!)
                    selected.experimental?.let { enabled ->
                        if (a.settings.experimentalSignals != enabled)
                            a.applySettings(CaptureSettings(a.settings).apply { experimentalSignals = enabled })
                    }
                    android.widget.Toast.makeText(a,R.string.saved_signal_applied,android.widget.Toast.LENGTH_SHORT).show()
                }
            }, .75f)
    }

    fun releasePlayback() {
        a.handler.removeCallbacks(tick)
        a.handler.removeCallbacks(prepareTimeout)
        prepared = false
        abandonFocus()
        val old = player
        player = null
        if (old != null) {
            try {
                old.setOnPreparedListener(null)
                old.setOnCompletionListener(null)
                old.setOnErrorListener(null)
            } catch (ignored: RuntimeException) {}
            try {
                old.release()
            } catch (ignored: RuntimeException) {}
        }
        if (videoSurface != null) {
            videoSurface!!.release()
            videoSurface = null
        }
    }

    fun bindGestures() {
        val scale =
            ScaleGestureDetector(
                a,
                object : SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        if (image == null) return false
                        zoom = max(1f, min(5f, zoom * detector.scaleFactor))
                        image!!.setScaleX(zoom)
                        image!!.setScaleY(zoom)
                        clampImage()
                        return true
                    }
                },
            )
        media.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (image != null && zoom > 1) {
                    zoom = 1f
                    image!!.setScaleX(1f)
                    image!!.setScaleY(1f)
                    image!!.setTranslationX(0f)
                    image!!.setTranslationY(0f)
                } else togglePlayback()
            }
        )
        media.setOnTouchListener(
            object : OnTouchListener {
                var x: Float = 0f
                var y: Float = 0f
                var lastX: Float = 0f
                var lastY: Float = 0f
                var multiple: Boolean = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    scale.onTouchEvent(event)
                    val rawX = event.rawX
                    val rawY = event.rawY
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            run {
                                lastX = rawX
                                x = lastX
                            }
                            run {
                                lastY = rawY
                                y = lastY
                            }
                            multiple = false
                            panel.animate().cancel()
                            return true
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            multiple = true
                            panel.setTranslationY(0f)
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (scale.isInProgress()) return true
                            if (image != null && zoom > 1) {
                                image!!.setTranslationX(image!!.translationX + rawX - lastX)
                                image!!.setTranslationY(image!!.translationY + rawY - lastY)
                                clampImage()
                            } else if (!multiple && rawY - y > 0 && rawY - y > abs(rawX - x) * 1.2f)
                                panel.setTranslationY((rawY - y) * .75f)
                            lastX = rawX
                            lastY = rawY
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            val dx = rawX - x
                            val dy = rawY - y
                            if (!multiple && zoom <= 1 && dy > a.dp(80f) && dy > abs(dx) * 1.2f) {
                                dismiss()
                                return true
                            }
                            if (
                                !multiple &&
                                    zoom <= 1 &&
                                    abs(dx) > a.dp(64f) &&
                                    abs(dx) > abs(dy) * 1.2f
                            )
                                move(if (dx < 0) 1 else -1)
                            else if (!multiple && abs(dx) < a.dp(8f) && abs(dy) < a.dp(8f))
                                v.performClick()
                            panel.animate().translationY(0f).setDuration(140).start()
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            panel.animate().translationY(0f).setDuration(140).start()
                            return true
                        }
                    }
                    return true
                }
            }
        )
    }

    fun clampImage() {
        if (image == null || image!!.drawable == null) return
        val ratio =
            image!!.drawable.intrinsicWidth /
                max(
                        1,
                        image!!.drawable.intrinsicHeight,
                    )
                    .toFloat()
        val w = min(media.width.toFloat(), media.height * ratio)
        val h = w / ratio
        val maxX = max(0f, (w * zoom - media.width) / 2)
        val maxY = max(0f, (h * zoom - media.height) / 2)
        image!!.setTranslationX(max(-maxX, min(maxX, image!!.translationX)))
        image!!.setTranslationY(max(-maxY, min(maxY, image!!.translationY)))
    }

    fun dismiss() {
        if (closed) return
        closed = true
        signalDialog?.dismiss()
        signalDialog = null
        generation++
        scan.cancel()
        panel.animate().cancel()
        releasePlayback()
        worker.shutdownNow()
        if (image != null) image!!.setImageDrawable(null)
        dialog.dismiss()
        if (a.mediaPreview == this) {
            a.mediaPreview = null
            a.resumeCameraPreview()
        }
    }

    companion object {
        fun clock(millis: Int): String {
            val seconds = max(0, millis / 1000)
            return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
        }
    }
}
