package com.ryntra.mobile

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ryntra.mobile.auth.OAuthCallbackResult
import com.ryntra.mobile.auth.OAuthCoordinator
import com.ryntra.mobile.preferences.AppLanguage
import com.ryntra.mobile.preferences.AppLocale
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.RyntraPreferences
import com.ryntra.mobile.preferences.RyntraPreferencesStore
import com.ryntra.mobile.preferences.ThemeStyle
import com.ryntra.mobile.notifications.NotificationScheduler
import com.ryntra.mobile.notifications.NotificationBadgeStore
import com.ryntra.mobile.notifications.NotificationRefreshSignal
import com.ryntra.mobile.notifications.instant.InstantCallbackResult
import com.ryntra.mobile.notifications.instant.InstantNotificationCoordinator
import com.ryntra.mobile.security.SecureTokenStore
import com.ryntra.shared.app.AppController
import com.ryntra.shared.app.AppState
import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModerationThread
import com.ryntra.shared.model.Account
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.AnalyticsReport
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectUploadLimits
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.model.WalletReport
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.updates.AppUpdate
import com.ryntra.shared.updates.AppUpdateClient
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

class RyntraViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = SecureTokenStore(application)
    private val preferencesStore = RyntraPreferencesStore(application)
    private val oauthCoordinator = OAuthCoordinator(application)
    private val instantNotificationCoordinator = InstantNotificationCoordinator(application)
    private val notificationBadgeStore = NotificationBadgeStore(application)
    private val controller = AppController()
    private val updateClient = AppUpdateClient()
    private val mutableOAuthError = MutableStateFlow<String?>(null)
    private val mutableProjectDetail = MutableStateFlow<ProjectDetailState?>(null)
    private val mutableOrganizationDetail = MutableStateFlow<OrganizationDetailState?>(null)
    private val mutableProfileUpdate = MutableStateFlow(ProfileUpdateState())
    private val mutableProjectUpdate = MutableStateFlow(ProjectUpdateState())
    private val mutableProjectAction = MutableStateFlow(ProjectActionState())
    private val mutableModeration = MutableStateFlow(ProjectModerationState())
    private val mutableMemberSearch = MutableStateFlow(MemberSearchState())
    private val mutableAnalytics = MutableStateFlow(AnalyticsState())
    private val mutableNotifications = MutableStateFlow(NotificationState())
    private val mutableInstantNotifications = MutableStateFlow(
        InstantNotificationState(
            isAvailable = instantNotificationCoordinator.isAvailable,
            isConnected = instantNotificationCoordinator.isConnected,
        ),
    )
    private val mutableAppUpdate = MutableStateFlow<AppUpdate?>(null)
    private var pendingToken: String? = null
    private var projectLoadJob: Job? = null
    private var organizationLoadJob: Job? = null
    private var projectActionJob: Job? = null
    private var moderationJob: Job? = null
    private var memberSearchJob: Job? = null
    private var analyticsJob: Job? = null
    private var notificationsJob: Job? = null
    private var notificationAccountId: String? = null
    private var pendingNotificationProjectReference: String? = null
    private var instantSynchronizationJob: Job? = null
    private var lastForegroundRefreshAt = 0L
    private val notificationRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationRefreshSignal.ACTION) refreshNotifications()
        }
    }

    val state: StateFlow<AppState> = controller.state
    val oauthError: StateFlow<String?> = mutableOAuthError.asStateFlow()
    val projectDetail: StateFlow<ProjectDetailState?> = mutableProjectDetail.asStateFlow()
    val organizationDetail: StateFlow<OrganizationDetailState?> = mutableOrganizationDetail.asStateFlow()
    val profileUpdate: StateFlow<ProfileUpdateState> = mutableProfileUpdate.asStateFlow()
    val projectUpdate: StateFlow<ProjectUpdateState> = mutableProjectUpdate.asStateFlow()
    val projectAction: StateFlow<ProjectActionState> = mutableProjectAction.asStateFlow()
    val moderation: StateFlow<ProjectModerationState> = mutableModeration.asStateFlow()
    val memberSearch: StateFlow<MemberSearchState> = mutableMemberSearch.asStateFlow()
    val analytics: StateFlow<AnalyticsState> = mutableAnalytics.asStateFlow()
    val notifications: StateFlow<NotificationState> = mutableNotifications.asStateFlow()
    val instantNotifications: StateFlow<InstantNotificationState> = mutableInstantNotifications.asStateFlow()
    val appUpdate: StateFlow<AppUpdate?> = mutableAppUpdate.asStateFlow()
    val preferences: StateFlow<RyntraPreferences> = preferencesStore.preferences

    init {
        ContextCompat.registerReceiver(
            application,
            notificationRefreshReceiver,
            IntentFilter(NotificationRefreshSignal.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (instantNotificationCoordinator.isConnected && preferencesStore.preferences.value.localNotificationsEnabled) {
            setLocalNotificationsEnabled(false)
        }
        tokenStore.read()?.let { savedToken ->
            pendingToken = savedToken
            controller.signIn(savedToken)
        }
        viewModelScope.launch {
            state.collect { currentState ->
                if (currentState is AppState.Ready) {
                    pendingToken?.let(tokenStore::write)
                    pendingToken = null
                    if (notificationAccountId != currentState.dashboard.account.id) {
                        notificationAccountId = currentState.dashboard.account.id
                        refreshNotifications()
                    }
                    pendingNotificationProjectReference?.let { reference ->
                        pendingNotificationProjectReference = null
                        openNotificationProject(reference)
                    }
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
        when (val instantResult = instantNotificationCoordinator.consumeCallback(uri)) {
            InstantCallbackResult.Ignored -> Unit
            InstantCallbackResult.Success -> {
                setLocalNotificationsEnabled(false)
                mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                    isConnected = true,
                    isLoading = false,
                    errorMessage = null,
                )
                return
            }
            is InstantCallbackResult.Failure -> {
                mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                    isLoading = false,
                    errorMessage = instantResult.message,
                )
                return
            }
        }
        when (val result = oauthCoordinator.consumeCallback(uri)) {
            OAuthCallbackResult.Ignored -> Unit
            is OAuthCallbackResult.Failure -> mutableOAuthError.value = result.message
            is OAuthCallbackResult.Success -> signIn(result.token)
        }
    }

    fun refresh() = controller.refresh()

    fun checkForUpdates() {
        viewModelScope.launch {
            val update = runCatching { updateClient.latestRelease("apk") }.getOrNull()
            if (update != null && AppUpdateClient.isNewerVersion(update.version)) {
                mutableAppUpdate.value = update
            }
        }
    }

    fun dismissAppUpdate() {
        mutableAppUpdate.value = null
    }

    fun onAppForeground() {
        val now = SystemClock.elapsedRealtime()
        if (lastForegroundRefreshAt != 0L && now - lastForegroundRefreshAt < FOREGROUND_REFRESH_INTERVAL_MS) return
        lastForegroundRefreshAt = now
        if (state.value is AppState.Ready) {
            refreshNotifications()
            controller.refresh()
        }
        synchronizeInstantNotifications()
    }

    private fun synchronizeInstantNotifications() {
        if (!instantNotificationCoordinator.isAvailable || instantSynchronizationJob?.isActive == true) return
        instantSynchronizationJob = viewModelScope.launch {
            suspendCatching { instantNotificationCoordinator.synchronize() }.fold(
                onSuccess = { result ->
                    if (result.isConnected) setLocalNotificationsEnabled(false)
                    else if (result.hasRegistration) setLocalNotificationsEnabled(true)
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isConnected = result.isConnected,
                        errorMessage = when (result.disabledReason) {
                            "authorization_expired" -> "Modrinth authorization expired. Connect server notifications again."
                            else -> null
                        },
                    )
                },
                onFailure = { error ->
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isConnected = instantNotificationCoordinator.isConnected,
                        errorMessage = error.message ?: "Unable to synchronize instant notifications.",
                    )
                },
            )
        }
    }

    fun setShowFavoriteProjects(isEnabled: Boolean) = preferencesStore.setShowFavoriteProjects(isEnabled)

    fun setShowProjectBanners(isEnabled: Boolean) = preferencesStore.setShowProjectBanners(isEnabled)

    fun setThemeStyle(style: ThemeStyle) = preferencesStore.setThemeStyle(style)

    fun setAppearanceMode(mode: AppearanceMode) = preferencesStore.setAppearanceMode(mode)

    fun setAppLanguage(language: AppLanguage) {
        preferencesStore.setAppLanguage(language)
        AppLocale.apply(language)
        if (instantNotificationCoordinator.isConnected) {
            viewModelScope.launch { suspendCatching { instantNotificationCoordinator.updateLocale() } }
        }
    }

    fun setReduceMotion(isEnabled: Boolean) = preferencesStore.setReduceMotion(isEnabled)

    fun setGlassQuality(quality: GlassQuality) = preferencesStore.setGlassQuality(quality)

    fun setProjectSortMode(mode: ProjectSortMode) = preferencesStore.setProjectSortMode(mode)

    fun setLocalNotificationsEnabled(isEnabled: Boolean) {
        preferencesStore.setLocalNotificationsEnabled(isEnabled)
        if (isEnabled) NotificationScheduler.enable(getApplication())
        else NotificationScheduler.disable(getApplication())
    }

    fun startInstantNotifications() {
        if (!instantNotificationCoordinator.isAvailable || mutableInstantNotifications.value.isLoading) return
        mutableInstantNotifications.value = mutableInstantNotifications.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            suspendCatching { instantNotificationCoordinator.createAuthorizationUri() }.fold(
                onSuccess = { uri ->
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isLoading = false,
                        authorizationUri = uri,
                    )
                },
                onFailure = { error ->
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to start instant notifications.",
                    )
                },
            )
        }
    }

    fun authorizationUriOpened() {
        mutableInstantNotifications.value = mutableInstantNotifications.value.copy(authorizationUri = null)
    }

    fun disconnectInstantNotifications() {
        if (mutableInstantNotifications.value.isLoading) return
        mutableInstantNotifications.value = mutableInstantNotifications.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            suspendCatching { instantNotificationCoordinator.disconnect() }.fold(
                onSuccess = {
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isConnected = false,
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    mutableInstantNotifications.value = mutableInstantNotifications.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to disconnect instant notifications.",
                    )
                },
            )
        }
    }

    fun toggleFavoriteProject(projectId: String) = preferencesStore.toggleFavoriteProject(projectId)

    fun refreshNotifications() {
        if (state.value !is AppState.Ready) return
        notificationsJob?.cancel()
        mutableNotifications.value = mutableNotifications.value.copy(isLoading = true, errorMessage = null)
        notificationsJob = viewModelScope.launch {
            suspendCatching { controller.loadNotifications() }.fold(
                onSuccess = { items ->
                    val unreadCount = items.count { !it.read }
                    notificationBadgeStore.replace(unreadCount)
                    mutableNotifications.value = NotificationState(items = items, hasLoaded = true)
                },
                onFailure = { error ->
                    mutableNotifications.value = mutableNotifications.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load notifications.",
                    )
                },
            )
        }
    }

    fun markNotificationsRead(notificationIds: List<String>) {
        val ids = notificationIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        val unreadIds = mutableNotifications.value.items
            .asSequence()
            .filter { !it.read && it.id in ids }
            .map { it.id }
            .toSet()
        if (unreadIds.isEmpty()) return

        val optimisticState = mutableNotifications.value.withReadNotifications(unreadIds)
        mutableNotifications.value = optimisticState
        notificationBadgeStore.replace(optimisticState.unreadCount)

        viewModelScope.launch {
            suspendCatching { controller.markNotificationsRead(unreadIds.toList()) }.fold(
                onSuccess = {
                    mutableNotifications.value = mutableNotifications.value.copy(errorMessage = null)
                },
                onFailure = { error ->
                    val restored = mutableNotifications.value.withUnreadNotifications(unreadIds).copy(
                        errorMessage = error.message ?: "Unable to update notifications.",
                    )
                    mutableNotifications.value = restored
                    notificationBadgeStore.replace(restored.unreadCount)
                },
            )
        }
    }

    fun acceptNotificationInvitation(notification: ModrinthNotification) {
        if (mutableNotifications.value.activeActionNotificationId != null) return
        mutableNotifications.value = mutableNotifications.value.copy(
            activeActionNotificationId = notification.id,
            errorMessage = null,
        )
        viewModelScope.launch {
            suspendCatching { controller.acceptNotificationInvitation(notification) }.fold(
                onSuccess = {
                    mutableNotifications.value = mutableNotifications.value.copy(activeActionNotificationId = null)
                    refreshNotifications()
                    controller.refresh()
                },
                onFailure = { error ->
                    mutableNotifications.value = mutableNotifications.value.copy(
                        activeActionNotificationId = null,
                        errorMessage = error.message ?: "Unable to accept the invitation.",
                    )
                },
            )
        }
    }

    fun resetAppearance() = preferencesStore.resetAppearance()

    fun exportPreferences(username: String): String =
        preferencesStore.exportJson(username = username, appVersion = BuildConfig.VERSION_NAME)

    fun importPreferences(rawJson: String): Result<Unit> =
        preferencesStore.importJson(rawJson).onSuccess {
            AppLocale.apply(preferencesStore.preferences.value.appLanguage)
            if (preferencesStore.preferences.value.localNotificationsEnabled) {
                NotificationScheduler.enable(getApplication())
            } else {
                NotificationScheduler.disable(getApplication())
            }
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
        loadProject(seed = project, projectKey = project.slug ?: project.id)
    }

    fun openNotificationProject(projectReference: String) {
        if (projectReference.isBlank()) return
        if (currentDashboard() == null) {
            pendingNotificationProjectReference = projectReference
            return
        }
        val managedProject = currentDashboard()?.projects?.firstOrNull { project ->
            project.id == projectReference || project.slug == projectReference
        }
        if (managedProject != null) {
            openProject(managedProject)
            return
        }
        loadProject(
            seed = Project(id = projectReference, slug = projectReference, title = projectReference),
            projectKey = projectReference,
            initiallyReadOnly = true,
        )
    }

    private fun loadProject(
        seed: Project,
        projectKey: String,
        initiallyReadOnly: Boolean = false,
    ) {
        projectLoadJob?.cancel()
        moderationJob?.cancel()
        mutableModeration.value = ProjectModerationState()
        mutableProjectDetail.value = ProjectDetailState(
            project = seed,
            isReadOnly = initiallyReadOnly,
            isLoading = true,
        )
        projectLoadJob = viewModelScope.launch {
            val detailsDeferred = async { suspendCatching { controller.loadProjectDetails(projectKey) } }
            val versionsDeferred = async { suspendCatching { controller.loadProjectVersions(projectKey) } }
            val details = detailsDeferred.await()
            val versions = versionsDeferred.await()
            val loadedProject = details.getOrElse { seed }
            val isReadOnly = !isManagedProject(loadedProject)
            // Public projects do not need private roster requests. This also keeps organization
            // permissions intact for projects present in the signed-in creator portfolio.
            val roster = if (isReadOnly) {
                null
            } else {
                suspendCatching { controller.loadProjectTeamRoster(loadedProject) }
            }
            val dependencies = versions.getOrNull()?.let { loadedVersions ->
                suspendCatching { controller.loadProjectDependencies(loadedVersions) }.getOrDefault(emptyList())
            }.orEmpty()
            val current = mutableProjectDetail.value
            if (current?.project?.id != seed.id) return@launch
            if (details.isSuccess) controller.updateCachedProject(loadedProject)

            val teamRoster = roster?.getOrNull()
            mutableProjectDetail.value = ProjectDetailState(
                project = loadedProject,
                versions = versions.getOrDefault(emptyList()),
                dependencies = dependencies,
                members = teamRoster?.projectMembers.orEmpty(),
                organizationMembers = teamRoster?.organizationMembers.orEmpty(),
                organizationName = teamRoster?.organization?.name,
                isReadOnly = isReadOnly,
                isLoading = false,
                errorMessage = details.exceptionOrNull()?.message ?: versions.exceptionOrNull()?.message,
                memberErrorMessage = roster?.exceptionOrNull()?.message,
            )
        }
    }

    private fun isManagedProject(project: Project): Boolean {
        val dashboard = currentDashboard() ?: return false
        if (dashboard.projects.any { it.matchesProject(project) }) return true
        if (mutableOrganizationDetail.value?.projects?.any { it.matchesProject(project) } == true) return true

        val organizationReference = project.organization?.normalizedReference() ?: return false
        return dashboard.organizations.any { organization ->
            organizationReference in setOf(
                organization.id.normalizedReference(),
                organization.slug.normalizedReference(),
                organization.name.normalizedReference(),
            )
        }
    }

    private fun currentDashboard(): com.ryntra.shared.model.Dashboard? = when (val current = state.value) {
        is AppState.Ready -> current.dashboard
        is AppState.Loading -> current.previousDashboard
        is AppState.Failed -> current.previousDashboard
        AppState.SignedOut -> null
    }

    fun closeProject() {
        projectLoadJob?.cancel()
        moderationJob?.cancel()
        projectLoadJob = null
        mutableProjectDetail.value = null
        mutableProjectAction.value = ProjectActionState()
        mutableModeration.value = ProjectModerationState()
        mutableMemberSearch.value = MemberSearchState()
    }

    fun loadProjectModeration(threadId: String, force: Boolean = false) {
        if (threadId.isBlank()) return
        val current = mutableModeration.value
        if (!force && (current.isLoading || current.thread?.id == threadId)) return

        moderationJob?.cancel()
        mutableModeration.value = current.copy(
            isLoading = true,
            errorMessage = null,
            requiresNewAuthorization = false,
        )
        moderationJob = viewModelScope.launch {
            val result = suspendCatching { controller.loadModerationThread(threadId) }
            if (mutableProjectDetail.value?.project?.threadId != threadId) return@launch
            mutableModeration.value = result.fold(
                onSuccess = { thread ->
                    mutableModeration.value.copy(thread = thread, isLoading = false)
                },
                onFailure = { error ->
                    mutableModeration.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load project moderation.",
                        requiresNewAuthorization = (error as? ApiException)?.statusCode in setOf(401, 403),
                    )
                },
            )
        }
    }

    fun sendModerationReply(threadId: String, body: String, replyingTo: String?) {
        if (threadId.isBlank() || body.isBlank() || mutableModeration.value.isSending) return
        moderationJob?.cancel()
        val previous = mutableModeration.value
        mutableModeration.value = previous.copy(isSending = true, errorMessage = null)
        moderationJob = viewModelScope.launch {
            val result = suspendCatching {
                controller.replyToModerationThread(threadId, body, replyingTo)
                controller.loadModerationThread(threadId)
            }
            if (mutableProjectDetail.value?.project?.threadId != threadId) return@launch
            mutableModeration.value = result.fold(
                onSuccess = { thread ->
                    mutableModeration.value.copy(
                        thread = thread,
                        isSending = false,
                        replyGeneration = previous.replyGeneration + 1,
                    )
                },
                onFailure = { error ->
                    mutableModeration.value.copy(
                        isSending = false,
                        errorMessage = error.message ?: "Unable to send the moderation reply.",
                        requiresNewAuthorization = (error as? ApiException)?.statusCode in setOf(401, 403),
                    )
                },
            )
        }
    }

    fun deleteModerationMessage(threadId: String, messageId: String) {
        if (threadId.isBlank() || messageId.isBlank() || mutableModeration.value.deletingMessageId != null) return
        moderationJob?.cancel()
        mutableModeration.value = mutableModeration.value.copy(deletingMessageId = messageId, errorMessage = null)
        moderationJob = viewModelScope.launch {
            val result = suspendCatching {
                controller.deleteModerationMessage(messageId)
                controller.loadModerationThread(threadId)
            }
            if (mutableProjectDetail.value?.project?.threadId != threadId) return@launch
            mutableModeration.value = result.fold(
                onSuccess = { thread ->
                    mutableModeration.value.copy(thread = thread, deletingMessageId = null)
                },
                onFailure = { error ->
                    mutableModeration.value.copy(
                        deletingMessageId = null,
                        errorMessage = error.message ?: "Unable to delete the moderation reply.",
                        requiresNewAuthorization = (error as? ApiException)?.statusCode in setOf(401, 403),
                    )
                },
            )
        }
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
        if (file.bytes.size > ProjectUploadLimits.USER_AVATAR_BYTES) {
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

    fun updateProject(projectId: String, update: com.ryntra.shared.model.ProjectUpdate) {
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
        controller.updateCachedProject(project)
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
        mutableModeration.value = ProjectModerationState()
        mutableMemberSearch.value = MemberSearchState()
        mutableAnalytics.value = AnalyticsState()
        mutableNotifications.value = NotificationState()
        notificationBadgeStore.clear()
        notificationAccountId = null
        pendingNotificationProjectReference = null
        oauthCoordinator.clear()
        tokenStore.clear()
        projectLoadJob?.cancel()
        projectActionJob?.cancel()
        moderationJob?.cancel()
        memberSearchJob?.cancel()
        analyticsJob?.cancel()
        notificationsJob?.cancel()
        organizationLoadJob?.cancel()
        projectLoadJob = null
        organizationLoadJob = null
        controller.signOut()
        mutableProjectDetail.value = null
        mutableOrganizationDetail.value = null
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(notificationRefreshReceiver) }
        instantNotificationCoordinator.close()
        controller.close()
        super.onCleared()
    }

    private companion object {
        const val FOREGROUND_REFRESH_INTERVAL_MS = 15_000L
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

private fun Project.matchesProject(other: Project): Boolean =
    id == other.id || (!slug.isNullOrBlank() && slug == other.slug)

private fun String.normalizedReference(): String = trim().lowercase()

data class ProjectDetailState(
    val project: Project,
    val versions: List<ProjectVersion> = emptyList(),
    val dependencies: List<ProjectDependency> = emptyList(),
    /** Direct project team collaborators. */
    val members: List<ProjectMember> = emptyList(),
    /** Organization members who inherit access when the project is under an org. */
    val organizationMembers: List<ProjectMember> = emptyList(),
    val organizationName: String? = null,
    val isReadOnly: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memberErrorMessage: String? = null,
)

data class OrganizationDetailState(
    val organization: Organization,
    val projects: List<Project> = emptyList(),
    val members: List<com.ryntra.shared.model.ProjectMember> = emptyList(),
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

data class ProjectModerationState(
    val thread: ModerationThread? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val deletingMessageId: String? = null,
    val errorMessage: String? = null,
    val requiresNewAuthorization: Boolean = false,
    val replyGeneration: Int = 0,
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

data class NotificationState(
    val items: List<ModrinthNotification> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeActionNotificationId: String? = null,
) {
    val unreadCount: Int get() = if (hasLoaded) items.count { !it.read } else 0
}

private fun NotificationState.withReadNotifications(notificationIds: Set<String>): NotificationState = copy(
    items = items.map { notification ->
        if (notification.id in notificationIds) notification.copy(read = true) else notification
    },
    hasLoaded = true,
    errorMessage = null,
)

private fun NotificationState.withUnreadNotifications(notificationIds: Set<String>): NotificationState = copy(
    items = items.map { notification ->
        if (notification.id in notificationIds) notification.copy(read = false) else notification
    },
    hasLoaded = true,
)

data class InstantNotificationState(
    val isAvailable: Boolean = false,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val authorizationUri: Uri? = null,
    val errorMessage: String? = null,
)
