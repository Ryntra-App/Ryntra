package com.ryntra.mobile.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ryntra.shared.model.WidgetSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * The last data the widgets were given. A widget renders from here straight away and never
 * waits on the network; refreshes overwrite it.
 */
internal class WidgetSnapshotStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): WidgetSnapshot? = WidgetSnapshot.fromJson(preferences.getString(KEY_SNAPSHOT, null))

    /**
     * The current snapshot, then every one written after it. A widget keeps its composition
     * alive between updates, so it has to observe the store rather than read it once.
     */
    fun snapshots(): Flow<WidgetSnapshot?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SNAPSHOT || key == null) trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun write(snapshot: WidgetSnapshot) = preferences.edit { putString(KEY_SNAPSHOT, snapshot.toJson()) }

    fun lastRefreshAttemptEpochMillis(): Long = preferences.getLong(KEY_LAST_ATTEMPT, 0L)

    fun recordRefreshAttempt() = preferences.edit { putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis()) }

    /** On sign-out, so a widget does not keep showing the previous account's money. */
    fun clear() = preferences.edit { remove(KEY_SNAPSHOT) }

    private companion object {
        const val FILE_NAME = "ryntra_widgets"
        const val KEY_SNAPSHOT = "snapshot"
        const val KEY_LAST_ATTEMPT = "last_refresh_attempt"
    }
}
