package com.ryntra.mobile.notifications

import android.content.Context
import androidx.core.content.edit
import com.ryntra.shared.model.ModrinthNotification

internal class NotificationBadgeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun readCount(): Int = preferences.getInt(KEY_UNREAD_COUNT, 0).coerceAtLeast(0)

    fun replace(unreadCount: Int) = preferences.edit {
        putInt(KEY_UNREAD_COUNT, unreadCount.coerceAtLeast(0))
    }

    fun recordPush(notificationId: String): Boolean = synchronized(lock) {
        val normalizedId = notificationId.trim()
        if (normalizedId.isEmpty()) return@synchronized false
        val knownIds = preferences.getStringSet(KEY_PUSH_IDS, emptySet()).orEmpty()
        if (normalizedId in knownIds) return@synchronized false
        val pendingIds = preferences.getStringSet(KEY_PENDING_PUSH_IDS, emptySet()).orEmpty()

        preferences.edit {
            putInt(KEY_UNREAD_COUNT, readCount() + 1)
            putStringSet(KEY_PUSH_IDS, (listOf(normalizedId) + knownIds).take(MAX_PUSH_IDS).toSet())
            putStringSet(KEY_PENDING_PUSH_IDS, pendingIds + normalizedId)
        }
        true
    }

    fun synchronize(notifications: List<ModrinthNotification>): Int = synchronized(lock) {
        val apiIds = notifications.mapTo(mutableSetOf(), ModrinthNotification::id)
        val pendingIds = preferences.getStringSet(KEY_PENDING_PUSH_IDS, emptySet())
            .orEmpty()
            .filterNotTo(mutableSetOf()) { it in apiIds }
        val unreadCount = notifications.count { !it.read } + pendingIds.size
        preferences.edit {
            putInt(KEY_UNREAD_COUNT, unreadCount)
            putStringSet(KEY_PENDING_PUSH_IDS, pendingIds)
        }
        unreadCount
    }

    fun clear() = preferences.edit { clear() }

    private companion object {
        const val FILE_NAME = "notification_badge"
        const val KEY_UNREAD_COUNT = "unread_count"
        const val KEY_PUSH_IDS = "received_push_ids"
        const val KEY_PENDING_PUSH_IDS = "pending_push_ids"
        const val MAX_PUSH_IDS = 300
        val lock = Any()
    }
}
