package com.rinthy.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RinthyGreen = Color(0xFF63DD7A)
val RinthyCyan = Color(0xFF48C9F0)
private val DarkColors = darkColorScheme(
    primary = RinthyGreen,
    onPrimary = Color(0xFF05210E),
    secondary = RinthyCyan,
    onSecondary = Color(0xFF002029),
    tertiary = Color(0xFFFFC45D),
    background = Color(0xFF0C100E),
    onBackground = Color(0xFFEAF1EC),
    surface = Color(0xFF141A16),
    onSurface = Color(0xFFEAF1EC),
    surfaceVariant = Color(0xFF202922),
    onSurfaceVariant = Color(0xFFB7C2BA),
    outline = Color(0xFF3A463D),
    outlineVariant = Color(0xFF28332B),
    error = Color(0xFFFF716C),
    onError = Color(0xFF300303),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF14753A),
    onPrimary = Color(0xFFF5FFF7),
    secondary = Color(0xFF00677D),
    onSecondary = Color(0xFFF1FCFF),
    tertiary = Color(0xFF765A00),
    background = Color(0xFFF4F8F5),
    onBackground = Color(0xFF151A16),
    surface = Color(0xFFFBFEFC),
    onSurface = Color(0xFF151A16),
    surfaceVariant = Color(0xFFE5ECE7),
    onSurfaceVariant = Color(0xFF465049),
    outline = Color(0xFF737E76),
    outlineVariant = Color(0xFFC7D0C9),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFF8F7),
)

@Composable
fun RinthyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
