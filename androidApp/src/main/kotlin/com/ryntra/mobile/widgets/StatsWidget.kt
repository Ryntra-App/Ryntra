package com.ryntra.mobile.widgets

import android.content.BroadcastReceiver
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import com.ryntra.mobile.AppScreenRequest
import com.ryntra.mobile.R
import com.ryntra.shared.model.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lifetime totals. Small is downloads alone — the number creators open the app for —
 * medium adds followers and projects, and large lists the most downloaded projects.
 */
class StatsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSize.all)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = withContext(Dispatchers.IO) {
            WidgetSnapshotStore(context).read().also { RyntraWidgets.ensureFresh(context, it) }
        }
        provideContent {
            val snapshot = rememberWidgetSnapshot(context, initial)
            GlanceTheme {
                WidgetCard(context, AppScreenRequest.Analytics) {
                    WidgetHeader(snapshot?.username ?: context.getString(R.string.app_name))
                    Spacer(GlanceModifier.defaultWeight())
                    if (snapshot == null) {
                        WidgetSignedOut(context)
                    } else {
                        when (WidgetSize.current()) {
                            WidgetSize.Small -> Downloads(context, snapshot, size = 24)
                            WidgetSize.Medium -> {
                                Downloads(context, snapshot, size = 28)
                                Totals(context, snapshot)
                            }
                            WidgetSize.Large -> {
                                Downloads(context, snapshot, size = 32)
                                Totals(context, snapshot)
                                snapshot.topProjects.forEach { project ->
                                    WidgetRow(project.title, widgetCount(project.downloads))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Downloads(context: Context, snapshot: WidgetSnapshot, size: Int) {
        WidgetFigure(widgetCount(snapshot.downloads), size = size)
        WidgetCaption(context.getString(R.string.widget_stats_downloads))
    }

    @Composable
    private fun Totals(context: Context, snapshot: WidgetSnapshot) {
        Row(modifier = GlanceModifier.padding(top = 8.dp)) {
            Total(widgetCount(snapshot.followers), context.getString(R.string.widget_stats_followers))
            Total(snapshot.projectCount.toString(), context.getString(R.string.widget_stats_projects))
        }
    }

    @Composable
    private fun Total(value: String, label: String) {
        Column(modifier = GlanceModifier.padding(end = 20.dp)) {
            WidgetFigure(value, size = 16)
            WidgetCaption(label)
        }
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RyntraWidgets.scheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelRefreshIfUnused(context)
    }
}

/** The last Ryntra widget of any kind removed stops the hourly refresh. */
internal fun BroadcastReceiver.cancelRefreshIfUnused(context: Context) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            RyntraWidgets.cancelRefreshIfUnused(context.applicationContext)
        } finally {
            pending.finish()
        }
    }
}
