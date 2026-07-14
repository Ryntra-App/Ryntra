package com.rinthy.shared.network.modrinth

import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectUpdate
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class ProjectEndpoints(
    private val client: HttpClient,
) {
    suspend fun getForUser(userId: String, token: String): List<Project> =
        client.get("user/$userId/projects") { authorize(token) }.decode()

    suspend fun get(projectIdOrSlug: String, token: String): Project =
        client.get("project/$projectIdOrSlug") { authorize(token) }.decode()

    suspend fun update(projectIdOrSlug: String, update: ProjectUpdate, token: String) {
        client.patch("project/$projectIdOrSlug") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(update)
        }.ensureSuccess()
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
}
