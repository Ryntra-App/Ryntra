package com.ryntra.mobile.ui.dashboard.project.disclosures

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleDollarSign
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.RyntraSwitch
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.components.ryntraSegmentedButtonColors
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.DisclosureRules
import com.ryntra.shared.model.DerivativeSource
import com.ryntra.shared.model.DisclosureType
import com.ryntra.shared.model.ProjectDisclosure

/**
 * One disclosure: a switch that declares it, and — once declared — the detail Modrinth requires
 * for that particular disclosure.
 */
@Composable
internal fun DisclosureCard(
    entry: ProjectDisclosure,
    canEdit: Boolean,
    onChange: (ProjectDisclosure) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = RyntraDesign.motion
    val canToggle = canEdit && (entry.canDisable || !entry.enabled) && entry.canEdit
    val canEditDetails = canEdit && entry.canEdit
    val title = entry.type.title()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(10.dp),
                        ),
                ) {
                    RyntraIcon(
                        icon = entry.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.type.description(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                RyntraSwitch(
                    checked = entry.enabled,
                    onCheckedChange = { onChange(entry.withEnabled(it)) },
                    contentDescription = title,
                    enabled = canToggle,
                )
            }

            if (entry.setByModerator || !entry.canDisable) {
                ModeratorLockNote(entry)
            }

            AnimatedVisibility(
                visible = entry.enabled,
                enter = expandVertically(animationSpec = tween(motion.duration(170))) +
                    fadeIn(animationSpec = tween(motion.duration(120))),
                exit = shrinkVertically(animationSpec = tween(motion.duration(120))) +
                    fadeOut(animationSpec = tween(motion.duration(80))),
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DisclosureEditor(entry = entry, enabled = canEditDetails, onChange = onChange)
                }
            }
        }
    }
}

