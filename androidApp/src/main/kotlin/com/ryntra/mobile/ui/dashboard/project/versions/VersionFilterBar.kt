package com.ryntra.mobile.ui.dashboard.project.versions

import com.ryntra.mobile.ui.components.RyntraMenuChip
import com.ryntra.mobile.ui.components.RyntraFilterSheet
import com.ryntra.mobile.ui.components.RyntraFilterSection
import com.ryntra.mobile.ui.components.RyntraFilterChips
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Wrench
import com.composables.icons.lucide.Tag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Star
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraSearchField
import com.ryntra.shared.model.VersionFacets
import com.ryntra.shared.model.VersionFilter

/** Which facet list the sheet is currently showing, or none. */
private enum class VersionFacetSheet { Channel, Loader, GameVersion }

/**
 * Narrows a long version list the way Modrinth's own version page does.
 *
 * Loaders and game versions are offered in a sheet rather than inline: a mature project
 * ships for dozens of game versions, and a scrolling wall of chips hides the release
 * channel that most people came to filter on.
 */
@Composable
internal fun VersionFilterBar(
    facets: VersionFacets,
    filter: VersionFilter,
    matchCount: Int,
    totalCount: Int,
    onFilterChange: (VersionFilter) -> Unit,
) {
    var openSheet by remember { mutableStateOf<VersionFacetSheet?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        RyntraSearchField(
            value = filter.query,
            onValueChange = { onFilterChange(filter.copy(query = it)) },
            placeholder = stringResource(R.string.version_filter_search),
            leadingIcon = Lucide.Search,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            if (facets.channels.isNotEmpty()) {
                FacetChip(
                    label = stringResource(R.string.version_filter_channel),
                    icon = Lucide.Tag,
                    selectedCount = filter.channels.size,
                    onClick = { openSheet = VersionFacetSheet.Channel },
                )
            }
            if (facets.loaders.isNotEmpty()) {
                FacetChip(
                    label = stringResource(R.string.version_filter_loader),
                    icon = Lucide.Wrench,
                    selectedCount = filter.loaders.size,
                    onClick = { openSheet = VersionFacetSheet.Loader },
                )
            }
            if (facets.gameVersions.isNotEmpty()) {
                FacetChip(
                    label = stringResource(R.string.version_filter_game_version),
                    icon = Lucide.Gamepad2,
                    selectedCount = filter.gameVersions.size,
                    onClick = { openSheet = VersionFacetSheet.GameVersion },
                )
            }
            FilterChip(
                selected = filter.featuredOnly,
                onClick = { onFilterChange(filter.copy(featuredOnly = !filter.featuredOnly)) },
                label = { Text(stringResource(R.string.version_filter_featured)) },
                leadingIcon = { Icon(Lucide.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        if (filter.isActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.version_filter_matches, totalCount, matchCount, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onFilterChange(VersionFilter()) }) {
                    Text(stringResource(R.string.version_filter_clear))
                }
            }
        }
    }

    openSheet?.let { sheet ->
        val model = when (sheet) {
            VersionFacetSheet.Channel -> FacetSheetModel(
                titleRes = R.string.version_filter_channel,
                icon = Lucide.Tag,
                values = facets.channels,
                selected = filter.channels,
                onToggle = { onFilterChange(filter.toggleChannel(it)) },
                onClear = { onFilterChange(filter.copy(channels = emptySet())) },
            )
            VersionFacetSheet.Loader -> FacetSheetModel(
                titleRes = R.string.version_filter_loader,
                icon = Lucide.Wrench,
                values = facets.loaders,
                selected = filter.loaders,
                onToggle = { onFilterChange(filter.toggleLoader(it)) },
                onClear = { onFilterChange(filter.copy(loaders = emptySet())) },
            )
            VersionFacetSheet.GameVersion -> FacetSheetModel(
                titleRes = R.string.version_filter_game_version,
                icon = Lucide.Gamepad2,
                values = facets.gameVersions,
                selected = filter.gameVersions,
                onToggle = { onFilterChange(filter.toggleGameVersion(it)) },
                onClear = { onFilterChange(filter.copy(gameVersions = emptySet())) },
            )
        }
        val title = stringResource(model.titleRes)
        RyntraFilterSheet(
            title = title,
            canReset = model.selected.isNotEmpty(),
            onReset = model.onClear,
            onDismiss = { openSheet = null },
        ) {
            RyntraFilterSection(title, model.icon) {
                RyntraFilterChips(
                    values = model.values,
                    isSelected = { it in model.selected },
                    onToggle = model.onToggle,
                    foldedCount = FOLDED_FACET_VALUES,
                )
            }
        }
    }
}

private data class FacetSheetModel(
    val titleRes: Int,
    val icon: ImageVector,
    val values: List<String>,
    val selected: Set<String>,
    val onToggle: (String) -> Unit,
    val onClear: () -> Unit,
)

/** Game versions run to dozens on a mature project; the newest dozen covers most filtering. */
private const val FOLDED_FACET_VALUES = 12

@Composable
private fun FacetChip(label: String, icon: ImageVector, selectedCount: Int, onClick: () -> Unit) {
    RyntraMenuChip(
        label = if (selectedCount > 0) "$label · $selectedCount" else label,
        icon = icon,
        isActive = selectedCount > 0,
        onClick = onClick,
    )
}
