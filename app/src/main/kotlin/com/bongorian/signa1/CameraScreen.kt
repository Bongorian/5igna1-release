package com.bongorian.signa1

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowInsets
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Space
import android.widget.TextView
import android.widget.ToggleButton
import com.bongorian.signa1.MainActivity.Companion.BG
import com.bongorian.signa1.MainActivity.Companion.LIME
import com.bongorian.signa1.MainActivity.Companion.MODES
import com.bongorian.signa1.MainActivity.Companion.MUTED
import com.bongorian.signa1.MainActivity.Companion.PANEL
import kotlin.math.abs
import kotlin.math.min

/** Constructs the camera screen and binds its user actions. */
internal fun MainActivity.buildUi() {
    val root = CameraWorkspace(this)
    cameraRoot = root
    root.setOrientation(LinearLayout.VERTICAL)
    root.setBackgroundColor(BG)
    root.setPadding(dp(18f), dp(8f), dp(18f), dp(4f))
    setContentView(root)
    root.setOnApplyWindowInsetsListener(
        OnApplyWindowInsetsListener@{ view: View?, insets: WindowInsets? ->
            val safe =
                insets!!.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
            root.applySafeInsets(safe)
            insets
        }
    )
    root.requestApplyInsets()
    val previewColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val controlsColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val controlBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val controlScroll = android.widget.ScrollView(this).apply {
        isFillViewport = false
        isVerticalScrollBarEnabled = true
        addView(controlBody)
    }
    root.previewColumn = previewColumn
    root.controlsColumn = controlsColumn
    root.controlsScroll = controlScroll
    root.addView(previewColumn)
    root.addView(controlsColumn)
    controlsColumn.addView(controlScroll, LinearLayout.LayoutParams(-1, 0, 1f))
    val utility = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    root.utilityBlock = utility
    previewColumn.addView(utility, LinearLayout.LayoutParams(-1, -2))
    val header = row()
    utility.addView(header, LinearLayout.LayoutParams(-1, dp(48f)))
    formatButton = button("")
    formatButton.background = android.graphics.drawable.InsetDrawable(bg(PANEL, 0), 0, dp(2f), 0, dp(2f))
    formatButton.setTextSize(11f)
    formatButton.setPadding(dp(2f), 0, dp(2f), 0)
    formatButton.setOnClickListener(OnClickListener@{ v: View? -> cycleFormat() })
    torchButton = iconButton(R.drawable.ic_flash, getString(R.string.ui_light_off))
    header.addView(torchButton, LinearLayout.LayoutParams(dp(44f), dp(48f)))
    header.addView(formatButton, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    count = text("—", 12, MainActivity.WHITE).apply {
        gravity = Gravity.CENTER
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        contentDescription = getString(R.string.pro_output_size)
        setOnClickListener { ResolutionPicker.show(this@buildUi, videoMode) }
    }
    header.addView(count, LinearLayout.LayoutParams(0, dp(48f), 1f))
    proModeButton = button("AUTO").apply {
        textSize = 11f
        contentDescription = getString(R.string.pro_mode)
        setOnClickListener {
            if (!recording && !engine.photoBusy && !tapMode && externalCapture == null) {
                engine.setProMode(!engine.proMode)
                handler.postDelayed({ renderProCamera() }, 100)
            }
        }
    }
    header.addView(proModeButton, LinearLayout.LayoutParams(dp(64f), dp(48f)))
    renderTorch()
    torchButton.setOnClickListener(
        OnClickListener@{ v: View? ->
            engine.torch()
            handler.postDelayed(Runnable@{ this.renderTorch() }, 200)
        }
    )
    geoButton = iconButton(R.drawable.ic_location, getString(R.string.ui_capture_location_settings))
    geoButton.setOnClickListener(OnClickListener@{ v: View? -> showLocation() })
    renderGeo()
    micButton = iconButton(R.drawable.ic_mic, getString(R.string.ui_audio_on))
    renderAudio()
    micButton.setOnClickListener(
        OnClickListener@{ v: View? ->
            if (!recording) {
                sound = !sound
                renderAudio()
                savePrefs()
            }
        }
    )
    val settingsButton =
        iconButton(R.drawable.ic_settings, getString(R.string.ui_capture_and_language_settings))
    settingsButton.tag = "guide-settings"
    header.addView(settingsButton, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    settingsButton.setOnClickListener(OnClickListener@{ v: View? -> showSettings() })
    val info = row()
    utility.addView(info, LinearLayout.LayoutParams(-1, dp(27f)))
    status = text(getString(R.string.ui_preparing_the_camera), 11, MUTED)
    status.setTypeface(Typeface.MONOSPACE)
    status.setSingleLine(true)
    status.setEllipsize(TextUtils.TruncateAt.END)
    info.addView(status, LinearLayout.LayoutParams(0, -1, 1f))
    // Fit the actual signal aspect without cropping or stretching.
    previewArea = FrameLayout(this)
    previewColumn.addView(previewArea, LinearLayout.LayoutParams(-1, 0, 1f))
    viewfinder = FrameLayout(this)
    viewfinder.setBackground(bg(Color.BLACK, 0))
    viewfinder.setClipToOutline(true)
    previewArea.addView(viewfinder, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
    previewArea.addOnLayoutChangeListener(
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
            val availableW = r - l
            val availableH = b - t
            if (availableW <= 0 || availableH <= 0) return@OnLayoutChangeListener
            val w = min(availableW, (availableH * displayAspect).toInt())
            val h = (w / displayAspect).toInt()
            val p = viewfinder.layoutParams as FrameLayout.LayoutParams
            if (p.width != w || p.height != h) {
                p.width = w
                p.height = h
                p.gravity = Gravity.CENTER
                viewfinder.setLayoutParams(p)
            }
        }
    )
    preview = TextureView(this)
    preview.setContentDescription(getString(R.string.ui_camera_preview_tap_to_focus))
    preview.setOnClickListener(
        OnClickListener@{ v: View? -> if (ready) engine.focus() else engine.retryPreview() }
    )
    preview.setSurfaceTextureListener(this)
    viewfinder.addView(preview, FrameLayout.LayoutParams(-1, -1))
    overlay = Overlay()
    viewfinder.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    micButton.background = bg(0xAA101410.toInt(), 0)
    geoButton.background = bg(0xAA101410.toInt(), 0)
    viewfinder.addView(micButton, FrameLayout.LayoutParams(dp(44f), dp(44f), Gravity.TOP or Gravity.END).apply {
        topMargin = dp(8f); rightMargin = dp(8f)
    })
    viewfinder.addView(geoButton, FrameLayout.LayoutParams(dp(44f), dp(44f), Gravity.TOP or Gravity.END).apply {
        topMargin = dp(8f); rightMargin = dp(58f)
    })
    tapControls = row()
    tapControls.visibility = View.GONE
    tapControls.setPadding(dp(8f), dp(4f), dp(8f), dp(8f))
    tapPlay = button(getString(R.string.tap_play))
    tapPlay.tag = "tap-play"
    tapPlay.setOnClickListener { engine.toggleTapPlayback() }
    tapControls.addView(tapPlay, LinearLayout.LayoutParams(0, dp(44f), 1f))
    tapChoose = button(getString(R.string.tap_choose))
    tapChoose.tag = "tap-choose"
    tapChoose.setOnClickListener { chooseTap() }
    tapControls.addView(tapChoose, LinearLayout.LayoutParams(0, dp(44f), 1f).apply { leftMargin = dp(6f) })
    viewfinder.addView(tapControls, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

    preview.setOnTouchListener(
        object : OnTouchListener {
            var downX: Float = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    downX = e.getX()
                    return true
                }
                if (e.action == MotionEvent.ACTION_UP) {
                    val diff = e.getX() - downX
                    if (abs(diff) > dp(60f)) {
                        setVideo(diff < 0)
                    } else {
                        v.performClick()
                        overlay.focusX = if (tapMode || engine.proEffective.manualFocus) -1f else preview.width / 2f
                        overlay.focusY = preview.height / 2f
                        overlay.invalidate()
                        handler.postDelayed(
                            Runnable@{
                                overlay.focusX = -1f
                                overlay.invalidate()
                            },
                            900,
                        )
                    }
                    return true
                }
                return true
            }
        }
    )
    val liveTag = text("  FX / " + MODES[effectState.selected()] + "  ", 10, LIME)
    liveTag.setTypeface(Typeface.MONOSPACE)
    liveTag.setBackground(detailBg(-0x66efebf0, 0))
    val tagP = FrameLayout.LayoutParams(-2, dp(26f), Gravity.TOP or Gravity.START)
    tagP.setMargins(dp(12f), dp(12f), 0, 0)
    viewfinder.addView(liveTag, tagP)
    liveTag.setTag("fx")
    lensButton = button(getString(R.string.lens_select))
    lensButton.setTextSize(11f)
    lensButton.setPadding(dp(12f),0,dp(12f),0)
    lensButton.setBackground(bg(-0x33ede8ee,0))
    lensButton.setOnClickListener { showCameras() }
    viewfinder.addView(lensButton,FrameLayout.LayoutParams(-2,dp(44f),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
        bottomMargin=dp(12f)
    })
    proStrip = buildProStrip()
    controlBody.addView(proStrip, LinearLayout.LayoutParams(-1, -2))
    val faultBar = row()
    controlBody.addView(faultBar, LinearLayout.LayoutParams(-1, dp(48f)).apply { topMargin = dp(6f) })
    faultDeckButton = button("").apply {
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        textSize = 12f
        setOnClickListener { faultDeckExpanded = !faultDeckExpanded; renderFaultDeck() }
    }
    faultBar.addView(faultDeckButton, LinearLayout.LayoutParams(0, -1, 1f))
    faultDeck = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
    controlBody.addView(faultDeck, LinearLayout.LayoutParams(-1, -2))
    val effects = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2f), dp(10f), dp(2f), dp(2f))
        setBackgroundColor(BG)
    }
    faultDeck.addView(effects, LinearLayout.LayoutParams(-1, -2))
    val chainRow = row()
    effects.addView(chainRow, LinearLayout.LayoutParams(-1, dp(48f)))
    val scroll = HorizontalScrollView(this)
    scroll.setHorizontalScrollBarEnabled(false)
    chainRow.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
    selectedRoute = row()
    scroll.addView(selectedRoute)
    val add = button("＋")
    add.tag = "guide-add"
    add.setTextSize(20f)
    add.background = android.graphics.drawable.InsetDrawable(bg(PANEL, 0), 0, dp(2f), 0, dp(2f))
    add.setContentDescription(getString(R.string.fault_add_remove))
    chainRow.addView(add, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    add.setOnClickListener(OnClickListener@{ v: View? -> showChain() })
    val random =
        iconButton(
            R.drawable.ic_shuffle,
            getString(R.string.ui_tap_to_randomize_an_effect_hold_to_randomize),
        )
    chainRow.addView(random, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    random.setOnClickListener(OnClickListener@{ v: View? -> randomChain() })
    random.setOnLongClickListener(
        OnLongClickListener@{ v: View? ->
            reseed()
            true
        }
    )
    liveChainStatus = text("", 10, LIME)
    liveChainStatus.setSingleLine(true)
    liveChainStatus.setEllipsize(TextUtils.TruncateAt.END)
    liveChainStatus.setVisibility(View.GONE)
    effects.addView(liveChainStatus, LinearLayout.LayoutParams(-1, dp(25f)))
    liveChainStatus.setOnClickListener(
        OnClickListener@{ v: View? ->
            if (shownLiveFrame != null) {
                faultStatePanel = FaultStateDialog(this)
                liveChainDialog = faultStatePanel!!.show(shownLiveFrame!!)
            }
        }
    )
    val power = row()
    effects.addView(power, LinearLayout.LayoutParams(-1, dp(39f)))
    val strengthLabel = text(getString(R.string.ui_strength), 11, MUTED)
    power.addView(strengthLabel, LinearLayout.LayoutParams(dp(48f), -1))
    strength = SeekBar(this)
    strength.setContentDescription(getString(R.string.ui_strength))
    strength.setMax(100)
    strength.setProgress(Math.round(effectState.amount * 100))
    strength.setProgressTintList(ColorStateList.valueOf(LIME))
    strength.setThumbTintList(ColorStateList.valueOf(LIME))
    power.addView(strength, LinearLayout.LayoutParams(0, dp(36f), 1f))
    strengthValue = text(strength.progress.toString() + "%", 11, LIME)
    strengthValue.setTypeface(Typeface.MONOSPACE)
    strengthValue.setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
    power.addView(strengthValue, LinearLayout.LayoutParams(dp(43f), -1))
    strength.setOnSeekBarChangeListener(
        object : OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, from: Boolean) {
                strengthValue.setText(p.toString() + "%")
                if (from) commitEffects(effectState.amount(p / 100f))
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}

            override fun onStopTrackingTouch(s: SeekBar?) {
                savePrefs()
            }
        }
    )
    val modes = CaptureToolbar(this)
    controlsColumn.addView(modes, LinearLayout.LayoutParams(-1, dp(48f)))
    photoTab = CaptureModeButton(this, R.drawable.ic_mode_photo, getString(R.string.ui_photo))
    videoTab = CaptureModeButton(this, R.drawable.ic_mode_video, getString(R.string.ui_video))
    tapTab = CaptureModeButton(this, R.drawable.ic_mode_tap, getString(R.string.tap_mode))
    tapTab.visibility = if (externalCapture == null && settings.experimentalSignals) View.VISIBLE else View.GONE
    for (mode in listOf(photoTab, videoTab, tapTab)) modes.addView(mode, LinearLayout.LayoutParams(dp(96f), dp(48f)))
    tapTab.setOnClickListener { if (!tapMode) { if (tapInput != null) enterTap(tapInput!!) else chooseTap() } }
    photoTab.setOnClickListener { setVideo(false) }
    videoTab.setOnClickListener { setVideo(true) }
    photoTab.setOnLongClickListener { ResolutionPicker.show(this, false); true }
    videoTab.setOnLongClickListener { ResolutionPicker.show(this, true); true }
    photoTab.tooltipText = getString(R.string.ui_photo) + " · " + getString(R.string.resolution_hold)
    videoTab.tooltipText = getString(R.string.ui_video) + " · " + getString(R.string.resolution_hold)
    val live = row()
    live.background = bg(PANEL, 0)
    faultBar.addView(live, LinearLayout.LayoutParams(-2, -1).apply { leftMargin = dp(8f) })
    faultSwitch = ToggleButton(this)
    faultSwitch.setTextOn("LIVE ON")
    faultSwitch.setTextOff("LIVE OFF")
    typography(faultSwitch, 11, true)
    faultSwitch.setAllCaps(false)
    faultSwitch.setBackgroundColor(Color.TRANSPARENT)
    faultSwitch.setPadding(0, 0, 0, 0)
    faultSwitch.setChecked(faultConfig.enabled)
    faultSwitch.setTextColor(if (faultConfig.enabled) LIME else MUTED)
    faultSwitch.setContentDescription(getString(R.string.ui_toggle_live_fault))
    live.addView(faultSwitch, LinearLayout.LayoutParams(dp(84f), -1))
    val reactions = iconButton(R.drawable.ic_tune, getString(R.string.ui_live_fault_settings))
    reactions.setPadding(dp(11f), dp(11f), dp(11f), dp(11f))
    live.addView(reactions, LinearLayout.LayoutParams(dp(42f), -1))
    reactions.setOnClickListener(OnClickListener@{ v: View? -> FaultDialog.show(this) })
    faultSwitch.setOnCheckedChangeListener(
        OnCheckedChangeListener@{ view: CompoundButton?, checked: Boolean ->
            if (checked != faultConfig.enabled) requestFaultConfig(faultConfig.enabled(checked))
        }
    )
    liveTransport = row()
    val timeRow = LinearLayout.LayoutParams(-1, dp(44f))
    timeRow.topMargin = dp(8f)
    timeRow.bottomMargin = dp(4f)
    faultDeck.addView(liveTransport, timeRow)
    liveHold = button(getString(R.string.live_pause))
    val hit = button(getString(R.string.live_trigger))
    val rewind = button(getString(R.string.live_reset))
    liveHold.setContentDescription(getString(R.string.live_hold_hint))
    hit.setContentDescription(getString(R.string.live_hit_hint))
    rewind.setContentDescription(getString(R.string.live_reset_hint))
    FaultDialog.timeButtons(this, liveTransport, liveHold, hit, rewind)
    liveHold.setOnClickListener(
        OnClickListener@{ v: View? ->
            applyFaultConfig(
                faultConfig.performance(faultConfig.performance.held(!faultConfig.performance.hold))
            )
        }
    )
    hit.setOnClickListener(
        OnClickListener@{ v: View? ->
            engine.hitFaults()
            confirmHaptic(v!!, HapticFeedbackConstants.CONFIRM)
        }
    )
    rewind.setOnClickListener(OnClickListener@{ v: View? -> engine.rewindFaults() })
    echoButton = button(getString(R.string.echo_trigger))
    echoButton.contentDescription = getString(R.string.echo_hint)
    echoButton.setOnClickListener { engine.triggerEcho() }
    faultDeck.addView(echoButton, LinearLayout.LayoutParams(-1, dp(44f)))
    val controls = row()
    controls.setGravity(Gravity.CENTER)
    controlsColumn.addView(controls, LinearLayout.LayoutParams(-1, dp(88f)))
    galleryButton = MediaThumbnail(this)
    val gallerySlot = FrameLayout(this)
    gallerySlot.addView(galleryButton, FrameLayout.LayoutParams(dp(44f), dp(44f), Gravity.CENTER))
    controls.addView(gallerySlot, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    galleryButton.setOnClickListener(OnClickListener@{ v: View? -> openGallery() })
    galleryButton.load(latest, latestVideo)
    val spacer = Space(this)
    controls.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
    capture = CaptureButton()
    controls.addView(capture, LinearLayout.LayoutParams(dp(80f), dp(80f)))
    capture.setContentDescription(getString(R.string.ui_take_a_photo))
    capture.setOnClickListener(OnClickListener@{ v: View? -> shoot() })
    controls.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
    flipButton =
        iconButton(
            R.drawable.ic_camera_flip,
            getString(R.string.camera_flip_facing),
        )
    flipButton.background = android.graphics.drawable.InsetDrawable(bg(PANEL, 0), dp(2f))
    controls.addView(flipButton, LinearLayout.LayoutParams(dp(48f), dp(48f)))
    flipButton.setOnClickListener {
        if (!recording && !engine.photoBusy && !tapMode) {
            cancelEffectPreview()
            engine.switchCamera()
        }
    }
    renderEffects()
    renderCaptureMode()
    renderFaultDeck()
    savePrefs()
}
