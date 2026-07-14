package com.rinthy.shared.network

import com.rinthy.shared.model.Account
import com.rinthy.shared.model.AccountProfileUpdate
import com.rinthy.shared.model.AnalyticsQuery
import com.rinthy.shared.model.CreateVersionRequest
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate
import com.rinthy.shared.model.ProjectUpdate
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.model.VersionUpdate
import com.rinthy.shared.network.modrinth.AccountEndpoints
import com.rinthy.shared.network.modrinth.InsightEndpoints
import com.rinthy.shared.network.modrinth.ProjectEndpoints
import com.rinthy.shared.network.modrinth.TeamOrganizationEndpoints
import com.rinthy.shared.network.modrinth.VersionEndpoints
import io.ktor.client.HttpClient

class ModrinthApi(
    private val httpClient: HttpClient,
) {
    private val accounts = AccountEndpoints(httpClient)
    private val projects = ProjectEndpoints(httpClient)
    private val versions = VersionEndpoints(httpClient)
    private val teams = TeamOrganizationEndpoints(httpClient)
    private val insights = InsightEndpoints(httpClient)

    suspend fun getCurrentAccount(token: String): Account = accounts.getCurrent(token)

    suspend fun updateAccountProfile(userId: String, update: AccountProfileUpdate, token: String) =
        accounts.updateProfile(userId, update, token)

    suspend fun findUser(username: String, token: String): Account? = accounts.findUser(username, token)

    suspend fun getProjects(userId: String, token: String): List<Project> = projects.getForUser(userId, token)

    suspend fun getProject(projectIdOrSlug: String, token: String): Project = projects.get(projectIdOrSlug, token)

    suspend fun updateProject(projectIdOrSlug: String, update: ProjectUpdate, token: String) =
        projects.update(projectIdOrSlug, update, token)

    suspend fun changeProjectIcon(projectIdOrSlug: String, file: ProjectFileUpload, token: String) =
        projects.changeIcon(projectIdOrSlug, file, token)

    suspend fun deleteProjectIcon(projectIdOrSlug: String, token: String) =
        projects.deleteIcon(projectIdOrSlug, token)

    suspend fun addGalleryImage(
        projectIdOrSlug: String,
        file: ProjectFileUpload,
        featured: Boolean,
        title: String,
        description: String,
        token: String,
    ) = projects.addGalleryImage(projectIdOrSlug, file, featured, title, description, token)

    suspend fun deleteGalleryImage(projectIdOrSlug: String, imageUrl: String, token: String) =
        projects.deleteGalleryImage(projectIdOrSlug, imageUrl, token)

    suspend fun getProjectVersions(projectIdOrSlug: String, token: String): List<ProjectVersion> =
        versions.getForProject(projectIdOrSlug, token)

    suspend fun createVersion(projectId: String, request: CreateVersionRequest, token: String): ProjectVersion =
        versions.create(projectId, request, token)

    suspend fun updateVersion(versionId: String, update: VersionUpdate, token: String) =
        versions.update(versionId, update, token)

    suspend fun deleteVersion(versionId: String, token: String) = versions.delete(versionId, token)

    suspend fun getProjectMembers(projectIdOrSlug: String, teamId: String?, token: String): List<ProjectMember> =
        teams.getProjectMembers(projectIdOrSlug, teamId, token)

    suspend fun addTeamMember(teamId: String, userId: String, token: String) =
        teams.addMember(teamId, userId, token)

    suspend fun updateTeamMember(teamId: String, userId: String, update: ProjectMemberUpdate, token: String) =
        teams.updateMember(teamId, userId, update, token)

    suspend fun deleteTeamMember(teamId: String, userId: String, token: String) =
        teams.deleteMember(teamId, userId, token)

    suspend fun joinTeam(teamId: String, token: String) = teams.join(teamId, token)

    suspend fun transferTeamOwnership(teamId: String, userId: String, token: String) =
        teams.transferOwnership(teamId, userId, token)

    suspend fun getOrganizations(userId: String, token: String): List<Organization> =
        teams.getOrganizations(userId, token)

    suspend fun getOrganization(organizationIdOrSlug: String, token: String): Organization =
        teams.getOrganization(organizationIdOrSlug, token)

    suspend fun getOrganizationMembers(
        organizationIdOrSlug: String,
        teamId: String?,
        token: String,
    ): List<ProjectMember> = teams.getOrganizationMembers(organizationIdOrSlug, teamId, token)

    suspend fun getOrganizationProjects(organizationIdOrSlug: String, token: String): List<Project> =
        teams.getOrganizationProjects(organizationIdOrSlug, token)

    suspend fun getAnalytics(query: AnalyticsQuery, includeRevenue: Boolean, token: String): AnalyticsResponse =
        insights.getAnalytics(query, includeRevenue, token)

    suspend fun getPayoutHistory(userId: String, token: String): PayoutHistoryResponse =
        insights.getPayoutHistory(userId, token)

    suspend fun getPayoutBalance(token: String): PayoutBalanceResponse = insights.getPayoutBalance(token)

    fun close() = httpClient.close()
}
