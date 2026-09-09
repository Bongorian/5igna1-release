package com.bongorian.signa1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import com.bongorian.signa1.FaultModel.Inputs
import java.util.Arrays
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/** Sensor ownership is tied to foreground GL attachment. All fields except micPeak are GL-owned. */
internal class FaultInputs(val context: Context, val handler: Handler) : SensorEventListener {
    val sensors: SensorManager?
    val values: Inputs = Inputs()
    var config: FaultConfig = FaultConfig.defaults()
    var active: Boolean = false
    var recorderAudio: Boolean = false
    var hasAccel: Boolean = false
    var hasGyro: Boolean = false
    var gravityReady: Boolean = false
    var gx: Float = 0f
    var gy: Float = 0f
    var gz: Float = 0f
    var batteryC: Float = Float.NaN
    var thermalStatus: Int = -1
    var accelNs: Long = 0
    var lastCpuWall: Long = 0
    var lastCpuTime: Long = 0
    var lastFrame: Long = 0
    var lastArrival: Long = 0
    var framePeriod: Float = 0f
    val metadata: Array<LongArray> = Array(16) { LongArray(3) }
    var metadataIndex: Int = 0

    @Volatile var micPeak: Float = 0f

    @Volatile var micRunning: Boolean = false

    @Volatile var micState: String = "OFF"
    var microphone: AudioRecord? = null
    var micThread: Thread? = null

    fun configure(next: FaultConfig, foreground: Boolean, recordingAudio: Boolean) {
        val changed = active != (foreground && next.enabled) || config.motion != next.motion
        config = next
        recorderAudio = recordingAudio
        if (changed) {
            stopSensors()
            active = foreground && next.enabled
            if (active && config.motion) {
                val accel =
                    if (sensors == null) null
                    else sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val gyro =
                    if (sensors == null) null else sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                if (accel != null)
                    hasAccel = sensors!!.registerListener(this, accel, 20000, handler)
                if (gyro != null) hasGyro = sensors!!.registerListener(this, gyro, 20000, handler)
            }
        }
        active = foreground && next.enabled
        handler.removeCallbacks(poll)
        if (active) {
            poll.run()
        } else {
            values.jitter = 0f
            values.heat = values.jitter
            values.cpu = values.heat
            lastArrival = 0
            lastFrame = lastArrival
            framePeriod = 0f
        }
        val wanted =
            active &&
                config.audio &&
                !recorderAudio &&
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        if (wanted && microphone == null) startMic() else if (!wanted) stopMic()
        if (
            active &&
                config.audio &&
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
        )
            micState = context.getString(R.string.ui_no_permission)
        else if (active && config.audio && recorderAudio)
            micState = context.getString(R.string.ui_recorded_audio)
    }

    fun stopSensors() {
        if (sensors != null) sensors.unregisterListener(this)
        gravityReady = false
        hasGyro = gravityReady
        hasAccel = hasGyro
        accelNs = 0
        values.rotation = 0f
        values.tilt = values.rotation
        values.az = values.tilt
        values.ay = values.az
        values.ax = values.ay
        values.motionAvailable = false
    }

    fun stop() {
        configure(config, false, false)
        lastCpuTime = 0
        lastCpuWall = lastCpuTime
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active || !config.motion) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            if (
                !java.lang.Float.isFinite(x) ||
                    !java.lang.Float.isFinite(y) ||
                    !java.lang.Float.isFinite(z)
            )
                return
            if (!gravityReady) {
                gx = x
                gy = y
                gz = z
                gravityReady = true
            }
            val dt =
                if (accelNs == 0L) .02f
                else
                    max(
                        .001f,
                        min(.2f, (event.timestamp - accelNs) * 1e-9f),
                    )
            accelNs = event.timestamp
            val alpha = exp(-dt / .35).toFloat()
            gx = alpha * gx + (1 - alpha) * x
            gy = alpha * gy + (1 - alpha) * y
            gz = alpha * gz + (1 - alpha) * z
            values.ax = x - gx
            values.ay = y - gy
            values.az = z - gz
            values.tilt = FaultModel.clamp(gx / 9.80665f, -1f, 1f)
            values.motionAvailable = true
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            values.rotation = FaultModel.clamp(event.values[2], -6f, 6f)
            values.motionAvailable = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    val poll: Runnable =
        object : Runnable {
            override fun run() {
                if (!active) return
                val wall = SystemClock.elapsedRealtime()
                val cpu = Process.getElapsedCpuTime()
                if (lastCpuWall > 0 && wall > lastCpuWall)
                    values.cpu =
                        FaultModel.clamp(
                            (cpu - lastCpuTime) / (wall - lastCpuWall).toFloat(),
                            0f,
                            1f,
                        )
                lastCpuWall = wall
                lastCpuTime = cpu
                values.heat = 0f
                batteryC = Float.NaN
                thermalStatus = -1
                if (config.thermal) {
                    val battery =
                        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    if (battery != null && battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                        val c = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                        if (c > -20 && c < 90) {
                            batteryC = c
                            values.heat = FaultModel.clamp((c - 32) / 15, 0f, 1f)
                        }
                    }
                    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                    if (power != null)
                        try {
                            thermalStatus = power.currentThermalStatus
                            values.heat =
                                max(values.heat, FaultModel.clamp(thermalStatus / 4f, 0f, 1f))
                        } catch (ignored: RuntimeException) {}
                }
                handler.postDelayed(this, 1000)
            }
        }

