package com.ryntra.mobile.ui.dashboard.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraSearchField
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.shared.model.sortedForDisplay

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    sortMode: ProjectSortMode = ProjectSortMode.Popularity,
    favoriteProjectIds: Set<String> = emptySet(),
    showFavoriteProjects: Boolean = true,
    onSortModeChange: (ProjectSortMode) -> Unit = {},
    onToggleFavoriteProject: (String) -> Unit = {},
    onProjectClick: (Project) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Optimistic local set so the icon swaps immediately on tap.
    var localFavoriteIds by remember { mutableStateOf(favoriteProjectIds) }
    LaunchedEffect(favoriteProjectIds) {
        localFavoriteIds = favoriteProjectIds
    }
    val favoriteIds = if (showFavoriteProjects) localFavoriteIds else emptySet()
    val totalDownloads = remember(projects) { projects.sumOf(Project::downloads) }
    val totalFollowers = remember(projects) { projects.sumOf(Project::followers) }
    val filteredProjects = remember(projects, query, sortMode, favoriteIds) {
        projects
            .filter { project ->
                query.isBlank() ||
                    project.title.contains(query, ignoreCase = true) ||
                    project.description.contains(query, ignoreCase = true) ||
                    project.slug?.contains(query, ignoreCase = true) == true
            }
            .sortedForDisplay(sortMode, favoriteIds)
    }
    val visibleProjects = filteredProjects.map { it.toProjectRowModel() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = RyntraDesign.bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "projects-summary", contentType = "summary") {
            ProjectSummaryBand(
                projectCount = projects.size,
                downloads = totalDownloads,
                followers = totalFollowers,
            )
            RyntraSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.projects_search),
                leadingIcon = Lucide.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            ProjectSortControl(
                selected = sortMode,
                onSelect = onSortModeChange,
            )
        }
        if (visibleProjects.isEmpty()) {
            item(key = "projects-empty", contentType = "empty") {
                RyntraEmptyState(
                    title = stringResource(if (projects.isEmpty()) R.string.projects_empty else R.string.projects_no_matches),
                    message = if (projects.isEmpty()) {
                        stringResource(R.string.projects_empty_hint)
                    } else {
                        stringResource(R.string.projects_no_matches_hint)
                    },
                )
            }
        } else {
            items(visibleProjects, key = { it.project.id }, contentType = { "project" }) { model ->
                Box(modifier = Modifier.animateItem()) {
                    val projectId = model.project.id
                    ProjectBannerCard(
                        model = model,
                        isFavorite = projectId in favoriteIds,
                        onFavoriteClick = if (showFavoriteProjects) {
                            {
                                localFavoriteIds = if (projectId in localFavoriteIds) {
                                    localFavoriteIds - projectId
                                } else {
                                    localFavoriteIds + projectId
                                }
                                onToggleFavoriteProject(projectId)
                            }
                        } else {
                            null
                        },
                        onClick = { onProjectClick(model.project) },
                    )
                }
            }
        }
    }
}
