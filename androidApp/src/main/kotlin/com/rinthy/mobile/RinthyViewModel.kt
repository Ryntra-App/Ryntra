package com.rinthy.mobile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinthy.mobile.auth.OAuthCallbackResult
import com.rinthy.mobile.auth.OAuthCoordinator
import com.rinthy.mobile.preferences.AppLanguage
import com.rinthy.mobile.preferences.AppLocale
import com.rinthy.mobile.preferences.GlassQuality
import com.rinthy.mobile.preferences.AppearanceMode
import com.rinthy.mobile.preferences.RinthyPreferences
import com.rinthy.mobile.preferences.RinthyPreferencesStore
import com.rinthy.mobile.preferences.ThemeStyle
import com.rinthy.mobile.security.SecureTokenStore
import com.rinthy.shared.app.AppController
import com.rinthy.shared.app.AppState
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.Account
import com.rinthy.shared.model.AnalyticsQuery
import com.rinthy.shared.model.AnalyticsReport
import com.rinthy.shared.model.CreateVersionRequest
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectDependency
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate
import com.rinthy.shared.model.ProjectSortMode
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.model.VersionUpdate
import com.rinthy.shared.model.WalletReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class RinthyViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = SecureTokenStore(application)
    private val preferencesStore = RinthyPreferencesStore(application)
    private val oauthCoordinator = OAuthCoordinator(application)
    private val controller = AppController()
    private val mutableOAuthError = MutableStateFlow<String?>(null)
    private val mutableProjectDetail = MutableStateFlow<ProjectDetailState?>(null)
    private val mutableOrganizationDetail = MutableStateFlow<OrganizationDetailState?>(null)
    private val mutableProfileUpdate = MutableStateFlow(ProfileUpdateState())
    private val mutableProjectUpdate = MutableStateFlow(ProjectUpdateState())
    private val mutableProjectAction = MutableStateFlow(ProjectActionState())
    private val mutableMemberSearch = MutableStateFlow(MemberSearchState())
    private val mutableAnalytics = MutableStateFlow(AnalyticsState())
    private var pendingToken: String? = null
    private var projectLoadJob: Job? = null
    private var organizationLoadJob: Job? = null
    private var projectActionJob: Job? = null
    private var memberSearchJob: Job? = null
    private var analyticsJob: Job? = null

    val state: StateFlow<AppState> = controller.state
    val oauthError: StateFlow<String?> = mutableOAuthError.asStateFlow()
    val projectDetail: StateFlow<ProjectDetailState?> = mutableProjectDetail.asStateFlow()
    val organizationDetail: StateFlow<OrganizationDetailState?> = mutableOrganizationDetail.asStateFlow()
    val profileUpdate: StateFlow<ProfileUpdateState> = mutableProfileUpdate.asStateFlow()
    val projectUpdate: StateFlow<ProjectUpdateState> = mutableProjectUpdate.asStateFlow()
    val projectAction: StateFlow<ProjectActionState> = mutableProjectAction.asStateFlow()
    val memberSearch: StateFlow<MemberSearchState> = mutableMemberSearch.asStateFlow()
    val analytics: StateFlow<AnalyticsState> = mutableAnalytics.asStateFlow()
    val preferences: StateFlow<RinthyPreferences> = preferencesStore.preferences

    init {
        tokenStore.read()?.let { savedToken ->
            pendingToken = savedToken
            controller.signIn(savedToken)
        }
        viewModelScope.launch {
            state.collect { currentState ->
                if (currentState is AppState.Ready) {
                    pendingToken?.let(tokenStore::write)
                    pendingToken = null
                } else if (currentState is AppState.Failed && currentState.isAuthenticationFailure) {
                    tokenStore.clear()
                    pendingToken = null
                }
            }
        }
    }

    fun signIn(token: String) {
        mutableOAuthError.value = null
        pendingToken = token.trim()
        controller.signIn(token)
    }

    fun startOAuth(): Uri {
        mutableOAuthError.value = null
        return oauthCoordinator.createAuthorizationUri()
    }

    fun handleOAuthCallback(uri: Uri) {
        when (val result = oauthCoordinator.consumeCallback(uri)) {
            OAuthCallbackResult.Ignored -> Unit
            is OAuthCallbackResult.Failure -> mutableOAuthError.value = result.message
            is OAuthCallbackResult.Success -> signIn(result.token)
        }
    }

    fun refresh() = controller.refresh()

    fun setShowFavoriteProjects(isEnabled: Boolean) = preferencesStore.setShowFavoriteProjects(isEnabled)

    fun setThemeStyle(style: ThemeStyle) = preferencesStore.setThemeStyle(style)

    fun setAppearanceMode(mode: AppearanceMode) = preferencesStore.setAppearanceMode(mode)

    fun setAppLanguage(language: AppLanguage) {
        preferencesStore.setAppLanguage(language)
        AppLocale.apply(language)
    }

    fun setReduceMotion(isEnabled: Boolean) = preferencesStore.setReduceMotion(isEnabled)

    fun setGlassQuality(quality: GlassQuality) = preferencesStore.setGlassQuality(quality)

    fun setProjectSortMode(mode: ProjectSortMode) = preferencesStore.setProjectSortMode(mode)

    fun toggleFavoriteProject(projectId: String) = preferencesStore.toggleFavoriteProject(projectId)

    fun resetAppearance() = preferencesStore.resetAppearance()

    fun exportPreferences(username: String): String =
        preferencesStore.exportJson(username = username, appVersion = BuildConfig.VERSION_NAME)

    fun importPreferences(rawJson: String): Result<Unit> =
        preferencesStore.importJson(rawJson).onSuccess {
            AppLocale.apply(preferencesStore.preferences.value.appLanguage)
        }

    fun loadAnalytics(rangeDays: Int, force: Boolean = false) {
        require(rangeDays in setOf(7, 30, 90, 180)) { "Analytics range must be 7, 30, 90, or 180 days." }
        val dashboard = when (val current = state.value) {
            is AppState.Ready -> current.dashboard
            is AppState.Loading -> current.previousDashboard
            is AppState.Failed -> current.previousDashboard
            AppState.SignedOut -> null
        } ?: return
        val projectIds = dashboard.projects.map(Project::id)
        if (
            !force &&
            mutableAnalytics.value.rangeDays == rangeDays &&
            mutableAnalytics.value.projectIds == projectIds &&
            mutableAnalytics.value.report != null &&
            mutableAnalytics.value.wallet != null
        ) return

        analyticsJob?.cancel()
        val end = Instant.now()
        val currentStart = end.minus(rangeDays.toLong(), ChronoUnit.DAYS)
        val query = AnalyticsQuery(
            startTime = end.minus(rangeDays.toLong() * 2, ChronoUnit.DAYS).toString(),
            endTime = end.toString(),
            slices = rangeDays * 2,
            projectIds = projectIds,
            currentStartTime = currentStart.toString(),
            currentSlices = rangeDays,
        )
        mutableAnalytics.value = mutableAnalytics.value.copy(
            rangeDays = rangeDays,
            isLoading = true,
            errorMessage = null,
        )
        analyticsJob = viewModelScope.launch {
            val analyticsRequest = async {
                if (projectIds.isEmpty()) {
                    Result.success(AnalyticsReport(coreStatus = 200, revenueStatus = 200))
                } else {
                    suspendCatching { controller.loadAnalytics(query) }
                }
            }
            val existingWallet = mutableAnalytics.value.wallet
            val walletRequest = async {
                if (existingWallet != null && !force) Result.success(existingWallet)
                else suspendCatching { controller.loadWallet() }
            }
            val analyticsResult = analyticsRequest.await()
            val walletResult = walletRequest.await()
            mutableAnalytics.value = AnalyticsState(
                rangeDays = rangeDays,
                projectIds = projectIds,
                report = analyticsResult.getOrNull(),
                wallet = walletResult.getOrNull(),
                errorMessage = analyticsResult.exceptionOrNull()?.message ?: if (analyticsResult.isFailure) {
                    "Unable to load analytics."
                } else {
                    null
                },
                walletErrorMessage = walletResult.exceptionOrNull()?.message ?: if (walletResult.isFailure) {
                    "Unable to load wallet data."
                } else {
                    null
                },
            )
        }
    }

    fun openProject(project: Project) {
        projectLoadJob?.cancel()
        mutableProjectDetail.value = ProjectDetailState(project = project, isLoading = true)
        projectLoadJob = viewModelScope.launch {
            val projectKey = project.slug ?: project.id
            val detailsDeferred = async { suspendCatching { controller.loadProjectDetails(projectKey) } }
            val versionsDeferred = async { suspendCatching { controller.loadProjectVersions(projectKey) } }
            val details = detailsDeferred.await()
            val versions = versionsDeferred.await()
            val loadedProject = details.getOrElse { project }
            // Load roster after details so team_id / organization from the full project are available.
            val roster = suspendCatching { controller.loadProjectTeamRoster(loadedProject) }
            val dependencies = versions.getOrNull()?.let { loadedVersions ->
                suspendCatching { controller.loadProjectDependencies(loadedVersions) }.getOrDefault(emptyList())
            }.orEmpty()
            val current = mutableProjectDetail.value
            if (current?.project?.id != project.id) return@launch

            val teamRoster = roster.getOrNull()
            mutableProjectDetail.value = ProjectDetailState(
                project = loadedProject,
                versions = versions.getOrDefault(emptyList()),
                dependencies = dependencies,
                members = teamRoster?.projectMembers.orEmpty(),
                organizationMembers = teamRoster?.organizationMembers.orEmpty(),
                organizationName = teamRoster?.organization?.name,
                isLoading = false,
                errorMessage = details.exceptionOrNull()?.message ?: versions.exceptionOrNull()?.message,
                memberErrorMessage = roster.exceptionOrNull()?.message,
            )
        }
    }

    fun closeProject() {
        projectLoadJob?.cancel()
        projectLoadJob = null
        mutableProjectDetail.value = null
        mutableProjectAction.value = ProjectActionState()
        mutableMemberSearch.value = MemberSearchState()
    }

    fun openOrganization(organization: Organization) {
        organizationLoadJob?.cancel()
        // Seed from the already-loaded dashboard portfolio so the list is never blank
        // while the dedicated org projects route is in flight.
        val seededProjects = organizationProjectsFromDashboard(organization)
        mutableOrganizationDetail.value = OrganizationDetailState(
            organization = organization,
            projects = seededProjects,
            members = organization.members,
            isLoading = true,
        )
        organizationLoadJob = viewModelScope.launch {
            val detail = suspendCatching { controller.loadOrganizationDetail(organization) }
            val current = mutableOrganizationDetail.value
            if (current?.organization?.id != organization.id) return@launch

            detail.fold(
                onSuccess = { loaded ->
                    mutableOrganizationDetail.value = OrganizationDetailState(
                        organization = loaded.organization,
                        projects = loaded.projects.ifEmpty { seededProjects },
                        members = loaded.members.ifEmpty { organization.members },
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    mutableOrganizationDetail.value = OrganizationDetailState(
                        organization = organization,
                        projects = seededProjects,
                        members = organization.members,
                        isLoading = false,
                        // Only surface the error when we truly have nothing to show.
                        errorMessage = if (seededProjects.isEmpty()) error.message else null,
                    )
                },
            )
        }
    }

    private fun organizationProjectsFromDashboard(organization: Organization): List<Project> {
        val dashboard = when (val current = state.value) {
            is AppState.Ready -> current.dashboard
            is AppState.Loading -> current.previousDashboard
            is AppState.Failed -> current.previousDashboard
            AppState.SignedOut -> null
        } ?: return emptyList()
        val matchers = setOf(organization.id, organization.slug, organization.name)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        return dashboard.projects
            .filter { project ->
                project.organization?.trim()?.lowercase() in matchers
            }
            .sortedByDescending { it.downloads }
    }

    fun closeOrganization() {
        organizationLoadJob?.cancel()
        organizationLoadJob = null
        mutableOrganizationDetail.value = null
    }

    fun changeAvatar(userId: String, file: ProjectFileUpload) {
        if (file.bytes.size > MAX_USER_AVATAR_BYTES) {
            mutableProfileUpdate.value = ProfileUpdateState(
                errorMessage = "Avatar images must be 2 MiB or smaller.",
            )
            return
        }
        if (!file.contentType.startsWith("image/")) {
            mutableProfileUpdate.value = ProfileUpdateState(
                errorMessage = "Only image files can be used as an avatar.",
            )
            return
        }
        mutableProfileUpdate.value = ProfileUpdateState(isSaving = true)
        viewModelScope.launch {
            suspendCatching { controller.changeUserAvatar(userId, file) }.fold(
                onSuccess = { mutableProfileUpdate.value = ProfileUpdateState(isSuccess = true) },
                onFailure = { error ->
                    mutableProfileUpdate.value = ProfileUpdateState(
                        errorMessage = error.message ?: "Unable to update avatar.",
                    )
                },
            )
        }
    }

    fun deleteAvatar(userId: String) {
        mutableProfileUpdate.value = ProfileUpdateState(isSaving = true)
        viewModelScope.launch {
            suspendCatching { controller.deleteUserAvatar(userId) }.fold(
                onSuccess = { mutableProfileUpdate.value = ProfileUpdateState(isSuccess = true) },
                onFailure = { error ->
                    mutableProfileUpdate.value = ProfileUpdateState(
                        errorMessage = error.message ?: "Unable to remove avatar.",
                    )
                },
            )
        }
    }

    fun updateProfile(userId: String, username: String, bio: String) {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isEmpty()) {
            mutableProfileUpdate.value = ProfileUpdateState(errorMessage = "Username cannot be empty.")
            return
        }
        mutableProfileUpdate.value = ProfileUpdateState(isSaving = true)
        viewModelScope.launch {
            suspendCatching {
                controller.updateAccountProfile(
                    userId = userId,
                    username = normalizedUsername,
                    bio = bio,
                )
            }.fold(
                onSuccess = { mutableProfileUpdate.value = ProfileUpdateState() },
                onFailure = { error ->
                    mutableProfileUpdate.value = ProfileUpdateState(
                        errorMessage = error.message ?: "Unable to update profile.",
                    )
                },
            )
        }
    }

    fun updateProject(projectId: String, update: com.rinthy.shared.model.ProjectUpdate) {
        mutableProjectUpdate.value = ProjectUpdateState(isSaving = true)
        viewModelScope.launch {
            suspendCatching {
                controller.updateProject(projectId, update)
            }.fold(
                onSuccess = {
                    mutableProjectUpdate.value = ProjectUpdateState(isSuccess = true)
                    refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
                },
                onFailure = { error ->
                    mutableProjectUpdate.value = ProjectUpdateState(
                        errorMessage = error.message ?: "Unable to update project.",
                    )
                },
            )
        }
    }

    fun clearProjectUpdateStatus() {
        mutableProjectUpdate.value = ProjectUpdateState()
    }

    fun changeProjectIcon(projectId: String, file: ProjectFileUpload) = runProjectAction(projectId, "Icon updated") {
        controller.changeProjectIcon(projectId, file)
        refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
    }

    fun deleteProjectIcon(projectId: String) = runProjectAction(projectId, "Icon removed") {
        controller.deleteProjectIcon(projectId)
        refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
    }

    fun addGalleryImage(
        projectId: String,
        file: ProjectFileUpload,
        featured: Boolean = false,
        title: String = "",
        description: String = "",
    ) = runProjectAction(projectId, "Gallery image added") {
        controller.addGalleryImage(
            projectIdOrSlug = projectId,
            file = file,
            featured = featured,
            title = title.ifBlank { "Gallery image" },
            description = description,
        )
        refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
    }

    fun deleteGalleryImage(projectId: String, imageUrl: String) = runProjectAction(imageUrl, "Gallery image removed") {
        controller.deleteGalleryImage(projectId, imageUrl)
        refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
    }

    fun setGalleryImageAsBanner(projectId: String, imageUrl: String) =
        runProjectAction(imageUrl, "Banner updated") {
            controller.setGalleryImageAsBanner(projectId, imageUrl)
            refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
        }

    fun modifyGalleryImage(
        projectId: String,
        imageUrl: String,
        featured: Boolean? = null,
        title: String? = null,
        description: String? = null,
        ordering: Int? = null,
    ) = runProjectAction(imageUrl, "Gallery image updated") {
        controller.modifyGalleryImage(
            projectIdOrSlug = projectId,
            imageUrl = imageUrl,
            featured = featured,
            title = title,
            description = description,
            ordering = ordering,
        )
        refreshProjectAfterMutation(projectId, refreshVersions = false, refreshMembers = false)
    }

    fun createVersion(projectId: String, request: CreateVersionRequest) = runProjectAction(projectId, "Version created") {
        controller.createVersion(projectId, request)
        refreshProjectAfterMutation(projectId, refreshVersions = true, refreshMembers = false)
    }

    fun updateVersion(versionId: String, update: VersionUpdate) = runProjectAction(versionId, "Version updated") {
        controller.updateVersion(versionId, update)
        mutableProjectDetail.value?.project?.id?.let {
            refreshProjectAfterMutation(it, refreshVersions = true, refreshMembers = false)
        }
    }

    fun deleteVersion(versionId: String) = runProjectAction(versionId, "Version deleted") {
        controller.deleteVersion(versionId)
        mutableProjectDetail.value?.project?.id?.let {
            refreshProjectAfterMutation(it, refreshVersions = true, refreshMembers = false)
        }
    }

    fun searchMember(query: String) {
        memberSearchJob?.cancel()
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            mutableMemberSearch.value = MemberSearchState()
            return
        }
        mutableMemberSearch.value = MemberSearchState(query = normalized, isSearching = true)
        memberSearchJob = viewModelScope.launch {
            delay(400)
            suspendCatching { controller.findUser(normalized) }.fold(
                onSuccess = { mutableMemberSearch.value = MemberSearchState(query = normalized, user = it) },
                onFailure = { error ->
                    mutableMemberSearch.value = MemberSearchState(
                        query = normalized,
                        errorMessage = error.message ?: "Unable to search for that user.",
                    )
                },
            )
        }
    }

    fun inviteMember(teamId: String, userId: String) = runProjectAction(userId, "Invitation sent") {
        controller.addTeamMember(teamId, userId)
        refreshMembersAfterMutation()
        refreshOrganizationMembersAfterMutation()
    }

    fun updateMember(teamId: String, userId: String, update: ProjectMemberUpdate) =
        runProjectAction(userId, "Member updated") {
            controller.updateTeamMember(teamId, userId, update)
            refreshMembersAfterMutation()
            refreshOrganizationMembersAfterMutation()
        }

    fun removeMember(teamId: String, userId: String) = runProjectAction(userId, "Member removed") {
        controller.deleteTeamMember(teamId, userId)
        refreshMembersAfterMutation()
        refreshOrganizationMembersAfterMutation()
    }

    fun joinTeam(teamId: String) = runProjectAction(teamId, "Invitation accepted") {
        controller.joinTeam(teamId)
        refreshMembersAfterMutation()
        refreshOrganizationMembersAfterMutation()
    }

    fun transferTeamOwnership(teamId: String, userId: String) = runProjectAction(userId, "Ownership transferred") {
        controller.transferTeamOwnership(teamId, userId)
        refreshMembersAfterMutation()
        refreshOrganizationMembersAfterMutation()
    }

    fun clearProjectActionStatus() {
        if (!mutableProjectAction.value.isRunning) mutableProjectAction.value = ProjectActionState()
    }

    private fun runProjectAction(targetId: String, successMessage: String, action: suspend () -> Unit) {
        if (mutableProjectAction.value.isRunning) return
        projectActionJob?.cancel()
        mutableProjectAction.value = ProjectActionState(isRunning = true, targetId = targetId)
        projectActionJob = viewModelScope.launch {
            suspendCatching { action() }.fold(
                onSuccess = { mutableProjectAction.value = ProjectActionState(successMessage = successMessage) },
                onFailure = { error ->
                    mutableProjectAction.value = ProjectActionState(
                        targetId = targetId,
                        errorMessage = error.message ?: "The project operation failed.",
                    )
                },
            )
        }
    }

    private suspend fun refreshMembersAfterMutation() {
        val detail = mutableProjectDetail.value ?: return
        val roster = controller.loadProjectTeamRoster(detail.project)
        if (mutableProjectDetail.value?.project?.id == detail.project.id) {
            mutableProjectDetail.value = detail.copy(
                members = roster.projectMembers,
                organizationMembers = roster.organizationMembers,
                organizationName = roster.organization?.name ?: detail.organizationName,
                memberErrorMessage = null,
            )
        }
    }

    private suspend fun refreshOrganizationMembersAfterMutation() {
        val detail = mutableOrganizationDetail.value ?: return
        val org = detail.organization
        val key = org.slug.ifBlank { org.id }
        val members = runCatching {
            controller.loadOrganizationMembers(key, org.teamId)
        }.getOrElse { return }
        if (mutableOrganizationDetail.value?.organization?.id == org.id) {
            mutableOrganizationDetail.value = detail.copy(
                organization = org.copy(members = members),
                members = members,
            )
        }
    }

    private suspend fun refreshProjectAfterMutation(
        projectId: String,
        refreshVersions: Boolean,
        refreshMembers: Boolean,
    ) {
        val detail = mutableProjectDetail.value ?: return
        if (detail.project.id != projectId && detail.project.slug != projectId) return
        val projectKey = detail.project.slug ?: detail.project.id
        val project = controller.loadProjectDetails(projectKey)
        val versions = if (refreshVersions) controller.loadProjectVersions(projectKey) else detail.versions
        val dependencies = if (refreshVersions) controller.loadProjectDependencies(versions) else detail.dependencies
        val roster = if (refreshMembers) {
            controller.loadProjectTeamRoster(project)
        } else {
            null
        }
        if (mutableProjectDetail.value?.project?.id == detail.project.id) {
            mutableProjectDetail.value = detail.copy(
                project = project,
                versions = versions,
                dependencies = dependencies,
                members = roster?.projectMembers ?: detail.members,
                organizationMembers = roster?.organizationMembers ?: detail.organizationMembers,
                organizationName = roster?.organization?.name ?: detail.organizationName,
                errorMessage = null,
                memberErrorMessage = null,
            )
        }
    }

    fun signOut() {
        pendingToken = null
        mutableOAuthError.value = null
        mutableProfileUpdate.value = ProfileUpdateState()
        mutableProjectUpdate.value = ProjectUpdateState()
        mutableProjectAction.value = ProjectActionState()
        mutableMemberSearch.value = MemberSearchState()
        mutableAnalytics.value = AnalyticsState()
        oauthCoordinator.clear()
        tokenStore.clear()
        projectLoadJob?.cancel()
        projectActionJob?.cancel()
        memberSearchJob?.cancel()
        analyticsJob?.cancel()
        organizationLoadJob?.cancel()
        projectLoadJob = null
        organizationLoadJob = null
        controller.signOut()
        mutableProjectDetail.value = null
        mutableOrganizationDetail.value = null
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    private companion object {
        const val MAX_USER_AVATAR_BYTES = 2 * 1024 * 1024
    }
}

private suspend inline fun <T> suspendCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

data class ProjectDetailState(
    val project: Project,
    val versions: List<ProjectVersion> = emptyList(),
    val dependencies: List<ProjectDependency> = emptyList(),
    /** Direct project team collaborators. */
    val members: List<ProjectMember> = emptyList(),
    /** Organization members who inherit access when the project is under an org. */
    val organizationMembers: List<ProjectMember> = emptyList(),
    val organizationName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memberErrorMessage: String? = null,
)

data class OrganizationDetailState(
    val organization: Organization,
    val projects: List<Project> = emptyList(),
    val members: List<com.rinthy.shared.model.ProjectMember> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ProfileUpdateState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

data class ProjectUpdateState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

data class ProjectActionState(
    val isRunning: Boolean = false,
    val targetId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

data class MemberSearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val user: Account? = null,
    val errorMessage: String? = null,
)

data class AnalyticsState(
    val rangeDays: Int = 30,
    val projectIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val report: AnalyticsReport? = null,
    val wallet: WalletReport? = null,
    val errorMessage: String? = null,
    val walletErrorMessage: String? = null,
)
