package com.bongorian.signa1

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.security.SecureRandom
import java.util.Locale
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

internal class MainActivity : AppCompatActivity(), GlitchEngine.Listener, SurfaceTextureListener {
    lateinit var engine: GlitchEngine
    lateinit var preview: TextureView
    lateinit var viewfinder: FrameLayout
    lateinit var previewArea: FrameLayout
    var cameraOptions: CameraOptions? = null
    lateinit var settings: CaptureSettings
    lateinit var geo: GeoTags
    lateinit var geoButton: ImageView
    lateinit var torchButton: ImageView
    lateinit var micButton: ImageView
    var displayAspect: Float = .75f
    var measuredFps: Float = 0f
    lateinit var status: TextView
    lateinit var count: TextView
    lateinit var strengthValue: TextView
    lateinit var photoTab: TextView
    lateinit var videoTab: TextView
    lateinit var zoomButton: TextView
    lateinit var flipButton: ImageView
    lateinit var galleryButton: MediaThumbnail
    lateinit var selectedRoute: LinearLayout
    lateinit var formatButton: TextView
    lateinit var strength: SeekBar
    lateinit var capture: CaptureButton
    lateinit var overlay: Overlay
    var resumed: Boolean = false
    var ready: Boolean = false
    var recording: Boolean = false
    var videoMode: Boolean = false
    var sound: Boolean = true
    var latestVideo: Boolean = false
    var advancedMode: Boolean = false
    var captureCount: Int = 0
    var tutorial: TutorialDialog? = null
    var tutorialPage: Int = -1
    lateinit var effectState: EffectState
    lateinit var liveChainStatus: TextView
    var liveChainDialog: Dialog? = null
    var faultStatePanel: FaultStateDialog? = null
    var shownLiveFrame: EffectState.Frame? = null
    var liveEditor: FaultDialog.Editor? = null
    lateinit var liveTransport: LinearLayout
    lateinit var liveHold: TextView
    var faultConfig: FaultConfig = FaultConfig.defaults()
    var pendingFaultConfig: FaultConfig? = null
    lateinit var faultSwitch: ToggleButton

    internal class EffectPreview(val base: EffectState, val video: Boolean, val format: Int) {
        var dialog: Dialog? = null
    }

    private var effectPreview: EffectPreview? = null
    var start: Long = 0
    var zoom: Float = 1f
    var latest: Uri? = null
    var mediaPreview: MediaPreview? = null
    val handler: Handler = Handler(Looper.getMainLooper())

    // Some devices consume a TextureView buffer during dialog/resize transitions without
    // delivering its update callback. Read only the UI-consumed timestamp to release the
    // bounded history; never acknowledge a merely submitted GL buffer.
    val previewAcknowledgement: Runnable =
        object : Runnable {
            override fun run() {
                if (!resumed || mediaPreview != null || tutorial != null) return
                if (preview.isAvailable())
                    engine.previewPresented(preview.surfaceTexture!!.timestamp)
                handler.postDelayed(this, 100)
            }
        }
    val timer: Runnable =
        object : Runnable {
            override fun run() {
                if (recording) {
                    val sec = (SystemClock.elapsedRealtime() - start) / 1000
                    status.setText(
                        if (settings.rawVideo)
                            String.format(
                                Locale.US,
                                "● RAW %02d:%02d  ",
                                sec / 60,
                                sec % 60,
                            ) + engine.rawProgress()
                        else
                            String.format(
                                Locale.US,
                                "● REC %02d:%02d  %.0f fps",
                                sec / 60,
                                sec % 60,
                                measuredFps,
                            )
                    )
                    handler.postDelayed(this, 250)
                }
            }
        }

    fun dp(n: Float): Int {
        return (n * getResources().displayMetrics.density + .5f).toInt()
    }

