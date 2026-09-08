package com.bongorian.signa1

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** AndroidX persists app locales on Android 12 and syncs with Android 13+ system settings. */
internal object AppLanguage {
    val TAGS: Array<String> = arrayOf<String>("", "ja", "en", "zh")

    fun current(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }

    fun labels(context: Context): Array<String> {
        return arrayOf<String>(
            context.getString(R.string.language_system),
            "日本語",
            "English",
            "简体中文",
        )
    }

    fun index(tags: String): Int {
        for (n in 1..<TAGS.size) if (tags == TAGS[n] || tags.startsWith(TAGS[n] + "-")) return n
        return 0
    }

    fun select(tags: String?) {
        if (current() != tags)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
