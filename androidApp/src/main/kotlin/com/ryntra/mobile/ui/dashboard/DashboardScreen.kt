package com.ryntra.mobile.ui.dashboard

import com.ryntra.mobile.AppScreenRequest
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.ryntra.mobile.BrowseState
import com.ryntra.mobile.ProjectDisclosuresState
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
import com.ryntra.mobile.ui.dashboard.browse.BrowseScreen
import com.ryntra.mobile.ui.dashboard.notifications.NotificationsScreen
import com.ryntra.mobile.ui.dashboard.wallet.WalletAffiliateState
import com.ryntra.mobile.ui.dashboard.wallet.WalletScreen
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
import com.ryntra.shared.model.ProjectDisclosureDraft
import com.ryntra.shared.model.ProjectSearchHit
import com.ryntra.shared.model.ProjectSearchQuery
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.ProjectCreationMetadata
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
    Browse,
    Wallet,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(
    dashboard: Dashboard,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRetryDashboard: () -> Unit = {},
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
    disclosures: ProjectDisclosuresState = ProjectDisclosuresState(),
    browse: BrowseState = BrowseState(),
    memberSearch: MemberSearchState = MemberSearchState(),
    analytics: AnalyticsState = AnalyticsState(),
    notifications: NotificationState = NotificationState(),
    instantNotifications: InstantNotificationState = InstantNotificationState(),
    preferences: RyntraPreferences = RyntraPreferences(),
    onLoadAnalytics: (Int) -> Unit = {},
    onRetryAnalytics: (Int) -> Unit = {},
    onRefreshWallet: () -> Unit = {},
    requestedScreen: AppScreenRequest? = null,
    onRequestedScreenShown: () -> Unit = {},
    onCancelPayout: (String) -> Unit = {},
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
    onOpenBrowse: () -> Unit = {},
    onCloseBrowse: () -> Unit = {},
    onBrowseTextChange: (String) -> Unit = {},
    onBrowseSubmit: () -> Unit = {},
    onBrowseQueryChange: (ProjectSearchQuery) -> Unit = {},
    onBrowseLoadMore: () -> Unit = {},
    onOpenSearchHit: (ProjectSearchHit) -> Unit = {},
    onForgetRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onLoadProjectDisclosures: (String, Boolean) -> Unit = { _, _ -> },
    onSaveProjectDisclosures: (String, ProjectDisclosureDraft) -> Unit = { _, _ -> },
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
    var isBrowseVisible by rememberSaveable { mutableStateOf(false) }
    var isWalletVisible by rememberSaveable { mutableStateOf(false) }
    var isCreatingProject by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var retainedProjectDetail by remember { mutableStateOf<ProjectDetailState?>(null) }
    var retainedOrganizationDetail by remember { mutableStateOf<OrganizationDetailState?>(null) }
    var hasUnsavedProjectChanges by remember { mutableStateOf(false) }
    var isConfirmingProjectClose by remember { mutableStateOf(false) }
    val tabStateHolder = rememberSaveableStateHolder()
    val hazeState = rememberHazeState()
    val colors = RyntraDesign.colors
    // Read outside the transition lambdas below, which are not composable scopes.
    val slideSpec = RyntraDesign.spatialSpec<IntOffset>()
    val scaleSpec = RyntraDesign.spatialSpec<Float>()
    val fadeSpec = RyntraDesign.effectsSpec<Float>()
    val retryLabel = stringResource(R.string.common_retry)
    val isPlatformNative = RyntraDesign.isPlatformNative
    val detailTitle = projectDetail?.project?.title ?: organizationDetail?.organization?.name
    val isDetailVisible = isProfileVisible || isNotificationsVisible || isBrowseVisible || isWalletVisible ||
        projectDetail != null || organizationDetail != null
    val contentLayer = when {
        projectDetail != null -> DashboardLayer.Project
        organizationDetail != null -> DashboardLayer.Organization
        isProfileVisible -> DashboardLayer.Profile
        isNotificationsVisible -> DashboardLayer.Notifications
        isBrowseVisible -> DashboardLayer.Browse
        isWalletVisible -> DashboardLayer.Wallet
        else -> DashboardLayer.Tabs
    }
    // The collapsing headline belongs to the tab roots; a pushed screen keeps a
    // pinned bar so its back button never scrolls away. Both are remembered so the
    // active one can change without tearing down the scaffold.
    val collapsingTopBarBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pinnedTopBarBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarScrollBehavior = if (isDetailVisible) pinnedTopBarBehavior else collapsingTopBarBehavior
    val requestCloseProject = {
        if (hasUnsavedProjectChanges) {
            isConfirmingProjectClose = true
        } else {
            onCloseProject()
        }
    }

    // A screen swapped in under a collapsed bar could be too short to scroll the
    // headline back out, leaving it stuck shut.
    LaunchedEffect(contentLayer, destination) {
        collapsingTopBarBehavior.state.heightOffset = 0f
        collapsingTopBarBehavior.state.contentOffset = 0f
    }

    // A widget tap names a screen; it opens over whatever tab was last shown.
    LaunchedEffect(requestedScreen) {
        val screen = requestedScreen ?: return@LaunchedEffect
        isProfileVisible = false
        isBrowseVisible = false
        isNotificationsVisible = false
        isWalletVisible = false
        when (screen) {
            AppScreenRequest.Wallet -> {
                isWalletVisible = true
                onRefreshWallet()
            }
            AppScreenRequest.Analytics -> destination = DashboardDestination.Analytics
            AppScreenRequest.Notifications -> {
                isNotificationsVisible = true
                onRefreshNotifications()
            }
        }
        onRequestedScreenShown()
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
        val message = errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onRetryDashboard()
        }
    }

    val destinations = DashboardDestination.entries
    val destinationLabels = destinations.map { stringResource(it.labelRes) }
    val tabs = remember(destinationLabels) {
        destinations.mapIndexed { index, item -> RyntraTab(destinationLabels[index], item.icon) }
    }
    val selectDestination = { target: DashboardDestination ->
        destination = target
        isProfileVisible = false
        isBrowseVisible = false
        isWalletVisible = false
    }
    BackHandler(
        enabled = isProfileVisible || isNotificationsVisible || isBrowseVisible || isWalletVisible ||
            projectDetail != null || organizationDetail != null,
    ) {
        when {
            projectDetail != null -> requestCloseProject()
            organizationDetail != null -> onCloseOrganization()
            isProfileVisible -> isProfileVisible = false
            isNotificationsVisible -> isNotificationsVisible = false
            isWalletVisible -> isWalletVisible = false
            isBrowseVisible -> {
                isBrowseVisible = false
                onCloseBrowse()
            }
        }
    }

    Scaffold(
        topBar = {
            RyntraTopBar(
                title = detailTitle ?: when {
                    isProfileVisible -> stringResource(R.string.nav_profile)
                    isNotificationsVisible -> stringResource(R.string.notifications_title)
                    isBrowseVisible -> stringResource(R.string.browse_title)
                    isWalletVisible -> stringResource(R.string.wallet_title)
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
                        isWalletVisible -> isWalletVisible = false
                        isBrowseVisible -> {
                            isBrowseVisible = false
                            onCloseBrowse()
                        }
                    }
                },
                showAvatar = !isDetailVisible,
                onSearchClick = if (!isDetailVisible) {
                    {
                        isBrowseVisible = true
                        onOpenBrowse()
                    }
                } else {
                    null
                },
                searchDescription = stringResource(R.string.browse_open),
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
                scrollBehavior = topBarScrollBehavior,
            )
        },
        bottomBar = {
            // Material's navigation bar is a real scaffold bar, so content is laid out
            // above it. The Ryntra tab bar is glass over the content and is placed in
            // the content layer below instead.
            if (isPlatformNative && !isDetailVisible) {
                RyntraTabBar(
                    tabs = tabs,
                    selectedIndex = destination.ordinal,
                    onSelect = { selectDestination(destinations[it]) },
                    hazeState = hazeState,
                    glassQuality = preferences.glassQuality,
                )
            }
        },
        snackbarHost = {
            // The scaffold lifts a snackbar above its own bottomBar. The Ryntra tab
            // bar is not one, so that theme has to clear it by hand.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = if (isPlatformNative || isDetailVisible) {
                    Modifier
                } else {
                    Modifier.padding(bottom = 86.dp)
                },
            )
        },
        containerColor = colors.background,
        modifier = Modifier
            .fillMaxSize()
            .preferredFrameRate(FrameRateCategory.High)
            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .then(if (isDetailVisible || isPlatformNative) Modifier else Modifier.hazeSource(hazeState)),
            ) {
                AnimatedContent(
                    targetState = contentLayer,
                    transitionSpec = {
                        when {
                            targetState == DashboardLayer.Tabs -> {
                                fadeIn(fadeSpec) togetherWith
                                    slideOutOfContainer(
                                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = slideSpec,
                                    )
                            }
                            initialState == DashboardLayer.Tabs -> {
                                slideIntoContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = slideSpec,
                                ) togetherWith fadeOut(fadeSpec)
                            }
                            else -> fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
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
                        disclosures = disclosures,
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
                        onLoadDisclosures = onLoadProjectDisclosures,
                        onSaveDisclosures = onSaveProjectDisclosures,
                        onLoadModeration = onLoadProjectModeration,
                        onSendModerationReply = onSendModerationReply,
                        onDeleteModerationMessage = onDeleteModerationMessage,
                        loadProjectCreationMetadata = onLoadProjectCreationMetadata,
                        onUnsavedChangesChanged = { hasUnsavedProjectChanges = it },
                        onRetry = { onProjectClick(detail.project) },
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
                        onRetry = { onOrganizationClick(detail.organization) },
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
                    DashboardLayer.Browse -> BrowseScreen(
                        state = browse,
                        onTextChange = onBrowseTextChange,
                        onSubmit = onBrowseSubmit,
                        onQueryChange = onBrowseQueryChange,
                        onLoadMore = onBrowseLoadMore,
                        onOpenHit = onOpenSearchHit,
                        onForgetRecentSearch = onForgetRecentSearch,
                        onClearRecentSearches = onClearRecentSearches,
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
                    DashboardLayer.Wallet -> WalletScreen(
                        report = analytics.wallet,
                        isLoading = analytics.isWalletLoading,
                        errorMessage = analytics.walletErrorMessage,
                        cancellingPayoutId = analytics.cancellingPayoutId,
                        actionErrorMessage = analytics.walletActionErrorMessage,
                        onRefresh = onRefreshWallet,
                        onCancelPayout = onCancelPayout,
                        affiliate = WalletAffiliateState(
                            report = analytics.affiliate,
                            isLoading = analytics.isAffiliateLoading,
                            errorMessage = analytics.affiliateErrorMessage,
                        ),
                    )
                    DashboardLayer.Tabs -> AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            // Material's fade-through: the outgoing screen clears out
                            // first, and the incoming one is lifted in on a slight scale
                            // so the swap reads as new content rather than a dissolve of
                            // two screens over each other.
                            (fadeIn(fadeSpec) + scaleIn(initialScale = 0.92f, animationSpec = scaleSpec))
                                .togetherWith(fadeOut(fadeSpec))
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
                                onRetry = onRetryAnalytics,
                                onOpenWallet = {
                                    isWalletVisible = true
                                    onRefreshWallet()
                                },
                                onRetryWallet = onRefreshWallet,
                            )
                            }
                        }
                    }
                    }
                }
            }
            if (!isPlatformNative && !isDetailVisible) {
                RyntraTabBar(
                    tabs = tabs,
                    selectedIndex = destination.ordinal,
                    onSelect = { selectDestination(destinations[it]) },
                    hazeState = hazeState,
                    glassQuality = preferences.glassQuality,
                    modifier = Modifier.align(Alignment.BottomCenter),
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