    public override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        getWindow().setDecorFitsSystemWindows(false)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        getWindow().decorView.setSystemUiVisibility(0)
        val prefs = getSharedPreferences("signal", 0)
        sound = prefs.getBoolean("sound", true)
        advancedMode = prefs.getBoolean("advancedMode", false)
        val last = prefs.getString("last", null)
        if (last != null) latest = Uri.parse(last)
        latestVideo = prefs.getBoolean("lastVideo", false)
        if (b != null) {
            videoMode = b.getBoolean("session.video", false)
            zoom = b.getFloat("session.zoom", 1f)
            captureCount = b.getInt("session.count", 0)
        }
        settings = CaptureSettings.load(prefs)
        geo = GeoTags(this, Runnable { this.renderGeo() })
        geo.enabled = settings.location
        effectState = EffectStateStore.load(prefs)
        engine = GlitchEngine(this, this)
        engine.position = Supplier { geo.snapshot() }
        engine.configure(settings, videoMode, effectState)
        if (b != null) {
            engine.front = b.getBoolean("session.front", false)
            engine.zoom = zoom
        }
        faultConfig = FaultPreferences.load(prefs)
        if (b != null) faultConfig = faultConfig.enabled(b.getBoolean("session.live", false))
        engine.applyFaultConfig(faultConfig)
        buildUi()
        tutorialPage =
            if (b != null && b.containsKey("tutorial.page")) b.getInt("tutorial.page")
            else
                (if (
                    prefs.getBoolean(
                        TutorialDialog.SEEN,
                        false,
                    )
                )
                    -1
                else 0)
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putInt("tutorial.page", if (tutorial == null) tutorialPage else tutorial!!.page)
        savePrefs()
        out.putInt("session.count", captureCount)
        out.putBoolean("session.video", videoMode)
        out.putBoolean("session.front", engine.front)
        out.putFloat("session.zoom", zoom)
        out.putBoolean("session.live", faultConfig.enabled)
    }

    fun bg(color: Int, border: Int): GradientDrawable {
        return roundedBackground(color, UI_RADIUS, border)
    }

    fun detailBg(color: Int, border: Int): GradientDrawable {
        return roundedBackground(color, DETAIL_RADIUS, border)
    }

    private fun roundedBackground(color: Int, radius: Int, border: Int): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.setCornerRadius(dp(radius.toFloat()).toFloat())
        if (border != 0) d.setStroke(dp(1f), border)
        return d
    }

    fun typography(view: TextView, size: Int, medium: Boolean) {
        view.setTextSize(size.toFloat())
        view.setTypeface(
            Typeface.create(
                if (medium) "sans-serif-medium" else "sans-serif",
                Typeface.NORMAL,
            )
        )
        view.setLetterSpacing(0f)
        view.setFontFeatureSettings("kern,tnum")
        view.setIncludeFontPadding(false)
        view.setLineSpacing(dp(2f).toFloat(), 1f)
        if (Build.VERSION.SDK_INT >= 28) view.setFallbackLineSpacing(true)
    }

    fun title(label: String?): TextView {
        val view = text(label, TEXT_TITLE, WHITE)
        typography(view, TEXT_TITLE, true)
        return view
    }

    fun text(label: String?, size: Int, color: Int): TextView {
        val t = TextView(this)
        t.setText(label)
        t.setTextColor(color)
        typography(t, size, false)
        t.setGravity(Gravity.CENTER_VERTICAL)
        return t
    }

    fun button(label: String?): TextView {
        val t = text(label, 12, WHITE)
        t.setGravity(Gravity.CENTER)
        typography(t, TEXT_BODY, true)
        t.setBackground(bg(PANEL, 0))
        t.setPadding(dp(12f), 0, dp(12f), 0)
        t.setContentDescription(label)
        return t
    }

    fun iconButton(resource: Int, description: String?): ImageView {
        val icon = ImageView(this)
        icon.setImageResource(resource)
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        icon.setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
        icon.setContentDescription(description)
        icon.setTooltipText(description)
        icon.setFocusable(true)
        icon.setClickable(true)
        return icon
    }

    fun row(): LinearLayout {
        val l = LinearLayout(this)
        l.setOrientation(LinearLayout.HORIZONTAL)
        l.setGravity(Gravity.CENTER_VERTICAL)
        return l
    }

    lateinit var cameraRoot: LinearLayout
    var effectEditorSpace: Space? = null
    var effectEditorOwner: Any? = null
    val editorHiddenViews: MutableMap<View?, Int?> = LinkedHashMap<View?, Int?>()

    fun reserveEffectEditor(owner: Any?, pixels: Int) {
        effectEditorOwner = owner
        if (effectEditorSpace == null) {
            var belowPreview = false
            for (i in 0..<cameraRoot.childCount) {
                val child = cameraRoot.getChildAt(i)
                if (belowPreview) {
                    editorHiddenViews.put(child, child.visibility)
                    child.setVisibility(View.GONE)
                }
                if (child === previewArea) belowPreview = true
            }
            effectEditorSpace = Space(this)
            cameraRoot.addView(effectEditorSpace)
        }
        // Dialog ends at the usable window bottom; reserve that same height in the camera layout.
        effectEditorSpace!!.setLayoutParams(LinearLayout.LayoutParams(-1, pixels))
    }

    fun restoreEffectEditor(owner: Any?) {
        if (effectEditorOwner !== owner) return
        effectEditorOwner = null
        if (effectEditorSpace != null) {
            cameraRoot.removeView(effectEditorSpace)
            effectEditorSpace = null
        }
        editorHiddenViews.forEach { (obj: View?, visibility: Int?) ->
            obj!!.setVisibility(visibility!!)
        }
        editorHiddenViews.clear()
    }

    fun rawOriginal(): Boolean {
        return if (videoMode) settings.rawVideo else settings.photoFormat == 1
    }

    fun availableEffects(): IntArray {
        return Effects.choices(videoMode, !videoMode && settings.photoFormat == 2)
            .filter { settings.experimentalSignals || !Effects.physical(it) }.toIntArray()
    }

    fun uiEffects(): IntArray {
        return if (rawOriginal()) IntArray(0)
        else
            effectState
                .snapshot(
                    videoMode,
                    settings.photoFormat,
                )
                .ids().filter { settings.experimentalSignals || !Effects.physical(it) }.toIntArray()
    }

    fun nextAvailable(delta: Int): Int {
        val ids = availableEffects()
        for (n in ids.indices) if (ids[n] == effectState.selected())
            return ids[
                Math.floorMod(
                    n + delta,
                    ids.size,
                )]
        return Effects.CLEAN
    }

    fun chooseEffect(id: Int) {
        commitEffects(effectState.single(id))
    }

    fun commitEffects(next: EffectState) {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Effect edits require UI thread" }
        cancelEffectPreview()
        effectState = next
        engine.setEffects(effectState)
        renderEffects()
        savePrefs()
    }

    fun beginEffectPreview(): EffectPreview {
        if (liveEditor != null) liveEditor!!.dialog!!.dismiss()
        cancelEffectPreview()
        effectPreview = EffectPreview(effectState, videoMode, settings.photoFormat)
        return effectPreview!!
    }

    fun validPreview(edit: EffectPreview): Boolean {
        return effectPreview == edit &&
            effectState == edit.base &&
            videoMode == edit.video &&
            settings.photoFormat == edit.format
    }

    fun previewEffectEdit(edit: EffectPreview, draft: EffectState) {
        if (validPreview(edit)) {
            engine.previewEffects(draft.forContext(videoMode, settings.photoFormat))
            val tag = viewfinder.findViewWithTag<TextView>("fx")
            tag.setText(getString(R.string.ui_preview_editing))
        }
    }

    fun finishEffectEdit(edit: EffectPreview, draft: EffectState, apply: Boolean) {
        if (effectPreview != edit) return
        val valid = validPreview(edit)
        effectPreview = null
        if (apply && valid) commitEffects(draft)
        else {
            engine.clearEffectPreview()
            renderEffects()
            if (apply)
                Toast.makeText(
                        this,
                        getString(R.string.ui_edits_discarded_because_the_capture_mode_changed),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
        }
    }

    fun cancelEffectPreview() {
        val edit = effectPreview
        if (edit == null) return
        effectPreview = null
        engine.clearEffectPreview()
        if (edit.dialog != null) edit.dialog!!.dismiss()
    }

    fun renderEffects() {
        liveTransport.setVisibility(
            if (
                faultConfig.enabled && !rawOriginal() && effectPreview == null && liveEditor == null
            )
                View.VISIBLE
            else View.GONE
        )
        liveHold.setText(
            getString(
                if (faultConfig.performance.hold) R.string.live_resume else R.string.live_pause
            )
        )
        liveHold.setTextColor(if (faultConfig.performance.hold) LIME else MUTED)

        val original = rawOriginal()
        val active = uiEffects()
        formatButton.setText(
            if (videoMode) (if (settings.rawVideo) "RAW ZIP ▾" else "MP4 ▾")
            else if (settings.photoFormat == 0) "JPG ▾" else "RAW ▾"
        )
        selectedRoute.removeAllViews()
        for (id in effectState.ids()) {
            val enabled = effectAvailable(id)
            val chip =
                button(
                    Effects.name(id) +
                        (if (enabled) "" else " · " + (if (videoMode) "MP4" else "JPG"))
                )
            chip.setTextSize(10f)
            chip.setTextColor(if (enabled) LIME else MUTED)
            val cp = LinearLayout.LayoutParams(-2, dp(44f))
            cp.rightMargin = dp(6f)
            selectedRoute.addView(chip, cp)
            chip.setOnClickListener(OnClickListener@{ v: View? -> showEffect(id) })
        }
        if (effectState.mask == 0) {
            val empty = button(getString(R.string.fault_chain_empty))
            empty.setTextColor(MUTED)
            selectedRoute.addView(empty, LinearLayout.LayoutParams(-2, dp(44f)))
            empty.setOnClickListener(OnClickListener@{ v: View? -> showChain() })
        }
        val tag = viewfinder.findViewWithTag<TextView>("fx")
        tag.setText(if (original) "  RAW / ORIGINAL  " else "  FX / " + active.size + "  ")
        strength.setProgress(Math.round(effectState.amount * 100))
        strengthValue.setText(strength.progress.toString() + "%")
        val processing = !original && active.size > 0
        strength.setEnabled(processing)
        strength.setAlpha(if (processing) 1f else .35f)
        if (effectPreview != null) tag.setText(getString(R.string.ui_preview_editing))
    }

    fun liveChainDetails(frame: EffectState.Frame): String {
        val text = StringBuilder(Effects.chainName(frame.ids()))
        for (n in frame.nodes) text
            .append("\n")
            .append(Effects.name(n.id))
            .append(" · ")
            .append(getString(R.string.fault_incident))
            .append(" ")
            .append(Math.round(n.event.envelope * 100))
            .append("%")
        return text.toString()
    }

    override fun liveFrame(frame: EffectState.Frame) {
        shownLiveFrame = frame
        if (effectEditorOwner is EffectDialog) {
            val controls = (effectEditorOwner as EffectDialog).advancedControls
            if (controls != null) controls.update(frame)
        }
        if (faultStatePanel != null && liveChainDialog != null && liveChainDialog!!.isShowing())
            faultStatePanel!!.update(frame)
        val visible = faultConfig.enabled && !rawOriginal() && effectPreview == null
        liveChainStatus.setVisibility(if (visible) View.VISIBLE else View.GONE)
        if (!visible) return
        val label =
            getString(R.string.ui_live_fault_current_chain) +
                "  ·  " +
                frame.nodes.size +
                "  ·  " +
                FaultDialog.styles(this)[faultConfig.performance.style] +
                "   ›"
        if (!label.contentEquals(liveChainStatus.text)) liveChainStatus.setText(label)
        liveChainStatus.setContentDescription(
            label + getString(R.string.ui_tap_to_view_the_full_chain)
        )
        val tag = viewfinder.findViewWithTag<TextView>("fx")
        tag.setText("  LIVE / " + frame.nodes.size + "/" + frame.ids().size + "  ")
    }

    fun showChain() {
        if (recording) {
            Toast.makeText(this, R.string.fault_edit_after_recording, Toast.LENGTH_SHORT).show()
            return
        }
        EffectDialog(this, false).show()
    }

    fun showParameters() {
        if (rawOriginal() || effectState.mask == 0) {
            showChain()
            return
        }
        if (recording) {
            Toast.makeText(this, R.string.fault_edit_after_recording, Toast.LENGTH_SHORT).show()
            return
        }
        EffectDialog(this, true).show()
    }

    fun effectAvailable(id: Int): Boolean {
        return !rawOriginal() && (settings.experimentalSignals || !Effects.physical(id)) &&
            Effects.available(
                id,
                videoMode,
                !videoMode && settings.photoFormat == 2,
            )
    }

    fun showEffect(id: Int) {
        if (recording) {
            Toast.makeText(this, R.string.fault_edit_after_recording, Toast.LENGTH_SHORT).show()
            return
        }
        val d = EffectDialog(this, true)
        d.focused = id
        d.tuning = true
        d.show()
    }

    fun showFormat(): Dialog? {
        if (recording || engine.photoBusy) return null
        val labels: Array<String> =
            if (videoMode)
                (if (cameraOptions != null && cameraOptions!!.rawVideoAvailable())
                    arrayOf<String>(
                        "MP4",
                        "RAW ZIP",
                    )
                else arrayOf<String>("MP4"))
            else
                (if (cameraOptions != null && !cameraOptions!!.raws.isEmpty())
                    arrayOf<String>(
                        "JPG",
                        "RAW",
                    )
                else arrayOf<String>("JPG"))
        return SignalSheet.anchoredPick(
            this,
            formatButton,
            getString(R.string.ui_save_format),
            labels,
            if (videoMode) (if (settings.rawVideo) 1 else 0)
            else (if (settings.photoFormat == 0) 0 else 1),
            IntConsumer@{ index: Int ->
                val next = CaptureSettings(settings)
                if (videoMode) next.rawVideo = index == 1
                else {
                    next.photoFormat = if (index == 0) 0 else 2
                    next.photoSize = "recommended"
                }
                applySettings(next)
            },
        )
    }

    fun randomChain() {
        if (rawOriginal()) return
        if (recording) {
            Toast.makeText(this, R.string.fault_edit_after_recording, Toast.LENGTH_SHORT).show()
            return
        }
        commitEffects(EffectRandomizer.chain(effectState, availableEffects(), SecureRandom()))
        selectedRoute.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun reseed() {
        if (rawOriginal()) return
        var p = effectState.parameters()
        val random = SecureRandom()
        for (id in effectState.ids()) if (id != Effects.COLOR_MAP)
            p = p.reseed(id, random.nextLong())
        commitEffects(effectState.edit(effectState.chained, effectState.mask, p))
        selectedRoute.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun renderCaptureMode() {
        renderAudio()
        val value = videoMode
        photoTab.setTextColor(if (!value) LIME else MUTED)
        videoTab.setTextColor(if (value) LIME else MUTED)
        photoTab.setBackground(bg(if (!value) PANEL else BG, 0))
        videoTab.setBackground(bg(if (value) PANEL else BG, 0))
        capture.setContentDescription(
            if (value) getString(R.string.ui_start_video_recording)
            else getString(R.string.ui_take_a_photo)
        )
        capture.invalidate()
    }

    fun setVideo(value: Boolean) {
        if (!ready || recording || engine.photoBusy || videoMode == value) return
        if (
            value &&
                cameraOptions != null &&
                cameraOptions!!.videos.isEmpty() &&
                !(settings.rawVideo && cameraOptions!!.rawVideoAvailable())
        ) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_video_is_unavailable_on_this_camera),
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        cancelEffectPreview()
        videoMode = value
        ready(false)
        engine.configure(settings, value, effectState)
        renderEffects()
        renderCaptureMode()
        savePrefs()
    }

    fun requestFaultConfig(value: FaultConfig) {
        if (
            value.enabled &&
                value.audio &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            pendingFaultConfig = value
            requestPermissions(arrayOf<String>(Manifest.permission.RECORD_AUDIO), 4)
            return
        }
        applyFaultConfig(value)
    }

    fun applyFaultConfig(value: FaultConfig) {
        faultConfig = value
        liveChainStatus.setVisibility(
            if (value.enabled && !rawOriginal()) View.VISIBLE else View.GONE
        )
        renderEffects()
        engine.applyFaultConfig(value)
        FaultPreferences.save(getSharedPreferences("signal", 0), value)
        faultSwitch.setChecked(value.enabled)
        faultSwitch.setTextColor(if (value.enabled) LIME else MUTED)
    }

    fun savePrefs() {
        val prefs = getSharedPreferences("signal", 0).edit().putBoolean("sound", sound)
        EffectStateStore.write(prefs, effectState)
        prefs.apply()
    }

    fun shoot() {
        if (liveEditor != null) {
            if (recording) liveEditor!!.dialog!!.dismiss()
            else {
                Toast.makeText(
                        this,
                        R.string.ui_apply_or_discard_your_edits_before_capturing,
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                return
            }
        }
        if (effectPreview != null) {
            if (recording) cancelEffectPreview()
            else {
                Toast.makeText(
                        this,
                        getString(R.string.ui_apply_or_discard_your_edits_before_capturing),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                return
            }
        }
        if (!ready) {
            if (
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissions(
                    arrayOf<String>(Manifest.permission.CAMERA),
                    1,
                )
            else
                Toast.makeText(
                        this,
                        getString(R.string.ui_preparing_the_camera),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            return
        }
        capture.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (videoMode) {
            if (
                !recording &&
                    sound &&
                    !settings.rawVideo &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf<String>(Manifest.permission.RECORD_AUDIO), 2)
                return
            }
            ready(false)
            engine.toggleVideo(sound && !settings.rawVideo)
        } else {
            ready(false)
            engine.photo(
                if (preview.surfaceTexture == null) 0 else preview.surfaceTexture!!.timestamp
            )
            overlay.flash = true
            overlay.invalidate()
            handler.postDelayed(
                Runnable@{
                    overlay.flash = false
                    overlay.invalidate()
                },
                90,
            )
        }
    }

    fun openGallery() {
        if (recording || engine.photoBusy || mediaPreview != null) return
        cancelEffectPreview()
        if (liveEditor != null) liveEditor!!.dialog!!.dismiss()
        if (liveChainDialog != null) liveChainDialog!!.dismiss()
        if (latest == null) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_open_your_captured_photos_and_videos_here),
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        try {
            if ("application/zip" == getContentResolver().getType(latest!!)) {
                openExternal(latest!!)
                return
            }
        } catch (ignored: Exception) {}
        MediaPreview(this, requireNotNull(latest), latestVideo).show()
    }

    fun openExternal(uri: Uri) {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, getContentResolver().getType(uri))
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_photos_are_in_pictures_videos_in_movies_and),
                    Toast.LENGTH_LONG,
                )
                .show()
        }
    }

    fun resumeCameraPreview() {
        if (!resumed || mediaPreview != null || tutorial != null) return
        handler.removeCallbacks(previewAcknowledgement)
        handler.post(previewAcknowledgement)
        if (
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                preview.isAvailable()
        )
            engine.attach(
                preview.surfaceTexture,
                preview.width,
                preview.height,
            )
    }

    override fun status(s: String) {
        if (!recording) status.setText(s)
        if (
            s.contains(getString(R.string.ui_failed)) ||
                s.contains(getString(R.string.ui_could_not)) ||
                s.contains(getString(R.string.ui_error))
        )
            Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    override fun ready(value: Boolean) {
        ready = value && resumed && mediaPreview == null && tutorial == null
        capture.setAlpha(if (ready) 1f else .4f)
    }

    override fun recording(value: Boolean) {
        recording = value
        capture.invalidate()
        capture.setContentDescription(
            if (value) getString(R.string.ui_stop_recording)
            else if (videoMode) getString(R.string.ui_start_video_recording)
            else getString(R.string.ui_take_a_photo)
        )
        flipButton.setEnabled(!value)
        flipButton.setAlpha(if (value) .3f else 1f)
        micButton.setAlpha(if (value) .3f else 1f)
        renderAudio()
        galleryButton.setEnabled(!value)
        if (value) {
            start = SystemClock.elapsedRealtime()
            status.setTextColor(RED)
            handler.post(timer)
        } else {
            handler.removeCallbacks(timer)
            status.setTextColor(LIME)
            status.setText("LIVE · " + engine.description())
        }
    }

    override fun saved(uri: Uri, video: Boolean) {
        val rawSequence = "application/zip" == getContentResolver().getType(uri)
        latest = uri
        latestVideo = video
        captureCount++
        getSharedPreferences("signal", 0)
            .edit()
            .putString("last", uri.toString())
            .putBoolean("lastVideo", video)
            .apply()
        Toast.makeText(
                this,
                if (rawSequence) getString(R.string.ui_raw_video_saved_to_download_5igna1)
                else if (video) getString(R.string.ui_video_saved_to_movies_5igna1)
                else getString(R.string.ui_photo_saved_to_pictures_5igna1),
                Toast.LENGTH_SHORT,
            )
            .show()
        galleryButton.load(uri, video)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        handler.removeCallbacks(previewAcknowledgement)
        handler.post(previewAcknowledgement)
        if (tutorialPage < 0 && tutorial == null) geo.start()
        galleryButton.load(latest, latestVideo)
        if (tutorialPage >= 0 && tutorial == null) showTutorial()
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            resumeCameraPreview()
        else if (tutorial == null)
            requestPermissions(
                arrayOf<String>(Manifest.permission.CAMERA),
                1,
            )
    }

    override fun onPause() {
        resumed = false
        if (mediaPreview != null) mediaPreview!!.dismiss()
        if (liveChainDialog != null) liveChainDialog!!.dismiss()
        if (liveEditor != null) liveEditor!!.dialog!!.dismiss()
        cancelEffectPreview()
        savePrefs()
        ready(false)
        resumed = false
        handler.removeCallbacks(previewAcknowledgement)
        geo.stop()
        engine.detach()
        handler.removeCallbacks(timer)
        super.onPause()
    }

    fun showTutorial() = showTutorial(null)

    fun showTutorial(host: android.app.Dialog?) {
        if (tutorial != null || recording || engine.photoBusy) return
        tutorial = TutorialDialog(this, max(0, tutorialPage), host)
        ready(false)
        handler.removeCallbacks(previewAcknowledgement)
        geo.stop()
        engine.detach()
        tutorial!!.show()
    }

    fun tutorialClosed() {
        tutorial = null
        tutorialPage = -1
        if (resumed) {
            geo.start()
            resumeCameraPreview()
        }
        getSharedPreferences("signal", 0).edit().putBoolean(TutorialDialog.SEEN, true).apply()
        if (
            resumed &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(
                arrayOf<String>(Manifest.permission.CAMERA),
                1,
            )
    }

    override fun onDestroy() {
        if (tutorial != null) tutorial!!.dispose()
        galleryButton.dispose()
        engine.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, p, results)
        if (code == 4 && pendingFaultConfig != null) {
            val requested = pendingFaultConfig
            pendingFaultConfig = null
            val allowed =
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            applyFaultConfig((if (allowed) requested else requested!!.audio(false))!!)
            if (!allowed)
                Toast.makeText(
                        this,
                        getString(R.string.ui_audio_reaction_is_off_live_fault_can_use),
                        Toast.LENGTH_LONG,
                    )
                    .show()
            return
        }
        if (code == 1) {
            if (tutorial != null) return
            if (
                checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    resumed &&
                    mediaPreview == null &&
                    preview.isAvailable()
            )
                engine.attach(
                    preview.surfaceTexture,
                    preview.width,
                    preview.height,
                )
            else {
                status.setText(getString(R.string.ui_tap_capture_to_allow_camera_access))
                SignalSheet.message(
                    this,
                    getString(R.string.ui_settings),
                    getString(R.string.ui_camera_access_is_required_to_capture_if_the),
                    R.string.ui_settings,
                    Runnable { openAppSettings() },
                )
            }
        }
        if (code == 3) {
            if (resumed && tutorial == null) geo.start()
            renderGeo()
            showLocation()
        }
        if (code == 2) {
            if (
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            )
                shoot()
            else
                Toast.makeText(
                        this,
                        getString(R.string.ui_turn_audio_off_to_record_a_silent_video),
                        Toast.LENGTH_LONG,
                    )
                    .show()
        }
    }

    override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) {
        if (
            resumed &&
                mediaPreview == null &&
                tutorial == null &&
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
            engine.attach(
                s,
                w,
                h,
            )
    }

    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {
        engine.resize(w, h)
    }

    override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean {
        engine.releaseSurface(s)
        return false
    }

    override fun onSurfaceTextureUpdated(s: SurfaceTexture) {
        engine.previewPresented(s.timestamp)
    }

    fun renderAudio() {
        if (micButton != null) {
            val silent = videoMode && settings.rawVideo
            val enabled = sound && !silent
            micButton.setImageResource(if (enabled) R.drawable.ic_mic else R.drawable.ic_mic_off)
            micButton.setColorFilter(if (enabled) LIME else MUTED)
            micButton.setSelected(enabled)
            micButton.setEnabled(!recording && !silent)
            val label =
                if (silent) getString(R.string.ui_raw_silent)
                else
                    getString(R.string.capture_record_audio) +
                        " · " +
                        getString(if (sound) R.string.ui_audio_on else R.string.ui_audio_off)
            micButton.setContentDescription(label)
            micButton.setTooltipText(label)
        }
    }

    fun renderTorch() {
        if (torchButton != null) {
            torchButton.setColorFilter(if (engine.torch) LIME else MUTED)
            torchButton.setSelected(engine.torch)
            val label = getString(if (engine.torch) R.string.ui_light_on else R.string.ui_light_off)
            torchButton.setContentDescription(label)
            torchButton.setTooltipText(label)
        }
    }

    fun renderGeo() {
        if (geoButton != null) {
            geoButton.setColorFilter(if (geo.enabled) LIME else MUTED)
            geoButton.setSelected(geo.enabled)
            val label = getString(R.string.ui_capture_location_settings) + " · " + geo.label()
            geoButton.setContentDescription(label)
            geoButton.setTooltipText(label)
        }
    }

    fun setLocationEnabled(value: Boolean) {
        settings.location = value
        settings.save(getSharedPreferences("signal", 0))
        geo.updateEnabled(value)
        engine.setLocationEnabled(value)
        renderGeo()
    }

    fun requestLocationAccess() {
        val prefs = getSharedPreferences("signal", 0)
        val asked = prefs.getBoolean("locationPermissionAsked", false)
        if (
            !geo.permitted() &&
                asked &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            SignalSheet.message(
                this,
                getString(R.string.ui_check_location_permission),
                getString(R.string.ui_in_app_info_permissions_location_allow_access_while),
                R.string.ui_app_settings,
                Runnable { openAppSettings() },
            )
            return
        }
        prefs.edit().putBoolean("locationPermissionAsked", true).apply()
        requestPermissions(
            arrayOf<String>(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            3,
        )
    }

    fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()),
            )
        )
    }

    fun showLocation() {
        if (recording) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_change_location_settings_after_recording_stops),
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        val body = LinearLayout(this)
        body.setOrientation(LinearLayout.VERTICAL)
        body.setPadding(dp(22f), dp(12f), dp(22f), 0)
        val details = text(geo.detail(), 13, WHITE)
        details.setLineSpacing(dp(4f).toFloat(), 1f)
        body.addView(details)
        val primary =
            if (!geo.enabled) getString(R.string.ui_enable_capture_location)
            else if (!geo.permitted()) getString(R.string.ui_allow_location)
            else if (!geo.servicesEnabled()) getString(R.string.ui_device_location_settings)
            else getString(R.string.ui_get_location_again)
        val enable = button(primary)
        val bp = LinearLayout.LayoutParams(-1, dp(48f))
        bp.topMargin = dp(16f)
        body.addView(enable, bp)
        val precise = button(getString(R.string.ui_allow_precise_location))
        if (geo.enabled && geo.permitted() && !geo.precise()) {
            val pp = LinearLayout.LayoutParams(-1, dp(48f))
            pp.topMargin = dp(8f)
            body.addView(precise, pp)
        }
        val permissions = button(getString(R.string.ui_app_permissions))
        val ap = LinearLayout.LayoutParams(-1, dp(44f))
        ap.topMargin = dp(8f)
        body.addView(permissions, ap)
        permissions.setOnClickListener(OnClickListener@{ v: View? -> openAppSettings() })
        val dialog =
            SignalSheet.content(
                this,
                getString(R.string.ui_capture_location_gps),
                body,
                if (geo.enabled) R.string.ui_do_not_save_location else 0,
                Runnable { setLocationEnabled(false) },
                .65f,
            )
        enable.setOnClickListener(
            OnClickListener@{ v: View? ->
                dialog.dismiss()
                setLocationEnabled(true)
                if (!geo.permitted()) requestLocationAccess()
                else if (!geo.servicesEnabled())
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                else {
                    geo.retry()
                    showLocation()
                }
            }
        )
        precise.setOnClickListener(
            OnClickListener@{ v: View? ->
                dialog.dismiss()
                requestLocationAccess()
            }
        )
        val update: Runnable =
            object : Runnable {
                override fun run() {
                    if (!dialog.isShowing()) return
                    details.setText(geo.detail())
                    handler.postDelayed(this, 1000)
                }
            }
        update.run()
        dialog.setOnDismissListener(
            OnDismissListener@{ d: DialogInterface? ->
                handler.removeCallbacks(update)
            }
        )
    }

    fun showSettings() {
        if (recording || engine.photoBusy) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_change_settings_after_capture_finishes),
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        QualityDialog(this).show()
    }

    fun applySettings(value: CaptureSettings) {
        if (recording || engine.photoBusy) {
            Toast.makeText(
                    this,
                    getString(R.string.ui_change_settings_after_capture_finishes),
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        cancelEffectPreview()
        settings = CaptureSettings(value)
        if (settings.photoFormat == 1) settings.photoFormat = 2
        settings.save(getSharedPreferences("signal", 0))
        geo.updateEnabled(settings.location)
        if (settings.location && !geo.permitted()) requestLocationAccess()
        ready(false)
        engine.configure(settings, videoMode, effectState)
        renderEffects()
        renderCaptureMode()
        savePrefs()
    }

    override fun configured(
        choices: CameraOptions,
        actual: CaptureSettings,
        actualVideo: Boolean,
        w: Int,
        h: Int,
        detail: String,
    ) {
        if (actualVideo != videoMode) return
        if (actual.photoFormat != settings.photoFormat) cancelEffectPreview()
        cameraOptions = choices
        settings = CaptureSettings(actual)
        settings.location = geo.enabled
        renderAudio()
        displayAspect = w / h.toFloat()
        val aw = previewArea.width
        val ah = previewArea.height
        measuredFps = 0f
        zoomButton.setEnabled(!rawOriginal() && (videoMode || settings.photoFormat == 0))
        zoomButton.setAlpha(
            if (!rawOriginal() && (videoMode || settings.photoFormat == 0)) 1f else .4f
        )
        if (rawOriginal() || (!videoMode && settings.photoFormat != 0)) {
            zoom = 1f
            zoomButton.setText("1×")
        }
        if (aw > 0 && ah > 0) {
            val vw = min(aw, (ah * displayAspect).toInt())
            val p = viewfinder.layoutParams as FrameLayout.LayoutParams
            p.width = vw
            p.height = (vw / displayAspect).toInt()
            p.gravity = Gravity.CENTER
            viewfinder.setLayoutParams(p)
        }
        count.setText(
            if (videoMode)
                (if (settings.rawVideo) "RAW · " else "") +
                    requireNotNull(engine.videoChoice).fps +
                    " fps / ⚙"
            else
                String.format(
                    Locale.US,
                    "%.1f MP / ⚙",
                    w * h.toDouble() / 1e6,
                )
        )
        renderEffects()
    }

    override fun fps(value: Float) {
        measuredFps = value
        if (ready && !recording && !engine.cooling)
            status.setText("LIVE · " + engine.description() + " · " + engine.loadSummary())
    }

    override fun onKeyDown(key: Int, event: KeyEvent): Boolean {
        if (key == KeyEvent.KEYCODE_VOLUME_DOWN || key == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.repeatCount == 0) shoot()
            return true
        }
        return super.onKeyDown(key, event)
    }

    override fun onBackPressed() {
        if (recording) engine.toggleVideo(sound && !settings.rawVideo) else super.onBackPressed()
    }

    internal inner class CaptureButton : View(this@MainActivity) {
        var p: Paint = Paint(3)

        init {
            setClickable(true)
        }

        override fun onDraw(c: Canvas) {
            val x = getWidth() / 2f
            val y = getHeight() / 2f
            val r = min(x, y) - dp(5f)
            p.setStyle(Paint.Style.STROKE)
            p.setStrokeWidth(dp(2f).toFloat())
            p.setColor(if (recording) RED else WHITE)
            c.drawCircle(x, y, r, p)
            p.setStyle(Paint.Style.FILL)
            p.setColor(if (videoMode) RED else LIME)
            if (recording)
                c.drawRoundRect(
                    x - dp(13f),
                    y - dp(13f),
                    x + dp(13f),
                    y + dp(13f),
                    dp(5f).toFloat(),
                    dp(5f).toFloat(),
                    p,
                )
            else c.drawCircle(x, y, r - dp(6f), p)
            if (!videoMode) {
                p.setColor(BG)
                c.drawRect(x - dp(8f), y - dp(2f), x + dp(8f), y + dp(2f), p)
                c.drawRect(x - dp(2f), y - dp(8f), x + dp(2f), y + dp(8f), p)
            }
        }
    }

    internal inner class Overlay : View(this@MainActivity) {
        var p: Paint = Paint(3)
        var flash: Boolean = false
        var focusX: Float = -1f
        var focusY: Float = 0f

        init {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
        }

        override fun onDraw(c: Canvas) {
            val w = getWidth().toFloat()
            val h = getHeight().toFloat()
            p.setColor(0x228C9B83)
            p.setStrokeWidth(1f)
            for (i in 1..2) {
                c.drawLine(w * i / 3, 0f, w * i / 3, h, p)
                c.drawLine(0f, h * i / 3, w, h * i / 3, p)
            }
            p.setColor(-0x668000c0)
            p.setStrokeWidth(dp(1f).toFloat())
            val cx = w / 2
            val cy = h / 2
            c.drawLine(cx - dp(6f), cy, cx + dp(6f), cy, p)
            c.drawLine(cx, cy - dp(6f), cx, cy + dp(6f), p)
            if (focusX >= 0) {
                p.setStyle(Paint.Style.STROKE)
                p.setColor(LIME)
                c.drawRoundRect(
                    focusX - dp(25f),
                    focusY - dp(25f),
                    focusX + dp(25f),
                    focusY + dp(25f),
                    dp(6f).toFloat(),
                    dp(6f).toFloat(),
                    p,
                )
                p.setStyle(Paint.Style.FILL)
            }
            if (flash) c.drawColor(-0x55000001)
        }
    }

    companion object {
        val BG: Int = Color.rgb(10, 12, 13)
        val PANEL: Int = Color.rgb(24, 27, 28)
        val LIME: Int = Color.rgb(208, 242, 139)
        val WHITE: Int = Color.rgb(239, 242, 231)
        val MUTED: Int = Color.rgb(142, 151, 140)
        val RED: Int = Color.rgb(255, 74, 107)
        val MODES: Array<String> = Effects.NAMES

        // One radius for panels, fields and controls; small labels use a scaled detail radius.
        const val UI_RADIUS: Int = 12
        const val DETAIL_RADIUS: Int = 4
        const val TEXT_TITLE: Int = 18
        const val TEXT_LABEL: Int = 13
        const val TEXT_BODY: Int = 12
    }
}
