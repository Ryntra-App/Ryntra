package com.ryntra.mobile.preferences

import android.content.Context
import com.ryntra.shared.model.ProjectSearchHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Recent catalogue searches.
 *
 * Kept out of [RyntraPreferencesStore] on purpose: preferences are exported and imported as a
 * settings file, and what someone searched for does not belong in a file they hand to someone else.
 */
class SearchHistoryStore(context: Context) {
    private val storage = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutableHistory = MutableStateFlow(read())

    val history: StateFlow<List<String>> = mutableHistory.asStateFlow()

    fun remember(query: String) = write(ProjectSearchHistory.adding(mutableHistory.value, query))

    fun forget(query: String) = write(ProjectSearchHistory.removing(mutableHistory.value, query))

    fun clear() = write(emptyList())

    private fun write(history: List<String>) {
        if (history == mutableHistory.value) return
        mutableHistory.value = history
        storage.edit()
            .putString(KEY_QUERIES, JSONArray(history).toString())
            .apply()
    }

    private fun read(): List<String> {
        val raw = storage.getString(KEY_QUERIES, null) ?: return emptyList()
        val parsed = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until parsed.length()) {
                parsed.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }.take(ProjectSearchHistory.MAX_ENTRIES)
    }

    private companion object {
        const val FILE_NAME = "ryntra_search_history"
        const val KEY_QUERIES = "queries"
    }
}
