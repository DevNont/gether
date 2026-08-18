package com.triptogether.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
