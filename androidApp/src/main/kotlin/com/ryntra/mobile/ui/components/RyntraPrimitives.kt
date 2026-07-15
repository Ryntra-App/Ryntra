package com.ryntra.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.theme.RyntraDesign

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
    Canvas(modifier = modifier.rotate(rotation)) {
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
    val isPlatformNative = RyntraDesign.isPlatformNative
    BasicText(
        text = if (isPlatformNative) text else text.uppercase(),
        style = RyntraDesign.sectionLabel.copy(
            color = if (isPlatformNative) RyntraDesign.colors.labelSecondary else RyntraDesign.colors.accent,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
    )
}
