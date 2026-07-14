package com.rinthy.mobile.ui.dashboard.projects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rinthy.mobile.ui.components.formatExactCount
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.rinthySegmentedButtonColors
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.ProjectSortMode

@Composable
internal fun ProjectSummaryBand(
    projectCount: Int,
    downloads: Long,
    followers: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surface)
            .padding(vertical = 13.dp),
    ) {
        SummaryMetric(projectCount.toString(), stringResource(R.string.analytics_projects), Modifier.weight(1f))
        SummaryMetric(formatExactCount(downloads), stringResource(R.string.analytics_downloads), Modifier.weight(1f))
        SummaryMetric(formatExactCount(followers), stringResource(R.string.analytics_followers), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun ProjectSortControl(
    selected: ProjectSortMode,
    onSelect: (ProjectSortMode) -> Unit,
) {
    if (RinthyDesign.isPlatformNative) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ProjectSortMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ProjectSortMode.entries.size),
                    colors = rinthySegmentedButtonColors(),
                    label = { Text(projectSortLabel(mode), maxLines = 1) },
                )
            }
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surface)
            .padding(3.dp),
    ) {
        ProjectSortMode.entries.forEach { mode ->
            SortSegment(
                label = projectSortLabel(mode),
                isSelected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SortSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) RinthyDesign.colors.surfaceRaised else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(RinthyDesign.motion.duration(160)),
        label = "Project sort background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary,
        animationSpec = tween(RinthyDesign.motion.duration(160)),
        label = "Project sort content",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun projectSortLabel(mode: ProjectSortMode): String = stringResource(
    when (mode) {
        ProjectSortMode.Popularity -> R.string.projects_sort_popular
        ProjectSortMode.Updated -> R.string.projects_sort_updated
        ProjectSortMode.Title -> R.string.projects_sort_title
        ProjectSortMode.Followers -> R.string.projects_sort_followers
    },
)