@Composable
private fun ModeratorLockNote(entry: ProjectDisclosure) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        RyntraIcon(
            icon = Lucide.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(
                if (entry.canEdit) R.string.disclosures_locked_hint else R.string.disclosures_fully_locked_hint,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun DisclosureEditor(
    entry: ProjectDisclosure,
    enabled: Boolean,
    onChange: (ProjectDisclosure) -> Unit,
) {
    when (entry.type) {
        DisclosureType.AiContent -> {
            DisclosureFieldLabel(stringResource(R.string.disclosures_ai_uses))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                DisclosureRules.aiUsages.forEach { use ->
                    FilterChip(
                        selected = entry.hasUse(use),
                        enabled = enabled,
                        onClick = { onChange(entry.withUse(use, !entry.hasUse(use))) },
                        label = { Text(use.label()) },
                    )
                }
            }
            NoteField(
                label = stringResource(R.string.disclosures_explanation_optional),
                value = entry.note,
                placeholder = stringResource(R.string.disclosures_ai_note_hint),
                enabled = enabled,
                onChange = { onChange(entry.withNote(it)) },
            )
        }

        DisclosureType.Advertisements -> NoteField(
            label = stringResource(R.string.disclosures_explanation),
            value = entry.note,
            placeholder = stringResource(R.string.disclosures_ads_note_hint),
            enabled = enabled,
            onChange = { onChange(entry.withNote(it)) },
        )

        DisclosureType.EpilepsyTriggers -> NoteField(
            label = stringResource(R.string.disclosures_explanation),
            value = entry.note,
            placeholder = stringResource(R.string.disclosures_epilepsy_note_hint),
            enabled = enabled,
            onChange = { onChange(entry.withNote(it)) },
        )

        DisclosureType.SystemInteractions -> NoteField(
            label = stringResource(R.string.disclosures_system_note_label),
            value = entry.note,
            placeholder = stringResource(R.string.disclosures_system_note_hint),
            enabled = enabled,
            onChange = { onChange(entry.withNote(it)) },
        )

        DisclosureType.Archived -> NoteField(
            label = stringResource(R.string.disclosures_explanation_optional),
            value = entry.note,
            placeholder = stringResource(R.string.disclosures_archived_note_hint),
            enabled = enabled,
            onChange = { onChange(entry.withNote(it)) },
        )

        DisclosureType.Telemetry -> {
            DisclosureFieldLabel(stringResource(R.string.disclosures_telemetry_consent))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                DisclosureRules.telemetryConsents.forEachIndexed { index, consent ->
                    SegmentedButton(
                        selected = entry.consent == consent,
                        enabled = enabled,
                        onClick = { onChange(entry.withConsent(consent)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DisclosureRules.telemetryConsents.size),
                        colors = ryntraSegmentedButtonColors(),
                        label = {
                            Text(
                                text = consent.label(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }
            DisclosureFieldLabel(stringResource(R.string.disclosures_telemetry_data_label))
            Text(
                text = stringResource(R.string.disclosures_telemetry_data_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            RowListEditor(
                rows = entry.editableRows(),
                placeholder = stringResource(R.string.disclosures_telemetry_data_hint),
                icon = Lucide.Database,
                addLabel = stringResource(R.string.disclosures_telemetry_add_data),
                removeDescription = stringResource(R.string.disclosures_telemetry_remove_data),
                enabled = enabled,
                onChange = { onChange(entry.withDataCollected(it)) },
            )
        }

        DisclosureType.PaidFeatures -> {
            DisclosureFieldLabel(stringResource(R.string.disclosures_paid_features_label))
            RowListEditor(
                rows = entry.editableRows(),
                placeholder = stringResource(R.string.disclosures_paid_feature_hint),
                icon = Lucide.CircleDollarSign,
                addLabel = stringResource(R.string.disclosures_paid_add_feature),
                removeDescription = stringResource(R.string.disclosures_paid_remove_feature),
                enabled = enabled,
                onChange = { onChange(entry.withFeatures(it)) },
            )
        }

        DisclosureType.DerivativeWork -> DerivativeSourcesEditor(
            sources = entry.editableSources(),
            enabled = enabled,
            onChange = { onChange(entry.withSources(it)) },
        )
    }
}

/**
 * A growable list of single-line values. The last row is never removable, which keeps the editor
 * from collapsing into a state where there is nothing to type into.
 */
@Composable
private fun RowListEditor(
    rows: List<String>,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    addLabel: String,
    removeDescription: String,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RyntraTextField(
                    value = row,
                    onValueChange = { value -> onChange(rows.toMutableList().also { it[index] = value }) },
                    placeholder = placeholder,
                    leadingIcon = icon,
                    leadingIconDescription = null,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                if (rows.size > 1) {
                    IconButton(
                        enabled = enabled,
                        onClick = { onChange(rows.filterIndexed { position, _ -> position != index }) },
                    ) {
                        Icon(
                            imageVector = Lucide.Trash2,
                            contentDescription = removeDescription,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        RyntraSecondaryButton(
            text = addLabel,
            icon = Lucide.Plus,
            enabled = enabled,
            onClick = { onChange(rows + "") },
        )
    }
}

@Composable
private fun DerivativeSourcesEditor(
    sources: List<DerivativeSource>,
    enabled: Boolean,
    onChange: (List<DerivativeSource>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        sources.forEachIndexed { index, source ->
            fun replace(updated: DerivativeSource) =
                onChange(sources.toMutableList().also { it[index] = updated })

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.disclosures_derivative_source_index, index + 1),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (sources.size > 1) {
                            IconButton(
                                enabled = enabled,
                                onClick = { onChange(sources.filterIndexed { position, _ -> position != index }) },
                            ) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = stringResource(R.string.disclosures_derivative_remove_source),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    DisclosureField(
                        label = stringResource(R.string.disclosures_derivative_name_label),
                        value = source.label,
                        placeholder = stringResource(R.string.disclosures_derivative_name_hint),
                        icon = Lucide.Hash,
                        enabled = enabled,
                        onChange = { replace(source.withLabel(it)) },
                    )
                    DisclosureField(
                        label = stringResource(R.string.disclosures_derivative_link_label),
                        value = source.link,
                        placeholder = "https://example.com",
                        icon = Lucide.Link,
                        enabled = enabled,
                        onChange = { replace(source.withLink(it)) },
                    )
                    NoteField(
                        label = stringResource(R.string.disclosures_derivative_note_label),
                        value = source.note,
                        placeholder = stringResource(R.string.disclosures_derivative_note_hint),
                        enabled = enabled,
                        onChange = { replace(source.withNote(it)) },
                    )
                }
            }
        }
        RyntraSecondaryButton(
            text = stringResource(R.string.disclosures_derivative_add_source),
            icon = Lucide.Plus,
            enabled = enabled,
            onClick = { onChange(sources + DerivativeSource()) },
        )
    }
}

@Composable
private fun DisclosureField(
    label: String,
    value: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    DisclosureFieldLabel(label)
    RyntraTextField(
        value = value,
        onValueChange = onChange,
        placeholder = placeholder,
        label = label,
        leadingIcon = icon,
        leadingIconDescription = null,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    )
}

@Composable
private fun NoteField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    DisclosureFieldLabel(label)
    RyntraTextField(
        value = value,
        onValueChange = onChange,
        placeholder = placeholder,
        label = label,
        leadingIcon = Lucide.Info,
        leadingIconDescription = null,
        enabled = enabled,
        singleLine = false,
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DisclosureFieldLabel(label: String) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
