package com.bongorian.signa1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.Locale
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.max

/** Foreground-only location with independent permission, service and fix states. */
internal class GeoTags(val activity: Activity, val changed: Runnable) : LocationListener {
    val manager: LocationManager?
    val handler: Handler = Handler(Looper.getMainLooper())
    val requests: ArrayList<CancellationSignal> = ArrayList<CancellationSignal>()

    @Volatile var last: Location? = null

    @Volatile var enabled: Boolean = false
    var active: Boolean = false
    var foreground: Boolean = false
    var generation: Int = 0
    var permissionLevel: Int = 0
    var attempted: Long = 0
    var error: String = ""

    fun precise(): Boolean {
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun permitted(): Boolean {
        return precise() ||
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun servicesEnabled(): Boolean {
        return manager != null && manager.isLocationEnabled()
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            last = null
            disconnect()
        } else if (foreground) connect(false)
        changed.run()
    }

    fun start() {
        foreground = true
        connect(false)
        handler.removeCallbacks(heartbeat)
        handler.postDelayed(heartbeat, 1000)
        changed.run()
    }

    fun retry() {
        if (foreground) connect(true)
        changed.run()
    }

    fun connect(force: Boolean) {
        val level = if (precise()) 2 else if (permitted()) 1 else 0
        if (level < permissionLevel) last = null
        if (!foreground || !enabled || level == 0 || !servicesEnabled()) {
            disconnect()
            permissionLevel = level
            return
        }
        if (!force && active && permissionLevel == level) return
        disconnect()
        permissionLevel = level
        attempted = SystemClock.elapsedRealtime()
        error = ""
        val ticket = generation
        for (provider in manager!!.getProviders(true)) {
            if (provider == LocationManager.PASSIVE_PROVIDER) continue
            try {
                val known = manager.getLastKnownLocation(provider)
                if (known != null) onLocationChanged(known)
                manager.requestLocationUpdates(provider, 1000, 0f, this, Looper.getMainLooper())
                active = true
                val cancellation = CancellationSignal()
                requests.add(cancellation)
                val request =
                    LocationRequest.Builder(1000)
                        .setQuality(
                            if (level == 2) LocationRequest.QUALITY_HIGH_ACCURACY
                            else LocationRequest.QUALITY_BALANCED_POWER_ACCURACY
                        )
                        .setDurationMillis(30000)
                        .build()
                manager.getCurrentLocation(
                    provider,
                    request,
                    cancellation,
                    activity.mainExecutor,
                    Consumer { location: Location? ->
                        if (ticket != generation || !foreground || !enabled) return@Consumer
                        if (location != null) onLocationChanged(location) else changed.run()
                    },
                )
            } catch (denied: SecurityException) {
                error = activity.getString(R.string.ui_check_location_permissions)
            } catch (unavailable: IllegalArgumentException) {
                error = activity.getString(R.string.ui_location_service_unavailable)
            }
        }
        changed.run()
    }

    fun disconnect() {
        generation++
        for (signal in requests) signal.cancel()
        requests.clear()
        if (manager != null)
            try {
                manager.removeUpdates(this)
            } catch (ignored: SecurityException) {}
        active = false
    }

    fun stop() {
        foreground = false
        handler.removeCallbacks(heartbeat)
        disconnect()
    }

    val heartbeat: Runnable =
        object : Runnable {
            override fun run() {
                if (!foreground) return
                val level = if (precise()) 2 else if (permitted()) 1 else 0
                if (!enabled || !servicesEnabled() || level != permissionLevel) connect(false)
                else if (enabled && !active && SystemClock.elapsedRealtime() - attempted > 10000)
                    connect(false)
                changed.run()
                handler.postDelayed(this, 1000)
            }
        }

    init {
        manager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    }

    fun snapshot(): Location? {
        val value = last
        return if (
            !enabled ||
                !permitted() ||
                (!precise() && permissionLevel == 2) ||
                !servicesEnabled() ||
                !fresh(
                    value,
                    SystemClock.elapsedRealtimeNanos(),
                )
        )
            null
        else Location(value!!)
    }

    fun label(): String {
        return if (!enabled) "GPS OFF"
        else if (!permitted()) activity.getString(R.string.ui_gps_denied)
        else if (!servicesEnabled()) activity.getString(R.string.ui_gps_device_off)
        else if (snapshot() != null)
            (if (precise()) "GPS ON" else activity.getString(R.string.ui_gps_approx))
        else if (!active) activity.getString(R.string.ui_gps_unavailable)
        else if (SystemClock.elapsedRealtime() - attempted > 30000)
            activity.getString(R.string.ui_gps_no_fix)
        else activity.getString(R.string.ui_gps_locating)
    }

    fun detail(): String {
        val permission =
            if (precise()) activity.getString(R.string.ui_precise_location_allowed)
            else if (permitted()) activity.getString(R.string.ui_approximate_location_allowed)
            else activity.getString(R.string.ui_location_not_allowed)
        val service =
            if (servicesEnabled()) activity.getString(R.string.ui_device_location_on)
            else activity.getString(R.string.ui_device_location_off)
        val value = snapshot()
        val fix: String?
        if (!enabled) fix = activity.getString(R.string.ui_saving_capture_location_is_off)
        else if (!permitted())
            fix = activity.getString(R.string.ui_select_allow_location_and_grant_access_while_using)
        else if (!servicesEnabled())
            fix = activity.getString(R.string.ui_enable_location_in_the_device_settings)
        else if (value != null)
            fix =
                (if (value.hasAccuracy())
                    String.format(
                        Locale.JAPAN,
                        activity.getString(R.string.ui_estimated_accuracy_0f_m),
                        value.accuracy,
                    )
                else activity.getString(R.string.ui_location_acquired)) +
                    String.format(
                        Locale.JAPAN,
                        activity.getString(R.string.ui_d_s_ago),
                        max(
                            0,
                            (SystemClock.elapsedRealtimeNanos() - value.elapsedRealtimeNanos) /
                                1000000000L,
                        ),
                    )
        else
            fix =
                if (active)
                    activity.getString(
                        R.string.ui_acquiring_location_gps_reception_may_be_weak_indoors
                    )
                else if (error.isEmpty())
                    activity.getString(
                        R.string.ui_no_location_service_is_available_check_your_device
                    )
                else error
        return permission +
            "\n" +
            service +
            "\n\n" +
            fix +
            activity.getString(R.string.ui_an_available_location_is_saved_in_photo_and)
    }

    override fun onLocationChanged(value: Location) {
        if (
            !foreground ||
                !enabled ||
                !permitted() ||
                (!precise() && permissionLevel == 2) ||
                !fresh(
                    value,
                    SystemClock.elapsedRealtimeNanos(),
                )
        )
            return
        val old = last
        val difference =
            if (old == null) Long.MAX_VALUE
            else value!!.elapsedRealtimeNanos - old.elapsedRealtimeNanos
        if (
            !fresh(
                old,
                SystemClock.elapsedRealtimeNanos(),
            ) ||
                difference > 15000000000L ||
                difference >= 0 &&
                    (!old!!.hasAccuracy() ||
                        value!!.hasAccuracy() && value.accuracy <= old.accuracy * 2)
        )
            last = Location(value!!)
        changed.run()
    }

    override fun onProviderEnabled(provider: String) {
        handler.post(Runnable@{ if (foreground) connect(true) })
    }

    override fun onProviderDisabled(provider: String) {
        changed.run()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    companion object {
        fun fresh(value: Location?, now: Long): Boolean {
            if (value == null) return false
            val age = now - value.elapsedRealtimeNanos
            return age >= 0 &&
                age <= 120000000000L &&
                java.lang.Double.isFinite(value.latitude) &&
                java.lang.Double.isFinite(value.longitude) &&
                abs(value.latitude) <= 90 &&
                abs(value.longitude) <= 180
        }
    }
}
