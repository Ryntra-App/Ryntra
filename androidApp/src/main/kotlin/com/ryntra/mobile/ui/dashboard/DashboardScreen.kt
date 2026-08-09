package com.ryntra.mobile.ui.dashboard

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.preferredFrameRate
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChartNoAxesColumnIncreasing
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.UsersRound
import com.ryntra.mobile.OrganizationDetailState
import com.ryntra.mobile.R
import com.ryntra.mobile.MemberSearchState
import com.ryntra.mobile.AnalyticsState
import com.ryntra.mobile.ProfileUpdateState
import com.ryntra.mobile.NotificationState
import com.ryntra.mobile.InstantNotificationState
import com.ryntra.mobile.ProjectActionState
import com.ryntra.mobile.ProjectModerationState
import com.ryntra.mobile.ProjectDetailState
import com.ryntra.mobile.preferences.AppLanguage
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.RyntraPreferences
import com.ryntra.mobile.preferences.ThemeStyle
import com.ryntra.mobile.ui.components.RyntraTab
import com.ryntra.mobile.ui.components.RyntraTabBar
import com.ryntra.mobile.ui.components.RyntraTopBar
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.mobile.ui.dashboard.account.AccountScreen
import com.ryntra.mobile.ui.dashboard.notifications.NotificationsScreen
import com.ryntra.mobile.ui.dashboard.analytics.AnalyticsScreen
import com.ryntra.mobile.ui.dashboard.organizations.OrganizationDetailScreen
import com.ryntra.mobile.ui.dashboard.organizations.OrganizationsScreen
import com.ryntra.mobile.ui.dashboard.overview.OverviewScreen
import com.ryntra.mobile.ui.dashboard.project.ProjectDetailScreen
import com.ryntra.mobile.ui.dashboard.projects.ProjectsScreen
import com.ryntra.mobile.ui.dashboard.project.create.CreateProjectDialog
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Organization
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationMetadata
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

private enum class DashboardDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Overview(R.string.nav_dashboard, Lucide.LayoutGrid),
    Projects(R.string.nav_projects, Lucide.Package),
    Organizations(R.string.nav_teams, Lucide.UsersRound),
    Analytics(R.string.nav_analytics, Lucide.ChartNoAxesColumnIncreasing),
}

private enum class DashboardLayer {
    Tabs,
    Profile,
    Project,
    Organization,
    Notifications,
}

