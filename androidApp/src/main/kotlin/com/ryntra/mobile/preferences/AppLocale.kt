package com.ryntra.mobile.preferences

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocale {
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(localesFor(language))
    }

    /**
     * Returns a context that resolves strings/resources in [language] while keeping the original
     * base identity (Activity / Service). Never use [Context.createConfigurationContext] as
     * [androidx.compose.ui.platform.LocalContext] — it drops ActivityResultRegistryOwner and
     * crashes export/import launchers on the profile screen.
     */
    fun wrap(base: Context, language: AppLanguage = languageFromStorage(base)): Context {
        val tag = language.tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return try {
            // Prefer AppCompat override so resources stay tied to the Activity theme/base.
            object : ContextThemeWrapper(base, base.theme) {
                init {
                    applyOverrideConfiguration(config)
                }
            }
        } catch (_: Exception) {
            // Fallback: thin wrapper that only overrides resources.
            val localized = base.createConfigurationContext(config)
            object : ContextWrapper(base) {
                override fun getResources() = localized.resources
                override fun getAssets() = localized.assets
            }
        }
    }

    fun languageFromStorage(context: Context): AppLanguage {
        // During Application.attachBaseContext, applicationContext is still null.
        val stored = runCatching {
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                .getString(KEY_APP_LANGUAGE, AppLanguage.System.name)
        }.getOrNull()
        return AppLanguage.entries.firstOrNull { it.name.equals(stored, ignoreCase = true) }
            ?: AppLanguage.System
    }

    fun localesFor(language: AppLanguage): LocaleListCompat = when (val tag = language.tag) {
        null -> LocaleListCompat.getEmptyLocaleList()
        else -> LocaleListCompat.forLanguageTags(tag)
    }

    private const val PREFS_FILE = "ryntra_preferences"
    private const val KEY_APP_LANGUAGE = "appLanguage"
}
