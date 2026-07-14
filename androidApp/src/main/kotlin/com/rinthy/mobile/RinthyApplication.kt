package com.rinthy.mobile

import android.app.Application
import android.content.Context
import com.rinthy.mobile.preferences.AppLocale
import com.rinthy.mobile.preferences.RinthyPreferencesStore

class RinthyApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        val language = RinthyPreferencesStore(this).preferences.value.appLanguage
        AppLocale.apply(language)
    }
}
