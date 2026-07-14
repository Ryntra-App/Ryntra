package com.rinthy.mobile.ui.dashboard.project.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectVersion
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
                    if (version == null) "Create version" else "Edit ${version.versionNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (version == null) "Prepare a complete Modrinth release" else "Update release metadata",
                    color = RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SmallActionButton(Lucide.X, "Close version editor", onDismiss)
        }
        HorizontalDivider(color = RinthyDesign.colors.separator)
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
            .background(RinthyDesign.colors.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        RinthySecondaryButton("Cancel", Lucide.X, onDismiss, modifier = Modifier.weight(1f))
        RinthyPrimaryButton(
            text = if (isCreating) "Create" else "Save",
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
    RinthySecondaryButton(
        text = if (isReading) "Reading files" else "Add version files",
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
                        "Primary · ${formatFileSize(file.bytes.size.toLong())}"
                    } else {
                        formatFileSize(file.bytes.size.toLong())
                    },
                    color = if (primaryIndex == index) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            SmallActionButton(Lucide.Trash2, "Remove file", { onRemove(index) }, tintDestructive = true)
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
                if (isSelected) RinthyDesign.colors.accent else RinthyDesign.colors.separator,
                CircleShape,
            ),
    ) {
        if (isSelected) Box(Modifier.size(12.dp).background(RinthyDesign.colors.accent, CircleShape))
    }
}

@Composable
internal fun FeaturedToggle(featured: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(8.dp))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(12.dp),
    ) {
        RinthyIcon(Lucide.Star, null, if (featured) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary, Modifier.size(19.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Featured version", fontWeight = FontWeight.SemiBold)
            Text("Highlight this release on the project page", color = RinthyDesign.colors.labelSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            contentAlignment = if (featured) Alignment.CenterEnd else Alignment.CenterStart,
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .background(if (featured) RinthyDesign.colors.accent else RinthyDesign.colors.surfaceRaised, CircleShape)
                .padding(3.dp),
        ) {
            Box(Modifier.size(20.dp).background(if (featured) Color.Black else RinthyDesign.colors.labelSecondary, CircleShape))
        }
    }
}

@Composable
internal fun ReleaseChecklist(warnings: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RinthyIcon(
                if (warnings.isEmpty()) Lucide.Check else Lucide.TriangleAlert,
                null,
                if (warnings.isEmpty()) RinthyDesign.colors.positive else RinthyDesign.colors.warning,
                Modifier.size(18.dp),
            )
            Text(
                if (warnings.isEmpty()) "Ready to publish" else "Before publishing",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        warnings.forEach { warning ->
            Text(
                "• $warning",
                color = RinthyDesign.colors.labelSecondary,
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
            .size(46.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(RinthyDesign.colors.surface)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        RinthyIcon(
            icon,
            description,
            if (tintDestructive) RinthyDesign.colors.destructive else RinthyDesign.colors.accent,
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
