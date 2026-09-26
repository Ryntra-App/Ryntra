package com.ryntra.mobile.ui.dashboard.project

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.UsersRound
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.theme.RyntraDesign

internal enum class ProjectDetailTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Overview(R.string.project_tab_overview, Lucide.LayoutGrid),
    Versions(R.string.project_tab_versions, Lucide.Download),
    Edit(R.string.project_tab_edit, Lucide.Pencil),
    Disclosures(R.string.project_tab_disclosures, Lucide.ShieldCheck),
    Members(R.string.project_tab_members, Lucide.UsersRound),
    Moderation(R.string.project_tab_moderation, Lucide.MessageSquareText),
}

@Composable
internal fun ProjectDetailTabs(
    selected: ProjectDetailTab,
    isReadOnly: Boolean,
    onSelect: (ProjectDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableTabs = ProjectDetailTab.entries.filter { tab ->
        !isReadOnly || tab == ProjectDetailTab.Overview || tab == ProjectDetailTab.Versions
    }
    val isScrollable = availableTabs.size > 4
    if (RyntraDesign.isPlatformNative) {
        PlatformDetailTabs(
            selected = selected,
            availableTabs = availableTabs,
            isScrollable = isScrollable,
            onSelect = onSelect,
            modifier = modifier,
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    val outerShape = RoundedCornerShape(11.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(colors.surfaceContainerLow)
            .border(0.75.dp, colors.outlineVariant, outerShape)
            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(3.dp),
    ) {
        availableTabs.forEach { tab ->
            val isSelected = selected == tab
            val itemShape = RoundedCornerShape(8.dp)
            val contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .then(if (isScrollable) Modifier.width(112.dp) else Modifier.weight(1f))
                    .height(48.dp)
                    .clip(itemShape)
                    .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 4.dp),
            ) {
                RyntraIcon(
                    icon = tab.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                BasicText(
                    text = stringResource(tab.labelRes),
                    style = RyntraDesign.caption.copy(
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

// Material navigates between views inside a screen with a tab row, not with a
// segmented button — the row owns the sliding indicator, the ripple and the
// selected-tab semantics that a hand-built strip has to reimplement.
@Composable
private fun PlatformDetailTabs(
    selected: ProjectDetailTab,
    availableTabs: List<ProjectDetailTab>,
    isScrollable: Boolean,
    onSelect: (ProjectDetailTab) -> Unit,
    modifier: Modifier,
) {
    val selectedIndex = availableTabs.indexOf(selected).coerceAtLeast(0)
    if (isScrollable) {
        // The row already sits inside the list's 16dp gutter; the 52dp default would
        // add a second inset and leave the first tab stranded.
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            modifier = modifier,
        ) {
            availableTabs.forEach { tab -> DetailTab(tab = tab, isSelected = tab == selected, onSelect = onSelect) }
        }
    } else {
        PrimaryTabRow(selectedTabIndex = selectedIndex, modifier = modifier) {
            availableTabs.forEach { tab -> DetailTab(tab = tab, isSelected = tab == selected, onSelect = onSelect) }
        }
    }
}

@Composable
private fun DetailTab(
    tab: ProjectDetailTab,
    isSelected: Boolean,
    onSelect: (ProjectDetailTab) -> Unit,
) {
    LeadingIconTab(
        selected = isSelected,
        onClick = { onSelect(tab) },
        text = {
            Text(
                text = stringResource(tab.labelRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}
