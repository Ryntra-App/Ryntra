package com.rinthy.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinthy.mobile.security.SecureTokenStore
import com.rinthy.shared.app.AppController
import com.rinthy.shared.app.AppState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RinthyViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = SecureTokenStore(application)
    private val controller = AppController()
    private var pendingToken: String? = null

    val state: StateFlow<AppState> = controller.state

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
        pendingToken = token.trim()
        controller.signIn(token)
    }

    fun refresh() = controller.refresh()

    fun signOut() {
        pendingToken = null
        tokenStore.clear()
        controller.signOut()
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
