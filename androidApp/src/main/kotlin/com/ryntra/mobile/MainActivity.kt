package com.ryntra.mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ryntra.mobile.preferences.AppLocale
import com.ryntra.mobile.ui.RyntraApp
import com.ryntra.shared.model.ModrinthNotificationLink

/**
 * Must be an [AppCompatActivity] so [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]
 * recreates the activity and reloads string resources.
 */
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<RyntraViewModel>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLocale.apply(AppLocale.languageFromStorage(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RyntraApp(viewModel)
        }
        window.decorView.post(::preferHighestRefreshRate)
        handleAppIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post(::preferHighestRefreshRate)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppIntent(intent)
    }

    private fun handleAppIntent(intent: Intent?) {
        intent ?: return
        intent.data?.let(viewModel::handleOAuthCallback)
        intent.getStringExtra(EXTRA_DEEP_LINK)
            ?.let(ModrinthNotificationLink::parse)
            ?.projectIdOrSlug
            ?.let(viewModel::openNotificationProject)
        val sanitizedIntent = Intent(intent).setData(null)
        sanitizedIntent.removeExtra(EXTRA_DEEP_LINK)
        setIntent(sanitizedIntent)
    }

    // Some non-ARR OEM displays ignore Compose's high frame-rate vote and pin the app to 60 Hz.
    // A window-level preference lets the system select a faster mode without forcing a resolution.
    private fun preferHighestRefreshRate() {
        val display = window.decorView.display ?: return
        val highestRefreshRate = display.supportedModes.maxOfOrNull { it.refreshRate } ?: return
        if (window.attributes.preferredRefreshRate == highestRefreshRate) return

        window.attributes = window.attributes.apply {
            preferredRefreshRate = highestRefreshRate
        }
    }

    companion object {
        const val EXTRA_DEEP_LINK = "deep_link"
    }
}
