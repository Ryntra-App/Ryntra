package com.ryntra.mobile.ui.dashboard.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.BrowseCategory
import com.ryntra.shared.model.BrowseMetadata
import com.ryntra.shared.model.ProjectSearchHit
import com.ryntra.shared.model.ProjectSearchQuery
import com.ryntra.shared.model.ProjectSearchSort

@Composable
internal fun BrowseCategoryRow(
    selected: BrowseCategory,
    onSelect: (BrowseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        BrowseCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.label()) },
            )
        }
    }
}

@Composable
internal fun BrowseToolRow(
    query: ProjectSearchQuery,
    isFilterPanelOpen: Boolean,
    onSortChange: (ProjectSearchSort) -> Unit,
    onToggleFilterPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }
    val activeFilterCount = query.gameVersions.size + query.loaders.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box {
            RyntraSecondaryButton(
                text = query.effectiveSort.label(),
                icon = Lucide.ArrowUpDown,
                onClick = { isSortMenuOpen = true },
            )
            DropdownMenu(expanded = isSortMenuOpen, onDismissRequest = { isSortMenuOpen = false }) {
                ProjectSearchSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label()) },
                        onClick = {
                            isSortMenuOpen = false
                            onSortChange(sort)
                        },
                    )
                }
            }
        }
        RyntraSecondaryButton(
            text = if (activeFilterCount > 0) {
                stringResource(R.string.browse_filters_active, activeFilterCount)
            } else {
                stringResource(R.string.browse_filters)
            },
            icon = Lucide.SlidersHorizontal,
            onClick = onToggleFilterPanel,
        )
    }
}

@Composable
internal fun BrowseFilterPanel(
    isOpen: Boolean,
    query: ProjectSearchQuery,
    metadata: BrowseMetadata,
    onToggleGameVersion: (String) -> Unit,
    onToggleLoader: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = RyntraDesign.motion
    val versions = remember(metadata) { metadata.releaseVersions() }
    val loaders = remember(metadata, query.category) { metadata.loadersFor(query.category) }

    AnimatedVisibility(
        visible = isOpen,
        enter = expandVertically(animationSpec = tween(motion.duration(170))) +
            fadeIn(animationSpec = tween(motion.duration(120))),
        exit = shrinkVertically(animationSpec = tween(motion.duration(120))) +
            fadeOut(animationSpec = tween(motion.duration(80))),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (versions.isEmpty() && loaders.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.browse_filters_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    return@Column
                }

                if (versions.isNotEmpty()) {
                    BrowseFilterLabel(stringResource(R.string.browse_filter_game_versions))
                    ChipGroup(
                        values = versions,
                        selected = query.gameVersions,
                        onToggle = onToggleGameVersion,
                    )
                }
                if (loaders.isNotEmpty()) {
                    BrowseFilterLabel(
                        stringResource(R.string.browse_filter_loaders),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    ChipGroup(
                        values = loaders,
                        selected = query.loaders,
                        onToggle = onToggleLoader,
                    )
                }
                if (query.hasFilters) {
                    RyntraSecondaryButton(
                        text = stringResource(R.string.browse_filters_reset),
                        icon = Lucide.X,
                        onClick = onReset,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipGroup(values: List<String>, selected: List<String>, onToggle: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value in selected,
                onClick = { onToggle(value) },
                label = { Text(value) },
            )
        }
    }
}

@Composable
private fun BrowseFilterLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
internal fun BrowseHighlightStrip(
    hits: List<ProjectSearchHit>,
    onOpen: (ProjectSearchHit) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(hits, key = { it.projectId }) { hit ->
            BrowseHighlightCard(hit = hit, onClick = { onOpen(hit) })
        }
    }
}

@Composable
private fun BrowseHighlightCard(hit: ProjectSearchHit, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(168.dp).clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp),
            ) {
                if (hit.iconUrl != null) {
                    AsyncImage(
                        model = hit.iconUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        RyntraIcon(
                            icon = Lucide.Package,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Text(
                text = hit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            hit.byline?.let { byline ->
                Text(
                    text = byline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RyntraIcon(
                    icon = Lucide.Download,
                    contentDescription = null,
                    tint = RyntraDesign.colors.accent,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = formatExactCount(hit.downloads),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

@Composable
internal fun RecentSearchRow(
    query: String,
    onSelect: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 4.dp),
    ) {
        RyntraIcon(
            icon = Lucide.Clock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        IconButton(onClick = onForget) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = stringResource(R.string.browse_recent_forget),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun BrowseSectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 18.dp, bottom = 10.dp),
    )
}

@Composable
internal fun BrowseCategory.label(): String = stringResource(
    when (this) {
        BrowseCategory.All -> R.string.browse_category_all
        BrowseCategory.Mod -> R.string.browse_category_mods
        BrowseCategory.Plugin -> R.string.browse_category_plugins
        BrowseCategory.DataPack -> R.string.browse_category_datapacks
        BrowseCategory.Shader -> R.string.browse_category_shaders
        BrowseCategory.ResourcePack -> R.string.browse_category_resourcepacks
        BrowseCategory.Modpack -> R.string.browse_category_modpacks
    },
)

@Composable
internal fun ProjectSearchSort.label(): String = stringResource(
    when (this) {
        ProjectSearchSort.Relevance -> R.string.browse_sort_relevance
        ProjectSearchSort.Downloads -> R.string.browse_sort_downloads
        ProjectSearchSort.Follows -> R.string.browse_sort_follows
        ProjectSearchSort.Updated -> R.string.browse_sort_updated
        ProjectSearchSort.Newest -> R.string.browse_sort_newest
    },
)
