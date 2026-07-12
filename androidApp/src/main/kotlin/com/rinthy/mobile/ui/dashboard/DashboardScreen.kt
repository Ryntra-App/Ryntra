package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import com.rinthy.mobile.OrganizationDetailState
import com.rinthy.mobile.ui.components.RinthyTab
import com.rinthy.mobile.ui.components.RinthyTabBar
import com.rinthy.mobile.ui.components.RinthyTopBar
import com.rinthy.mobile.ProjectDetailState
import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.Project
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChartNoAxesColumnIncreasing
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.UsersRound
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private enum class DashboardDestination(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Dashboard", Lucide.LayoutGrid),
    Projects("Projects", Lucide.Package),
    Organizations("Teams", Lucide.UsersRound),
    Analytics("Analytics", Lucide.ChartNoAxesColumnIncreasing),
}

@Composable
fun DashboardScreen(
    dashboard: Dashboard,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    projectDetail: ProjectDetailState? = null,
    organizationDetail: OrganizationDetailState? = null,
    onProjectClick: (Project) -> Unit = {},
    onCloseProject: () -> Unit = {},
    onOrganizationClick: (Organization) -> Unit = {},
    onCloseOrganization: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(DashboardDestination.Overview) }
    var isProfileVisible by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val destinations = DashboardDestination.entries
    val tabs = remember {
        destinations.map { RinthyTab(it.label, it.icon) }
    }
    BackHandler(enabled = isProfileVisible || projectDetail != null || organizationDetail != null) {
        when {
            projectDetail != null -> onCloseProject()
            organizationDetail != null -> onCloseOrganization()
            isProfileVisible -> isProfileVisible = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 76.dp),
            ) {
                if (projectDetail != null) {
                    ProjectDetailScreen(
                        project = projectDetail.project,
                        versions = projectDetail.versions,
                        members = projectDetail.members,
                        isLoading = projectDetail.isLoading,
                        errorMessage = projectDetail.errorMessage,
                        memberErrorMessage = projectDetail.memberErrorMessage,
                    )
                } else if (organizationDetail != null) {
                    OrganizationDetailScreen(
                        organization = organizationDetail.organization,
                        projects = organizationDetail.projects,
                        isLoading = organizationDetail.isLoading,
                        errorMessage = organizationDetail.errorMessage,
                        onProjectClick = onProjectClick,
                    )
                } else if (isProfileVisible) {
                    AccountScreen(
                        account = dashboard.account,
                        projectCount = dashboard.projects.size,
                        organizationCount = dashboard.organizations.size,
                        onSignOut = onSignOut,
                    )
                } else {
                    when (destination) {
                        DashboardDestination.Overview -> OverviewScreen(
                            dashboard = dashboard,
                            onProjectClick = onProjectClick,
                        )
                        DashboardDestination.Projects -> ProjectsScreen(
                            projects = dashboard.projects,
                            onProjectClick = onProjectClick,
                        )
                        DashboardDestination.Organizations -> OrganizationsScreen(
                            organizations = dashboard.organizations,
                            onOrganizationClick = onOrganizationClick,
                        )
                        DashboardDestination.Analytics -> AnalyticsScreen(dashboard)
                    }
                }
            }
            val detailTitle = projectDetail?.project?.title ?: organizationDetail?.organization?.name
            RinthyTopBar(
                title = detailTitle ?: if (isProfileVisible) "Profile" else destination.label,
                avatarUrl = dashboard.account.avatarUrl,
                avatarDescription = "Open ${dashboard.account.username}'s account",
                isRefreshing = isRefreshing,
                onAvatarClick = { isProfileVisible = true },
                navigationIcon = if (isProfileVisible || projectDetail != null || organizationDetail != null) Lucide.ArrowLeft else null,
                navigationDescription = if (isProfileVisible || projectDetail != null || organizationDetail != null) "Back" else null,
                onNavigationClick = {
                    onCloseProject()
                    onCloseOrganization()
                    isProfileVisible = false
                },
                showAvatar = !isProfileVisible && projectDetail == null && organizationDetail == null,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (!isProfileVisible && projectDetail == null && organizationDetail == null) {
                RinthyTabBar(
                    tabs = tabs,
                    selectedIndex = destination.ordinal,
                    onSelect = {
                        destination = destinations[it]
                        isProfileVisible = false
                    },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp),
            )
        }
    }
}
