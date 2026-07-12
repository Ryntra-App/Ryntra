package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectSortMode
import com.rinthy.shared.model.sortedForDisplay
import com.rinthy.mobile.ui.components.RinthySearchField
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Star

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(ProjectSortMode.Popularity) }
    var favoriteIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val favoriteSet = remember(favoriteIds) { favoriteIds.toSet() }
    val visibleProjects = remember(projects, query, sortMode, favoriteIds) {
        projects.filter { project ->
            query.isBlank() || project.title.contains(query, ignoreCase = true) ||
                project.description.contains(query, ignoreCase = true)
        }.sortedForDisplay(sortMode, favoriteSet)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "${projects.size} projects · ${formatCompact(projects.sumOf { it.downloads })} downloads",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            RinthySearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search projects",
                leadingIcon = Lucide.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
            )
            SortBar(
                selected = sortMode,
                onSelect = { sortMode = it },
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (visibleProjects.isEmpty()) {
            item {
                EmptyState(
                    title = if (projects.isEmpty()) "No projects yet" else "No matching projects",
                    message = if (projects.isEmpty()) {
                        "Projects you own or manage will appear here."
                    } else {
                        "Try a different search term."
                    },
                )
            }
        } else {
            items(visibleProjects, key = Project::id) { project ->
                ProjectRow(
                    project = project,
                    isFavorite = project.id in favoriteSet,
                    onFavoriteClick = {
                        favoriteIds = if (project.id in favoriteSet) {
                            favoriteIds - project.id
                        } else {
                            favoriteIds + project.id
                        }
                    },
                    onClick = { onProjectClick(project) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
internal fun ProjectRow(
    project: Project,
    showDescription: Boolean = true,
    showStatus: Boolean = true,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(vertical = 12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier,
    ) {
        ProjectArtwork(project)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showStatus && project.status != "approved") {
                    StatusLabel(project.status)
                }
                if (onFavoriteClick != null) {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Lucide.Star,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) RinthyDesign.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = project.projectType.replaceFirstChar(Char::uppercase),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            if (showDescription && project.description.isNotBlank()) {
                Text(
                    text = project.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Metric(Lucide.Download, formatCompact(project.downloads))
                Metric(Lucide.Heart, formatCompact(project.followers))
            }
        }
    }
}

@Composable
private fun SortBar(
    selected: ProjectSortMode,
    onSelect: (ProjectSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        ProjectSortMode.entries.forEach { mode ->
            SortChip(
                label = mode.label,
                selected = mode == selected,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) RinthyDesign.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (selected) color.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun ProjectArtwork(project: Project) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(project.title.take(1).uppercase(), fontWeight = FontWeight.Black)
        if (project.iconUrl != null) {
            AsyncImage(
                model = project.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun StatusLabel(status: String) {
    val color = when (status) {
        "rejected", "withheld" -> MaterialTheme.colorScheme.error
        "processing", "scheduled" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(
            text = status.replaceFirstChar(Char::uppercase),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

internal fun formatCompact(value: Long): String = when {
    value >= 1_000_000 -> "${value / 100_000 / 10.0}M"
    value >= 1_000 -> "${value / 100 / 10.0}K"
    else -> value.toString()
}

@Composable
internal fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
