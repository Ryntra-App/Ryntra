package com.rinthy.mobile.ui.dashboard.overview

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheckBig
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthySectionLabel
import com.rinthy.mobile.ui.components.attentionMessage
import com.rinthy.mobile.ui.components.formatExactCount
import com.rinthy.mobile.ui.components.label
import com.rinthy.mobile.ui.dashboard.projects.ProjectArtwork
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.Project

@Composable
internal fun OverviewSectionHeader(
    title: String,
    supportingText: String? = null,
) {
    Column(modifier = Modifier.padding(top = 26.dp, bottom = 8.dp)) {
        RinthySectionLabel(title)
        supportingText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
internal fun AttentionRow(project: Project, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        ProjectArtwork(project)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(project.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = project.attentionMessage(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Lucide.TriangleAlert,
            contentDescription = stringResource(R.string.overview_attention_required),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun InReviewRow(project: Project, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        ProjectArtwork(project)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(project.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.attention_review_for_publication),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Lucide.Clock,
            contentDescription = stringResource(R.string.overview_in_review),
            tint = RinthyDesign.colors.warning,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun AllClearRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, RoundedCornerShape(10.dp))
            .padding(14.dp),
    ) {
        Icon(Lucide.CircleCheckBig, contentDescription = null, tint = RinthyDesign.colors.positive)
        Column {
            Text(stringResource(R.string.overview_all_clear), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.overview_all_clear_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun LeadingProjectRow(
    project: Project,
    totalDownloads: Long,
    onClick: () -> Unit,
) {
    val share = if (totalDownloads == 0L) 0f else project.downloads.toFloat() / totalDownloads.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProjectArtwork(project)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(project.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.overview_download_count, formatExactCount(project.downloads)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Lucide.Download, contentDescription = null, tint = RinthyDesign.colors.accent)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RinthyDesign.colors.separator),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(RinthyDesign.colors.accent, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = stringResource(R.string.overview_portfolio_share, (share * 100).toInt()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
internal fun PortfolioMixRow(
    item: ProjectTypeCount,
    totalProjects: Int,
    showDivider: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(item.kind.label(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.overview_count_of, item.count, totalProjects),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (showDivider) HorizontalDivider(color = RinthyDesign.colors.separator)
}
