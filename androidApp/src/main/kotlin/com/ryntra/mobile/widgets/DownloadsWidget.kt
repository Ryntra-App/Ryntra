package com.ryntra.mobile.widgets

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.LocalGlanceId
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
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
 * keeps its own range in [WidgetRangeStore], so two of them can show a week and a quarter
 * side by side.
 *
 * Small is the total and its trend; medium and large add the chart.
 */
class DownloadsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSize.all)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = withContext(Dispatchers.IO) {
            WidgetSnapshotStore(context).read().also { RyntraWidgets.ensureFresh(context, it) }
        }
        provideContent {
            val snapshot = rememberWidgetSnapshot(context, initial)
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(LocalGlanceId.current)
            val days = rememberRange(context, appWidgetId)
            GlanceTheme {
                val size = WidgetSize.current()
                // Only the body opens the app, so the range buttons above it get their taps.
                WidgetCard(context, destination = null) {
                    Header(context, appWidgetId, days, size)
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .clickable(openAppAction(context, AppScreenRequest.Analytics)),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val window = snapshot?.downloadWindow(days)
                        when {
                            snapshot == null -> WidgetSignedOut(context)
                            window == null -> WidgetCaption(context.getString(R.string.widget_downloads_loading))
                            size == WidgetSize.Small -> SmallDownloads(context, window)
                            size == WidgetSize.Medium -> MediumDownloads(context, window)
                            else -> LargeDownloads(context, window, snapshot)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Header(context: Context, appWidgetId: Int, days: Int, size: WidgetSize) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Box(modifier = GlanceModifier.defaultWeight()) {
                WidgetHeader(context.getString(R.string.widget_downloads_title))
            }
            if (size == WidgetSize.Small) {
                // No room for three buttons: one shows the range and steps to the next.
                val next = RANGES[(RANGES.indexOf(days) + 1) % RANGES.size]
                RangeButton(context, appWidgetId, days, isSelected = true, target = next)
            } else {
                RANGES.forEach { range ->
                    RangeButton(context, appWidgetId, range, isSelected = range == days, target = range)
                }
            }
        }
    }

    /** 32dp tall, like a Material filter chip; anything smaller is hard to hit on a home screen. */
    @Composable
    private fun RangeButton(context: Context, appWidgetId: Int, days: Int, isSelected: Boolean, target: Int) {
        val colors = GlanceTheme.colors
        Box(modifier = GlanceModifier.padding(start = 4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .height(32.dp)
                    .cornerRadius(16.dp)
                    .background(if (isSelected) colors.primaryContainer else colors.surfaceVariant)
                    .clickable(
                        actionStartActivity<WidgetRangeActivity>(
                            actionParametersOf(APP_WIDGET_ID_PARAMETER to appWidgetId, RANGE_PARAMETER to target),
                        ),
                    )
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = context.getString(R.string.widget_range_days, days),
                    style = TextStyle(
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }

    @Composable
    private fun rememberRange(context: Context, appWidgetId: Int): Int {
        val store = remember { WidgetRangeStore(context) }
        val ranges = remember(appWidgetId) { store.ranges(appWidgetId) }
        val days by ranges.collectAsState(store.rangeFor(appWidgetId))
        return days
    }

    @Composable
    private fun SmallDownloads(context: Context, window: DownloadWindow) {
        WidgetFigure(widgetCount(window.total), size = 26)
        Change(context, window)
    }

    @Composable
    private fun MediumDownloads(context: Context, window: DownloadWindow) {
        val size = LocalSize.current
        val contentWidth = size.width.value - CARD_PADDING
        Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.width((contentWidth * 0.4f).dp)) {
                WidgetFigure(widgetCount(window.total), size = 26)
                Change(context, window)
            }
            Chart(
                context,
                window,
                widthDp = contentWidth * 0.6f,
                heightDp = (size.height.value - CARD_PADDING - HEADER_HEIGHT).coerceAtLeast(MIN_CHART_HEIGHT),
            )
        }
    }

    @Composable
    private fun LargeDownloads(context: Context, window: DownloadWindow, snapshot: WidgetSnapshot) {
        val size = LocalSize.current
        WidgetFigure(widgetCount(window.total), size = 30)
        Change(context, window)
        Chart(
            context,
            window,
            widthDp = size.width.value - CARD_PADDING,
            heightDp = (size.height.value - CARD_PADDING - HEADER_HEIGHT - FIGURE_HEIGHT - AXIS_HEIGHT)
                .coerceAtLeast(MIN_CHART_HEIGHT),
            modifier = GlanceModifier.padding(top = 8.dp),
        )
        AxisLabels(window, snapshot)
    }

    @Composable
    private fun Change(context: Context, window: DownloadWindow) {
        val change = window.changePercent
        if (change == null) {
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
    private fun Chart(
        context: Context,
        window: DownloadWindow,
        widthDp: Float,
        heightDp: Float,
        modifier: GlanceModifier = GlanceModifier,
    ) {
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
            modifier = modifier.width(widthDp.dp).height(heightDp.dp),
        )
    }

    internal companion object {
        val RANGES = listOf(7, 30, 90)
        const val DEFAULT_RANGE = 30
        private val APP_WIDGET_ID_PARAMETER = ActionParameters.Key<Int>(WidgetRangeActivity.EXTRA_APP_WIDGET_ID)
        private val RANGE_PARAMETER = ActionParameters.Key<Int>(WidgetRangeActivity.EXTRA_RANGE_DAYS)

        // Sizes, in dp, of what surrounds the chart in each layout.
        const val CARD_PADDING = 32f
        const val HEADER_HEIGHT = 44f
        const val FIGURE_HEIGHT = 56f
        const val AXIS_HEIGHT = 22f
        const val MIN_CHART_HEIGHT = 40f
    }
}

class DownloadsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DownloadsWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetRangeStore(context).remove(appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelRefreshIfUnused(context)
    }
}