    init {
        sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    }

    fun resetTiming() {
        lastArrival = 0
        lastFrame = lastArrival
        framePeriod = 0f
        values.jitter = 0f
        values.timingAvailable = false
        for (row in metadata) Arrays.fill(row, 0)
    }

    fun capture(result: TotalCaptureResult) {
        val stamp = result.get<Long?>(CaptureResult.SENSOR_TIMESTAMP)
        val exposure = result.get<Long?>(CaptureResult.SENSOR_EXPOSURE_TIME)
        val skew = result.get<Long?>(CaptureResult.SENSOR_ROLLING_SHUTTER_SKEW)
        if (stamp != null && exposure != null && skew != null) {
            val slot = metadata[metadataIndex++ % metadata.size]
            slot[0] = stamp
            slot[1] = exposure
            slot[2] = skew
        }
    }

    fun frame(stamp: Long, arrival: Long, recorder: MediaRecorder?): Inputs {
        if (lastFrame > 0 && stamp > lastFrame) {
            val period = (stamp - lastFrame) * 1e-9f
            if (framePeriod == 0f) framePeriod = period
            val gap = max(0f, period - framePeriod * 1.15f) / max(.001f, framePeriod)
            // Arrival lateness measures this app's delivery cadence, never network packet loss.
            val late =
                if (lastArrival > 0)
                    max(0f, (arrival - lastArrival) * 1e-9f - period * 1.2f) /
                        max(
                            .001f,
                            period,
                        )
                else 0f
            values.jitter = FaultModel.clamp(max(gap, late), 0f, 1f)
            if (period < framePeriod * 1.5f) framePeriod = framePeriod * .98f + period * .02f
        }
        lastFrame = stamp
        lastArrival = arrival
        values.sensorNs = stamp
        values.timingAvailable = false
        var best: Long = -1
        for (slot in metadata) if (
            slot[0] > 0 && slot[0] <= stamp && stamp - slot[0] < 250000000L && slot[0] > best
        ) {
            best = slot[0]
            values.exposureNs = slot[1]
            values.skewNs = slot[2]
            values.timingAvailable = true
        }
        values.audio = 0f
        if (active && config.audio) {
            if (recorderAudio && recorder != null)
                try {
                    values.audio = level(recorder.maxAmplitude)
                } catch (ignored: RuntimeException) {
                    micState = context.getString(R.string.ui_unavailable_220)
                }
            else values.audio = micPeak
        }
        return values
    }

    fun startMic() {
        if (
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            micState = context.getString(R.string.ui_no_permission)
            return
        }
        try {
            val size =
                max(
                    4096,
                    AudioRecord.getMinBufferSize(
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    ),
                )
            val next =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size,
                )
            microphone = next
            check(next.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }
            next.startRecording()
            micRunning = true
            micState = context.getString(R.string.ui_level_detection)
            micThread =
                Thread(
                    Runnable {
                        val buffer = ShortArray(1024)
                        try {
                            while (micRunning) {
                                val count = next.read(buffer, 0, buffer.size)
                                if (count <= 0) break
                                var peak = 0
                                for (n in 0..<count) peak = max(peak, abs(buffer[n].toInt()))
                                micPeak = level(peak)
                            }
                        } catch (ignored: RuntimeException) {} finally {
                            micPeak = 0f
                            if (micRunning)
                                micState = context.getString(R.string.ui_unavailable_220)
                        }
                    },
                    "FaultAudio",
                )
            micThread!!.start()
        } catch (error: RuntimeException) {
            stopMic()
            micState = context.getString(R.string.ui_unavailable_220)
        }
    }

    fun stopMic() {
        micRunning = false
        micPeak = 0f
        if (microphone != null) {
            try {
                microphone!!.stop()
            } catch (ignored: RuntimeException) {}
            if (micThread != null)
                try {
                    micThread!!.join(300)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            microphone!!.release()
            microphone = null
        }
        micThread = null
        micState = "OFF"
    }

    fun summary(): String {
        return (if (hasAccel || hasGyro) context.getString(R.string.ui_reading_motion)
        else context.getString(R.string.ui_motion)) +
            context.getString(R.string.ui_audio) +
            micState +
            "\n" +
            (if (java.lang.Float.isNaN(batteryC)) context.getString(R.string.ui_battery_temperature)
            else
                String.format(
                    Locale.US,
                    context.getString(R.string.ui_battery_1f_c),
                    batteryC,
                )) +
            context.getString(R.string.ui_thermal_state) +
            (if (thermalStatus < 0) "—" else thermalStatus) +
            "\n" +
            (if (config.cpu)
                String.format(
                    Locale.US,
                    context.getString(R.string.ui_app_cpu_0f_one_core),
                    values.cpu * 100,
                )
            else "CPU OFF") +
            " / " +
            (if (values.timingAvailable)
                String.format(
                    Locale.US,
                    context.getString(R.string.ui_readout_1f_ms),
                    values.skewNs * 1e-6,
                )
            else context.getString(R.string.ui_readout))
    }

    companion object {
        fun level(amplitude: Int): Float {
            return if (amplitude <= 0) 0f
            else
                FaultModel.clamp(
                    ((20 * log10(amplitude / 32768.0)).toFloat() + 55) / 45,
                    0f,
                    1f,
                )
        }
    }
}
