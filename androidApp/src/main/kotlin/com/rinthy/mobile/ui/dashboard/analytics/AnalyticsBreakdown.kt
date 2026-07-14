package com.rinthy.mobile.ui.dashboard.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.theme.RinthyDesign

@Composable
internal fun AnalyticsBreakdownRow(
    insight: ProjectInsight,
    previousMetrics: com.rinthy.shared.model.AnalyticsMetrics,
    metric: AnalyticsMetric,
    share: Double,
) {
    val projectColor = analyticsSeriesColor(insight.project.id)
    val current = metric.value(insight.metrics)
    val previous = metric.value(previousMetrics)
    val change = when {
        previous == 0.0 && current == 0.0 -> 0.0
        previous == 0.0 -> 100.0
        else -> ((current - previous) / previous) * 100.0
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        AsyncImage(
            model = insight.project.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RinthyDesign.colors.surfaceRaised),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(
                text = insight.project.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatMetric(AnalyticsMetric.Views, insight.metrics.views)} ${stringResource(R.string.analytics_views).lowercase()}  ·  " +
                    "${formatMetric(AnalyticsMetric.Downloads, insight.metrics.downloads)} ${stringResource(R.string.analytics_downloads).lowercase()}",
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = "${formatMetric(AnalyticsMetric.Revenue, insight.metrics.revenue)}  ·  " +
                    formatMetric(AnalyticsMetric.Playtime, insight.metrics.playtimeSeconds),
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(82.dp)) {
            Text(
                text = formatMetric(metric, current),
                color = projectColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.analytics_change_share, change, share * 100.0),
                color = if (change >= 0.0) RinthyDesign.colors.positive else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
