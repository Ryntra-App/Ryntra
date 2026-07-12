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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import com.rinthy.mobile.ui.components.RinthyTab
import com.rinthy.mobile.ui.components.RinthyTabBar
import com.rinthy.mobile.ui.components.RinthyTopBar
import com.rinthy.shared.model.Dashboard
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
    onSignOut: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(DashboardDestination.Overview) }
    var isProfileVisible by rememberSaveable { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val destinations = DashboardDestination.entries
    val tabs = remember {
        destinations.map { RinthyTab(it.label, it.icon) }
    }
    BackHandler(enabled = isProfileVisible || selectedProject != null) {
        when {
            selectedProject != null -> selectedProject = null
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
                if (selectedProject != null) {
                    val project = requireNotNull(selectedProject)
                    key(project.id) {
                        ProjectDetailScreen(project = project)
                    }
                } else if (isProfileVisible) {
                    AccountScreen(
                        account = dashboard.account,
                        projectCount = dashboard.projects.size,
                        organizationCount = dashboard.organizations.size,
                        onSignOut = onSignOut,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        destinations.forEach { page ->
                            val isSelected = destination == page
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = if (isSelected) 1f else 0f }
                                    .zIndex(if (isSelected) 1f else 0f),
                            ) {
                                when (page) {
                                    DashboardDestination.Overview -> OverviewScreen(
                                        dashboard = dashboard,
                                        onProjectClick = { selectedProject = it },
                                    )
                                    DashboardDestination.Projects -> ProjectsScreen(
                                        projects = dashboard.projects,
                                        onProjectClick = { selectedProject = it },
                                    )
                                    DashboardDestination.Organizations -> OrganizationsScreen(dashboard.organizations)
                                    DashboardDestination.Analytics -> AnalyticsScreen(dashboard)
                                }
                            }
                        }
                    }
                }
            }
            RinthyTopBar(
                title = selectedProject?.title ?: if (isProfileVisible) "Profile" else destination.label,
                avatarUrl = dashboard.account.avatarUrl,
                avatarDescription = "Open ${dashboard.account.username}'s account",
                isRefreshing = isRefreshing,
                onAvatarClick = { isProfileVisible = true },
                navigationIcon = if (isProfileVisible || selectedProject != null) Lucide.ArrowLeft else null,
                navigationDescription = if (isProfileVisible || selectedProject != null) "Back" else null,
                onNavigationClick = {
                    selectedProject = null
                    isProfileVisible = false
                },
                showAvatar = !isProfileVisible && selectedProject == null,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (!isProfileVisible && selectedProject == null) {
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
