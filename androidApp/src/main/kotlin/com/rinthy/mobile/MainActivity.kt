package com.rinthy.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rinthy.mobile.ui.RinthyApp
import com.rinthy.mobile.ui.theme.RinthyTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RinthyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RinthyTheme {
                RinthyApp(viewModel)
            }
        }
        handleOAuthIntent(intent)
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
}
