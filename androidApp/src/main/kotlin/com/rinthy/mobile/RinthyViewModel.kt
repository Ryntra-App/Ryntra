package com.rinthy.mobile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinthy.mobile.auth.OAuthCallbackResult
import com.rinthy.mobile.auth.OAuthCoordinator
import com.rinthy.mobile.security.SecureTokenStore
import com.rinthy.shared.app.AppController
import com.rinthy.shared.app.AppState
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RinthyViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = SecureTokenStore(application)
    private val oauthCoordinator = OAuthCoordinator(application)
    private val controller = AppController()
    private val mutableOAuthError = MutableStateFlow<String?>(null)
    private val mutableProjectDetail = MutableStateFlow<ProjectDetailState?>(null)
    private var pendingToken: String? = null

    val state: StateFlow<AppState> = controller.state
    val oauthError: StateFlow<String?> = mutableOAuthError.asStateFlow()
    val projectDetail: StateFlow<ProjectDetailState?> = mutableProjectDetail.asStateFlow()

    init {
        tokenStore.read()?.let { savedToken ->
            pendingToken = savedToken
            controller.signIn(savedToken)
        }
        viewModelScope.launch {
            state.collect { currentState ->
                if (currentState is AppState.Ready) {
                    pendingToken?.let(tokenStore::write)
                    pendingToken = null
                } else if (currentState is AppState.Failed && currentState.previousDashboard == null) {
                    tokenStore.clear()
                    pendingToken = null
                }
            }
        }
    }

    fun signIn(token: String) {
        mutableOAuthError.value = null
        pendingToken = token.trim()
        controller.signIn(token)
    }

    fun startOAuth(): Uri {
        mutableOAuthError.value = null
        return oauthCoordinator.createAuthorizationUri()
    }

    fun handleOAuthCallback(uri: Uri) {
        when (val result = oauthCoordinator.consumeCallback(uri)) {
            OAuthCallbackResult.Ignored -> Unit
            is OAuthCallbackResult.Failure -> mutableOAuthError.value = result.message
            is OAuthCallbackResult.Success -> signIn(result.token)
        }
    }

    fun refresh() = controller.refresh()

    fun openProject(project: Project) {
        mutableProjectDetail.value = ProjectDetailState(project = project, isLoading = true)
        viewModelScope.launch {
            val projectKey = project.slug ?: project.id
            val detailsDeferred = async { runCatching { controller.loadProjectDetails(projectKey) } }
            val versionsDeferred = async { runCatching { controller.loadProjectVersions(projectKey) } }
            val details = detailsDeferred.await()
            val versions = versionsDeferred.await()
            val current = mutableProjectDetail.value
            if (current?.project?.id != project.id) return@launch

            mutableProjectDetail.value = ProjectDetailState(
                project = details.getOrElse { project },
                versions = versions.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = details.exceptionOrNull()?.message ?: versions.exceptionOrNull()?.message,
            )
        }
    }

    fun closeProject() {
        mutableProjectDetail.value = null
    }

    fun signOut() {
        pendingToken = null
        mutableOAuthError.value = null
        oauthCoordinator.clear()
        tokenStore.clear()
        controller.signOut()
        mutableProjectDetail.value = null
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}

data class ProjectDetailState(
    val project: Project,
    val versions: List<ProjectVersion> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
