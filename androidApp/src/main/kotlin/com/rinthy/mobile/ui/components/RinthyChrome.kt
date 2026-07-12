package com.rinthy.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rinthy.mobile.ui.theme.RinthyDesign
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Immutable
data class RinthyTab(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun RinthyTopBar(
    title: String,
    avatarUrl: String?,
    avatarDescription: String,
    isRefreshing: Boolean,
    onAvatarClick: () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    showAvatar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(76.dp)
            .padding(start = 20.dp, end = 18.dp),
    ) {
        if (navigationIcon != null) {
            IconButton(onClick = onNavigationClick) {
                Icon(navigationIcon, contentDescription = navigationDescription)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isRefreshing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(19.dp),
            )
        }
        if (showAvatar) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = avatarDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button, onClick = onAvatarClick),
            )
        }
    }
}

@Composable
fun RinthyTabBar(
    tabs: List<RinthyTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        shape = RoundedCornerShape(28.dp),
        hazeState = hazeState,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                RinthyTabItem(
                    tab = tab,
                    isSelected = selectedIndex == index,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.RinthyTabItem(
    tab: RinthyTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "Tab press",
    )
    val color = if (isSelected) {
        RinthyDesign.colors.positive
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val itemShape = RoundedCornerShape(22.dp)
    val selectedFill = if (isSelected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.075f)
    } else {
        Color.Transparent
    }
    val selectedBorder = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
            RinthyDesign.colors.positive.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .height(60.dp)
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clip(itemShape)
            .background(selectedFill)
            .then(if (isSelected) Modifier.border(0.75.dp, selectedBorder, itemShape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { selected = isSelected },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 38.dp, height = 30.dp)
                .clip(RoundedCornerShape(11.dp))
        ) {
            Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        }
        Text(
            text = tab.label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlassSurface(
    modifier: Modifier,
    shape: RoundedCornerShape,
    hazeState: HazeState,
    content: @Composable () -> Unit,
) {
    val chrome = RinthyDesign.colors.chrome
    val background = MaterialTheme.colorScheme.background
    val glassTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val hazeStyle = remember(chrome, background, glassTint) {
        HazeStyle(
            backgroundColor = background,
            tint = HazeTint(glassTint),
            blurRadius = 18.dp,
            noiseFactor = 0.01f,
            fallbackTint = HazeTint(chrome.copy(alpha = 0.88f)),
        )
    }
    val glassBorder = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            RinthyDesign.colors.positive.copy(alpha = 0.08f),
        ),
    )
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        border = BorderStroke(0.8.dp, glassBorder),
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.background.copy(alpha = 0.28f),
                spotColor = MaterialTheme.colorScheme.background.copy(alpha = 0.38f),
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = hazeStyle),
        content = content,
    )
}
