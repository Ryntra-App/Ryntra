package com.ryntra.mobile.ui.dashboard.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.displayTypeLabel
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.components.formatProjectDate
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Project

internal data class ProjectRowModel(
    val project: Project,
    val subtitle: String,
    val downloads: String,
    val followers: String,
    val updated: String?,
)

@Composable
internal fun Project.toProjectRowModel(): ProjectRowModel {
    val typeLabel = displayTypeLabel()
    return ProjectRowModel(
        project = this,
        subtitle = slug?.let { "$it  ·  $typeLabel" } ?: typeLabel,
        downloads = formatExactCount(downloads),
        followers = formatExactCount(followers),
        updated = formatProjectDate(updated),
    )
}

@Composable
internal fun ProjectRow(
    project: Project,
    showDescription: Boolean = true,
    showStatus: Boolean = true,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val model = project.toProjectRowModel()
    val selectionColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Project selection",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionColor, RoundedCornerShape(10.dp))
            .then(
                when {
                    onClick != null && onLongClick != null -> Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                },
            )
            .padding(vertical = 13.dp),
    ) {
        ProjectArtwork(project)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = project.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showStatus && project.status != "approved") {
                    StatusLabel(project.status, Modifier.padding(start = 8.dp))
                }
            }
            Text(
                text = model.subtitle,
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (showDescription && project.description.isNotBlank()) {
                Text(
                    text = project.description,
                    color = RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            ProjectMetadata(model, Modifier.padding(top = 7.dp))
        }
        if (onClick != null) {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = RyntraDesign.colors.labelSecondary,
                modifier = Modifier.padding(start = 8.dp).size(17.dp),
            )
        }
    }
}

@Composable
private fun ProjectMetadata(model: ProjectRowModel, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        ProjectMetric(Lucide.Download, model.downloads, RyntraDesign.colors.accent)
        ProjectMetric(Lucide.Heart, model.followers, RyntraDesign.colors.accent)
        model.updated?.let { date ->
            ProjectMetric(
                Lucide.RefreshCw,
                stringResource(R.string.project_updated_label) + " " + date,
                RyntraDesign.colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun ProjectMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = value,
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
internal fun ProjectArtwork(project: Project, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(project.title.take(1).uppercase(), fontWeight = FontWeight.Bold)
        project.iconUrl?.let { iconUrl ->
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun StatusLabel(
    status: String,
    modifier: Modifier = Modifier,
) {
    val color = when (status.lowercase()) {
        "rejected", "withheld" -> MaterialTheme.colorScheme.error
        "processing", "scheduled", "draft" -> RyntraDesign.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (status.lowercase()) {
        "approved" -> stringResource(R.string.project_status_approved)
        "archived" -> stringResource(R.string.project_status_archived)
        "rejected" -> stringResource(R.string.project_status_rejected)
        "draft" -> stringResource(R.string.project_status_draft)
        "unlisted" -> stringResource(R.string.project_status_unlisted)
        "processing" -> stringResource(R.string.project_status_processing)
        "withheld" -> stringResource(R.string.project_status_withheld)
        "scheduled" -> stringResource(R.string.project_status_scheduled)
        "private" -> stringResource(R.string.project_status_private)
        "unknown" -> stringResource(R.string.project_status_unknown)
        else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.wrapContentWidth(unbounded = false),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
