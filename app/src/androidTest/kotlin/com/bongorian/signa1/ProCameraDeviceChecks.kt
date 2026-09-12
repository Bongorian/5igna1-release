package com.bongorian.signa1

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Camera2 requests against an offscreen YUV sink. No Activity, UI automation or image capture files. */
internal object ProCameraDeviceChecks {
    fun run(context: Context): String {
        val manager = context.getSystemService(CameraManager::class.java)
        val lenses = CameraLenses.read(manager)
        check(lenses.isNotEmpty())
        val lens = lenses.firstOrNull { !it.front && it.physicalId == null } ?: requireNotNull(lenses.firstOrNull { it.physicalId == null })
        val controls = ProCameraControls(lens)
        val capabilities = controls.capabilities
        val thread = HandlerThread("ProCameraChecks").apply { start() }
        val handler = Handler(thread.looper)
        val device = AtomicReference<CameraDevice?>()
        val session = AtomicReference<CameraCaptureSession?>()
        val failure = AtomicReference<Throwable?>()
        var reader: ImageReader? = null
        fun wait(latch: CountDownLatch, label: String) {
            check(latch.await(15, TimeUnit.SECONDS)) { "$label timed out: ${failure.get()}" }
            failure.get()?.let { throw AssertionError(label, it) }
        }
        try {
            val opened = CountDownLatch(1)
            manager.openCamera(lens.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) { device.set(camera); opened.countDown() }
                override fun onDisconnected(camera: CameraDevice) { camera.close(); failure.set(IllegalStateException("Disconnected")); opened.countDown() }
                override fun onError(camera: CameraDevice, error: Int) { camera.close(); failure.set(IllegalStateException("Camera error $error")); opened.countDown() }
            }, handler)
            wait(opened, "camera open")
            val camera = requireNotNull(device.get())
            val sizes = lens.characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.YUV_420_888)
            val size = sizes.filter { it.width >= 320 && it.height >= 240 }.minByOrNull { it.width * it.height } ?: sizes.first()
            reader = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 3)
            reader.setOnImageAvailableListener({ it.acquireLatestImage()?.close() }, handler)
            val configured = CountDownLatch(1)
            camera.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(value: CameraCaptureSession) { session.set(value); configured.countDown() }
                override fun onConfigureFailed(value: CameraCaptureSession) { failure.set(IllegalStateException("Session rejected")); configured.countDown() }
            }, handler)
            wait(configured, "session")
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(reader.surface)
            controls.remember(CameraDevice.TEMPLATE_PREVIEW, builder)
            val baseline = builder.build()
            fun capture(tag: String): TotalCaptureResult {
                val complete = CountDownLatch(1)
                val latest = AtomicReference<TotalCaptureResult?>()
                var frames = 0
                builder.setTag(tag)
                session.get()!!.setRepeatingRequest(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                        if (++frames >= 8) { latest.set(result); complete.countDown() }
                    }
                    override fun onCaptureFailed(s: CameraCaptureSession, r: CaptureRequest, result: CaptureFailure) {
                        failure.set(IllegalStateException("Capture failed: ${result.reason}")); complete.countDown()
                    }
                }, handler)
                wait(complete, tag)
                session.get()!!.stopRepeating()
                return requireNotNull(latest.get())
            }
            val automatic = capture("auto")
            val desired = ProCameraState(manualExposure = true, exposureNs = 8_000_000L, iso = 200,
                manualFocus = capabilities.focusMax > 0, focus = 0f,
                whiteBalance = if (5 in capabilities.whiteBalance) 5 else 1)
            val resolved = controls.apply(builder, desired, ProCameraContext())
            val manual = capture("pro")
            if (resolved.manualExposure) {
                check(manual.get(CaptureResult.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_OFF)
                val exposure = manual.get(CaptureResult.SENSOR_EXPOSURE_TIME)!!
                check(kotlin.math.abs(exposure - resolved.exposureNs) <= maxOf(100_000L, resolved.exposureNs / 20)) { "Exposure $exposure vs ${resolved.exposureNs}" }
                check(manual.get(CaptureResult.SENSOR_SENSITIVITY) == resolved.iso)
            }
            if (capabilities.whiteBalance.isNotEmpty()) check(manual.get(CaptureResult.CONTROL_AWB_MODE) == resolved.whiteBalance)
            controls.reset(builder, CameraDevice.TEMPLATE_PREVIEW)
            for (key in listOf(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.SENSOR_EXPOSURE_TIME,
                CaptureRequest.SENSOR_SENSITIVITY, CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.LENS_FOCUS_DISTANCE, CaptureRequest.SENSOR_FRAME_DURATION)) {
                check(builder.get(key) == baseline.get(key)) { "AUTO did not restore $key" }
            }
            val restored = capture("restored")
            check(restored.get(CaptureResult.CONTROL_AE_MODE) == automatic.get(CaptureResult.CONTROL_AE_MODE))
            check(restored.get(CaptureResult.CONTROL_AWB_MODE) == automatic.get(CaptureResult.CONTROL_AWB_MODE))
            return "PASS offscreen Camera2 ${lens.key}; manual=${resolved.manualExposure}, exposure=${manual.get(CaptureResult.SENSOR_EXPOSURE_TIME)}, ISO=${manual.get(CaptureResult.SENSOR_SENSITIVITY)}, WB=${manual.get(CaptureResult.CONTROL_AWB_MODE)}; AUTO request and result restored"
        } finally {
            session.get()?.close()
            device.get()?.close()
            reader?.close()
            thread.quitSafely()
            thread.join(3000)
        }
    }
}
