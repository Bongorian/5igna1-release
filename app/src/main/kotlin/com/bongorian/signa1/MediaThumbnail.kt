package com.bongorian.signa1

import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Loads only the app's last saved URI. No photo-library permission or main-thread decoding. */
internal class MediaThumbnail(val activity: MainActivity) : FrameLayout(activity) {
    val image: ImageView
    val badge: TextView
    val worker: ExecutorService = Executors.newSingleThreadExecutor()
    var cancellation: CancellationSignal? = null
    var requested: Uri? = null
    var loading: Boolean = false
    var revision: Int = 0
    var disposed: Boolean = false

    @Volatile var hasThumbnail: Boolean = false

    init {
        setBackground(activity.bg(MainActivity.PANEL, -0xc6bfc7))
        setClipToOutline(true)
        setClickable(true)
        image = ImageView(activity)
        image.setScaleType(ImageView.ScaleType.CENTER_CROP)
        addView(image, LayoutParams(-1, -1))
        badge = activity.text("", 8, MainActivity.WHITE)
        badge.setGravity(Gravity.CENTER)
        badge.setBackground(activity.detailBg(-0x33f5f3f3, 0))
        val bp = LayoutParams(-2, activity.dp(17f), Gravity.BOTTOM or Gravity.END)
        bp.setMargins(0, 0, activity.dp(3f), activity.dp(3f))
        badge.setPadding(activity.dp(4f), 0, activity.dp(4f), 0)
        addView(badge, bp)
        placeholder()
    }

    fun placeholder() {
        hasThumbnail = false
        image.setScaleType(ImageView.ScaleType.CENTER)
        image.setImageResource(R.drawable.ic_gallery)
        badge.setVisibility(GONE)
        setContentDescription(activity.getString(R.string.ui_open_saved_photos_and_videos))
    }

    fun load(uri: Uri?, video: Boolean) {
        if (disposed) return
        if (uri != null && uri == requested && (loading || hasThumbnail)) return
        requested = uri
        loading = uri != null
        val ticket = ++revision
        if (cancellation != null) cancellation!!.cancel()
        placeholder()
        if (uri == null) return
        setContentDescription(
            if (video) activity.getString(R.string.ui_open_the_last_saved_video)
            else activity.getString(R.string.ui_open_the_last_saved_photo)
        )
        setTooltipText(getContentDescription())
        val signal = CancellationSignal()
        cancellation = signal
        worker.execute(
            Runnable@{
                var bitmap: Bitmap? = null
                var type: String? = null
                try {
                    type = activity.contentResolver.getType(uri)
                    bitmap = activity.contentResolver.loadThumbnail(uri, Size(192, 192), signal)
                } catch (ignored: Exception) {}
                val result = bitmap
                val sequence = "application/zip" == type
                val raw = sequence || (type != null && type.contains("dng"))
                activity.handler.post(
                    Runnable@{
                        if (disposed || ticket != revision) {
                            if (result != null) result.recycle()
                            return@Runnable
                        }
                        loading = false
                        if (result == null) placeholder()
                        if (result != null) {
                            hasThumbnail = true
                            image.setScaleType(ImageView.ScaleType.CENTER_CROP)
                            image.setImageBitmap(result)
                        }
                        badge.setText(
                            if (sequence) "RAW ZIP" else if (video) "▶" else if (raw) "RAW" else ""
                        )
                        if (sequence)
                            setContentDescription(
                                activity.getString(R.string.ui_open_the_last_saved_raw_video_zip)
                            )
                        badge.setVisibility(if (video || raw) VISIBLE else GONE)
                    }
                )
            }
        )
    }

    fun dispose() {
        disposed = true
        revision++
        if (cancellation != null) cancellation!!.cancel()
        worker.shutdownNow()
        image.setImageDrawable(null)
    }
}
