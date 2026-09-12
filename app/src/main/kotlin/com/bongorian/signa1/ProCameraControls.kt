package com.bongorian.signa1

import android.hardware.camera2.CameraCharacteristics as C
import android.hardware.camera2.CaptureRequest as R
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult

/** Writes only keys advertised for this exact logical/physical request target. */
internal class ProCameraControls(private val lens: CameraLens) {
    private val cc = lens.characteristics
    private val keys = (if (lens.physicalId == null) lens.logical.availableCaptureRequestKeys
        else lens.logical.availablePhysicalCameraRequestKeys).orEmpty().toSet()
    private val defaults = mutableMapOf<Int, R>()
    private val physicalDefaults = mutableMapOf<Int, Map<R.Key<*>, Any?>>()
    private fun supports(vararg wanted: R.Key<*>) = wanted.all { it in keys }
    private fun ints(key: C.Key<IntArray>): List<Int> = cc.get(key)?.toList().orEmpty()
    private fun floats(key: C.Key<FloatArray>): List<Float> = cc.get(key)?.filter { it.isFinite() && it >= 0 }?.distinct()?.sorted().orEmpty()
    private val manualSensor = C.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR in ints(C.REQUEST_AVAILABLE_CAPABILITIES)
    private val manual = manualSensor &&
        supports(R.CONTROL_AE_MODE, R.SENSOR_EXPOSURE_TIME, R.SENSOR_SENSITIVITY, R.SENSOR_FRAME_DURATION) &&
        R.CONTROL_AE_MODE_OFF in ints(C.CONTROL_AE_AVAILABLE_MODES)
    val capabilities = ProCameraCapabilities(
        exposure = if (manual) cc.get(C.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { it.lower..it.upper } else null,
        maxFrameNs = cc.get(C.SENSOR_INFO_MAX_FRAME_DURATION) ?: 0,
        iso = if (manual) cc.get(C.SENSOR_INFO_SENSITIVITY_RANGE)?.let { it.lower..it.upper } else null,
        ev = if (supports(R.CONTROL_AE_MODE, R.CONTROL_AE_EXPOSURE_COMPENSATION) &&
            R.CONTROL_AE_MODE_ON in ints(C.CONTROL_AE_AVAILABLE_MODES) && (cc.get(C.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0f) > 0) cc.get(C.CONTROL_AE_COMPENSATION_RANGE)
            ?.takeIf { it.upper > it.lower }?.let { it.lower..it.upper } else null,
        evStep = cc.get(C.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0f,
        aeLock = supports(R.CONTROL_AE_LOCK, R.CONTROL_AE_MODE) && R.CONTROL_AE_MODE_ON in ints(C.CONTROL_AE_AVAILABLE_MODES) && cc.get(C.CONTROL_AE_LOCK_AVAILABLE) == true,
        whiteBalance = if (supports(R.CONTROL_AWB_MODE)) ints(C.CONTROL_AWB_AVAILABLE_MODES).filter { it in 1..8 } else emptyList(),
        awbLock = supports(R.CONTROL_AWB_LOCK, R.CONTROL_AWB_MODE) && R.CONTROL_AWB_MODE_AUTO in ints(C.CONTROL_AWB_AVAILABLE_MODES) && cc.get(C.CONTROL_AWB_LOCK_AVAILABLE) == true,
        focusMax = if (manualSensor && supports(R.CONTROL_AF_MODE, R.LENS_FOCUS_DISTANCE) && R.CONTROL_AF_MODE_OFF in ints(C.CONTROL_AF_AVAILABLE_MODES))
            (cc.get(C.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f).takeIf { it.isFinite() && it > 0 } ?: 0f else 0f,
        focusCalibrated = cc.get(C.LENS_INFO_FOCUS_DISTANCE_CALIBRATION) == C.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED,
        apertures = if (manual && supports(R.LENS_APERTURE)) floats(C.LENS_INFO_AVAILABLE_APERTURES).filter { it > 0 }.takeIf { it.size > 1 }.orEmpty() else emptyList(),
        filterDensities = if (supports(R.LENS_FILTER_DENSITY)) floats(C.LENS_INFO_AVAILABLE_FILTER_DENSITIES).takeIf { it.size > 1 }.orEmpty() else emptyList(),
        opticalStabilization = supports(R.LENS_OPTICAL_STABILIZATION_MODE) && ints(C.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION).containsAll(listOf(0, 1)),
        videoStabilization = supports(R.CONTROL_VIDEO_STABILIZATION_MODE) && ints(C.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES).containsAll(listOf(0, 1)),
        antiBanding = if (supports(R.CONTROL_AE_ANTIBANDING_MODE)) ints(C.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES) else emptyList(),
    )

    fun remember(template: Int, builder: R.Builder) {
        defaults[template] = builder.build()
        lens.physicalId?.let { id -> physicalDefaults[template] = keys.associateWith { builder.getPhysicalCameraKey(it, id) } }
    }

    fun autofocus(builder: R.Builder, mode: Int) { write(builder, R.CONTROL_AF_MODE, mode) }

    private fun <T> write(builder: R.Builder, key: R.Key<T>, value: T?) {
        if (key !in keys) return
        if (lens.physicalId == null) builder.set(key, value)
        else builder.setPhysicalCameraKey(key, value, lens.physicalId)
    }
    private fun <T> restore(builder: R.Builder, template: Int, key: R.Key<T>) {
        @Suppress("UNCHECKED_CAST")
        val value = if (lens.physicalId == null) defaults[template]?.get(key)
            else physicalDefaults[template]?.get(key) as T?
        write(builder, key, value)
    }

    fun reset(builder: R.Builder, template: Int) {
        restore(builder, template, R.CONTROL_AE_MODE)
        restore(builder, template, R.CONTROL_AE_EXPOSURE_COMPENSATION)
        restore(builder, template, R.CONTROL_AE_LOCK)
        restore(builder, template, R.SENSOR_EXPOSURE_TIME)
        restore(builder, template, R.SENSOR_SENSITIVITY)
        restore(builder, template, R.SENSOR_FRAME_DURATION)
        restore(builder, template, R.CONTROL_AWB_MODE)
        restore(builder, template, R.CONTROL_AWB_LOCK)
        restore(builder, template, R.CONTROL_AF_MODE)
        restore(builder, template, R.LENS_FOCUS_DISTANCE)
        restore(builder, template, R.LENS_APERTURE)
        restore(builder, template, R.LENS_FILTER_DENSITY)
        restore(builder, template, R.LENS_OPTICAL_STABILIZATION_MODE)
        restore(builder, template, R.CONTROL_VIDEO_STABILIZATION_MODE)
        restore(builder, template, R.CONTROL_AE_ANTIBANDING_MODE)
    }

    fun apply(builder: R.Builder, desired: ProCameraState, context: ProCameraContext): ProCameraState {
        val state = desired.resolve(capabilities, context)
        if (context.highSpeed) return state
        if (state.manualExposure) {
            write(builder, R.CONTROL_AE_MODE, R.CONTROL_AE_MODE_OFF)
            write(builder, R.SENSOR_EXPOSURE_TIME, state.exposureNs)
            write(builder, R.SENSOR_SENSITIVITY, state.iso)
            write(builder, R.SENSOR_FRAME_DURATION, capabilities.frameDuration(state, context))
        } else if (R.CONTROL_AE_MODE_ON in ints(C.CONTROL_AE_AVAILABLE_MODES)) {
            write(builder, R.CONTROL_AE_MODE, R.CONTROL_AE_MODE_ON)
        }
        if (capabilities.ev != null) write(builder, R.CONTROL_AE_EXPOSURE_COMPENSATION, state.ev)
        if (capabilities.aeLock) write(builder, R.CONTROL_AE_LOCK, state.aeLock)
        if (capabilities.whiteBalance.isNotEmpty()) write(builder, R.CONTROL_AWB_MODE, state.whiteBalance)
        if (capabilities.awbLock) write(builder, R.CONTROL_AWB_LOCK, state.awbLock)
        if (state.manualFocus) {
            write(builder, R.CONTROL_AF_MODE, R.CONTROL_AF_MODE_OFF)
            write(builder, R.LENS_FOCUS_DISTANCE, state.focus)
        }
        state.aperture?.let { write(builder, R.LENS_APERTURE, it) }
        state.filterDensity?.let { write(builder, R.LENS_FILTER_DENSITY, it) }
        if (state.stabilization >= 0) {
            if (capabilities.opticalStabilization) write(builder, R.LENS_OPTICAL_STABILIZATION_MODE, if (state.stabilization == 1) 1 else 0)
            if (capabilities.videoStabilization) write(builder, R.CONTROL_VIDEO_STABILIZATION_MODE, if (state.stabilization == 2) 1 else 0)
        }
        if (state.antiBanding >= 0) write(builder, R.CONTROL_AE_ANTIBANDING_MODE, state.antiBanding)
        return state
    }

    companion object {
        fun reading(result: TotalCaptureResult) = ProCameraReading(
            result.get(CaptureResult.SENSOR_TIMESTAMP) ?: 0,
            result.get(CaptureResult.SENSOR_EXPOSURE_TIME), result.get(CaptureResult.SENSOR_SENSITIVITY),
            result.get(CaptureResult.SENSOR_FRAME_DURATION), result.get(CaptureResult.LENS_FOCUS_DISTANCE),
            result.get(CaptureResult.LENS_APERTURE), result.get(CaptureResult.CONTROL_AWB_MODE),
            result.get(CaptureResult.CONTROL_AE_LOCK), result.get(CaptureResult.CONTROL_AWB_LOCK),
            result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE), result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE),
        )
    }
}
