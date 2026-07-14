package com.rinthy.mobile.ui.dashboard.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Layers
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyProgressIndicator
import com.rinthy.mobile.ui.theme.RinthyDesign

import com.rinthy.shared.model.Project

private val analyticsRanges = listOf(7, 30, 90, 180)

@Composable
internal fun AnalyticsRangeHeader(
    selectedDays: Int,
    isLoading: Boolean,
    isLive: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.analytics_performance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                if (isLoading) {
                    RinthyProgressIndicator(RinthyDesign.colors.accent, Modifier.size(13.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (isLive) RinthyDesign.colors.positive else RinthyDesign.colors.warning,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
                Text(
                    text = when {
                        isLoading -> stringResource(R.string.analytics_refreshing)
                        isLive -> stringResource(R.string.analytics_live)
                        else -> stringResource(R.string.analytics_limited)
                    },
                    color = RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        RangePicker(selectedDays, onSelect)
    }
}

@Composable
internal fun AnalyticsProjectPicker(
    projects: List<Project>,
    selectedProjectId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(RinthyDesign.colors.surface, RoundedCornerShape(9.dp))
                .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(9.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            RinthyIcon(Lucide.Layers, null, RinthyDesign.colors.accent, Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    stringResource(R.string.analytics_filter_project),
                    color = RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    selectedProject?.title ?: stringResource(R.string.analytics_all_projects),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            RinthyIcon(Lucide.ChevronDown, null, RinthyDesign.colors.labelSecondary, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_all_projects)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.title, maxLines = 1) },
                    onClick = {
                        onSelect(project.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RangePicker(selectedDays: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = Modifier
            .background(RinthyDesign.colors.surface, shape)
            .border(0.75.dp, RinthyDesign.colors.separator, shape)
            .padding(3.dp),
    ) {
        analyticsRanges.forEach { days ->
            val isSelected = selectedDays == days
            val background by animateColorAsState(
                targetValue = if (isSelected) RinthyDesign.colors.surfaceRaised else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = tween(durationMillis = RinthyDesign.motion.duration(180)),
                label = "Analytics range background",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary,
                animationSpec = tween(durationMillis = RinthyDesign.motion.duration(180)),
                label = "Analytics range content",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(34.dp)
                    .background(background, RoundedCornerShape(7.dp))
                    .clickable(role = Role.Tab) { onSelect(days) }
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.analytics_days_short, days),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun AnalyticsMetricPicker(selected: AnalyticsMetric, onSelect: (AnalyticsMetric) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AnalyticsMetric.entries.forEachIndexed { index, metric ->
            val icon = when (metric) {
                AnalyticsMetric.Downloads -> Lucide.Download
                AnalyticsMetric.Views -> Lucide.Eye
                AnalyticsMetric.Playtime -> Lucide.Timer
                AnalyticsMetric.Revenue -> Lucide.Wallet
            }
            val isSelected = selected == metric
            val background by animateColorAsState(
                targetValue = if (isSelected) RinthyDesign.colors.surfaceRaised else RinthyDesign.colors.surface,
                animationSpec = tween(durationMillis = RinthyDesign.motion.duration(180)),
                label = "Analytics metric background",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) metric.color() else RinthyDesign.colors.labelSecondary,
                animationSpec = tween(durationMillis = RinthyDesign.motion.duration(180)),
                label = "Analytics metric content",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (index == 0) 0.dp else 6.dp)
                    .height(42.dp)
                    .background(background, RoundedCornerShape(8.dp))
                    .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Tab) { onSelect(metric) },
            ) {
                RinthyIcon(
                    icon = icon,
                    contentDescription = stringResource(metric.labelRes),
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
