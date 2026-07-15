package com.ryntra.mobile.preferences

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import com.ryntra.shared.model.ProjectSortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class RyntraPreferences(
    val themeStyle: ThemeStyle = ThemeStyle.Platform,
    val appearanceMode: AppearanceMode = AppearanceMode.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val showFavoriteProjects: Boolean = true,
    val reduceMotion: Boolean = false,
    val glassQuality: GlassQuality = GlassQuality.Balanced,
    val projectSortMode: ProjectSortMode = ProjectSortMode.Popularity,
    val favoriteProjectIds: Set<String> = emptySet(),
)

enum class ThemeStyle(val label: String) {
    Platform("Platform"),
    Ryntra("Ryntra"),
}

enum class AppearanceMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

enum class AppLanguage(val label: String, val tag: String?) {
    // <localization-tool:android-languages>
    System("System", null),
    English("English", "en"),
    Russian("Русский", "ru"),
    // </localization-tool:android-languages>
}

enum class GlassQuality(val label: String) {
    Performance("Fast"),
    Balanced("Balanced"),
    Quality("Best"),
}

class RyntraPreferencesStore(context: Context) {
    private val storage = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutablePreferences = MutableStateFlow(readPreferences())

    val preferences: StateFlow<RyntraPreferences> = mutablePreferences.asStateFlow()

    fun setThemeStyle(style: ThemeStyle) = update { copy(themeStyle = style) }

    fun setAppearanceMode(mode: AppearanceMode) = update { copy(appearanceMode = mode) }

    fun setAppLanguage(language: AppLanguage) = update { copy(appLanguage = language) }

    fun setShowFavoriteProjects(isEnabled: Boolean) = update { copy(showFavoriteProjects = isEnabled) }

    fun setReduceMotion(isEnabled: Boolean) = update { copy(reduceMotion = isEnabled) }

    fun setGlassQuality(quality: GlassQuality) = update { copy(glassQuality = quality) }

    fun setProjectSortMode(mode: ProjectSortMode) = update { copy(projectSortMode = mode) }

    fun toggleFavoriteProject(projectId: String) {
        if (projectId.isBlank()) {
            Log.w(TAG, "toggleFavoriteProject ignored: blank id")
            return
        }
        update {
            val nextFavorites = if (projectId in favoriteProjectIds) {
                favoriteProjectIds - projectId
            } else {
                favoriteProjectIds + projectId
            }
            Log.d(TAG, "toggleFavoriteProject id=$projectId next=$nextFavorites")
            copy(favoriteProjectIds = nextFavorites)
        }
    }

    fun resetAppearance() = update {
        copy(
            themeStyle = ThemeStyle.Platform,
            appearanceMode = AppearanceMode.System,
            showFavoriteProjects = true,
            reduceMotion = false,
            glassQuality = GlassQuality.Balanced,
        )
    }

    fun exportJson(username: String, appVersion: String): String {
        val current = preferences.value
        val settings = JSONObject()
            .put(KEY_THEME_STYLE, current.themeStyle.name.lowercase())
            .put(KEY_APPEARANCE_MODE, current.appearanceMode.name.lowercase())
            .put(KEY_APP_LANGUAGE, current.appLanguage.name.lowercase())
            .put("theme", current.appearanceMode.name.lowercase())
            .put("accentColor", "#30D158")
            .put(KEY_SHOW_FAVORITES, current.showFavoriteProjects)
            .put(KEY_REDUCE_MOTION, current.reduceMotion)
            .put(KEY_GLASS_QUALITY, current.glassQuality.name.lowercase())
            .put("projectSortMode", current.projectSortMode.name.lowercase())
            .put("favoriteProjectIds", JSONArray(current.favoriteProjectIds.sorted()))

        return JSONObject()
            .put("app", "Ryntra")
            .put("appVersion", appVersion)
            .put("profile", username)
            .put("settings", settings)
            .toString(2)
    }

    fun importJson(rawJson: String): Result<Unit> = runCatching {
        val root = JSONObject(rawJson)
        val imported = root.optJSONObject("settings") ?: root
        val current = preferences.value
        val favorites = imported.optJSONArray("favoriteProjectIds")?.stringSet() ?: current.favoriteProjectIds
        val importedStyle = imported.optionalEnum<ThemeStyle>(KEY_THEME_STYLE)
            ?: ThemeStyle.Ryntra.takeIf { imported.has("theme") }
        val importedAppearance = imported.optionalEnum<AppearanceMode>(KEY_APPEARANCE_MODE)
            ?: imported.optionalEnum<AppearanceMode>("theme")

        val next = current.copy(
            themeStyle = importedStyle ?: current.themeStyle,
            appearanceMode = importedAppearance ?: current.appearanceMode,
            appLanguage = imported.optionalEnum<AppLanguage>(KEY_APP_LANGUAGE) ?: current.appLanguage,
            showFavoriteProjects = imported.optionalBoolean(KEY_SHOW_FAVORITES) ?: current.showFavoriteProjects,
            reduceMotion = imported.optionalBoolean(KEY_REDUCE_MOTION) ?: current.reduceMotion,
            glassQuality = imported.optionalEnum<GlassQuality>(KEY_GLASS_QUALITY) ?: current.glassQuality,
            projectSortMode = imported.optionalEnum<ProjectSortMode>("projectSortMode") ?: current.projectSortMode,
            favoriteProjectIds = favorites,
        )
        persist(next)
    }

