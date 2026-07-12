package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project
import com.composables.icons.lucide.CircleCheckBig
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.TriangleAlert

@Composable
fun OverviewScreen(
    dashboard: Dashboard,
    onProjectClick: (Project) -> Unit = {},
) {
    val projects = dashboard.projects
    val needsAttention = remember(projects) { projects.filterNot { it.isHealthy() }.take(3) }
    val recentProjects = remember(projects) {
        projects.sortedByDescending { it.updated.orEmpty() }.take(4)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
    ) {
        item { CreatorHeader(dashboard.account.username) }
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
                    AttentionRow(project = project, onClick = { onProjectClick(project) })
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
                    ProjectRow(
                        project = project,
                        showDescription = false,
                        showStatus = false,
                        onClick = { onProjectClick(project) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CreatorHeader(username: String) {
    Text(
        text = "Welcome back, $username",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 18.dp),
    )
}

@Composable
private fun MetricStrip(downloads: Long, followers: Long, projectCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RinthyDesign.contentShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, RinthyDesign.colors.separator),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(vertical = 13.dp)) {
            OverviewMetric(Lucide.Download, formatCompact(downloads), "Downloads", Modifier.weight(1f))
            OverviewMetric(Lucide.Heart, formatCompact(followers), "Followers", Modifier.weight(1f))
            OverviewMetric(Lucide.Package, projectCount.toString(), "Projects", Modifier.weight(1f))
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Icon(icon, contentDescription = null, tint = RinthyDesign.colors.positive, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.padding(start = 7.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
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
private fun AttentionRow(project: Project, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Lucide.TriangleAlert,
            contentDescription = "Attention required",
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AllClearRow() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RinthyDesign.contentShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, RinthyDesign.colors.separator),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(Lucide.CircleCheckBig, contentDescription = null, tint = RinthyDesign.colors.positive)
            Column {
                Text("All clear", fontWeight = FontWeight.SemiBold)
                Text(
                    "No projects currently require action.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
