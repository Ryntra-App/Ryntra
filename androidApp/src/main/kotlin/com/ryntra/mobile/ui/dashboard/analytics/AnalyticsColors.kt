package com.ryntra.mobile.ui.dashboard.analytics

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class AnalyticsColorPalette(
    val blue: Color,
    val orange: Color,
    val green: Color,
    val pink: Color,
    val cyan: Color,
    val violet: Color,
    val red: Color,
    val gold: Color,
) {
    val series: List<Color>
        get() = listOf(blue, orange, green, pink, cyan, violet, red, gold)
}

@Composable
internal fun analyticsColors(): AnalyticsColorPalette = if (isSystemInDarkTheme()) {
    AnalyticsColorPalette(
        blue = Color(0xFF65A9FF),
        orange = Color(0xFFFFA24B),
        green = Color(0xFF4FD17C),
        pink = Color(0xFFF27BB8),
        cyan = Color(0xFF45C8D0),
        violet = Color(0xFFAA98FF),
        red = Color(0xFFFF7078),
        gold = Color(0xFFE9C653),
    )
} else {
    AnalyticsColorPalette(
        blue = Color(0xFF0768C8),
        orange = Color(0xFFA94C08),
        green = Color(0xFF187B3B),
        pink = Color(0xFFAE286A),
        cyan = Color(0xFF08767D),
        violet = Color(0xFF6854BE),
        red = Color(0xFFC63840),
        gold = Color(0xFF856400),
    )
}

@Composable
internal fun AnalyticsMetric.color(): Color {
    val colors = analyticsColors()
    return when (this) {
        AnalyticsMetric.Downloads -> colors.blue
        AnalyticsMetric.Views -> colors.violet
        AnalyticsMetric.Playtime -> colors.orange
        AnalyticsMetric.Revenue -> colors.green
    }
}

@Composable
internal fun analyticsSeriesColor(projectId: String): Color {
    return projectSeriesColor(projectId, analyticsColors().series)
}

internal fun projectSeriesColor(projectId: String, series: List<Color>): Color =
    series[(projectId.hashCode() and Int.MAX_VALUE) % series.size]
