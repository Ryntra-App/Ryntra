package com.ryntra.mobile.ui.dashboard.project.versions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.composables.icons.lucide.ChevronDown
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
                    selectedCount = filter.channels.size,
                    onClick = { openSheet = VersionFacetSheet.Channel },
                )
            }
            if (facets.loaders.isNotEmpty()) {
                FacetChip(
                    label = stringResource(R.string.version_filter_loader),
                    selectedCount = filter.loaders.size,
                    onClick = { openSheet = VersionFacetSheet.Loader },
                )
            }
            if (facets.gameVersions.isNotEmpty()) {
                FacetChip(
                    label = stringResource(R.string.version_filter_game_version),
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
        val (titleRes, values, selected, onToggle) = when (sheet) {
            VersionFacetSheet.Channel -> FacetSheetModel(
                R.string.version_filter_channel,
                facets.channels,
                filter.channels,
            ) { onFilterChange(filter.toggleChannel(it)) }
            VersionFacetSheet.Loader -> FacetSheetModel(
                R.string.version_filter_loader,
                facets.loaders,
                filter.loaders,
            ) { onFilterChange(filter.toggleLoader(it)) }
            VersionFacetSheet.GameVersion -> FacetSheetModel(
                R.string.version_filter_game_version,
                facets.gameVersions,
                filter.gameVersions,
            ) { onFilterChange(filter.toggleGameVersion(it)) }
        }
        FacetSheet(
            title = stringResource(titleRes),
            values = values,
            selected = selected,
            onToggle = onToggle,
            onDismiss = { openSheet = null },
        )
    }
}

private data class FacetSheetModel(
    val titleRes: Int,
    val values: List<String>,
    val selected: Set<String>,
    val onToggle: (String) -> Unit,
)

@Composable
private fun FacetChip(label: String, selectedCount: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selectedCount > 0,
        onClick = onClick,
        label = { Text(if (selectedCount > 0) "$label · $selectedCount" else label) },
        trailingIcon = { Icon(Lucide.ChevronDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FacetSheet(
    title: String,
    values: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
    }
}
