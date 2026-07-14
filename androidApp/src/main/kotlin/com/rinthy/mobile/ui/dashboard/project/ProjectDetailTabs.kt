package com.rinthy.mobile.ui.dashboard.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.ui.components.RinthyIcon
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
    val colors = RinthyDesign.colors
    val outerShape = RoundedCornerShape(11.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(colors.surface)
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), outerShape)
            .padding(3.dp),
    ) {
        ProjectDetailTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val itemShape = RoundedCornerShape(8.dp)
            val contentColor = if (isSelected) colors.accent else colors.labelSecondary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(itemShape)
                    .background(if (isSelected) colors.surfaceRaised else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 4.dp),
            ) {
                RinthyIcon(
                    icon = tab.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                BasicText(
                    text = tab.label,
                    style = RinthyDesign.caption.copy(
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}
