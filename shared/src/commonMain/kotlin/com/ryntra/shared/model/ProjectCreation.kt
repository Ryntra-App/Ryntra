package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class CreateProjectRequest(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val projectType: String,
    val categories: List<String>,
    val additionalCategories: List<String> = emptyList(),
    val clientSide: String = "unknown",
    val serverSide: String = "unknown",
    val licenseId: String,
    val licenseUrl: String? = null,
    val sourceUrl: String? = null,
    val issuesUrl: String? = null,
    val wikiUrl: String? = null,
    val discordUrl: String? = null,
    val icon: ProjectFileUpload? = null,
)

@Serializable
data class ProjectCategory(
    val name: String,
    @SerialName("project_type") val projectType: String,
    val header: String,
    val icon: String = "",
)

data class ProjectCreationMetadata(
    val projectTypes: List<String>,
    val categories: List<ProjectCategory>,
    val licenses: List<ProjectLicense>,
)

object ProjectCreationRules {
    const val SLUG_MIN_LENGTH = 3
    const val SLUG_MAX_LENGTH = 64
    const val TITLE_MIN_LENGTH = 3
    const val TITLE_MAX_LENGTH = 64
    const val DESCRIPTION_MIN_LENGTH = 3
    const val DESCRIPTION_MAX_LENGTH = 256
    const val BODY_MAX_LENGTH = 65_536
    const val CATEGORIES_MAX_COUNT = 3
    private val slugPattern = Regex("^[A-Za-z0-9_!@$()`.+,\\\"'\\-]{3,64}$")
    val environmentValues = listOf("required", "optional", "unsupported", "unknown")

    fun validate(request: CreateProjectRequest): List<String> = buildList {
        val slug = request.slug.trim()
        val title = request.title.trim()
        val description = request.description.trim()
        if (!isSlugValid(slug)) add("Slug must be 3–64 characters and contain only Modrinth-supported characters.")
        if (title.length < TITLE_MIN_LENGTH) add("Project name must be at least $TITLE_MIN_LENGTH characters.")
        if (title.length > TITLE_MAX_LENGTH) add("Project name must be $TITLE_MAX_LENGTH characters or fewer.")
        if (description.length < DESCRIPTION_MIN_LENGTH) add("Summary must be at least $DESCRIPTION_MIN_LENGTH characters.")
        if (description.length > DESCRIPTION_MAX_LENGTH) add("Summary must be $DESCRIPTION_MAX_LENGTH characters or fewer.")
        if (request.body.isBlank()) add("Add a full project description.")
        if (request.body.length > BODY_MAX_LENGTH) add("Full description must be $BODY_MAX_LENGTH characters or fewer.")
        if (request.projectType.isBlank()) add("Select a project type.")
        if (request.licenseId.isBlank()) add("Select a license.")
        if (request.categories.distinct().size > CATEGORIES_MAX_COUNT) {
            add("Select no more than $CATEGORIES_MAX_COUNT primary categories.")
        }
        if (request.clientSide !in environmentValues || request.serverSide !in environmentValues) {
            add("Select valid client and server support values.")
        }
        request.icon?.let { icon ->
            if (!icon.contentType.startsWith("image/")) add("Project icon must be an image.")
            if (icon.bytes.size > ProjectUploadLimits.PROJECT_ICON_BYTES) add("Project icon must be 256 KiB or smaller.")
        }
        listOf(request.sourceUrl, request.issuesUrl, request.wikiUrl, request.discordUrl, request.licenseUrl)
            .filterNotNull().filter(String::isNotBlank).forEach { url ->
                if (!url.startsWith("https://") && !url.startsWith("http://")) add("Links must start with https:// or http://.")
            }
    }.distinct()

    fun isSlugValid(value: String): Boolean = slugPattern.matches(value.trim())
}
