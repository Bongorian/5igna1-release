package com.bongorian.signa1

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import java.util.Locale
import kotlin.math.hypot

/** Only devices and physical sensors advertised to this application by Camera2. */
internal data class CameraLens(
    val cameraId: String,
    val physicalId: String?,
    val characteristics: CameraCharacteristics,
    val logical: CameraCharacteristics,
) {
    val key = if (physicalId == null) "camera:$cameraId" else "physical:$cameraId:$physicalId"
    val front = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
    val automatic = physicalId == null && characteristics.physicalCameraIds.isNotEmpty()
    val focalLengths = OpticalFocal.values(characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf())
    val focalMm = focalLengths.firstOrNull()
    val opticalFocals: List<Float> get() = if (!automatic &&
        (if (physicalId == null) logical.availableCaptureRequestKeys
         else logical.availablePhysicalCameraRequestKeys)?.contains(android.hardware.camera2.CaptureRequest.LENS_FOCAL_LENGTH) == true)
        focalLengths else emptyList()
    val primary = physicalId == null || focalMm == logical.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
    val equivalentMm: Double? = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let {
        val diagonal = hypot(it.width.toDouble(),it.height.toDouble())
        focalMm?.takeIf { focal -> focal > 0 && diagonal > 0 }?.let { focal -> focal*43.2666/diagonal }
    }
    fun label(context: Context): String {
        val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> R.string.lens_front
            CameraCharacteristics.LENS_FACING_BACK -> R.string.lens_back
            else -> R.string.lens_external
        }
        val type = when {
            automatic -> R.string.lens_auto
            front || equivalentMm == null -> R.string.lens_camera
            equivalentMm < 20 -> R.string.lens_ultrawide
            equivalentMm <= 40 -> R.string.lens_wide
            else -> R.string.lens_telephoto
        }
        return context.getString(facing)+" · "+context.getString(type)
    }
    fun detail(context: Context): String = buildString {
        focalMm?.let { append(String.format(Locale.US,"%.2f mm",it)) }
        equivalentMm?.let { append(" · ").append(context.getString(R.string.lens_equivalent,it)) }
    }.trim(' ','·')
}

internal object CameraLenses {
    fun read(manager: CameraManager): List<CameraLens> {
        val public = manager.cameraIdList.mapNotNull { id ->
            runCatching { manager.getCameraCharacteristics(id) }.getOrNull()?.let { id to it }
        }
        fun usable(cc: CameraCharacteristics): Boolean = runCatching {
            cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(SurfaceTexture::class.java)?.isNotEmpty() == true
        }.getOrDefault(false)
        val result = public.filter { usable(it.second) }.map { (id,cc) -> CameraLens(id,null,cc,cc) }.toMutableList()
        val seen = public.map { it.first }.toMutableSet()
        for ((id,logical) in public) for (physical in logical.physicalCameraIds.sorted()) {
            if (physical in seen) continue
            val cc = runCatching {manager.getCameraCharacteristics(physical)}.getOrNull() ?: continue
            if (!usable(cc)) continue
            result.add(CameraLens(id,physical,cc,logical));seen.add(physical)
        }
        // When individual sensors are available, offer those instead of a fused logical route.
        val explicitIds = result.filterNot { it.automatic }.map { it.physicalId ?: it.cameraId }.toSet()
        return result.filterNot { it.automatic && it.characteristics.physicalCameraIds.any { id -> id in explicitIds } }
            .sortedWith(compareBy<CameraLens> { it.front }.thenBy { !it.primary })
    }
}
