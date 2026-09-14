package com.bongorian.signa1

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

/** Queries the Play Store service; the app needs no network or notification permission. */
internal object DistributionUpdates {
    fun addTo(dialog: QualityDialog) {
        val activity = dialog.activity
        val row = dialog.field().apply { tag = "settings-play-update" }
        var active = true
        var finished = false
        val timeout = Runnable {
            if (active && !finished) {
                finished = true
                row.setText(R.string.play_update_unknown)
            }
        }
        row.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                active = false
                activity.handler.removeCallbacks(timeout)
            }
        })
        fun finish(label: Int) {
            if (!active || finished || activity.isDestroyed) return
            finished = true
            activity.handler.removeCallbacks(timeout)
            row.setText(label)
        }
        row.setText(R.string.play_update_checking)
        row.setOnClickListener {
            val uri = Uri.parse("market://details?id=" + activity.packageName)
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
            } catch (_: ActivityNotFoundException) {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + activity.packageName)))
                } catch (_: ActivityNotFoundException) {
                    row.setText(R.string.play_update_store_unavailable)
                }
            }
        }
        // DEV has a different application ID and is not the Play release listing.
        if (BuildConfig.DEBUG) {
            finish(R.string.play_update_dev)
            row.setOnClickListener(null)
            return
        }
        activity.handler.postDelayed(timeout, 10_000)
        try {
            AppUpdateManagerFactory.create(activity.applicationContext).appUpdateInfo
                .addOnSuccessListener { info ->
                    finish(when (info.updateAvailability()) {
                        UpdateAvailability.UPDATE_AVAILABLE,
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> R.string.play_update_available
                        UpdateAvailability.UPDATE_NOT_AVAILABLE -> R.string.play_update_none
                        else -> R.string.play_update_unknown
                    })
                }
                .addOnFailureListener { finish(R.string.play_update_unknown) }
        } catch (_: RuntimeException) {
            finish(R.string.play_update_unknown)
        }
    }
}
