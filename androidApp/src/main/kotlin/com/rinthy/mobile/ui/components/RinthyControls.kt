package com.rinthy.mobile.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import com.rinthy.mobile.ui.theme.RinthyDesign

@Composable
fun RinthySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    RinthyTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        leadingIconDescription = null,
        modifier = modifier,
    )
}

@Composable
fun RinthyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    leadingIconDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    if (RinthyDesign.isPlatformNative) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            placeholder = { Text(placeholder, maxLines = if (singleLine) 1 else minLines) },
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
            modifier = modifier,
        )
        return
    }
    val colors = RinthyDesign.colors
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) colors.accent.copy(alpha = 0.62f) else colors.separator,
        animationSpec = tween(RinthyDesign.motion.duration(160)),
        label = "Field focus",
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = RinthyDesign.body.merge(TextStyle(color = colors.labelPrimary)),
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
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
                RinthyIcon(
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
                            style = RinthyDesign.body.copy(color = colors.labelSecondary),
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
fun RinthyPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    if (RinthyDesign.isPlatformNative) {
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
                Text(text)
            }
        }
        return
    }
    val colors = RinthyDesign.colors
    val background by animateColorAsState(
        targetValue = if (enabled) colors.accent else colors.surfaceRaised,
        animationSpec = tween(RinthyDesign.motion.duration(160)),
        label = "Primary button background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) Color.Black else colors.labelSecondary.copy(alpha = 0.55f),
        animationSpec = tween(RinthyDesign.motion.duration(160)),
        label = "Primary button content",
    )
    val semanticsModifier = if (enabled) Modifier else Modifier.semantics { disabled() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.975f else 1f,
        animationSpec = tween(RinthyDesign.motion.duration(110)),
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
            RinthyProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp))
        } else {
            ButtonLabel(text = text, icon = icon, color = contentColor)
        }
    }
}

@Composable
fun RinthySecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
) {
    if (RinthyDesign.isPlatformNative) {
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
            Text(text)
        }
        return
    }
    val colors = RinthyDesign.colors
    val color = if (isDestructive) colors.destructive else colors.accent
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.975f else 1f,
        animationSpec = tween(RinthyDesign.motion.duration(110)),
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
fun rinthySegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    activeBorderColor = MaterialTheme.colorScheme.primary,
    inactiveContainerColor = Color.Transparent,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun ButtonLabel(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RinthyIcon(icon = icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        BasicText(
            text = text,
            style = RinthyDesign.body.copy(color = color),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
