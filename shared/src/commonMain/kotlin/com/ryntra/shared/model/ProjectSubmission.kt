package com.ryntra.shared.model

enum class ProjectSubmissionRequirement {
    Version,
    Icon,
    Summary,
    Description,
    License,
    LicenseUrl,
    Gallery,
}

/**
 * Requirements that stop a moderation submission are kept separate from quality
 * recommendations. This mirrors Modrinth's publication checklist: an icon and a
 * useful short summary improve the listing, but their absence must not disable
 * submission.
 */
data class ProjectSubmissionReadiness(
    val blockingRequirements: List<ProjectSubmissionRequirement>,
    val warningRequirements: List<ProjectSubmissionRequirement> = emptyList(),
) {
    val canSubmit: Boolean
        get() = blockingRequirements.isEmpty()

    /** Compatibility accessor for existing platform clients. */
    val missingRequirements: List<ProjectSubmissionRequirement>
        get() = blockingRequirements

    val missingRequirementKeys: List<String>
        get() = blockingRequirements.map(ProjectSubmissionRequirement::apiKey)

    val warningRequirementKeys: List<String>
        get() = warningRequirements.map(ProjectSubmissionRequirement::apiKey)
}

fun Project.submissionReadiness(versionCount: Int): ProjectSubmissionReadiness {
    val normalizedType = projectType.trim().lowercase()
    val normalizedCategories = categories.map { it.trim().lowercase() }.toSet()
    val selectedLicense = license

    return ProjectSubmissionReadiness(
        blockingRequirements = buildList {
            if (versionCount <= 0 && normalizedType != "minecraft_java_server") {
                add(ProjectSubmissionRequirement.Version)
            }
            if (body.isBlank()) add(ProjectSubmissionRequirement.Description)
            if (!selectedLicense.isValidForSubmission()) {
                add(ProjectSubmissionRequirement.License)
            } else if (
                selectedLicense != null &&
                selectedLicense.requiresCustomUrl() &&
                selectedLicense.url.isNullOrBlank()
            ) {
                add(ProjectSubmissionRequirement.LicenseUrl)
            }

            val requiredGalleryImages = when (normalizedType) {
                "shader" -> 3
                // Modrinth exempts resource packs categorized specifically as
                // audio or locale packs from the visual-gallery requirement.
                "resourcepack" -> if (normalizedCategories.any(::isGalleryExemptResourcePackCategory)) 0 else 1
                else -> 0
            }
            if (gallery.size < requiredGalleryImages) add(ProjectSubmissionRequirement.Gallery)
        },
        warningRequirements = buildList {
            if (iconUrl.isNullOrBlank()) add(ProjectSubmissionRequirement.Icon)
            if (description.trim().length < RECOMMENDED_SUMMARY_LENGTH) {
                add(ProjectSubmissionRequirement.Summary)
            }
        },
    )
}

fun Project.canEnterModeration(): Boolean =
    status.lowercase() in setOf("draft", "rejected", "withheld")

private fun ProjectLicense?.isValidForSubmission(): Boolean {
    val normalizedId = this?.id?.trim()?.lowercase().orEmpty()
    return normalizedId.isNotEmpty() && normalizedId !in INVALID_LICENSE_IDS
}

private fun ProjectLicense.requiresCustomUrl(): Boolean {
    val normalizedId = id.trim().lowercase()
    return normalizedId.isCustomLicenseReference()
}

fun String.isCustomLicenseReference(): Boolean {
    val normalizedId = trim().lowercase()
    return normalizedId.startsWith("licenseref-") && normalizedId !in INVALID_LICENSE_IDS
}

private fun ProjectSubmissionRequirement.apiKey(): String = when (this) {
    ProjectSubmissionRequirement.LicenseUrl -> "license_url"
    else -> name.lowercase()
}

private const val RECOMMENDED_SUMMARY_LENGTH = 30

private fun isGalleryExemptResourcePackCategory(category: String): Boolean =
    category == "audio" || category == "locale"

private val INVALID_LICENSE_IDS = setOf(
    "licenseref-unknown",
    "noassertion",
    "licenseref-noassertion",
)
