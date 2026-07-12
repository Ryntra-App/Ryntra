package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project
import com.rinthy.mobile.ui.theme.RinthyDesign

private data class AnalyticsSnapshot(
    val totalDownloads: Long,
    val totalFollowers: Long,
    val topProjects: List<Project>,
    val largestProject: Long,
    val projectTypes: List<Pair<String, Int>>,
)

@Composable
fun AnalyticsScreen(dashboard: Dashboard) {
    val projects = dashboard.projects
    val snapshot = remember(projects) {
        val topProjects = projects.sortedByDescending(Project::downloads).take(5)
        AnalyticsSnapshot(
            totalDownloads = projects.sumOf(Project::downloads),
            totalFollowers = projects.sumOf(Project::followers),
            topProjects = topProjects,
            largestProject = topProjects.firstOrNull()?.downloads?.coerceAtLeast(1) ?: 1,
            projectTypes = projects
                .groupingBy { it.projectType.replaceFirstChar(Char::uppercase) }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key to it.value },
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
    ) {
        item {
            Text(
                text = "Workspace performance",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnalyticsMetric(Lucide.Download, formatCompact(snapshot.totalDownloads), "Downloads", Modifier.weight(1f))
                AnalyticsMetric(Lucide.Heart, formatCompact(snapshot.totalFollowers), "Followers", Modifier.weight(1f))
                AnalyticsMetric(Lucide.Package, projects.size.toString(), "Projects", Modifier.weight(1f))
            }
        }
        item {
            Text(
                text = "Top projects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 30.dp, bottom = 12.dp),
            )
        }
        if (snapshot.topProjects.isEmpty()) {
            item { EmptyState("No analytics yet", "Project performance will appear after your projects load.") }
        } else {
            snapshot.topProjects.forEach { project ->
                item(key = project.id) {
                    ProjectPerformanceRow(
                        project = project,
                        progress = project.downloads.toFloat() / snapshot.largestProject.toFloat(),
                    )
                }
            }
        }
        item {
            Text(
                text = "Project mix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 30.dp, bottom = 8.dp),
            )
            snapshot.projectTypes.forEach { (type, count) ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(type, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(count.toString(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetric(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RinthyDesign.colors.positive,
            modifier = Modifier.size(18.dp),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProjectPerformanceRow(project: Project, progress: Float) {
    Column(modifier = Modifier.padding(vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatCompact(project.downloads),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(RinthyDesign.colors.positive, RoundedCornerShape(3.dp)),
            )
        }
    }
}
