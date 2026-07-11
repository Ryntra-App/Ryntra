package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
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
import com.rinthy.mobile.ui.components.RinthyTab
import com.rinthy.mobile.ui.components.RinthyTabBar
import com.rinthy.mobile.ui.components.RinthyTopBar
import com.rinthy.shared.model.Dashboard

private enum class DashboardDestination(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Overview", Icons.Rounded.Dashboard),
    Projects("Projects", Icons.Rounded.Inventory2),
    Organizations("Teams", Icons.Rounded.Groups),
    Account("Account", Icons.Rounded.AccountCircle),
}

@Composable
fun DashboardScreen(
    dashboard: Dashboard,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(DashboardDestination.Overview) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val destinations = DashboardDestination.entries
    val tabs = destinations.map { RinthyTab(it.label, it.icon) }

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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 78.dp, bottom = 88.dp),
            ) {
                when (destination) {
                    DashboardDestination.Overview -> OverviewScreen(dashboard)
                    DashboardDestination.Projects -> ProjectsScreen(dashboard.projects)
                    DashboardDestination.Organizations -> OrganizationsScreen(dashboard.organizations)
                    DashboardDestination.Account -> AccountScreen(
                        account = dashboard.account,
                        projectCount = dashboard.projects.size,
                        organizationCount = dashboard.organizations.size,
                        onSignOut = onSignOut,
                    )
                }
            }
            RinthyTopBar(
                title = if (destination == DashboardDestination.Overview) "Rinthy" else destination.label,
                avatarUrl = dashboard.account.avatarUrl,
                avatarDescription = "Open ${dashboard.account.username}'s account",
                isRefreshing = isRefreshing,
                canRefresh = destination != DashboardDestination.Account,
                refreshIcon = Icons.Rounded.Refresh,
                onRefresh = onRefresh,
                onAvatarClick = { destination = DashboardDestination.Account },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            RinthyTabBar(
                tabs = tabs,
                selectedIndex = destination.ordinal,
                onSelect = { destination = destinations[it] },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
