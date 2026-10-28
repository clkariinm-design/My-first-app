package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkObsidian,
    primaryContainer = NeonCyanVariant.copy(alpha = 0.2f),
    onPrimaryContainer = NeonCyan,
    secondary = ElectricAmber,
    onSecondary = DarkObsidian,
    secondaryContainer = ElectricAmber.copy(alpha = 0.2f),
    onSecondaryContainer = ElectricAmber,
    tertiary = CyberGreen,
    onTertiary = DarkObsidian,
    background = DarkObsidian,
    onBackground = TextPrimary,
    surface = DarkSlate,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = NeonRed,
    onError = DarkObsidian
)

@Composable
fun MacroFlowTheme(
    darkTheme: Boolean = true, // Automation tools shine in modern high-contrast dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkObsidian.toArgb()
                window.navigationBarColor = DarkObsidian.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MacroFlowTheme(darkTheme = true, content = content)
}
