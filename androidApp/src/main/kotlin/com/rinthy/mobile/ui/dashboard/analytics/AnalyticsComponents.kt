package com.rinthy.mobile.ui.dashboard.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Check
import com.rinthy.mobile.R
import com.composables.icons.lucide.Lucide
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.theme.RinthyDesign

internal data class MetricValue(
    val metric: AnalyticsMetric?,
    val icon: ImageVector,
    val label: String,
    val value: String,
    val change: Double? = null,
)

@Composable
internal fun AnalyticsMetricRow(
    first: MetricValue,
    second: MetricValue,
    modifier: Modifier = Modifier,
    selectedMetric: AnalyticsMetric? = null,
    onMetricSelect: ((AnalyticsMetric) -> Unit)? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        AnalyticsMetricCard(first, Modifier.weight(1f), selectedMetric, onMetricSelect)
        AnalyticsMetricCard(second, Modifier.weight(1f), selectedMetric, onMetricSelect)
    }
}

@Composable
private fun AnalyticsMetricCard(
    metric: MetricValue,
    modifier: Modifier,
    selectedMetric: AnalyticsMetric?,
    onMetricSelect: ((AnalyticsMetric) -> Unit)?,
) {
    val metricType = metric.metric
    val isSelected = metricType != null && selectedMetric == metricType
    val metricColor = metricType?.color() ?: RinthyDesign.colors.labelSecondary
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .background(if (isSelected) RinthyDesign.colors.surfaceRaised else RinthyDesign.colors.surface, shape)
            .border(
                width = if (isSelected) 1.25.dp else 0.75.dp,
                color = if (isSelected) metricColor else RinthyDesign.colors.separator,
                shape = shape,
            )
            .then(
                if (metricType != null && onMetricSelect != null) Modifier.clickable { onMetricSelect(metricType) }
                else Modifier,
            )
            .padding(horizontal = 13.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RinthyIcon(metric.icon, null, metricColor, Modifier.size(18.dp))
            Text(
                metric.label,
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        metric.change?.let { change ->
            val color = when {
                change > 0.0 -> RinthyDesign.colors.positive
                change < 0.0 -> MaterialTheme.colorScheme.error
                else -> RinthyDesign.colors.labelSecondary
            }
            Text(
                text = stringResource(R.string.analytics_vs_previous, change),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
internal fun ProjectInsightRow(
    insight: ProjectInsight,
    metric: AnalyticsMetric,
    maximum: Double,
) {
    val projectColor = analyticsSeriesColor(insight.project.id)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        AsyncImage(
            model = insight.project.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RinthyDesign.colors.surface),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    insight.project.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatMetric(metric, metric.value(insight.metrics)),
                    color = projectColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(5.dp)
                    .background(RinthyDesign.colors.surfaceRaised, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((metric.value(insight.metrics) / maximum.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f))
                        .height(5.dp)
                        .background(projectColor, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
internal fun HealthRow(icon: ImageVector, label: String, detail: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        RinthyIcon(
            icon = if (count == 0) Lucide.Check else icon,
            contentDescription = null,
            tint = if (count == 0) RinthyDesign.colors.positive else RinthyDesign.colors.warning,
            modifier = Modifier.size(19.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(detail, color = RinthyDesign.colors.labelSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(
            count.toString(),
            color = if (count == 0) RinthyDesign.colors.positive else RinthyDesign.colors.labelPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
internal fun AnalyticsNotice(message: String) {
    Text(
        text = message,
        color = RinthyDesign.colors.labelSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(8.dp))
            .padding(12.dp),
    )
}
