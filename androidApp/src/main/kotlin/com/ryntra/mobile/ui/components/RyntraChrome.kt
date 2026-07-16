package com.ryntra.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.ui.theme.RyntraDesign
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Immutable
data class RyntraTab(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun RyntraTopBar(
    title: String,
    avatarUrl: String?,
    avatarDescription: String,
    isRefreshing: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    showAvatar: Boolean = true,
    onNotificationsClick: (() -> Unit)? = null,
    unreadNotificationCount: Int = 0,
    notificationsDescription: String = "",
) {
    if (RyntraDesign.isPlatformNative) {
        PlatformTopBar(
            title = title,
            avatarUrl = avatarUrl,
            avatarDescription = avatarDescription,
            isRefreshing = isRefreshing,
            onAvatarClick = onAvatarClick,
            navigationIcon = navigationIcon,
            navigationDescription = navigationDescription,
            onNavigationClick = onNavigationClick,
            showAvatar = showAvatar,
            onNotificationsClick = onNotificationsClick,
            unreadNotificationCount = unreadNotificationCount,
            notificationsDescription = notificationsDescription,
            modifier = modifier,
        )
        return
    }
    val colors = RyntraDesign.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(84.dp)
            .background(colors.background)
            .padding(start = if (navigationIcon == null) 20.dp else 8.dp, end = 18.dp),
    ) {
        if (navigationIcon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onNavigationClick),
            ) {
                RyntraIcon(
                    icon = navigationIcon,
                    contentDescription = navigationDescription,
                    tint = colors.accent,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        Crossfade(
            targetState = title,
            animationSpec = tween(RyntraDesign.motion.duration(180)),
            label = "Top bar title",
            modifier = Modifier.weight(1f),
        ) { currentTitle ->
            BasicText(
                text = currentTitle,
                style = RyntraDesign.largeTitle.copy(color = colors.labelPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isRefreshing) {
            RyntraProgressIndicator(
                color = colors.labelSecondary,
                modifier = Modifier.padding(end = 12.dp).size(19.dp),
            )
        }
        if (onNotificationsClick != null) {
            NotificationBell(
                unreadCount = unreadNotificationCount,
                onClick = onNotificationsClick,
                tint = colors.labelSecondary,
                contentDescription = notificationsDescription,
            )
        }
        if (showAvatar) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = avatarDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceRaised)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable(role = Role.Button, onClick = onAvatarClick),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlatformTopBar(
    title: String,
    avatarUrl: String?,
    avatarDescription: String,
    isRefreshing: Boolean,
    onAvatarClick: () -> Unit,
    navigationIcon: ImageVector?,
    navigationDescription: String?,
    onNavigationClick: () -> Unit,
    showAvatar: Boolean,
    onNotificationsClick: (() -> Unit)?,
    unreadNotificationCount: Int,
    notificationsDescription: String,
    modifier: Modifier,
) {
    TopAppBar(
        title = {
            Crossfade(
                targetState = title,
                animationSpec = tween(RyntraDesign.motion.duration(180)),
                label = "Top app bar title",
            ) { currentTitle ->
                Text(currentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            navigationIcon?.let { icon ->
                IconButton(onClick = onNavigationClick) {
                    Icon(icon, contentDescription = navigationDescription)
                }
            }
        },
        actions = {
            if (isRefreshing) {
                RyntraProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(18.dp),
                )
            }
            if (onNotificationsClick != null) {
                NotificationBell(
                    unreadCount = unreadNotificationCount,
                    onClick = onNotificationsClick,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = notificationsDescription,
                )
            }
            if (showAvatar) {
                IconButton(onClick = onAvatarClick) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = avatarDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier,
    )
}

@Composable
private fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
    tint: Color,
    contentDescription: String,
) {
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(Lucide.Bell, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (unreadCount > 9) 18.dp else 15.dp)
                    .background(RyntraDesign.colors.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun RyntraTabBar(
    tabs: List<RyntraTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    glassQuality: GlassQuality = GlassQuality.Balanced,
    modifier: Modifier = Modifier,
) {
    if (RyntraDesign.isPlatformNative) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        return
    }
    GlassSurface(
        hazeState = hazeState,
        quality = glassQuality,
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                RyntraTabItem(
                    tab = tab,
                    isSelected = selectedIndex == index,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.RyntraTabItem(
    tab: RyntraTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = RyntraDesign.colors
    val motion = RyntraDesign.motion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = motion.duration(120)),
        label = "Tab press",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.labelPrimary.copy(alpha = 0.78f),
        animationSpec = tween(durationMillis = motion.duration(180)),
        label = "Tab content",
    )
    val selectionColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xD909090B) else Color.Transparent,
        animationSpec = tween(durationMillis = motion.duration(180)),
        label = "Tab selection",
    )
    val selectionBorder by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = motion.duration(180)),
        label = "Tab border",
    )
    val itemShape = RoundedCornerShape(28.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .height(60.dp)
            .padding(horizontal = 2.dp)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clip(itemShape)
            .background(selectionColor, itemShape)
            .border(0.75.dp, selectionBorder, itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { selected = isSelected },
    ) {
        RyntraIcon(
            icon = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(23.dp),
        )
        BasicText(
            text = tab.label,
            style = RyntraDesign.caption.copy(
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalHazeApi::class)
private fun GlassSurface(
    hazeState: HazeState,
    quality: GlassQuality,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = RyntraDesign.colors
    val shape = RoundedCornerShape(35.dp)
    val blurRadius = when (quality) {
        GlassQuality.Performance -> 12.dp
        GlassQuality.Balanced -> 15.dp
        GlassQuality.Quality -> 18.dp
    }
    val inputScale = when (quality) {
        GlassQuality.Performance -> 0.30f
        GlassQuality.Balanced -> 0.40f
        GlassQuality.Quality -> 0.55f
    }
    val hazeStyle = remember(colors.background, colors.chrome, blurRadius) {
        HazeStyle(
            backgroundColor = colors.background,
            tint = HazeTint(Color(0x940B0B0D)),
            blurRadius = blurRadius,
            noiseFactor = 0f,
            fallbackTint = HazeTint(colors.chrome),
        )
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.48f),
                spotColor = Color.Black.copy(alpha = 0.62f),
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = hazeStyle) {
                this.inputScale = HazeInputScale.Fixed(inputScale)
            }
            .border(0.75.dp, colors.chromeBorder, shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(0.5.dp, colors.chromeHighlight, RoundedCornerShape(34.dp)),
        )
        content()
    }
}
