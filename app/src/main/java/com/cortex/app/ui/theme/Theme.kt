package com.cortex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background = CortexInk,
    surface = CortexInkElevated,
    primary = CortexAccent,
    secondary = CortexAccentMuted,
    onBackground = CortexTextPrimary,
    onSurface = CortexTextPrimary,
    onPrimary = CortexTextPrimary
)

private val LightColors = lightColorScheme(
    background = CortexLightBackground,
    surface = CortexLightSurface,
    primary = CortexAccent,
    secondary = CortexAccentMuted,
    onBackground = CortexLightTextPrimary,
    onSurface = CortexLightTextPrimary,
    onPrimary = CortexLightBackground
)

@Composable
fun CortexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CortexTypography,
        content = content
    )
}
