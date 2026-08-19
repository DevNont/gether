package com.triptogether.feature.auth

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * Light / Dark / System theme choice.
 *
 * Stored in SharedPreferences so it survives process death, and applied through AppCompat night
 * mode — which updates the activity configuration, so Compose's isSystemInDarkTheme() reflects it
 * without any extra state threading. Mirrors how the language setting rides on AppCompatDelegate.
 */
object ThemePreference {
    private const val PREFS = "tt_settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    /** Re-apply the saved mode. Call from Application.onCreate so the choice survives a cold start. */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(read(context))
    }

    /** One of AppCompatDelegate.MODE_NIGHT_{NO,YES,FOLLOW_SYSTEM}; defaults to follow-system. */
    fun read(context: Context): Int = prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /** Persist and apply immediately; AppCompat recreates the activity to swap the theme. */
    fun set(
        context: Context,
        mode: Int,
    ) {
        prefs(context).edit { putInt(KEY_NIGHT_MODE, mode) }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
