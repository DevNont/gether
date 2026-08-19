package com.triptogether

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.triptogether.feature.auth.ThemePreference
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TripTogetherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        seedDefaultLanguage()
        // Re-apply the saved Light/Dark/System choice so it survives a cold start.
        ThemePreference.apply(this)
    }

    /**
     * Thai is the app's default language regardless of the device locale. Force it once on the
     * first launch after install, tracked by our own flag. On API 33+ use the platform
     * LocaleManager directly — AppCompatDelegate.setApplicationLocales does not take effect when
     * called from Application.onCreate there (it only sticks from an Activity), so an en-TH device
     * stayed English. After this the user's in-app choice persists and is not reset.
     */
    private fun seedDefaultLanguage() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LOCALE_SEEDED, false)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags("th")
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
        }
        prefs.edit { putBoolean(KEY_LOCALE_SEEDED, true) }
    }

    private companion object {
        const val PREFS = "tt_settings"
        const val KEY_LOCALE_SEEDED = "locale_seeded"
    }
}
