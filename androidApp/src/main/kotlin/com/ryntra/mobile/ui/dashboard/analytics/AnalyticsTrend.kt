package com.ryntra.mobile.ui.dashboard.analytics

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.mobile.R
import com.ryntra.shared.model.AnalyticsMetrics
import com.ryntra.shared.model.AnalyticsPoint
import com.ryntra.shared.model.AnalyticsProjectEvent
import com.ryntra.shared.model.AnalyticsReport
import com.ryntra.shared.model.Project
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private enum class TrendStyle(@StringRes val labelRes: Int) {
    Line(R.string.analytics_line),
    Area(R.string.analytics_area),
    Bar(R.string.analytics_bar),
}

private data class TrendSeries(
    val projectId: String,
    val title: String,
    val values: List<Double>,
    val color: Color,
)

private data class ProjectTrendOption(
    val project: Project,
    val total: Double,
)

@Composable
internal fun AnalyticsTrend(
    report: AnalyticsReport?,
    projects: List<Project>,
    selectedProjectId: String?,
    metric: AnalyticsMetric,
    rangeDays: Int,
    modifier: Modifier = Modifier,
) {
    val points = if (metric == AnalyticsMetric.Revenue) report?.revenuePoints.orEmpty() else report?.points.orEmpty()
    val projectOptions = remember(projects, report, metric, selectedProjectId) {
        projects
            .filter { selectedProjectId == null || it.id == selectedProjectId }
            .map { project -> ProjectTrendOption(project, metric.value(report?.projectMetrics(project.id) ?: AnalyticsMetrics())) }
            .sortedByDescending(ProjectTrendOption::total)
    }
    val defaultProjectIds = remember(projectOptions, selectedProjectId) {
        if (selectedProjectId != null) {
            projectOptions.map { it.project.id }.toSet()
        } else {
            projectOptions.filter { it.total > 0.0 }.take(6).map { it.project.id }.toSet()
        }
    }
    val projectKey = remember(projectOptions) { projectOptions.joinToString("|") { it.project.id } }
    var includedProjectIds by remember(metric, selectedProjectId, projectKey) { mutableStateOf(defaultProjectIds) }
    var style by remember { mutableStateOf(TrendStyle.Line) }
    var windowSize by remember(points.size, rangeDays) { mutableIntStateOf(points.size.coerceAtMost(30).coerceAtLeast(1)) }
    var windowStart by remember(points.size, rangeDays) { mutableIntStateOf((points.size - windowSize).coerceAtLeast(0)) }
    var selectedIndex by remember(points.size, metric) { mutableIntStateOf((points.lastIndex).coerceAtLeast(0)) }

    val selectedProjects = remember(projectOptions, includedProjectIds) {
        projectOptions.filter { it.project.id in includedProjectIds }.map(ProjectTrendOption::project)
    }
    val fullAggregateValues = remember(points, selectedProjectId, metric) {
        points.map { point ->
            if (selectedProjectId == null) metric.value(point.metrics)
            else metric.value(point.projects[selectedProjectId] ?: AnalyticsMetrics())
        }
    }
    val safeWindowSize = windowSize.coerceIn(1, points.size.coerceAtLeast(1))
    val safeWindowStart = windowStart.coerceWindowStart(safeWindowSize, points.size)
    val windowEndExclusive = (safeWindowStart + safeWindowSize).coerceAtMost(points.size)
    val visibleAggregateValues = fullAggregateValues.sliceDoublesSafe(safeWindowStart, windowEndExclusive)
    val selectedActualIndex = selectedIndex.coerceIn(safeWindowStart, (windowEndExclusive - 1).coerceAtLeast(safeWindowStart))
    val selectedWindowIndex = selectedActualIndex - safeWindowStart
    val palette = analyticsColors().series
    val projectColors = remember(selectedProjects, palette) {
        selectedProjects.associate { it.id to projectSeriesColor(it.id, palette) }
    }
    val series = remember(points, selectedProjects, metric, projectColors, safeWindowStart, windowEndExclusive) {
        selectedProjects.map { project ->
            TrendSeries(
                projectId = project.id,
                title = project.title,
                values = points
                    .slicePointsSafe(safeWindowStart, windowEndExclusive)
                    .map { point -> metric.value(point.projects[project.id] ?: AnalyticsMetrics()) },
                color = projectColors.getValue(project.id),
            )
        }
    }
    val eventIndices = remember(report?.events, report?.periodStartTime, report?.periodEndTime, selectedProjectId, points.size) {
        eventIndices(
            events = report?.events.orEmpty(),
            periodStart = report?.periodStartTime.orEmpty(),
            periodEnd = report?.periodEndTime.orEmpty(),
            selectedProjectId = selectedProjectId,
            pointCount = points.size,
        )
    }
    val visibleEventIndices = remember(eventIndices, safeWindowStart, windowEndExclusive) {
        eventIndices
            .filterKeys { it in safeWindowStart until windowEndExclusive }
            .mapKeys { (index, _) -> index - safeWindowStart }
    }
    val selectedValue = fullAggregateValues.getOrElse(selectedActualIndex) { 0.0 }
    val peak = fullAggregateValues.maxOrNull() ?: 0.0
    val hasData = fullAggregateValues.any { it > 0.0 }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surface, shape)
            .border(0.75.dp, RyntraDesign.colors.separator, shape)
            .padding(14.dp)
            .animateContentSize(),
    ) {
        TrendHeader(
            metric = metric,
            selectedValue = selectedValue,
            selectedDate = dateForIndex(selectedActualIndex, fullAggregateValues.size),
            style = style,
            onStyleChange = { style = it },
        )
        TrendWindowControls(
            windowStart = safeWindowStart,
            windowSize = safeWindowSize,
            pointCount = points.size,
            onZoomIn = {
                val nextSize = (safeWindowSize / 2).coerceAtLeast(7).coerceAtMost(points.size.coerceAtLeast(1))
                windowStart = (selectedActualIndex - nextSize / 2).coerceWindowStart(nextSize, points.size)
                windowSize = nextSize
            },
            onZoomOut = {
                val nextSize = (safeWindowSize * 2).coerceAtMost(points.size.coerceAtLeast(1))
                windowStart = (selectedActualIndex - nextSize / 2).coerceWindowStart(nextSize, points.size)
                windowSize = nextSize
            },
            onPanLeft = {
                val step = (safeWindowSize / 2).coerceAtLeast(1)
                windowStart = (safeWindowStart - step).coerceWindowStart(safeWindowSize, points.size)
            },
            onPanRight = {
                val step = (safeWindowSize / 2).coerceAtLeast(1)
                windowStart = (safeWindowStart + step).coerceWindowStart(safeWindowSize, points.size)
            },
            onShowAll = {
                windowSize = points.size.coerceAtLeast(1)
                windowStart = 0
                selectedIndex = points.lastIndex.coerceAtLeast(0)
            },
            modifier = Modifier.padding(top = 12.dp),
        )
        ProjectPickerStrip(
            options = projectOptions,
            selectedIds = includedProjectIds,
            metric = metric,
            selectedProjectId = selectedProjectId,
            onSelectTop = { includedProjectIds = projectOptions.filter { it.total > 0.0 }.take(6).map { it.project.id }.toSet() },
            onSelectAll = { includedProjectIds = projectOptions.map { it.project.id }.toSet() },
            onShowTotalOnly = { includedProjectIds = emptySet() },
            onToggle = { projectId ->
                includedProjectIds = if (projectId in includedProjectIds) {
                    includedProjectIds - projectId
                } else {
                    includedProjectIds + projectId
                }
            },
            modifier = Modifier.padding(top = 12.dp),
        )
        if (hasData) {
            TrendCanvas(
                series = series,
                aggregateValues = visibleAggregateValues,
                style = style,
                selectedWindowIndex = selectedWindowIndex,
                eventIndices = visibleEventIndices.keys,
                onSelectWindowIndex = { selectedIndex = safeWindowStart + it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(318.dp)
                    .padding(top = 14.dp),
            )
            TrendRangeFooter(
                metric = metric,
                peak = peak,
                windowStart = safeWindowStart,
                windowEndExclusive = windowEndExclusive,
                totalCount = fullAggregateValues.size,
                modifier = Modifier.padding(top = 8.dp),
            )
            TrendFocusPanel(
                metric = metric,
                series = series,
                selectedWindowIndex = selectedWindowIndex,
                totalValue = selectedValue,
                event = visibleEventIndices[selectedWindowIndex]?.firstOrNull(),
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.analytics_no_metric, stringResource(metric.labelRes).lowercase()),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 42.dp),
            )
        }
    }
}

