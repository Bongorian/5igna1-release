package com.bongorian.signa1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.location.Location
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaRecorder
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.bongorian.signa1.CameraOptions.RawVideo
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class GlitchEngine(val context: Activity, val listener: Listener) {
    internal interface Listener {
        fun status(text: String)

        fun ready(value: Boolean)

        fun recording(value: Boolean)

        fun saved(uri: Uri, video: Boolean)

        fun configured(
            options: CameraOptions,
            settings: CaptureSettings,
            video: Boolean,
            width: Int,
            height: Int,
            detail: String,
        )

        fun fps(fps: Float)

        fun liveFrame(frame: EffectState.Frame) {}
    }

    val thread: HandlerThread = HandlerThread("SignalGL")
    val gl: Handler
    val ui: Handler = Handler(Looper.getMainLooper())
    val files: ExecutorService = Executors.newSingleThreadExecutor()
    var camera: CameraDevice? = null
    var session: CameraCaptureSession? = null
    var request: CaptureRequest.Builder? = null
    var characteristics: CameraCharacteristics? = null
    var displayTarget: SurfaceTexture? = null
    var cameraTexture: SurfaceTexture? = null
    var cameraSurface: Surface? = null
    var displaySurface: Surface? = null
    var encoderSurface: Surface? = null
    var stillReader: ImageReader? = null
    var display: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    var eglContext: EGLContext? = EGL14.EGL_NO_CONTEXT
    var window: EGLSurface? = EGL14.EGL_NO_SURFACE
    var encoder: EGLSurface? = EGL14.EGL_NO_SURFACE
    var eglConfig: EGLConfig? = null
    var previewChain: EffectChain? = null
    private var lightPhotoChain: EffectChain? = null
    private var captureInputTexture = 0
    private var captureInputExternal = false
    private val captureInputMatrix = FloatArray(16)
    private var captureInputWidth = 0
    private var captureInputHeight = 0
    private var captureInputNs = 0L
    val foregroundWork = ForegroundWork()
    @Volatile var foreground = true
    /** Stop capture and finalize its file before releasing camera/GL resources. */
    fun background() {
        foreground = false
        foregroundWork.pause()
        gl.post { close() }
    }

    var tapInput: TapInput? = null
        private set
    @Volatile var tapSource: TapSource? = null
        private set
    val tapTick: Runnable = object : Runnable {
        override fun run() {
            if (!attached || tapInput == null) return
            frame()
            gl.postDelayed(this, 33)
        }
    }
    fun toggleTapPlayback() { gl.post { tapSource?.toggle() } }

    /** Metadata only: a restored TAP source has no preceding camera session. */
    private fun loadCameraCatalog(manager: CameraManager): String {
        var id: String? = null
        for (candidate in manager.cameraIdList) {
            val cc = manager.getCameraCharacteristics(candidate)
            val facing = cc.get<Int?>(CameraCharacteristics.LENS_FACING)
            if (
                cc.get<StreamConfigurationMap?>(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) != null &&
                    facing != null &&
                    facing ==
                        (if (front) CameraCharacteristics.LENS_FACING_FRONT
                        else CameraCharacteristics.LENS_FACING_BACK)
            ) {
                id = candidate
                characteristics = cc
                break
            }
        }
        if (id == null)
            for (candidate in manager.cameraIdList) {
                val cc = manager.getCameraCharacteristics(candidate)
                if (
                    cc.get<StreamConfigurationMap?>(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    ) != null
                ) {
                    id = candidate
                    characteristics = cc
                    break
                }
            }
        checkNotNull(id) { "No camera" }
        front =
            CameraCharacteristics.LENS_FACING_FRONT ==
                characteristics!!.get<Int?>(CameraCharacteristics.LENS_FACING)
        val rotation = characteristics!!.get<Int?>(CameraCharacteristics.SENSOR_ORIENTATION)
        sensorRotation = if (rotation == null) 0 else rotation
        options = catalogs.get(id)
        if (options == null) {
            options = CameraOptions(id, requireNotNull(characteristics), maxTexture)
            catalogs.put(id, options)
        }
        options!!.recommendedPhotoPixels = deviceProfile.photoPixels()
        options!!.recommendedVideoPixels = deviceProfile.videoPixels()
        return requireNotNull(id)
    }

    private fun openTap() {
        deviceProfile.recommend(width,height,context.display?.refreshRate ?: 60f)
        if (options == null) loadCameraCatalog(context.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
        val source = TapSource(this, tapInput ?: return)
        tapSource = source
        status(context.getString(R.string.tap_loading))
        source.open()
    }

    fun tapPrepared(source: TapSource) {
        if (tapSource !== source || !attached) return
        val scale = minOf(1.0, kotlin.math.sqrt((if (videoMode) deviceProfile.videoPixels()
            else deviceProfile.photoPixels()).toDouble() / (source.width.toDouble() * source.height)))
        outW = maxOf(2, (source.width * scale).toInt() / 2 * 2)
        outH = maxOf(2, (source.height * scale).toInt() / 2 * 2)
        signalW = outW
        signalH = outH
        videoChoice = CameraOptions.Video(Size(outW, outH), 30, false)
        recommendPreview()
        val catalog = options ?: return
        val actual = CaptureSettings(settings)
        val ticket = generation
        ui.post { if (ticket == generation) listener.configured(catalog, actual, videoMode,
            outW, outH, context.getString(R.string.tap_ready)) }
        gl.removeCallbacks(tapTick)
        gl.post(tapTick)
    }

    val timeEcho = TimeEcho()
    fun triggerEcho() { gl.post { timeEcho.trigger() } }
    var blitChain: EffectChain? = null
    val presentedFrames: FrameHistory<SignalBuffer> =
        FrameHistory<SignalBuffer>(3, Supplier { SignalBuffer() })
    val encoderScratch: SignalBuffer = SignalBuffer()
    @Volatile private var normalPublishedNs = 0L
    @Volatile private var normalFirstPublishedNs = 0L
    @Volatile private var normalAcknowledgedNs = 0L

    fun previewAcknowledged(): Long =
        if (settings.advancedMode) presentedFrames.acknowledged() else normalAcknowledgedNs
    val cleanFrame: EffectState.Frame = EffectState.defaults().snapshot(true, 0)
    var signalW: Int = 0
    var signalH: Int = 0
    private var cameraStreamSize: Size? = null
    val signalMetadata: LinkedHashMap<Long?, TotalCaptureResult?> =
        LinkedHashMap<Long?, TotalCaptureResult?>()
    var faults: FaultModel = FaultModel()
    var previewFaults: FaultModel? = null
    var previewFaultConfig: FaultConfig? = null
    val faultInputs: FaultInputs
    var faultConfig: FaultConfig = FaultConfig.defaults()
    var recorderAudio: Boolean = false

    @Volatile var faultStatus: String = "LIVE FAULT OFF"
    var faultUiNs: Long = 0
    var liveUiNs: Long = 0
    var liveUiMask: Int = -1

    fun hitFaults() {
        gl.post(Runnable@{ activeFaults()!!.hit() })
    }

    fun rewindFaults() {
        gl.post(Runnable@{ activeFaults()!!.rewind() })
    }

    fun applyFaultConfig(next: FaultConfig) {
        gl.post(
            Runnable@{
                faultConfig = next
                faultInputs.configure(inputFaultConfig(), attached && !cooling, recorderAudio)
            }
        )
    }

    fun activeFaults(): FaultModel {
        return previewFaults ?: faults
    }

    fun activeFaultConfig(): FaultConfig {
        return previewFaultConfig ?: faultConfig
    }

    fun inputFaultConfig(): FaultConfig {
        if (previewFaultConfig == null) return faultConfig
        val p = previewFaultConfig
        val c = faultConfig
        return FaultConfig(
            p!!.enabled || c!!.enabled,
            p.enabled && p.motion || c!!.enabled && c.motion,
            p.enabled && p.audio || c!!.enabled && c.audio,
            p.enabled && p.timing || c!!.enabled && c.timing,
            p.enabled && p.thermal || c!!.enabled && c.thermal,
            p.enabled && p.cpu || c!!.enabled && c.cpu,
            c!!.sensitivity,
            c.mains,
        )
    }

    fun previewFaultConfig(next: FaultConfig?) {
        gl.post(
            Runnable@{
                if (previewFaults == null) previewFaults = faults!!.copy()
                previewFaultConfig = next
                faultInputs.configure(inputFaultConfig(), attached && !cooling, recorderAudio)
            }
        )
    }

    fun finishFaultPreview(apply: Boolean) {
        gl.post(
            Runnable@{
                if (apply && previewFaults != null) {
                    faults = previewFaults!!
                    faultConfig = previewFaultConfig!!
                }
                previewFaults = null
                previewFaultConfig = null
                faultInputs.configure(faultConfig, attached && !cooling, recorderAudio)
            }
        )
    }

    fun faultFrame(state: EffectState): EffectState.Frame {
        val frame = activeFaults()!!.apply(
            if (rawVideoMode())
                state.snapshot(
                    false,
                    1,
                )
            else state.snapshot(videoMode, settings.photoFormat),
            activeFaultConfig().experimental(settings.experimentalSignals),
        )
        return if (tapInput == null) frame else frame.afterReadout()
    }

    val timingCallback: CaptureCallback =
        object : CaptureCallback() {
            override fun onCaptureCompleted(
                s: CameraCaptureSession,
                r: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                if (s === session) {
                    faultInputs.capture(result)
                    val ns = result.get<Long?>(CaptureResult.SENSOR_TIMESTAMP)
                    if (ns != null) {
                        signalMetadata.put(ns, result)
                        while (signalMetadata.size > 16) signalMetadata.remove(
                            signalMetadata.keys.iterator().next()
                        )
                    }
                    if (rawRecorder != null) rawRecorder!!.result(result)
                    if (rawProbe != null) rawProbe!!.result(result)
                }
            }
        }
    private var effectState: EffectState = EffectState.defaults()
    private var previewEffects: EffectState? = null
    private val configRevision = AtomicLong()
    // Surface lifecycle changes must not discard the latest settings selection.
    private val settingsRevision = AtomicLong()
    private var appliedRevision: Long = 0

    fun setLocationEnabled(value: Boolean) {
        gl.post(Runnable@{ settings.location = value })
    }

    fun setEffects(next: EffectState) {
        gl.post(
            Runnable@{
                effectState = next
                previewEffects = null
            }
        )
    }

    fun previewEffects(next: EffectState) {
        gl.post(Runnable@{ previewEffects = next })
    }

    fun clearEffectPreview() {
        gl.post(Runnable@{ previewEffects = null })
    }

    val adaptiveLoad: AdaptiveLoad = AdaptiveLoad()
    val thermalMonitor: ThermalMonitor
    val deviceProfile: DeviceProfile

    @Volatile var previewFps: Int = 24

    @Volatile var cooling: Boolean = false

    @Volatile var renderedFrames: Long = 0
    var requestedPreviewCameraFps: Int = 30
    val thermalPoll: Runnable =
        object : Runnable {
            override fun run() {
                if (!attached) return
                val now = SystemClock.elapsedRealtime()
                if (!settings.expertMode) thermalMonitor.sample(now)
                applyLoadSample(
                    now,
                    thermalMonitor.status,
                    thermalMonitor.batteryC,
                    thermalMonitor.headroom,
                )
                gl.postDelayed(this, 1000)
            }
        }

    fun applyLoadSample(now: Long, thermal: Int, battery: Float, headroom: Float) {
        adaptiveLoad.setExpert(settings.expertMode, expertCameraFps())
        val selected = (if (previewEffects == null) effectState else previewEffects)!!
        val passes =
            if (selected.amount <= 0) 1
            else
                1 +
                    selected
                        .forContext(
                            videoMode,
                            settings.photoFormat,
                        )
                        .ids().sumOf { id ->
                            if (tapInput != null && Effects.point(id).ordinal <= Effects.Point.READOUT.ordinal) 0
                            else if (Effects.physical(id) && !settings.experimentalSignals) 0
                            else if (deviceProfile.gpu?.measurement != null) GpuRecommendation.workUnits(id)
                            else if (id == Effects.MOTION_BLUR || id == Effects.SMEAR) 9 else 1
                        }
        adaptiveLoad.sample(
            now,
            thermal,
            battery,
            headroom,
            if (settings.lightMode && encoderScratch.width > 0)
                encoderScratch.width.toLong() * encoderScratch.height else signalW.toLong() * signalH,
            passes,
            deviceProfile.constrained,
        )
        previewFps = adaptiveLoad.previewFps
        val wasCooling = cooling
        cooling = adaptiveLoad.cooling
        if (cooling) {
            if (recording) stopVideo()
            if (!wasCooling) faultInputs.stop()
            if (!photoBusy && session != null)
                try {
                    session!!.stopRepeating()
                } catch (ignored: Exception) {}
            ready(false)
            if (!wasCooling) status(context.getString(R.string.load_cooling))
        } else if (wasCooling) {
            faultInputs.configure(inputFaultConfig(), attached, recorderAudio)
            updateRequest()
            gl.post(Runnable@{ this.frame() })
            status(context.getString(R.string.load_resumed))
        }
        val rate = adaptiveLoad.cameraFps()
        if (rate != requestedPreviewCameraFps) {
            requestedPreviewCameraFps = rate
            if (!cooling && !recording && !videoMode) updateRequest()
        }
    }

    fun expertCameraFps(): Int {
        if (videoMode && videoChoice != null) return videoChoice!!.fps
        if (tapInput != null) return 30
        var best = 0
        if (characteristics == null) return 30
        var duration: Long = 0
        if (options != null && signalW > 0 && signalH > 0)
            try {
                duration =
                    options!!
                        .map
                        .getOutputMinFrameDuration<SurfaceTexture?>(
                            SurfaceTexture::class.java,
                            cameraStreamSize ?: Size(signalH, signalW),
                        )
            } catch (ignored: IllegalArgumentException) {}
        val ceiling =
            if (duration > 0) max(1, Math.round(1e9 / duration)).toInt() else Int.MAX_VALUE
        val ranges: Array<Range<Int>>? =
            characteristics!!.get<Array<Range<Int>>?>(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )
        if (ranges != null)
            for (range in ranges) if (range.upper!! <= ceiling) best = max(best, range.upper!!)
        return if (best > 0) best else min(30,ceiling)
    }

    private fun recommendPreview() {
        val size = PreviewSizing.choose(signalW,signalH,width,height,settings.lightMode,false,false)
        val refresh = context.display?.refreshRate ?: 60f
        adaptiveLoad.recommend(deviceProfile.initialFps(size.width.toLong()*size.height,refresh,expertCameraFps()),
            deviceProfile.recommendation?.pixelWorkBudget)
    }

    fun loadSummary(): String {
        return if (settings.expertMode) context.getString(R.string.expert_active)
        else if (cooling) context.getString(R.string.load_cooling)
        else
            String.format(
                Locale.US,
                context.getString(R.string.load_preview_rate),
                previewFps,
            )
    }

    var program: Int = 0
    var texture: Int = 0
    var width: Int = 0
    var height: Int = 0
    var sensorRotation: Int = 90
    @Volatile var captureRotation: Int = 90
    private var displayDegrees = 0
    private val orientedMatrix = FloatArray(16)
    private val displayMatrix = FloatArray(16)

    @Suppress("DEPRECATION")
    private fun displayDegrees(): Int = (context.getSystemService(Context.WINDOW_SERVICE) as
        android.view.WindowManager).defaultDisplay.rotation * 90

    private fun updateOrientation() {
        displayDegrees = displayDegrees()
        captureRotation = CameraOrientation.relative(sensorRotation, displayDegrees, front)
        CameraOrientation.textureTransform(displayDegrees).copyInto(displayMatrix)
    }
    var maxTexture: Int = 4096

    @Volatile var generation: Int = 0

    @Volatile var outW: Int = 3072

    @Volatile var outH: Int = 4096

    @Volatile var front: Boolean = false

    @Volatile var torch: Boolean = false

    @Volatile var frameSeen: Boolean = false

    @Volatile var recording: Boolean = false

    @Volatile var photoBusy: Boolean = false
    var sessionFallback: Boolean = false
    var attached: Boolean = false
    var videoMode: Boolean = false
    var zoom: Float = 1f
    var maxZoom: Float = 4f
    val matrix: FloatArray = FloatArray(16)
    var vertices: FloatBuffer?
    var positionLoc: Int = 0
    var matrixLoc: Int = 0
    var timeLoc: Int = 0
    var amountLoc: Int = 0
    var modeLoc: Int = 0
    var sizeLoc: Int = 0
    var rawVideoChoice: RawVideo? = null
    var rawReader: ImageReader? = null
    var rawProbe: RawVideoRecorder? = null

    @Volatile var rawRecorder: RawVideoRecorder? = null

    @Volatile var rawFrameSeen: Boolean = false

    fun rawProgress(): String {
        val current = rawRecorder
        return if (current == null) ""
        else
            current.written.toString() +
                context.getString(R.string.ui_frames_dropped_253) +
                current.dropped
    }

    fun rawVideoMode(): Boolean {
        return videoMode && settings.rawVideo
    }

    var options: CameraOptions? = null
    var photoChoice: CameraOptions.Photo? = null
    var videoChoice: CameraOptions.Video? = null
    @Volatile var settings: CaptureSettings
    var position: Supplier<Location?> = Supplier { null }
    var recorder: MediaRecorder? = null
    var videoFd: ParcelFileDescriptor? = null
    var nextFd: ParcelFileDescriptor? = null
    var videoUri: Uri? = null
    var nextUri: Uri? = null
    var videoTaken: Long = 0
    var nextTaken: Long = 0
    var lastFrameNs: Long = 0
    var lastPreviewNs: Long = 0
    var frameCount: Long = 0
    var fpsStart: Long = 0
    var encoderTimeOffset: Long = Long.MIN_VALUE
    var segmentBytes: Long = 3500000000L // bounded files, no total recording time limit
    var pending: PendingPhoto? = null
    val catalogs: MutableMap<String?, CameraOptions?> = HashMap<String?, CameraOptions?>()
    val storageWatch: Runnable =
        object : Runnable {
            override fun run() {
                if (!recording) return
                try {
                    if (
                        StatFs(Environment.getExternalStorageDirectory().path).availableBytes <
                            256000000L
                    ) {
                        stopVideo()
                        status(
                            context.getString(
                                R.string.ui_recording_saved_and_stopped_due_to_low_storage
                            )
                        )
                        return
                    }
                } catch (ignored: Exception) {}
                gl.postDelayed(this, 5000)
            }
        }

    fun status(text: String) {
        ui.post(Runnable@{ listener.status(text) })
    }

    fun ready(value: Boolean) {
        var value = value
        value = value && !cooling
        val actualReady = value
        val revision = appliedRevision
        val ticket = generation
        ui.post(
            Runnable@{
                if (revision == configRevision.get() && (!actualReady || ticket == generation))
                    listener.ready(actualReady)
            }
        )
    }

    fun error(text: String?, e: Exception) {
        Log.e("Signal", text, e)
        status(text + " · " + e.javaClass.simpleName)
    }

    fun attach(target: SurfaceTexture?, w: Int, h: Int) {
        val revision = configRevision.incrementAndGet()
        gl.post(
            Runnable@{
                if (revision != configRevision.get()) return@Runnable
                if (attached && displayTarget === target) {
                    appliedRevision = revision
                    width = w
                    height = h
                    ready(frameSeen)
                    return@Runnable
                }
                appliedRevision = revision
                close()
                displayTarget = target
                width = w
                height = h
                attached = true
                reconnectAttempts = 0
                previewStartedMs = SystemClock.elapsedRealtime()
                gl.removeCallbacks(previewWatch)
                gl.postDelayed(previewWatch, 1000)
                gl.removeCallbacks(thermalPoll)
                gl.post(thermalPoll)
                faultInputs.configure(inputFaultConfig(), !cooling, recorderAudio)
                sessionFallback = false
                try {
                    initGl(target)
                    if (tapInput == null) openCamera() else openTap()
                } catch (e: Exception) {
                    error(context.getString(R.string.ui_could_not_start_the_camera), e)
                    close()
                }
            }
        )
    }

    var reconnectAttempts: Int = 0
    var reconnectPending: Boolean = false
    var previewStartedMs: Long = 0
    var lastPreviewAckMs: Long = 0
    var healthySinceMs: Long = 0
    val reconnectCamera: Runnable =
        object : Runnable {
            override fun run() {
                reconnectPending = false
                if (!attached || cooling || recording || photoBusy) return
                status(context.getString(R.string.camera_reconnecting))
                restart()
            }
        }
    val previewWatch: Runnable =
        object : Runnable {
            override fun run() {
                if (!attached) return
                val now = SystemClock.elapsedRealtime()
                if (
                    !cooling &&
                        !recording &&
                        !photoBusy &&
                        !reconnectPending &&
                        now -
                            max(
                                previewStartedMs,
                                lastPreviewAckMs,
                            ) > 6000
                ) {
                    scheduleReconnect()
                    if (reconnectAttempts >= 3 && !reconnectPending) return
                }
                gl.postDelayed(this, 1000)
            }
        }

    init {
        thermalMonitor = ThermalMonitor(context)
        deviceProfile = DeviceProfile(context)
        settings = CaptureSettings.load(context.getSharedPreferences("signal", 0))
        thread.start()
        gl = Handler(thread.looper)
        faultInputs = FaultInputs(context, gl)
        vertices = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertices!!.put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0)
        RawVideoRecorder.recoverPending(this)
    }

    fun scheduleReconnect() {
        if (!attached || cooling || recording || photoBusy || reconnectPending) return
        ready(false)
        if (reconnectAttempts >= 3) {
            status(context.getString(R.string.camera_retry_hint))
            gl.removeCallbacks(previewWatch)
            return
        }
        reconnectPending = true
        val delay = 300L shl reconnectAttempts++
        gl.postDelayed(reconnectCamera, delay)
    }

    fun cameraInterrupted() {
        closeCamera()
        scheduleReconnect()
    }

    fun retryPreview() {
        gl.post(
            Runnable@{
                if (!attached || recording || photoBusy || cooling) return@Runnable
                reconnectAttempts = 0
                gl.removeCallbacks(reconnectCamera)
                reconnectPending = false
                gl.removeCallbacks(previewWatch)
                gl.post(previewWatch)
                scheduleReconnect()
            }
        )
    }

    fun resize(w: Int, h: Int) {
        gl.post(
            Runnable@{
                width = w
                height = h
            }
        )
    }

    fun detach() {
        configRevision.incrementAndGet()
        gl.post(Runnable@{ this.close() })
    }

    fun releaseSurface(target: SurfaceTexture) {
        gl.post(
            Runnable@{
                if (displayTarget === target) close()
                target.release()
            }
        )
    }

    fun shutdown() {
        foregroundWork.close()
        gl.post(
            Runnable@{
                close()
                files.shutdown()
                thread.quitSafely()
            }
        )
    }

    fun configure(next: CaptureSettings, video: Boolean, effects: EffectState, input: TapInput? = null) {
        val copy = CaptureSettings(next)
        val selection = settingsRevision.incrementAndGet()
        val revision = configRevision.incrementAndGet()
        gl.post(
            Runnable@{
                if (recording || photoBusy || selection != settingsRevision.get()) return@Runnable
                appliedRevision = revision
                tapInput = input.takeIf { copy.experimentalSignals }
                if (tapInput != null) { copy.photoFormat = 0; copy.rawVideo = false }
                settings = copy
                if (!settings.expertMode) thermalMonitor.sample(SystemClock.elapsedRealtime())
                videoMode = video
                effectState = effects
                previewEffects = null
                if (attached) restart()
            }
        )
    }

    fun restart() {
        previewStartedMs = SystemClock.elapsedRealtime()
        sessionFallback = false
        try {
            closeCamera()
            current(window)
            for (buffer in presentedFrames.values()) buffer.release()
            encoderScratch.release()
            previewChain?.releaseBuffers()
            lightPhotoChain?.releaseBuffers()
            timeEcho.release()
            if (cameraTexture != null) {
                cameraTexture!!.setOnFrameAvailableListener(null)
                cameraTexture!!.release()
            }
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            initCameraTexture()
            status(context.getString(R.string.ui_configuring_camera))
            if (tapInput == null) openCamera() else openTap()
        } catch (e: Exception) {
            error(context.getString(R.string.ui_could_not_change_capture_settings), e)
            scheduleReconnect()
        }
    }

    fun switchCamera() {
        gl.post(
            Runnable@{
                if (recording || photoBusy || !attached) return@Runnable
                front = !front
                torch = false
                zoom = 1f
                restart()
            }
        )
    }

    fun torch() {
        gl.post(
            Runnable@{
                if (characteristics == null) return@Runnable
                if (
                    true !=
                        characteristics!!.get<Boolean?>(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                ) {
                    status(context.getString(R.string.ui_this_camera_has_no_light))
                    return@Runnable
                }
                torch = !torch
                updateRequest()
            }
        )
    }

    fun zoom(value: Float) {
        gl.post(
            Runnable@{
                zoom = max(1f, min(maxZoom, value))
                updateRequest()
            }
        )
    }

    fun focus() {
        gl.post(
            Runnable@{
                if (
                    cooling ||
                        request == null ||
                        session == null ||
                        session is CameraConstrainedHighSpeedCaptureSession
                )
                    return@Runnable
                try {
                    request!!.set<Int?>(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_START,
                    )
                    session!!.capture(request!!.build(), null, gl)
                    request!!.set<Int?>(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_IDLE,
                    )
                } catch (e: Exception) {
                    Log.w("Signal", "Focus", e)
                }
            }
        )
    }

    fun openCamera() {
        if (
            !attached ||
                context.checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            status(context.getString(R.string.ui_allow_camera_access))
            return
        }
        try {
            deviceProfile.recommend(width,height,context.display?.refreshRate ?: 60f)
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = loadCameraCatalog(manager)
            if (options!!.videosFor(settings.codec).isEmpty())
                settings.codec = if (settings.codec == "video/hevc") "video/avc" else "video/hevc"
            if (settings.photoFormat != 0 && options!!.raws.isEmpty()) {
                settings.photoFormat = 0
                status(context.getString(R.string.ui_raw_is_unavailable_on_this_camera_switched_to))
            }
            if (rawVideoMode() || (!videoMode && settings.photoFormat != 0)) zoom = 1f
            if (settings.rawVideo && !options!!.rawVideoAvailable()) {
                settings.rawVideo = false
                status(context.getString(R.string.ui_raw_video_is_unavailable_on_this_camera))
            }
            rawVideoChoice = options!!.rawVideo(settings)
            photoChoice = options!!.photo(settings)
            videoChoice =
                if (rawVideoMode() && rawVideoChoice != null)
                    CameraOptions.Video(
                        rawVideoChoice!!.size,
                        min(settings.rawVideoFps, rawVideoChoice!!.maxFps),
                        false,
                    )
                else options!!.video(settings)
            check(!(if (videoMode) videoChoice == null else photoChoice == null)) {
                "No supported output"
            }
            var stream =
                if (rawVideoMode())
                    options!!.previewFor(
                        CameraOptions.Photo(
                            rawVideoChoice!!.size,
                            false,
                        )
                    )
                else if (videoMode) videoChoice!!.size
                else if (settings.photoFormat == 0) photoChoice!!.size
                else options!!.previewFor(photoChoice!!)
            if (sessionFallback && !videoMode)
                for (candidate in
                    options!!.map.getOutputSizes<SurfaceTexture?>(SurfaceTexture::class.java)) if (
                    CameraOptions.area(candidate) < CameraOptions.area(stream)
                )
                    stream = candidate
            val output =
                if (videoMode) videoChoice!!.size
                else if (settings.photoFormat == 0) stream else photoChoice!!.size
            updateOrientation()
            val swap = captureRotation % 180 != 0
            signalW = if (swap) stream.height else stream.width
            signalH = if (swap) stream.width else stream.height
            cameraStreamSize = stream
            outW = if (swap) output.height else output.width
            outH = if (swap) output.width else output.height
            cameraTexture!!.setDefaultBufferSize(stream.width, stream.height)
            recommendPreview()
            applyLoadSample(
                SystemClock.elapsedRealtime(),
                thermalMonitor.status,
                thermalMonitor.batteryC,
                thermalMonitor.headroom,
            )
            val mz =
                characteristics!!.get<Float?>(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM
                )
            maxZoom = if (mz == null) 1f else min(4f, mz)
            if (appliedRevision == configRevision.get())
                settings.save(context.getSharedPreferences("signal", 0))
            val catalog = requireNotNull(options)
            val actual = CaptureSettings(settings)
            val ow = outW
            val oh = outH
            val detail = description()
            val actualVideo = videoMode
            val revision = appliedRevision
            val ticket = ++generation
            ui.post(
                Runnable@{
                    if (ticket == generation && revision == configRevision.get())
                        listener.configured(
                            catalog,
                            actual,
                            actualVideo,
                            ow,
                            oh,
                            detail,
                        )
                }
            )
            Log.i(
                "Signal",
                "Camera=" +
                    id +
                    " stream=" +
                    stream +
                    " output=" +
                    outW +
                    "x" +
                    outH +
                    " " +
                    detail,
            )
            manager.openCamera(
                id,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(c: CameraDevice) {
                        if (ticket != generation || !attached) {
                            c.close()
                            return
                        }
                        camera = c
                        createSession(c, ticket)
                    }

                    override fun onDisconnected(c: CameraDevice) {
                        c.close()
                        if (ticket == generation) {
                            cameraInterrupted()
                        }
                    }

                    override fun onError(c: CameraDevice, code: Int) {
                        c.close()
                        if (ticket == generation) {
                            cameraInterrupted()
                        }
                    }
                },
                gl,
            )
        } catch (e: Exception) {
            error(context.getString(R.string.ui_could_not_open_the_camera), e)
            scheduleReconnect()
        }
    }

    fun description(): String {
        if (tapInput != null) return "TAP · ${outW}×${outH} · READOUT → DATA"
        if (rawVideoMode())
            return context.getString(R.string.ui_raw_sequence) +
                outW +
                "×" +
                outH +
                context.getString(R.string.ui_target) +
                videoChoice!!.fps +
                "fps"
        if (videoMode) return outW.toString() + "×" + outH + " / " + videoChoice!!.fps + "fps"
        return String.format(
            Locale.US,
            "%.1f MP / %s",
            outW * outH.toDouble() / 1e6,
            if (settings.photoFormat == 0) "JPG " + settings.jpegQuality else "RAW",
        )
    }

    fun createSession(c: CameraDevice, ticket: Int) {
        try {
            cameraSurface = Surface(cameraTexture)
            val surfaces: MutableList<Surface> = ArrayList<Surface>()
            surfaces.add(cameraSurface!!)
            if (rawVideoMode()) {
                rawFrameSeen = false
                rawReader =
                    ImageReader.newInstance(
                        rawVideoChoice!!.size.width,
                        rawVideoChoice!!.size.height,
                        ImageFormat.RAW_SENSOR,
                        3,
                    )
                rawReader!!.setOnImageAvailableListener(
                    OnImageAvailableListener { reader: ImageReader? ->
                        rawImage(
                            reader!!,
                            ticket,
                        )
                    },
                    gl,
                )
                surfaces.add(rawReader!!.surface)
            }
            if (!videoMode && settings.photoFormat != 0) {
                val format = ImageFormat.RAW_SENSOR
                stillReader =
                    ImageReader.newInstance(
                        photoChoice!!.size.width,
                        photoChoice!!.size.height,
                        format,
                        2,
                    )
                stillReader!!.setOnImageAvailableListener(
                    OnImageAvailableListener { reader: ImageReader? ->
                        imageAvailable(
                            reader!!,
                            ticket,
                        )
                    },
                    gl,
                )
                surfaces.add(stillReader!!.surface)
            }
            val callback: CameraCaptureSession.StateCallback =
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        if (ticket != generation || camera !== c) {
                            s.close()
                            return
                        }
                        session = s
                        try {
                            request =
                                c.createCaptureRequest(
                                    if (videoMode) CameraDevice.TEMPLATE_RECORD
                                    else CameraDevice.TEMPLATE_PREVIEW
                                )
                            request!!.addTarget(cameraSurface!!)
                            if (rawVideoMode() && rawReader != null) {
                                request!!.addTarget(rawReader!!.surface)
                                val shading =
                                    characteristics!!.get<IntArray?>(
                                        CameraCharacteristics
                                            .STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES
                                    )
                                if (shading != null)
                                    for (mode in shading) if (
                                        mode == CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON
                                    )
                                        request!!.set<Int?>(
                                            CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                                            mode,
                                        )
                                rawProbe = RawVideoRecorder(this@GlitchEngine, true)
                            }
                            updateRequest()
                            if (rawVideoMode())
                                gl.postDelayed(
                                    Runnable@{
                                        if (ticket == generation && !rawFrameSeen && !cooling)
                                            failRawSession(
                                                context.getString(
                                                    R.string
                                                        .ui_could_not_receive_continuous_raw_frames
                                                )
                                            )
                                    },
                                    8000,
                                )
                        } catch (e: Exception) {
                            error(context.getString(R.string.ui_could_not_configure_the_preview), e)
                        }
                    }

                    override fun onConfigureFailed(s: CameraCaptureSession) {
                        s.close()
                        if (ticket == generation) recoverSession()
                    }
                }
            if (rawVideoMode()) {
                val outputs: MutableList<OutputConfiguration?> = ArrayList<OutputConfiguration?>()
                for (surface in surfaces) outputs.add(OutputConfiguration(surface))
                val configuration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        Executor { r: Runnable? -> gl.post(r!!) },
                        callback,
                    )
                try {
                    if (!c.isSessionConfigurationSupported(configuration)) {
                        failRawSession(
                            context.getString(
                                R.string.ui_simultaneous_raw_and_preview_output_is_unsupported
                            )
                        )
                        return
                    }
                } catch (ignored: UnsupportedOperationException) {}
                c.createCaptureSession(configuration)
            } else if (videoMode && videoChoice!!.highSpeed)
                c.createConstrainedHighSpeedCaptureSession(
                    surfaces,
                    callback,
                    gl,
                )
            else if (!videoMode && photoChoice!!.maximumPixelMode) {
                val outputs: MutableList<OutputConfiguration?> = ArrayList<OutputConfiguration?>()
                outputs.add(OutputConfiguration(cameraSurface!!))
                val still = OutputConfiguration(stillReader!!.surface)
                still.addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                outputs.add(still)
                c.createCaptureSession(
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        Executor { r: Runnable? -> gl.post(r!!) },
                        callback,
                    )
                )
            } else c.createCaptureSession(surfaces, callback, gl)
        } catch (e: Exception) {
            Log.w("Signal", "Session configuration", e)
            if (ticket == generation) recoverSession()
        }
    }

    fun failRawSession(reason: String?) {
        if (options != null) options!!.rawVideoFailure = reason
        settings.rawVideo = false
        settings.save(context.getSharedPreferences("signal", 0))
        status(reason + context.getString(R.string.ui_returning_to_standard_video))
        restart()
    }

    fun recoverSession() {
        if (rawVideoMode()) {
            failRawSession(context.getString(R.string.ui_could_not_start_the_raw_video_session))
            return
        }
        if (sessionFallback) {
            ready(false)
            status(context.getString(R.string.ui_this_camera_cannot_use_these_capture_settings))
            return
        }
        sessionFallback = true
        closeCamera()
        settings.photoFormat = 0
        settings.photoSize = "auto"
        settings.videoKey = ""
        settings.codec = "video/avc"
        if (!videoMode && !options!!.photos.isEmpty())
            for (p in options!!.photos) if (!p.maximumPixelMode) settings.photoSize = p.key()
        if (videoMode) {
            val choices = options!!.videosFor(settings.codec)
            if (!choices.isEmpty()) settings.videoKey = choices.get(choices.size - 1)!!.key()
        }
        status(context.getString(R.string.ui_retrying_with_a_compatible_lower_resolution))
        try {
            current(window)
            cameraTexture!!.setOnFrameAvailableListener(null)
            cameraTexture!!.release()
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            initCameraTexture()
            openCamera()
        } catch (e: Exception) {
            error(context.getString(R.string.ui_could_not_reconfigure_the_camera), e)
        }
    }

    fun applyControls(builder: CaptureRequest.Builder, still: Boolean) {
        builder.set<Int?>(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        val af = characteristics!!.get<IntArray?>(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        val desired =
            if (still) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        if (af != null)
            for (value in af) if (value == desired)
                builder.set<Int?>(
                    CaptureRequest.CONTROL_AF_MODE,
                    desired,
                )
        if (true == characteristics!!.get<Boolean?>(CameraCharacteristics.FLASH_INFO_AVAILABLE))
            builder.set<Int?>(
                CaptureRequest.FLASH_MODE,
                if (torch) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
            )
        val bounds =
            characteristics!!.get<Rect?>(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (bounds == null) return
        val w = (bounds.width() / zoom).toInt()
        val h = (bounds.height() / zoom).toInt()
        builder.set<Rect?>(
            CaptureRequest.SCALER_CROP_REGION,
            Rect(
                bounds.centerX() - w / 2,
                bounds.centerY() - h / 2,
                bounds.centerX() + w / 2,
                bounds.centerY() + h / 2,
            ),
        )
    }

    fun updateRequest() {
        if (session == null || request == null || cooling) return
        try {
            applyControls(request!!, false)
            val fps = if (videoMode) videoChoice!!.fps else requestedPreviewCameraFps
            if (session is CameraConstrainedHighSpeedCaptureSession) {
                request!!.set<Range<Int>?>(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range<Int>(fps, fps),
                )
                val high = session as CameraConstrainedHighSpeedCaptureSession
                high.setRepeatingBurst(
                    high.createHighSpeedRequestList(request!!.build()),
                    timingCallback,
                    gl,
                )
            } else {
                var best: Range<Int>? = null
                val ranges: Array<Range<Int>>? =
                    characteristics!!.get<Array<Range<Int>>?>(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
                    )
                if (ranges != null)
                    for (r in ranges) if (
                        if (rawVideoMode())
                            (r.upper!! >= fps &&
                                (best == null ||
                                    r.upper!! < best.upper!! ||
                                    (r.upper == best.upper && r.lower!! < best.lower!!)))
                        else (r.upper == fps && (best == null || r.lower!! > best.lower!!))
                    )
                        best = r
                if (!videoMode && ranges != null) {
                    var score = Int.MAX_VALUE
                    for (r in ranges) {
                        val candidate =
                            abs(r.upper!! - fps) * 100 +
                                ((if (settings.expertMode) -r.lower!! else r.lower)!!)
                        if (
                            r.upper!! <= fps && candidate < score
                        ) {
                            score = candidate
                            best = r
                        }
                    }
                }
                if (best != null)
                    request!!.set<Range<Int>?>(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        best,
                    )
                session!!.setRepeatingRequest(request!!.build(), timingCallback, gl)
            }
        } catch (e: Exception) {
            error(context.getString(R.string.ui_could_not_apply_camera_settings), e)
        }
    }

    @Throws(IOException::class)
    fun initGl(target: SurfaceTexture?) {
        display = EglLease.acquire()
        val attrs =
            intArrayOf(
                EGL14.EGL_RED_SIZE,
                8,
                EGL14.EGL_GREEN_SIZE,
                8,
                EGL14.EGL_BLUE_SIZE,
                8,
                EGL14.EGL_ALPHA_SIZE,
                8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
                0x3142,
                1,
                EGL14.EGL_NONE,
            )
        val configs = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        check(
            !(!EGL14.eglChooseConfig(
                display,
                attrs,
                0,
                configs,
                0,
                1,
                n,
                0,
            ) || n[0] == 0)
        ) {
            "EGL config"
        }
        eglConfig = configs[0]
        eglContext =
            EGL14.eglCreateContext(
                display,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
        displaySurface = Surface(target)
        window = windowFor(displaySurface)
        current(window)
        val shader = PhotoRenderer.shaderSource(context)
        previewChain = EffectChain(shader, true)
        blitChain = EffectChain(shader, false)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, n, 0)
        maxTexture = n[0]
        Log.i(
            "Signal",
            "GPU=" + GLES20.glGetString(GLES20.GL_RENDERER) + " maxTexture=" + maxTexture,
        )
        deviceProfile.calibrate(context,shader,thermalMonitor)
        GLES20.glViewport(0,0,width,height)
        initCameraTexture()
    }

    fun initCameraTexture() {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        cameraTexture = SurfaceTexture(texture)
        cameraTexture!!.setOnFrameAvailableListener(
            OnFrameAvailableListener { st: SurfaceTexture? -> if (st === cameraTexture) frame() },
            gl,
        )
    }

    fun windowFor(surface: Surface?): EGLSurface? {
        val s =
            EGL14.eglCreateWindowSurface(display, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(s !== EGL14.EGL_NO_SURFACE) { "EGL surface " + EGL14.eglGetError() }
        return s
    }

    fun current(surface: EGLSurface?) {
        check(EGL14.eglMakeCurrent(display, surface, surface, eglContext)) { "EGL current" }
    }

    fun blit(signal: SignalBuffer, w: Int, h: Int) {
        blitChain!!.render(
            signal.texture,
            false,
            IDENTITY,
            cleanFrame,
            w,
            h,
            signal.width,
            signal.height,
            0,
        )
    }

    fun previewPresented(timestamp: Long) {
        if (if (settings.advancedMode) {
                timestamp > presentedFrames.acknowledged() && presentedFrames.acknowledge(timestamp)
            } else {
                val accepted = normalFirstPublishedNs > 0 && timestamp >= normalFirstPublishedNs &&
                    timestamp > normalAcknowledgedNs && timestamp <= normalPublishedNs
                if (accepted) normalAcknowledgedNs = timestamp
                accepted
            })
            gl.post(
                Runnable@{
                    lastPreviewAckMs = SystemClock.elapsedRealtime()
                    if (healthySinceMs == 0L) healthySinceMs = lastPreviewAckMs
                    if (lastPreviewAckMs - healthySinceMs > 2000) reconnectAttempts = 0
                    if (frameSeen && !photoBusy) ready(!rawVideoMode() || rawFrameSeen)
                }
            )
    }

    fun frame() {
        if (!attached || !foreground || cooling || (if (tapInput == null) cameraTexture == null || camera == null
            else tapSource?.ready != true)) return
        try {
            current(window)
            val source = tapSource
            if (source == null) {
                cameraTexture!!.updateTexImage()
                cameraTexture!!.getTransformMatrix(matrix)
                // Freeform windows may retain the activity even on a quarter turn.
                // Freeze orientation during a capture; rebuild sized buffers afterwards.
                val nextRotation = displayDegrees()
                if (!recording && !photoBusy && nextRotation != displayDegrees) {
                    if ((nextRotation - displayDegrees) % 180 != 0) {
                        restart()
                        return
                    }
                    updateOrientation()
                }
                android.opengl.Matrix.multiplyMM(orientedMatrix, 0, matrix, 0, displayMatrix, 0)
            } else source.update()
            val inputTexture = source?.texture ?: texture
            val inputExternal = source?.external ?: true
            val inputMatrix = source?.matrix ?: orientedMatrix
            val timestamp = if (source == null) cameraTexture!!.timestamp else SystemClock.elapsedRealtimeNanos()
            if (timestamp <= lastFrameNs) return
            lastFrameNs = timestamp
            val arrival = SystemClock.elapsedRealtimeNanos()
            val input = faultInputs.frame(timestamp, arrival, recorder)
            faults!!.advance(arrival * 1e-9, input, faultConfig)
            if (previewFaults != null)
                previewFaults!!.advance(
                    arrival * 1e-9,
                    input,
                    previewFaultConfig!!,
                )
            val echoConfig = activeFaultConfig()
            val echoEnabled = settings.experimentalSignals && echoConfig.enabled && echoConfig.echo.enabled &&
                !rawVideoMode() && (videoMode || settings.photoFormat == 0)
            val echo = if (echoEnabled) try {
                timeEcho.sample(timestamp, echoConfig.echo, signalW, signalH) { buffer ->
                    previewChain!!.render(inputTexture, inputExternal, inputMatrix, cleanFrame, buffer.width, buffer.height,
                        signalW, signalH, buffer.fbo, buffer.texture)
                }
            } catch (error: Exception) {
                timeEcho.fail()
                status(context.getString(R.string.echo_memory))
                null
            } catch (error: OutOfMemoryError) {
                timeEcho.fail()
                status(context.getString(R.string.echo_memory))
                null
            } else {
                timeEcho.disable()
                null
            }
            // The GL thread owns these inputs until the next frame. LIGHT JPEG capture can render
            // the latest source once at the selected output size, without enlarging its preview.
            if (settings.lightMode) {
                captureInputTexture = echo?.buffer?.texture ?: inputTexture
                captureInputExternal = echo == null && inputExternal
                (if (echo == null) inputMatrix else IDENTITY).copyInto(captureInputMatrix)
                captureInputWidth = echo?.buffer?.width ?: signalW
                captureInputHeight = echo?.buffer?.height ?: signalH
                captureInputNs = echo?.cameraNs ?: timestamp
            }
            val show = adaptiveLoad.due(lastFrameNs) || !frameSeen
            if (show || recording && !rawVideoMode()) {
                val renderStarted = System.nanoTime()
                val currentState = faultFrame((if (previewEffects == null) effectState else previewEffects)!!)
                val state = if (echo == null) currentState else EffectState.Frame(currentState.ids(),
                    currentState.amount, currentState.parameters, echo.cameraNs, currentState.time,
                    currentState.nodes, currentState.experimental, currentState.injection)
                var didRender = false
                val slot = if (show && settings.advancedMode) presentedFrames.acquire() else null
                val rendered: SignalBuffer = (if (slot == null) encoderScratch else slot.value)!!
                // NETWORK retains processed history. Keep that history at the capture resolution;
                // a small preview must never replace the full-size stalled frame used for a shot.
                val renderSize = PreviewSizing.choose(signalW, signalH, width, height,
                    settings.lightMode, recording && !rawVideoMode(),
                    state.nodes.any { it.id == Effects.CRT && Math.round(it.profile["transportKind"] ?: 0f) == 2 })
                if (slot != null || show && !settings.advancedMode || recording && !rawVideoMode()) {
                    try {
                        rendered.allocate(renderSize.width, renderSize.height)
                        previewChain!!.render(
                            echo?.buffer?.texture ?: inputTexture,
                            echo == null && inputExternal,
                            if (echo == null) inputMatrix else IDENTITY,
                            state,
                            renderSize.width,
                            renderSize.height,
                            echo?.buffer?.width ?: signalW,
                            echo?.buffer?.height ?: signalH,
                            rendered.fbo,
                            rendered.texture,
                        )
                        rendered.frame = state
                        didRender = true
                        renderedFrames++
                    } catch (failure: Exception) {
                        if (slot != null) presentedFrames.abandon(slot)
                        throw failure
                    }
                }
                if (show && didRender && (!settings.advancedMode || slot != null)) {
                    try {
                        blit(rendered, width, height)
                        rendered.presentedAt = System.currentTimeMillis()
                        // Camera clocks may differ from EGL's monotonic clock. The presentation
                        // token
                        // identifies this buffer; the immutable payload retains the original
                        // cameraNs.
                        val presentationNs = System.nanoTime()
                        if (slot != null) presentedFrames.publish(slot, presentationNs)
                        else {
                            if (normalFirstPublishedNs == 0L) normalFirstPublishedNs = presentationNs
                            normalPublishedNs = presentationNs
                        }
                        check(
                            !(!EGLExt.eglPresentationTimeANDROID(
                                display,
                                window,
                                presentationNs,
                            ) || !EGL14.eglSwapBuffers(display, window))
                        ) {
                            "Preview presentation"
                        }
                    } catch (failure: Exception) {
                        if (slot != null) presentedFrames.abandon(slot)
                        throw failure
                    }
                    lastPreviewNs = lastFrameNs
                    adaptiveLoad.presented(lastFrameNs)
                    var shownMask = 0
                    for (id in state.ids()) shownMask = shownMask or (1 shl id)
                    if (shownMask != liveUiMask || arrival - liveUiNs > 150000000L) {
                        liveUiMask = shownMask
                        liveUiNs = arrival
                        val revision = appliedRevision
                        ui.post(
                            Runnable@{
                                if (revision == configRevision.get()) listener.liveFrame(state)
                            }
                        )
                    }
                }
                if (recording && !rawVideoMode()) {
                    current(encoder)
                    blit(rendered, outW, outH)
                    if (encoderTimeOffset == Long.MIN_VALUE) {
                        encoderTimeOffset = System.nanoTime() - lastFrameNs
                    }
                    tapSource?.recordingFrame(lastFrameNs)
                    EGLExt.eglPresentationTimeANDROID(
                        display,
                        encoder,
                        lastFrameNs + encoderTimeOffset,
                    )
                    check(EGL14.eglSwapBuffers(display, encoder)) { "Encoder surface" }
                    current(window)
                }
                if (didRender) adaptiveLoad.rendered((System.nanoTime() - renderStarted) / 1e6)
            }
            if (arrival - faultUiNs > 500000000L) {
                faultUiNs = arrival
                faultStatus = if (faultConfig.enabled) faultInputs.summary() else "LIVE FAULT OFF"
            }
            if (!frameSeen) {
                frameSeen = true
                ready(
                    previewAcknowledged() > 0 &&
                        !photoBusy &&
                        (!rawVideoMode() || rawFrameSeen)
                )
                status("LIVE · " + description())
                fpsStart = lastFrameNs
                frameCount = 0
            }
            frameCount++
            val elapsed = lastFrameNs - fpsStart
            if (elapsed >= 2000000000L) {
                val measured = frameCount * 1e9f / elapsed
                ui.post(Runnable@{ listener.fps(measured) })
                frameCount = 0
                fpsStart = lastFrameNs
            }
        } catch (e: Exception) {
            if (recording) stopVideo()
            error(context.getString(R.string.ui_image_processing_error), e)
        }
    }

    /** Full-resolution one-shot rendering from the latest GL input; never upscale a LIGHT preview. */
    internal fun renderLightPhoto(target: SignalBuffer) {
        check(settings.lightMode && captureInputTexture != 0 && captureInputNs > 0)
        current(window)
        val current = faultFrame((previewEffects ?: effectState)!!)
        val frame = EffectState.Frame(current.ids(), current.amount, current.parameters, captureInputNs,
            current.time, current.nodes, current.experimental, current.injection)
        val chain = lightPhotoChain ?: EffectChain(PhotoRenderer.shaderSource(context), true).also { lightPhotoChain = it }
        target.allocate(outW, outH)
        chain.render(captureInputTexture, captureInputExternal, captureInputMatrix, frame,
            outW, outH, captureInputWidth, captureInputHeight, target.fbo, target.texture)
        target.frame = frame
        target.presentedAt = System.currentTimeMillis()
    }

    internal class PendingPhoto(e: GlitchEngine, displayed: SignalBuffer) {
        val cameraInfo: CameraCharacteristics?
        val settings: CaptureSettings
        val choice: CameraOptions.Photo?
        val rotation: Int
        val frame: EffectState.Frame
        val front: Boolean
        val location: Location?
        val taken: Long
        var bytes: ByteArray? = null
        var signal: Bitmap? = null
        var result: TotalCaptureResult? = null
        var dispatched: Boolean = false

        init {
            cameraInfo = e.characteristics
            settings = CaptureSettings(e.settings)
            choice = e.photoChoice
            frame = requireNotNull(displayed.frame)
            front = e.front
            rotation = e.captureRotation
            location = if (settings.location) e.position.get() else null
            taken =
                if (settings.photoFormat == 0) displayed.presentedAt else System.currentTimeMillis()
        }

        fun description(): String {
            return BuildConfig.APP_NAME +
                " " +
                BuildConfig.VERSION_NAME +
                " | " +
                (if (settings.photoFormat == 1) "RAW ORIGINAL"
                else Effects.chainName(frame.ids()) + " | " + frame.describe()) +
                " | " +
                (if (settings.photoFormat == 0) if (settings.advancedMode) "Displayed RGB signal" else if (settings.lightMode) "Full-resolution RGB capture (LIGHT)" else "Processed RGB capture"
                else if (settings.photoFormat == 1) "Separate RAW exposure"
                else "Separate RAW exposure, latched fault state; RGB preview approximate")
        }
    }

    @JvmOverloads
    fun photo(displayedTimestamp: Long = previewAcknowledged()) {
        val lease = if (settings.advancedMode) presentedFrames.reserve(displayedTimestamp) else null
        gl.post(
            Runnable@{
                if (
                    !frameSeen ||
                        photoBusy ||
                        videoMode ||
                        cooling ||
                        (if (settings.advancedMode) !presentedFrames.valid(lease) else encoderScratch.frame == null) ||
                        (settings.photoFormat != 0 && stillReader == null)
                ) {
                    presentedFrames.release(lease)
                    ready(frameSeen && !photoBusy && previewAcknowledged() > 0)
                    return@Runnable
                }
                photoBusy = true
                ready(false)
                // GL serializes this snapshot/readback before any subsequent camera frame.
                val fullResolution = SignalBuffer()
                var readback: Bitmap? = null
                try {
                    var captured = if (settings.advancedMode) lease!!.value else encoderScratch
                    if (settings.lightMode && settings.photoFormat == 0 &&
                        (captured.width != outW || captured.height != outH)) {
                        renderLightPhoto(fullResolution)
                        captured = fullResolution
                    }
                    val shot = PendingPhoto(this, captured)
                    pending = shot
                    if (settings.photoFormat == 0) {
                        current(window)
                        shot.signal = captured.read()
                        readback = shot.signal
                        shot.result = signalMetadata.get(shot.frame.cameraNs)
                        shot.dispatched = true
                        pending = null
                        status(context.getString(R.string.fault_capture_signal))
                        files.execute(Runnable@{ savePhoto(shot) })
                        return@Runnable
                    }
                    status(context.getString(R.string.ui_capturing_at_full_resolution))
                    val still = camera!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    still.addTarget(stillReader!!.surface)
                    applyControls(still, true)
                    if (photoChoice!!.maximumPixelMode)
                        still.set<Int?>(
                            CaptureRequest.SENSOR_PIXEL_MODE,
                            CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION,
                        )
                    session!!.capture(
                        still.build(),
                        object : CaptureCallback() {
                            override fun onCaptureCompleted(
                                s: CameraCaptureSession,
                                r: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                if (pending == shot) {
                                    shot.result = result
                                    dispatchPhoto(shot)
                                }
                            }

                            override fun onCaptureFailed(
                                s: CameraCaptureSession,
                                r: CaptureRequest,
                                failure: CaptureFailure,
                            ) {
                                if (pending == shot) {
                                    pending = null
                                    photoBusy = false
                                    ready(frameSeen)
                                    status(
                                        context.getString(
                                            R.string
                                                .ui_could_not_capture_the_photo_try_lower_settings
                                        )
                                    )
                                }
                            }
                        },
                        gl,
                    )
                    gl.postDelayed(
                        Runnable@{
                            if (pending == shot && !shot.dispatched) {
                                pending = null
                                photoBusy = false
                                ready(frameSeen)
                                status(context.getString(R.string.ui_photo_capture_timed_out))
                            }
                        },
                        20000,
                    )
                } catch (e: Exception) {
                    readback?.recycle()
                    pending = null
                    photoBusy = false
                    ready(frameSeen)
                    error(context.getString(R.string.ui_could_not_capture), e)
                } catch (e: OutOfMemoryError) {
                    readback?.recycle()
                    pending = null
                    photoBusy = false
                    ready(frameSeen)
                    status(
                        context.getString(R.string.ui_not_enough_memory_to_process_the_photo_lower)
                    )
                } finally {
                    fullResolution.release()
                    lightPhotoChain?.releaseBuffers()
                    presentedFrames.release(lease)
                }
            }
        )
    }

    fun rawImage(reader: ImageReader, ticket: Int) {
        try {
            reader.acquireNextImage().use { image ->
                if (image == null || ticket != generation) return
                if (rawProbe != null) rawProbe!!.image(image)
                else if (rawRecorder != null) rawRecorder!!.image(image)
            }
        } catch (e: Exception) {
            if (ticket == generation) {
                if (rawRecorder != null) rawRecorder!!.fail(e)
                else failRawSession(context.getString(R.string.ui_could_not_read_raw_frames))
            }
        }
    }

    fun imageAvailable(reader: ImageReader, ticket: Int) {
        try {
            reader.acquireNextImage().use { image ->
                if (image == null || ticket != generation || pending == null) return
                val shot = pending
                val plane = image.planes[0]
                val source = plane.buffer
                if (image.format == ImageFormat.JPEG) {
                    shot!!.bytes = ByteArray(source.remaining()).also { source.get(it) }
                } else {
                    val w = image.width
                    val h = image.height
                    val rowStride = plane.rowStride
                    val pixelStride = plane.pixelStride
                    shot!!.bytes = ByteArray(w * h * 2)
                    for (y in 0..<h) for (x in 0..<w) {
                        val src = y * rowStride + x * pixelStride
                        val dst = (y * w + x) * 2
                        shot.bytes!![dst] = source.get(src)
                        shot.bytes!![dst + 1] = source.get(src + 1)
                    }
                }
                dispatchPhoto(shot)
            }
        } catch (e: Exception) {
            pending = null
            photoBusy = false
            ready(frameSeen)
            error(context.getString(R.string.ui_could_not_read_capture_data), e)
        }
    }

    fun dispatchPhoto(shot: PendingPhoto) {
        if (shot.bytes == null || shot.result == null || shot.dispatched) return
        shot.dispatched = true
        pending = null
        status(
            if (shot.settings.photoFormat == 0)
                context.getString(R.string.ui_saving_full_resolution_effects)
            else context.getString(R.string.ui_saving_raw_data)
        )
        files.execute(Runnable@{ savePhoto(shot) })
    }

    fun toggleVideo(sound: Boolean) {
        if (recording) { gl.post { stopVideo() }; return }
        gl.post {
            if (foreground && attached && videoMode && !photoBusy) startVideo(sound)
        }
    }

    fun startVideo(requestedSound: Boolean) {
        val sound = requestedSound && tapSource?.hasAudio != true
        if (previewEffects != null) {
            status(context.getString(R.string.fault_recording_draft))
            ready(true)
            return
        }
        if (!foreground || !frameSeen || !attached || recording || photoBusy || !videoMode || cooling) return
        if (rawVideoMode()) {
            if (!rawFrameSeen || rawReader == null) {
                status(context.getString(R.string.ui_checking_raw_output))
                ready(false)
                return
            }
            rawRecorder = RawVideoRecorder(this)
            recording = true
            ready(true)
            ui.post(Runnable@{ listener.recording(true) })
            gl.post(storageWatch)
            return
        }
        ready(false)
        try {
            if (sound) {
                faultInputs.stopMic()
                recorderAudio = true
                faultInputs.configure(faultConfig, attached && !cooling, true)
            }
            videoTaken = System.currentTimeMillis()
            videoUri = createMedia("mp4", videoTaken)
            videoFd = context.contentResolver.openFileDescriptor(videoUri!!, "w")
            recorder = MediaRecorder(context)
            if (sound) recorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            recorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder!!.setOutputFile(videoFd!!.fileDescriptor)
            recorder!!.setVideoEncoder(
                if (settings.codec == "video/hevc") MediaRecorder.VideoEncoder.HEVC
                else MediaRecorder.VideoEncoder.H264
            )
            recorder!!.setVideoSize(outW, outH)
            recorder!!.setVideoFrameRate(videoChoice!!.fps)
            val bitrate = options!!.bitrate(requireNotNull(videoChoice), settings)
            recorder!!.setVideoEncodingBitRate(bitrate)
            recorder!!.setMaxFileSize(segmentBytes)
            if (sound) {
                recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                val audio = RecordingAudio.supported(outW, outH, settings.resolutionAudio)
                recorder!!.setAudioSamplingRate(audio.sampleRate)
                recorder!!.setAudioEncodingBitRate(audio.bitRate)
                recorder!!.setAudioChannels(1)
            }
            val geo = if (settings.location) position.get() else null
            if (geo != null)
                recorder!!.setLocation(
                    geo.latitude.toFloat(),
                    geo.longitude.toFloat(),
                )
            recorder!!.setOnErrorListener(
                MediaRecorder.OnErrorListener { r: MediaRecorder?, what: Int, extra: Int ->
                    gl.post(
                        Runnable@{
                            if (recording) {
                                stopVideo()
                                status(
                                    context.getString(
                                        R.string.ui_recording_interrupted_check_the_saved_files
                                    )
                                )
                            }
                        }
                    )
                }
            )
            recorder!!.setOnInfoListener(
                OnInfoListener@{ r: MediaRecorder?, what: Int, extra: Int ->
                    if (!recording) return@OnInfoListener
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING)
                        prepareNextSegment()
                    else if (what == MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED)
                        advanceSegment()
                    else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        stopVideo()
                        status(
                            context.getString(
                                R.string.ui_could_not_start_the_next_file_recording_saved
                            )
                        )
                    }
                }
            )
            recorder!!.prepare()
            encoderSurface = recorder!!.surface
            encoder = windowFor(encoderSurface)
            recorder!!.start()
            encoderTimeOffset = Long.MIN_VALUE
            recording = true
            tapSource?.recordingStarted()
            if (!recording) return
            ready(true)
            ui.post(Runnable@{ listener.recording(true) })
            gl.post(storageWatch)
            Log.i(
                "Signal",
                "Recording started " +
                    outW +
                    "x" +
                    outH +
                    " fps=" +
                    videoChoice!!.fps +
                    " codec=" +
                    settings.codec +
                    " bitrate=" +
                    bitrate +
                    " audio=" +
                    sound +
                    " GPS=" +
                    (geo != null),
            )
            if (settings.location && geo == null)
                status(context.getString(R.string.ui_no_gps_fix_video_will_have_no_location))
        } catch (e: Exception) {
            releaseRecorder()
            discard(videoUri)
            videoUri = null
            ready(frameSeen)
            ui.post(Runnable@{ listener.recording(false) })
            error(context.getString(R.string.ui_could_not_start_recording_try_lower_settings), e)
        }
    }

    fun prepareNextSegment() {
        if (nextUri != null) return
        try {
            nextTaken = System.currentTimeMillis()
            nextUri = createMedia("mp4", nextTaken)
            nextFd = context.contentResolver.openFileDescriptor(nextUri!!, "w")
            recorder!!.setNextOutputFile(nextFd!!.fileDescriptor)
        } catch (e: Exception) {
            discard(nextUri)
            nextUri = null
            if (nextFd != null)
                try {
                    nextFd!!.close()
                } catch (ignored: IOException) {}
            nextFd = null
            error(context.getString(R.string.ui_could_not_prepare_the_next_recording_file), e)
        }
    }

    fun advanceSegment() {
        if (nextUri == null) return
        val completed = videoUri
        val taken = videoTaken
        try {
            videoFd!!.close()
        } catch (ignored: IOException) {}
        videoUri = nextUri
        videoFd = nextFd
        videoTaken = nextTaken
        nextUri = null
        nextFd = null
        try {
            finishRecordedVideo(completed!!,taken,tapSource?.recordedAudio(),false)
            Log.i("Signal", "Recording continued in next segment")
        } catch (e: Exception) {
            error(context.getString(R.string.ui_could_not_publish_the_recording_segment), e)
        }
    }

    fun stopVideo() {
        if (!recording) return
        if (rawRecorder != null) {
            recording = false
            photoBusy = true
            gl.removeCallbacks(storageWatch)
            ready(false)
            val completed = rawRecorder
            rawRecorder = null
            completed!!.stop()
            ui.post(Runnable@{ listener.recording(false) })
            status(context.getString(R.string.ui_finalizing_raw_sequence))
            return
        }
        recording = false
        tapSource?.recordingStopped()
        val sourceAudio = tapSource?.recordedAudio()
        if (sourceAudio != null) photoBusy = true
        gl.removeCallbacks(storageWatch)
        ready(false)
        val result = videoUri
        val taken = videoTaken
        var ok = false
        try {
            recorder!!.stop()
            ok = true
        } catch (e: Exception) {
            error(context.getString(R.string.ui_recording_was_too_short_or_could_not_be), e)
        } finally {
            releaseRecorder()
            videoUri = null
            ready(frameSeen && attached && tapSource?.ready != false && sourceAudio == null)
            ui.post(Runnable@{ listener.recording(false) })
        }
        if (ok)
            try {
                finishRecordedVideo(result!!,taken,sourceAudio,true)
            } catch (e: Exception) {
                if (sourceAudio != null) photoBusy = false
                discard(result)
                error(context.getString(R.string.ui_could_not_save_the_video), e)
            }
        else {
            if (sourceAudio != null) photoBusy = false
            discard(result)
        }
    }

    private fun finishRecordedVideo(uri: Uri, taken: Long, audio: TapAudio.Job?, final: Boolean) {
        if (audio == null) { publish(uri,true,taken);return }
        if (final) status(context.getString(R.string.tap_audio_saving))
        files.execute {
            var file: File? = null
            var combined: Uri? = null
            try {
                file = TapAudio.remux(context,uri,audio)
                combined = createMedia("mp4",taken)
                context.contentResolver.openOutputStream(combined,"w")!!.use { output ->
                    file.inputStream().use { input -> copy(input,output) }
                }
                publish(combined,true,taken)
                discard(uri)
            } catch (error: Exception) {
                discard(combined)
                // Keep the already finished video if its source audio cannot be read/converted.
                try { publish(uri,true,taken) } catch (save: Exception) { discard(uri) }
                error(context.getString(R.string.tap_audio_failed),error)
            } finally {
                file?.delete()
                if (final) gl.post {
                    photoBusy = false
                    ready(frameSeen && attached && !cooling && tapSource?.ready != false)
                }
            }
        }
    }

    fun releaseRecorder() {
        if (display !== EGL14.EGL_NO_DISPLAY && window !== EGL14.EGL_NO_SURFACE) current(window)
        if (encoder !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, encoder)
            encoder = EGL14.EGL_NO_SURFACE
        }
        if (encoderSurface != null) {
            encoderSurface!!.release()
            encoderSurface = null
        }
        if (recorder != null) {
            recorder!!.release()
            recorder = null
        }
        if (videoFd != null) {
            try {
                videoFd!!.close()
            } catch (ignored: IOException) {}
            videoFd = null
        }
        if (nextFd != null) {
            try {
                nextFd!!.close()
            } catch (ignored: IOException) {}
            nextFd = null
        }
        discard(nextUri)
        nextUri = null
        recorderAudio = false
        faultInputs.configure(faultConfig, attached && !cooling, false)
    }

    fun closeCamera() {
        gl.removeCallbacks(tapTick)
        if (recording && tapSource != null) stopVideo()
        tapSource?.close()
        tapSource = null
        timeEcho.reset()
        healthySinceMs = 0
        if (rawProbe != null) {
            rawProbe!!.stop()
            rawProbe = null
        }
        rawFrameSeen = false
        faultInputs.resetTiming()
        generation++
        presentedFrames.clear()
        normalPublishedNs = 0
        normalFirstPublishedNs = 0
        normalAcknowledgedNs = 0
        encoderScratch.frame = null
        captureInputTexture = 0
        captureInputNs = 0
        signalMetadata.clear()
        frameSeen = false
        adaptiveLoad.resetClock()
        lastFrameNs = 0
        lastPreviewNs = 0
        ready(false)
        if (recording) stopVideo()
        if (pending != null) {
            pending = null
            photoBusy = false
            status(
                context.getString(
                    R.string.ui_capture_cancelled_because_the_camera_closed_before_completion
                )
            )
        }
        if (session != null) {
            session!!.close()
            session = null
        }
        if (camera != null) {
            camera!!.close()
            camera = null
        }
        if (cameraSurface != null) {
            cameraSurface!!.release()
            cameraSurface = null
        }
        if (stillReader != null) {
            stillReader!!.close()
            stillReader = null
        }
        if (rawReader != null) {
            rawReader!!.close()
            rawReader = null
        }
        request = null
    }

    fun close() {
        gl.removeCallbacks(previewWatch)
        gl.removeCallbacks(reconnectCamera)
        reconnectPending = false
        lastPreviewAckMs = 0
        gl.removeCallbacks(thermalPoll)
        displayTarget = null
        attached = false
        closeCamera()
        faultInputs.stop()
        faults!!.reset()
        if (previewFaults != null) previewFaults!!.reset()
        if (cameraTexture != null) {
            cameraTexture!!.setOnFrameAvailableListener(null)
            cameraTexture!!.release()
            cameraTexture = null
        }
        if (display !== EGL14.EGL_NO_DISPLAY) {
            if (eglContext !== EGL14.EGL_NO_CONTEXT && window !== EGL14.EGL_NO_SURFACE) {
                current(window)
                for (buffer in presentedFrames.values()) buffer.release()
                encoderScratch.release()
                timeEcho.release()
                if (previewChain != null) previewChain!!.release()
                lightPhotoChain?.release()
                lightPhotoChain = null
                if (blitChain != null) blitChain!!.release()
                blitChain = null
                previewChain = blitChain
            }
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            if (window !== EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, window)
            if (eglContext !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EglLease.release()
            EGL14.eglReleaseThread()
        }
        display = EGL14.EGL_NO_DISPLAY
        window = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        if (displaySurface != null) {
            displaySurface!!.release()
            displaySurface = null
        }
    }

    companion object {
        val IDENTITY: FloatArray =
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

        @Throws(IOException::class)
        fun copy(`in`: InputStream, out: OutputStream) {
            val b = ByteArray(65536)
            var n: Int
            while ((`in`.read(b).also { n = it }) != -1) out.write(b, 0, n)
        }
    }
}
