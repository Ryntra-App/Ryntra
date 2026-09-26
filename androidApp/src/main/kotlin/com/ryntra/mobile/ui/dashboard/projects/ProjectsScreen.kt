package com.ryntra.mobile.ui.dashboard.projects

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Plus
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import com.ryntra.mobile.R
import com.ryntra.mobile.ProjectActionState
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraSearchField
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.shared.model.sortedForDisplay

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    projectAction: ProjectActionState = ProjectActionState(),
    sortMode: ProjectSortMode = ProjectSortMode.Popularity,
    favoriteProjectIds: Set<String> = emptySet(),
    showFavoriteProjects: Boolean = true,
    showProjectBanners: Boolean = true,
    onSortModeChange: (ProjectSortMode) -> Unit = {},
    onToggleFavoriteProject: (String) -> Unit = {},
    onProjectClick: (Project) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onClearProjectActionStatus: () -> Unit = {},
    onCreateProject: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var actionProject by remember { mutableStateOf<Project?>(null) }
    var deletingProject by remember { mutableStateOf<Project?>(null) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(projectAction.successMessage) {
        if (projectAction.successMessage != null && deletingProject != null) {
            deletingProject = null
            onClearProjectActionStatus()
        }
    }
    val pinnedFavoriteIds = if (showFavoriteProjects) favoriteProjectIds else emptySet()
    val totalDownloads = remember(projects) { projects.sumOf(Project::downloads) }
    val totalFollowers = remember(projects) { projects.sumOf(Project::followers) }
    val filteredProjects = remember(projects, query, sortMode, pinnedFavoriteIds) {
        projects
            .filter { project ->
                query.isBlank() ||
                    project.title.contains(query, ignoreCase = true) ||
                    project.description.contains(query, ignoreCase = true) ||
                    project.slug?.contains(query, ignoreCase = true) == true
            }
            .sortedForDisplay(sortMode, pinnedFavoriteIds)
    }
    val listState = rememberLazyListState()
    // Collapses to its icon once the list is scrolled, the way Material's extended FAB
    // gets out of the way of the content it sits on.
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = RyntraDesign.bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "projects-summary", contentType = "summary") {
            ProjectSummaryBand(
                projectCount = projects.size,
                downloads = totalDownloads,
                followers = totalFollowers,
            )
        }
        // Search and sort act on the same list, so they sit together as one group — a
        // tighter gap inside it than the list's own spacing around it.
        item(key = "projects-controls", contentType = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RyntraSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.projects_search),
                    leadingIcon = Lucide.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
                ProjectSortControl(
                    selected = sortMode,
                    onSelect = onSortModeChange,
                )
            }
        }
        if (filteredProjects.isEmpty()) {
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
            items(filteredProjects, key = Project::id, contentType = { "project" }) { project ->
                val projectId = project.id
                // Search, sorting and pinning a favourite move cards rather than redraw
                // the list, so they travel to their new place instead of jumping.
                Box(modifier = Modifier.animateItem()) {
                if (!showProjectBanners) {
                    ProjectRow(
                        project = project,
                        showDescription = false,
                        isSelected = actionProject?.id == project.id,
                        asCard = true,
                        onClick = { onProjectClick(project) },
                        onLongClick = { actionProject = project },
                    )
                } else {
                    ProjectBannerCard(
                        model = project.toProjectRowModel(),
                        isFavorite = projectId in favoriteProjectIds,
                        isSelected = actionProject?.id == project.id,
                        onFavoriteClick = { onToggleFavoriteProject(projectId) },
                        onClick = { onProjectClick(project) },
                        onLongClick = { actionProject = project },
                    )
                }
                }
            }
        }
    }
        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.project_create)) },
            icon = { Icon(Lucide.Plus, contentDescription = null) },
            onClick = onCreateProject,
            expanded = isFabExpanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = RyntraDesign.floatingActionInset),
        )
    }

    actionProject?.let { project ->
        ProjectActionsSheet(
            project = project,
            canDelete = true,
            onDismiss = { actionProject = null },
            onOpen = {
                actionProject = null
                onProjectClick(project)
            },
            onOpenInBrowser = {
                actionProject = null
                uriHandler.openUri(project.modrinthPageUrl())
            },
            onDeleteRequest = {
                actionProject = null
                deletingProject = project
                onClearProjectActionStatus()
            },
        )
    }
    deletingProject?.let { project ->
        DeleteProjectDialog(
            project = project,
            isDeleting = projectAction.isRunning && projectAction.targetId == project.id,
            errorMessage = projectAction.errorMessage.takeIf { projectAction.targetId == project.id },
            onDismiss = {
                deletingProject = null
                onClearProjectActionStatus()
            },
            onConfirm = { onDeleteProject(project.id) },
        )
    }
}
