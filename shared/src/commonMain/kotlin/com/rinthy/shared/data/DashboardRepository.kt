package com.rinthy.shared.data

import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.network.ModrinthApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DashboardRepository(
    private val api: ModrinthApi,
) {
    suspend fun load(token: String): Dashboard = coroutineScope {
        val account = api.getCurrentAccount(token)
        val projects = async { api.getProjects(account.id, token) }
        val organizations = async { api.getOrganizations(account.id, token) }

        Dashboard(
            account = account,
            projects = projects.await().sortedByDescending { it.updated.orEmpty() },
            organizations = organizations.await().sortedBy { it.name.lowercase() },
        )
    }

    suspend fun loadProject(projectIdOrSlug: String, token: String): Project =
        api.getProject(projectIdOrSlug, token)

    suspend fun loadProjectVersions(projectIdOrSlug: String, token: String): List<ProjectVersion> =
        api.getProjectVersions(projectIdOrSlug, token)

    suspend fun loadProjectMembers(projectIdOrSlug: String, teamId: String?, token: String): List<ProjectMember> =
        api.getProjectMembers(projectIdOrSlug, teamId, token)

    fun close() = api.close()
}
