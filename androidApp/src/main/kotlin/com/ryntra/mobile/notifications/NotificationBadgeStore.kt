package com.ryntra.mobile.notifications

import android.content.Context
import androidx.core.content.edit

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

        preferences.edit {
            putInt(KEY_UNREAD_COUNT, readCount() + 1)
            putStringSet(KEY_PUSH_IDS, (listOf(normalizedId) + knownIds).take(MAX_PUSH_IDS).toSet())
        }
        true
    }

    fun clear() = preferences.edit { clear() }

    private companion object {
        const val FILE_NAME = "notification_badge"
        const val KEY_UNREAD_COUNT = "unread_count"
        const val KEY_PUSH_IDS = "received_push_ids"
        const val MAX_PUSH_IDS = 300
        val lock = Any()
    }
}
