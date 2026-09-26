package com.ryntra.mobile.ui.dashboard.account

import com.ryntra.mobile.ui.components.RyntraChoiceGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.preferences.AppLanguage
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.ThemeStyle
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.ryntraCard
import com.ryntra.mobile.ui.theme.RyntraDesign

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RyntraSectionLabel(title, modifier = Modifier.padding(start = 4.dp, bottom = 9.dp))
        if (RyntraDesign.isPlatformNative) {
            // Material's segmented list: every row is its own tile with a thin seam of
            // background between them. The group clips to a large shape and each tile
            // to a small one, so the ends of the group round off generously while the
            // seams inside stay tight.
            Column(
                verticalArrangement = Arrangement.spacedBy(SettingsTileSeam),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
            ) {
                content()
            }
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ryntraCard(RoundedCornerShape(12.dp)),
        ) {
            content()
        }
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    // A control that belongs to this row — a picker under its heading. It sits in the
    // row's own tile so the two read as one setting rather than two.
    below: (@Composable () -> Unit)? = null,
) {
    if (RyntraDesign.isPlatformNative) {
        PlatformSettingsRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            isDestructive = isDestructive,
            trailing = trailing,
            below = below,
        )
        return
    }
    RyntraSettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        isDestructive = isDestructive,
        trailing = trailing,
    )
    below?.invoke()
}

@Composable
private fun RyntraSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    isDestructive: Boolean,
    trailing: (@Composable RowScope.() -> Unit)?,
) {
    val contentColor = if (isDestructive) RyntraDesign.colors.destructive else RyntraDesign.colors.labelPrimary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.992f else 1f,
        animationSpec = tween(RyntraDesign.motion.duration(100)),
        label = "Settings row press",
    )
    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.72f else 1f,
        animationSpec = tween(RyntraDesign.motion.duration(100)),
        label = "Settings row opacity",
    )
    val rowModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
                alpha = pressedAlpha
            }
            .then(rowModifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isDestructive) contentColor.copy(alpha = 0.10f) else RyntraDesign.colors.accent.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(9.dp),
                ),
        ) {
            RyntraIcon(
                icon = icon,
                contentDescription = null,
                tint = if (isDestructive) contentColor else RyntraDesign.colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            RyntraIcon(
                icon = Lucide.ChevronRight,
                contentDescription = null,
                tint = RyntraDesign.colors.labelSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PlatformSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    isDestructive: Boolean,
    trailing: (@Composable RowScope.() -> Unit)?,
    below: (@Composable () -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val titleColor = if (isDestructive) colors.error else colors.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(colors.surfaceContainer)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = subtitle?.let { supportingText ->
                {
                    Text(
                        text = supportingText,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = { SettingsIconBadge(icon = icon, isDestructive = isDestructive) },
            trailingContent = {
                when {
                    trailing != null -> Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
                    onClick != null -> Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        below?.invoke()
    }
}

// The glyph sits in a tonal circle, as Android's own settings draw it, so a column of
// rows reads as a set of distinct places rather than a list of lines.
@Composable
private fun SettingsIconBadge(icon: ImageVector, isDestructive: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (isDestructive) colors.errorContainer else colors.secondaryContainer,
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) colors.onErrorContainer else colors.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val SettingsTileSeam = 2.dp

@Composable
internal fun SettingsDivider() {
    // Platform rows are separate tiles; the seam between them is the divider.
    if (RyntraDesign.isPlatformNative) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 61.dp)
            .height(0.75.dp)
            .background(RyntraDesign.colors.separator),
    )
}

@Composable
internal fun GlassQualityPicker(
    selected: GlassQuality,
    onSelect: (GlassQuality) -> Unit,
) {
    SettingsSegmentedPicker(
        options = GlassQuality.entries,
        selected = selected,
        label = { quality ->
            stringResource(
                when (quality) {
                    GlassQuality.Performance -> R.string.settings_glass_fast
                    GlassQuality.Balanced -> R.string.settings_glass_balanced
                    GlassQuality.Quality -> R.string.settings_glass_best
                },
            )
        },
        onSelect = onSelect,
    )
}

@Composable
internal fun ThemeStylePicker(
    selected: ThemeStyle,
    onSelect: (ThemeStyle) -> Unit,
) {
    SettingsSegmentedPicker(
        options = ThemeStyle.entries,
        selected = selected,
        label = { style ->
            stringResource(
                if (style == ThemeStyle.Platform) R.string.settings_theme_platform else R.string.settings_theme_ryntra,
            )
        },
        onSelect = onSelect,
    )
}

@Composable
internal fun AppearanceModePicker(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
) {
    SettingsSegmentedPicker(
        options = AppearanceMode.entries,
        selected = selected,
        label = { mode ->
            stringResource(
                when (mode) {
                    AppearanceMode.System -> R.string.settings_mode_system
                    AppearanceMode.Light -> R.string.settings_mode_light
                    AppearanceMode.Dark -> R.string.settings_mode_dark
                },
            )
        },
        onSelect = onSelect,
    )
}

@Composable
internal fun AppLanguagePicker(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedLabel = selected.displayName()
    val isPlatformNative = RyntraDesign.isPlatformNative
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (isPlatformNative) {
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { isExpanded = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = true }
                    .padding(start = 61.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)
            },
        ) {
            Text(
                text = selectedLabel,
                color = RyntraDesign.colors.labelPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            RyntraIcon(
                icon = Lucide.ChevronDown,
                contentDescription = null,
                tint = RyntraDesign.colors.labelSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName()) },
                    onClick = {
                        isExpanded = false
                        onSelect(language)
                    },
                    trailingIcon = if (language == selected) {
                        {
                            RyntraIcon(
                                icon = Lucide.Check,
                                contentDescription = null,
                                tint = RyntraDesign.colors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun AppLanguage.displayName(): String =
    if (this == AppLanguage.System) stringResource(R.string.settings_language_system) else label

@Composable
private fun <T> SettingsSegmentedPicker(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    if (RyntraDesign.isPlatformNative) {
        PlatformChoiceGroup(options = options, selected = selected, label = label, onSelect = onSelect)
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 61.dp, end = 12.dp, bottom = 12.dp)
            .background(RyntraDesign.colors.background, RoundedCornerShape(9.dp))
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) RyntraDesign.colors.surfaceRaised else Color.Transparent,
                animationSpec = tween(RyntraDesign.motion.duration(180)),
                label = "Glass quality",
            )
            Text(
                text = label(option),
                color = if (isSelected) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(background)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
            )
        }
    }
}

// Expressive replaces the segmented button with a connected button group: the chosen
// option morphs to a full pill while its neighbours keep their squared inner edges.
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> PlatformChoiceGroup(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    RyntraChoiceGroup(
        options = options,
        isChecked = { it == selected },
        onToggle = onSelect,
        label = label,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    )
}
