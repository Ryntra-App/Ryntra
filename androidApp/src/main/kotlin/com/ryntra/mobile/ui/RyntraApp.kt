package com.ryntra.mobile.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryntra.mobile.RyntraViewModel
import com.ryntra.mobile.ui.dashboard.DashboardScreen
import com.ryntra.mobile.ui.login.LoginScreen
import com.ryntra.mobile.ui.theme.RyntraMotionProvider
import com.ryntra.mobile.ui.theme.RyntraTheme
import com.ryntra.shared.app.AppState
import com.ryntra.shared.model.Dashboard

@Composable
fun RyntraApp(viewModel: RyntraViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val oauthError by viewModel.oauthError.collectAsStateWithLifecycle()
    val projectDetail by viewModel.projectDetail.collectAsStateWithLifecycle()
    val organizationDetail by viewModel.organizationDetail.collectAsStateWithLifecycle()
    val profileUpdate by viewModel.profileUpdate.collectAsStateWithLifecycle()
    val projectUpdate by viewModel.projectUpdate.collectAsStateWithLifecycle()
    val projectAction by viewModel.projectAction.collectAsStateWithLifecycle()
    val memberSearch by viewModel.memberSearch.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val instantNotifications by viewModel.instantNotifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val startOAuth = {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, viewModel.startOAuth())
    }
    LaunchedEffect(instantNotifications.authorizationUri) {
        val uri = instantNotifications.authorizationUri ?: return@LaunchedEffect
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
        viewModel.authorizationUriOpened()
    }

    // key(appLanguage) forces stringResource lookups to re-run after a language change.
    // Do NOT replace LocalContext with createConfigurationContext — that drops
    // LocalActivityResultRegistryOwner and crashes the profile screen (export/import).
    // Locale is applied via AppCompatDelegate + Activity.attachBaseContext instead.
    key(preferences.appLanguage) {
        RyntraTheme(
            themeStyle = preferences.themeStyle,
            appearanceMode = preferences.appearanceMode,
        ) {
            RyntraMotionProvider(reduceMotion = preferences.reduceMotion) {
                val presentation = state.dashboardPresentation()
                if (presentation == null) {
                    LoginScreen(
                        isLoading = state is AppState.Loading,
                        errorMessage = oauthError ?: (state as? AppState.Failed)?.message,
                        onStartOAuth = startOAuth,
                        onSignIn = viewModel::signIn,
                    )
                } else {
                    DashboardScreen(
                        dashboard = presentation.dashboard,
                        isRefreshing = presentation.isRefreshing,
                        errorMessage = presentation.errorMessage,
                        projectDetail = projectDetail,
                        organizationDetail = organizationDetail,
                        profileUpdate = profileUpdate,
                        projectUpdate = projectUpdate,
                        projectAction = projectAction,
                        memberSearch = memberSearch,
                        analytics = analytics,
                        notifications = notifications,
                        instantNotifications = instantNotifications,
                        preferences = preferences,
                        onProjectClick = viewModel::openProject,
                        onCloseProject = viewModel::closeProject,
                        onOrganizationClick = viewModel::openOrganization,
                        onCloseOrganization = viewModel::closeOrganization,
                        onUpdateProfile = viewModel::updateProfile,
                        onChangeAvatar = viewModel::changeAvatar,
                        onDeleteAvatar = viewModel::deleteAvatar,
                        onUpdateProject = viewModel::updateProject,
                        onClearProjectUpdateStatus = viewModel::clearProjectUpdateStatus,
                        onLoadAnalytics = viewModel::loadAnalytics,
                        onChangeProjectIcon = viewModel::changeProjectIcon,
                        onDeleteProjectIcon = viewModel::deleteProjectIcon,
                        onAddGalleryImage = { projectId, file, featured, title, description ->
                            viewModel.addGalleryImage(projectId, file, featured, title, description)
                        },
                        onDeleteGalleryImage = viewModel::deleteGalleryImage,
                        onSetGalleryBanner = viewModel::setGalleryImageAsBanner,
                        onModifyGalleryImage = { projectId, url, title, description, ordering ->
                            viewModel.modifyGalleryImage(
                                projectId = projectId,
                                imageUrl = url,
                                title = title,
                                description = description,
                                ordering = ordering,
                            )
                        },
                        onCreateVersion = viewModel::createVersion,
                        onUpdateVersion = viewModel::updateVersion,
                        onDeleteVersion = viewModel::deleteVersion,
                        onSearchMember = viewModel::searchMember,
                        onInviteMember = viewModel::inviteMember,
                        onUpdateMember = viewModel::updateMember,
                        onRemoveMember = viewModel::removeMember,
                        onJoinTeam = viewModel::joinTeam,
                        onTransferOwnership = viewModel::transferTeamOwnership,
                        onClearProjectActionStatus = viewModel::clearProjectActionStatus,
                        onThemeStyleChange = viewModel::setThemeStyle,
                        onAppearanceModeChange = viewModel::setAppearanceMode,
                        onAppLanguageChange = viewModel::setAppLanguage,
                        onShowFavoriteProjectsChange = viewModel::setShowFavoriteProjects,
                        onReduceMotionChange = viewModel::setReduceMotion,
                        onGlassQualityChange = viewModel::setGlassQuality,
                        onSortModeChange = viewModel::setProjectSortMode,
                        onToggleFavoriteProject = viewModel::toggleFavoriteProject,
                        onLocalNotificationsChange = viewModel::setLocalNotificationsEnabled,
                        onStartInstantNotifications = viewModel::startInstantNotifications,
                        onDisconnectInstantNotifications = viewModel::disconnectInstantNotifications,
                        onRefreshNotifications = viewModel::refreshNotifications,
                        onMarkNotificationsRead = viewModel::markNotificationsRead,
                        onResetAppearance = viewModel::resetAppearance,
                        onExportPreferences = viewModel::exportPreferences,
                        onImportPreferences = viewModel::importPreferences,
                        onSignOut = viewModel::signOut,
                    )
                }
            }
        }
    }
}

private data class DashboardPresentation(
    val dashboard: Dashboard,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

private fun AppState.dashboardPresentation(): DashboardPresentation? = when (this) {
    AppState.SignedOut -> null
    is AppState.Loading -> previousDashboard?.let { DashboardPresentation(it, isRefreshing = true) }
    is AppState.Ready -> DashboardPresentation(dashboard)
    is AppState.Failed -> previousDashboard?.let { DashboardPresentation(it, errorMessage = message) }
}
