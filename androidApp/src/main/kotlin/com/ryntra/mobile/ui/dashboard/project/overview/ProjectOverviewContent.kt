package com.ryntra.mobile.ui.dashboard.project.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.dashboard.projects.StatusLabel
import com.ryntra.mobile.ui.components.displayTypeLabel
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectDependency

@Composable
internal fun ProjectDependencyRow(dependency: ProjectDependency) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        AsyncImage(
            model = dependency.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                dependency.title ?: dependency.projectId ?: dependency.fileName
                    ?: stringResource(R.string.project_external_dependency),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                dependency.dependencyType.replaceFirstChar(Char::uppercase),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun LoadingMembers() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 28.dp)) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            stringResource(R.string.project_members_loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
internal fun ProjectIdentity(project: Project) {
    Column(modifier = Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(132.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                project.title.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
            project.bannerUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(project.title.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            project.iconUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                project.displayTypeLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (project.status != "approved") {
                StatusLabel(
                    project.status,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        }
    }
}

@Composable
internal fun ProjectMetrics(project: Project) {
    Column {
        HorizontalDivider(color = RyntraDesign.colors.separator)
        Row(modifier = Modifier.padding(vertical = 14.dp)) {
            DetailMetric(
                Lucide.Download,
                stringResource(R.string.project_downloads),
                formatExactCount(project.downloads),
                Modifier.weight(1f),
            )
            DetailMetric(
                Lucide.Heart,
                stringResource(R.string.project_followers),
                formatExactCount(project.followers),
                Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = RyntraDesign.colors.separator)
    }
}

@Composable
private fun DetailMetric(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = RyntraDesign.colors.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun DetailSection(title: String, value: String) {
    DetailHeading(title)
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun DetailHeading(title: String) {
    RyntraSectionLabel(
        text = title,
        modifier = Modifier.padding(top = 30.dp, bottom = 11.dp),
    )
}

@Composable
internal fun EnvironmentValue(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = RyntraDesign.colors.accent, modifier = Modifier.size(19.dp))
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value.replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun CategoryChip(category: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(7.dp)) {
        Text(
            text = category.replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun DetailValue(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Icon(icon, contentDescription = null, tint = RyntraDesign.colors.accent, modifier = Modifier.size(18.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
    }
}

@Composable
internal fun ResourceRow(label: String, onClick: () -> Unit) {
    val iconTint = if (RyntraDesign.isPlatformNative) {
        MaterialTheme.colorScheme.primary
    } else {
        RyntraDesign.colors.accent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        Icon(Lucide.Globe, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Icon(
            Lucide.ExternalLink,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
    }
}

internal data class ProjectResource(val label: String, val url: String)
