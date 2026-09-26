package com.ryntra.mobile.ui.dashboard.projects

import com.ryntra.mobile.ui.components.RyntraChoiceGroup
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ProjectSortMode
import com.ryntra.mobile.ui.components.ExpressiveStatTile
import com.ryntra.mobile.ui.components.StatTone
import androidx.compose.foundation.layout.Arrangement
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Lucide

@Composable
internal fun ProjectSummaryBand(
    projectCount: Int,
    downloads: Long,
    followers: Long,
) {
    if (RyntraDesign.isPlatformNative) {
        // Downloads are the figure people open this tab for, so it takes the full width;
        // the other two share the row beneath it.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ExpressiveStatTile(
                icon = Lucide.Download,
                label = stringResource(R.string.analytics_downloads),
                value = formatExactCount(downloads),
                tone = StatTone.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExpressiveStatTile(
                    icon = Lucide.Package,
                    label = stringResource(R.string.analytics_projects),
                    value = projectCount.toString(),
                    tone = StatTone.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ExpressiveStatTile(
                    icon = Lucide.Heart,
                    label = stringResource(R.string.analytics_followers),
                    value = formatExactCount(followers),
                    tone = StatTone.Tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RyntraDesign.contentShape)
            .background(RyntraDesign.colors.surface)
            .padding(vertical = 16.dp),
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
internal fun ProjectSortControl(
    selected: ProjectSortMode,
    onSelect: (ProjectSortMode) -> Unit,
) {
    if (RyntraDesign.isPlatformNative) {
        PlatformSortGroup(selected, onSelect)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RyntraDesign.contentShape)
            .background(RyntraDesign.colors.surface)
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
        targetValue = if (isSelected) RyntraDesign.colors.surfaceRaised else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Project sort background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Project sort content",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected }
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

// Expressive's connected button group, matching the range and settings pickers: the
// chosen order morphs to a full pill instead of taking a checkmark inside a segment.
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun PlatformSortGroup(selected: ProjectSortMode, onSelect: (ProjectSortMode) -> Unit) {
    RyntraChoiceGroup(
        options = ProjectSortMode.entries,
        isChecked = { it == selected },
        onToggle = onSelect,
        label = { projectSortLabel(it) },
        contentPadding = PaddingValues(horizontal = 8.dp),
    )
}
