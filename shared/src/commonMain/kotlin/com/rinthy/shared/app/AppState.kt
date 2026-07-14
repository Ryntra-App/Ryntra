package com.rinthy.shared.app

import com.rinthy.shared.model.Dashboard

sealed interface AppState {
    data object SignedOut : AppState

    data class Loading(
        val previousDashboard: Dashboard? = null,
    ) : AppState

    data class Ready(
        val dashboard: Dashboard,
    ) : AppState

    data class Failed(
        val message: String,
        val previousDashboard: Dashboard? = null,
        val isAuthenticationFailure: Boolean = false,
    ) : AppState
}
