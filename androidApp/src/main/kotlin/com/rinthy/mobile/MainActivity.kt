package com.rinthy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinthy.mobile.ui.RinthyApp
import com.rinthy.mobile.ui.theme.RinthyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RinthyTheme {
                val viewModel: RinthyViewModel = viewModel()
                RinthyApp(viewModel)
            }
        }
    }
}
