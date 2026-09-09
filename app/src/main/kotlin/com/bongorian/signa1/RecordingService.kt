package com.bongorian.signa1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/** The only long-running background session is an explicitly started output recording. */
internal class RecordingService : Service() {
    private var wake: PowerManager.WakeLock? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val renew = object : Runnable {
        override fun run() {
            if (owner?.recording != true) { stopSelf(); return }
            wake?.acquire(180_000L)
            handler.postDelayed(this, 120_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = owner
        if (intent?.action == STOP || engine == null) {
            engine?.gl?.post { engine.stopVideo() }
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL,
                getString(R.string.recording_notification), NotificationManager.IMPORTANCE_LOW))
            val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
            val stop = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.icon_monochrome)
                .setContentTitle(getString(R.string.recording_notification))
                .setContentText(getString(R.string.recording_background_hint))
                .setContentIntent(open).setOngoing(true)
                .addAction(Notification.Action.Builder(null, getString(R.string.ui_stop_recording), stop).build())
                .build()
            var type = if (!intent!!.getBooleanExtra("tap", false)) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                else if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            if (intent.getBooleanExtra("microphone", false)) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(81, notification, type)
            if (wake == null) {
                wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "5igna1:recording").apply {
                        setReferenceCounted(false)
                        acquire(180_000L)
                    }
                handler.postDelayed(renew, 120_000L)
            }
            val start = pending
            pending = null
            start?.invoke()
        } catch (error: Exception) {
            android.util.Log.e("Signal", "Recording foreground service", error)
            pending = null
            engine.status(getString(R.string.recording_service_failed))
            engine.ready(engine.frameSeen)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        owner?.let { engine -> engine.gl.post { engine.stopVideo() } }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        owner?.let { engine -> engine.gl.post { engine.stopVideo() } }
        stopSelf()
    }

    override fun onDestroy() {
        val engine = owner
        owner = null
        pending = null
        if (engine?.recording == true) engine.gl.post { engine.stopVideo() }
        handler.removeCallbacks(renew)
        wake?.let { if (it.isHeld) it.release() }
        wake = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "recording"
        private const val STOP = "com.bongorian.signa1.STOP_RECORDING"
        @Volatile private var owner: GlitchEngine? = null
        private var pending: (() -> Unit)? = null
        private var serviceIntent: Intent? = null

        fun begin(context: Context, engine: GlitchEngine, microphone: Boolean, start: () -> Unit) {
            if (!engine.foreground) return
            owner = engine
            pending = start
            try {
                val intent = Intent(context, RecordingService::class.java)
                    .putExtra("tap", engine.tapInput != null).putExtra("microphone", microphone)
                serviceIntent = intent
                context.startForegroundService(intent)
            } catch (error: Exception) {
                android.util.Log.e("Signal", "Start recording foreground service", error)
                owner = null
                pending = null
                engine.status(context.getString(R.string.recording_service_failed))
                engine.ready(engine.frameSeen)
            }
        }

        fun end(context: Context, engine: GlitchEngine) {
            if (owner !== engine) return
            owner = null
            pending = null
            serviceIntent?.let { context.stopService(it) }
            serviceIntent = null
        }
    }
}
