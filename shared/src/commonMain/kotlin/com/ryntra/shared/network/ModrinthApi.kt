package com.ryntra.shared.network

import com.ryntra.shared.model.Account
import com.ryntra.shared.model.AccountProfileUpdate
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.DisclosureChangeSet
import com.ryntra.shared.model.ProjectCategory
import com.ryntra.shared.model.ProjectDisclosure
import com.ryntra.shared.model.ProjectLicense
import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModerationThread
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectUpdate
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.network.modrinth.AccountEndpoints
import com.ryntra.shared.network.modrinth.DisclosureEndpoints
import com.ryntra.shared.network.modrinth.InsightEndpoints
import com.ryntra.shared.network.modrinth.NotificationEndpoints
import com.ryntra.shared.network.modrinth.NotificationContentResolver
import com.ryntra.shared.network.modrinth.ProjectEndpoints
import com.ryntra.shared.network.modrinth.TeamOrganizationEndpoints
import com.ryntra.shared.network.modrinth.ThreadEndpoints
import com.ryntra.shared.network.modrinth.TagEndpoints
import com.ryntra.shared.network.modrinth.VersionEndpoints
import io.ktor.client.HttpClient

class ModrinthApi(
    private val httpClient: HttpClient,
) {
    private val accounts = AccountEndpoints(httpClient)
    private val projects = ProjectEndpoints(httpClient)
    private val versions = VersionEndpoints(httpClient)
    private val teams = TeamOrganizationEndpoints(httpClient)
    private val disclosures = DisclosureEndpoints(httpClient)
    private val insights = InsightEndpoints(httpClient)
    private val notifications = NotificationEndpoints(httpClient)
    private val threads = ThreadEndpoints(httpClient)
    private val tags = TagEndpoints(httpClient)
    private val notificationContent = NotificationContentResolver(projects, versions)

    suspend fun getCurrentAccount(token: String): Account = accounts.getCurrent(token)

    suspend fun updateAccountProfile(userId: String, update: AccountProfileUpdate, token: String) =
        accounts.updateProfile(userId, update, token)

    suspend fun findUser(username: String, token: String): Account? = accounts.findUser(username, token)

    suspend fun changeUserAvatar(userId: String, file: ProjectFileUpload, token: String) =
        accounts.changeAvatar(userId, file, token)

    suspend fun deleteUserAvatar(userId: String, token: String) =
        accounts.deleteAvatar(userId, token)

    suspend fun getProjects(userId: String, token: String): List<Project> = projects.getForUser(userId, token)

    suspend fun getProject(projectIdOrSlug: String, token: String): Project = projects.get(projectIdOrSlug, token)

    suspend fun getProjectsByIds(projectIds: List<String>, token: String): List<Project> = projects.getMany(projectIds, token)

    suspend fun updateProject(projectIdOrSlug: String, update: ProjectUpdate, token: String) =
        projects.update(projectIdOrSlug, update, token)

    suspend fun deleteProject(projectIdOrSlug: String, token: String) =
        projects.delete(projectIdOrSlug, token)

    suspend fun createProject(request: CreateProjectRequest, token: String): Project = projects.create(request, token)

    suspend fun getProjectDisclosures(projectIdOrSlug: String, token: String): List<ProjectDisclosure> =
        disclosures.getForProject(projectIdOrSlug, token)

    suspend fun modifyProjectDisclosures(
        projectIdOrSlug: String,
        changes: DisclosureChangeSet,
        token: String,
    ) = disclosures.modify(projectIdOrSlug, changes, token)

    suspend fun getProjectTypes(): List<String> = tags.projectTypes()

    suspend fun getProjectCategories(): List<ProjectCategory> = tags.categories()

    suspend fun getLicenses(): List<ProjectLicense> = tags.licenses()

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

    suspend fun modifyGalleryImage(
        projectIdOrSlug: String,
        imageUrl: String,
        featured: Boolean? = null,
        title: String? = null,
        description: String? = null,
        ordering: Int? = null,
        token: String,
    ) = projects.modifyGalleryImage(
        projectIdOrSlug = projectIdOrSlug,
        imageUrl = imageUrl,
        featured = featured,
        title = title,
        description = description,
        ordering = ordering,
        token = token,
    )

    suspend fun getProjectVersions(projectIdOrSlug: String, token: String): List<ProjectVersion> =
        versions.getForProject(projectIdOrSlug, token)

    suspend fun getVersionsByIds(versionIds: List<String>, token: String): List<ProjectVersion> =
        versions.getMany(versionIds, token)

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

    suspend fun getNotifications(userId: String, token: String): List<ModrinthNotification> =
        notificationContent.resolve(notifications.getForUser(userId, token), token)

    suspend fun markNotificationsRead(notificationIds: List<String>, token: String) =
        notifications.markRead(notificationIds, token)

    suspend fun getModerationThread(threadId: String, token: String): ModerationThread =
        threads.get(threadId, token)

    suspend fun replyToModerationThread(threadId: String, body: String, replyingTo: String?, token: String) =
        threads.reply(threadId, body, replyingTo, token)

    suspend fun deleteModerationMessage(messageId: String, token: String) =
        threads.deleteMessage(messageId, token)

    fun close() = httpClient.close()
}
