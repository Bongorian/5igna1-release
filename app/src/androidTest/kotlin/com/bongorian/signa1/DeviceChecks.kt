package com.bongorian.signa1

import android.app.*
import android.content.*
import android.graphics.*
import android.location.Location
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Range
import android.view.View
import android.widget.SeekBar
import java.io.*
import java.util.*
import java.util.function.BooleanSupplier

class DeviceChecks : Instrumentation() {
    internal var args: Bundle? = null
    internal var activity: MainActivity? = null

    public override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        args = if (arguments == null) Bundle() else arguments
        start()
    }

    internal fun await(what: String, condition: BooleanSupplier, millis: Long) {
        val until = SystemClock.elapsedRealtime() + millis
        while (SystemClock.elapsedRealtime() < until) {
            if (condition.getAsBoolean()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timeout: " + what)
    }

    public override fun onStart() {
        val result = Bundle()
        var original: CaptureSettings? = null
        var effectsBefore: EffectState? = null
        var faultsBefore: FaultConfig? = null
        var video = false
        var launchMonitor: ActivityMonitor? = null
        try {
            if (args!!.getString("action", "") == "pro-camera") {
                result.putString("result", ProCameraDeviceChecks.run(targetContext))
                return
            }
            if (args!!.getString("action", "") == "network-render") {
                // Offscreen synthetic GL fixtures only: no activity, screen or accessibility APIs.
                result.putString("result", FaultRenderChecks.run(targetContext, false, true))
                return
            }
            if (args!!.getString("action", "") == "camera-intents") {
                // This path uses instrumentation callbacks only: no screen capture, UI automation,
                // coordinate input or accessibility-tree inspection.
                activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                runOnMainSync { activity!!.tutorial?.dialog?.dismiss() }
                await("camera ready", { activity!!.ready && activity!!.cameraOptions != null }, 25000)
                result.putString("result", CameraIntentChecks.run(this))
                return
            }
            Thread(
                    {
                        SystemClock.sleep(12000)
                        if (activity == null) {
                            for (entry in Thread.getAllStackTraces().entries) if (
                                entry.key.getName() == "main" ||
                                    entry.key.getName().contains("Signal") ||
                                    entry.key.getName().contains("Instr")
                            )
                                android.util.Log.i(
                                    "SignalCheck",
                                    entry.key.getName() +
                                        " " +
                                        java.util.Arrays.toString(entry.value),
                                )
                        }
                    },
                    "CheckWatch",
                )
                .start()
            if (args!!.getString("action", "").startsWith("tutorial"))
                getTargetContext()
                    .getSharedPreferences("signal", 0)
                    .edit()
                    .remove(TutorialDialog.SEEN)
                    .commit()
            if (args!!.getString("action", "") == "release-defaults") {
                val prefs = getTargetContext().getSharedPreferences("signal", 0)
                check(!prefs.contains("sound") && !prefs.contains("advancedMode") && !prefs.contains("experimentalSignals")) { "Requires fresh test installation" }
            }
            val monitor = addMonitor(MainActivity::class.java!!.getName(), null, false)
            launchMonitor = monitor
            getUiAutomation()
                .executeShellCommand(
                    "am start -f 0x10008000 -n " +
                        getTargetContext().getPackageName() +
                        "/" +
                        MainActivity::class.java!!.getName()
                )
                .use({ launched -> })
            activity = monitor.waitForActivityWithTimeout(20000) as MainActivity
            if (activity == null) throw AssertionError("Activity start timeout")
            if (args!!.getString("action", "") == "release-defaults") {
                val prefs = getTargetContext().getSharedPreferences("signal", 0)
                check(!activity!!.sound && !activity!!.advancedMode && !activity!!.settings.experimentalSignals && !activity!!.faultConfig.audio) { "Release defaults must be OFF" }
                check(!CaptureSettings.load(prefs).experimentalSignals && !FaultPreferences.load(prefs).audio)
                result.putString("result", "PASS fresh install: experimental, ADVANCED, video audio and LIVE audio all OFF")
                return
            }
            if (args!!.getString("action", "") == "tutorial-permission") {
                await(
                    "guide before permission",
                    { activity!!.tutorial != null && activity!!.tutorial!!.dialog.isShowing() },
                    5000,
                )
                if (
                    activity!!.checkSelfPermission(android.Manifest.permission.CAMERA) ===
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                )
                    throw AssertionError("Test requires denied camera permission")
                SystemClock.sleep(1200)
                languageScreenshot("tutorial-before-permission")
                if (
                    !getTargetContext()
                        .getPackageName()
                        .contentEquals(getUiAutomation().getRootInActiveWindow().getPackageName())
                )
                    throw AssertionError("Permission covered tutorial")
                tutorialClick("tutorial-skip")
                await(
                    "camera permission after guide",
                    {
                        val root = getUiAutomation().getRootInActiveWindow()
                        root != null &&
                            root!!.getPackageName().toString().contains("permissioncontroller")
                    },
                    5000,
                )
                result.putString(
                    "result",
                    "PASS tutorial readable before camera permission; permission requested only after dismissal",
                )
                return
            }
            if (args!!.getString("action", "") == "tutorial") {
                checkTutorial()
                result.putString(
                    "result",
                    "PASS first launch, page restoration, skip/back/completion, immediate settings persistence, all locales and camera recovery",
                )
                return
            }
            runOnMainSync({
                if (activity!!.tutorial != null) activity!!.tutorial!!.dialog.dismiss()
            })
            runOnMainSync({
                val cover = android.widget.TextView(activity)
                cover.setText("撮影テスト中…")
                cover.setTextColor(-0x3800b6)
                cover.setTextSize(16f)
                cover.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL)
                cover.setPadding(0, activity!!.dp(35f), 0, 0)
                cover.setClickable(true)
                cover.keepScreenOn = true
                cover.setTag("deviceCheckOverlay")
                (activity!!.getWindow().getDecorView() as android.widget.FrameLayout).addView(
                    cover,
                    android.widget.FrameLayout.LayoutParams(-1, -1),
                )
            })
            await(
                "camera ready",
                {
                    // A launch into another orientation may create a replacement activity.
                    val latest = monitor.lastActivity as? MainActivity
                    if (latest != null && latest !== activity) {
                        activity = latest
                        runOnMainSync { latest.tutorial?.dialog?.dismiss() }
                    }
                    activity!!.resumed && !activity!!.isDestroyed && activity!!.engine.frameSeen && activity!!.cameraOptions != null
                },
                20000,
            )
            removeMonitor(monitor)
            launchMonitor = null
            original = CaptureSettings(activity!!.settings)
            effectsBefore = activity!!.effectState
            faultsBefore = activity!!.faultConfig
            video = activity!!.videoMode
            if (args!!.getString("action", "") == "light-mode") {
                result.putString("report", LightModeChecks.run(this).toString())
                result.putString("result", "PASS LIGHT mode and LED copy optimization")
                return
            }
            if (args!!.getString("action", "") == "gpu-detail") {
                val dimensions = org.json.JSONObject()
                runOnMainSync {
                    activity!!.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    activity!!.engine.gl.post {
                        val e = activity!!.engine
                        dimensions.put("viewWidth", e.width).put("viewHeight", e.height)
                            .put("signalWidth", e.signalW).put("signalHeight", e.signalH)
                            .put("outputWidth", e.outW).put("outputHeight", e.outH)
                            .put("advancedMode", e.settings.advancedMode)
                    }
                    activity!!.engine.detach()
                }
                val closed = java.util.concurrent.CountDownLatch(1)
                activity!!.engine.gl.post { closed.countDown() }
                check(closed.await(10, java.util.concurrent.TimeUnit.SECONDS))
                result.putString("report", FaultRenderChecks.run(targetContext, false, gpuDetail = dimensions))
                result.putString("result", "PASS GPU detail investigation")
                return
            }
            if (args!!.getString("action", "") == "load-investigation") {
                runOnMainSync { activity!!.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                if (args!!.getString("part", "gpu") == "gpu") {
                    runOnMainSync { activity!!.engine.detach() }
                    val closed = java.util.concurrent.CountDownLatch(1)
                    activity!!.engine.gl.post { closed.countDown() }
                    check(closed.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    result.putString("report", FaultRenderChecks.run(targetContext, false, investigation = true))
                } else result.putString("report", LoadInvestigation.raw(this))
                result.putString("result", "PASS load investigation")
                return
            }
            if (args!!.getString("action", "") == "gpu-optimization") {
                runOnMainSync {
                    activity!!.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                result.putString("dngBuffers", DngBufferChecks.run(activity!!.engine))
                runOnMainSync { activity!!.engine.detach() }
                val closed = java.util.concurrent.CountDownLatch(1)
                activity!!.engine.gl.post { closed.countDown() }
                check(closed.await(10, java.util.concurrent.TimeUnit.SECONDS)) { "Camera detach" }
                result.putString("report", FaultRenderChecks.run(getTargetContext(),
                    args!!.getString("benchmark", "false") == "true"))
                result.putString("result", "PASS byte-exact GPU renderer comparison")
                return
            }
            if (args!!.getString("liveFault", "false") == "true")
                runOnMainSync({
                    activity!!.applyFaultConfig(
                        FaultConfig(true, true, true, true, true, true, .5f, 50)
                    )
                })
            val action = args!!.getString("action", "photo")
            if (action == "camera-lenses") {
                result.putString("result", CameraLensChecks.run(this))
            } else if (action == "video-signal") {
                result.putString("result", VideoSignalChecks.run(this))
            } else if (action == "tap-audio") {
                result.putString("result", TapAudioChecks.run(this))
            } else if (action == "saved-signal") {
                result.putString("result", SavedSignalChecks.run(this))
            } else if (action == "gpu-defaults") {
                result.putString("result", GpuDefaultsChecks.run(this))
            } else if (action == "responsive") {
                result.putString("result", ResponsiveUiChecks.run(this))
            } else if (action == "adaptive-rotation") {
                result.putString("result", AdaptiveRotationChecks.run(this))
            } else if (action == "adaptive") {
                result.putString("result", AdaptiveUiChecks.run(this))
            } else if (action == "seed") {
                result.putString("result", SeedUiChecks.run(this))
            } else if (action == "transport") {
                TransportUiChecks.run(this)
                result.putString("result", FaultRenderChecks.run(activity!!, false, true))
            } else if (action == "tap") {
                result.putString("result", TapChecks.run(this))
            } else if (action == "raw-echo") {
                checkRawEcho()
                result.putString("result", "PASS full-chain JPEG/RAW switching, resolution long-press, TIME ECHO current FAULT / past camera timestamp, automatic and manual bursts, RAW bypass, JPEG/DNG/MP4 saving and pixel-exact pinned echo JPEG")
            } else if (action == "pixel-preview") {
                checkPixelPreview()
                result.putString(
                    "result",
                    "PASS sparse pixel damage at preview and photo resolutions",
                )
            } else if (action == "screenshots") {
                checkStoreScreenshots()
                result.putString(
                    "result",
                    "PASS captured current Japanese and English UI screenshots",
                )
            } else if (action == "product-ui") {
                checkProductUi()
                result.putString(
                    "result",
                    "PASS anchored format menu, top utilities, live fault meters, camera interruption and stalled-preview recovery",
                )
            } else if (action == "experimental") {
                result.putString("gpu", ExperimentalSignalChecks.run(getTargetContext()))
                checkExperimentalControls()
                checkCaptureContract(true)
                result.putString("result", "PASS experimental gate, per-stage controls/apply/cancel/persistence, GPU artifacts, displayed JPEG and metadata")
            } else if (action == "settings-auto") {
                checkImmediateSettings()
                result.putString("result", "PASS immediate selection, disk persistence, rapid changes, no Apply, close/recreate/reopen")
            } else if (action == "expert") {
                checkExpert(result)
            } else if (action == "load-record") {
                checkLoadRecording(result)
            } else if (action == "load") {
                checkAdaptiveLoad(result)
            } else if (action == "advanced") {
                checkAdvanced()
                result.putString(
                    "result",
                    "PASS advanced UI, exact values, cancel/apply, saved overrides, full-range GPU and RAW contracts",
                )
            } else if (action == "performance") {
                checkPerformance()
                result.putString(
                    "result",
                    "PASS LIVE preview isolation, style/time controls, apply/cancel, HOLD and HIT",
                )
            } else if (action == "format-ui") {
                checkFormatUi()
                result.putString(
                    "result",
                    "PASS direct JPG/RAW and MP4/RAW ZIP cycling, RAW video opt-in persistence and opt-out, recording guard, TAP fixed exports and legacy migration",
                )
            } else if (action == "chain-format") {
                checkChainFormat()
                result.putString(
                    "result",
                    "PASS 13-fault RAW catalog, explicit JPEG switch with draft retention, cancel and format round-trip",
                )
            } else if (action == "editor") {
                checkEditor()
                result.putString(
                    "result",
                    "PASS unobscured preview, draft controls, multi-selection, random chain, cancel and apply",
                )
            } else if (action == "normal-capture") {
                checkNormalCapture()
                result.putString("result", "PASS normal capture, immutable state, released history and mode persistence")
            } else if (action == "capture-contract") {
                checkCaptureContract()
                result.putString(
                    "result",
                    "PASS displayed timestamp pin, later camera frames, JPEG pixel equality and snapshot metadata",
                )
            } else if (action == "language") {
                checkLanguage()
                result.putString(
                    "result",
                    "PASS Japanese, English, Chinese, system default, locale recreation, camera recovery and retained settings/effects/LIVE/count",
                )
            } else if (action == "raw-video-caps") {
                checkRawCaps(result)
            } else if (action == "raw-video") {
                checkRawVideo(result)
            } else if (action == "live-selection") {
                checkLiveSelection()
                result.putString("result", "PASS stable selected route and LIVE state UI")
            } else if (action == "mixed-preview") {
                checkMixedPreview()
                result.putString(
                    "result",
                    "PASS mixed photo/video swipes, playback/pause/seek, six video swipe dismissals, loading/error/background cleanup and fresh camera/capture recovery",
                )
            } else if (action == "swipe-return") {
                checkSwipeReturn()
                result.putString(
                    "result",
                    "PASS six downward-swipe dismissals, fresh displayed frames and capture after return",
                )
            } else if (action == "return") {
                checkPreviewReturn()
                result.putString(
                    "result",
                    "PASS six external preview round trips (including immediate return), effect/config preservation, camera recovery and capture after return",
                )
            } else if (action == "faults") {
                checkFaults()
                result.putString(
                    "result",
                    "PASS LIVE FAULT GPU snapshots, recoverable bypass, chain invariance, microphone handoff prerequisites and foreground cleanup",
                )
            } else if (action == "geo") {
                checkGeo()
                result.putString(
                    "result",
                    "PASS location checks for the current permission/service state",
                )
            } else if (action == "compatibility") {
                checkCompatibility()
                result.putString(
                    "result",
                    "PASS camera catalogs, low-resolution selection, JPEG without encoder, session recovery, front/back startup",
                )
            } else if (action == "effects") {
                checkGpu()
                checkNewEffects()
                result.putString(
                    "result",
                    "PASS all 13 fault shaders, every named control, snapshot replay, bypass and causal composition",
                )
            } else if (action == "metadata") {
                checkMetadata()
                checkGpu()
                result.putString(
                    "result",
                    "PASS JPEG EXIF/GPS, RAW invariants, GPU chain composition/orientation/zero strength",
                )
            } else if (action == "state") {
                checkState()
                checkGpu()
                result.putString(
                    "result",
                    "PASS state commit/cancel/cleanup and fault GPU behavior",
                )
            } else {
                val chosen = CaptureSettings(original)
                chosen.rawVideo = false
                chosen.resolutionAudio = args!!.getString("resolutionAudio", "false") == "true"
                chosen.advancedMode = args!!.getString("advanced", "false") == "true"
                chosen.experimentalSignals = args!!.getString("experimental", "false") == "true"
                chosen.location = args!!.getString("gps", "false") == "true"
                chosen.jpegQuality = Integer.parseInt(args!!.getString("quality", "100"))
                chosen.videoQuality = Integer.parseInt(args!!.getString("bitrate", "3"))
                chosen.codec = args!!.getString("codec", original!!.codec)
                chosen.videoKey = args!!.getString("videoKey", "")
                chosen.photoSize = args!!.getString("photoSize", "auto")
                chosen.photoFormat =
                    if (action == "raw") 2 else if (action == "raw-original") 1 else 0
                val recording = action == "video" || action == "segment"
                val requestedSound = args!!.getString("sound", "true") != "false"
                val preset =
                    Integer.parseInt(
                        args!!.getString(
                            "effect",
                            Integer.toString(
                                if (chosen.photoFormat == 2) Effects.ROW_ERROR else Effects.CLEAN
                            ),
                        )
                    )
                val power = Integer.parseInt(args!!.getString("power", "70"))
                val previous = activity!!.engine.generation
                runOnMainSync({
                    activity!!.videoMode = recording
                    activity!!.applySettings(chosen)
                    val mask = Integer.parseInt(args!!.getString("chainMask", "0"))
                    var selected = activity!!.effectState.single(preset)
                    if (mask != 0) selected = selected.chain(mask)
                    if (chosen.experimentalSignals) selected = selected.edit(true, selected.mask,
                        selected.parameters().override(Effects.MOTION_BLUR, "blurX", .04f)
                            .override(Effects.THERMAL_NOISE, "noiseAmplitude", .12f)
                            .override(Effects.SMEAR, "smearAmount", 1f))
                    activity!!.commitEffects(selected.amount(power / 100f))
                })
                await(
                    "configured",
                    {
                        activity!!.engine.generation > previous &&
                            activity!!.engine.frameSeen &&
                            activity!!.ready
                    },
                    20000,
                )
                SystemClock.sleep(1000)
                if (
                    recording &&
                        !chosen.videoKey.isEmpty() &&
                        chosen.videoKey != "recommended" &&
                        activity!!.engine.videoChoice!!.key() != chosen.videoKey
                )
                    throw AssertionError(
                        "Requested video mode not selected: " +
                            activity!!.engine.videoChoice!!.key()
                    )
                if (chosen.location) await("GPS fix", { activity!!.geo.snapshot() != null }, 15000)
                val before = activity!!.latest
                if (recording) {
                    if (action == "segment") activity!!.engine.segmentBytes = 8_000_000L
                    activity!!.engine.toggleVideo(requestedSound)
                    await("recording", { activity!!.engine.recording }, 15000)
                    if (args!!.getString("liveFault", "false") == "true") {
                        await(
                            "audio ownership",
                            {
                                activity!!.engine.recorderAudio == requestedSound &&
                                    (if (requestedSound)
                                        activity!!.engine.faultInputs.microphone == null
                                    else activity!!.engine.faultInputs.microphone != null)
                            },
                            5000,
                        )
                    }
                    SystemClock.sleep(Integer.parseInt(args!!.getString("seconds", "6")) * 1000L)
                    activity!!.engine.toggleVideo(requestedSound)
                    await(
                        "stop",
                        { !activity!!.engine.recording && activity!!.engine.recorder == null },
                        20000,
                    )
                } else {
                    val captures = args!!.getString("captures", "1")!!.toInt().coerceIn(1, 5)
                    val savedUris = ArrayList<String>()
                    for (index in 0 until captures) {
                        val prior = activity!!.latest
                        await("acknowledged photo frame", { activity!!.engine.previewAcknowledged() > 0 && activity!!.ready }, 20000)
                        runOnMainSync { activity!!.engine.photo() }
                        await("saved capture $index", { activity!!.latest != null && activity!!.latest != prior && !activity!!.engine.photoBusy }, 60000)
                        savedUris.add(activity!!.latest.toString())
                    }
                    result.putString("captureUris", savedUris.joinToString("\n"))
                }
                await(
                    "saved",
                    {
                        activity!!.latest != null &&
                            activity!!.latest != before &&
                            !activity!!.engine.photoBusy
                    },
                    40000,
                )
                SystemClock.sleep(600)
                if (chosen.photoFormat == 0 || recording)
                    await("saved thumbnail", { activity!!.galleryButton!!.hasThumbnail }, 10000)
                val saved = requireNotNull(activity!!.latest)
                result.putString("uri", saved.toString())
                result.putString("mime", getTargetContext().getContentResolver().getType(saved))
                if (chosen.location && !recording) {
                    getTargetContext()
                        .getContentResolver()
                        .openInputStream(saved)
                        .use({ `in` ->
                            if (!ExifInterface(requireNotNull(`in`)).getLatLong(FloatArray(2)))
                                throw AssertionError("Saved JPEG is missing GPS")
                        })
                }
                result.putString(
                    "result",
                    "PASS saved " + action + " / measured " + activity!!.measuredFps + " fps",
                )
                getTargetContext()
                    .getContentResolver()
                    .query(
                        saved,
                        arrayOf<String>(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.WIDTH,
                            MediaStore.MediaColumns.HEIGHT,
                            MediaStore.MediaColumns.RELATIVE_PATH,
                        ),
                        null,
                        null,
                        null,
                    )
                    .use({ cursor ->
                        if (cursor != null && cursor!!.moveToFirst()) {
                            val name = cursor!!.getString(0)
                            result.putString("name", name)
                            result.putString(
                                "dimensions",
                                cursor!!.getInt(1).toString() + "x" + cursor!!.getInt(2),
                            )
                            val folder = cursor!!.getString(3)
                            result.putString("folder", folder)
                            if ("DCIM/5igna1/" != folder)
                                throw AssertionError(
                                    "Capture outside shared camera folder: " + folder
                                )
                            val dir = File(getTargetContext().getFilesDir(), "verification")
                            dir.mkdirs()
                            getTargetContext()
                                .getContentResolver()
                                .openInputStream(saved)
                                .use({ `in` ->
                                    FileOutputStream(File(dir, name))
                                        .use({ out ->
                                            GlitchEngine.copy(requireNotNull(`in`), out)
                                        })
                                })
                        }
                    })
            }
        } catch (error: Throwable) {
            result.putString("failure", android.util.Log.getStackTraceString(error))
            activity?.engine?.let { e -> result.putString("captureState", "busy=${e.photoBusy} pending=${e.pending != null} ack=${e.presentedFrames.acknowledged()} attached=${e.attached} cooling=${e.cooling} still=${e.stillReader != null} format=${e.settings.photoFormat}") }
        } finally {
            launchMonitor?.let { removeMonitor(it) }
            if (activity != null && original != null) {
                val restore = original
                val restoreEffects = requireNotNull(effectsBefore)
                val restoreVideo = video
                val restoreFaults = requireNotNull(faultsBefore)
                runOnMainSync({
                    activity!!.applyFaultConfig(restoreFaults)
                    activity!!.tapMode = false
                    activity!!.videoMode = restoreVideo
                    val restoreLocation = restore!!.location
                    restore!!.location = false
                    activity!!.applySettings(restore)
                    activity!!.setLocationEnabled(restoreLocation)
                    activity!!.commitEffects(restoreEffects)
                    val cover =
                        activity!!
                            .getWindow()
                            .getDecorView()
                            .findViewWithTag<View>("deviceCheckOverlay")
                    if (cover != null)
                        (cover!!.getParent() as android.view.ViewGroup).removeView(cover)
                })
                getTargetContext().getSharedPreferences("signal", 0).edit().commit()
            }
            finish(
                if (result.containsKey("failure")) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                result,
            )
        }
    }

    internal fun tutorialClick(tag: String) {
        runOnMainSync({
            activity!!
                .tutorial!!
                .dialog
                .getWindow()!!
                .getDecorView()
                .findViewWithTag<View>(tag)
                .performClick()
        })
        waitForIdleSync()
    }

    @Throws(Exception::class)
    internal fun recreateTutorialActivity() {
        val old = activity
        val monitor = addMonitor(MainActivity::class.java!!.getName(), null, false)
        runOnMainSync(Runnable({ old!!.recreate() }))
        activity = monitor.waitForActivityWithTimeout(20000) as MainActivity
        removeMonitor(monitor)
        if (activity == null) throw AssertionError("Recreation timeout")
        waitForIdleSync()
    }

    internal fun checkExperimentalControls() {
        val advancedBefore = activity!!.advancedMode
        val languageBefore = AppLanguage.current()
        try {
            changeLanguage("ja")
            runOnMainSync {
                val off = CaptureSettings(activity!!.settings)
                off.experimentalSignals = false
                activity!!.applySettings(off)
                activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
            }
            await("experimental off ready", { activity!!.ready }, 20000)
            lateinit var q: QualityDialog
            runOnMainSync {
                q = QualityDialog(activity!!); q.show()
                q.content!!.findViewWithTag<View>("experimental-signals").performClick()
                if (!activity!!.settings.experimentalSignals || !CaptureSettings.load(activity!!.getSharedPreferences("signal", 0)).experimentalSignals)
                    throw AssertionError("Experimental switch not immediately saved")
                activity!!.advancedMode = true
            }
            saveUi("experimental-settings-ja.png")
            runOnMainSync { q.sheet!!.dismiss() }
            await("experimental on ready", { activity!!.ready }, 20000)
            lateinit var edit: EffectDialog
            runOnMainSync {
                edit = EffectDialog(activity!!, true); edit.show()
                edit.body!!.findViewWithTag<View>("group-INPUT").performClick()
                edit.body!!.findViewWithTag<View>("auto-thermalSensitivity").performClick()
                edit.draft = edit.draft.override(Effects.PIXEL_DAMAGE, "thermalSensitivity", 3f)
                edit.preview(); edit.renderBody()
            }
            saveUi("experimental-inputs-ja.png")
            runOnMainSync { edit.sheet!!.dismiss() }
            if (activity!!.effectState.parameters().manual(Effects.PIXEL_DAMAGE, "thermalSensitivity"))
                throw AssertionError("Cancelled input sensitivity leaked")
            runOnMainSync {
                edit = EffectDialog(activity!!, true); edit.show()
                edit.draft = edit.draft.override(Effects.PIXEL_DAMAGE, "thermalSensitivity", 3f)
                edit.sheet!!.window!!.decorView.findViewWithTag<View>("apply").performClick()
            }
            if (activity!!.effectState.parameters().overrides(Effects.PIXEL_DAMAGE)["thermalSensitivity"] != 3f)
                throw AssertionError("Sensitivity not committed")
            val selected = activity!!.effectState.chain(activity!!.effectState.mask or (1 shl Effects.THERMAL_NOISE))
            runOnMainSync {
                activity!!.commitEffects(selected)
                q = QualityDialog(activity!!); q.show()
                q.content!!.findViewWithTag<View>("experimental-signals").performClick()
                q.sheet!!.dismiss()
            }
            await("experimental bypass", { activity!!.ready }, 20000)
            if (!activity!!.effectState.enabled(Effects.THERMAL_NOISE) || activity!!.effectState.parameters().overrides(Effects.PIXEL_DAMAGE)["thermalSensitivity"] != 3f)
                throw AssertionError("Switch discarded saved experimental data")
            val m = FaultModel(7).apply(activity!!.effectState.snapshot(false, 0), activity!!.faultConfig.experimental(false))
            if (m.ids().any { Effects.physical(it) }) throw AssertionError("Experimental stage not bypassed")
            runOnMainSync {
                edit = EffectDialog(activity!!, true); edit.show()
                if (edit.body!!.findViewWithTag<View>("group-INPUT") != null)
                    throw AssertionError("Input controls visible while disabled")
                edit.sheet!!.dismiss()
            }
            recreateTutorialActivity()
            await("experimental preference restored", { activity!!.ready }, 20000)
            if (activity!!.settings.experimentalSignals || !activity!!.effectState.enabled(Effects.THERMAL_NOISE))
                throw AssertionError("Experiment preference/selection lost on recreation")
        } finally {
            runOnMainSync { activity!!.advancedMode = advancedBefore; activity!!.savePrefs() }
            changeLanguage(languageBefore)
        }
    }

    internal fun checkImmediateSettings() {
        val beforeAdvanced = activity!!.advancedMode
        val beforeAudio = activity!!.settings.resolutionAudio
        val beforeSound = activity!!.sound
        try {
            runOnMainSync {
                val q = QualityDialog(activity!!)
                q.show()
                if (findText(q.sheet!!.window!!.decorView, activity!!.getString(R.string.ui_apply)) != null)
                    throw AssertionError("Apply remains in Settings")
                q.content!!.findViewWithTag<View>("advanced-mode").performClick()
                if (activity!!.advancedMode == beforeAdvanced)
                    throw AssertionError("Advanced not immediate")
                q.content!!.findViewWithTag<View>("resolution-audio").performClick()
                check(activity!!.settings.resolutionAudio != beforeAudio && activity!!.sound == beforeSound)
                check(CaptureSettings.load(activity!!.getSharedPreferences("signal", 0)).resolutionAudio != beforeAudio)
                q.jpeg!!.performClick()
                val choices = android.view.inspector.WindowInspector.getGlobalWindowViews()
                    .first { it.findViewWithTag<View>("choice-0") != null }
                choices.findViewWithTag<View>("choice-0").performClick()
                if (activity!!.settings.jpegQuality != 85 || CaptureSettings.load(activity!!.getSharedPreferences("signal", 0)).jpegQuality != 85)
                    throw AssertionError("JPEG selection not saved immediately")
                // Several changes within one UI turn must converge on the latest request.
                repeat(4) { q.content!!.findViewWithTag<View>("expert-mode").performClick() }
                q.content!!.findViewWithTag<View>("load-recommend").performClick()
                q.sheet!!.dismiss()
            }
            await("latest settings configured", { activity!!.ready && !activity!!.settings.expertMode }, 20000)
            if (activity!!.settings.jpegQuality != 85 || activity!!.settings.photoSize != "recommended")
                throw AssertionError("Older configuration overwrote selection")
            recreateTutorialActivity()
            await("camera after settings recreation", { activity!!.ready }, 20000)
            lateinit var reopened: QualityDialog
            runOnMainSync {
                reopened = QualityDialog(activity!!)
                val q = reopened
                q.show()
                if (q.draft.jpegQuality != 85 || q.draft.expertMode || q.advanced == beforeAdvanced || q.draft.resolutionAudio == beforeAudio)
                    throw AssertionError("Settings lost on recreation")
            }
            saveUi("settings-immediate.png")
            runOnMainSync { reopened.sheet!!.dismiss() }
        } finally {
            runOnMainSync { activity!!.advancedMode = beforeAdvanced; activity!!.savePrefs() }
        }
    }

    @Throws(Exception::class)
    internal fun checkTutorial() {
        await(
            "first-launch tutorial",
            { activity!!.tutorial != null && activity!!.tutorial!!.dialog.isShowing() },
            5000,
        )
        if (
            getTargetContext()
                .getSharedPreferences("signal", 0)
                .getBoolean(TutorialDialog.SEEN, false)
        )
            throw AssertionError("Seen before user dismissal")
        tutorialClick("tutorial-next")
        tutorialClick("tutorial-next")
        recreateTutorialActivity()
        if (activity!!.tutorial == null || activity!!.tutorial!!.page != 2)
            throw AssertionError("Lost tutorial page on recreation")
        tutorialClick("tutorial-back")
        if (activity!!.tutorial!!.page != 1) throw AssertionError("Back page")
        tutorialClick("tutorial-skip")
        if (
            !getTargetContext()
                .getSharedPreferences("signal", 0)
                .getBoolean(TutorialDialog.SEEN, false)
        )
            throw AssertionError("Skip not persisted")
        recreateTutorialActivity()
        if (activity!!.tutorial != null) throw AssertionError("Tutorial repeated after skip")
        await("camera after guide", { activity!!.ready && activity!!.engine.frameSeen }, 20000)
        val savedSettings = CaptureSettings(activity!!.settings)
        try {
            runOnMainSync {
                val q = QualityDialog(activity!!)
                q.show()
                q.draft.expertMode = true
                q.content!!.findViewWithTag<SignalToggle>("expert-mode").isChecked = true
                q.content!!.findViewWithTag<View>("load-recommend").performClick()
                if (q.draft.expertMode || q.content!!.findViewWithTag<SignalToggle>("expert-mode").isChecked)
                    throw AssertionError("Recommended keeps EXPERT")
                if (activity!!.settings.expertMode || CaptureSettings.load(activity!!.getSharedPreferences("signal", 0)).expertMode)
                    throw AssertionError("Recommended not immediately saved")
                if (findText(q.sheet!!.window!!.decorView, activity!!.getString(R.string.ui_apply)) != null)
                    throw AssertionError("Settings still has Apply")
                q.sheet!!.dismiss()
                val reopened = QualityDialog(activity!!)
                reopened.show()
                if (reopened.draft.expertMode || reopened.draft.photoSize != "recommended")
                    throw AssertionError("Settings lost on reopening")
                reopened.sheet!!.dismiss()
            }
        } finally {
            runOnMainSync { activity!!.applySettings(savedSettings) }
        }
        await("camera after recommendation", { activity!!.ready }, 20000)
        val language = AppLanguage.current()
        var tutorialFailure: Throwable? = null
        try {
            for (tag in arrayOf<String>("ja", "en", "zh")) {
                changeLanguage(tag)
                val settings = arrayOfNulls<QualityDialog>(1)
                val before = activity!!.advancedMode
                runOnMainSync({
                    settings[0] = QualityDialog(activity!!)
                    settings[0]!!.show()
                    settings[0]!!.content!!.findViewWithTag<View>("advanced-mode").performClick()
                    settings[0]!!
                        .content!!
                        .findViewWithTag<View>("settings-tutorial")
                        .performClick()
                })
                for (page in 0 until TutorialDialog.PAGE_COUNT) {
                    if (activity!!.tutorial!!.page != page) throw AssertionError("Unexpected page")
                    SystemClock.sleep(900)
                    val guide = activity!!.tutorial!!
                    if (activity!!.ready || activity!!.engine.attached)
                        throw AssertionError("Camera active behind guide")
                    if (page > 0 && guide.targetBounds.isEmpty)
                        throw AssertionError("Missing real control highlight")
                    val beforeEffect = activity!!.effectState.encode()
                    val beforeCount = activity!!.captureCount
                    tutorialClick("tutorial-practice")
                    if (page > 0) tutorialClick("tutorial-target")
                    if (beforeEffect != activity!!.effectState.encode() || beforeCount != activity!!.captureCount)
                        throw AssertionError("Guide changed capture state")
                    languageScreenshot("tutorial-" + tag + "-" + page)
                    tutorialClick("tutorial-next")
                }
                if (
                    activity!!.tutorial != null ||
                        !settings[0]!!.sheet!!.isShowing() ||
                        settings[0]!!.advanced == before ||
                        activity!!.advancedMode == before
                )
                    throw AssertionError("Tutorial lost saved settings")
                runOnMainSync({
                    settings[0]!!.sheet!!.dismiss()
                    activity!!.advancedMode = before
                    activity!!.savePrefs()
                })
            }
        } catch (failure: Throwable) {
            tutorialFailure = failure
            throw failure
        } finally {
            try { changeLanguage(language) } catch (cleanup: Throwable) {
                if (tutorialFailure != null) tutorialFailure.addSuppressed(cleanup) else throw cleanup
            }
        }
        runOnMainSync({ activity!!.showTutorial() })
        // Dialog.show returns before WindowManager necessarily assigns input focus.
        await(
            "tutorial input focus",
            {
                activity!!.tutorial?.dialog?.window?.decorView?.hasWindowFocus() == true
            },
            5000,
        )
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await("system back closes tutorial", { activity!!.tutorial == null }, 5000)
        recreateTutorialActivity()
        if (activity!!.tutorial != null) throw AssertionError("Tutorial repeated after completion")
        await("final preview", { activity!!.ready && activity!!.engine.frameSeen }, 20000)
    }

    @Throws(Exception::class)
    internal fun checkStoreScreenshots() {
        val locale = AppLanguage.current()
        try {
            runOnMainSync({
                val cover =
                    activity!!
                        .getWindow()
                        .getDecorView()
                        .findViewWithTag<View>("deviceCheckOverlay")
                if (cover != null) (cover!!.getParent() as android.view.ViewGroup).removeView(cover)
                val settings = CaptureSettings(activity!!.settings)
                settings.rawVideo = false
                settings.photoFormat = 0
                settings.photoSize = "auto"
                settings.location = false
                activity!!.videoMode = false
                activity!!.applySettings(settings)
                activity!!.applyFaultConfig(FaultConfig.defaults())
                activity!!.commitEffects(EffectState.defaults())
            })
            await("screenshot camera", { activity!!.ready && activity!!.engine.frameSeen }, 20000)
            val before = activity!!.captureCount
            runOnMainSync({ activity!!.shoot() })
            await(
                "screenshot thumbnail",
                { activity!!.captureCount > before && activity!!.galleryButton!!.hasThumbnail },
                30000,
            )
            for (language in arrayOf<String>("ja", "en")) {
                changeLanguage(language)
                runOnMainSync({ activity!!.commitEffects(EffectState.defaults()) })
                SystemClock.sleep(2000)
                languageScreenshot("store-" + language + "-01-camera")
                runOnMainSync({
                    activity!!.commitEffects(
                        EffectState.defaults().single(Effects.ROW_ERROR).amount(.8f)
                    )
                })
                SystemClock.sleep(2000)
                languageScreenshot("store-" + language + "-02-row-shift")
                runOnMainSync({
                    activity!!.commitEffects(
                        EffectState.defaults()
                            .chain(
                                (1 shl Effects.ROW_ERROR) or
                                    (1 shl Effects.CHROMA_ERROR) or
                                    (1 shl Effects.VHS)
                            )
                            .amount(.65f)
                    )
                })
                SystemClock.sleep(2000)
                languageScreenshot("store-" + language + "-03-chain")
                runOnMainSync({ EffectDialog(activity!!, true).show() })
                SystemClock.sleep(750)
                languageScreenshot("store-" + language + "-04-adjust")
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                runOnMainSync({ AboutDialog.showLicenses(activity!!) })
                SystemClock.sleep(500)
                languageScreenshot("store-" + language + "-licenses")
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            }
        } finally {
            changeLanguage(locale)
        }
    }

    @Throws(Exception::class)
    internal fun changeLanguage(tags: String) {
        if (AppLanguage.current() == tags) return
        val before = activity
        val monitor = addMonitor(MainActivity::class.java!!.getName(), null, false)
        runOnMainSync({ AppLanguage.select(tags) })
        val deadline = SystemClock.elapsedRealtime() + 30000
        var complete = false
        while (SystemClock.elapsedRealtime() < deadline) {
            val next = monitor.waitForActivityWithTimeout(250) as? MainActivity
            if (next != null) activity = next
            val current = activity!!
            complete = current !== before && !current.isDestroyed && current.resumed && current.ready && current.engine.frameSeen
            if (complete) break
        }
        removeMonitor(monitor)
        check(complete) { "Localized camera $tags: destroyed=${activity!!.isDestroyed}, resumed=${activity!!.resumed}, tutorial=${activity!!.tutorial?.page}, attached=${activity!!.engine.attached}" }
        if (before!!.engine.attached) throw AssertionError("Old camera remains attached")
    }

    @Throws(Exception::class)
    internal fun languageScreenshot(name: String) {
        val dir = File(getTargetContext().getFilesDir(), "verification")
        dir.mkdirs()
        val screen = getUiAutomation().takeScreenshot()
        try {
            FileOutputStream(File(dir, "language-" + name + ".png"))
                .use({ out -> screen.compress(Bitmap.CompressFormat.PNG, 100, out) })
        } finally {
            screen.recycle()
        }
    }

    @Throws(Exception::class)
    internal fun checkLanguage() {
        val originalLocale = AppLanguage.current()
        val originalCount = activity!!.captureCount
        try {
            runOnMainSync({
                activity!!.commitEffects(EffectState.defaults().single(Effects.VHS).amount(.63f))
                activity!!.applyFaultConfig(FaultConfig.defaults().enabled(true))
                activity!!.captureCount = 123
            })
            val encoded = activity!!.effectState.encode()
            val quality = activity!!.settings.jpegQuality
            val tags = arrayOf<String>("ja", "en", "zh", "")
            val titles = arrayOf<String>("言語", "App language", "应用语言")
            val policies = arrayOf<String>("プライバシーポリシー", "Privacy Policy", "隐私政策")
            for (n in tags.indices) {
                changeLanguage(tags[n])
                if (
                    encoded != activity!!.effectState.encode() ||
                        activity!!.settings.jpegQuality != quality ||
                        !activity!!.faultConfig.enabled ||
                        activity!!.captureCount != 123
                )
                    throw AssertionError("Locale changed session state")
                if (n < 3) {
                    if (!activity!!.getString(R.string.language_title).contains(titles[n]))
                        throw AssertionError(
                            "Wrong translated title: " +
                                activity!!.getString(R.string.language_title)
                        )
                    activity!!
                        .getResources()
                        .openRawResource(R.raw.privacy_policy)
                        .use({ `in` ->
                            if (
                                !String(
                                        `in`.readAllBytes(),
                                        java.nio.charset.StandardCharsets.UTF_8,
                                    )
                                    .contains(policies[n])
                            )
                                throw AssertionError("Wrong policy locale")
                        })
                } else {
                    val expected =
                        if (Build.VERSION.SDK_INT >= 33)
                            activity!!
                                .getSystemService(android.app.LocaleManager::class.java)
                                .getSystemLocales()
                                .get(0)
                                .getLanguage()
                        else
                            android.content.res.Resources.getSystem()
                                .getConfiguration()
                                .getLocales()
                                .get(0)
                                .getLanguage()
                    if (
                        !activity!!
                            .getResources()
                            .getConfiguration()
                            .getLocales()
                            .get(0)
                            .getLanguage()
                            .equals(expected)
                    )
                        throw AssertionError(
                            "System language not followed: expected " +
                                expected +
                                " actual " +
                                activity!!.getResources().getConfiguration().getLocales()
                        )
                }
                val name = if (tags[n].isEmpty()) "system" else tags[n]
                languageScreenshot(name + "-main")
                runOnMainSync({ QualityDialog(activity!!).show() })
                SystemClock.sleep(500)
                languageScreenshot(name + "-settings")
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                runOnMainSync({ FaultDialog.show(activity!!) })
                SystemClock.sleep(500)
                languageScreenshot(name + "-live")
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            }
        } finally {
            changeLanguage(originalLocale)
            runOnMainSync({ activity!!.captureCount = originalCount })
        }
    }

    @Throws(Exception::class)
    internal fun checkRawCaps(result: Bundle) {
        val manager =
            activity!!.getSystemService(Context.CAMERA_SERVICE)
                as android.hardware.camera2.CameraManager
        val caps = StringBuilder()
        for (id in manager.getCameraIdList()) {
            val options = CameraOptions(id, manager.getCameraCharacteristics(id), 4096)
            caps.append(id).append(": ")
            for (raw in options.rawVideos) caps.append(raw.label(activity!!)).append("; ")
            if (options.rawVideos.isEmpty()) caps.append(options.rawVideoReason(activity!!))
            caps.append('\n')
        }
        result.putString("result", "PASS queried RAW capture capabilities")
        result.putString("rawVideo", caps.toString())
    }

    @Throws(Exception::class)
    internal fun checkLiveSelection() {
        val selected =
            EffectState.defaults().chain((1 shl Effects.VHS) or (1 shl Effects.CHROMA_ERROR))
        runOnMainSync({
            activity!!.commitEffects(selected)
            activity!!.applyFaultConfig(FaultConfig.defaults().enabled(true))
        })
        await(
            "live UI frame",
            {
                activity!!.shownLiveFrame != null &&
                    Arrays.equals(activity!!.shownLiveFrame!!.ids(), selected.ids())
            },
            5000,
        )
        SystemClock.sleep(1800)
        runOnMainSync({
            if (!Arrays.equals(activity!!.shownLiveFrame!!.ids(), selected.ids()))
                throw AssertionError("Route changed during LIVE")
            if (
                !activity!!
                    .liveChainStatus!!
                    .getText()
                    .toString()
                    .contains(Effects.chainName(selected.ids()))
            )
                throw AssertionError("Actual route is not displayed")
        })
    }

    @Throws(Exception::class)
    internal fun checkRawVideo(result: Bundle) {
        if (!activity!!.cameraOptions!!.rawVideoAvailable()) {
            result.putString(
                "result",
                "UNAVAILABLE: " + activity!!.cameraOptions!!.rawVideoReason(activity!!),
            )
            return
        }
        val raw = CaptureSettings(activity!!.settings)
        raw.location = false
        raw.rawVideoEnabled = true
        raw.rawVideo = true
        raw.rawVideoFps = 2
        raw.rawVideoSize = activity!!.cameraOptions!!.rawVideos.get(0).size.toString()
        val generation = activity!!.engine.generation
        runOnMainSync({
            activity!!.videoMode = true
            activity!!.applySettings(raw)
        })
        await(
            "RAW session validation",
            {
                activity!!.engine.generation > generation &&
                    activity!!.engine.frameSeen &&
                    (activity!!.engine.rawFrameSeen || !activity!!.settings.rawVideo)
            },
            20000,
        )
        if (!activity!!.settings.rawVideo) {
            result.putString(
                "result",
                "UNAVAILABLE verified: " + activity!!.cameraOptions!!.rawVideoReason(activity!!),
            )
            return
        }
        await("RAW ready", { activity!!.ready }, 5000)
        val saved = activity!!.captureCount
        activity!!.engine.toggleVideo(false)
        await(
            "RAW recording",
            { activity!!.engine.rawRecorder != null && activity!!.engine.recording },
            5000,
        )
        val writer = activity!!.engine.rawRecorder
        val background = args!!.getString("background", "false") == "true"
        val minimumFrames = if (background) 1 else 2
        await("RAW frames written", { writer!!.written >= minimumFrames || writer!!.failed }, 20000)
        if (writer!!.failed) throw AssertionError("DNG writer failed")
        if (background) {
            getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME").use({ home -> })
            await("RAW pause", { !activity!!.resumed && !activity!!.engine.attached }, 5000)
        } else activity!!.engine.toggleVideo(false)
        await(
            "RAW ZIP saved",
            {
                !activity!!.engine.photoBusy &&
                    !activity!!.engine.recording &&
                    activity!!.captureCount > saved
            },
            20000,
        )
        val uri = requireNotNull(activity!!.latest)
        if ("application/zip" != getTargetContext().getContentResolver().getType(uri))
            throw AssertionError("RAW recording was not a ZIP")
        var frames = 0
        var manifest = false
        var timestamps = false
        java.util.zip
            .ZipInputStream(getTargetContext().getContentResolver().openInputStream(uri))
            .use({ zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val bytes = ByteArrayOutputStream()
                    val block = ByteArray(65536)
                    while (true) {
                        val n = zip.read(block)
                        if (n == -1) break
                        bytes.write(block, 0, n)
                    }
                    if (entry.getName().endsWith(".dng")) {
                        frames++
                        val exif = ExifInterface(ByteArrayInputStream(bytes.toByteArray()))
                        if (
                            exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) !=
                                writer!!.size.getWidth() ||
                                exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) !=
                                    writer!!.size.getHeight()
                        )
                            throw AssertionError("DNG dimensions differ from sensor stream")
                    }
                    if (entry.getName() == "manifest.json") {
                        val info = org.json.JSONObject(bytes.toString("UTF-8"))
                        manifest =
                            !info.getBoolean("effects_applied") &&
                                !info.getBoolean("audio") &&
                                info.getInt("frames") >= minimumFrames
                    }
                    if (entry.getName() == "timestamps.csv")
                        timestamps =
                            bytes
                                .toString("UTF-8")
                                .split(("\n").toRegex())
                                .dropLastWhile({ it.isEmpty() })
                                .toTypedArray()
                                .size >= minimumFrames + 1
                    zip.closeEntry()
                }
            })
        if (frames < minimumFrames || !manifest || !timestamps)
            throw AssertionError("Incomplete RAW sequence archive")
        result.putString(
            "result",
            "PASS RAW session, original DNG sequence, matched metadata, timestamps, manifest and stop/finalize",
        )
        result.putInt("frames", frames)
        result.putString("uri", uri.toString())
        if (background) {
            getUiAutomation()
                .executeShellCommand(
                    "am start -n " +
                        getTargetContext().getPackageName() +
                        "/" +
                        MainActivity::class.java!!.getName()
                )
                .use({ resume -> })
            await(
                "RAW resume validation",
                { activity!!.resumed && activity!!.ready && activity!!.engine.rawFrameSeen },
                20000,
            )
            result.putString(
                "lifecycle",
                "PASS background stop/finalize and resumed RAW capture validation",
            )
        }
    }

    @Throws(Exception::class)
    internal fun checkProductUi() {
        runOnMainSync({
            val next = CaptureSettings(activity!!.settings)
            next.photoFormat = 0
            next.photoSize = "recommended"
            next.expertMode = false
            activity!!.videoMode = false
            activity!!.applySettings(next)
            activity!!.commitEffects(
                EffectState.defaults()
                    .chain(
                        (1 shl Effects.VHS) or
                            (1 shl Effects.PIXEL_DAMAGE) or
                            (1 shl Effects.ROW_ERROR)
                    )
            )
            activity!!.applyFaultConfig(FaultConfig.defaults().enabled(true))
        })
        await("product preview", { activity!!.ready && activity!!.shownLiveFrame != null }, 20000)
        runOnMainSync({
            val top = IntArray(2)
            val camera = IntArray(2)
            activity!!.previewArea!!.getLocationOnScreen(camera)
            for (utility in
                arrayOf<android.view.View>(
                    activity!!.torchButton!!,
                    activity!!.geoButton!!,
                    activity!!.micButton!!,
                )) {
                utility.getLocationOnScreen(top)
                if (top[1] + utility.getHeight() > camera[1])
                    throw AssertionError("Utility remains below preview")
            }
        })
        runOnMainSync({
            val sound = activity!!.sound
            if (activity!!.micButton!!.getVisibility() != android.view.View.VISIBLE)
                throw AssertionError("Audio control hidden in photo mode")
            activity!!.micButton!!.performClick()
            if (
                activity!!.sound == sound || activity!!.micButton!!.isSelected() != activity!!.sound
            )
                throw AssertionError("Audio toggle failed")
            activity!!.micButton!!.performClick()
            if (activity!!.sound != sound) throw AssertionError("Audio restore failed")
        })
        SystemClock.sleep(500)
        saveUi("product-main.png")
        val originalFormat = activity!!.capturePhotoFormat
        if (activity!!.canCycleFormat()) {
            runOnMainSync { activity!!.formatButton.performClick() }
            await("cycled format", { activity!!.ready && activity!!.capturePhotoFormat != originalFormat }, 20000)
            saveUi("product-format-cycle.png")
            runOnMainSync { activity!!.formatButton.performClick() }
            await("restored format", { activity!!.ready && activity!!.capturePhotoFormat == originalFormat }, 20000)
        }
        runOnMainSync({ activity!!.liveChainStatus!!.performClick() })
        waitForIdleSync()
        SystemClock.sleep(500)
        if (activity!!.faultStatePanel == null || activity!!.faultStatePanel!!.items.size != 3)
            throw AssertionError("Missing fault meter rows")
        saveUi("product-fault-state.png")
        runOnMainSync({ activity!!.liveChainDialog!!.dismiss() })
        val config = activity!!.effectState.encode()
        val generation = activity!!.engine.generation
        glSync({ activity!!.engine.cameraInterrupted() })
        await(
            "camera interruption recovery",
            {
                activity!!.ready &&
                    activity!!.engine.frameSeen &&
                    activity!!.engine.generation > generation
            },
            15000,
        )
        val stamp = activity!!.engine.lastFrameNs
        await("recovered fresh frame", { activity!!.engine.lastFrameNs > stamp }, 5000)
        val beforeStall = activity!!.engine.generation
        glSync({
            try {
                activity!!.engine.session!!.stopRepeating()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        })
        await(
            "stalled preview reconnect",
            {
                activity!!.engine.generation > beforeStall &&
                    activity!!.ready &&
                    activity!!.engine.frameSeen
            },
            18000,
        )
        if (config != activity!!.effectState.encode())
            throw AssertionError("Recovery changed chain")
    }

    @Throws(Exception::class)
    internal fun swipeMedia(horizontal: Boolean, forward: Boolean) {
        val bounds = android.graphics.Rect()
        runOnMainSync({ activity!!.mediaPreview!!.media.getGlobalVisibleRect(bounds) })
        val x = bounds.centerX()
        val y = bounds.centerY()
        val command =
            if (horizontal)
                "input swipe " +
                    (if (forward) bounds.right - bounds.width() / 6
                    else bounds.left + bounds.width() / 6) +
                    " " +
                    y +
                    " " +
                    (if (forward) bounds.left + bounds.width() / 6
                    else bounds.right - bounds.width() / 6) +
                    " " +
                    y +
                    " 250"
            else
                "input swipe " +
                    x +
                    " " +
                    (bounds.top + bounds.height() / 3) +
                    " " +
                    x +
                    " " +
                    (bounds.bottom - bounds.height() / 8) +
                    " 250"
        getUiAutomation().executeShellCommand(command).use({ gesture -> })
    }

    @Throws(Exception::class)
    internal fun checkMixedPreview() {
        runOnMainSync({
            val next = CaptureSettings(activity!!.settings)
            next.photoFormat = 0
            next.photoSize = "recommended"
            next.rawVideo = false
            next.videoKey = "recommended"
            next.videoQuality = 1
            activity!!.videoMode = false
            activity!!.applySettings(next)
        })
        await("mixed photo camera", { activity!!.ready }, 20000)
        val first = activity!!.captureCount
        runOnMainSync({ activity!!.shoot() })
        await(
            "mixed photo saved",
            { activity!!.captureCount > first && !activity!!.engine.photoBusy },
            30000,
        )
        val photo = activity!!.latest
        runOnMainSync({
            activity!!.videoMode = true
            activity!!.applySettings(CaptureSettings(activity!!.settings))
        })
        await("mixed video camera", { activity!!.ready }, 20000)
        val second = activity!!.captureCount
        activity!!.engine.toggleVideo(false)
        await("mixed recording", { activity!!.recording }, 10000)
        SystemClock.sleep(3500)
        activity!!.engine.toggleVideo(false)
        await(
            "mixed video saved",
            { activity!!.captureCount > second && !activity!!.recording },
            20000,
        )
        val movie = activity!!.latest
        val effects = activity!!.effectState.encode()
        runOnMainSync({ activity!!.openGallery() })
        await(
            "internal video plays",
            {
                activity!!.mediaPreview != null &&
                    activity!!.mediaPreview!!.prepared &&
                    activity!!.mediaPreview!!.videoFrameSeen
            },
            15000,
        )
        val viewer = activity!!.mediaPreview
        if (activity!!.engine.attached || !activity!!.resumed)
            throw AssertionError("Camera overlaps internal decoder or viewer left app")
        if (viewer!!.items.get(viewer!!.index).uri != movie)
            throw AssertionError("Wrong initial capture")
        saveUi("mixed-video.png")
        runOnMainSync({
            viewer!!.pausePlayback()
            viewer!!.player!!.seekTo(1000)
        })
        SystemClock.sleep(600)
        val paused = viewer!!.player!!.getCurrentPosition()
        SystemClock.sleep(400)
        if (
            viewer!!.player!!.isPlaying() ||
                Math.abs(viewer!!.player!!.getCurrentPosition() - paused) > 150
        )
            throw AssertionError("Video pause failed")
        runOnMainSync(Runnable({ viewer!!.startPlayback() }))
        await(
            "video time advances",
            {
                viewer!!.player!!.getCurrentPosition() > paused + 200 ||
                    viewer!!.player!!.getCurrentPosition() < paused
            },
            5000,
        )
        swipeMedia(true, true)
        await(
            "photo after video swipe",
            { viewer!!.image != null && viewer!!.image!!.getDrawable() != null },
            15000,
        )
        if (viewer!!.items.get(viewer!!.index).uri != photo || viewer!!.player != null)
            throw AssertionError("Mixed sequence order or decoder cleanup")
        saveUi("mixed-photo.png")
        swipeMedia(true, false)
        await("video after photo swipe", { viewer!!.prepared && viewer!!.videoFrameSeen }, 15000)
        for (n in 0..5) {
            val current = activity!!.mediaPreview
            swipeMedia(false, true)
            await(
                "video swipe dismiss",
                {
                    activity!!.mediaPreview == null &&
                        activity!!.ready &&
                        activity!!.engine.frameSeen
                },
                15000,
            )
            if (current!!.player != null || current!!.videoSurface != null || !current!!.closed)
                throw AssertionError("Video resources retained after dismiss")
            val token = activity!!.engine.previewAcknowledged()
            await(
                "fresh displayed frame after video",
                { activity!!.engine.previewAcknowledged() > token },
                8000,
            )
            if (n < 5) {
                runOnMainSync({ activity!!.openGallery() })
                await(
                    "video reopen",
                    {
                        activity!!.mediaPreview != null &&
                            activity!!.mediaPreview!!.prepared &&
                            activity!!.mediaPreview!!.videoFrameSeen
                    },
                    15000,
                )
                SystemClock.sleep((if (n % 2 == 0) 500 else 1500).toLong())
            }
        }
        runOnMainSync({
            activity!!.openGallery()
            activity!!.mediaPreview!!.dismiss()
        })
        await(
            "close during loading",
            { activity!!.mediaPreview == null && activity!!.ready },
            15000,
        )
        SystemClock.sleep(700)
        if (activity!!.mediaPreview != null || effects != activity!!.effectState.encode())
            throw AssertionError("Late callback reopened viewer or changed effects")
        saveUi("mixed-camera-return.png")
        runOnMainSync({
            MediaPreview(
                    activity!!,
                    Uri.parse("content://media/external/video/media/9223372036854775807"),
                    true,
                )
                .show()
        })
        await(
            "unavailable video handled",
            {
                activity!!.mediaPreview != null &&
                    activity!!
                        .getString(R.string.media_preview_unavailable)
                        .contentEquals(activity!!.mediaPreview!!.notice.getText())
            },
            15000,
        )
        swipeMedia(false, true)
        await(
            "camera after unavailable media",
            { activity!!.mediaPreview == null && activity!!.ready },
            15000,
        )
        runOnMainSync({ activity!!.openGallery() })
        await(
            "viewer before background",
            { activity!!.mediaPreview != null && activity!!.mediaPreview!!.prepared },
            15000,
        )
        val background = activity!!.mediaPreview
        getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME").use({ home -> })
        await(
            "viewer cleanup on background",
            {
                !activity!!.resumed &&
                    activity!!.mediaPreview == null &&
                    background!!.player == null &&
                    !activity!!.engine.attached
            },
            10000,
        )
        getUiAutomation()
            .executeShellCommand(
                "am start -n " +
                    getTargetContext().getPackageName() +
                    "/" +
                    MainActivity::class.java!!.getName()
            )
            .use({ resume -> })
        await("camera after viewer background", { activity!!.resumed && activity!!.ready }, 20000)
        val before = activity!!.captureCount
        activity!!.engine.toggleVideo(false)
        await("record after mixed preview", { activity!!.recording }, 10000)
        SystemClock.sleep(1500)
        activity!!.engine.toggleVideo(false)
        await(
            "save after mixed preview",
            { activity!!.captureCount > before && !activity!!.recording },
            20000,
        )
    }

    @Throws(Exception::class)
    internal fun checkSwipeReturn() {
        val video = args!!.getString("media", "photo") == "video"
        runOnMainSync({
            val next = CaptureSettings(activity!!.settings)
            next.photoFormat = 0
            next.photoSize = "recommended"
            next.rawVideo = false
            if (args!!.getString("quality", "recommended") != "selected") {
                next.videoKey = "recommended"
                next.videoQuality = 1
            }
            activity!!.videoMode = video
            activity!!.applySettings(next)
            activity!!.commitEffects(EffectState.defaults().single(Effects.CLEAN))
        })
        await("swipe test camera", { activity!!.ready }, 20000)
        val initialCount = activity!!.captureCount
        if (video) {
            activity!!.engine.toggleVideo(false)
            await("swipe fixture recording", { activity!!.recording }, 10000)
            SystemClock.sleep(4000)
            activity!!.engine.toggleVideo(false)
        } else runOnMainSync({ activity!!.shoot() })
        await(
            "swipe fixture saved",
            {
                activity!!.captureCount > initialCount &&
                    !activity!!.engine.photoBusy &&
                    !activity!!.recording
            },
            30000,
        )
        val w = getTargetContext().getResources().getDisplayMetrics().widthPixels
        val h = getTargetContext().getResources().getDisplayMetrics().heightPixels
        try {
            for (n in 0..5) {
                runOnMainSync({ activity!!.openGallery() })
                await("viewer opened", { !activity!!.resumed }, 10000)
                SystemClock.sleep((if (n % 2 == 0) 2500 else 350).toLong())
                if (n == 0) saveUi("swipe-viewer.png")
                getUiAutomation()
                    .executeShellCommand(
                        "input swipe " +
                            w / 2 +
                            " " +
                            (h * 40 / 100) +
                            " " +
                            w / 2 +
                            " " +
                            (h * 85 / 100) +
                            " 250"
                    )
                    .use({ gesture -> })
                val limit = SystemClock.elapsedRealtime() + 15000
                while (
                    SystemClock.elapsedRealtime() < limit &&
                        (!activity!!.resumed || !activity!!.ready)
                ) SystemClock.sleep(100)
                if (!activity!!.resumed || !activity!!.ready) {
                    saveUi("swipe-return-failure.png")
                    throw AssertionError(
                        "Swipe return " +
                            n +
                            ": resumed=" +
                            activity!!.resumed +
                            ", ready=" +
                            activity!!.ready +
                            ", attached=" +
                            activity!!.engine.attached +
                            ", cameraFrames=" +
                            activity!!.engine.frameSeen +
                            ", ack=" +
                            activity!!.engine.previewAcknowledged()
                    )
                }
                val token = activity!!.engine.previewAcknowledged()
                await(
                    "new displayed frame after swipe",
                    { activity!!.engine.previewAcknowledged() > token },
                    8000,
                )
                if (n == 5) saveUi("swipe-return-final.png")
            }
            val before = activity!!.captureCount
            if (video) {
                activity!!.engine.toggleVideo(false)
                await("recording after swipe return", { activity!!.recording }, 10000)
                SystemClock.sleep(1500)
                activity!!.engine.toggleVideo(false)
            } else runOnMainSync({ activity!!.shoot() })
            await(
                "capture after swipe return",
                {
                    activity!!.captureCount > before &&
                        !activity!!.engine.photoBusy &&
                        !activity!!.recording
                },
                30000,
            )
        } finally {
            if (!activity!!.resumed) {
                getUiAutomation()
                    .executeShellCommand(
                        "am start -n " +
                            getTargetContext().getPackageName() +
                            "/" +
                            MainActivity::class.java!!.getName()
                    )
                    .use({ recover -> })
                await("return to app after swipe test", { activity!!.resumed }, 10000)
            }
        }
    }

    @Throws(Exception::class)
    internal fun checkPreviewReturn() {
        if (activity!!.latest == null) throw AssertionError("Capture a photo before return test")
        runOnMainSync({
            activity!!.commitEffects(
                EffectState.defaults()
                    .chain((1 shl Effects.VHS) or (1 shl Effects.CHROMA_ERROR))
                    .amount(.7f)
            )
            activity!!.applyFaultConfig(FaultConfig.defaults().enabled(true))
        })
        val encoded = activity!!.effectState.encode()
        val saved = requireNotNull(activity!!.latest)
        for (n in 0..5) {
            runOnMainSync({ activity!!.openExternal(activity!!.latest!!) })
            await("external preview pause", { !activity!!.resumed }, 10000)
            if (n < 3) {
                await("camera closed outside app", { !activity!!.engine.attached }, 10000)
                SystemClock.sleep(700)
            }
            getUiAutomation().executeShellCommand("input keyevent KEYCODE_BACK").use({ back -> })
            await(
                "camera returns",
                { activity!!.resumed && activity!!.ready && activity!!.engine.frameSeen },
                20000,
            )
            if (
                encoded != activity!!.effectState.encode() ||
                    saved != activity!!.latest ||
                    !activity!!.faultConfig.enabled
            )
                throw AssertionError("Preview round trip changed manual state")
            val timestamp = activity!!.engine.lastFrameNs
            await("fresh resumed frames", { activity!!.engine.lastFrameNs > timestamp }, 5000)
            if (
                activity!!.engine.faultInputs.microphone != null ||
                    !activity!!.engine.faultInputs.active
            )
                throw AssertionError("Configured LIVE inputs did not resume correctly")
        }
        val count = activity!!.captureCount
        runOnMainSync({ activity!!.shoot() })
        await(
            "capture after external preview",
            { activity!!.captureCount > count && !activity!!.engine.photoBusy },
            30000,
        )
        if (FaultPreferences.load(getTargetContext().getSharedPreferences("signal", 0)).enabled)
            throw AssertionError("Cold startup must be OFF")
    }

    @Throws(Exception::class)
    internal fun checkFaults() {
        val model = FaultModel()
        val config = FaultConfig(true, true, true, true, true, true, .5f, 50)
        val inputs = FaultModel.Inputs()
        inputs.motionAvailable = true
        inputs.ax = 15f
        inputs.jitter = 1f
        model.advance(1.0, inputs, config)
        model.advance(1.04, inputs, config)
        val selected = EffectState.defaults().single(Effects.VHS).amount(.8f)
        val live = model.apply(selected.snapshot(true, 0), config)
        val jpeg = fixture()
        val first = PhotoRenderer.render(getTargetContext(), jpeg, false, live)
        val same = PhotoRenderer.render(getTargetContext(), jpeg, false, live)
        if (!first!!.sameAs(same)) throw AssertionError("Snapshot replay changed pixels")
        first!!.recycle()
        same!!.recycle()
        val before = activity!!.effectState.encode()
        runOnMainSync({ activity!!.applyFaultConfig(config) })
        await("fault microphone", { activity!!.engine.faultInputs.microphone != null }, 5000)
        SystemClock.sleep(500)
        if (before != activity!!.effectState.encode())
            throw AssertionError("Sensors changed effect selection")
        getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME").use({ home -> })
        await(
            "background input cleanup",
            {
                !activity!!.engine.faultInputs.active &&
                    activity!!.engine.faultInputs.microphone == null
            },
            5000,
        )
        getUiAutomation()
            .executeShellCommand(
                "am start -n " +
                    getTargetContext().getPackageName() +
                    "/" +
                    MainActivity::class.java!!.getName()
            )
            .use({ resume -> })
        await(
            "foreground input restart",
            {
                activity!!.engine.faultInputs.active &&
                    activity!!.engine.faultInputs.microphone != null &&
                    activity!!.engine.frameSeen
            },
            15000,
        )
        runOnMainSync({ activity!!.applyFaultConfig(config.enabled(false)) })
        await(
            "fault input cleanup",
            {
                !activity!!.engine.faultInputs.active &&
                    activity!!.engine.faultInputs.microphone == null
            },
            5000,
        )
    }

    @Throws(Exception::class)
    internal fun checkGeo() {
        val allowed = activity!!.geo.permitted()
        runOnMainSync({ activity!!.setLocationEnabled(true) })
        if (!allowed) {
            if (
                activity!!.geo.snapshot() != null ||
                    activity!!.geo.label() != activity!!.getString(R.string.ui_gps_denied)
            )
                throw AssertionError("Denied permission shown as waiting/fixed")
            return
        }
        if (!activity!!.geo.servicesEnabled()) {
            if (
                activity!!.geo.snapshot() != null ||
                    activity!!.geo.label() != activity!!.getString(R.string.ui_gps_device_off)
            )
                throw AssertionError("Disabled location services shown as usable")
            return
        }
        runOnMainSync({
            val stale = Location("gps")
            stale.setLatitude(35.0)
            stale.setLongitude(139.0)
            stale.setAccuracy(5f)
            stale.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos() - 121_000_000_000L)
            if (GeoTags.fresh(stale, SystemClock.elapsedRealtimeNanos()))
                throw AssertionError("Expired location accepted")
            val fresh = Location(stale)
            fresh.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos())
            activity!!.geo.onLocationChanged(fresh)
            if (activity!!.geo.snapshot() == null) throw AssertionError("Fresh location missing")
            if (!activity!!.geo.precise()) {
                activity!!.geo.permissionLevel = 2
                if (activity!!.geo.snapshot() != null)
                    throw AssertionError("Precise cache survived approximate permission downgrade")
                activity!!.geo.retry()
                activity!!.geo.onLocationChanged(fresh)
            }
            activity!!.geo.start()
            if (activity!!.geo.snapshot() == null) throw AssertionError("Resume erased usable fix")
            activity!!.geo.retry()
            if (activity!!.geo.snapshot() == null) throw AssertionError("Retry erased usable fix")
            val copy = activity!!.geo.snapshot()
            copy!!.setLatitude(0.0)
            if (activity!!.geo.snapshot()!!.getLatitude() == 0.0)
                throw AssertionError("Snapshot is mutable shared state")
            if (
                if (activity!!.geo.precise())
                    activity!!.geo.label().contains(activity!!.getString(R.string.ui_gps_approx))
                else !activity!!.geo.label().contains(activity!!.getString(R.string.ui_gps_approx))
            )
                throw AssertionError("Precision permission not shown")
            activity!!.setLocationEnabled(false)
            if (activity!!.geo.snapshot() != null || activity!!.geo.active)
                throw AssertionError("OFF retained location or updates")
        })
    }

    @Throws(Exception::class)
    internal fun checkCompatibility() {
        val manager =
            getTargetContext().getSystemService(Context.CAMERA_SERVICE)
                as android.hardware.camera2.CameraManager
        for (id in manager.getCameraIdList()) {
            val catalog =
                CameraOptions(
                    id,
                    manager.getCameraCharacteristics(id),
                    activity!!.engine.maxTexture,
                )
            val defaults = CaptureSettings()
            if (catalog.photo(defaults) == null) throw AssertionError("No default JPEG: " + id)
            if ("recommended" != defaults.photoSize)
                throw AssertionError("Default JPEG is not recommended")
            defaults.photoSize = "max"
            for (advertised in
                catalog.map.getOutputSizes<android.graphics.SurfaceTexture>(
                    android.graphics.SurfaceTexture::class.java
                )) if (
                Math.max(advertised.getWidth(), advertised.getHeight()) <=
                    activity!!.engine.maxTexture &&
                    CameraOptions.area(advertised) >
                        CameraOptions.area(catalog.photo(defaults)!!.size)
            )
                throw AssertionError("Live photo catalog discards maximum stream: " + id)
            if (!catalog.raws.isEmpty()) {
                defaults.photoFormat = 2
                if (catalog.photo(defaults) != catalog.raws.get(0))
                    throw AssertionError("Default RAW is not maximum")
                defaults.photoFormat = 0
            }
            val supported = catalog.videosFor(defaults.codec)
            if (!supported.isEmpty()) {
                val recommended = catalog.video(defaults)
                val modest =
                    supported
                        .stream()
                        .anyMatch({ v ->
                            !v.highSpeed &&
                                v.fps <= 30 &&
                                CameraOptions.area(v.size) <= catalog.recommendedVideoPixels
                        })
                if (
                    modest &&
                        (recommended!!.highSpeed ||
                            recommended!!.fps > 30 ||
                            CameraOptions.area(recommended!!.size) > catalog.recommendedVideoPixels)
                )
                    throw AssertionError("Video recommendation exceeds budget")
            }
            val tiny = CameraOptions.Photo(android.util.Size(640, 480), false)
            catalog.photos.clear()
            catalog.photos.add(tiny)
            catalog.videos.clear()
            catalog.encoders.clear()
            if (catalog.photo(defaults) != tiny || catalog.video(defaults) != null)
                throw AssertionError("Low-resolution photo must not require video")
            if (catalog.previewFor(tiny) == null) throw AssertionError("No preview for VGA photo")
        }
        // Exercise the actual photo startup path when no video encoder is available.
        val live = activity!!.cameraOptions
        val videos = java.util.ArrayList<CameraOptions.Video>(live!!.videos)
        val before = activity!!.engine.generation
        activity!!
            .engine
            .gl
            .post({
                live!!.videos.clear()
                activity!!.engine.restart()
            })
        try {
            await(
                "photo preview without encoder",
                {
                    activity!!.engine.generation > before &&
                        activity!!.engine.frameSeen &&
                        activity!!.ready
                },
                20000,
            )
        } finally {
            activity!!.engine.gl.post({ live!!.videos.addAll(videos) })
        }
        val recovery = activity!!.engine.generation
        activity!!.engine.gl.post({ activity!!.engine.recoverSession() })
        await(
            "low-resolution session recovery",
            {
                activity!!.engine.generation > recovery &&
                    activity!!.engine.frameSeen &&
                    activity!!.ready
            },
            20000,
        )
        if (
            activity!!.engine.photoChoice!!.maximumPixelMode || activity!!.settings.photoFormat != 0
        )
            throw AssertionError("Recovery must use normal JPEG")
        for (i in 0..1) {
            val previous = activity!!.engine.generation
            activity!!.engine.switchCamera()
            await(
                "camera switch",
                {
                    activity!!.engine.generation > previous &&
                        activity!!.engine.frameSeen &&
                        activity!!.ready
                },
                20000,
            )
        }
    }

    @Throws(Exception::class)
    internal fun checkState() {
        val jpeg = CaptureSettings(activity!!.settings)
        jpeg.photoFormat = 0
        val previous = activity!!.engine.generation
        runOnMainSync({
            activity!!.videoMode = false
            activity!!.applySettings(jpeg)
        })
        await(
            "JPEG context",
            { activity!!.engine.generation > previous && activity!!.ready },
            20000,
        )
        runOnMainSync({
            activity!!.commitEffects(
                EffectState.defaults()
                    .chain((1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.ROW_ERROR))
                    .amount(.7f)
            )
            val base = activity!!.effectState
            val persisted =
                activity!!.getSharedPreferences("signal", 0).getString(EffectStateStore.KEY, "")
            var edit: MainActivity.EffectPreview = activity!!.beginEffectPreview()
            val draft = base.single(Effects.ROW_ERROR).amount(.2f)
            activity!!.previewEffectEdit(edit, draft)
            if (
                activity!!.effectState != base ||
                    persisted !=
                        activity!!
                            .getSharedPreferences("signal", 0)
                            .getString(EffectStateStore.KEY, "")
            )
                throw AssertionError("Preview leaked to committed state")
            activity!!.finishEffectEdit(edit, draft, false)
            if (activity!!.effectState != base) throw AssertionError("Cancel did not restore base")
            edit = activity!!.beginEffectPreview()
            activity!!.previewEffectEdit(edit, draft)
            activity!!.finishEffectEdit(edit, draft, true)
            if (
                activity!!.effectState.chained ||
                    activity!!.effectState.mask != (1 shl Effects.ROW_ERROR) ||
                    activity!!.effectState.amount != .2f
            )
                throw AssertionError("Apply was not atomic")
            edit = activity!!.beginEffectPreview()
            activity!!.chooseEffect(Effects.CLEAN)
            activity!!.finishEffectEdit(edit, base, true)
            if (activity!!.effectState.mask != 0 || activity!!.effectState.chained)
                throw AssertionError("Stale editor resurrected chain")
            activity!!.commitEffects(
                base.chain((1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.STREAM_ERROR))
            )
            if (
                activity!!.effectState.mask !=
                    ((1 shl Effects.PIXEL_DAMAGE) or (1 shl Effects.STREAM_ERROR))
            )
                throw AssertionError("Photo lost stream model")
            val before = activity!!.effectState.encode()
            activity!!.renderEffects()
            if (before != activity!!.effectState.encode())
                throw AssertionError("Redraw mutated effect state")
        })
    }

    internal fun checkNormalCapture() {
        val next = CaptureSettings(activity!!.settings)
        next.lightMode = false
        next.advancedMode = false
        next.photoFormat = 0
        next.photoSize = "recommended"
        next.rawVideo = false
        next.resolutionAudio = true
        val generation = activity!!.engine.generation
        runOnMainSync {
            activity!!.videoMode = false
            activity!!.applySettings(next)
            activity!!.commitEffects(EffectState.defaults().single(Effects.BIT_ERROR).amount(.8f))
        }
        await("normal preview", { activity!!.engine.generation > generation && activity!!.ready }, 20000)
        val e = activity!!.engine
        val before = activity!!.latest
        val latch = java.util.concurrent.CountDownLatch(1)
        var issue: Throwable? = null
        val saveGate = java.util.concurrent.CountDownLatch(1)
        e.files.execute { saveGate.await(15, java.util.concurrent.TimeUnit.SECONDS) }
        e.gl.post {
            try {
                check(e.presentedFrames.values().all { it.texture == 0 }) { "Normal mode retained history textures" }
                check(e.encoderScratch.texture != 0 && e.encoderScratch.frame != null)
                e.photo()
            } catch (failure: Throwable) { issue = failure }
            finally { latch.countDown() }
        }
        check(latch.await(5, java.util.concurrent.TimeUnit.SECONDS))
        issue?.let { throw AssertionError("Normal buffer policy", it) }
        try {
            await("normal snapshot before save", { e.photoBusy && e.pending == null }, 5000)
            runOnMainSync { activity!!.commitEffects(EffectState.defaults().single(Effects.CRT)) }
            await("later processed state", { e.encoderScratch.frame?.ids()?.contains(Effects.CRT) == true }, 5000)
        } finally {
            saveGate.countDown()
        }
        await("normal JPEG", { activity!!.latest != before && !e.photoBusy }, 30000)
        val saved = activity!!.latest!!
        getTargetContext().contentResolver.openInputStream(saved).use {
            val description = ExifInterface(requireNotNull(it)).getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)!!
            check(description.contains("Processed RGB capture") && description.contains("BIT ERROR")) { description }
        }
        getTargetContext().contentResolver.openInputStream(saved).use {
            val bitmap = BitmapFactory.decodeStream(it)!!
            check(bitmap.width > 0 && bitmap.height > 0)
            bitmap.recycle()
        }
        val restored = CaptureSettings.load(getTargetContext().getSharedPreferences("signal", 0))
        check(!restored.advancedMode && restored.resolutionAudio)
    }

    @Throws(Exception::class)
    internal fun checkRawEcho() {
        val a = activity!!
        check(a.cameraOptions!!.raws.isNotEmpty()) { "RAW camera required" }
        for (advanced in listOf(false, true)) for (format in listOf(0, 2, 0, 2)) {
            val revision = a.engine.generation
            runOnMainSync {
                a.videoMode = false
                a.commitEffects(EffectState.defaults().chain(-1).amount(.8f))
                a.applyFaultConfig(FaultConfig(true, true, false, true, true, true, .5f, 50))
                a.applySettings(CaptureSettings(a.settings).apply {
                    photoFormat = format
                    photoSize = "recommended"
                    advancedMode = advanced
                    experimentalSignals = advanced
                    expertMode = false
                })
            }
            await("all-chain format $format advanced $advanced", {
                a.engine.generation > revision && a.engine.frameSeen && a.ready
            }, 30000)
            SystemClock.sleep(750)
            check(a.settings.photoFormat == format)
        }
        runOnMainSync { check(a.photoTab.performLongClick()) }
        SystemClock.sleep(400)
        check(uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(
            a.getString(R.string.ui_photo_resolution)).isNotEmpty()) { "Photo long-press picker" }
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        val modeBefore = a.videoMode
        runOnMainSync { check(a.videoTab.performLongClick()) }
        SystemClock.sleep(400)
        check(uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(
            a.getString(if (a.settings.rawVideo) R.string.ui_raw_video_resolution else R.string.ui_video_resolution_fps)).isNotEmpty())
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        check(a.videoMode == modeBefore) { "Long press changed mode" }
        val revision = a.engine.generation
        runOnMainSync {
            a.applySettings(CaptureSettings(a.settings).apply { photoFormat = 0; advancedMode = false; experimentalSignals = true })
            a.applyFaultConfig(FaultConfig(true, false, false, false, false, false, .5f, 50,
                echo = EchoConfig(true, 1f)))
        }
        await("echo camera", { a.engine.generation > revision && a.ready }, 30000)
        SystemClock.sleep(3000)
        await("echo ready and idle", { a.engine.timeEcho.ready && !a.engine.timeEcho.replaying }, 8000)
        val before = a.engine.timeEcho.bursts
        runOnMainSync { a.echoButton.performClick() }
        await("manual echo", { a.engine.timeEcho.bursts > before && a.engine.timeEcho.replaying }, 5000)
        val evaluated = java.util.concurrent.CountDownLatch(1)
        var evaluationError: Throwable? = null
        a.engine.gl.post {
            try {
                val shown = a.engine.encoderScratch.frame!!
                check(a.engine.lastFrameNs - shown.cameraNs >= 400_000_000L) { "Expected past camera image" }
                check(kotlin.math.abs(shown.time - a.engine.faultFrame(a.effectState).time) < .1) { "FAULT time rewound" }
            } catch (error: Throwable) { evaluationError = error }
            finally { evaluated.countDown() }
        }
        check(evaluated.await(5, java.util.concurrent.TimeUnit.SECONDS))
        evaluationError?.let { throw AssertionError("Echo temporal contract", it) }
        val previousPhoto = a.latest
        a.engine.photo(a.engine.previewAcknowledged())
        await("echo JPEG save", { a.latest != previousPhoto && !a.engine.photoBusy }, 30000)
        languageScreenshot("time-echo-camera")
        await("echo ends", { !a.engine.timeEcho.replaying }, 5000)
        await("automatic echo", { a.engine.timeEcho.bursts > before + 1 }, 16000)
        runOnMainSync { FaultDialog.show(a) }
        SystemClock.sleep(500)
        languageScreenshot("time-echo-editor")
        runOnMainSync { a.liveEditor!!.echo = EchoConfig(false, 0f); a.liveEditor!!.preview() }
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        check(a.faultConfig.echo == EchoConfig(true, 1f)) { "Echo cancel mutated committed state" }
        val rawRevision = a.engine.generation
        runOnMainSync { a.applySettings(CaptureSettings(a.settings).apply { photoFormat = 2 }) }
        await("echo RAW bypass", { a.engine.generation > rawRevision && a.ready }, 30000)
        check(!a.engine.timeEcho.replaying && a.echoButton.visibility == View.GONE)
        val beforeRaw = a.latest
        a.engine.photo(a.engine.previewAcknowledged())
        await("full chain RAW save", { a.latest != beforeRaw && !a.engine.photoBusy }, 30000)
        val videoRevision = a.engine.generation
        runOnMainSync {
            a.videoMode = true
            a.applySettings(CaptureSettings(a.settings).apply { rawVideo = false; videoKey = "recommended" })
        }
        await("echo video ready", { a.engine.generation > videoRevision && a.ready }, 30000)
        SystemClock.sleep(2500)
        a.engine.toggleVideo(false)
        await("echo video recording", { a.engine.recording }, 15000)
        val videoBursts = a.engine.timeEcho.bursts
        runOnMainSync { a.echoButton.performClick() }
        await("echo during MP4", { a.engine.timeEcho.bursts > videoBursts && a.engine.timeEcho.replaying }, 5000)
        SystemClock.sleep(2300)
        val beforeVideo = a.latest
        a.engine.toggleVideo(false)
        await("echo MP4 saved", { !a.engine.recording && a.latest != beforeVideo }, 30000)
        checkCaptureContract(true, true)
    }

    internal fun checkCaptureContract(experimental: Boolean = false, echo: Boolean = false) {
        val settings = CaptureSettings(activity!!.settings)
        settings.advancedMode = true
        settings.photoFormat = 0
        settings.rawVideo = false
        settings.photoSize = if (experimental) "recommended" else "auto"
        settings.experimentalSignals = experimental
        settings.jpegQuality = 100
        val generation = activity!!.engine.generation
        runOnMainSync({
            activity!!.videoMode = false
            activity!!.applySettings(settings)
            var captured = EffectState.defaults()
                .chain((1 shl Effects.ROW_ERROR) or (1 shl Effects.VHS) or (1 shl Effects.CRT))
                .amount(.8f)
            if (experimental) captured = captured.edit(true,
                captured.mask or (1 shl Effects.MOTION_BLUR) or (1 shl Effects.THERMAL_NOISE) or (1 shl Effects.SMEAR),
                captured.parameters().override(Effects.MOTION_BLUR, "blurX", .04f)
                    .override(Effects.THERMAL_NOISE, "noiseAmplitude", .12f)
                    .override(Effects.SMEAR, "smearAmount", 1f))
            activity!!.commitEffects(captured)
        })
        await(
            "live signal",
            {
                activity!!.engine.generation > generation &&
                    activity!!.ready &&
                    activity!!.engine.previewAcknowledged() > 0
            },
            20000,
        )
        SystemClock.sleep(500)
        if (echo) {
            await("echo history for pinned capture", { activity!!.engine.timeEcho.ready }, 8000)
            activity!!.engine.triggerEcho()
            await("echo pinned capture", { activity!!.engine.timeEcho.replaying }, 5000)
            SystemClock.sleep(150)
        }
        val displayed = activity!!.engine.presentedFrames.reserve()
        if (displayed == null) throw AssertionError("No acknowledged image")
        val capturedCameraNs = displayed!!.value.frame!!.cameraNs
        if (echo) check(activity!!.engine.lastFrameNs - capturedCameraNs >= 400_000_000L)
        val reference = arrayOfNulls<Bitmap>(1)
        val problem = arrayOfNulls<Throwable>(1)
        val read = java.util.concurrent.CountDownLatch(1)
        activity!!
            .engine
            .gl
            .post({
                try {
                    activity!!.engine.current(activity!!.engine.window)
                    reference[0] = displayed!!.value.read()
                } catch (failure: Throwable) {
                    problem[0] = failure
                } finally {
                    read.countDown()
                }
            })
        if (!read.await(5, java.util.concurrent.TimeUnit.SECONDS) || problem[0] != null)
            throw AssertionError("Reference readback", problem[0])
        // Simulate delayed GL shutter handling while new camera states arrive. The old image is
        // pinned.
        await(
            "later camera signal",
            { activity!!.engine.lastFrameNs > displayed!!.value.frame!!.cameraNs + 150_000_000L },
            5000,
        )
        val before = activity!!.latest
        activity!!.engine.photo(displayed!!.timestamp)
        activity!!.engine.presentedFrames.release(displayed)
        runOnMainSync({
            activity!!.commitEffects(EffectState.defaults().single(Effects.COLOR_MAP))
        })
        await(
            "latched JPEG",
            {
                activity!!.latest != null &&
                    activity!!.latest != before &&
                    !activity!!.engine.photoBusy
            },
            30000,
        )
        val actual: Bitmap?
        getTargetContext()
            .getContentResolver()
            .openInputStream(requireNotNull(activity!!.latest))
            .use({ `in` -> actual = BitmapFactory.decodeStream(`in`) })
        val encoded = ByteArrayOutputStream()
        reference[0]!!.compress(Bitmap.CompressFormat.JPEG, 100, encoded)
        val expected = BitmapFactory.decodeByteArray(encoded.toByteArray(), 0, encoded.size())
        reference[0]!!.recycle()
        if (actual == null || !actual!!.sameAs(expected))
            throw AssertionError("Saved JPEG is not the pinned displayed signal")
        actual!!.recycle()
        expected.recycle()
        getTargetContext()
            .getContentResolver()
            .openInputStream(requireNotNull(activity!!.latest))
            .use({ `in` ->
                val description =
                    ExifInterface(requireNotNull(`in`))
                        .getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                if (
                    description == null ||
                        !description!!.contains("cameraNs=" + capturedCameraNs) ||
                        !description!!.contains("VHS") ||
                        (experimental && !description.contains("experimental=true"))
                )
                    throw AssertionError("Capture lost timestamp/state metadata")
            })
    }

    @Throws(Exception::class)
    internal fun checkNewEffects() {
        val prefs = getTargetContext().getSharedPreferences("effect-schema-check", 0)
        prefs
            .edit()
            .clear()
            .putString("effect_state_v2", "2|0|256|0.73")
            .putInt("effect", 8)
            .commit()
        if (EffectStateStore.load(prefs).mask != 0) throw AssertionError("Old IDs reinterpreted")
        val editor = prefs.edit()
        EffectStateStore.write(editor, EffectState.defaults().single(Effects.DEMOSAIC_ERROR))
        editor.commit()
        if (
            EffectStateStore.load(prefs).selected() != Effects.DEMOSAIC_ERROR ||
                prefs.contains("effect_state_v2")
        )
            throw AssertionError("New schema migration")
        prefs.edit().clear().commit()
    }

    internal fun fixture(): ByteArray {
        val input = Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)
        for (y in 0..255) for (x in 0..191) input.setPixel(
            x,
            y,
            Color.rgb((x * 13 + y * 3) % 256, y, ((x / 7 + y / 9) % 2) * 255),
        )
        val bytes = ByteArrayOutputStream()
        input.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        input.recycle()
        return bytes.toByteArray()
    }

    internal fun evaluated(state: EffectState, time: Double): EffectState.Frame {
        val model = FaultModel(5)
        val input = FaultModel.Inputs()
        model.advance(0.0, input, FaultConfig.defaults())
        input.sensorNs = (time * 1e9).toLong()
        model.advance(time, input, FaultConfig.defaults())
        return model.apply(state.snapshot(true, 0), FaultConfig.defaults())
    }

    @Throws(Exception::class)
    internal fun checkGpu() {
        val jpeg = fixture()
        val clean =
            PhotoRenderer.render(
                getTargetContext(),
                jpeg,
                false,
                evaluated(EffectState.defaults(), 0.0),
            )
        val zero =
            PhotoRenderer.render(
                getTargetContext(),
                jpeg,
                false,
                evaluated(EffectState.defaults().chain(-1).amount(0f), 4.0),
            )
        if (!zero!!.sameAs(clean)) throw AssertionError("LEVEL zero bypass")
        zero!!.recycle()
        if (Color.green(clean!!.getPixel(80, 10)) >= Color.green(clean!!.getPixel(80, 240)))
            throw AssertionError("GPU orientation")
        val dir = File(getTargetContext().getFilesDir(), "verification")
        dir.mkdirs()
        for (id in Effects.ORDER) {
            if (id == 0) continue
            val selected = EffectState.defaults().single(id).amount(1f)
            var frame: EffectState.Frame? = null
            for (i in 1..199) {
                frame = evaluated(selected, i * .05)
                if (id != Effects.STREAM_ERROR || frame!!.nodes.get(0).event.envelope > .5) break
            }
            val image = PhotoRenderer.render(getTargetContext(), jpeg, false, requireNotNull(frame))
            val again = PhotoRenderer.render(getTargetContext(), jpeg, false, requireNotNull(frame))
            if (!image!!.sameAs(again)) throw AssertionError("Snapshot replay: " + Effects.name(id))
            if (image!!.sameAs(clean))
                throw AssertionError("Fault has no visible mechanism: " + Effects.name(id))
            FileOutputStream(File(dir, "fault-" + id + ".png"))
                .use({ out -> image!!.compress(Bitmap.CompressFormat.PNG, 100, out) })
            image!!.recycle()
            again!!.recycle()
            var incidentTime = .08
            for (step in 1..1200) {
                val probe = evaluated(selected, step * .05).nodes.get(0)
                if (probe.event.envelope > .5 && probe.event.position > .03) {
                    incidentTime = step * .05
                    break
                }
                if (probe.event.serial < 0) break
            }
            for (control in Effects.CONTROLS[id]) {
                var responds = false
                for (time in doubleArrayOf(incidentTime, .08, 1.18, 2.78, 5.48, 8.18)) {
                    val p = selected.parameters()
                    val low =
                        PhotoRenderer.render(
                            getTargetContext(),
                            jpeg,
                            false,
                            evaluated(
                                selected.edit(false, selected.mask, p.with(id, control.key, 0f)),
                                time,
                            ),
                        )
                    val high =
                        PhotoRenderer.render(
                            getTargetContext(),
                            jpeg,
                            false,
                            evaluated(
                                selected.edit(false, selected.mask, p.with(id, control.key, 1f)),
                                time,
                            ),
                        )
                    responds = !low!!.sameAs(high)
                    low!!.recycle()
                    high!!.recycle()
                    if (responds) break
                }
                if (!responds)
                    throw AssertionError(
                        "Control has no visible effect: " + Effects.name(id) + " / " + control.key
                    )
            }
        }
        val chain =
            EffectState.defaults()
                .chain(
                    (1 shl Effects.EXPOSURE) or (1 shl Effects.CHROMA_ERROR) or (1 shl Effects.CRT)
                )
        val composed = PhotoRenderer.render(getTargetContext(), jpeg, false, evaluated(chain, 3.0))
        val last =
            PhotoRenderer.render(
                getTargetContext(),
                jpeg,
                false,
                evaluated(chain.single(Effects.CRT), 3.0),
            )
        if (composed!!.sameAs(last) || composed!!.sameAs(clean))
            throw AssertionError("Causal chain composition")
        composed!!.recycle()
        last!!.recycle()
        clean!!.recycle()
    }

    internal fun findText(root: android.view.View, text: String): android.view.View? {
        if (
            root is android.widget.TextView &&
                text.contentEquals((root as android.widget.TextView).getText())
        )
            return root
        if (root is android.view.ViewGroup) {
            val group = root as android.view.ViewGroup
            for (i in 0 until group.getChildCount()) {
                val found = findText(group.getChildAt(i), text)
                if (found != null) return found
            }
        }
        return null
    }

    internal fun presented(): EffectState.Frame? {
        if (!activity!!.engine.settings.advancedMode) return activity!!.engine.encoderScratch.frame
        val lease = activity!!.engine.presentedFrames.reserve()
        if (lease == null) return null
        try {
            return lease!!.value.frame
        } finally {
            activity!!.engine.presentedFrames.release(lease)
        }
    }

    @Throws(Exception::class)
    internal fun checkExpert(result: Bundle) {
        runOnMainSync({
            val next = CaptureSettings(activity!!.settings)
            next.photoFormat = 0
            next.photoSize = "recommended"
            next.rawVideo = false
            next.expertMode = false
            activity!!.videoMode = false
            activity!!.applySettings(next)
            activity!!.applyFaultConfig(FaultConfig.defaults())
            activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
        })
        await(
            "normal before expert",
            { activity!!.ready && !activity!!.settings.expertMode },
            20000,
        )
        val panel = arrayOfNulls<QualityDialog>(1)
        runOnMainSync({
            panel[0] = QualityDialog(requireNotNull(activity))
            panel[0]!!.show()
            panel[0]!!.content!!.findViewWithTag<View>("expert-mode").performClick()
            if (!activity!!.settings.expertMode) throw AssertionError("Expert not immediately saved")
        })
        waitForIdleSync()
        SystemClock.sleep(400)
        saveUi("expert-settings-top.png")
        runOnMainSync({
            (panel[0]!!.content!!.getParent() as android.widget.ScrollView).fullScroll(
                android.view.View.FOCUS_DOWN
            )
        })
        waitForIdleSync()
        SystemClock.sleep(900)
        saveUi("expert-settings-bottom.png")
        runOnMainSync({ panel[0]!!.sheet!!.dismiss() })
        if (!activity!!.settings.expertMode) throw AssertionError("Expert lost on close")
        runOnMainSync({
            panel[0] = QualityDialog(activity!!)
            panel[0]!!.show()
            if (!panel[0]!!.draft.expertMode) throw AssertionError("Expert lost on reopen")
            panel[0]!!.sheet!!.dismiss()
        })
        await(
            "expert enabled",
            {
                activity!!.ready &&
                    activity!!.settings.expertMode &&
                    activity!!.engine.adaptiveLoad.expert
            },
            20000,
        )
        if (!CaptureSettings.load(activity!!.getSharedPreferences("signal", 0)).expertMode)
            throw AssertionError("Expert not persisted")
        val engine = activity!!.engine
        val route = activity!!.effectState.encode()
        try {
            glSync({
                engine.gl.removeCallbacks(engine.thermalPoll)
                engine.adaptiveLoad.renderMillis = 500.0
                engine.applyLoadSample(1000, 6, 60f, 2f)
                if (
                    engine.cooling ||
                        !engine.adaptiveLoad.expert ||
                        engine.previewFps != engine.expertCameraFps()
                )
                    throw AssertionError("Expert still capped")
            })
            val before = engine.renderedFrames
            SystemClock.sleep(2000)
            val rendered = engine.renderedFrames - before
            if (
                rendered < 5 ||
                    engine.cooling ||
                    !activity!!.ready ||
                    route != activity!!.effectState.encode()
            )
                throw AssertionError("Expert stopped or changed faults")
            saveUi("expert-preview.png")
            runOnMainSync({
                val next = CaptureSettings(activity!!.settings)
                next.videoKey = "recommended"
                next.videoQuality = 1
                activity!!.videoMode = true
                activity!!.applySettings(next)
            })
            await("expert video ready", { activity!!.ready && activity!!.videoMode }, 20000)
            val previous = activity!!.latest
            engine.toggleVideo(false)
            await("expert recording", { engine.recording }, 15000)
            SystemClock.sleep(1200)
            glSync({ engine.applyLoadSample(2000, 6, 60f, 2f) })
            SystemClock.sleep(1200)
            if (!engine.recording || engine.cooling)
                throw AssertionError("Expert thermal stop still active")
            engine.toggleVideo(false)
            await(
                "expert recording saved",
                { !engine.recording && activity!!.latest != null && activity!!.latest != previous },
                15000,
            )
            runOnMainSync({
                val next = CaptureSettings(activity!!.settings)
                next.expertMode = false
                activity!!.applySettings(next)
            })
            await("expert disabled", { activity!!.ready && !engine.adaptiveLoad.expert }, 20000)
            glSync({
                engine.applyLoadSample(3000, 4, 30f, java.lang.Float.NaN)
                if (!engine.cooling || engine.previewFps != 6)
                    throw AssertionError("Normal protection not restored")
            })
            result.putString(
                "result",
                "PASS expert UI immediate persistence, uncapped preview " +
                    rendered +
                    " frames/2s, simulated critical heat ignored during recording, manual stop saved, normal thermal protection restored",
            )
        } finally {
            glSync({
                if (engine.recording) engine.stopVideo()
                engine.adaptiveLoad.cooling = false
                engine.cooling = false
                engine.adaptiveLoad.renderMillis = 0.0
                engine.updateRequest()
                engine.gl.post(Runnable({ engine.frame() }))
                engine.gl.removeCallbacks(engine.thermalPoll)
                engine.gl.post(engine.thermalPoll)
            })
        }
    }

    @Throws(Exception::class)
    internal fun checkLoadRecording(result: Bundle) {
        runOnMainSync({
            val next = CaptureSettings(activity!!.settings)
            next.rawVideo = false
            next.videoKey = "recommended"
            next.videoQuality = 1
            activity!!.videoMode = true
            activity!!.applySettings(next)
            activity!!.applyFaultConfig(FaultConfig.defaults())
            activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
        })
        await("recommended video", { activity!!.ready && activity!!.videoMode }, 20000)
        val engine = activity!!.engine
        val width = engine.outW
        val height = engine.outH
        val before = activity!!.latest
        glSync({ engine.gl.removeCallbacks(engine.thermalPoll) })
        try {
            engine.toggleVideo(false)
            await("recording for thermal stop", { engine.recording }, 15000)
            SystemClock.sleep(2200)
            glSync({ engine.applyLoadSample(1000, 4, 30f, java.lang.Float.NaN) })
            await(
                "thermal stop saved",
                { !engine.recording && activity!!.latest != null && activity!!.latest != before },
                15000,
            )
            if (!engine.cooling || engine.outW != width || engine.outH != height)
                throw AssertionError("Thermal stop changed resolution or failed to pause")
            activity!!
                .getContentResolver()
                .query(
                    requireNotNull(activity!!.latest),
                    arrayOf<String>(MediaStore.MediaColumns.RELATIVE_PATH),
                    null,
                    null,
                    null,
                )
                .use({ c ->
                    if (c == null || !c!!.moveToFirst() || "DCIM/5igna1/" != c!!.getString(0))
                        throw AssertionError("Thermal stop output not saved to camera folder")
                })
            glSync({
                engine.applyLoadSample(2000, 0, 30f, java.lang.Float.NaN)
                engine.applyLoadSample(32000, 0, 30f, java.lang.Float.NaN)
            })
            await("video preview after cooling", { !engine.cooling && activity!!.ready }, 8000)
            if (engine.recording) throw AssertionError("Recording restarted automatically")
            result.putString(
                "result",
                "PASS recommended video " +
                    width.toString() +
                    "x" +
                    height +
                    ", critical-heat stop saved MP4 and preview resumed without restarting recording",
            )
        } finally {
            glSync({
                if (engine.recording) engine.stopVideo()
                engine.adaptiveLoad.cooling = false
                engine.cooling = false
                engine.adaptiveLoad.relaxedSince = -1
                engine.updateRequest()
                engine.gl.post(Runnable({ engine.frame() }))
                engine.gl.removeCallbacks(engine.thermalPoll)
                engine.gl.post(engine.thermalPoll)
            })
        }
    }

    @Throws(Exception::class)
    internal fun checkAdaptiveLoad(result: Bundle) {
        val panel = arrayOfNulls<QualityDialog>(1)
        val oldSize = activity!!.settings.photoSize
        runOnMainSync({
            panel[0] = QualityDialog(activity!!)
            panel[0]!!.show()
            panel[0]!!.content!!.findViewWithTag<View>("load-recommend").performClick()
            if (
                panel[0]!!.draft.photoSize != "recommended" ||
                    panel[0]!!.draft.videoKey != "recommended"
            )
                throw AssertionError("Recommendation button failed")
        })
        waitForIdleSync()
        SystemClock.sleep(400)
        saveUi("adaptive-settings.png")
        runOnMainSync({ panel[0]!!.sheet!!.dismiss() })
        if (activity!!.settings.photoSize != "recommended" || activity!!.settings.expertMode)
            throw AssertionError("Recommendation not saved on selection")
        val migration = activity!!.getSharedPreferences("loadMigrationTest", 0)
        try {
            migration.edit().clear().putString("photoSize", "max").commit()
            if (CaptureSettings.load(migration).photoSize != "recommended")
                throw AssertionError("Legacy maximum not migrated")
            val explicit = CaptureSettings.load(migration)
            explicit.photoSize = "max"
            explicit.save(migration)
            if (CaptureSettings.load(migration).photoSize != "max")
                throw AssertionError("Explicit maximum not retained")
            migration.edit().clear().putString("photoSize", "1920x1080").commit()
            if (CaptureSettings.load(migration).photoSize != "1920x1080")
                throw AssertionError("Explicit size changed")
        } finally {
            migration.edit().clear().commit()
        }
        runOnMainSync({
            activity!!.videoMode = false
            val next = CaptureSettings(activity!!.settings)
            next.photoFormat = 0
            next.photoSize = "recommended"
            next.rawVideo = false
            activity!!.applySettings(next)
            activity!!.applyFaultConfig(FaultConfig.defaults())
            activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
        })
        await(
            "recommended camera",
            { activity!!.ready && activity!!.settings.photoSize == "recommended" },
            20000,
        )
        val engine = activity!!.engine
        val budget = engine.deviceProfile.photoPixels()
        val modest =
            activity!!
                .cameraOptions!!
                .photos
                .stream()
                .anyMatch({ p -> CameraOptions.area(p.size) <= budget })
        if (modest && engine.signalW.toLong() * engine.signalH > budget)
            throw AssertionError("Recommendation exceeds pixel budget")
        val effects = activity!!.effectState.encode()
        val width = engine.outW
        val height = engine.outH
        glSync({
            engine.gl.removeCallbacks(engine.thermalPoll)
            engine.adaptiveLoad.renderMillis = 0.0
            engine.adaptiveLoad.previewFps = 24
            engine.applyLoadSample(0, 0, 30f, java.lang.Float.NaN)
        })
        SystemClock.sleep(400)
        val normalStart = engine.renderedFrames
        SystemClock.sleep(2000)
        val normal = engine.renderedFrames - normalStart
        try {
            val transitionStart = engine.renderedFrames
            glSync({ engine.applyLoadSample(1000, 3, 30f, java.lang.Float.NaN) })
            await(
                "lower-rate camera resumed",
                { engine.renderedFrames >= transitionStart + 2 },
                10000,
            )
            SystemClock.sleep(400)
            val warmStart = engine.renderedFrames
            val warmCamera = engine.lastFrameNs
            SystemClock.sleep(2000)
            val warm = engine.renderedFrames - warmStart
            if (engine.previewFps != 6 || warm > 15 || warm < 5 || warm >= normal) {
                saveUi("adaptive-timeout.png")
                throw AssertionError(
                    "Adaptive render count normal=" +
                        normal +
                        " warm=" +
                        warm +
                        " fps=" +
                        engine.previewFps +
                        " cameraDelta=" +
                        (engine.lastFrameNs - warmCamera) +
                        " nextDelta=" +
                        (engine.adaptiveLoad.nextPreviewNs - engine.lastFrameNs) +
                        " cooling=" +
                        engine.cooling +
                        " range=" +
                        engine.request!!.get<Range<Int>>(
                            android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
                        ) +
                        " ack=" +
                        engine.presentedFrames.acknowledged() +
                        " surface=" +
                        activity!!.preview!!.getSurfaceTexture()!!.getTimestamp()
                )
            }
            if (
                engine.outW != width ||
                    engine.outH != height ||
                    effects != activity!!.effectState.encode() ||
                    activity!!.faultConfig.enabled
            )
                throw AssertionError("Load control changed capture/effect state")
            saveUi("adaptive-load.png")
            glSync({ engine.applyLoadSample(2000, 4, 30f, java.lang.Float.NaN) })
            await("cooling pause", { engine.cooling && !activity!!.ready }, 5000)
            val paused = engine.renderedFrames
            SystemClock.sleep(500)
            if (engine.renderedFrames != paused) throw AssertionError("GPU renders while cooling")
            saveUi("adaptive-cooling.png")
            glSync({
                engine.applyLoadSample(3000, 0, 30f, java.lang.Float.NaN)
                if (!engine.cooling) throw AssertionError("Cooling resumed too soon")
                engine.applyLoadSample(33000, 0, 30f, java.lang.Float.NaN)
            })
            await(
                "cooling recovery",
                { !engine.cooling && activity!!.ready && engine.renderedFrames > paused },
                8000,
            )
            result.putString(
                "result",
                "PASS recommendations, migration, adaptive render count " +
                    normal +
                    " -> " +
                    warm +
                    " per 2s, cooling pause/recovery, effects and capture size retained with LIVE off",
            )
            result.putString("recommended", width.toString() + "x" + height)
        } finally {
            glSync({
                engine.adaptiveLoad.cooling = false
                engine.cooling = false
                engine.adaptiveLoad.relaxedSince = -1
                engine.updateRequest()
                engine.gl.post(Runnable({ engine.frame() }))
                engine.gl.removeCallbacks(engine.thermalPoll)
                engine.gl.post(engine.thermalPoll)
            })
        }
    }

    @Throws(Exception::class)
    internal fun glSync(action: Runnable) {
        val done = java.util.concurrent.CountDownLatch(1)
        val error = arrayOfNulls<Throwable>(1)
        activity!!
            .engine
            .gl
            .post({
                try {
                    action.run()
                } catch (failure: Throwable) {
                    error[0] = failure
                } finally {
                    done.countDown()
                }
            })
        if (!done.await(10, java.util.concurrent.TimeUnit.SECONDS))
            throw AssertionError("GL check timeout")
        if (error[0] != null) throw AssertionError("GL check", error[0])
    }

    @Throws(Exception::class)
    internal fun checkAdvanced() {
        val advancedBefore = activity!!.advancedMode
        val dialog = arrayOfNulls<EffectDialog>(1)
        runOnMainSync({
            activity!!.videoMode = false
            val settings = CaptureSettings(activity!!.settings)
            settings.photoFormat = 0
            settings.photoSize = "auto"
            activity!!.applySettings(settings)
            activity!!.applyFaultConfig(FaultConfig.defaults())
            activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
            activity!!.advancedMode = false
        })
        await(
            "advanced camera",
            { activity!!.ready && activity!!.settings.photoFormat == 0 },
            20000,
        )
        val original = activity!!.effectState
        runOnMainSync({
            activity!!
                .preview!!
                .setSurfaceTextureListener(
                    object : android.view.TextureView.SurfaceTextureListener {
                        public override fun onSurfaceTextureAvailable(
                            t: android.graphics.SurfaceTexture,
                            w: Int,
                            h: Int,
                        ) {
                            activity!!.onSurfaceTextureAvailable(t, w, h)
                        }

                        public override fun onSurfaceTextureSizeChanged(
                            t: android.graphics.SurfaceTexture,
                            w: Int,
                            h: Int,
                        ) {
                            activity!!.onSurfaceTextureSizeChanged(t, w, h)
                        }

                        public override fun onSurfaceTextureDestroyed(
                            t: android.graphics.SurfaceTexture
                        ): Boolean {
                            return activity!!.onSurfaceTextureDestroyed(t)
                        }

                        public override fun onSurfaceTextureUpdated(
                            t: android.graphics.SurfaceTexture
                        ) {
                            /* Simulate the device's missing presentation callback. */
                        }
                    }
                )
        })
        try {
            val settingsEditor = arrayOfNulls<QualityDialog>(1)
            runOnMainSync({
                settingsEditor[0] = QualityDialog(activity!!)
                settingsEditor[0]!!.show()
                settingsEditor[0]!!.content!!.findViewWithTag<View>("advanced-mode").performClick()
                if (!activity!!.advancedMode)
                    throw AssertionError("Global mode not saved immediately")
            })
            waitForIdleSync()
            SystemClock.sleep(300)
            saveUi("advanced-global-setting.png")
            runOnMainSync({ settingsEditor[0]!!.sheet!!.dismiss() })
            if (!activity!!.advancedMode) throw AssertionError("Global mode lost on close")
            runOnMainSync({
                settingsEditor[0] = QualityDialog(activity!!)
                settingsEditor[0]!!.show()
                if (!settingsEditor[0]!!.advanced) throw AssertionError("Global mode lost on reopen")
                settingsEditor[0]!!.sheet!!.dismiss()
            })
            await("global mode applied", { activity!!.ready && activity!!.advancedMode }, 20000)
            if (!activity!!.getSharedPreferences("signal", 0).getBoolean("advancedMode", false))
                throw AssertionError("Global mode not persisted")
            runOnMainSync({
                dialog[0] = EffectDialog(activity!!, true)
                dialog[0]!!.show()
                if (dialog[0]!!.body!!.findViewWithTag<View>("advanced-mode") != null)
                    throw AssertionError("Per-fault mode switch remains")
            })
            waitForIdleSync()
            runOnMainSync({
                dialog[0]!!.body!!.findViewWithTag<View>("value-pixelDensity").performClick()
                val input =
                    dialog[0]!!
                        .auxiliary!!
                        .findViewById<View>(android.R.id.content)
                        .findViewWithTag<android.widget.EditText>("number-input")
                input.setText("0.25")
                dialog[0]!!
                    .auxiliary!!
                    .findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("number-apply")
                    .performClick()
            })
            try {
                await(
                    "advanced preview value",
                    {
                        val frame = presented()
                        frame != null &&
                            !frame!!.nodes.isEmpty() &&
                            frame!!.nodes.get(0).get("pixelDensity") == .25f
                    },
                    5000,
                )
            } catch (error: AssertionError) {
                saveUi("advanced-timeout.png")
                val frame = presented()
                throw AssertionError(
                    "Advanced preview: surface=" +
                        activity!!.preview!!.getSurfaceTexture()!!.getTimestamp() +
                        " ack=" +
                        activity!!.engine.previewAcknowledged() +
                        " camera=" +
                        activity!!.engine.lastFrameNs +
                        " ready=" +
                        activity!!.ready +
                        " valid=" +
                        activity!!.validPreview(dialog[0]!!.edit) +
                        " draft=" +
                        dialog[0]!!.draft.resolved(Effects.PIXEL_DAMAGE, "pixelDensity", -1f) +
                        " frame=" +
                        (if (frame == null) "null" else frame!!.nodes.toString()) +
                        " density=" +
                        (if (frame == null || frame!!.nodes.isEmpty()) -1
                        else frame!!.nodes.get(0).get("pixelDensity")),
                    error,
                )
            }

            if (activity!!.effectState != original)
                throw AssertionError("Advanced value committed before Apply")
            waitForIdleSync()
            SystemClock.sleep(300)
            saveUi("advanced-signal.png")
            runOnMainSync({
                val camera = IntArray(2)
                val panel = IntArray(2)
                activity!!.preview!!.getLocationOnScreen(camera)
                dialog[0]!!.sheet!!.getWindow()!!.getDecorView().getLocationOnScreen(panel)
                if (
                    camera[1] + activity!!.preview!!.getHeight() > panel[1] ||
                        activity!!.preview!!.getHeight() < activity!!.dp(140f)
                )
                    throw AssertionError("Advanced controls obscure preview")
                dialog[0]!!.sheet!!.dismiss()
            })
            waitForIdleSync()
            if (activity!!.effectState != original)
                throw AssertionError("Advanced cancel lost original")
            runOnMainSync({
                dialog[0] = EffectDialog(activity!!, true)
                dialog[0]!!.show()
                dialog[0]!!.body!!.findViewWithTag<View>("value-pixelDensity").performClick()
                val input =
                    dialog[0]!!
                        .auxiliary!!
                        .findViewById<View>(android.R.id.content)
                        .findViewWithTag<android.widget.EditText>("number-input")
                input.setText("-1")
                dialog[0]!!
                    .auxiliary!!
                    .findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("number-apply")
                    .performClick()
                if (
                    !dialog[0]!!.auxiliary!!.isShowing() ||
                        !dialog[0]!!.draft.overrides(Effects.PIXEL_DAMAGE).isEmpty()
                )
                    throw AssertionError("Invalid number applied")
                input.setText("0.125")
                dialog[0]!!
                    .auxiliary!!
                    .findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("number-apply")
                    .performClick()
                dialog[0]!!
                    .sheet!!
                    .getWindow()!!
                    .getDecorView()
                    .findViewWithTag<View>("apply")
                    .performClick()
            })
            waitForIdleSync()
            if (
                EffectStateStore.load(activity!!.getSharedPreferences("signal", 0))
                    .parameters()
                    .resolved(Effects.PIXEL_DAMAGE, "pixelDensity", 0f) != .125f
            )
                throw AssertionError("Override not persisted")
            runOnMainSync({
                activity!!.commitEffects(EffectState.defaults().single(Effects.VHS))
                dialog[0] = EffectDialog(activity!!, true)
                dialog[0]!!.show()
                dialog[0]!!.body!!.findViewWithTag<View>("event-identity").performClick()
                val input =
                    dialog[0]!!
                        .auxiliary!!
                        .findViewById<View>(android.R.id.content)
                        .findViewWithTag<android.widget.EditText>("number-input")
                input.setText(java.lang.Long.toString(java.lang.Long.MIN_VALUE))
                dialog[0]!!
                    .auxiliary!!
                    .findViewById<View>(android.R.id.content)
                    .findViewWithTag<View>("number-apply")
                    .performClick()
                if (dialog[0]!!.draft.eventIdentity(Effects.VHS, 0) != java.lang.Long.MIN_VALUE)
                    throw AssertionError("Event seed exact input failed")
            })
            waitForIdleSync()
            SystemClock.sleep(500)
            saveUi("advanced-events.png")
            runOnMainSync({
                dialog[0]!!
                    .sheet!!
                    .getWindow()!!
                    .getDecorView()
                    .findViewWithTag<View>("apply")
                    .performClick()
            })
            waitForIdleSync()
            if (
                EffectStateStore.load(activity!!.getSharedPreferences("signal", 0))
                    .parameters()
                    .eventIdentity(Effects.VHS, 0) != java.lang.Long.MIN_VALUE
            )
                throw AssertionError("Event seed not persisted")
            val jpeg = fixture()
            val hot = EffectState.defaults().single(Effects.PIXEL_DAMAGE)
            val p =
                hot.parameters()
                    .override(Effects.PIXEL_DAMAGE, "pixelDensity", 1f)
                    .override(Effects.PIXEL_DAMAGE, "columnDensity", 0f)
                    .override(Effects.PIXEL_DAMAGE, "hotFraction", 1f)
                    .override(Effects.PIXEL_DAMAGE, "hotValue", 1f)
                    .override(Effects.PIXEL_DAMAGE, "sensorNoise", 0f)
            val white =
                PhotoRenderer.render(
                    getTargetContext(),
                    jpeg,
                    false,
                    evaluated(hot.edit(false, hot.mask, p), 0.0),
                )
            val pixels = IntArray(white!!.getWidth() * white!!.getHeight())
            white!!.getPixels(
                pixels,
                0,
                white!!.getWidth(),
                0,
                0,
                white!!.getWidth(),
                white!!.getHeight(),
            )
            white!!.recycle()
            for (pixel in pixels) if (
                Color.red(pixel) < 253 || Color.green(pixel) < 253 || Color.blue(pixel) < 253
            )
                throw AssertionError("Direct pixelDensity not bound to GPU")
            for (id in Effects.ORDER) if (id != 0)
                for (high in booleanArrayOf(false, true)) {
                    val state = EffectState.defaults().single(id)
                    var values = state.parameters()
                    for (spec in FaultParameters.all(id)) if (
                        spec.group == FaultParameters.Group.SIGNAL ||
                            spec.group == FaultParameters.Group.PROFILE
                    )
                        values = values.override(id, spec.key, if (high) spec.max else spec.min)
                    val result =
                        PhotoRenderer.render(
                            getTargetContext(),
                            jpeg,
                            false,
                            evaluated(state.edit(false, state.mask, values), .1),
                        )
                    result!!.recycle()
                }
        } finally {
            runOnMainSync({
                if (dialog[0] != null && dialog[0]!!.sheet!!.isShowing())
                    dialog[0]!!.sheet!!.dismiss()
                activity!!.preview!!.setSurfaceTextureListener(activity)
                activity!!.advancedMode = advancedBefore
                activity!!
                    .getSharedPreferences("signal", 0)
                    .edit()
                    .putBoolean("advancedMode", advancedBefore)
                    .apply()
            })
        }
    }

    @Throws(Exception::class)
    internal fun checkPerformance() {
        val base = FaultConfig(true, false, false, false, false, false, .5f, 50)
        val dialog = arrayOfNulls<Dialog>(1)
        runOnMainSync({
            activity!!.videoMode = false
            val settings = CaptureSettings(activity!!.settings)
            settings.photoFormat = 0
            settings.photoSize = "auto"
            activity!!.applySettings(settings)
            activity!!.applyFaultConfig(base)
            activity!!.commitEffects(
                EffectState.defaults().chain((1 shl Effects.VHS) or (1 shl Effects.CRT))
            )
        })
        await("performance camera", { activity!!.ready }, 20000)
        try {
            runOnMainSync({ dialog[0] = FaultDialog.show(requireNotNull(activity)) })
            await("performance preview fork", { activity!!.engine.previewFaults != null }, 5000)
            runOnMainSync({
                val editor = activity!!.liveEditor
                editor!!.body!!.findViewWithTag<View>("live-style").performClick()
                editor!!
                    .child!!
                    .getWindow()!!
                    .getDecorView()
                    .findViewWithTag<View>("choice-3")
                    .performClick()
                editor!!.performance =
                    editor!!
                        .performance
                        .with("speed", -2f)
                        .with("clock", LivePerformance.LOOP.toFloat())
                editor!!.preview()
                editor!!.render()
            })
            await(
                "performance preview setting",
                {
                    activity!!.engine.previewFaultConfig != null &&
                        activity!!.engine.previewFaultConfig!!.performance.speed == -2f
                },
                5000,
            )
            if (activity!!.faultConfig != base || activity!!.engine.faultConfig != base)
                throw AssertionError("LIVE preview committed config")
            runOnMainSync({ dialog[0]!!.dismiss() })
            await("performance discard", { activity!!.engine.previewFaults == null }, 5000)
            if (activity!!.engine.faultConfig != base)
                throw AssertionError("LIVE cancel lost config")
            runOnMainSync({
                dialog[0] = FaultDialog.show(activity!!)
                val editor = activity!!.liveEditor
                editor!!.performance =
                    editor!!
                        .performance
                        .with("style", LivePerformance.CASCADE.toFloat())
                        .with("clock", LivePerformance.PING_PONG.toFloat())
                        .with("period", 2.5f)
                        .with("interval", .2f)
                editor!!.preview()
                editor!!.render()
            })
            waitForIdleSync()
            SystemClock.sleep(400)
            runOnMainSync({
                val camera = IntArray(2)
                val panel = IntArray(2)
                activity!!.preview!!.getLocationOnScreen(camera)
                dialog[0]!!.getWindow()!!.getDecorView().getLocationOnScreen(panel)
                if (
                    camera[1] + activity!!.preview!!.getHeight() > panel[1] ||
                        activity!!.preview!!.getHeight() < activity!!.dp(140f)
                )
                    throw AssertionError("LIVE editor obscures preview")
            })
            saveUi("live-direction.png")
            runOnMainSync({
                findText(
                        dialog[0]!!.getWindow()!!.getDecorView(),
                        activity!!.getString(R.string.ui_apply),
                    )!!
                    .performClick()
            })
            await(
                "performance commit",
                {
                    activity!!.engine.previewFaults == null &&
                        activity!!.engine.faultConfig.performance.clock == LivePerformance.PING_PONG
                },
                5000,
            )
            if (
                FaultPreferences.load(activity!!.getSharedPreferences("signal", 0))
                    .performance
                    .style != LivePerformance.CASCADE ||
                    FaultPreferences.load(activity!!.getSharedPreferences("signal", 0))
                        .performance
                        .periodSeconds != 2.5f ||
                    FaultPreferences.load(activity!!.getSharedPreferences("signal", 0))
                        .performance
                        .stepSeconds != .2f
            )
                throw AssertionError("Performance not saved")
            waitForIdleSync()
            SystemClock.sleep(300)
            saveUi("live-time-buttons.png")
            runOnMainSync({ activity!!.liveHold!!.performClick() })
            val barrier = java.util.concurrent.CountDownLatch(1)
            val beforeHold = longArrayOf(0)
            val expectedTime = doubleArrayOf(0.0)
            activity!!
                .engine
                .gl
                .post({
                    beforeHold[0] = activity!!.engine.lastFrameNs
                    expectedTime[0] = activity!!.engine.faults.time(activity!!.engine.faultConfig)
                    barrier.countDown()
                })
            if (!barrier.await(5, java.util.concurrent.TimeUnit.SECONDS))
                throw AssertionError("HOLD queue timeout")
            await(
                "first held frame",
                {
                    val frame = presented()
                    frame != null && frame!!.cameraNs > beforeHold[0]
                },
                5000,
            )
            val held = presented()
            await(
                "camera continues during HOLD",
                {
                    val frame = presented()
                    frame != null && frame!!.cameraNs > held!!.cameraNs
                },
                5000,
            )
            val later = presented()
            if (held!!.time != later!!.time)
                throw AssertionError(
                    "HOLD time changed: " +
                        held!!.time +
                        " -> " +
                        later!!.time +
                        " expected=" +
                        expectedTime[0] +
                        " hold=" +
                        activity!!.engine.faultConfig.performance.hold
                )
            activity!!.engine.hitFaults()
            await(
                "manual HIT",
                {
                    val frame = presented()
                    frame != null &&
                        frame!!
                            .nodes
                            .stream()
                            .anyMatch({ n -> n.id == Effects.VHS && n.event.envelope == 1f })
                },
                5000,
            )
            activity!!.engine.rewindFaults()
            await(
                "timeline restart",
                {
                    val frame = presented()
                    frame != null && frame!!.time == 0.0
                },
                5000,
            )
        } finally {
            runOnMainSync({
                if (activity!!.liveEditor != null) activity!!.liveEditor!!.dialog!!.dismiss()
            })
        }
    }

    @Throws(Exception::class)
    internal fun checkFormatUi() { FormatUiChecks.run(this) }

    @Throws(Exception::class)
    internal fun saveUi(name: String) {
        runOnMainSync({ activity!!.getWindow().getDecorView().invalidate() })
        waitForIdleSync()
        SystemClock.sleep(100)
        val image = getUiAutomation().takeScreenshot()
        val dir = File(getTargetContext().getFilesDir(), "verification")
        dir.mkdirs()
        try {
            FileOutputStream(File(dir, name))
                .use({ out -> image.compress(Bitmap.CompressFormat.PNG, 100, out) })
        } finally {
            image.recycle()
        }
    }

    @Throws(Exception::class)
    internal fun checkChainFormat() {
        val selected =
            EffectState.defaults()
                .chain((1 shl Effects.ROW_ERROR) or (1 shl Effects.CRT) or (1 shl Effects.VHS))
        runOnMainSync({
            activity!!.videoMode = false
            activity!!.applyFaultConfig(FaultConfig.defaults())
            val raw = CaptureSettings(activity!!.settings)
            raw.photoFormat = 2
            raw.photoSize = "max"
            activity!!.applySettings(raw)
            activity!!.commitEffects(selected)
        })
        await("RAW ready", { activity!!.ready && activity!!.settings.photoFormat == 2 }, 20000)
        val dialog = arrayOfNulls<EffectDialog>(1)
        runOnMainSync({
            if (activity!!.effectState.mask != selected.mask || activity!!.uiEffects().size != 1)
                throw AssertionError("RAW erased route or enabled RGB faults")
            dialog[0] = EffectDialog(activity!!, false)
            dialog[0]!!.show()
            if (
                dialog[0]!!.choices.size != 13 ||
                    !dialog[0]!!.choices.get(Effects.CRT)!!.getText().toString().contains("DISPLAY")
            )
                throw AssertionError("Missing DISPLAY catalog")
        })
        waitForIdleSync()
        SystemClock.sleep(250)
        saveUi("lean-raw-catalog.png")
        runOnMainSync({
            dialog[0]!!.choices.get(Effects.EXPOSURE)!!.performClick()
            dialog[0]!!.choices.get(Effects.CRT)!!.performClick()
            if (
                activity!!.settings.photoFormat != 2 || activity!!.effectState.mask != selected.mask
            )
                throw AssertionError("Catalog changed format or committed draft")
            dialog[0]!!
                .sheet!!
                .getWindow()!!
                .getDecorView()
                .findViewWithTag<View>("switch-format")
                .performClick()
        })
        waitForIdleSync()
        await("JPEG ready", { activity!!.ready && activity!!.settings.photoFormat == 0 }, 20000)
        saveUi("lean-crt-tuning.png")
        runOnMainSync({
            val replacement = activity!!.effectEditorOwner as EffectDialog
            dialog[0] = replacement
            if (
                replacement == null ||
                    !replacement!!.sheet!!.isShowing() ||
                    replacement!!.focused != Effects.CRT ||
                    replacement!!.state().mask != (selected.mask or (1 shl Effects.EXPOSURE))
            )
                throw AssertionError("Conversion lost draft")
            if (activity!!.effectState.mask != selected.mask)
                throw AssertionError("Conversion committed draft")
            replacement!!.sheet!!.dismiss()
        })
        waitForIdleSync()
        if (activity!!.effectState.mask != selected.mask || activity!!.uiEffects().size != 3)
            throw AssertionError("Cancel or JPEG activation failed")
        for (format in intArrayOf(2, 0)) {
            runOnMainSync({
                val next = CaptureSettings(activity!!.settings)
                next.photoFormat = format
                next.photoSize = "max"
                activity!!.applySettings(next)
            })
            await(
                "format roundtrip",
                { activity!!.ready && activity!!.settings.photoFormat == format },
                20000,
            )
            if (
                activity!!.effectState.mask != selected.mask ||
                    EffectStateStore.load(activity!!.getSharedPreferences("signal", 0)).mask !=
                        selected.mask
            )
                throw AssertionError("Format change lost saved route")
        }
    }

    @Throws(Exception::class)
    internal fun checkEditor() {
        runOnMainSync({
            activity!!.applyFaultConfig(activity!!.faultConfig.enabled(false))
            activity!!.commitEffects(EffectState.defaults().single(Effects.PIXEL_DAMAGE))
        })
        val advancedBefore = activity!!.advancedMode
        runOnMainSync({ activity!!.advancedMode = false })
        val original = activity!!.effectState
        val reference = arrayOfNulls<EffectDialog>(1)
        runOnMainSync({
            reference[0] = EffectDialog(activity!!, false)
            reference[0]!!.show()
        })
        val editor = reference[0]
        try {
            await(
                "editor layout",
                {
                    editor!!.sheet!!.getWindow()!!.getDecorView().getHeight() > 0 &&
                        activity!!.effectEditorSpace != null
                },
                5000,
            )
            SystemClock.sleep(300)
            runOnMainSync({
                val camera = IntArray(2)
                val panel = IntArray(2)
                activity!!.preview!!.getLocationOnScreen(camera)
                editor!!.sheet!!.getWindow()!!.getDecorView().getLocationOnScreen(panel)
                if (
                    activity!!.preview!!.getHeight() < activity!!.dp(140f) ||
                        camera[1] + activity!!.preview!!.getHeight() > panel[1]
                )
                    throw AssertionError("Editor covers the preview")
                editor!!.choices.get(Effects.EXPOSURE)!!.performClick()
                editor!!.choices.get(Effects.ROW_ERROR)!!.performClick()
                if (editor!!.state().ids().size != 3 || !editor!!.state().chained)
                    throw AssertionError("Multi-selection lost faults")
                if (original.encode() != activity!!.effectState.encode())
                    throw AssertionError("Draft selection committed before Apply")
                editor!!.focused = Effects.PIXEL_DAMAGE
                editor!!.tuning = true
                editor!!.renderRoute()
                editor!!.renderBody()
            })
            SystemClock.sleep(200)
            runOnMainSync({
                val slider = editor!!.body!!.findViewWithTag<android.widget.SeekBar>("density")
                val time = SystemClock.uptimeMillis()
                for (action in
                    intArrayOf(
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_MOVE,
                        android.view.MotionEvent.ACTION_UP,
                    )) {
                    val event =
                        android.view.MotionEvent.obtain(
                            time,
                            time + 10,
                            action,
                            slider.getWidth() * .8f,
                            slider.getHeight() * .5f,
                            0,
                        )
                    slider.dispatchTouchEvent(event)
                    event.recycle()
                }
                if (editor!!.draft.get(Effects.PIXEL_DAMAGE, "density") <= .65f)
                    throw AssertionError("Slider did not update draft")
                val camera = IntArray(2)
                val panel = IntArray(2)
                activity!!.preview!!.getLocationOnScreen(camera)
                editor!!.sheet!!.getWindow()!!.getDecorView().getLocationOnScreen(panel)
                if (camera[1] + activity!!.preview!!.getHeight() > panel[1])
                    throw AssertionError("Tuning covers the preview")
            })
            await(
                "draft preview frame",
                {
                    return@await (presented()?.parameters?.get(Effects.PIXEL_DAMAGE, "density") ?: 0f) > .65f
                },
                5000,
            )
            runOnMainSync({ editor!!.sheet!!.dismiss() })
            waitForIdleSync()
            if (
                original.encode() != activity!!.effectState.encode() ||
                    activity!!.effectEditorSpace != null
            )
                throw AssertionError("Cancel did not restore editor layout/state")
            runOnMainSync({
                reference[0] = EffectDialog(activity!!, false)
                reference[0]!!.show()
            })
            val second = reference[0]
            runOnMainSync({
                second!!
                    .sheet!!
                    .getWindow()!!
                    .getDecorView()
                    .findViewWithTag<View>("random-chain")
                    .performClick()
                if (second!!.state().ids().size < 2)
                    throw AssertionError("Random chain missing stages")
                for (id in second!!.state().ids()) if (
                    !Effects.available(
                        id,
                        activity!!.videoMode,
                        !activity!!.videoMode && activity!!.settings.photoFormat == 2,
                    )
                )
                    throw AssertionError("Unsupported random fault")
                val generated = second!!.state().encode()
                second!!
                    .sheet!!
                    .getWindow()!!
                    .getDecorView()
                    .findViewWithTag<View>("apply")
                    .performClick()
                if (generated != activity!!.effectState.encode())
                    throw AssertionError("Apply did not preserve generated chain")
            })
            waitForIdleSync()
            if (activity!!.effectEditorSpace != null)
                throw AssertionError("Apply did not restore layout")
            runOnMainSync({
                val old = EffectDialog(activity!!, false)
                old.show()
                reference[0] = EffectDialog(activity!!, false)
                reference[0]!!.show()
            })
            waitForIdleSync()
            if (activity!!.effectEditorSpace == null || !reference[0]!!.sheet!!.isShowing())
                throw AssertionError("Stale dismissal erased new editor space")
        } finally {
            runOnMainSync({
                activity!!.advancedMode = advancedBefore
                if (reference[0]!!.sheet!!.isShowing()) reference[0]!!.sheet!!.dismiss()
            })
            waitForIdleSync()
        }
    }

    @Throws(Exception::class)
    internal fun checkPixelPreview() {
        for (size in arrayOf<IntArray>(intArrayOf(192, 256), intArrayOf(1080, 1440))) {
            val input = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888)
            input.eraseColor(Color.rgb(100, 100, 100))
            val bytes = ByteArrayOutputStream()
            input.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            input.recycle()
            for (seed in longArrayOf(4547633322485754723L, 0x51a1L, 91L)) {
                val base = EffectState.defaults().single(Effects.PIXEL_DAMAGE).amount(.88f)
                val controls =
                    base
                        .parameters()
                        .with(Effects.PIXEL_DAMAGE, "density", 1f)
                        .with(Effects.PIXEL_DAMAGE, "hot", 1f)
                        .with(Effects.PIXEL_DAMAGE, "columns", 1f)
                        .reseed(Effects.PIXEL_DAMAGE, seed)
                val output =
                    PhotoRenderer.render(
                        getTargetContext(),
                        bytes.toByteArray(),
                        false,
                        evaluated(base.edit(false, base.mask, controls), 0.0),
                    )
                val pixels = IntArray(size[0] * size[1])
                output!!.getPixels(pixels, 0, size[0], 0, 0, size[0], size[1])
                output!!.recycle()
                var damaged = 0
                for (pixel in pixels) if (Math.abs(Color.red(pixel) - 100) > 30) damaged++
                val ratio = damaged / pixels.size.toDouble()
                android.util.Log.i(
                    "SignalCheck",
                    "Pixel damage " +
                        size[0] +
                        "x" +
                        size[1] +
                        " seed=" +
                        seed +
                        " damaged=" +
                        ratio,
                )
                if (ratio < .005 || ratio > .25)
                    throw AssertionError(
                        "Pixel damage density collapsed at " +
                            size[0] +
                            "x" +
                            size[1] +
                            " seed=" +
                            seed +
                            ": " +
                            ratio
                    )
            }
        }
    }

    @Throws(Exception::class)
    internal fun checkMetadata() {
        val b = Bitmap.createBitmap(64, 96, Bitmap.Config.ARGB_8888)
        b.eraseColor(Color.GREEN)
        val file = File(getTargetContext().getCacheDir(), "metadata-check.jpg")
        FileOutputStream(file).use({ out -> b.compress(Bitmap.CompressFormat.JPEG, 95, out) })
        b.recycle()
        val fake = Location("gps")
        fake.setLatitude(-12.345678)
        fake.setLongitude(123.456789)
        fake.setAltitude(42.5)
        fake.setAccuracy(3f)
        fake.setTime(System.currentTimeMillis())
        fake.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos())
        PhotoMetadata.write(
            file,
            null,
            null,
            System.currentTimeMillis(),
            64,
            96,
            fake,
            "SIGNAL METADATA CHECK",
        )
        val exif = ExifInterface(file)
        val latlong = FloatArray(2)
        if (
            !exif.getLatLong(latlong) ||
                Math.abs(latlong[0] + 12.345678) > .00001 ||
                Math.abs(latlong[1] - 123.456789) > .00002
        )
            throw AssertionError("GPS roundtrip")
        if (
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) == null ||
                exif.getAttribute(ExifInterface.TAG_SOFTWARE) == null ||
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) != 1
        )
            throw AssertionError("EXIF fields")
        file.delete()
        val w = 128
        val h = 96
        val samples = ByteArray(w * h * 2)
        for (i in 0 until w * h) RawGlitch.write(samples, i, 256 + i % 3500)
        val frame = evaluated(EffectState.defaults().chain(-1), .1)
        val first = RawGlitch.chain(samples, w, h, 4095, 256, frame)
        val same = RawGlitch.chain(samples, w, h, 4095, 256, frame)
        if (!Arrays.equals(first, same)) throw AssertionError("RAW snapshot replay")
        for (i in 0 until w * h) if (RawGlitch.read(first, i) > 4095)
            throw AssertionError("RAW bounds")
    }
}
