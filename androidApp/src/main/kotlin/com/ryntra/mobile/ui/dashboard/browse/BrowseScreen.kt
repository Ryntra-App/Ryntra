package com.ryntra.mobile.ui.dashboard.browse

import com.ryntra.mobile.ui.components.RyntraContentLoading
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.ryntra.mobile.BrowseState
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.components.label
import com.ryntra.mobile.ui.dashboard.projects.ProjectRow
import com.ryntra.shared.model.ProjectSearchHit
import com.ryntra.shared.model.ProjectSearchQuery

/**
 * The public Modrinth catalogue.
 *
 * Everything opened from here is read-only unless the project happens to be one the signed-in
 * user manages, which the view model resolves before pushing the detail screen.
 */
@Composable
fun BrowseScreen(
    state: BrowseState,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onQueryChange: (ProjectSearchQuery) -> Unit,
    onLoadMore: () -> Unit,
    onOpenHit: (ProjectSearchHit) -> Unit,
    onForgetRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var isFilterPanelOpen by rememberSaveable { mutableStateOf(false) }

    // Paging is driven by proximity to the end rather than by the last item appearing, so the next
    // page is already in flight by the time the user reaches it. The effect is keyed on the page
    // it observed: without that it would keep testing the first page's state forever.
    LaunchedEffect(listState, state.hasMore, state.hits.size) {
        if (!state.hasMore) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (lastVisible >= listState.layoutInfo.totalItemsCount - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
    ) {
        item(key = "browse-search", contentType = "search") {
            RyntraTextField(
                value = state.query.text,
                onValueChange = onTextChange,
                placeholder = stringResource(R.string.browse_search_placeholder),
                leadingIcon = Lucide.Search,
                leadingIconDescription = null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    onSubmit()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item(key = "browse-categories", contentType = "categories") {
            BrowseCategoryRow(
                selected = state.query.category,
                onSelect = { onQueryChange(state.query.withCategory(it)) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item(key = "browse-tools", contentType = "tools") {
            BrowseToolRow(
                query = state.query,
                isFilterPanelOpen = isFilterPanelOpen,
                onSortChange = { onQueryChange(state.query.withSort(it)) },
                onToggleFilterPanel = { isFilterPanelOpen = !isFilterPanelOpen },
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        item(key = "browse-filters", contentType = "filters") {
            BrowseFilterPanel(
                isOpen = isFilterPanelOpen,
                query = state.query,
                metadata = state.metadata,
                onToggleGameVersion = { onQueryChange(state.query.togglingGameVersion(it)) },
                onToggleLoader = { onQueryChange(state.query.togglingLoader(it)) },
                onReset = { onQueryChange(state.query.cleared()) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        state.errorMessage?.let { message ->
            item(key = "browse-error", contentType = "error") {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }

        if (state.isShowingHighlights) {
            highlightItems(
                state = state,
                onOpenHit = onOpenHit,
                onSelectRecent = { query ->
                    onTextChange(query)
                    onSubmit()
                },
                onForgetRecentSearch = onForgetRecentSearch,
                onClearRecentSearches = onClearRecentSearches,
            )
            return@LazyColumn
        }

        if (state.isLoading) {
            item(key = "browse-loading", contentType = "loading") {
                RyntraContentLoading()
            }
            return@LazyColumn
        }

        if (state.hits.isEmpty()) {
            item(key = "browse-empty", contentType = "empty") {
                RyntraEmptyState(
                    title = stringResource(R.string.browse_empty),
                    message = stringResource(R.string.browse_empty_hint),
                )
            }
            return@LazyColumn
        }

        item(key = "browse-count", contentType = "count") {
            Text(
                text = stringResource(R.string.browse_result_count, state.totalHits),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }

        items(state.hits, key = { it.projectId }, contentType = { "hit" }) { hit ->
            ProjectRow(
                project = hit.toProjectSeed(),
                showStatus = false,
                subtitleOverride = hit.browseSubtitle(),
                onClick = { onOpenHit(hit) },
            )
        }

        if (state.isLoadingMore) {
            item(key = "browse-loading-more", contentType = "loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

private fun LazyListScope.highlightItems(
    state: BrowseState,
    onOpenHit: (ProjectSearchHit) -> Unit,
    onSelectRecent: (String) -> Unit,
    onForgetRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
) {
    if (state.recentSearches.isNotEmpty()) {
        item(key = "browse-recent-heading", contentType = "heading") {
            BrowseRecentHeader(onClear = onClearRecentSearches)
        }
        items(state.recentSearches, key = { "recent-$it" }, contentType = { "recent" }) { query ->
            RecentSearchRow(
                query = query,
                onSelect = { onSelectRecent(query) },
                onForget = { onForgetRecentSearch(query) },
            )
        }
    }

    if (state.isLoadingHighlights && state.highlights.isEmpty) {
        item(key = "browse-highlights-loading", contentType = "loading") {
            RyntraContentLoading()
        }
        return
    }

    if (state.highlights.popular.isNotEmpty()) {
        item(key = "browse-popular", contentType = "strip") {
            BrowseSectionHeading(stringResource(R.string.browse_popular))
            BrowseHighlightStrip(hits = state.highlights.popular, onOpen = onOpenHit)
        }
    }
    if (state.highlights.recentlyUpdated.isNotEmpty()) {
        item(key = "browse-updated", contentType = "strip") {
            BrowseSectionHeading(stringResource(R.string.browse_recently_updated))
            BrowseHighlightStrip(hits = state.highlights.recentlyUpdated, onOpen = onOpenHit)
        }
    }
}

@Composable
private fun BrowseRecentHeader(onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        BrowseSectionHeading(stringResource(R.string.browse_recent), Modifier.weight(1f))
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.browse_recent_clear))
        }
    }
}

/** Browsing someone else's catalogue, the author matters more than the slug. */
@Composable
private fun ProjectSearchHit.browseSubtitle(): String {
    val type = displayKind().label()
    return byline?.let { "$it  ·  $type" } ?: type
}

private const val PREFETCH_DISTANCE = 4
