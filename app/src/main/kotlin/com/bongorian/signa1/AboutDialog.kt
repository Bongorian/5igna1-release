package com.bongorian.signa1

import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** Privacy information remains available offline and without camera permission. */
internal object AboutDialog {
    fun show(activity: MainActivity) {
        var policy: String?
        try {
            activity.resources.openRawResource(R.raw.privacy_policy).use { `in` ->
                ByteArrayOutputStream().use { out ->
                    val buffer = ByteArray(4096)
                    var n: Int
                    while ((`in`.read(buffer).also { n = it }) != -1) out.write(buffer, 0, n)
                    policy = out.toString(StandardCharsets.UTF_8.name())
                }
            }
        } catch (error: Exception) {
            policy =
                activity.getString(
                    R.string.ui_could_not_load_the_privacy_policy_contact_dennosamurai
                )
        }
        val text = activity.text(policy, 14, MainActivity.WHITE)
        text.setTextIsSelectable(true)
        text.setPadding(activity.dp(20f), activity.dp(12f), activity.dp(20f), activity.dp(16f))
        text.setLineSpacing(activity.dp(4f).toFloat(), 1f)
        text.setAutoLinkMask(Linkify.EMAIL_ADDRESSES)
        text.setLinkTextColor(MainActivity.LIME)
        val content = LinearLayout(activity)
        content.setOrientation(LinearLayout.VERTICAL)
        val licenses = activity.button(activity.getString(R.string.ui_open_source_licenses))
        licenses.setOnClickListener(OnClickListener@{ v: View? -> showLicenses(activity) })
        content.addView(licenses, LinearLayout.LayoutParams(-1, activity.dp(48f)))
        content.addView(text)
        SignalSheet.content(
            activity,
            BuildConfig.APP_NAME + " · " + BuildConfig.VERSION_NAME,
            content,
            R.string.ui_contact,
            Runnable {
                val intent =
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dennosamurai@gmail.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "5igna1 support")
                try {
                    activity.startActivity(intent)
                } catch (unavailable: Exception) {
                    Toast.makeText(
                            activity,
                            activity.getString(R.string.ui_contact_dennosamurai_gmail_com),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }
            },
            .87f,
        )
    }

    fun showLicenses(activity: MainActivity) {
        val legal = StringBuilder()
        for (name in
            arrayOf<String>(
                "NOTICE",
                "THIRD_PARTY_LICENSES.md",
                "LICENSE",
                "Kotlin-COPYRIGHT.txt",
                "Kotlin-Boost-1.0.txt",
            )) {
            try {
                activity.assets.open("licenses/" + name).use { `in` ->
                    ByteArrayOutputStream().use { out ->
                        val buffer = ByteArray(4096)
                        var n: Int
                        while ((`in`.read(buffer).also { n = it }) != -1) out.write(buffer, 0, n)
                        legal
                            .append(name)
                            .append("\n\n")
                            .append(out.toString(StandardCharsets.UTF_8.name()))
                            .append("\n\n")
                    }
                }
            } catch (error: Exception) {
                legal
                    .append(name)
                    .append(": ")
                    .append(activity.getString(R.string.ui_unavailable))
                    .append('\n')
            }
        }
        val text = activity.text(legal.toString(), 12, MainActivity.WHITE)
        text.setTextIsSelectable(true)
        text.setPadding(activity.dp(20f), activity.dp(12f), activity.dp(20f), activity.dp(16f))
        SignalSheet.content(
            activity,
            activity.getString(R.string.ui_open_source_licenses),
            text,
            0,
            null,
            .87f,
        )
    }
}
