package com.rinthy.shared.data

import com.rinthy.shared.model.Dashboard
import com.rinthy.shared.network.ModrinthApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DashboardRepository(
    private val api: ModrinthApi,
) {
    suspend fun load(token: String): Dashboard = coroutineScope {
        val account = api.getCurrentAccount(token)
        val projects = async { api.getProjects(account.id, token) }
        val organizations = async { api.getOrganizations(account.id, token) }

        Dashboard(
            account = account,
            projects = projects.await().sortedByDescending { it.updated.orEmpty() },
            organizations = organizations.await().sortedBy { it.name.lowercase() },
        )
    }

    fun close() = api.close()
}
