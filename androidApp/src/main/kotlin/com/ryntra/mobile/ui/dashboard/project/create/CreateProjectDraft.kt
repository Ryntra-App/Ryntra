package com.ryntra.mobile.ui.dashboard.project.create

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationRules
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.isCustomLicenseReference
import java.net.URI

@Stable
internal class CreateProjectDraft {
    var step by mutableStateOf(0)
    var title by mutableStateOf("")
    var slug by mutableStateOf("")
    var isSlugManuallyEdited by mutableStateOf(false)
    var summary by mutableStateOf("")
    var projectType by mutableStateOf("")
    var categories by mutableStateOf(emptySet<String>())
    var clientSide by mutableStateOf("unknown")
    var serverSide by mutableStateOf("unknown")
    var licenseId by mutableStateOf("MIT")
    var licenseUrl by mutableStateOf("")
    var body by mutableStateOf("")
    var sourceUrl by mutableStateOf("")
    var issuesUrl by mutableStateOf("")
    var wikiUrl by mutableStateOf("")
    var discordUrl by mutableStateOf("")
    var icon by mutableStateOf<ProjectFileUpload?>(null)
    var editorMode by mutableStateOf(MarkdownEditorMode.Write)
    private var validationAttemptedSteps by mutableStateOf(emptySet<Int>())

    val isDirty: Boolean
        get() = title.isNotBlank() || summary.isNotBlank() || body.isNotBlank() || icon != null

    val isTitleValid: Boolean
        get() = title.trim().length in ProjectCreationRules.TITLE_MIN_LENGTH..ProjectCreationRules.TITLE_MAX_LENGTH

    val isSlugValid: Boolean
        get() = ProjectCreationRules.isSlugValid(slug)

    val isSummaryValid: Boolean
        get() = summary.trim().length in ProjectCreationRules.DESCRIPTION_MIN_LENGTH..ProjectCreationRules.DESCRIPTION_MAX_LENGTH

    val areLinksValid: Boolean
        get() = listOf(sourceUrl, issuesUrl, wikiUrl, discordUrl).all { it.isBlank() || it.isWebUrl() }

    val isLicenseValid: Boolean
        get() = licenseId.isNotBlank() &&
            (!licenseId.isCustomLicenseReference() || licenseUrl.isWebUrl())

    fun updateTitle(value: String) {
        title = value.take(ProjectCreationRules.TITLE_MAX_LENGTH + 1)
        if (!isSlugManuallyEdited) slug = value.toModrinthSlug()
    }

    fun updateSlug(value: String) {
        isSlugManuallyEdited = true
        slug = value.lowercase().replace(' ', '-').take(ProjectCreationRules.SLUG_MAX_LENGTH)
    }

    fun toggleCategory(category: String) {
        categories = when {
            category in categories -> categories - category
            categories.size < ProjectCreationRules.CATEGORIES_MAX_COUNT -> categories + category
            else -> categories
        }
    }

    fun isStepValid(): Boolean = when (step) {
        0 -> isTitleValid && isSlugValid && isSummaryValid && projectType.isNotBlank()
        1 -> isLicenseValid
        else -> body.isNotBlank() && body.length <= ProjectCreationRules.BODY_MAX_LENGTH &&
            areLinksValid && ProjectCreationRules.validate(toRequest()).isEmpty()
    }

    fun markValidationAttempted() {
        validationAttemptedSteps = validationAttemptedSteps + step
    }

    fun shouldShowValidationErrors(step: Int): Boolean = step in validationAttemptedSteps

    fun toRequest(): CreateProjectRequest = CreateProjectRequest(
        slug = slug.trim(),
        title = title.trim(),
        description = summary.trim(),
        body = body.trim(),
        projectType = projectType,
        categories = categories.sorted(),
        clientSide = clientSide,
        serverSide = serverSide,
        licenseId = licenseId.trim(),
        licenseUrl = licenseUrl.trim().ifBlank { null },
        sourceUrl = sourceUrl.trim().ifBlank { null },
        issuesUrl = issuesUrl.trim().ifBlank { null },
        wikiUrl = wikiUrl.trim().ifBlank { null },
        discordUrl = discordUrl.trim().ifBlank { null },
        icon = icon,
    )
}

private fun String.toModrinthSlug(): String = lowercase()
    .trim()
    .replace(Regex("[^a-z0-9_-]+"), "-")
    .trim('-')
    .take(ProjectCreationRules.SLUG_MAX_LENGTH)

private fun String.isWebUrl(): Boolean = runCatching { URI(this) }.getOrNull()?.let { uri ->
    uri.scheme?.lowercase() in setOf("http", "https") && uri.host?.contains('.') == true
} == true
