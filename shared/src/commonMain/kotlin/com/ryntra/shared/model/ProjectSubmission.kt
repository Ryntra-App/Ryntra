package com.ryntra.shared.model

enum class ProjectSubmissionRequirement {
    Version,
    Icon,
    Summary,
    Description,
    License,
}

data class ProjectSubmissionReadiness(
    val missingRequirements: List<ProjectSubmissionRequirement>,
) {
    val canSubmit: Boolean
        get() = missingRequirements.isEmpty()

    val missingRequirementKeys: List<String>
        get() = missingRequirements.map { it.name.lowercase() }
}

fun Project.submissionReadiness(versionCount: Int): ProjectSubmissionReadiness =
    ProjectSubmissionReadiness(
        buildList {
            if (versionCount <= 0) add(ProjectSubmissionRequirement.Version)
            if (iconUrl.isNullOrBlank()) add(ProjectSubmissionRequirement.Icon)
            if (description.trim().length < ProjectCreationRules.DESCRIPTION_MIN_LENGTH) {
                add(ProjectSubmissionRequirement.Summary)
            }
            if (body.isBlank()) add(ProjectSubmissionRequirement.Description)
            if (license?.id.isNullOrBlank()) add(ProjectSubmissionRequirement.License)
        },
    )

fun Project.canEnterModeration(): Boolean =
    status.lowercase() in setOf("draft", "rejected", "withheld")
