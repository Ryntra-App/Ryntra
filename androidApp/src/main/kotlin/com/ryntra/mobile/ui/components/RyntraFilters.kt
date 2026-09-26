package com.ryntra.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R

/**
 * A filter chip that opens a menu or a sheet: Material's dropdown chip, with a leading icon
 * naming the facet and a chevron saying it opens something. It reads as selected while the
 * facet it opens narrows the list.
 */
@Composable
internal fun RyntraMenuChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
        trailingIcon = {
            Icon(Lucide.ChevronDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
        },
        modifier = modifier,
    )
}

/**
 * The bottom sheet every filter set opens in: a title with a reset action, the sections, and
 * one confirming button. Changes apply as they are made; "Done" only closes the sheet.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RyntraFilterSheet(
    title: String,
    canReset: Boolean,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (canReset) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.filters_reset)) }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 20.dp),
                content = content,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(bottom = 16.dp),
            ) {
                Text(stringResource(R.string.filters_done))
            }
        }
    }
}

/** One facet in a filter sheet: an icon and a title over its options. */
@Composable
internal fun RyntraFilterSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        content()
    }
}

/**
 * Selectable options as filter chips, wrapping onto new lines. Long lists — Minecraft
 * versions run to dozens — start folded at [foldedCount] with a way to show the rest.
 */
@Composable
internal fun RyntraFilterChips(
    values: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    label: (String) -> String = { it },
    foldedCount: Int = Int.MAX_VALUE,
    leadingIcon: (@Composable (String) -> Unit)? = null,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    // A selection past the fold is never hidden.
    val visible = if (isExpanded || values.size <= foldedCount) {
        values
    } else {
        values.take(foldedCount) + values.drop(foldedCount).filter(isSelected)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        visible.forEach { value ->
            FilterChip(
                selected = isSelected(value),
                onClick = { onToggle(value) },
                label = { Text(label(value)) },
                leadingIcon = leadingIcon?.let { icon -> { icon(value) } },
            )
        }
    }
    if (values.size > foldedCount) {
        TextButton(onClick = { isExpanded = !isExpanded }) {
            Text(
                if (isExpanded) {
                    stringResource(R.string.filters_show_less)
                } else {
                    stringResource(R.string.filters_show_all, values.size)
                },
            )
        }
    }
}
