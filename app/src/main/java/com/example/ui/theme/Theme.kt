package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IdeDarkColorScheme = darkColorScheme(
    primary = IdePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1F6FEB),
    onPrimaryContainer = Color.White,
    secondary = IdeSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF238636),
    onSecondaryContainer = Color.White,
    tertiary = IdeTertiary,
    onTertiary = Color.Black,
    background = IdeBackground,
    onBackground = IdeTextPrimary,
    surface = IdeSurface,
    onSurface = IdeTextPrimary,
    surfaceVariant = IdeSurfaceVariant,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeBorder,
    outlineVariant = Color(0xFF21262D),
    error = IdeError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce crisp developer dark IDE theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IdeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
