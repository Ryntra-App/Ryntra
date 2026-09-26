package com.ryntra.shared.network

import com.ryntra.shared.model.ModrinthNotification

/**
 * Notification work done outside the app's screens: background polling, and the actions
 * offered on a posted notification, which run with no dashboard loaded.
 */
class NotificationPollingClient {
    private val api = ModrinthApi(createPlatformHttpClient())

    suspend fun load(token: String): List<ModrinthNotification> {
        val account = api.getCurrentAccount(token)
        return api.getNotifications(account.id, token)
    }

    suspend fun markRead(notificationId: String, token: String) {
        api.markNotificationsRead(listOf(notificationId), token)
    }

    /**
     * Joins the team a `team_invite` notification points at, then marks it read. The team id
     * lives in the notification's action route, which a posted notification does not carry,
     * so the notification is looked up again first.
     */
    suspend fun acceptInvitation(notificationId: String, token: String) {
        val notification = load(token).firstOrNull { it.id == notificationId }
            ?: throw IllegalStateException("The invitation is no longer available.")
        val teamId = notification.actions.firstNotNullOfOrNull { it.teamJoinId }
            ?: throw IllegalStateException("This notification does not contain an invitation.")
        api.joinTeam(teamId, token)
        api.markNotificationsRead(listOf(notificationId), token)
    }

    fun close() = api.close()
}
