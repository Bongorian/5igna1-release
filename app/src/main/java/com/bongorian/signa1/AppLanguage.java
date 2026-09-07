package com.bongorian.signa1;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/** AndroidX persists app locales on Android 12 and syncs with Android 13+ system settings. */
final class AppLanguage {
    static final String[] TAGS={"","ja","en","zh"};
    static String current(){return AppCompatDelegate.getApplicationLocales().toLanguageTags();}
    static String[] labels(Context context){return new String[]{context.getString(R.string.language_system),"日本語","English","简体中文"};}
    static int index(String tags){for(int n=1;n<TAGS.length;n++)if(tags.equals(TAGS[n])||tags.startsWith(TAGS[n]+"-"))return n;return 0;}
    static void select(String tags){if(!current().equals(tags))AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags));}
}
