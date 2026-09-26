package com.ryntra.mobile.ui.dashboard.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Box as BoxIcon
import com.composables.icons.lucide.Braces
import com.composables.icons.lucide.CalendarPlus
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Glasses
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PackageOpen
import com.composables.icons.lucide.Paintbrush
import com.composables.icons.lucide.Plug
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Wrench
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraFilterChips
import com.ryntra.mobile.ui.components.RyntraFilterSection
import com.ryntra.mobile.ui.components.RyntraFilterSheet
import com.ryntra.mobile.ui.components.RyntraMenuChip
import com.ryntra.shared.model.BrowseCategory
import com.ryntra.shared.model.BrowseMetadata
import com.ryntra.shared.model.ProjectSearchQuery
import com.ryntra.shared.model.ProjectSearchSort

/** Content types as filter chips, each with the icon modrinth.com gives it. */
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
                leadingIcon = {
                    Icon(category.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
    }
}

/** Sort and filters as dropdown chips, so both read as parts of one control strip. */
@Composable
internal fun BrowseToolRow(
    query: ProjectSearchQuery,
    onSortChange: (ProjectSearchSort) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }
    val activeFilterCount = query.gameVersions.size + query.loaders.size
    val sort = query.effectiveSort

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        Box {
            RyntraMenuChip(
                label = sort.label(),
                icon = sort.icon,
                isActive = false,
                onClick = { isSortMenuOpen = true },
            )
            DropdownMenu(expanded = isSortMenuOpen, onDismissRequest = { isSortMenuOpen = false }) {
                ProjectSearchSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        leadingIcon = { Icon(option.icon, contentDescription = null) },
                        trailingIcon = if (option == sort) {
                            { Icon(Lucide.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else {
                            null
                        },
                        onClick = {
                            isSortMenuOpen = false
                            onSortChange(option)
                        },
                    )
                }
            }
        }
        RyntraMenuChip(
            label = if (activeFilterCount > 0) {
                stringResource(R.string.browse_filters_active, activeFilterCount)
            } else {
                stringResource(R.string.browse_filters)
            },
            icon = Lucide.SlidersHorizontal,
            isActive = activeFilterCount > 0,
            onClick = onOpenFilters,
        )
    }
}

/** Loaders and Minecraft versions, in a sheet rather than inline above the results. */
@Composable
internal fun BrowseFilterSheet(
    query: ProjectSearchQuery,
    metadata: BrowseMetadata,
    onToggleGameVersion: (String) -> Unit,
    onToggleLoader: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val versions = remember(metadata) { metadata.releaseVersions() }
    val loaders = remember(metadata, query.category) { metadata.loadersFor(query.category) }

    RyntraFilterSheet(
        title = stringResource(R.string.browse_filters),
        canReset = query.gameVersions.isNotEmpty() || query.loaders.isNotEmpty(),
        onReset = onReset,
        onDismiss = onDismiss,
    ) {
        if (versions.isEmpty() && loaders.isEmpty()) {
            Text(
                text = stringResource(R.string.browse_filters_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@RyntraFilterSheet
        }
        if (loaders.isNotEmpty()) {
            RyntraFilterSection(stringResource(R.string.browse_filter_loaders), Lucide.Wrench) {
                RyntraFilterChips(
                    values = loaders,
                    isSelected = { it in query.loaders },
                    onToggle = onToggleLoader,
                    label = ::loaderName,
                    leadingIcon = { loader -> metadata.loaderIcon(loader)?.let { LoaderIcon(it) } },
                )
            }
        }
        if (versions.isNotEmpty()) {
            RyntraFilterSection(stringResource(R.string.browse_filter_game_versions), Lucide.Gamepad2) {
                RyntraFilterChips(
                    values = versions,
                    isSelected = { it in query.gameVersions },
                    onToggle = onToggleGameVersion,
                    foldedCount = FOLDED_VERSIONS,
                )
            }
        }
    }
}

/** Twelve releases cover the last few years; older ones are one tap further. */
private const val FOLDED_VERSIONS = 12

/**
 * Modrinth's own loader artwork. It is stroked with `currentColor`, which an SVG decoder
 * cannot resolve, so the chip's content colour is written into the markup first.
 */
@Composable
private fun LoaderIcon(svg: String) {
    val color = LocalContentColor.current
    val tinted = remember(svg, color) {
        svg.replace("currentColor", "#%06X".format(color.toArgb() and 0xFFFFFF)).encodeToByteArray()
    }
    AsyncImage(model = tinted, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
}

/** `neoforge` → `Neoforge`; Modrinth's tag route has no display names. */
private fun loaderName(loader: String): String = loader.replaceFirstChar(Char::uppercase)

private val BrowseCategory.icon: ImageVector
    get() = when (this) {
        BrowseCategory.All -> Lucide.LayoutGrid
        BrowseCategory.Mod -> Lucide.BoxIcon
        BrowseCategory.Plugin -> Lucide.Plug
        BrowseCategory.DataPack -> Lucide.Braces
        BrowseCategory.Shader -> Lucide.Glasses
        BrowseCategory.ResourcePack -> Lucide.Paintbrush
        BrowseCategory.Modpack -> Lucide.PackageOpen
    }

private val ProjectSearchSort.icon: ImageVector
    get() = when (this) {
        ProjectSearchSort.Relevance -> Lucide.Sparkles
        ProjectSearchSort.Downloads -> Lucide.Download
        ProjectSearchSort.Follows -> Lucide.Heart
        ProjectSearchSort.Updated -> Lucide.RefreshCw
        ProjectSearchSort.Newest -> Lucide.CalendarPlus
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
