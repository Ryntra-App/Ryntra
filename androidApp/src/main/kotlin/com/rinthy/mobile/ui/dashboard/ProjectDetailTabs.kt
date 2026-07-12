package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.ui.theme.RinthyDesign

internal enum class ProjectDetailTab(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Overview", Lucide.LayoutGrid),
    Versions("Versions", Lucide.Download),
    Edit("Edit", Lucide.Scale),
    Members("Members", Lucide.UsersRound),
}

@Composable
internal fun ProjectDetailTabs(
    selected: ProjectDetailTab,
    onSelect: (ProjectDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        ProjectDetailTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.095f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = contentColor,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                    } else {
                        RinthyDesign.colors.separator
                    },
                ),
                modifier = Modifier.clickable(onClick = { onSelect(tab) }),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        }
    }
}