    private fun update(transform: RyntraPreferences.() -> RyntraPreferences) {
        persist(preferences.value.transform())
    }

    private fun persist(value: RyntraPreferences) {
        // Store favorites as a JSON array string — putStringSet is unreliable across processes/reloads.
        val favoritesJson = JSONArray(value.favoriteProjectIds.sorted()).toString()
        val written = storage.edit()
            .putString(KEY_THEME_STYLE, value.themeStyle.name)
            .putString(KEY_APPEARANCE_MODE, value.appearanceMode.name)
            .putString(KEY_APP_LANGUAGE, value.appLanguage.name)
            .putBoolean(KEY_SHOW_FAVORITES, value.showFavoriteProjects)
            .putBoolean(KEY_REDUCE_MOTION, value.reduceMotion)
            .putString(KEY_GLASS_QUALITY, value.glassQuality.name)
            .putString(KEY_PROJECT_SORT_MODE, value.projectSortMode.name)
            .putString(KEY_FAVORITE_PROJECT_IDS_JSON, favoritesJson)
            .remove(KEY_FAVORITE_PROJECT_IDS)
            .commit()
        if (!written) {
            Log.e(TAG, "Failed to persist preferences")
        }
        mutablePreferences.value = value
    }

    private fun readPreferences() = RyntraPreferences(
        themeStyle = storage.enumValue(KEY_THEME_STYLE, ThemeStyle.Platform),
        appearanceMode = storage.enumValue(KEY_APPEARANCE_MODE, AppearanceMode.System),
        appLanguage = storage.enumValue(KEY_APP_LANGUAGE, AppLanguage.System),
        showFavoriteProjects = storage.getBoolean(KEY_SHOW_FAVORITES, true),
        reduceMotion = storage.getBoolean(KEY_REDUCE_MOTION, false),
        glassQuality = storage.enumValue(KEY_GLASS_QUALITY, GlassQuality.Balanced),
        projectSortMode = storage.enumValue(KEY_PROJECT_SORT_MODE, ProjectSortMode.Popularity),
        favoriteProjectIds = readFavoriteProjectIds(),
    )

    private fun readFavoriteProjectIds(): Set<String> {
        val json = storage.getString(KEY_FAVORITE_PROJECT_IDS_JSON, null)
        if (!json.isNullOrBlank()) {
            return runCatching { JSONArray(json).stringSet() }.getOrDefault(emptySet())
        }
        // Migrate legacy StringSet storage if present.
        return storage.getStringSet(KEY_FAVORITE_PROJECT_IDS, null)?.let { HashSet(it) } ?: emptySet()
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumValue(key: String, fallback: T): T =
        getString(key, null)?.let { stored ->
            enumValues<T>().firstOrNull { it.name.equals(stored, ignoreCase = true) }
        } ?: fallback

    private inline fun <reified T : Enum<T>> JSONObject.optionalEnum(key: String): T? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return enumValues<T>().firstOrNull {
            it.name.equals(value, ignoreCase = true) || it.toString().equals(value, ignoreCase = true)
        }
    }

    private fun JSONObject.optionalBoolean(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null

    private fun JSONArray.stringSet(): Set<String> = buildSet {
        for (index in 0 until length()) {
            optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private companion object {
        const val TAG = "RyntraFavorites"
        const val FILE_NAME = "ryntra_preferences"
        const val KEY_THEME_STYLE = "themeStyle"
        const val KEY_APPEARANCE_MODE = "appearanceMode"
        const val KEY_APP_LANGUAGE = "appLanguage"
        const val KEY_SHOW_FAVORITES = "showFavoriteProjects"
        const val KEY_REDUCE_MOTION = "reduceMotion"
        const val KEY_GLASS_QUALITY = "glassQuality"
        const val KEY_PROJECT_SORT_MODE = "projectSortMode"
        const val KEY_FAVORITE_PROJECT_IDS = "favoriteProjectIds"
        const val KEY_FAVORITE_PROJECT_IDS_JSON = "favoriteProjectIdsJson"
    }
}
