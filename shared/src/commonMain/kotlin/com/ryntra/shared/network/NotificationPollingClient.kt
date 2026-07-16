package com.ryntra.shared.network

import com.ryntra.shared.model.ModrinthNotification

class NotificationPollingClient {
    private val api = ModrinthApi(createPlatformHttpClient())

    suspend fun load(token: String): List<ModrinthNotification> {
        val account = api.getCurrentAccount(token)
        return api.getNotifications(account.id, token)
    }

    fun close() = api.close()
}
