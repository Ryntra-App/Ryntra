package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectUpdate
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationRules
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

internal class ProjectEndpoints(
    private val client: HttpClient,
) {
    suspend fun getForUser(userId: String, token: String): List<Project> =
        client.get("user/$userId/projects") { authorize(token) }.decode()

    suspend fun get(projectIdOrSlug: String, token: String): Project =
        client.get("project/$projectIdOrSlug") { authorize(token) }.decode()

    suspend fun getMany(projectIds: List<String>, token: String): List<Project> {
        val ids = projectIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return emptyList()
        return client.get("projects") {
            authorize(token)
            parameter("ids", apiJson.encodeToString(ids))
        }.decode()
    }

    suspend fun update(projectIdOrSlug: String, update: ProjectUpdate, token: String) {
        client.patch("project/$projectIdOrSlug") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(update)
        }.ensureSuccess()
    }

    suspend fun create(request: CreateProjectRequest, token: String): Project {
        val errors = ProjectCreationRules.validate(request)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val payload = CreateProjectPayload(
            slug = request.slug.trim(), title = request.title.trim(), description = request.description.trim(),
            body = request.body.trim(), projectType = request.projectType, categories = request.categories.distinct(),
            additionalCategories = request.additionalCategories.distinct(), clientSide = request.clientSide,
            serverSide = request.serverSide, licenseId = request.licenseId, licenseUrl = request.licenseUrl.blankToNull(),
            sourceUrl = request.sourceUrl.blankToNull(), issuesUrl = request.issuesUrl.blankToNull(),
            wikiUrl = request.wikiUrl.blankToNull(), discordUrl = request.discordUrl.blankToNull(), isDraft = true,
        )
        val multipart = MultiPartFormDataContent(formData {
            append("data", apiJson.encodeToString(payload), Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            })
            request.icon?.let { icon ->
                append("icon", icon.bytes, Headers.build {
                    append(HttpHeaders.ContentType, icon.contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${icon.safeFileName()}\"")
                })
            }
        })
        return client.post("project") { authorize(token); setBody(multipart) }.decode()
    }

    suspend fun changeIcon(projectIdOrSlug: String, file: ProjectFileUpload, token: String) {
        client.patch("project/$projectIdOrSlug/icon") {
            authorize(token)
            parameter("ext", file.imageExtension())
            contentType(ContentType.parse(file.contentType))
            setBody(file.bytes)
        }.ensureSuccess()
    }

    suspend fun deleteIcon(projectIdOrSlug: String, token: String) {
        client.delete("project/$projectIdOrSlug/icon") { authorize(token) }.ensureSuccess()
    }

    suspend fun addGalleryImage(
        projectIdOrSlug: String,
        file: ProjectFileUpload,
        featured: Boolean,
        title: String,
        description: String,
        token: String,
    ) {
        client.post("project/$projectIdOrSlug/gallery") {
            authorize(token)
            parameter("ext", file.imageExtension())
            parameter("featured", featured)
            if (title.isNotBlank()) parameter("title", title)
            if (description.isNotBlank()) parameter("description", description)
            contentType(ContentType.parse(file.contentType))
            setBody(file.bytes)
        }.ensureSuccess()
    }

    suspend fun deleteGalleryImage(projectIdOrSlug: String, imageUrl: String, token: String) {
        client.delete("project/$projectIdOrSlug/gallery") {
            authorize(token)
            parameter("url", imageUrl)
        }.ensureSuccess()
    }

    /**
     * PATCH gallery metadata: featured (banner), title, description, ordering.
     * See Modrinth OpenAPI `modifyGalleryImage`.
     */
    suspend fun modifyGalleryImage(
        projectIdOrSlug: String,
        imageUrl: String,
        featured: Boolean? = null,
        title: String? = null,
        description: String? = null,
        ordering: Int? = null,
        token: String,
    ) {
        client.patch("project/$projectIdOrSlug/gallery") {
            authorize(token)
            parameter("url", imageUrl)
            featured?.let { parameter("featured", it) }
            title?.let { parameter("title", it) }
            description?.let { parameter("description", it) }
            ordering?.let { parameter("ordering", it) }
        }.ensureSuccess()
    }
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

@Serializable
private data class CreateProjectPayload(
    val slug: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    @SerialName("additional_categories") val additionalCategories: List<String>,
    @SerialName("client_side") val clientSide: String,
    @SerialName("server_side") val serverSide: String,
    val body: String,
    @SerialName("license_id") val licenseId: String,
    @SerialName("license_url") val licenseUrl: String? = null,
    @SerialName("project_type") val projectType: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    @SerialName("is_draft") val isDraft: Boolean = true,
    @SerialName("initial_versions") val initialVersions: List<String> = emptyList(),
)
