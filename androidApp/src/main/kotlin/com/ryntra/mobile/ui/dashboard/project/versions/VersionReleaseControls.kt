package com.ryntra.mobile.ui.dashboard.project.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.FileArchive
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.TriangleAlert
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectVersion
import java.util.Locale
import kotlin.math.roundToLong

@Composable
internal fun VersionEditorTopBar(version: ProjectVersion?, onDismiss: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (version == null) stringResource(R.string.version_editor_create_title)
                    else stringResource(R.string.version_editor_edit_title, version.versionNumber),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (version == null) stringResource(R.string.version_editor_create_subtitle)
                    else stringResource(R.string.version_editor_edit_subtitle),
                    color = RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SmallActionButton(Lucide.X, stringResource(R.string.version_editor_close), onDismiss)
        }
        HorizontalDivider(color = RyntraDesign.colors.separator)
    }
}

@Composable
internal fun VersionEditorActions(
    isCreating: Boolean,
    canSave: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        RyntraSecondaryButton(stringResource(R.string.version_editor_cancel), Lucide.X, onDismiss, modifier = Modifier.weight(1f))
        RyntraPrimaryButton(
            text = stringResource(if (isCreating) R.string.version_editor_create else R.string.version_editor_save),
            icon = if (isCreating) Lucide.Plus else Lucide.Save,
            enabled = canSave,
            isLoading = isSaving,
            modifier = Modifier.weight(1f),
            onClick = onSave,
        )
    }
}

@Composable
internal fun VersionFilesEditor(
    files: List<ProjectFileUpload>,
    primaryIndex: Int,
    isReading: Boolean,
    onChoose: () -> Unit,
    onSelectPrimary: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    RyntraSecondaryButton(
        text = stringResource(if (isReading) R.string.version_editor_files_reading else R.string.version_editor_files_add),
        icon = Lucide.FileArchive,
        enabled = !isReading,
        onClick = onChoose,
    )
    files.forEachIndexed { index, file ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectPrimary(index) }
                .padding(vertical = 9.dp),
        ) {
            PrimaryFileIndicator(isSelected = primaryIndex == index)
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (primaryIndex == index) {
                        stringResource(R.string.version_editor_file_primary, formatFileSize(file.bytes.size.toLong()))
                    } else {
                        formatFileSize(file.bytes.size.toLong())
                    },
                    color = if (primaryIndex == index) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            SmallActionButton(Lucide.Trash2, stringResource(R.string.version_editor_file_remove), { onRemove(index) }, tintDestructive = true)
        }
    }
}

@Composable
private fun PrimaryFileIndicator(isSelected: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .border(
                1.dp,
                if (isSelected) RyntraDesign.colors.accent else RyntraDesign.colors.separator,
                CircleShape,
            ),
    ) {
        if (isSelected) Box(Modifier.size(12.dp).background(RyntraDesign.colors.accent, CircleShape))
    }
}

@Composable
internal fun FeaturedToggle(featured: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(0.75.dp, RyntraDesign.colors.separator, RoundedCornerShape(8.dp))
            .toggleable(value = featured, role = Role.Switch, onValueChange = { onToggle() })
            .padding(12.dp),
    ) {
        RyntraIcon(Lucide.Star, null, if (featured) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary, Modifier.size(19.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(stringResource(R.string.version_editor_featured), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.version_editor_featured_hint), color = RyntraDesign.colors.labelSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = featured, onCheckedChange = null)
    }
}

@Composable
internal fun ReleaseChecklist(warnings: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(0.75.dp, RyntraDesign.colors.separator, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RyntraIcon(
                if (warnings.isEmpty()) Lucide.Check else Lucide.TriangleAlert,
                null,
                if (warnings.isEmpty()) RyntraDesign.colors.positive else RyntraDesign.colors.warning,
                Modifier.size(18.dp),
            )
            Text(
                stringResource(if (warnings.isEmpty()) R.string.version_editor_ready else R.string.version_editor_before_publish),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        warnings.forEach { warning ->
            Text(
                "• $warning",
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }
}

@Composable
internal fun SmallActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintDestructive: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(RyntraDesign.colors.surface)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        RyntraIcon(
            icon,
            description,
            if (tintDestructive) RyntraDesign.colors.destructive else RyntraDesign.colors.accent,
            Modifier.size(19.dp),
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return "${kib.roundToLong()} KiB"
    return String.format(Locale.US, "%.1f MiB", kib / 1024.0)
}
