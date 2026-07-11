package com.rinthy.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

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

@Immutable
data class RinthySemanticColors(
    val chrome: Color,
    val chromeBorder: Color,
    val separator: Color,
    val positive: Color,
    val info: Color,
    val warning: Color,
)

private val LocalRinthyColors = staticCompositionLocalOf {
    RinthySemanticColors(
        chrome = Color.Unspecified,
        chromeBorder = Color.Unspecified,
        separator = Color.Unspecified,
        positive = Color.Unspecified,
        info = Color.Unspecified,
        warning = Color.Unspecified,
    )
}

object RinthyDesign {
    val colors: RinthySemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRinthyColors.current

    val contentShape = RoundedCornerShape(10.dp)
    val chromeShape = RoundedCornerShape(22.dp)
}

private val RinthyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun RinthyTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = if (isDark) DarkColors else LightColors
    val semanticColors = if (isDark) {
        RinthySemanticColors(
            chrome = Color(0xE61B241E),
            chromeBorder = Color(0xFF46534A),
            separator = Color(0xFF29352D),
            positive = RinthyGreen,
            info = RinthyCyan,
            warning = Color(0xFFFFC45D),
        )
    } else {
        RinthySemanticColors(
            chrome = Color(0xEBF7FBF8),
            chromeBorder = Color(0xFFC0CBC3),
            separator = Color(0xFFD1D9D3),
            positive = Color(0xFF14753A),
            info = Color(0xFF00677D),
            warning = Color(0xFF765A00),
        )
    }

    CompositionLocalProvider(LocalRinthyColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = RinthyShapes,
            content = content,
        )
    }
}
