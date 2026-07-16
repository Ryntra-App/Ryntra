package com.ryntra.shared.data

import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.AccountProfileUpdate
import com.ryntra.shared.model.Account
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.AnalyticsReport
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectTeamRoster
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.model.WalletReport
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.network.ModrinthApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    suspend fun updateAccountProfile(userId: String, update: AccountProfileUpdate, token: String) =
        api.updateAccountProfile(userId, update, token)

    suspend fun changeUserAvatar(userId: String, file: ProjectFileUpload, token: String) =
        api.changeUserAvatar(userId, file, token)

    suspend fun deleteUserAvatar(userId: String, token: String) =
        api.deleteUserAvatar(userId, token)

    suspend fun loadCurrentAccount(token: String): Account = api.getCurrentAccount(token)

    suspend fun loadNotifications(userId: String, token: String): List<ModrinthNotification> =
        api.getNotifications(userId, token)

    suspend fun markNotificationsRead(notificationIds: List<String>, token: String) =
        api.markNotificationsRead(notificationIds, token)

    suspend fun updateProject(projectIdOrSlug: String, update: com.ryntra.shared.model.ProjectUpdate, token: String) =
        api.updateProject(projectIdOrSlug, update, token)

    suspend fun loadProjectVersions(projectIdOrSlug: String, token: String): List<ProjectVersion> =
        api.getProjectVersions(projectIdOrSlug, token)

    suspend fun loadAnalytics(query: AnalyticsQuery, token: String): AnalyticsReport = coroutineScope {
        val queries = query.batchedByProjectIds()
        val core = async {
            queries
                .map { batchedQuery -> async { api.getAnalytics(batchedQuery, includeRevenue = false, token) } }
                .awaitAll()
                .merge()
        }
        val revenue = async {
            queries
                .map { batchedQuery -> async { api.getAnalytics(batchedQuery, includeRevenue = true, token) } }
                .awaitAll()
                .merge()
        }
        val coreResponse = core.await()
        val revenueResponse = revenue.await()
        val currentSlices = query.currentSlices.coerceIn(0, query.slices)
        val previousSliceCount = (query.slices - currentSlices).coerceAtLeast(0)
        AnalyticsReport(
            points = coreResponse.points.drop(previousSliceCount),
            revenuePoints = revenueResponse.points.drop(previousSliceCount),
            previousPoints = coreResponse.points.take(previousSliceCount),
            previousRevenuePoints = revenueResponse.points.take(previousSliceCount),
            events = coreResponse.events,
            periodStartTime = query.currentStartTime,
            periodEndTime = query.endTime,
            coreStatus = coreResponse.status,
            revenueStatus = revenueResponse.status,
        )
    }

    suspend fun loadWallet(account: Account, token: String): WalletReport = coroutineScope {
        val historyRequest = async { api.getPayoutHistory(account.id, token) }
        val balanceRequest = async { api.getPayoutBalance(token) }
        val history = historyRequest.await()
        val balance = balanceRequest.await()
        val available = balance.available ?: account.payoutData?.balance
        val currentBalance = balance.total ?: nullableSum(available, balance.pending)

        WalletReport(
            currency = balance.currency ?: account.payoutData?.currency ?: "USD",
            wallet = account.payoutData?.wallet,
            walletType = account.payoutData?.walletType,
            payoutAddress = account.payoutData?.address,
            available = available,
            pending = balance.pending,
            withdrawnLifetime = balance.withdrawnLifetime,
            balance = currentBalance,
            allTime = history.allTime,
            lastMonth = history.lastMonth,
            transactions = history.transactions.sortedByDescending { it.created },
            balanceStatus = balance.status,
            historyStatus = history.status,
        )
    }

    suspend fun enrichDependencies(dependencies: List<ProjectDependency>, token: String): List<ProjectDependency> = coroutineScope {
        val projectIds = dependencies.mapNotNull(ProjectDependency::projectId).distinct()
        val projectsById = projectIds
            .map { projectId -> async { runCatching { api.getProject(projectId, token) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
            .associateBy(Project::id)

        dependencies.map { dependency ->
            val project = dependency.projectId?.let(projectsById::get)
            dependency.copy(title = project?.title, iconUrl = project?.iconUrl)
        }
    }

    suspend fun createVersion(projectId: String, request: CreateVersionRequest, token: String): ProjectVersion =
        api.createVersion(projectId, request, token)

    suspend fun updateVersion(versionId: String, update: VersionUpdate, token: String) =
        api.updateVersion(versionId, update, token)

    suspend fun deleteVersion(versionId: String, token: String) = api.deleteVersion(versionId, token)

    suspend fun loadProjectMembers(projectIdOrSlug: String, teamId: String?, token: String): List<ProjectMember> =
        api.getProjectMembers(projectIdOrSlug, teamId, token)

    /**
     * Project team + organization members (when the project belongs to an org).
     * Org members inherit access on Modrinth; they are listed separately so creators
     * can see who manages the project through the organization.
     */
    suspend fun loadProjectTeamRoster(project: Project, token: String): ProjectTeamRoster {
        val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
        val projectMembers = runCatching {
            api.getProjectMembers(projectKey, project.team, token)
        }.getOrDefault(emptyList())

        val orgKey = project.organization?.trim().orEmpty()
        if (orgKey.isEmpty()) {
            return ProjectTeamRoster(projectMembers = projectMembers)
        }

        val organization = runCatching { api.getOrganization(orgKey, token) }.getOrNull()
        val organizationMembers = runCatching {
            val lookup = organization?.slug?.takeIf { it.isNotBlank() }
                ?: organization?.id?.takeIf { it.isNotBlank() }
                ?: orgKey
            api.getOrganizationMembers(
                organizationIdOrSlug = lookup,
                teamId = organization?.teamId,
                token = token,
            )
        }.getOrDefault(emptyList())

        return ProjectTeamRoster(
            projectMembers = projectMembers,
            organizationMembers = organizationMembers,
            organization = organization,
        )
    }

    suspend fun loadOrganizationMembers(
        organizationIdOrSlug: String,
        teamId: String?,
        token: String,
    ): List<ProjectMember> = api.getOrganizationMembers(organizationIdOrSlug, teamId, token)

    suspend fun findUser(username: String, token: String): Account? = api.findUser(username, token)

    suspend fun addTeamMember(teamId: String, userId: String, token: String) = api.addTeamMember(teamId, userId, token)

    suspend fun updateTeamMember(teamId: String, userId: String, update: ProjectMemberUpdate, token: String) =
        api.updateTeamMember(teamId, userId, update, token)

    suspend fun deleteTeamMember(teamId: String, userId: String, token: String) = api.deleteTeamMember(teamId, userId, token)

    suspend fun joinTeam(teamId: String, token: String) = api.joinTeam(teamId, token)

    suspend fun transferTeamOwnership(teamId: String, userId: String, token: String) =
        api.transferTeamOwnership(teamId, userId, token)

    suspend fun changeProjectIcon(projectIdOrSlug: String, file: ProjectFileUpload, token: String) =
        api.changeProjectIcon(projectIdOrSlug, file, token)

    suspend fun deleteProjectIcon(projectIdOrSlug: String, token: String) = api.deleteProjectIcon(projectIdOrSlug, token)

    suspend fun addGalleryImage(
        projectIdOrSlug: String,
        file: ProjectFileUpload,
        featured: Boolean,
        title: String,
        description: String,
        token: String,
    ) = api.addGalleryImage(projectIdOrSlug, file, featured, title, description, token)

    suspend fun deleteGalleryImage(projectIdOrSlug: String, imageUrl: String, token: String) =
        api.deleteGalleryImage(projectIdOrSlug, imageUrl, token)

    suspend fun modifyGalleryImage(
        projectIdOrSlug: String,
        imageUrl: String,
        featured: Boolean? = null,
        title: String? = null,
        description: String? = null,
        ordering: Int? = null,
        token: String,
    ) = api.modifyGalleryImage(
        projectIdOrSlug = projectIdOrSlug,
        imageUrl = imageUrl,
        featured = featured,
        title = title,
        description = description,
        ordering = ordering,
        token = token,
    )

    suspend fun loadOrganizationProjects(organizationIdOrSlug: String, token: String): List<Project> =
        api.getOrganizationProjects(organizationIdOrSlug, token)

    /**
     * Loads full organization detail: prefer v3 org payload (with members), then projects,
     * then fall back to dedicated members endpoint / team members.
     *
     * Project sources (merged, de-duplicated):
     * 1. GET `/v3/organization/{slug|id}/projects` (primary — only real org projects route)
     * 2. [knownProjects] already on the dashboard (instant / offline-friendly seed)
     * 3. Fresh user portfolio, filtered by organization id / slug / name
     */
    suspend fun loadOrganizationDetail(
        organization: Organization,
        token: String,
        knownProjects: List<Project> = emptyList(),
    ): OrganizationDetail = coroutineScope {
        val slugKey = organization.slug.trim()
        val idKey = organization.id.trim()
        val lookupKeys = listOf(slugKey, idKey).filter { it.isNotEmpty() }.distinct()

        val detailDeferred = async {
            var loaded: Organization? = null
            for (key in lookupKeys) {
                loaded = runCatching { api.getOrganization(key, token) }.getOrNull()
                if (loaded != null) break
            }
            loaded ?: organization
        }
        val projectsByRouteDeferred = async {
            var authError: Exception? = null
            var found: List<Project> = emptyList()
            for (key in lookupKeys) {
                try {
                    val list = api.getOrganizationProjects(key, token)
                    if (list.isNotEmpty()) {
                        found = list
                        break
                    }
                    // Successful empty — keep trying alternate key (slug vs id).
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // 401/403 are meaningful; remember and rethrow if every key fails.
                    if (error is ApiException && (error.statusCode == 401 || error.statusCode == 403)) {
                        authError = error
                    }
                }
            }
            if (found.isEmpty() && authError != null) throw authError
            found
        }
        val userProjectsDeferred = async {
            runCatching {
                val userId = api.getCurrentAccount(token).id
                api.getProjects(userId, token)
            }.getOrDefault(emptyList())
        }

        val detail = detailDeferred.await()
        val fromRoute = projectsByRouteDeferred.await()
        val orgMatchers = organizationMatchers(detail, organization)
        val fromKnown = knownProjects.filter { it.belongsToOrganization(orgMatchers) }
        val fromPortfolio = userProjectsDeferred.await().filter { it.belongsToOrganization(orgMatchers) }

        val projects = (fromRoute + fromKnown + fromPortfolio)
            .distinctBy { it.id }
            .sortedByDescending { it.downloads }

        val members = detail.members.ifEmpty {
            var loaded = emptyList<ProjectMember>()
            for (key in lookupKeys) {
                loaded = runCatching {
                    api.getOrganizationMembers(key, detail.teamId ?: organization.teamId, token)
                }.getOrDefault(emptyList())
                if (loaded.isNotEmpty()) break
            }
            loaded
        }
        OrganizationDetail(
            organization = detail.copy(members = members),
            projects = projects,
            members = members.sortedWith(
                compareByDescending<ProjectMember> { it.isOwner }
                    .thenBy { it.ordering }
                    .thenBy { it.user.username.lowercase() },
            ),
        )
    }

    /** id / slug / display name keys used to match [Project.organization]. */
    private fun organizationMatchers(detail: Organization, fallback: Organization): Set<String> =
        setOf(
            detail.id,
            detail.slug,
            detail.name,
            fallback.id,
            fallback.slug,
            fallback.name,
        )
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun Project.belongsToOrganization(matchers: Set<String>): Boolean {
        val org = organization?.trim()?.lowercase() ?: return false
        return org in matchers
    }

    fun close() = api.close()
}

data class OrganizationDetail(
    val organization: Organization,
    val projects: List<Project>,
    val members: List<ProjectMember>,
)

private fun nullableSum(first: Double?, second: Double?): Double? =
    if (first == null && second == null) null else (first ?: 0.0) + (second ?: 0.0)

private const val analyticsProjectBatchSize = 40

private fun AnalyticsQuery.batchedByProjectIds(): List<AnalyticsQuery> =
    projectIds
        .chunked(analyticsProjectBatchSize)
        .ifEmpty { listOf(emptyList()) }
        .map { projectBatch -> copy(projectIds = projectBatch) }

private fun List<com.ryntra.shared.network.AnalyticsResponse>.merge(): com.ryntra.shared.network.AnalyticsResponse {
    if (isEmpty()) return com.ryntra.shared.network.AnalyticsResponse(status = 200)
    return com.ryntra.shared.network.AnalyticsResponse(
        status = firstFailedStatusOrSuccess(),
        points = mergeAnalyticsPoints(map { it.points }),
        events = flatMap { it.events }.distinctBy { event ->
            listOf(
                event.projectId,
                event.timestamp,
                event.kind,
                event.versionId,
                event.statusFrom,
                event.statusTo,
            ).joinToString("|")
        }.sortedBy { it.timestamp },
    )
}

private fun List<com.ryntra.shared.network.AnalyticsResponse>.firstFailedStatusOrSuccess(): Int =
    firstOrNull { it.status !in 200..299 }?.status ?: firstOrNull()?.status ?: 200

private fun mergeAnalyticsPoints(
    groups: List<List<com.ryntra.shared.model.AnalyticsPoint>>,
): List<com.ryntra.shared.model.AnalyticsPoint> {
    val maxSize = groups.maxOfOrNull { it.size } ?: return emptyList()
    return List(maxSize) { index ->
        val pointsAtIndex = groups.mapNotNull { it.getOrNull(index) }
        val mergedProjects = mutableMapOf<String, com.ryntra.shared.model.AnalyticsMetrics>()
        pointsAtIndex.forEach { point ->
            point.projects.forEach { (projectId, metrics) ->
                mergedProjects[projectId] =
                    mergedProjects.getOrElse(projectId) { com.ryntra.shared.model.AnalyticsMetrics() } + metrics
            }
        }
        com.ryntra.shared.model.AnalyticsPoint(
            startTime = pointsAtIndex.firstOrNull()?.startTime.orEmpty(),
            metrics = pointsAtIndex.fold(com.ryntra.shared.model.AnalyticsMetrics()) { total, point ->
                total + point.metrics
            },
            projects = mergedProjects,
        )
    }
}
