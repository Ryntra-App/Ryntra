package com.ryntra.mobile.ui.dashboard.project.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraProgressIndicator
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ProjectVersion

@Composable
internal fun LoadingVersions() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 28.dp)) {
        RyntraProgressIndicator(RyntraDesign.colors.accent, Modifier.size(18.dp))
        Text(
            stringResource(R.string.project_versions_loading),
            color = RyntraDesign.colors.labelSecondary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
internal fun VersionsHeader(canCreate: Boolean, onCreate: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RyntraSectionLabel(stringResource(R.string.project_versions_title))
            Text(
                stringResource(R.string.project_versions_hint),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (canCreate) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(RyntraDesign.colors.surface)
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                RyntraIcon(Lucide.Plus, null, RyntraDesign.colors.accent, Modifier.size(18.dp))
                Text(
                    stringResource(R.string.project_versions_new),
                    color = RyntraDesign.colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
internal fun VersionCard(
    version: ProjectVersion,
    canEdit: Boolean,
    canDelete: Boolean,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VersionTypeDot(version.versionType)
                    Text(
                        text = version.versionNumber,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
                Text(
                    text = version.name,
                    color = RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            if (isBusy) {
                RyntraProgressIndicator(RyntraDesign.colors.accent, Modifier.size(20.dp))
            } else {
                if (canEdit) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                        Icon(Lucide.Pencil, contentDescription = "Edit version", tint = RyntraDesign.colors.accent, modifier = Modifier.size(17.dp))
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(Lucide.Trash2, contentDescription = "Delete version", tint = RyntraDesign.colors.destructive, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            RyntraIcon(Lucide.Download, null, RyntraDesign.colors.accent, Modifier.size(15.dp))
            Text(
                text = formatExactCount(version.downloads),
                color = RyntraDesign.colors.accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 5.dp),
            )
            version.datePublished?.take(10)?.let { date ->
                Text(date, color = RyntraDesign.colors.labelSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 12.dp))
            }
        }
        if (version.gameVersions.isNotEmpty() || version.loaders.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()),
            ) {
                version.gameVersions.forEach { VersionTag(it) }
                version.loaders.forEach { VersionTag(it.uppercase()) }
            }
        }
        if (version.changelog.isNotBlank()) {
            Text(
                version.changelog,
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        HorizontalDivider(color = RyntraDesign.colors.separator, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun VersionTypeDot(type: String) {
    val color = when (type) {
        "release" -> RyntraDesign.colors.positive
        "beta" -> RyntraDesign.colors.warning
        "alpha" -> RyntraDesign.colors.destructive
        else -> RyntraDesign.colors.labelSecondary
    }
    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
}

@Composable
private fun VersionTag(value: String) {
    Text(
        text = value,
        color = RyntraDesign.colors.labelSecondary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(RyntraDesign.colors.surface, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
    )
}
