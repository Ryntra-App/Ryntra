package com.ryntra.mobile.ui.dashboard.project.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ProjectDependency

@Composable
internal fun VersionEditorSection(
    title: String,
    detail: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        RyntraSectionLabel(title)
        if (detail != null) {
            Text(
                text = detail,
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 11.dp),
            )
        } else {
            Box(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
internal fun VersionEditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Lucide.Pencil,
) {
    Text(label, color = RyntraDesign.colors.labelSecondary, style = MaterialTheme.typography.labelMedium)
    RyntraTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        leadingIconDescription = null,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

@Composable
internal fun ReleaseChannelPicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("release", "beta", "alpha").forEach { channel ->
            val isSelected = selected == channel
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        if (isSelected) RyntraDesign.colors.surfaceRaised else RyntraDesign.colors.surface,
                        RoundedCornerShape(8.dp),
                    )
                    .border(0.75.dp, RyntraDesign.colors.separator, RoundedCornerShape(8.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(channel) },
            ) {
                Text(
                    stringResource(channelLabel(channel)),
                    color = if (isSelected) channelColor(channel) else RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun ValueChoices(
    values: List<String>,
    selected: List<String>,
    customValue: String,
    customPlaceholder: String,
    onCustomValueChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onAddCustom: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        (selected + values).distinct().forEach { value ->
            ChoiceChip(value, value in selected) { onToggle(value) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        RyntraTextField(
            value = customValue,
            onValueChange = onCustomValueChange,
            placeholder = customPlaceholder,
            leadingIcon = Lucide.Plus,
            leadingIconDescription = null,
            modifier = Modifier.weight(1f),
        )
        SmallActionButton(Lucide.Plus, stringResource(R.string.version_editor_add), onAddCustom, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ChoiceChip(value: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (selected) RyntraDesign.colors.accent.copy(alpha = 0.14f) else RyntraDesign.colors.surface,
                RoundedCornerShape(7.dp),
            )
            .border(
                0.75.dp,
                if (selected) RyntraDesign.colors.accent.copy(alpha = 0.48f) else RyntraDesign.colors.separator,
                RoundedCornerShape(7.dp),
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .heightIn(min = 48.dp)
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        if (selected) RyntraIcon(Lucide.Check, null, RyntraDesign.colors.accent, Modifier.size(14.dp))
        Text(
            value,
            color = if (selected) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = if (selected) Modifier.padding(start = 4.dp) else Modifier,
        )
    }
}

@Composable
internal fun DependencyEditor(
    dependencies: List<ProjectDependency>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onChangeType: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    dependencies.forEachIndexed { index, dependency ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        ) {
            RyntraIcon(Lucide.Package, null, RyntraDesign.colors.accent, Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                Text(
                    dependency.projectId ?: dependency.versionId ?: dependency.fileName.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(dependencyTypeLabel(dependency.dependencyType)),
                    color = RyntraDesign.colors.accent,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .clickable(role = Role.Button) { onChangeType(index) }
                        .padding(end = 12.dp),
                )
            }
            SmallActionButton(Lucide.Trash2, stringResource(R.string.version_editor_dependency_remove), { onRemove(index) }, tintDestructive = true)
        }
    }
    if (dependencies.isEmpty()) {
        Text(stringResource(R.string.version_editor_dependency_empty), color = RyntraDesign.colors.labelSecondary, style = MaterialTheme.typography.bodySmall)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        RyntraTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = stringResource(R.string.version_editor_dependency_placeholder),
            leadingIcon = Lucide.Package,
            leadingIconDescription = null,
            modifier = Modifier.weight(1f),
        )
        SmallActionButton(Lucide.Plus, stringResource(R.string.version_editor_dependency_add), onAdd, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun channelColor(channel: String) = when (channel) {
    "release" -> RyntraDesign.colors.positive
    "beta" -> RyntraDesign.colors.warning
    else -> RyntraDesign.colors.destructive
}

private fun channelLabel(channel: String): Int = when (channel) {
    "release" -> R.string.version_editor_channel_release
    "beta" -> R.string.version_editor_channel_beta
    else -> R.string.version_editor_channel_alpha
}

private fun dependencyTypeLabel(type: String): Int = when (type) {
    "required" -> R.string.version_editor_dependency_required
    "optional" -> R.string.version_editor_dependency_optional
    "incompatible" -> R.string.version_editor_dependency_incompatible
    else -> R.string.version_editor_dependency_embedded
}
