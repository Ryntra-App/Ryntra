package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch

internal class NotificationEndpoints(
    private val client: HttpClient,
) {
    suspend fun getForUser(userId: String, token: String): List<ModrinthNotification> =
        client.get("user/$userId/notifications") { authorize(token) }
            .decode<List<ModrinthNotification>>()
            .sortedByDescending(ModrinthNotification::created)

    suspend fun markRead(notificationIds: List<String>, token: String) {
        val ids = notificationIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return
        client.patch("notifications") {
            authorize(token)
            parameter("ids", apiJson.encodeToString(ids))
        }.ensureSuccess()
    }
}
