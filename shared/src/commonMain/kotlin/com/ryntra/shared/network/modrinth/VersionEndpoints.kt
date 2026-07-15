package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

internal class VersionEndpoints(
    private val client: HttpClient,
) {
    suspend fun getForProject(projectIdOrSlug: String, token: String): List<ProjectVersion> =
        client.get("project/$projectIdOrSlug/version") { authorize(token) }.decode()

    suspend fun create(projectId: String, request: CreateVersionRequest, token: String): ProjectVersion {
        validate(request)
        val partNames = request.files.indices.map { "file$it" }
        val payload = CreateVersionPayload(
            name = request.name.trim(),
            versionNumber = request.versionNumber.trim(),
            changelog = request.changelog,
            dependencies = request.dependencies,
            gameVersions = request.gameVersions,
            versionType = request.versionType,
            loaders = request.loaders,
            featured = request.featured,
            projectId = projectId,
            fileParts = partNames,
            primaryFile = partNames[request.primaryFileIndex],
        )
        val multipart = MultiPartFormDataContent(
            formData {
                append(
                    key = "data",
                    value = apiJson.encodeToString(payload),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    },
                )
                request.files.forEachIndexed { index, file ->
                    append(
                        key = partNames[index],
                        value = file.bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, file.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.safeFileName()}\"")
                        },
                    )
                }
            },
        )
        return client.post("version") {
            authorize(token)
            setBody(multipart)
        }.decode()
    }

    suspend fun update(versionId: String, update: VersionUpdate, token: String) {
        client.patch("version/$versionId") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(update)
        }.ensureSuccess()
    }

    suspend fun delete(versionId: String, token: String) {
        client.delete("version/$versionId") { authorize(token) }.ensureSuccess()
    }

    private fun validate(request: CreateVersionRequest) {
        require(request.name.isNotBlank()) { "Version name cannot be empty." }
        require(request.versionNumber.isNotBlank()) { "Version number cannot be empty." }
        require(request.gameVersions.isNotEmpty()) { "Select at least one game version." }
        require(request.loaders.isNotEmpty()) { "Select at least one loader." }
        require(request.files.isNotEmpty()) { "Select at least one version file." }
        require(request.primaryFileIndex in request.files.indices) { "Select a valid primary file." }
    }
}

@Serializable
private data class CreateVersionPayload(
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    val changelog: String,
    val dependencies: List<ProjectDependency>,
    @SerialName("game_versions") val gameVersions: List<String>,
    @SerialName("version_type") val versionType: String,
    val loaders: List<String>,
    val featured: Boolean,
    @SerialName("project_id") val projectId: String,
    @SerialName("file_parts") val fileParts: List<String>,
    @SerialName("primary_file") val primaryFile: String,
)
