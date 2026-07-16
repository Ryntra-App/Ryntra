package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class TeamOrganizationEndpoints(
    private val client: HttpClient,
) {
    /**
     * Loads collaborators for a project.
     * Prefers `team/{id}/members` (includes pending invites when authorized),
     * then merges with `project/{id}/members` so org-linked projects still show a roster.
     */
    suspend fun getProjectMembers(projectIdOrSlug: String, teamId: String?, token: String): List<ProjectMember> {
        val byId = linkedMapOf<String, ProjectMember>()
        var lastError: Exception? = null

        if (!teamId.isNullOrBlank()) {
            try {
                client.get("team/$teamId/members") { authorize(token) }
                    .decode<List<ProjectMember>>()
                    .forEach { byId[it.user.id] = it }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }

        try {
            client.get("project/$projectIdOrSlug/members") { authorize(token) }
                .decode<List<ProjectMember>>()
                .forEach { member ->
                    if (member.user.id !in byId) byId[member.user.id] = member
                }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            lastError = error
        }

        if (byId.isEmpty() && lastError != null) throw lastError

        return byId.values.sortedWith(
            compareByDescending<ProjectMember> { it.isOwner }
                .thenBy { it.ordering }
                .thenBy { it.user.username.lowercase() },
        )
    }

    suspend fun addMember(teamId: String, userId: String, token: String) {
        client.post("team/$teamId/members") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(TeamUserRequest(userId))
        }.ensureSuccess()
    }

    suspend fun updateMember(teamId: String, userId: String, update: ProjectMemberUpdate, token: String) {
        client.patch("team/$teamId/members/$userId") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(update)
        }.ensureSuccess()
    }

    suspend fun deleteMember(teamId: String, userId: String, token: String) {
        client.delete("team/$teamId/members/$userId") { authorize(token) }.ensureSuccess()
    }

    suspend fun join(teamId: String, token: String) {
        client.post("team/$teamId/join") { authorize(token) }.ensureSuccess()
    }

    suspend fun transferOwnership(teamId: String, userId: String, token: String) {
        client.patch("team/$teamId/owner") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(TeamUserRequest(userId))
        }.ensureSuccess()
    }

    suspend fun getOrganizations(userId: String, token: String): List<Organization> {
        // v3 includes members inline; prefer it over the sparse v2 shape.
        val endpoints = listOf(
            "https://api.modrinth.com/v3/user/$userId/organizations",
            "https://api.modrinth.com/v2/user/$userId/organizations",
        )
        var authorizationFailure: ApiException? = null
        for (endpoint in endpoints) {
            try {
                return client.get(endpoint) { authorize(token) }.decode()
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                if (error.statusCode == 401 || error.statusCode == 403) authorizationFailure = error
            } catch (_: Exception) {
                // Organizations are optional dashboard data; try the fallback API version.
            }
        }
        authorizationFailure?.let { throw it }
        return emptyList()
    }

    suspend fun getOrganization(organizationIdOrSlug: String, token: String): Organization {
        val endpoints = listOf(
            "https://api.modrinth.com/v3/organization/$organizationIdOrSlug",
            "https://api.modrinth.com/v2/organization/$organizationIdOrSlug",
        )
        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                return client.get(endpoint) { authorize(token) }.decode()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: ApiException(404, "Organization not found.")
    }

    /**
     * Organization members: v3 org route first (includes pending + org permission bits),
     * then the underlying team route. Results are merged by user id.
     */
    suspend fun getOrganizationMembers(organizationIdOrSlug: String, teamId: String?, token: String): List<ProjectMember> {
        val byId = linkedMapOf<String, ProjectMember>()
        var lastError: Exception? = null
        val endpoints = buildList {
            add("https://api.modrinth.com/v3/organization/$organizationIdOrSlug/members")
            add("https://api.modrinth.com/v2/organization/$organizationIdOrSlug/members")
            if (!teamId.isNullOrBlank()) add("team/$teamId/members")
        }
        for (endpoint in endpoints) {
            try {
                client.get(endpoint) { authorize(token) }
                    .decode<List<ProjectMember>>()
                    .forEach { member ->
                        if (member.user.id !in byId) byId[member.user.id] = member
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        if (byId.isEmpty() && lastError != null) throw lastError
        return byId.values.sortedWith(
            compareByDescending<ProjectMember> { it.isOwner }
                .thenBy { it.ordering }
                .thenBy { it.user.username.lowercase() },
        )
    }

    /**
     * Organization projects live on **v3 only** (v2 route 404s).
     * Payload uses `name`/`summary`/`project_types` — map via [OrganizationProjectDto].
     *
     * Tries each id/slug key and prefers a non-empty result. Auth failures (401/403)
     * are rethrown so the UI can show a real error instead of a fake empty list.
     */
    suspend fun getOrganizationProjects(organizationIdOrSlug: String, token: String): List<Project> {
        val keys = listOf(organizationIdOrSlug)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        var authorizationFailure: ApiException? = null
        var lastError: Exception? = null
        var sawSuccessfulEmpty = false

        for (key in keys) {
            val encodedKey = key // path segment; Modrinth slugs/ids are URL-safe base62
            val endpoint = "https://api.modrinth.com/v3/organization/$encodedKey/projects"
            try {
                val response = client.get(endpoint) { authorize(token) }
                response.ensureSuccess()
                val text = response.bodyAsText()
                val projects = decodeOrganizationProjectsPayload(text)
                if (projects.isNotEmpty()) return projects
                sawSuccessfulEmpty = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                if (error.statusCode == 401 || error.statusCode == 403) {
                    authorizationFailure = error
                } else {
                    lastError = error
                }
            } catch (error: Exception) {
                lastError = error
            }
        }

        authorizationFailure?.let { throw it }
        if (sawSuccessfulEmpty) return emptyList()

        val fallbackKey = keys.firstOrNull()
        if (fallbackKey == null) {
            throw lastError ?: ApiException(404, "Organization projects unavailable.")
        }

        // Last resort: legacy decode path (kept for unit tests / future v2 restore).
        try {
            return client.get("https://api.modrinth.com/v2/organization/$fallbackKey/projects") {
                authorize(token)
            }.decode<List<Project>>()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.statusCode == 403) throw error
            throw error
        } catch (error: Exception) {
            throw lastError ?: error
        }
    }
}

/**
 * Decode org projects JSON. Primary shape is v3 OrganizationProjectDto[];
 * also accepts classic v2 Project[] when `title` is present.
 */
internal fun decodeOrganizationProjectsPayload(text: String): List<Project> {
    // Prefer the tolerant DTO (handles name/title and project_types/project_type).
    val asDto = runCatching {
        apiJson.decodeFromString<List<OrganizationProjectDto>>(text).map { it.toProject() }
    }.getOrNull()
    if (asDto != null) return asDto
    return apiJson.decodeFromString(text)
}

@Serializable
private data class TeamUserRequest(
    @SerialName("user_id") val userId: String,
)
