package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.GalleryImage
import com.ryntra.shared.model.ModeratorMessage
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectLicense
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modrinth **v3** organization project payload.
 *
 * Differs from classic v2 [Project]: uses `name`/`summary`/`project_types`/`team_id`
 * instead of `title`/`description`/`project_type`/`team`.
 * See GET `/v3/organization/{id|slug}/projects`.
 */
@Serializable
internal data class OrganizationProjectDto(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val body: String? = null,
    val categories: List<String> = emptyList(),
    @SerialName("project_type") val projectType: String? = null,
    @SerialName("project_types") val projectTypes: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    @SerialName("client_side") val clientSide: String? = null,
    @SerialName("server_side") val serverSide: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    val downloads: Long = 0,
    val followers: Long = 0,
    val status: String = "unknown",
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("moderator_message") val moderatorMessage: ModeratorMessage? = null,
    val queued: String? = null,
    val updated: String? = null,
    val team: String? = null,
    @SerialName("team_id") val teamId: String? = null,
    val organization: String? = null,
    val license: ProjectLicense? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    val published: String? = null,
    val gallery: List<GalleryImage> = emptyList(),
) {
    fun toProject(): Project = Project(
        id = id,
        slug = slug,
        title = title?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: slug
            ?: id,
        description = summary?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() && it.length < 500 }
            ?: "",
        body = body?.takeIf { it.isNotBlank() }
            ?: description.orEmpty(),
        categories = categories,
        projectType = projectType?.takeIf { it.isNotBlank() }
            ?: projectTypes.firstOrNull().orEmpty().ifBlank { "project" },
        loaders = loaders,
        clientSide = clientSide ?: "unknown",
        serverSide = serverSide ?: "unknown",
        iconUrl = iconUrl,
        downloads = downloads,
        followers = followers,
        status = status,
        requestedStatus = requestedStatus,
        moderatorMessage = moderatorMessage,
        queued = queued,
        updated = updated,
        team = team ?: teamId,
        organization = organization,
        license = license,
        sourceUrl = sourceUrl,
        issuesUrl = issuesUrl,
        wikiUrl = wikiUrl,
        discordUrl = discordUrl,
        published = published,
        gallery = gallery,
    )
}
