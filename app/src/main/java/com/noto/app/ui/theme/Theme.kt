package com.noto.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.noto.app.data.prefs.SettingsRepository

private val LightScheme = lightColorScheme(
    primary = NotoAccent,
    onPrimary = LightSurface,
    primaryContainer = NotoAccentSoft,
    onPrimaryContainer = NotoAccent,
    secondary = NotoAccent,
    onSecondary = LightSurface,
    secondaryContainer = NotoAccentSoft,
    onSecondaryContainer = NotoAccent,
    background = LightBg,
    onBackground = LightOnBg,
    surface = LightSurface,
    onSurface = LightOnBg,
    surfaceVariant = LightSurfaceElev,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightOutline,
    outlineVariant = LightOutline,
)

private val DarkScheme = darkColorScheme(
    primary = NotoAccentDark,
    onPrimary = DarkBg,
    primaryContainer = NotoAccentSoftDark,
    onPrimaryContainer = NotoAccentDark,
    secondary = NotoAccentDark,
    onSecondary = DarkBg,
    secondaryContainer = NotoAccentSoftDark,
    onSecondaryContainer = NotoAccentDark,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSurfaceElev,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
)

@Composable
fun NotoTheme(
    theme: SettingsRepository.Theme = SettingsRepository.Theme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        SettingsRepository.Theme.SYSTEM -> isSystemInDarkTheme()
        SettingsRepository.Theme.LIGHT -> false
        SettingsRepository.Theme.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = NotoTypography,
        content = content,
    )
}
