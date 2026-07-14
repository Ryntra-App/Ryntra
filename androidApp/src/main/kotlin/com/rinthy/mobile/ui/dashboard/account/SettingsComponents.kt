package com.rinthy.mobile.ui.dashboard.account

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.rinthy.mobile.preferences.AppLanguage
import com.rinthy.mobile.preferences.GlassQuality
import com.rinthy.mobile.preferences.AppearanceMode
import com.rinthy.mobile.preferences.ThemeStyle
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.components.RinthySectionLabel
import com.rinthy.mobile.ui.components.rinthySegmentedButtonColors
import com.rinthy.mobile.ui.theme.RinthyDesign

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RinthySectionLabel(title, modifier = Modifier.padding(start = 4.dp, bottom = 9.dp))
        if (RinthyDesign.isPlatformNative) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                content()
            }
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RinthyDesign.colors.surface)
                .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(12.dp)),
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
) {
    if (RinthyDesign.isPlatformNative) {
        PlatformSettingsRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            isDestructive = isDestructive,
            trailing = trailing,
        )
        return
    }
    val contentColor = if (isDestructive) RinthyDesign.colors.destructive else RinthyDesign.colors.labelPrimary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.992f else 1f,
        animationSpec = tween(RinthyDesign.motion.duration(100)),
        label = "Settings row press",
    )
    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.72f else 1f,
        animationSpec = tween(RinthyDesign.motion.duration(100)),
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
                    color = if (isDestructive) contentColor.copy(alpha = 0.10f) else RinthyDesign.colors.accent.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(9.dp),
                ),
        ) {
            RinthyIcon(
                icon = icon,
                contentDescription = null,
                tint = if (isDestructive) contentColor else RinthyDesign.colors.accent,
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
                    color = RinthyDesign.colors.labelSecondary,
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
            RinthyIcon(
                icon = Lucide.ChevronRight,
                contentDescription = null,
                tint = RinthyDesign.colors.labelSecondary,
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
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val interactionModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let { supportingText ->
            {
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) contentColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            when {
                trailing != null -> Row(content = trailing)
                onClick != null -> Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = interactionModifier,
    )
}

@Composable
internal fun SettingsDivider() {
    if (RinthyDesign.isPlatformNative) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 61.dp)
            .height(0.75.dp)
            .background(RinthyDesign.colors.separator),
    )
}

@Composable
internal fun RinthySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
) {
    if (RinthyDesign.isPlatformNative) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        )
        return
    }
    val motion = RinthyDesign.motion
    val trackColor by animateColorAsState(
        targetValue = if (checked) RinthyDesign.colors.accent else RinthyDesign.colors.surfaceRaised,
        animationSpec = tween(motion.duration(180)),
        label = "Switch track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 19.dp else 2.dp,
        animationSpec = tween(motion.duration(180)),
        label = "Switch thumb",
    )
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(0.75.dp, Color.White.copy(alpha = if (checked) 0.08f else 0.12f), CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 2.dp)
                .size(24.dp)
                .background(Color(0xFFF5F5F7), CircleShape),
        )
    }
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
                if (style == ThemeStyle.Platform) R.string.settings_theme_platform else R.string.settings_theme_rinthy,
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
    SettingsSegmentedPicker(
        options = AppLanguage.entries,
        selected = selected,
        label = { language ->
            stringResource(
                when (language) {
                    AppLanguage.System -> R.string.settings_language_system
                    AppLanguage.English -> R.string.settings_language_english
                    AppLanguage.Russian -> R.string.settings_language_russian
                },
            )
        },
        onSelect = onSelect,
    )
}

@Composable
private fun <T> SettingsSegmentedPicker(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    if (RinthyDesign.isPlatformNative) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 61.dp, end = 12.dp, bottom = 12.dp),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = rinthySegmentedButtonColors(),
                    label = {
                        Text(
                            text = label(option),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 61.dp, end = 12.dp, bottom = 12.dp)
            .background(RinthyDesign.colors.background, RoundedCornerShape(9.dp))
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) RinthyDesign.colors.surfaceRaised else Color.Transparent,
                animationSpec = tween(RinthyDesign.motion.duration(180)),
                label = "Glass quality",
            )
            Text(
                text = label(option),
                color = if (isSelected) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary,
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
