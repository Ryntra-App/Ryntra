package com.rinthy.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RinthyGreen = Color(0xFF1BD96A)
private val DarkColors = darkColorScheme(
    primary = RinthyGreen,
    onPrimary = Color(0xFF00210C),
    secondary = Color(0xFFFFC857),
    tertiary = Color(0xFFFF7A66),
    background = Color(0xFF0D100E),
    onBackground = Color(0xFFF0F4F1),
    surface = Color(0xFF151916),
    onSurface = Color(0xFFF0F4F1),
    surfaceVariant = Color(0xFF222823),
    onSurfaceVariant = Color(0xFFB7C0B9),
    outline = Color(0xFF3A443C),
    error = Color(0xFFFF716C),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF087A3A),
    onPrimary = Color.White,
    secondary = Color(0xFF8B5C00),
    tertiary = Color(0xFFB43A2D),
    background = Color(0xFFF6F8F6),
    onBackground = Color(0xFF161A17),
    surface = Color.White,
    onSurface = Color(0xFF161A17),
    surfaceVariant = Color(0xFFE9EEE9),
    onSurfaceVariant = Color(0xFF4C554E),
    outline = Color(0xFFBDC6BF),
    error = Color(0xFFBA1A1A),
)

@Composable
fun RinthyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
