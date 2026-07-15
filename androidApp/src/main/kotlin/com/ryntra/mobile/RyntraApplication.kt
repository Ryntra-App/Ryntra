package com.ryntra.mobile

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.ryntra.mobile.preferences.AppLocale
import com.ryntra.mobile.preferences.RyntraPreferencesStore
import okhttp3.OkHttpClient

class RyntraApplication : Application(), SingletonImageLoader.Factory {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        val language = RyntraPreferencesStore(this).preferences.value.appLanguage
        AppLocale.apply(language)
    }

    /**
     * SVG support is required for Modrinth description badges (shields.io).
     * Without [SvgDecoder], badge images load as empty.
     */
    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(OkHttpClient.Builder().build()))
                add(SvgDecoder.Factory())
            }
            .build()
}
