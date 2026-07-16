package com.ryntra.mobile.notifications

import android.content.Context
import androidx.core.content.edit

internal class NotificationBadgeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun readCount(): Int = preferences.getInt(KEY_UNREAD_COUNT, 0).coerceAtLeast(0)

    fun replace(unreadCount: Int) = preferences.edit {
        putInt(KEY_UNREAD_COUNT, unreadCount.coerceAtLeast(0))
    }

    fun increment() {
        preferences.edit { putInt(KEY_UNREAD_COUNT, readCount() + 1) }
    }

    fun clear() = preferences.edit { clear() }

    private companion object {
        const val FILE_NAME = "notification_badge"
        const val KEY_UNREAD_COUNT = "unread_count"
    }
}
