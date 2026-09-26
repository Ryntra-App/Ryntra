package com.ryntra.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Which of Material's three accent families a stat tile is washed in. */
internal enum class StatTone { Primary, Secondary, Tertiary }

/**
 * A figure the user came to see, set large on a tonal container.
 *
 * Material's expressive stat tiles lead with the number rather than the label. The
 * number shrinks to fit instead of ellipsizing: a creator with seven-figure downloads
 * would otherwise see "1,234,…" in exactly the place they look first.
 */
@Composable
internal fun ExpressiveStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    tone: StatTone,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) = when (tone) {
        StatTone.Primary -> colors.primaryContainer to colors.onPrimaryContainer
        StatTone.Secondary -> colors.secondaryContainer to colors.onSecondaryContainer
        StatTone.Tertiary -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    StatTileContent(icon, label, value, container, onContainer, modifier)
}

@Composable
private fun StatTileContent(
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(22.dp))
        RollingText(
            text = value,
            color = onContainer,
            style = MaterialTheme.typography.headlineMedium,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 32.sp, stepSize = 1.sp),
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = label,
            color = onContainer,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