@Composable
fun DashboardScreen(
    dashboard: Dashboard,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    projectDetail: ProjectDetailState? = null,
    organizationDetail: OrganizationDetailState? = null,
    onProjectClick: (Project) -> Unit = {},
    onNotificationProjectClick: (String) -> Unit = {},
    onCloseProject: () -> Unit = {},
    onOrganizationClick: (Organization) -> Unit = {},
    onCloseOrganization: () -> Unit = {},
    profileUpdate: ProfileUpdateState = ProfileUpdateState(),
    onUpdateProfile: (String, String, String) -> Unit = { _, _, _ -> },
    onChangeAvatar: (String, ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteAvatar: (String) -> Unit = {},
    projectUpdate: com.ryntra.mobile.ProjectUpdateState = com.ryntra.mobile.ProjectUpdateState(),
    onUpdateProject: (String, com.ryntra.shared.model.ProjectUpdate) -> Unit = { _, _ -> },
    onClearProjectUpdateStatus: () -> Unit = {},
    projectAction: ProjectActionState = ProjectActionState(),
    moderation: ProjectModerationState = ProjectModerationState(),
    memberSearch: MemberSearchState = MemberSearchState(),
    analytics: AnalyticsState = AnalyticsState(),
    notifications: NotificationState = NotificationState(),
    instantNotifications: InstantNotificationState = InstantNotificationState(),
    preferences: RyntraPreferences = RyntraPreferences(),
    onLoadAnalytics: (Int) -> Unit = {},
    onChangeProjectIcon: (String, ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteProjectIcon: (String) -> Unit = {},
    onSubmitProjectForModeration: (String) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onAddGalleryImage: (String, ProjectFileUpload, Boolean, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteGalleryImage: (String, String) -> Unit = { _, _ -> },
    onSetGalleryBanner: (String, String) -> Unit = { _, _ -> },
    onModifyGalleryImage: (String, String, String, String, Int?) -> Unit = { _, _, _, _, _ -> },
    onCreateVersion: (String, CreateVersionRequest) -> Unit = { _, _ -> },
    onLoadProjectCreationMetadata: suspend () -> ProjectCreationMetadata = { error("Unavailable") },
    onCreateProject: suspend (CreateProjectRequest) -> Result<Project> = { Result.failure(IllegalStateException("Unavailable")) },
    onUpdateVersion: (String, VersionUpdate) -> Unit = { _, _ -> },
    onDeleteVersion: (String) -> Unit = {},
    onSearchMember: (String) -> Unit = {},
    onInviteMember: (String, String) -> Unit = { _, _ -> },
    onUpdateMember: (String, String, ProjectMemberUpdate) -> Unit = { _, _, _ -> },
    onRemoveMember: (String, String) -> Unit = { _, _ -> },
    onJoinTeam: (String) -> Unit = {},
    onTransferOwnership: (String, String) -> Unit = { _, _ -> },
    onClearProjectActionStatus: () -> Unit = {},
    onLoadProjectModeration: (String, Boolean) -> Unit = { _, _ -> },
    onSendModerationReply: (String, String, String?) -> Unit = { _, _, _ -> },
    onDeleteModerationMessage: (String, String) -> Unit = { _, _ -> },
    onThemeStyleChange: (ThemeStyle) -> Unit = {},
    onAppearanceModeChange: (AppearanceMode) -> Unit = {},
    onAppLanguageChange: (AppLanguage) -> Unit = {},
    onShowFavoriteProjectsChange: (Boolean) -> Unit = {},
    onShowProjectBannersChange: (Boolean) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
    onGlassQualityChange: (GlassQuality) -> Unit = {},
    onSortModeChange: (ProjectSortMode) -> Unit = {},
    onToggleFavoriteProject: (String) -> Unit = {},
    onLocalNotificationsChange: (Boolean) -> Unit = {},
    onStartInstantNotifications: () -> Unit = {},
    onDisconnectInstantNotifications: () -> Unit = {},
    onRefreshNotifications: () -> Unit = {},
    onMarkNotificationsRead: (List<String>) -> Unit = {},
    onAcceptNotificationInvitation: (ModrinthNotification) -> Unit = {},
    onResetAppearance: () -> Unit = {},
    onExportPreferences: (String) -> String = { "" },
    onImportPreferences: (String) -> Result<Unit> = { Result.failure(IllegalArgumentException("Import unavailable.")) },
    onSignOut: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(DashboardDestination.Overview) }
    var isProfileVisible by rememberSaveable { mutableStateOf(false) }
    var isNotificationsVisible by rememberSaveable { mutableStateOf(false) }
    var isCreatingProject by rememberSaveable { mutableStateOf(false) }
    var visibleError by remember { mutableStateOf<String?>(null) }
    var retainedProjectDetail by remember { mutableStateOf<ProjectDetailState?>(null) }
    var retainedOrganizationDetail by remember { mutableStateOf<OrganizationDetailState?>(null) }
    var hasUnsavedProjectChanges by remember { mutableStateOf(false) }
    var isConfirmingProjectClose by remember { mutableStateOf(false) }
    val tabStateHolder = rememberSaveableStateHolder()
    val hazeState = rememberHazeState()
    val colors = RyntraDesign.colors
    val motion = RyntraDesign.motion
    val isPlatformNative = RyntraDesign.isPlatformNative
    val detailTitle = projectDetail?.project?.title ?: organizationDetail?.organization?.name
    val isDetailVisible = isProfileVisible || isNotificationsVisible || projectDetail != null || organizationDetail != null
    val contentLayer = when {
        projectDetail != null -> DashboardLayer.Project
        organizationDetail != null -> DashboardLayer.Organization
        isProfileVisible -> DashboardLayer.Profile
        isNotificationsVisible -> DashboardLayer.Notifications
        else -> DashboardLayer.Tabs
    }
    val requestCloseProject = {
        if (hasUnsavedProjectChanges) {
            isConfirmingProjectClose = true
        } else {
            onCloseProject()
        }
    }

    LaunchedEffect(projectDetail?.project?.id) {
        hasUnsavedProjectChanges = false
        isConfirmingProjectClose = false
    }

    LaunchedEffect(projectDetail) {
        projectDetail?.let { retainedProjectDetail = it }
    }
    LaunchedEffect(organizationDetail) {
        organizationDetail?.let { retainedOrganizationDetail = it }
    }

    LaunchedEffect(errorMessage) {
        visibleError = errorMessage
        if (errorMessage != null) {
            delay(4_500)
            visibleError = null
        }
    }

    val destinations = DashboardDestination.entries
    val destinationLabels = destinations.map { stringResource(it.labelRes) }
    val tabs = remember(destinationLabels) {
        destinations.mapIndexed { index, item -> RyntraTab(destinationLabels[index], item.icon) }
    }
    BackHandler(enabled = isProfileVisible || isNotificationsVisible || projectDetail != null || organizationDetail != null) {
        when {
            projectDetail != null -> requestCloseProject()
            organizationDetail != null -> onCloseOrganization()
            isProfileVisible -> isProfileVisible = false
            isNotificationsVisible -> isNotificationsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .preferredFrameRate(FrameRateCategory.High)
            .background(colors.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isDetailVisible || isPlatformNative) Modifier else Modifier.hazeSource(hazeState))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 84.dp),
            ) {
                AnimatedContent(
                    targetState = contentLayer,
                    transitionSpec = {
                        val duration = motion.duration(300)
                        when {
                            targetState == DashboardLayer.Tabs -> {
                                fadeIn(tween(duration)) togetherWith
                                    slideOutOfContainer(
                                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(duration),
                                    )
                            }
                            initialState == DashboardLayer.Tabs -> {
                                slideIntoContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(duration),
                                ) togetherWith
                                    fadeOut(tween(motion.duration(120)))
                            }
                            else -> fadeIn(tween(motion.duration(180))) togetherWith
                                fadeOut(tween(motion.duration(120)))
                        }
                    },
                    label = "Dashboard layer",
                    modifier = Modifier.fillMaxSize(),
                ) { layer ->
                    when (layer) {
                    DashboardLayer.Project -> (projectDetail ?: retainedProjectDetail)?.let { detail ->
                        ProjectDetailScreen(
                        project = detail.project,
                        versions = detail.versions,
                        dependencies = detail.dependencies,
                        members = detail.members,
                        organizationMembers = detail.organizationMembers,
                        organizationName = detail.organizationName,
                        currentUserId = dashboard.account.id,
                        isReadOnly = detail.isReadOnly,
                        isLoading = detail.isLoading,
                        errorMessage = detail.errorMessage,
                        memberErrorMessage = detail.memberErrorMessage,
                        projectUpdate = projectUpdate,
                        onUpdateProject = onUpdateProject,
                        onClearProjectUpdateStatus = onClearProjectUpdateStatus,
                        projectAction = projectAction,
                        moderation = moderation,
                        memberSearch = memberSearch,
                        onChangeProjectIcon = onChangeProjectIcon,
                        onDeleteProjectIcon = onDeleteProjectIcon,
                        onSubmitProjectForModeration = onSubmitProjectForModeration,
                        onDeleteProject = onDeleteProject,
                        onAddGalleryImage = onAddGalleryImage,
                        onDeleteGalleryImage = onDeleteGalleryImage,
                        onSetGalleryBanner = onSetGalleryBanner,
                        onModifyGalleryImage = onModifyGalleryImage,
                        onCreateVersion = onCreateVersion,
                        onUpdateVersion = onUpdateVersion,
                        onDeleteVersion = onDeleteVersion,
                        onSearchMember = onSearchMember,
                        onInviteMember = onInviteMember,
                        onUpdateMember = onUpdateMember,
                        onRemoveMember = onRemoveMember,
                        onJoinTeam = onJoinTeam,
                        onTransferOwnership = onTransferOwnership,
                        onClearProjectActionStatus = onClearProjectActionStatus,
                        onLoadModeration = onLoadProjectModeration,
                        onSendModerationReply = onSendModerationReply,
                        onDeleteModerationMessage = onDeleteModerationMessage,
                        loadProjectCreationMetadata = onLoadProjectCreationMetadata,
                        onUnsavedChangesChanged = { hasUnsavedProjectChanges = it },
                    )
                    }
                    DashboardLayer.Organization -> (organizationDetail ?: retainedOrganizationDetail)?.let { detail ->
                        OrganizationDetailScreen(
                        organization = detail.organization,
                        projects = detail.projects,
                        members = detail.members,
                        currentUserId = dashboard.account.id,
                        isLoading = detail.isLoading,
                        errorMessage = detail.errorMessage,
                        projectAction = projectAction,
                        memberSearch = memberSearch,
                        onProjectClick = onProjectClick,
                        onDeleteProject = onDeleteProject,
                        onSearchMember = onSearchMember,
                        onInviteMember = onInviteMember,
                        onUpdateMember = onUpdateMember,
                        onRemoveMember = onRemoveMember,
                        onJoinTeam = onJoinTeam,
                        onTransferOwnership = onTransferOwnership,
                        onClearProjectActionStatus = onClearProjectActionStatus,
                    )
                    }
                    DashboardLayer.Profile -> AccountScreen(
                        account = dashboard.account,
                        projectCount = dashboard.projects.size,
                        organizationCount = dashboard.organizations.size,
                        profileUpdate = profileUpdate,
                        preferences = preferences,
                        instantNotifications = instantNotifications,
                        onUpdateProfile = onUpdateProfile,
                        onChangeAvatar = onChangeAvatar,
                        onDeleteAvatar = onDeleteAvatar,
                        onThemeStyleChange = onThemeStyleChange,
                        onAppearanceModeChange = onAppearanceModeChange,
                        onAppLanguageChange = onAppLanguageChange,
                        onShowFavoriteProjectsChange = onShowFavoriteProjectsChange,
                        onShowProjectBannersChange = onShowProjectBannersChange,
                        onReduceMotionChange = onReduceMotionChange,
                        onGlassQualityChange = onGlassQualityChange,
                        onLocalNotificationsChange = onLocalNotificationsChange,
                        onStartInstantNotifications = onStartInstantNotifications,
                        onDisconnectInstantNotifications = onDisconnectInstantNotifications,
                        onResetAppearance = onResetAppearance,
                        onExportPreferences = { onExportPreferences(dashboard.account.username) },
                        onImportPreferences = onImportPreferences,
                        onSignOut = onSignOut,
                    )
                    DashboardLayer.Notifications -> NotificationsScreen(
                        state = notifications,
                        onRefresh = onRefreshNotifications,
                        onMarkRead = onMarkNotificationsRead,
                        onAcceptInvitation = onAcceptNotificationInvitation,
                        onOpenProject = { projectReference ->
                            isNotificationsVisible = false
                            onNotificationProjectClick(projectReference)
                        },
                    )
                    DashboardLayer.Tabs -> AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = motion.duration(220),
                                    delayMillis = motion.duration(90),
                                ),
                            ) togetherWith fadeOut(tween(motion.duration(90)))
                        },
                        label = "Dashboard destination",
                        modifier = Modifier.fillMaxSize(),
                    ) { targetDestination ->
                        tabStateHolder.SaveableStateProvider(targetDestination) {
                            when (targetDestination) {
                            DashboardDestination.Overview -> OverviewScreen(
                                dashboard = dashboard,
                                onProjectClick = onProjectClick,
                            )
                            DashboardDestination.Projects -> ProjectsScreen(
                                projects = dashboard.projects,
                                projectAction = projectAction,
                                sortMode = preferences.projectSortMode,
                                favoriteProjectIds = preferences.favoriteProjectIds,
                                showFavoriteProjects = preferences.showFavoriteProjects,
                                showProjectBanners = preferences.showProjectBanners,
                                onSortModeChange = onSortModeChange,
                                onToggleFavoriteProject = onToggleFavoriteProject,
                                onProjectClick = onProjectClick,
                                onDeleteProject = onDeleteProject,
                                onClearProjectActionStatus = onClearProjectActionStatus,
                                onCreateProject = { isCreatingProject = true },
                            )
                            DashboardDestination.Organizations -> OrganizationsScreen(
                                organizations = dashboard.organizations,
                                onOrganizationClick = onOrganizationClick,
                            )
                            DashboardDestination.Analytics -> AnalyticsScreen(
                                dashboard = dashboard,
                                state = analytics,
                                onRangeChange = onLoadAnalytics,
                            )
                            }
                        }
                    }
                    }
                }
            }
            RyntraTopBar(
                title = detailTitle ?: when {
                    isProfileVisible -> stringResource(R.string.nav_profile)
                    isNotificationsVisible -> stringResource(R.string.notifications_title)
                    else -> destinationLabels[destination.ordinal]
                },
                avatarUrl = dashboard.account.avatarUrl,
                avatarDescription = stringResource(R.string.nav_open_account, dashboard.account.username),
                isRefreshing = isRefreshing,
                onAvatarClick = { isProfileVisible = true },
                navigationIcon = if (isDetailVisible) Lucide.ArrowLeft else null,
                navigationDescription = if (isDetailVisible) stringResource(R.string.nav_back) else null,
                onNavigationClick = {
                    when {
                        projectDetail != null -> requestCloseProject()
                        organizationDetail != null -> onCloseOrganization()
                        isProfileVisible -> isProfileVisible = false
                        isNotificationsVisible -> isNotificationsVisible = false
                    }
                },
                showAvatar = !isDetailVisible,
                onNotificationsClick = if (!isDetailVisible) {
                    {
                        isNotificationsVisible = true
                        onRefreshNotifications()
                    }
                } else {
                    null
                },
                unreadNotificationCount = notifications.unreadCount,
                notificationsDescription = stringResource(R.string.notifications_title),
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (!isDetailVisible) {
                RyntraTabBar(
                    tabs = tabs,
                    selectedIndex = destination.ordinal,
                    onSelect = {
                        destination = destinations[it]
                        isProfileVisible = false
                    },
                    hazeState = hazeState,
                    glassQuality = preferences.glassQuality,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            AnimatedVisibility(
                visible = visibleError != null,
                enter = fadeIn(tween(motion.duration(180))) +
                    slideInVertically(tween(motion.duration(180))) { it / 2 },
                exit = fadeOut(tween(motion.duration(130))) +
                    slideOutVertically(tween(motion.duration(130))) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 96.dp),
            ) {
                BasicText(
                    text = visibleError.orEmpty(),
                    style = RyntraDesign.body.copy(color = colors.labelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceRaised.copy(alpha = 0.98f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                )
            }
            if (isCreatingProject) {
                CreateProjectDialog(
                    loadMetadata = onLoadProjectCreationMetadata,
                    createProject = onCreateProject,
                    onDismiss = { isCreatingProject = false },
                    onCreated = { project ->
                        isCreatingProject = false
                        onProjectClick(project)
                    },
                )
            }
        }
    }

    if (isConfirmingProjectClose) {
        AlertDialog(
            onDismissRequest = { isConfirmingProjectClose = false },
            title = { Text(stringResource(R.string.project_edit_discard_title)) },
            text = { Text(stringResource(R.string.project_edit_discard_message)) },
            dismissButton = {
                TextButton(onClick = { isConfirmingProjectClose = false }) {
                    Text(stringResource(R.string.project_edit_keep_editing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hasUnsavedProjectChanges = false
                        isConfirmingProjectClose = false
                        onCloseProject()
                    },
                ) {
                    Text(stringResource(R.string.project_edit_discard), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
