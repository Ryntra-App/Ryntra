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
        handleAppIntent(intent)
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

    companion object {
        const val EXTRA_DEEP_LINK = "deep_link"
    }
}
