package com.ryntra.shared.app

import com.ryntra.shared.data.DashboardRepository
import com.ryntra.shared.data.OrganizationDetail
import com.ryntra.shared.model.AccountProfileUpdate
import com.ryntra.shared.model.Account
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.AnalyticsReport
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModerationThread
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectUploadLimits
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectTeamRoster
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.model.WalletReport
import com.ryntra.shared.network.ModrinthApi
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.network.createPlatformHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppController internal constructor(
    private val repository: DashboardRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<AppState>(AppState.SignedOut)
    private var accessToken: String? = null
    private var loadJob: Job? = null

    val state: StateFlow<AppState> = mutableState.asStateFlow()

    constructor() : this(
        DashboardRepository(ModrinthApi(createPlatformHttpClient())),
    )

    fun signIn(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) {
            mutableState.value = AppState.Failed("Enter a Modrinth access token.")
            return
        }
        accessToken = normalizedToken
        load(previousDashboard = null)
    }

    fun refresh() {
        val dashboard = currentDashboard() ?: return
        load(previousDashboard = dashboard)
    }

    suspend fun loadProjectDetails(projectIdOrSlug: String): Project {
        val token = accessToken ?: error("Sign in before loading project details.")
        return repository.loadProject(projectIdOrSlug, token)
    }

    suspend fun loadProjectVersions(projectIdOrSlug: String): List<ProjectVersion> {
        val token = accessToken ?: error("Sign in before loading project versions.")
        return repository.loadProjectVersions(projectIdOrSlug, token)
    }

    suspend fun loadAnalytics(query: AnalyticsQuery): AnalyticsReport {
        val token = requireToken("loading analytics")
        return repository.loadAnalytics(query, token)
    }

    suspend fun loadWallet(): WalletReport {
        val token = requireToken("loading wallet")
        val account = currentDashboard()?.account ?: error("Load the dashboard before loading wallet data.")
        return repository.loadWallet(account, token)
    }

    suspend fun loadNotifications(): List<ModrinthNotification> {
        val token = requireToken("loading notifications")
        val account = currentDashboard()?.account ?: error("Load the dashboard before loading notifications.")
        return repository.loadNotifications(account.id, token)
    }

    suspend fun markNotificationsRead(notificationIds: List<String>) {
        val token = requireToken("updating notifications")
        repository.markNotificationsRead(notificationIds, token)
    }

    suspend fun acceptNotificationInvitation(notification: ModrinthNotification) {
        val teamId = notification.actions.firstNotNullOfOrNull { it.teamJoinId }
            ?: throw IllegalArgumentException("This notification does not contain a supported invitation action.")
        repository.joinTeam(teamId, requireToken("accepting an invitation"))
        repository.markNotificationsRead(listOf(notification.id), requireToken("updating notifications"))
    }

    fun updateCachedProject(project: Project) {
        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(current.dashboard.withFreshProject(project))
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.withFreshProject(project),
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.withFreshProject(project),
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    suspend fun loadModerationThread(threadId: String): ModerationThread =
        repository.loadModerationThread(threadId, requireToken("loading project moderation"))

    suspend fun replyToModerationThread(threadId: String, body: String, replyingTo: String?) {
        val normalizedBody = body.trim()
        require(normalizedBody.isNotEmpty()) { "Write a reply before sending it." }
        require(normalizedBody.length <= MAX_MODERATION_REPLY_LENGTH) {
            "Moderation replies must be $MAX_MODERATION_REPLY_LENGTH characters or fewer."
        }
        repository.replyToModerationThread(
            threadId = threadId,
            body = normalizedBody,
            replyingTo = replyingTo,
            token = requireToken("replying to project moderation"),
        )
    }

    suspend fun deleteModerationMessage(messageId: String) {
        repository.deleteModerationMessage(messageId, requireToken("deleting a moderation reply"))
    }

    suspend fun loadProjectDependencies(versions: List<ProjectVersion>): List<ProjectDependency> {
        val token = requireToken("loading project dependencies")
        val latestDependencies = versions.maxByOrNull { it.datePublished.orEmpty() }?.dependencies.orEmpty()
        return repository.enrichDependencies(latestDependencies, token)
    }

    suspend fun loadProjectMembers(projectIdOrSlug: String, teamId: String?): List<ProjectMember> {
        val token = accessToken ?: error("Sign in before loading project members.")
        return repository.loadProjectMembers(projectIdOrSlug, teamId, token)
    }

    suspend fun loadProjectTeamRoster(project: Project): ProjectTeamRoster {
        val token = accessToken ?: error("Sign in before loading project members.")
        return repository.loadProjectTeamRoster(project, token)
    }

    suspend fun loadOrganizationMembers(organizationIdOrSlug: String, teamId: String?): List<ProjectMember> {
        val token = accessToken ?: error("Sign in before loading organization members.")
        return repository.loadOrganizationMembers(organizationIdOrSlug, teamId, token)
    }

    suspend fun loadOrganizationProjects(organizationIdOrSlug: String): List<Project> {
        val token = accessToken ?: error("Sign in before loading organization projects.")
        return repository.loadOrganizationProjects(organizationIdOrSlug, token)
    }

    suspend fun loadOrganizationDetail(organization: Organization): OrganizationDetail {
        val token = accessToken ?: error("Sign in before loading organization details.")
        val knownProjects = when (val current = mutableState.value) {
            is AppState.Ready -> current.dashboard.projects
            is AppState.Loading -> current.previousDashboard?.projects.orEmpty()
            is AppState.Failed -> current.previousDashboard?.projects.orEmpty()
            AppState.SignedOut -> emptyList()
        }
        return repository.loadOrganizationDetail(
            organization = organization,
            token = token,
            knownProjects = knownProjects,
        )
    }

    suspend fun updateAccountProfile(userId: String, username: String, bio: String) {
        val token = accessToken ?: error("Sign in before updating your profile.")
        val normalizedUsername = username.trim()
        require(normalizedUsername.isNotEmpty()) { "Username cannot be empty." }
        val normalizedBio = bio.trim()

        repository.updateAccountProfile(
            userId = userId,
            update = AccountProfileUpdate(username = normalizedUsername, bio = normalizedBio),
            token = token,
        )
        if (accessToken != token) return

        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(current.dashboard.withUpdatedAccount(normalizedUsername, normalizedBio))
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.withUpdatedAccount(normalizedUsername, normalizedBio),
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.withUpdatedAccount(normalizedUsername, normalizedBio),
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    suspend fun changeUserAvatar(userId: String, file: ProjectFileUpload) {
        require(file.bytes.size <= ProjectUploadLimits.USER_AVATAR_BYTES) {
            "Avatar images must be 2 MiB or smaller."
        }
        val token = requireToken("updating your avatar")
        repository.changeUserAvatar(userId, file, token)
        if (accessToken != token) return
        // Refresh account so avatar_url points at the new CDN asset.
        val account = repository.loadCurrentAccount(token)
        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(current.dashboard.copy(account = account))
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.copy(account = account),
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.copy(account = account),
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    suspend fun deleteUserAvatar(userId: String) {
        val token = requireToken("removing your avatar")
        repository.deleteUserAvatar(userId, token)
        if (accessToken != token) return
        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(
                current.dashboard.copy(account = current.dashboard.account.copy(avatarUrl = null)),
            )
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.let {
                    it.copy(account = it.account.copy(avatarUrl = null))
                },
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.let {
                    it.copy(account = it.account.copy(avatarUrl = null))
                },
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    fun signOut() {
        loadJob?.cancel()
        accessToken = null
        mutableState.value = AppState.SignedOut
    }

    suspend fun updateProject(projectIdOrSlug: String, update: com.ryntra.shared.model.ProjectUpdate) {
        val token = accessToken ?: error("Sign in before updating a project.")
        repository.updateProject(projectIdOrSlug, update, token)
        if (accessToken != token) return

        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(current.dashboard.withUpdatedProject(projectIdOrSlug, update))
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.withUpdatedProject(projectIdOrSlug, update),
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.withUpdatedProject(projectIdOrSlug, update),
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    suspend fun submitProjectForModeration(projectIdOrSlug: String) {
        updateProject(
            projectIdOrSlug,
            com.ryntra.shared.model.ProjectUpdate(status = "processing"),
        )
    }

    suspend fun deleteProject(projectIdOrSlug: String) {
        val token = requireToken("deleting a project")
        repository.deleteProject(projectIdOrSlug, token)
        if (accessToken != token) return

        mutableState.value = when (val current = mutableState.value) {
            is AppState.Ready -> AppState.Ready(current.dashboard.withoutProject(projectIdOrSlug))
            is AppState.Loading -> current.copy(
                previousDashboard = current.previousDashboard?.withoutProject(projectIdOrSlug),
            )
            is AppState.Failed -> current.copy(
                previousDashboard = current.previousDashboard?.withoutProject(projectIdOrSlug),
            )
            AppState.SignedOut -> AppState.SignedOut
        }
    }

    suspend fun loadProjectCreationMetadata(): ProjectCreationMetadata = repository.loadProjectCreationMetadata()

    suspend fun createProject(request: CreateProjectRequest): Project {
        val token = requireToken("creating a project")
        val project = try {
            repository.createProject(request, token)
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.statusCode == 403) {
                throw IllegalStateException(
                    "Your Modrinth token cannot create projects. Sign in again and grant the PROJECT_CREATE permission.",
                    error,
                )
            }
            throw error
        }
        if (accessToken == token) {
            mutableState.value = when (val current = mutableState.value) {
                is AppState.Ready -> AppState.Ready(current.dashboard.copy(projects = listOf(project) + current.dashboard.projects))
                is AppState.Loading -> current.copy(previousDashboard = current.previousDashboard?.let {
                    it.copy(projects = listOf(project) + it.projects)
                })
                is AppState.Failed -> current.copy(previousDashboard = current.previousDashboard?.let {
                    it.copy(projects = listOf(project) + it.projects)
                })
                AppState.SignedOut -> AppState.SignedOut
            }
        }
        return project
    }

    suspend fun changeProjectIcon(projectIdOrSlug: String, file: ProjectFileUpload) {
        require(file.bytes.isNotEmpty()) { "Select an image to upload." }
        require(file.bytes.size <= ProjectUploadLimits.PROJECT_ICON_BYTES) {
            "Project icons must be 256 KiB or smaller."
        }
        repository.changeProjectIcon(projectIdOrSlug, file, requireToken("changing a project icon"))
    }

    suspend fun deleteProjectIcon(projectIdOrSlug: String) {
        repository.deleteProjectIcon(projectIdOrSlug, requireToken("deleting a project icon"))
    }

    suspend fun addGalleryImage(
        projectIdOrSlug: String,
        file: ProjectFileUpload,
        featured: Boolean = false,
        title: String = "Gallery image",
        description: String = "",
    ) {
        require(file.bytes.isNotEmpty()) { "Select an image to upload." }
        require(file.contentType.startsWith("image/")) { "Gallery uploads must be images." }
        require(file.bytes.size <= ProjectUploadLimits.GALLERY_IMAGE_BYTES) {
            "Gallery images must be 5 MiB or smaller."
        }
        repository.addGalleryImage(
            projectIdOrSlug,
            file,
            featured,
            title.trim(),
            description.trim(),
            requireToken("adding a gallery image"),
        )
    }

    suspend fun deleteGalleryImage(projectIdOrSlug: String, imageUrl: String) {
        repository.deleteGalleryImage(projectIdOrSlug, imageUrl, requireToken("deleting a gallery image"))
    }

    suspend fun modifyGalleryImage(
        projectIdOrSlug: String,
        imageUrl: String,
        featured: Boolean? = null,
        title: String? = null,
        description: String? = null,
        ordering: Int? = null,
    ) {
        repository.modifyGalleryImage(
            projectIdOrSlug = projectIdOrSlug,
            imageUrl = imageUrl,
            featured = featured,
            title = title,
            description = description,
            ordering = ordering,
            token = requireToken("updating a gallery image"),
        )
    }

    suspend fun setGalleryImageAsBanner(projectIdOrSlug: String, imageUrl: String) {
        modifyGalleryImage(projectIdOrSlug = projectIdOrSlug, imageUrl = imageUrl, featured = true)
    }

    suspend fun createVersion(projectId: String, request: CreateVersionRequest): ProjectVersion {
        require(request.files.sumOf { it.bytes.size.toLong() } <= ProjectUploadLimits.VERSION_FILES_BYTES) {
            "Version files must be 128 MiB or smaller in total."
        }
        return repository.createVersion(projectId, request, requireToken("creating a version"))
    }

    suspend fun updateVersion(versionId: String, update: VersionUpdate) {
        repository.updateVersion(versionId, update, requireToken("updating a version"))
    }

    suspend fun deleteVersion(versionId: String) {
        repository.deleteVersion(versionId, requireToken("deleting a version"))
    }

    suspend fun findUser(username: String): Account? = repository.findUser(username, requireToken("searching for a user"))

    suspend fun addTeamMember(teamId: String, userId: String) {
        repository.addTeamMember(teamId, userId, requireToken("inviting a team member"))
    }

    suspend fun updateTeamMember(teamId: String, userId: String, update: ProjectMemberUpdate) {
        repository.updateTeamMember(teamId, userId, update, requireToken("updating a team member"))
    }

    suspend fun deleteTeamMember(teamId: String, userId: String) {
        repository.deleteTeamMember(teamId, userId, requireToken("removing a team member"))
    }

    suspend fun joinTeam(teamId: String) {
        repository.joinTeam(teamId, requireToken("joining a team"))
    }

    suspend fun transferTeamOwnership(teamId: String, userId: String) {
        repository.transferTeamOwnership(teamId, userId, requireToken("transferring team ownership"))
    }

    fun observe(listener: (AppState) -> Unit): Observation {
        val job = scope.launch {
            state.collect(listener)
        }
        return Observation(job)
    }

    fun close() {
        loadJob?.cancel()
        scope.cancel()
        repository.close()
    }

    private fun load(previousDashboard: Dashboard?) {
        val token = accessToken ?: return
        loadJob?.cancel()
        mutableState.value = AppState.Loading(previousDashboard)
        loadJob = scope.launch {
            mutableState.value = try {
                AppState.Ready(repository.load(token))
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                AppState.Failed(
                    message = error.message,
                    previousDashboard = previousDashboard,
                    isAuthenticationFailure = error.statusCode == 401,
                )
            } catch (error: Exception) {
                AppState.Failed(
                    message = error.message ?: "Unable to reach Modrinth.",
                    previousDashboard = previousDashboard,
                )
            }
        }
    }

    private fun currentDashboard() = when (val current = state.value) {
        is AppState.Ready -> current.dashboard
        is AppState.Loading -> current.previousDashboard
        is AppState.Failed -> current.previousDashboard
        AppState.SignedOut -> null
    }

    private fun requireToken(action: String): String = accessToken ?: error("Sign in before $action.")

    private companion object {
        const val MAX_MODERATION_REPLY_LENGTH = 10_000
    }

    private fun Dashboard.withUpdatedAccount(username: String, bio: String): Dashboard =
        copy(account = account.copy(username = username, bio = bio.ifEmpty { null }))

    private fun Dashboard.withoutProject(projectIdOrSlug: String): Dashboard =
        copy(projects = projects.filterNot { project ->
            project.id == projectIdOrSlug || project.slug == projectIdOrSlug
        })

}

internal fun Dashboard.withUpdatedProject(
    projectIdOrSlug: String,
    update: com.ryntra.shared.model.ProjectUpdate,
): Dashboard = copy(projects = projects.map { project ->
    if (project.id == projectIdOrSlug || project.slug == projectIdOrSlug) {
        project.copy(
            title = update.title ?: project.title,
            description = update.description ?: project.description,
            body = update.body ?: project.body,
            sourceUrl = update.sourceUrl?.ifBlank { null } ?: if (update.sourceUrl != null) null else project.sourceUrl,
            issuesUrl = update.issuesUrl?.ifBlank { null } ?: if (update.issuesUrl != null) null else project.issuesUrl,
            wikiUrl = update.wikiUrl?.ifBlank { null } ?: if (update.wikiUrl != null) null else project.wikiUrl,
            discordUrl = update.discordUrl?.ifBlank { null } ?: if (update.discordUrl != null) null else project.discordUrl,
            status = update.status ?: project.status,
            requestedStatus = when {
                update.requestedStatus != null -> update.requestedStatus.ifBlank { null }
                update.status != null -> null
                else -> project.requestedStatus
            },
            license = when {
                update.licenseId != null -> com.ryntra.shared.model.ProjectLicense(
                    id = update.licenseId,
                    url = update.licenseUrl?.ifBlank { null },
                )
                update.licenseUrl != null -> project.license?.copy(url = update.licenseUrl.ifBlank { null })
                else -> project.license
            },
        )
    } else {
        project
    }
})

internal fun Dashboard.withFreshProject(project: Project): Dashboard = copy(
    projects = projects.map { cached ->
        if (cached.id == project.id || cached.slug != null && cached.slug == project.slug) project else cached
    },
)

class Observation internal constructor(
    private val job: Job,
) {
    fun cancel() = job.cancel()
}
