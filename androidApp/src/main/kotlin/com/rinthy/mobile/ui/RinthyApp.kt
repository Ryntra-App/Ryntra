package com.rinthy.mobile.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinthy.mobile.RinthyViewModel
import com.rinthy.mobile.ui.dashboard.DashboardScreen
import com.rinthy.mobile.ui.login.LoginScreen
import com.rinthy.shared.app.AppState

@Composable
fun RinthyApp(viewModel: RinthyViewModel) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val oauthError = viewModel.oauthError.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val startOAuth = {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, viewModel.startOAuth())
    }
    when (state) {
        AppState.SignedOut -> LoginScreen(
            errorMessage = oauthError,
            onStartOAuth = startOAuth,
            onSignIn = viewModel::signIn,
        )
        is AppState.Loading -> {
            val dashboard = state.previousDashboard
            if (dashboard == null) {
                LoginScreen(
                    isLoading = true,
                    onStartOAuth = startOAuth,
                    onSignIn = viewModel::signIn,
                )
            } else {
                DashboardScreen(
                    dashboard = dashboard,
                    isRefreshing = true,
                    onSignOut = viewModel::signOut,
                )
            }
        }
        is AppState.Ready -> DashboardScreen(
            dashboard = state.dashboard,
            onSignOut = viewModel::signOut,
        )
        is AppState.Failed -> {
            val dashboard = state.previousDashboard
            if (dashboard == null) {
                LoginScreen(
                    errorMessage = oauthError ?: state.message,
                    onStartOAuth = startOAuth,
                    onSignIn = viewModel::signIn,
                )
            } else {
                DashboardScreen(
                    dashboard = dashboard,
                    errorMessage = state.message,
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}
