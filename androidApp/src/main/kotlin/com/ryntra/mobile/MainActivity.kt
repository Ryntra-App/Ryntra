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
        handleOAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post(::preferHighestRefreshRate)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val callbackUri = intent?.data ?: return
        viewModel.handleOAuthCallback(callbackUri)
        setIntent(Intent(intent).setData(null))
    }

    private fun preferHighestRefreshRate() {
        val display = window.decorView.display ?: return
        val currentMode = display.mode
        val preferredMode = display.supportedModes
            .asSequence()
            .filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: return
        if (
            window.attributes.preferredDisplayModeId == preferredMode.modeId &&
            window.attributes.preferredRefreshRate == preferredMode.refreshRate
        ) return

        window.attributes = window.attributes.apply {
            preferredDisplayModeId = preferredMode.modeId
            preferredRefreshRate = preferredMode.refreshRate
        }
    }
}
