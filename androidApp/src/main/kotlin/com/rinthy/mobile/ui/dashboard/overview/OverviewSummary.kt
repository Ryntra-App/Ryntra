package com.rinthy.mobile.ui.dashboard.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BadgeCheck
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.ui.components.formatExactCount
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.theme.RinthyDesign

@Composable
internal fun CreatorSummary(
    username: String,
    projectCount: Int,
    organizationCount: Int,
    snapshot: OverviewSnapshot,
) {
    Text(
        text = stringResource(R.string.overview_welcome, username),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, RoundedCornerShape(10.dp)),
    ) {
        SummaryRow(Lucide.Download, stringResource(R.string.analytics_downloads), formatExactCount(snapshot.totalDownloads), isAccent = true)
        HorizontalDivider(color = RinthyDesign.colors.separator, modifier = Modifier.padding(start = 44.dp))
        SummaryRow(Lucide.Heart, stringResource(R.string.analytics_followers), formatExactCount(snapshot.totalFollowers))
        HorizontalDivider(color = RinthyDesign.colors.separator, modifier = Modifier.padding(start = 44.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CompactFact(
                Lucide.Package,
                pluralStringResource(R.plurals.overview_project_count, projectCount, projectCount),
                Modifier.weight(1f),
            )
            CompactFact(
                Lucide.BadgeCheck,
                pluralStringResource(R.plurals.overview_approved_count, snapshot.approvedProjects, snapshot.approvedProjects),
                Modifier.weight(1f),
            )
            CompactFact(
                Lucide.UsersRound,
                pluralStringResource(R.plurals.overview_team_count, organizationCount, organizationCount),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    isAccent: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAccent) RinthyDesign.colors.accent else RinthyDesign.colors.labelSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CompactFact(icon: ImageVector, text: String, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RinthyDesign.colors.labelSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}
