package com.ryntra.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.ryntra.mobile.ui.theme.RyntraDesign

/**
 * A figure that rolls in from below like a counter when it changes, instead of being
 * swapped in place — so a refreshed number is noticed rather than missed.
 *
 * Shrinks to fit rather than ellipsizing when [autoSize] is given: a truncated amount of
 * money or downloads is worse than a smaller one.
 */
@Composable
internal fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    autoSize: TextAutoSize? = null,
) {
    val rollSpec = RyntraDesign.spatialSpec<IntOffset>()
    val fadeSpec = RyntraDesign.effectsSpec<Float>()
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(rollSpec) { it / 2 } + fadeIn(fadeSpec))
                .togetherWith(slideOutVertically(rollSpec) { -it / 2 } + fadeOut(fadeSpec))
                .using(SizeTransform(clip = true))
        },
        label = "Rolling text",
        modifier = modifier,
    ) { shown ->
        Text(
            text = shown,
            color = color,
            style = style,
            fontWeight = fontWeight,
            maxLines = 1,
            autoSize = autoSize,
        )
    }
}
