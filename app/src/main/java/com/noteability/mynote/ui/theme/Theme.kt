package com.noteability.mynote.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Color Schemes
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue10,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,

    secondary = Neutral20,
    onSecondary = White,
    secondaryContainer = Neutral40,
    onSecondaryContainer = Neutral90,

    background = Neutral10,
    onBackground = Neutral90,

    surface = Neutral20,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral60,

    error = ErrorRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,

    secondary = Neutral95,
    onSecondary = Neutral10,
    secondaryContainer = Neutral90,
    onSecondaryContainer = Neutral20,

    background = Neutral99,
    onBackground = Neutral10,

    surface = White,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral60,

    error = ErrorRed,
    onError = White
)

// Theme Composable
@Composable
fun MyNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}