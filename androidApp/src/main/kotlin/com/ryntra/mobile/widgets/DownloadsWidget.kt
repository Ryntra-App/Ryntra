package com.ryntra.mobile.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ryntra.mobile.AppScreenRequest
import com.ryntra.mobile.R
import com.ryntra.shared.model.DownloadWindow
import com.ryntra.shared.model.WidgetSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Downloads over the last 7, 30 or 90 days, picked on the widget itself. Each placed widget
 * keeps its own range, so two of them can show a week and a quarter side by side.
 */
class DownloadsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSize.all)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { WidgetSnapshotStore(context).read() }
        provideContent {
            GlanceTheme {
                val days = currentState(RANGE_KEY)?.takeIf { it in RANGES } ?: DEFAULT_RANGE
                val size = WidgetSize.current()
                WidgetCard(context, AppScreenRequest.Analytics) {
                    Header(context, days, size)
                    Spacer(GlanceModifier.defaultWeight())
                    val window = snapshot?.downloadWindow(days)
                    when {
                        snapshot == null -> WidgetSignedOut(context)
                        window == null -> WidgetCaption(context.getString(R.string.widget_downloads_loading))
                        size == WidgetSize.Small -> SmallChart(context, window)
                        size == WidgetSize.Medium -> MediumChart(context, window)
                        else -> LargeChart(context, window, snapshot)
                    }
                }
            }
        }
    }

    @Composable
    private fun Header(context: Context, days: Int, size: WidgetSize) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Box(modifier = GlanceModifier.defaultWeight()) {
                WidgetHeader(context.getString(R.string.widget_downloads_title))
            }
            if (size == WidgetSize.Small) {
                // No room for three chips: one shows the range and steps to the next.
                RangeChip(context, days, selected = true, target = RANGES[(RANGES.indexOf(days) + 1) % RANGES.size])
            } else {
                RANGES.forEach { range -> RangeChip(context, range, selected = range == days, target = range) }
            }
        }
    }

    @Composable
    private fun RangeChip(context: Context, days: Int, selected: Boolean, target: Int) {
        val colors = GlanceTheme.colors
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .padding(start = 4.dp)
                .cornerRadius(12.dp)
                .background(if (selected) colors.primaryContainer else colors.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(actionRunCallback<SelectRangeAction>(actionParametersOf(RANGE_PARAMETER to target))),
        ) {
            Text(
                text = context.getString(R.string.widget_range_days, days),
                style = TextStyle(
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    @Composable
    private fun SmallChart(context: Context, window: DownloadWindow) {
        WidgetFigure(widgetCount(window.total), size = 22)
        Chart(context, window, widthDp = LocalSize.current.width.value - 32, heightDp = 26f)
    }

    @Composable
    private fun MediumChart(context: Context, window: DownloadWindow) {
        val size = LocalSize.current
        Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.width(((size.width.value - 32) * 0.4f).dp)) {
                WidgetFigure(widgetCount(window.total), size = 26)
                Change(context, window)
            }
            Chart(
                context,
                window,
                widthDp = (size.width.value - 32) * 0.6f,
                heightDp = (size.height.value - 32 - HEADER_HEIGHT).coerceAtLeast(24f),
            )
        }
    }

    @Composable
    private fun LargeChart(context: Context, window: DownloadWindow, snapshot: WidgetSnapshot) {
        val size = LocalSize.current
        WidgetFigure(widgetCount(window.total), size = 30)
        Change(context, window)
        Spacer(GlanceModifier.height(8.dp))
        Chart(
            context,
            window,
            widthDp = size.width.value - 32,
            heightDp = (size.height.value - 32 - HEADER_HEIGHT - FIGURE_HEIGHT - AXIS_HEIGHT).coerceAtLeast(40f),
        )
        AxisLabels(window, snapshot)
    }

    @Composable
    private fun Change(context: Context, window: DownloadWindow) {
        val change = window.changePercent ?: run {
            WidgetCaption(context.getString(R.string.widget_downloads_period, window.days))
            return
        }
        val rounded = change.roundToInt()
        val text = context.getString(
            R.string.widget_downloads_change,
            (if (rounded >= 0) "+" else "−") + abs(rounded) + "%",
            window.days,
        )
        WidgetCaption(text, color = if (rounded >= 0) GlanceTheme.colors.primary else GlanceTheme.colors.error)
    }

    /** First and last day of the window, so the chart has a scale without gridlines. */
    @Composable
    private fun AxisLabels(window: DownloadWindow, snapshot: WidgetSnapshot) {
        val end = snapshot.dailyDownloadsUpdatedAtEpochMillis
            ?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate() } ?: return
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(modifier = GlanceModifier.defaultWeight()) {
                WidgetCaption(end.minusDays(window.days - 1L).format(formatter))
            }
            WidgetCaption(end.format(formatter))
        }
    }

    @Composable
    private fun Chart(context: Context, window: DownloadWindow, widthDp: Float, heightDp: Float) {
        val bitmap = DownloadsChart.render(
            values = window.values,
            widthDp = widthDp,
            heightDp = heightDp,
            density = context.resources.displayMetrics.density,
            lineColor = GlanceTheme.colors.primary.getColor(context).toArgb(),
            trackColor = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
        )
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = context.getString(R.string.widget_downloads_chart, window.days, widgetCount(window.total)),
            modifier = GlanceModifier.width(widthDp.dp).height(heightDp.dp),
        )
    }

    internal companion object {
        val RANGES = listOf(7, 30, 90)
        const val DEFAULT_RANGE = 30
        val RANGE_KEY: Preferences.Key<Int> = intPreferencesKey("range_days")
        val RANGE_PARAMETER = ActionParameters.Key<Int>("range_days")

        // Heights, in dp, of what sits above and below the chart in each layout.
        const val HEADER_HEIGHT = 28f
        const val FIGURE_HEIGHT = 56f
        const val AXIS_HEIGHT = 22f
    }
}

/** Stores the range picked on one widget and redraws only that widget. */
class SelectRangeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val days = parameters[DownloadsWidget.RANGE_PARAMETER] ?: return
        updateAppWidgetState(context, glanceId) { state -> state[DownloadsWidget.RANGE_KEY] = days }
        DownloadsWidget().update(context, glanceId)
    }
}

class DownloadsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DownloadsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RyntraWidgets.scheduleRefresh(context)
        // The daily series is only loaded by the background refresh; do not make a new
        // chart widget wait up to an hour for its first data.
        RyntraWidgets.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelRefreshIfUnused(context)
    }
}
