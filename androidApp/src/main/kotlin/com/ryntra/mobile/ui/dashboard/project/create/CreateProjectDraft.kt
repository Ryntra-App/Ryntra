package com.ryntra.mobile.ui.dashboard.project.create

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationRules
import com.ryntra.shared.model.ProjectFileUpload

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
    var body by mutableStateOf("")
    var sourceUrl by mutableStateOf("")
    var issuesUrl by mutableStateOf("")
    var wikiUrl by mutableStateOf("")
    var discordUrl by mutableStateOf("")
    var icon by mutableStateOf<ProjectFileUpload?>(null)
    var editorMode by mutableStateOf(MarkdownEditorMode.Write)

    val isDirty: Boolean
        get() = title.isNotBlank() || summary.isNotBlank() || body.isNotBlank() || icon != null

    val isTitleValid: Boolean
        get() = title.isNotBlank() && title.length <= ProjectCreationRules.TITLE_MAX_LENGTH

    val isSlugValid: Boolean
        get() = slug.length in ProjectCreationRules.SLUG_MIN_LENGTH..ProjectCreationRules.SLUG_MAX_LENGTH &&
            slug.all { it.isLetterOrDigit() || it in "_!@$()`.+,\"'-" }

    val isSummaryValid: Boolean
        get() = summary.isNotBlank() && summary.length <= ProjectCreationRules.DESCRIPTION_MAX_LENGTH

    val areLinksValid: Boolean
        get() = listOf(sourceUrl, issuesUrl, wikiUrl, discordUrl).all { it.isBlank() || it.isWebUrl() }

    fun updateTitle(value: String) {
        title = value.take(ProjectCreationRules.TITLE_MAX_LENGTH + 1)
        if (!isSlugManuallyEdited) slug = value.toModrinthSlug()
    }

    fun updateSlug(value: String) {
        isSlugManuallyEdited = true
        slug = value.lowercase().replace(' ', '-').take(ProjectCreationRules.SLUG_MAX_LENGTH)
    }

    fun toggleCategory(category: String) {
        categories = if (category in categories) categories - category else categories + category
    }

    fun canContinue(): Boolean = when (step) {
        0 -> isTitleValid && isSlugValid && isSummaryValid && projectType.isNotBlank()
        1 -> licenseId.isNotBlank()
        else -> body.isNotBlank() && areLinksValid && ProjectCreationRules.validate(toRequest()).isEmpty()
    }

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

private fun String.isWebUrl(): Boolean = startsWith("https://") || startsWith("http://")
