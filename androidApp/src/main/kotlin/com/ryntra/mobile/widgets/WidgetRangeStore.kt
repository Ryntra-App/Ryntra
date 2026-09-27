package com.ryntra.mobile.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * The range each placed downloads widget shows, keyed by app widget id.
 *
 * Kept in the app's own preferences and observed, like the snapshot, rather than in Glance's
 * per-widget state: the widget then redraws the moment a range is picked, whether or not
 * Glance's session for it is still alive.
 */
internal class WidgetRangeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun rangeFor(appWidgetId: Int): Int =
        preferences.getInt(key(appWidgetId), DownloadsWidget.DEFAULT_RANGE).takeIf { it in DownloadsWidget.RANGES }
            ?: DownloadsWidget.DEFAULT_RANGE

    fun ranges(appWidgetId: Int): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key(appWidgetId) || changed == null) trySend(rangeFor(appWidgetId))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(rangeFor(appWidgetId))
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun setRange(appWidgetId: Int, days: Int) {
        require(days in DownloadsWidget.RANGES) { "Unsupported range: $days days" }
        preferences.edit { putInt(key(appWidgetId), days) }
    }

    fun remove(appWidgetIds: IntArray) = preferences.edit { appWidgetIds.forEach { remove(key(it)) } }

    private fun key(appWidgetId: Int) = "range_$appWidgetId"

    private companion object {
        const val FILE_NAME = "ryntra_widget_ranges"
    }
}
