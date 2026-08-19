package com.triptogether

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.triptogether.feature.auth.ThemePreference
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TripTogetherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Thai is the app's default language regardless of system locale;
        // the user can switch in Settings (or per-app language on Android 13+).
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
        }
        // Re-apply the saved Light/Dark/System choice so it survives a cold start.
        ThemePreference.apply(this)
    }
}
