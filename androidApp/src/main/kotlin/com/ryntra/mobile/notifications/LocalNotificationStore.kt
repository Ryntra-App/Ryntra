package com.ryntra.mobile.notifications

import android.content.Context

internal class LocalNotificationStore(context: Context) {
    private val storage = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun hasCompletedInitialSync(): Boolean = storage.getBoolean(KEY_INITIAL_SYNC, false)

    fun knownIds(): Set<String> = storage.getStringSet(KEY_KNOWN_IDS, emptySet()).orEmpty()

    fun updateKnownIds(ids: Collection<String>) {
        storage.edit()
            .putBoolean(KEY_INITIAL_SYNC, true)
            .putStringSet(KEY_KNOWN_IDS, ids.filter(String::isNotBlank).take(MAX_KNOWN_IDS).toSet())
            .apply()
    }

    fun clear() = storage.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "ryntra_local_notifications"
        const val KEY_INITIAL_SYNC = "initialSyncCompleted"
        const val KEY_KNOWN_IDS = "knownNotificationIds"
        const val MAX_KNOWN_IDS = 300
    }
}
