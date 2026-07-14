package com.rinthy.mobile.ui.dashboard.overview

import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectAttentionKind
import com.rinthy.shared.model.ProjectDisplayKind
import com.rinthy.shared.model.isInReview

internal data class OverviewSnapshot(
    val totalDownloads: Long,
    val totalFollowers: Long,
    val approvedProjects: Int,
    val attentionProjects: List<Project>,
    val attentionCount: Int,
    val inReviewProjects: List<Project>,
    val inReviewCount: Int,
    val recentProjects: List<Project>,
    val leadingProject: Project?,
    val projectTypes: List<ProjectTypeCount>,
)

internal data class ProjectTypeCount(
    val kind: ProjectDisplayKind,
    val count: Int,
)

internal fun Dashboard.createOverviewSnapshot(): OverviewSnapshot {
    val attention = projects
        .filter(Project::needsAttention)
        .sortedWith(
            compareBy<Project> { attentionSortRank(it) }
                .thenByDescending { it.updated.orEmpty() },
        )
    // Quiet "processing" only — projects that also need action live under attention.
    val inReview = projects
        .filter { it.isInReview() && !it.needsAttention() }
        .sortedByDescending { it.queued.orEmpty().ifBlank { it.updated.orEmpty() } }
    return OverviewSnapshot(
        totalDownloads = projects.sumOf(Project::downloads),
        totalFollowers = projects.sumOf(Project::followers),
        approvedProjects = projects.count { it.status.equals("approved", ignoreCase = true) },
        attentionProjects = attention.take(5),
        attentionCount = attention.size,
        inReviewProjects = inReview.take(5),
        inReviewCount = inReview.size,
        recentProjects = projects.sortedByDescending { it.updated.orEmpty() }.take(4),
        leadingProject = projects.maxByOrNull(Project::downloads),
        projectTypes = projects
            .groupingBy(Project::displayKind)
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<ProjectDisplayKind, Int>> { it.value }.thenBy { it.key.name })
            .take(4)
            .map { ProjectTypeCount(it.key, it.value) },
    )
}

/** Lower rank = higher priority in the attention list. */
private fun attentionSortRank(project: Project): Int = when (project.attentionState().kind) {
    ProjectAttentionKind.Rejected -> 0
    ProjectAttentionKind.Withheld -> 1
    ProjectAttentionKind.Unknown -> 2
    else -> 3
}
