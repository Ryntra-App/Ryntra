package com.ryntra.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide

/**
 * The app's one control for picking among two to four fixed options inside a screen or form:
 * Material 3 Expressive's connected button group, where the chosen option morphs to a pill.
 *
 * Single choice (a range, a sort order, a consent model) and multiple choice (what AI was used
 * for) share the same look so a form never mixes segmented buttons with chips for the same
 * kind of question. A multiple-choice option also shows a check, since "several may be on" is
 * not visible from the shape alone. Filters over a list stay chips — that is a different job.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun <T> RyntraChoiceGroup(
    options: List<T>,
    isChecked: (T) -> Boolean,
    onToggle: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    isMultipleChoice: Boolean = false,
    enabled: Boolean = true,
    icon: ((T) -> ImageVector)? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.size),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, option ->
            val checked = isChecked(option)
            ToggleButton(
                checked = checked,
                onCheckedChange = { onToggle(option) },
                enabled = enabled,
                colors = ryntraChoiceToggleColors(),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = contentPadding,
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = if (isMultipleChoice) Role.Checkbox else Role.RadioButton },
            ) {
                val leadingIcon = if (isMultipleChoice && checked) Lucide.Check else icon?.invoke(option)
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                }
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
