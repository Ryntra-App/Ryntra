package com.rinthy.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
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
    canRefresh: Boolean,
    refreshIcon: ImageVector,
    onRefresh: () -> Unit,
    onAvatarClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        shape = RinthyDesign.chromeShape,
        hazeState = hazeState,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(start = 16.dp, end = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (canRefresh) {
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(19.dp),
                        )
                    } else {
                        Icon(refreshIcon, contentDescription = "Refresh")
                    }
                }
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
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
                .height(68.dp)
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
    val color = if (isSelected) {
        RinthyDesign.colors.positive
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .height(68.dp)
            .padding(horizontal = 3.dp, vertical = 7.dp)
            .background(
                if (isSelected) color.copy(alpha = 0.13f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(20.dp),
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Tab, onClick = onClick)
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
    val hazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = HazeTint(chrome.copy(alpha = 0.58f)),
        blurRadius = 26.dp,
        noiseFactor = 0.035f,
        fallbackTint = HazeTint(chrome),
    )
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        border = BorderStroke(1.dp, RinthyDesign.colors.chromeBorder),
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
