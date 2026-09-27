package com.ryntra.mobile.widgets

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Receives a tap on a downloads widget's range button, stores the range and closes at once.
 *
 * The buttons start this invisible activity instead of sending Glance's callback broadcast.
 * Launchers and vendor builds that restrict background broadcasts to an app whose process is
 * gone dropped those taps, while an activity started by the user's own tap is always allowed.
 */
class WidgetRangeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val days = intent.getIntExtra(EXTRA_RANGE_DAYS, 0)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || days !in DownloadsWidget.RANGES) {
            close()
            return
        }
        WidgetRangeStore(this).setRange(appWidgetId, days)
        lifecycleScope.launch {
            // A widget whose Glance session is running redraws from the store on its own;
            // this covers the one whose session has already ended.
            val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
            DownloadsWidget().update(applicationContext, glanceId)
            close()
        }
    }

    private fun close() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    internal companion object {
        /** Extra names double as Glance action parameter keys, which Glance writes as extras. */
        const val EXTRA_APP_WIDGET_ID = "app_widget_id"
        const val EXTRA_RANGE_DAYS = "range_days"
    }
}
