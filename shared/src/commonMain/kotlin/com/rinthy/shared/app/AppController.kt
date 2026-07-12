package com.rinthy.shared.app

import com.rinthy.shared.data.DashboardRepository
import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.network.ModrinthApi
import com.rinthy.shared.network.createPlatformHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppController internal constructor(
    private val repository: DashboardRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<AppState>(AppState.SignedOut)
    private var accessToken: String? = null
    private var loadJob: Job? = null

    val state: StateFlow<AppState> = mutableState.asStateFlow()

    constructor() : this(
        DashboardRepository(ModrinthApi(createPlatformHttpClient())),
    )

    fun signIn(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) {
            mutableState.value = AppState.Failed("Enter a Modrinth access token.")
            return
        }
        accessToken = normalizedToken
        load(previousDashboard = null)
    }

    fun refresh() {
        val dashboard = currentDashboard() ?: return
        load(previousDashboard = dashboard)
    }

    suspend fun loadProjectDetails(projectIdOrSlug: String): Project {
        val token = accessToken ?: error("Sign in before loading project details.")
        return repository.loadProject(projectIdOrSlug, token)
    }

    suspend fun loadProjectVersions(projectIdOrSlug: String): List<ProjectVersion> {
        val token = accessToken ?: error("Sign in before loading project versions.")
        return repository.loadProjectVersions(projectIdOrSlug, token)
    }

    suspend fun loadProjectMembers(projectIdOrSlug: String, teamId: String?): List<ProjectMember> {
        val token = accessToken ?: error("Sign in before loading project members.")
        return repository.loadProjectMembers(projectIdOrSlug, teamId, token)
    }

    fun signOut() {
        loadJob?.cancel()
        accessToken = null
        mutableState.value = AppState.SignedOut
    }

    fun observe(listener: (AppState) -> Unit): Observation {
        val job = scope.launch {
            state.collect(listener)
        }
        return Observation(job)
    }

    fun close() {
        loadJob?.cancel()
        scope.cancel()
        repository.close()
    }

    private fun load(previousDashboard: Dashboard?) {
        val token = accessToken ?: return
        loadJob?.cancel()
        mutableState.value = AppState.Loading(previousDashboard)
        loadJob = scope.launch {
            mutableState.value = runCatching { repository.load(token) }
                .fold(
                    onSuccess = AppState::Ready,
                    onFailure = { error ->
                        AppState.Failed(
                            message = error.message ?: "Unable to reach Modrinth.",
                            previousDashboard = previousDashboard,
                        )
                    },
                )
        }
    }

    private fun currentDashboard() = when (val current = state.value) {
        is AppState.Ready -> current.dashboard
        is AppState.Loading -> current.previousDashboard
        is AppState.Failed -> current.previousDashboard
        AppState.SignedOut -> null
    }
}

class Observation internal constructor(
    private val job: Job,
) {
    fun cancel() = job.cancel()
}
