package com.ryntra.mobile.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.theme.RyntraDesign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

@Composable
internal fun RyntraIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

@Composable
internal fun RyntraProgressIndicator(color: Color, modifier: Modifier = Modifier) {
    // The hand-drawn arc belongs to the Ryntra style; Material draws its own.
    if (RyntraDesign.isPlatformNative) {
        CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = modifier)
        return
    }
    val rotation = if (RyntraDesign.motion.isReduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "Progress indicator")
        val animatedRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 850, easing = LinearEasing)),
            label = "Progress rotation",
        )
        animatedRotation
    }
    Canvas(
        modifier = modifier
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .rotate(rotation),
    ) {
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 275f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun RyntraSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (RyntraDesign.isPlatformNative) {
        // Material names this role titleSmall on onSurfaceVariant; the Ryntra style
        // uses its own uppercase accent caption instead.
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.semantics { heading() },
        )
        return
    }
    BasicText(
        text = text.uppercase(),
        style = RyntraDesign.sectionLabel.copy(
            color = RyntraDesign.colors.accent,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier.semantics { heading() },
    )
}

/**
 * A card surface in whichever language the active style speaks.
 *
 * The Ryntra style draws a near-black fill with a hairline edge. Material's expressive
 * style separates surfaces by tone alone, so in the platform style the same card is a
 * filled container on the theme's large shape with no outline. Screens used to draw the
 * Ryntra version unconditionally, which left iOS-like outlined cards inside Material.
 */
@Composable
internal fun Modifier.ryntraCard(ryntraShape: Shape): Modifier =
    if (RyntraDesign.isPlatformNative) {
        clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)
    } else {
        clip(ryntraShape)
            .background(RyntraDesign.colors.surface)
            .border(0.75.dp, RyntraDesign.colors.separator, ryntraShape)
    }

/**
 * A selectable tile — a range, a metric, a release channel.
 *
 * Material marks the chosen one with a secondary-container fill rather than an edge,
 * the same role its chips and navigation indicator use for "selected".
 */
@Composable
internal fun Modifier.ryntraChoice(
    isSelected: Boolean,
    ryntraShape: Shape,
    ryntraSelectedEdge: Color = RyntraDesign.colors.accent,
): Modifier =
    if (RyntraDesign.isPlatformNative) {
        val colors = MaterialTheme.colorScheme
        // Unselected sits a tone above surfaceContainer so it still reads when the
        // tile is placed on a card rather than on the page.
        clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) colors.secondaryContainer else colors.surfaceContainerHighest)
    } else {
        background(
            if (isSelected) RyntraDesign.colors.surfaceRaised else RyntraDesign.colors.surface,
            ryntraShape,
        ).border(0.75.dp, if (isSelected) ryntraSelectedEdge else RyntraDesign.colors.separator, ryntraShape)
    }

/**
 * A card that answers selection the way Material's expressive components do: it takes
 * the secondary-container tone, eases in slightly, and its corners open up. A tone
 * shift alone between two near-identical surfaces was too faint to read as "picked".
 */
@Composable
internal fun Modifier.expressiveSelectableCard(isSelected: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 20.dp,
        animationSpec = RyntraDesign.spatialSpec(),
        label = "Card corner",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = RyntraDesign.spatialSpec(),
        label = "Card scale",
    )
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.secondaryContainer else colors.surfaceContainer,
        animationSpec = RyntraDesign.effectsSpec(),
        label = "Card tone",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
        .clip(RoundedCornerShape(corner))
        .background(container)
}
