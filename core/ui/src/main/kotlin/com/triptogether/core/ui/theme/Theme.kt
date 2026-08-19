package com.triptogether.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fallback palette for Android 11 and below (dynamic color needs 12+). Teal travel tone.
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF00696E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6FF6FD),
        onPrimaryContainer = Color(0xFF002022),
        secondary = Color(0xFF4A6365),
        secondaryContainer = Color(0xFFCCE8EA),
        tertiary = Color(0xFF2E7D32),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF4DD9E1),
        onPrimary = Color(0xFF00363A),
        primaryContainer = Color(0xFF004F53),
        onPrimaryContainer = Color(0xFF6FF6FD),
        secondary = Color(0xFFB1CBCE),
        secondaryContainer = Color(0xFF324B4D),
        tertiary = Color(0xFF81C784),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
    )

/** M7.1 — full dark mode; Material 3 dynamic color on Android 12+, custom palette below that. */
@Composable
fun TripTogetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else -> LightColors
        }
    // Flip the system bar icons to match the theme; without this they stay light and
    // vanish against the light-mode status bar (the app is not edge-to-edge).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
