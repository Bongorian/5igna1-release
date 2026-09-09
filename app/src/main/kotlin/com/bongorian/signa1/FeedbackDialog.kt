package com.bongorian.signa1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

/** User-reviewed email only; no network client, automatic reporting, logs or attachments. */
internal object FeedbackDialog {
    const val ADDRESS = "dennosamurai@gmail.com"

    fun intent(subject: String, body: String): Intent = Intent(Intent.ACTION_SENDTO,
        Uri.parse("mailto:$ADDRESS?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"))
        .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body)

    fun save(a: MainActivity): android.os.Bundle? {
        val root = a.feedbackDialog?.window?.decorView ?: return null
        return android.os.Bundle().apply {
            putString("subject", root.findViewWithTag<EditText>("feedback-subject").text.toString())
            putString("message", root.findViewWithTag<EditText>("feedback-message").text.toString())
            putBoolean("device", root.findViewWithTag<CheckBox>("feedback-device-info").isChecked)
        }
    }

    fun show(a: MainActivity, saved: android.os.Bundle? = null): android.app.Dialog {
        a.feedbackDialog?.let { return it }
        val body = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        body.addView(a.text(a.getString(R.string.feedback_hint, ADDRESS), 13, MainActivity.MUTED))
        val subject = EditText(a).apply {
            tag = "feedback-subject"
            setText(saved?.getString("subject") ?: ("5igna1 · " + a.getString(R.string.feedback_title)))
            setTextColor(MainActivity.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            contentDescription = a.getString(R.string.feedback_subject)
        }
        body.addView(subject, LinearLayout.LayoutParams(-1, -2))
        val message = EditText(a).apply {
            tag = "feedback-message"
            hint = a.getString(R.string.feedback_message)
            setHintTextColor(MainActivity.MUTED)
            setTextColor(MainActivity.WHITE)
            setText(saved?.getString("message") ?: "")
            minLines = 5
            gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        body.addView(message, LinearLayout.LayoutParams(-1, -2))
        val details = "5igna1 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.FLAVOR}\n" +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n${Build.MANUFACTURER} ${Build.MODEL}"
        val include = CheckBox(a).apply {
            tag = "feedback-device-info"
            text = a.getString(R.string.feedback_device)
            setTextColor(MainActivity.WHITE)
            isChecked = saved?.getBoolean("device") ?: false
        }
        body.addView(include)
        body.addView(a.text(details, 12, MainActivity.MUTED))
        fun draft() = message.text.toString() + if (include.isChecked) "\n\n$details" else ""
        val copy = a.button(a.getString(R.string.feedback_copy)).apply { tag = "feedback-copy" }
        copy.setOnClickListener {
            (a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText(a.getString(R.string.feedback_title), "$ADDRESS\n${subject.text}\n\n${draft()}"))
            Toast.makeText(a, R.string.feedback_copied, Toast.LENGTH_SHORT).show()
        }
        body.addView(copy, LinearLayout.LayoutParams(-1, a.dp(48f)))
        val email = a.button(a.getString(R.string.feedback_email)).apply { tag = "feedback-email" }
        email.setOnClickListener {
            try { a.startActivity(intent(subject.text.toString(), draft())) }
            catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(a, R.string.feedback_no_email, Toast.LENGTH_LONG).show()
            }
        }
        body.addView(email, LinearLayout.LayoutParams(-1, a.dp(48f)))
        val dialog = SignalSheet.content(a, a.getString(R.string.feedback_title), body, 0, null, .9f)
        a.feedbackDialog = dialog
        dialog.setOnDismissListener { if (a.feedbackDialog === dialog) a.feedbackDialog = null }
        return dialog
    }
}