@Composable
private fun TrendHeader(
    metric: AnalyticsMetric,
    selectedValue: Double,
    selectedDate: String,
    style: TrendStyle,
    onStyleChange: (TrendStyle) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatMetric(metric, selectedValue),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = selectedDate.ifBlank { stringResource(R.string.analytics_select_point) },
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        TrendStylePicker(style, onStyleChange)
    }
}

@Composable
private fun TrendWindowControls(
    windowStart: Int,
    windowSize: Int,
    pointCount: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPanLeft: () -> Unit,
    onPanRight: () -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        TrendControlButton("‹", enabled = windowStart > 0, onClick = onPanLeft, modifier = Modifier.weight(0.75f))
        TrendControlButton("-", enabled = windowSize < pointCount, onClick = onZoomOut, modifier = Modifier.weight(0.75f))
        TrendControlButton(
            text = if (windowSize >= pointCount) {
                stringResource(R.string.analytics_all)
            } else {
                stringResource(R.string.analytics_range_slices, windowSize)
            },
            enabled = windowSize < pointCount,
            onClick = onShowAll,
            modifier = Modifier.weight(2f),
        )
        TrendControlButton("+", enabled = windowSize > 7, onClick = onZoomIn, modifier = Modifier.weight(0.75f))
        TrendControlButton(
            "›",
            enabled = windowStart + windowSize < pointCount,
            onClick = onPanRight,
            modifier = Modifier.weight(0.75f),
        )
    }
}

