package com.ryntra.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.ThemeStyle

val RyntraGreen = Color(0xFF30D158)
val RyntraCyan = Color(0xFF0A84FF)
private val RyntraDarkColors = darkColorScheme(
    primary = RyntraGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = RyntraGreen,
    secondary = Color(0xFF98989D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1C1C1E),
    onSecondaryContainer = Color(0xFFF5F5F7),
    tertiary = Color(0xFFFFD60A),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2C2C2E),
    onTertiaryContainer = Color(0xFFFFD60A),
    background = Color.Black,
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF0C0C0E),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF98989D),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF08080A),
    surfaceContainer = Color(0xFF0C0C0E),
    surfaceContainerHigh = Color(0xFF161618),
    surfaceContainerHighest = Color(0xFF1C1C1E),
    outline = Color(0xFF545458),
    outlineVariant = Color(0xFF2C2C2E),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF2C2C2E),
    onErrorContainer = Color(0xFFFF6961),
)

private val PlatformDarkColors = darkColorScheme(
    primary = Color(0xFFA8DAB5),
    onPrimary = Color(0xFF0B3A22),
    primaryContainer = Color(0xFF245234),
    onPrimaryContainer = Color(0xFFC3F7CF),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E4DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE0E4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
)

private val PlatformLightColors = lightColorScheme(
    primary = Color(0xFF246B3C),
    onPrimary = Color(0xFFF7FFF7),
    primaryContainer = Color(0xFFA8F2B9),
    onPrimaryContainer = Color(0xFF08210F),
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF191D1A),
    surface = Color(0xFFF7FBF6),
    onSurface = Color(0xFF191D1A),
    surfaceVariant = Color(0xFFDDE5DD),
    onSurfaceVariant = Color(0xFF414942),
)

@Immutable
data class RyntraSemanticColors(
    val isPlatformNative: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val labelPrimary: Color,
    val labelSecondary: Color,
    val accent: Color,
    val destructive: Color,
    val chrome: Color,
    val chromeBorder: Color,
    val chromeHighlight: Color,
    val separator: Color,
    val positive: Color,
    val info: Color,
    val warning: Color,
)

@Immutable
data class RyntraMotion(
    val isReduced: Boolean,
) {
    fun duration(defaultMillis: Int): Int = if (isReduced) 0 else defaultMillis
}

private val LocalRyntraColors = staticCompositionLocalOf {
    RyntraSemanticColors(
        isPlatformNative = false,
        background = Color.Unspecified,
        surface = Color.Unspecified,
        surfaceRaised = Color.Unspecified,
        labelPrimary = Color.Unspecified,
        labelSecondary = Color.Unspecified,
        accent = Color.Unspecified,
        destructive = Color.Unspecified,
        chrome = Color.Unspecified,
        chromeBorder = Color.Unspecified,
        chromeHighlight = Color.Unspecified,
        separator = Color.Unspecified,
        positive = Color.Unspecified,
        info = Color.Unspecified,
        warning = Color.Unspecified,
    )
}

private val LocalRyntraMotion = staticCompositionLocalOf { RyntraMotion(isReduced = false) }

object RyntraDesign {
    val colors: RyntraSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRyntraColors.current

    val motion: RyntraMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalRyntraMotion.current

    val isPlatformNative: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalRyntraColors.current.isPlatformNative

    val bottomContentPadding
        @Composable
        @ReadOnlyComposable
        get() = if (isPlatformNative) 112.dp else 188.dp

    // One outer surface language across project, organization, and summary cards.
    // Controls can still use the smaller Material 3 shape tokens below.
    val contentShape = RoundedCornerShape(16.dp)
    val chromeShape = RoundedCornerShape(22.dp)

    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    val title = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val sectionLabel = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    val caption = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
}

@Composable
fun RyntraMotionProvider(
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRyntraMotion provides RyntraMotion(isReduced = reduceMotion), content = content)
}

private val RyntraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

private val RyntraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
)

@Composable
fun RyntraTheme(
    themeStyle: ThemeStyle = ThemeStyle.Platform,
    appearanceMode: AppearanceMode = AppearanceMode.System,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isPlatformNative = themeStyle == ThemeStyle.Platform
    val isDark = if (!isPlatformNative) {
        true
    } else {
        when (appearanceMode) {
            AppearanceMode.System -> isSystemInDarkTheme()
            AppearanceMode.Light -> false
            AppearanceMode.Dark -> true
        }
    }
    val colorScheme = when {
        !isPlatformNative -> RyntraDarkColors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        isDark -> PlatformDarkColors
        else -> PlatformLightColors
    }
    val semanticColors = if (isPlatformNative) {
        RyntraSemanticColors(
            isPlatformNative = true,
            background = colorScheme.background,
            surface = colorScheme.surfaceContainerLow,
            surfaceRaised = colorScheme.surfaceContainerHigh,
            labelPrimary = colorScheme.onBackground,
            labelSecondary = colorScheme.onSurfaceVariant,
            accent = colorScheme.primary,
            destructive = colorScheme.error,
            chrome = colorScheme.surfaceContainer,
            chromeBorder = colorScheme.outlineVariant,
            chromeHighlight = colorScheme.surfaceTint.copy(alpha = 0.08f),
            separator = colorScheme.outlineVariant,
            positive = RyntraGreen,
            info = colorScheme.tertiary,
            warning = Color(0xFFFFB74D),
        )
    } else {
        RyntraSemanticColors(
            isPlatformNative = false,
            background = Color.Black,
            surface = Color(0xFF0C0C0E),
            surfaceRaised = Color(0xFF1C1C1E),
            labelPrimary = Color(0xFFF5F5F7),
            labelSecondary = Color(0xFF98989D),
            accent = RyntraGreen,
            destructive = Color(0xFFFF453A),
            chrome = Color(0xFF111113),
            chromeBorder = Color.White.copy(alpha = 0.14f),
            chromeHighlight = Color.White.copy(alpha = 0.05f),
            separator = Color(0xFF2C2C2E),
            positive = RyntraGreen,
            info = RyntraCyan,
            warning = Color(0xFFFFD60A),
        )
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
    }

    CompositionLocalProvider(LocalRyntraColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (isPlatformNative) Typography() else RyntraTypography,
            shapes = if (isPlatformNative) Shapes() else RyntraShapes,
        ) {
            CompositionLocalProvider(LocalContentColor provides semanticColors.labelPrimary) {
                content()
            }
        }
    }
}
