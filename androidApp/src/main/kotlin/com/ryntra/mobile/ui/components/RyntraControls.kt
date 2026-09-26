package com.ryntra.mobile.ui.components

import com.ryntra.mobile.R
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.X
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.ui.theme.RyntraDesign
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription

@Composable
fun RyntraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val keyboardActions = onSearch?.let { KeyboardActions(onSearch = { it() }) } ?: KeyboardActions.Default
    if (RyntraDesign.isPlatformNative) {
        PlatformSearchField(value, onValueChange, placeholder, leadingIcon, keyboardActions, modifier)
        return
    }
    RyntraTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        leadingIconDescription = null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = keyboardActions,
        modifier = modifier,
    )
}

/**
 * Material 3's search field: a filled, fully rounded bar rather than an outlined form field,
 * so a search reads as a search and not as one more input in a form. It clears with one tap.
 */
@Composable
private fun PlatformSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardActions: KeyboardActions,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = CircleShape,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Lucide.X, contentDescription = stringResource(R.string.common_clear_search))
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = keyboardActions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = colors.onSurfaceVariant,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
        ),
        modifier = modifier.heightIn(min = 56.dp),
    )
}

@Composable
fun RyntraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    leadingIconDescription: String?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    if (RyntraDesign.isPlatformNative) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            label = label?.let { fieldLabel -> { Text(fieldLabel) } },
            // Material floats the label to the top of the field and then shows the
            // placeholder inside it, so the same text in both would read twice.
            placeholder = if (placeholder == label) {
                null
            } else {
                { Text(placeholder, maxLines = if (singleLine) 1 else minLines) }
            },
            // Multi-line fields keep icons top-aligned; Material centers leadingIcon by default.
            leadingIcon = if (singleLine) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = leadingIconDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
            prefix = if (!singleLine) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = leadingIconDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp, top = 2.dp)
                            .size(20.dp),
                    )
                }
            } else {
                null
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = modifier,
        )
        return
    }
    val colors = RyntraDesign.colors
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.destructive
            isFocused -> colors.accent.copy(alpha = 0.62f)
            else -> colors.separator
        },
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Field focus",
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = RyntraDesign.body.merge(TextStyle(color = colors.labelPrimary)),
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (singleLine) 46.dp else 96.dp)
                    .background(colors.surface, shape)
                    .border(
                        width = if (isFocused) 1.dp else 0.75.dp,
                        color = borderColor,
                        shape = shape,
                    )
                    .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 12.dp),
            ) {
                RyntraIcon(
                    icon = leadingIcon,
                    contentDescription = leadingIconDescription,
                    tint = colors.accent,
                    modifier = Modifier
                        .padding(top = if (singleLine) 0.dp else 2.dp)
                        .size(19.dp),
                )
                Box(
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                ) {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = RyntraDesign.body.copy(color = colors.labelSecondary),
                            maxLines = if (singleLine) 1 else minLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun RyntraPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    if (RyntraDesign.isPlatformNative) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    val colors = RyntraDesign.colors
    val background by animateColorAsState(
        targetValue = if (enabled) colors.accent else colors.surfaceRaised,
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Primary button background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) Color.Black else colors.labelSecondary.copy(alpha = 0.55f),
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Primary button content",
    )
    val semanticsModifier = if (enabled && !isLoading) Modifier else Modifier.semantics { disabled() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading && !RyntraDesign.motion.isReduced) 0.975f else 1f,
        animationSpec = tween(RyntraDesign.motion.duration(110)),
        label = "Primary button press",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(semanticsModifier)
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .background(background, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
    ) {
        if (isLoading) {
            RyntraProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp))
        } else {
            ButtonLabel(text = text, icon = icon, color = contentColor)
        }
    }
}

@Composable
fun RyntraSecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
) {
    if (RyntraDesign.isPlatformNative) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    val colors = RyntraDesign.colors
    val color = if (isDestructive) colors.destructive else colors.accent
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !RyntraDesign.motion.isReduced) 0.975f else 1f,
        animationSpec = tween(RyntraDesign.motion.duration(110)),
        label = "Secondary button press",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .background(colors.surfaceRaised, shape)
            .border(0.75.dp, color.copy(alpha = if (enabled) 0.26f else 0.10f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        ButtonLabel(text = text, icon = icon, color = color.copy(alpha = if (enabled) 1f else 0.42f))
    }
}

@Composable
fun RyntraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
) {
    if (RyntraDesign.isPlatformNative) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            // Material's own switch-with-icon: the check makes "on" legible without
            // relying on the track colour alone.
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        )
        return
    }
    val motion = RyntraDesign.motion
    val trackColor by animateColorAsState(
        targetValue = if (checked) RyntraDesign.colors.accent else RyntraDesign.colors.surfaceRaised,
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
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(trackColor)
            .border(0.75.dp, Color.White.copy(alpha = if (checked) 0.08f else 0.12f), CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                enabled = enabled,
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

// Material's own roles for a segmented button: a secondaryContainer active segment
// on an outline border. Anything else drifts away from the rest of the app under
// dynamic colour, where primary and secondary are deliberately different hues.
@Composable
fun ryntraSegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors()

@Composable
private fun ButtonLabel(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RyntraIcon(icon = icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        BasicText(
            text = text,
            style = RyntraDesign.body.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Colours for an option in a connected choice group.
 *
 * Material's default unchecked container is surfaceContainer — the very tone the platform
 * style fills its settings tiles and cards with — so on those surfaces an unchecked option
 * vanished into the tile and read as bare text. One tone higher keeps every option visible
 * as a button wherever the group sits.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ryntraChoiceToggleColors(): ToggleButtonColors = ToggleButtonDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurface,
)