@Composable
private fun TrendControlButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .background(RyntraDesign.colors.surfaceRaised, shape)
            .border(0.75.dp, RyntraDesign.colors.separator, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) RyntraDesign.colors.labelPrimary else RyntraDesign.colors.labelSecondary.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProjectPickerStrip(
    options: List<ProjectTrendOption>,
    selectedIds: Set<String>,
    metric: AnalyticsMetric,
    selectedProjectId: String?,
    onSelectTop: () -> Unit,
    onSelectAll: () -> Unit,
    onShowTotalOnly: () -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TrendPresetChip(stringResource(R.string.analytics_top), selected = selectedProjectId == null && selectedIds.size <= 6 && selectedIds.isNotEmpty(), onClick = onSelectTop)
            TrendPresetChip(stringResource(R.string.analytics_all), selected = selectedIds.size == options.size && options.isNotEmpty(), onClick = onSelectAll)
            TrendPresetChip(stringResource(R.string.analytics_total), selected = selectedIds.isEmpty(), onClick = onShowTotalOnly)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
            items(options, key = { it.project.id }) { option ->
                ProjectChip(
                    title = option.project.title,
                    value = formatMetric(metric, option.total),
                    color = analyticsSeriesColor(option.project.id),
                    selected = option.project.id in selectedIds,
                    enabled = selectedProjectId == null,
                    onClick = { onToggle(option.project.id) },
                )
            }
        }
    }
}

