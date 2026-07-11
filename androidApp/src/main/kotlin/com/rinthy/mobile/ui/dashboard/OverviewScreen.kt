package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project

@Composable
fun OverviewScreen(dashboard: Dashboard) {
    val projects = dashboard.projects
    val needsAttention = projects.filterNot { it.isHealthy() }.take(3)
    val recentProjects = projects.sortedByDescending { it.updated.orEmpty() }.take(4)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
    ) {
        item { CreatorHeader(dashboard) }
        item {
            MetricStrip(
                downloads = projects.sumOf(Project::downloads),
                followers = projects.sumOf(Project::followers),
                projectCount = projects.size,
            )
        }
        item {
            SectionHeader(
                title = "Needs attention",
                supportingText = if (needsAttention.isEmpty()) null else "${needsAttention.size} open actions",
            )
        }
        if (needsAttention.isEmpty()) {
            item { AllClearRow() }
        } else {
            needsAttention.forEach { project ->
                item(key = "attention-${project.id}") {
                    AttentionRow(project)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item {
            SectionHeader(
                title = "Recent projects",
                supportingText = "Updated across your workspace",
            )
        }
        if (recentProjects.isEmpty()) {
            item {
                EmptyState(
                    title = "No projects yet",
                    message = "Projects you own or manage will appear here.",
                )
            }
        } else {
            recentProjects.forEach { project ->
                item(key = "recent-${project.id}") {
                    ProjectRow(project = project, showDescription = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CreatorHeader(dashboard: Dashboard) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Welcome back, ${dashboard.account.username}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(dashboard.account.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "${dashboard.account.username}'s profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
private fun MetricStrip(downloads: Long, followers: Long, projectCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(vertical = 16.dp)) {
            OverviewMetric(Icons.Rounded.Download, formatCompact(downloads), "Downloads", Modifier.weight(1f))
            OverviewMetric(Icons.Rounded.Favorite, formatCompact(followers), "Followers", Modifier.weight(1f))
            OverviewMetric(Icons.Rounded.Inventory2, projectCount.toString(), "Projects", Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionHeader(title: String, supportingText: String?) {
    Column(modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (supportingText != null) {
            Text(
                supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AttentionRow(project: Project) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        ProjectArtwork(project)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(project.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = statusMessage(project.status),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = "Attention required",
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AllClearRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text("All clear", fontWeight = FontWeight.Bold)
            Text(
                "No projects currently require action.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun Project.isHealthy(): Boolean = status == "approved" || status == "archived"

private fun statusMessage(status: String): String = when (status) {
    "processing" -> "Modrinth is processing this project"
    "rejected" -> "Review the moderation response"
    "withheld" -> "Project is withheld from publishing"
    "scheduled" -> "Publication is scheduled"
    "private" -> "Project is currently private"
    "draft" -> "Draft is waiting to be finished"
    else -> "Status: ${status.replaceFirstChar(Char::uppercase)}"
}
