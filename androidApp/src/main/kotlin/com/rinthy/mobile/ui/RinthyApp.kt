package com.rinthy.mobile.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinthy.mobile.RinthyViewModel
import com.rinthy.mobile.ui.dashboard.DashboardScreen
import com.rinthy.mobile.ui.login.LoginScreen
import com.rinthy.shared.app.AppState

@Composable
fun RinthyApp(viewModel: RinthyViewModel) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    when (state) {
        AppState.SignedOut -> LoginScreen(onSignIn = viewModel::signIn)
        is AppState.Loading -> {
            val dashboard = state.previousDashboard
            if (dashboard == null) {
                LoginScreen(isLoading = true, onSignIn = viewModel::signIn)
            } else {
                DashboardScreen(
                    dashboard = dashboard,
                    isRefreshing = true,
                    onRefresh = viewModel::refresh,
                    onSignOut = viewModel::signOut,
                )
            }
        }
        is AppState.Ready -> DashboardScreen(
            dashboard = state.dashboard,
            onRefresh = viewModel::refresh,
            onSignOut = viewModel::signOut,
        )
        is AppState.Failed -> {
            val dashboard = state.previousDashboard
            if (dashboard == null) {
                LoginScreen(
                    errorMessage = state.message,
                    onSignIn = viewModel::signIn,
                )
            } else {
                DashboardScreen(
                    dashboard = dashboard,
                    errorMessage = state.message,
                    onRefresh = viewModel::refresh,
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}
