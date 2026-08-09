package com.ryntra.mobile.ui.dashboard.project.edit

import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectUpdate

internal data class ProjectEditDraft(
    val title: String,
    val summary: String,
    val description: String,
    val sourceUrl: String,
    val issuesUrl: String,
    val wikiUrl: String,
    val discordUrl: String,
    val status: String,
    val licenseId: String,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && summary.isNotBlank()

    fun toUpdate(baseline: ProjectEditDraft): ProjectUpdate = ProjectUpdate(
        title = title.takeIf { it != baseline.title },
        description = summary.takeIf { it != baseline.summary },
        body = description.takeIf { it != baseline.description },
        sourceUrl = sourceUrl.takeIf { it != baseline.sourceUrl },
        issuesUrl = issuesUrl.takeIf { it != baseline.issuesUrl },
        wikiUrl = wikiUrl.takeIf { it != baseline.wikiUrl },
        discordUrl = discordUrl.takeIf { it != baseline.discordUrl },
        status = status.takeIf { it != baseline.status },
        licenseId = licenseId.takeIf { it != baseline.licenseId },
    )

    companion object {
        fun from(project: Project): ProjectEditDraft = ProjectEditDraft(
            title = project.title,
            summary = project.description,
            description = project.body,
            sourceUrl = project.sourceUrl.orEmpty(),
            issuesUrl = project.issuesUrl.orEmpty(),
            wikiUrl = project.wikiUrl.orEmpty(),
            discordUrl = project.discordUrl.orEmpty(),
            status = project.status,
            licenseId = project.license?.id.orEmpty(),
        )
    }
}
