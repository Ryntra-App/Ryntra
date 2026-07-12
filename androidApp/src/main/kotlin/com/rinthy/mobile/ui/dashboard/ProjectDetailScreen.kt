package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Server
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectVersion

@Composable
fun ProjectDetailScreen(
    project: Project,
    versions: List<ProjectVersion> = emptyList(),
    members: List<ProjectMember> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    memberErrorMessage: String? = null,
) {
    val uriHandler = LocalUriHandler.current
    var selectedTab by rememberSaveable(project.id) { mutableStateOf(ProjectDetailTab.Overview) }
    val resources = listOfNotNull(
        project.sourceUrl?.let { ProjectResource("Source", it) },
        project.issuesUrl?.let { ProjectResource("Issues", it) },
        project.wikiUrl?.let { ProjectResource("Wiki", it) },
        project.discordUrl?.let { ProjectResource("Discord", it) },
    )
    val cleanedBody = project.body.cleanMarkdown()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
    ) {
        item { ProjectIdentity(project) }
        item {
            ProjectDetailTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }

        when (selectedTab) {
            ProjectDetailTab.Overview -> {
                item { ProjectMetrics(project) }
                item { DetailSection("Summary", project.description.ifBlank { "No summary provided." }) }
                if (cleanedBody.isNotBlank() && cleanedBody != project.description.cleanMarkdown()) {
                    item { MarkdownSection("Description", cleanedBody) }
                }
                item {
                    DetailHeading("Environment")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EnvironmentValue("Client", project.clientSide, Lucide.Monitor, Modifier.weight(1f))
                        EnvironmentValue("Server", project.serverSide, Lucide.Server, Modifier.weight(1f))
                    }
                }
                if (project.categories.isNotEmpty()) {
                    item {
                        DetailHeading("Categories")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            project.categories.forEach { category -> CategoryChip(category) }
                        }
                    }
                }
                if (project.gallery.isNotEmpty()) {
                    item {
                        DetailHeading("Gallery")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(project.gallery, key = { it.url }) { image ->
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = image.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 172.dp, height = 108.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                }
                if (project.license != null || project.published != null) {
                    item {
                        DetailHeading("Details")
                        project.license?.let {
                            DetailValue(Lucide.Scale, "License", it.name ?: it.id)
                        }
                        project.published?.let {
                            DetailValue(Lucide.CalendarDays, "Published", it.take(10))
                        }
                    }
                }
                if (resources.isNotEmpty()) {
                    item {
                        DetailHeading("Resources")
                        resources.forEach { resource ->
                            ResourceRow(resource.label) { uriHandler.openUri(resource.url) }
                            HorizontalDivider(color = RinthyDesign.colors.separator)
                        }
                    }
                }
            }

            ProjectDetailTab.Versions -> {
                when {
                    isLoading -> item { LoadingVersions() }
                    errorMessage != null && versions.isEmpty() -> item {
                        EmptyState(title = "Versions unavailable", message = errorMessage)
                    }
                    versions.isEmpty() -> item {
                        EmptyState(
                            title = "No versions yet",
                            message = "Published releases for this project will appear here.",
                        )
                    }
                    else -> items(versions, key = ProjectVersion::id) { version ->
                        VersionCard(version)
                    }
                }
            }

            ProjectDetailTab.Edit -> item {
                PendingProjectTab(
                    title = "Edit tools are next",
                    message = "Metadata, status, links, icon, and gallery editing will move here from the old app.",
                )
            }

            ProjectDetailTab.Members -> {
                when {
                    isLoading -> item { LoadingMembers() }
                    memberErrorMessage != null && members.isEmpty() -> item {
                        EmptyState(title = "Members unavailable", message = memberErrorMessage)
                    }
                    members.isEmpty() -> item {
                        EmptyState(
                            title = "No members found",
                            message = "Team members for this project will appear here.",
                        )
                    }
                    else -> items(members, key = { it.user.id }) { member ->
                        ProjectMemberCard(member)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMembers() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 28.dp)) {
        androidx.compose.material3.CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            "Loading members",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun ProjectIdentity(project: Project) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
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
                project.projectType.replaceFirstChar(Char::uppercase),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (project.status != "approved") {
                StatusLabel(project.status)
            }
        }
    }
}

@Composable
private fun ProjectMetrics(project: Project) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RinthyDesign.colors.separator),
    ) {
        Row(modifier = Modifier.padding(vertical = 14.dp)) {
            DetailMetric(Lucide.Download, "Downloads", formatCompact(project.downloads), Modifier.weight(1f))
            DetailMetric(Lucide.Heart, "Followers", formatCompact(project.followers), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = RinthyDesign.colors.positive, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailSection(title: String, value: String) {
    DetailHeading(title)
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MarkdownSection(title: String, markdown: String) {
    DetailHeading(title)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        markdown.lines()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { it.toMarkdownBlock() }
            .filter { it.text.isNotBlank() }
            .forEach { block ->
                Text(
                    text = block.text,
                    color = if (block.isHeading) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = if (block.isHeading) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    fontWeight = if (block.isHeading) FontWeight.Bold else FontWeight.Normal,
                )
            }
    }
}

@Composable
private fun DetailHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
    )
}

@Composable
private fun EnvironmentValue(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = RinthyDesign.colors.positive, modifier = Modifier.size(19.dp))
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value.replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(7.dp)) {
        Text(
            text = category.replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun DetailValue(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Icon(icon, contentDescription = null, tint = RinthyDesign.colors.positive, modifier = Modifier.size(18.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
        Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp).weight(1f))
    }
}

@Composable
private fun ResourceRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        Icon(Lucide.Globe, contentDescription = null, tint = RinthyDesign.colors.positive, modifier = Modifier.size(19.dp))
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Icon(Lucide.ExternalLink, contentDescription = "Open $label", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
    }
}

private data class ProjectResource(val label: String, val url: String)

private data class MarkdownBlock(
    val text: String,
    val isHeading: Boolean,
)

private fun String.cleanMarkdown(): String =
    replace(Regex("\\[!\\[[^]]*]\\([^)]*\\)]\\([^)]*\\)"), "")
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^]]*)]\\(([^)]+)\\)"), "$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim()

private fun String.toMarkdownBlock(): MarkdownBlock {
    val headingLevel = takeWhile { it == '#' }.length
    if (headingLevel > 0) {
        return MarkdownBlock(drop(headingLevel).trim(), isHeading = true)
    }
    if (startsWith("](") || startsWith(")(") || startsWith("(")) {
        return MarkdownBlock("", isHeading = false)
    }
    val text = removePrefix("- ")
        .removePrefix("* ")
        .let { if (it === this) it else "• $it" }
    return MarkdownBlock(text, isHeading = false)
}
