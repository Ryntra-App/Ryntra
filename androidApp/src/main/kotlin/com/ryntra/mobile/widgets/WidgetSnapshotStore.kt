package com.ryntra.mobile.widgets

import android.content.Context
import androidx.core.content.edit
import com.ryntra.shared.model.WidgetSnapshot

/**
 * The last data the widgets were given. A widget renders from here straight away and never
 * waits on the network; refreshes overwrite it.
 */
internal class WidgetSnapshotStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): WidgetSnapshot? = WidgetSnapshot.fromJson(preferences.getString(KEY_SNAPSHOT, null))

    fun write(snapshot: WidgetSnapshot) = preferences.edit { putString(KEY_SNAPSHOT, snapshot.toJson()) }

    /** On sign-out, so a widget does not keep showing the previous account's money. */
    fun clear() = preferences.edit { remove(KEY_SNAPSHOT) }

    private companion object {
        const val FILE_NAME = "ryntra_widgets"
        const val KEY_SNAPSHOT = "snapshot"
    }
}