@Composable
private fun TrendPresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = text,
        color = if (selected) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(if (selected) RyntraDesign.colors.surfaceRaised else RyntraDesign.colors.surface, shape)
            .border(0.75.dp, if (selected) RyntraDesign.colors.accent else RyntraDesign.colors.separator, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun ProjectChip(
    title: String,
    value: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    Column(
        modifier = Modifier
            .background(if (selected) RyntraDesign.colors.surfaceRaised else RyntraDesign.colors.surface, shape)
            .border(0.75.dp, if (selected) color else RyntraDesign.colors.separator, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, RoundedCornerShape(4.dp)))
            Text(
                text = title,
                color = if (selected) RyntraDesign.colors.labelPrimary else RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Text(
            text = value,
            color = if (selected) color else RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun TrendStylePicker(selected: TrendStyle, onSelect: (TrendStyle) -> Unit) {
    Row(
        modifier = Modifier
            .background(RyntraDesign.colors.surfaceRaised, RoundedCornerShape(8.dp))
            .padding(2.dp),
    ) {
        TrendStyle.entries.forEach { style ->
            Text(
                text = stringResource(style.labelRes),
                color = if (style == selected) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (style == selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .background(
                        if (style == selected) RyntraDesign.colors.surface else Color.Transparent,
                        RoundedCornerShape(6.dp),
                    )
                    .clickable(role = Role.Tab) { onSelect(style) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun TrendCanvas(
    series: List<TrendSeries>,
    aggregateValues: List<Double>,
    style: TrendStyle,
    selectedWindowIndex: Int,
    eventIndices: Set<Int>,
    onSelectWindowIndex: (Int) -> Unit,
    modifier: Modifier,
) {
    val maximum = max(
        series.maxOfOrNull { it.values.maxOrNull() ?: 0.0 } ?: aggregateValues.maxOrNull() ?: 0.0,
        1.0,
    )
    val gridColor = RyntraDesign.colors.separator
    val labelColor = RyntraDesign.colors.labelSecondary
    val fallbackColor = RyntraDesign.colors.accent
    val pointFillColor = RyntraDesign.colors.surface
    fun selectIndex(x: Float, width: Float) {
        if (aggregateValues.isEmpty() || width <= 0f) return
        onSelectWindowIndex(((x / width) * aggregateValues.lastIndex).roundToInt().coerceIn(0, aggregateValues.lastIndex))
    }
    Canvas(
        modifier = modifier
            .pointerInput(aggregateValues.size) {
                detectTapGestures { offset -> selectIndex(offset.x, size.width.toFloat()) }
            }
            .pointerInput(aggregateValues.size) {
                detectDragGestures(
                    onDragStart = { offset -> selectIndex(offset.x, size.width.toFloat()) },
                    onDrag = { change, _ ->
                        change.consume()
                        selectIndex(change.position.x, size.width.toFloat())
                    },
                )
            },
    ) {
        repeat(5) { index ->
            val y = size.height * index / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        eventIndices.forEach { index ->
            val x = xForIndex(index, aggregateValues.size, size.width)
            drawLine(
                color = labelColor.copy(alpha = 0.50f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }
        val drawableSeries = series.ifEmpty {
            listOf(TrendSeries("total", "Total", aggregateValues, fallbackColor))
        }
        drawableSeries.forEachIndexed { seriesIndex, item ->
            when (style) {
                TrendStyle.Bar -> drawBars(item.values, item.color, seriesIndex, drawableSeries.size, maximum)
                TrendStyle.Line, TrendStyle.Area -> {
                    val line = linePath(item.values, maximum)
                    if (style == TrendStyle.Area) {
                        val area = linePath(item.values, maximum).apply {
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(area, item.color.copy(alpha = 0.16f))
                    }
                    drawPath(line, item.color, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                }
            }
        }
        if (aggregateValues.isNotEmpty()) {
            val x = xForIndex(selectedWindowIndex, aggregateValues.size, size.width)
            drawLine(labelColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
            drawableSeries.forEach { item ->
                val value = item.values.getOrElse(selectedWindowIndex) { 0.0 }
                val y = size.height - size.height * (value / maximum).toFloat()
                drawCircle(item.color, radius = 7f, center = Offset(x, y))
                drawCircle(pointFillColor, radius = 3f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun TrendRangeFooter(
    metric: AnalyticsMetric,
    peak: Double,
    windowStart: Int,
    windowEndExclusive: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = dateForIndex(windowStart, totalCount),
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = stringResource(R.string.analytics_peak, formatMetric(metric, peak)),
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = dateForIndex((windowEndExclusive - 1).coerceAtLeast(windowStart), totalCount),
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TrendFocusPanel(
    metric: AnalyticsMetric,
    series: List<TrendSeries>,
    selectedWindowIndex: Int,
    totalValue: Double,
    event: AnalyticsProjectEvent?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surfaceRaised, RoundedCornerShape(9.dp))
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.analytics_total),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMetric(metric, totalValue),
                fontWeight = FontWeight.Bold,
                color = RyntraDesign.colors.labelPrimary,
            )
        }
        series
            .map { it to it.values.getOrElse(selectedWindowIndex) { 0.0 } }
            .filter { (_, value) -> value > 0.0 }
            .sortedByDescending { (_, value) -> value }
            .take(6)
            .forEach { (item, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Box(Modifier.size(7.dp).background(item.color, RoundedCornerShape(4.dp)))
                    Text(
                        text = item.title,
                        color = RyntraDesign.colors.labelSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 7.dp, end = 8.dp),
                    )
                    Text(
                        text = formatMetric(metric, value),
                        color = item.color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        event?.let {
            Text(
                text = eventLabel(it),
                color = RyntraDesign.colors.accent,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.linePath(values: List<Double>, maximum: Double): Path = Path().apply {
    values.forEachIndexed { index, value ->
        val point = Offset(
            x = xForIndex(index, values.size, size.width),
            y = size.height - size.height * (value / maximum).toFloat(),
        )
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    values: List<Double>,
    color: Color,
    seriesIndex: Int,
    seriesCount: Int,
    maximum: Double,
) {
    if (values.isEmpty()) return
    val groupWidth = size.width / values.size
    val barWidth = (groupWidth * 0.74f / seriesCount).coerceAtLeast(1.5f)
    values.forEachIndexed { index, value ->
        val left = groupWidth * index + groupWidth * 0.13f + barWidth * seriesIndex
        val height = size.height * (value / maximum).toFloat()
        drawRect(
            color.copy(alpha = 0.86f),
            topLeft = Offset(left, size.height - height),
            size = androidx.compose.ui.geometry.Size(barWidth, height),
        )
    }
}

private fun List<AnalyticsPoint>.slicePointsSafe(start: Int, endExclusive: Int): List<AnalyticsPoint> =
    if (isEmpty()) emptyList() else subList(start.coerceIn(0, size), endExclusive.coerceIn(start.coerceIn(0, size), size))

private fun List<Double>.sliceDoublesSafe(start: Int, endExclusive: Int): List<Double> =
    if (isEmpty()) emptyList() else subList(start.coerceIn(0, size), endExclusive.coerceIn(start.coerceIn(0, size), size))

private fun Int.coerceWindowStart(windowSize: Int, pointCount: Int): Int {
    if (pointCount <= 0) return 0
    return coerceIn(0, (pointCount - windowSize).coerceAtLeast(0))
}

private fun xForIndex(index: Int, count: Int, width: Float): Float =
    if (count <= 1) width / 2f else width * index.coerceIn(0, count - 1) / (count - 1).toFloat()

private fun dateForIndex(index: Int, count: Int): String {
    if (count <= 0) return ""
    val daysAgo = (count - 1 - index.coerceIn(0, count - 1)).toLong()
    return LocalDate.now().minusDays(daysAgo).format(DateTimeFormatter.ofPattern("d MMM"))
}

private fun eventIndices(
    events: List<AnalyticsProjectEvent>,
    periodStart: String,
    periodEnd: String,
    selectedProjectId: String?,
    pointCount: Int,
): Map<Int, List<AnalyticsProjectEvent>> {
    if (pointCount <= 0) return emptyMap()
    val start = runCatching { Instant.parse(periodStart) }.getOrNull() ?: return emptyMap()
    val end = runCatching { Instant.parse(periodEnd) }.getOrNull() ?: return emptyMap()
    val totalMillis = Duration.between(start, end).toMillis().coerceAtLeast(1L)
    return events
        .filter { selectedProjectId == null || it.projectId == selectedProjectId }
        .mapNotNull { event ->
            val timestamp = runCatching { Instant.parse(event.timestamp) }.getOrNull() ?: return@mapNotNull null
            if (timestamp < start || timestamp > end) return@mapNotNull null
            val ratio = Duration.between(start, timestamp).toMillis().toDouble() / totalMillis
            (ratio * (pointCount - 1)).roundToInt().coerceIn(0, pointCount - 1) to event
        }
        .groupBy({ it.first }, { it.second })
}

private fun eventLabel(event: AnalyticsProjectEvent): String = when (event.kind) {
    "version_uploaded" -> "Release ${event.versionNumber ?: event.versionName.orEmpty()}"
    "status_changed" -> "Status ${event.statusFrom.orEmpty()} to ${event.statusTo.orEmpty()}"
    else -> event.kind.replace('_', ' ')
}
