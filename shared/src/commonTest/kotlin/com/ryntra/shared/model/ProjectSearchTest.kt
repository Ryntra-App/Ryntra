package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectSearchTest {
    @Test
    fun serverHostingIsNotABrowsableCategory() {
        assertFalse(BrowseCategory.entries.any { it.apiValue == "minecraft_java_server" })
        assertEquals(BrowseCategory.All, BrowseCategory.fromApi("minecraft_java_server"))
    }

    @Test
    fun anEmptyQueryLeadsWithPopularityInsteadOfRelevance() {
        assertEquals(ProjectSearchSort.Downloads, ProjectSearchQuery().effectiveSort)
        assertEquals(ProjectSearchSort.Relevance, ProjectSearchQuery(text = "sodium").effectiveSort)
    }

    @Test
    fun anExplicitSortSurvivesAnEmptyQuery() {
        val query = ProjectSearchQuery(sort = ProjectSearchSort.Updated)

        assertEquals(ProjectSearchSort.Updated, query.effectiveSort)
    }

    @Test
    fun switchingCategoryDropsLoadersThatNoLongerApply() {
        val query = ProjectSearchQuery(category = BrowseCategory.Mod, loaders = listOf("fabric"))

        val switched = query.withCategory(BrowseCategory.ResourcePack)

        assertTrue(switched.loaders.isEmpty())
    }

    @Test
    fun everyFilterChangeReturnsToTheFirstPage() {
        val paged = ProjectSearchQuery(text = "sodium").nextPage()
        assertEquals(ProjectSearchQuery.PAGE_SIZE, paged.offset)

        assertEquals(0, paged.togglingLoader("fabric").offset)
        assertEquals(0, paged.togglingGameVersion("1.21").offset)
        assertEquals(0, paged.withSort(ProjectSearchSort.Follows).offset)
        assertEquals(0, paged.withText("iris").offset)
    }

    @Test
    fun togglingAFilterAddsThenRemovesIt() {
        val once = ProjectSearchQuery().togglingGameVersion("1.21")
        assertEquals(listOf("1.21"), once.gameVersions)

        assertTrue(once.togglingGameVersion("1.21").gameVersions.isEmpty())
    }

    @Test
    fun clearingKeepsTheTypedQueryAndSort() {
        val query = ProjectSearchQuery(
            text = "sodium",
            category = BrowseCategory.Mod,
            gameVersions = listOf("1.21"),
            loaders = listOf("fabric"),
            sort = ProjectSearchSort.Follows,
        )

        val cleared = query.cleared()

        assertEquals("sodium", cleared.text)
        assertEquals(ProjectSearchSort.Follows, cleared.sort)
        assertFalse(cleared.hasFilters)
    }

    @Test
    fun pagingStopsWhenEverythingHasBeenSeen() {
        val hits = List(20) { index -> hit("id-$index") }

        assertTrue(ProjectSearchPage(hits = hits, offset = 0, totalHits = 98).hasMore)
        assertFalse(ProjectSearchPage(hits = hits, offset = 80, totalHits = 98).hasMore)
    }

    @Test
    fun searchHitsSeedTheDetailScreenWithWhatTheCardAlreadyShowed() {
        val seed = hit("AANobbMI").copy(slug = "sodium", title = "Sodium", downloads = 209_000_000).toProjectSeed()

        assertEquals("AANobbMI", seed.id)
        assertEquals("sodium", seed.slug)
        assertEquals(209_000_000, seed.downloads)
    }

    @Test
    fun pluginsAreRecognisedFromTheLoadersFoldedIntoCategories() {
        val plugin = hit("x").copy(projectType = "mod", categories = listOf("paper", "utility"))

        assertEquals(ProjectDisplayKind.Plugin, plugin.displayKind())
    }

    @Test
    fun snapshotsAreNotOfferedAsVersionFilters() {
        val metadata = BrowseMetadata(
            gameVersions = listOf(
                GameVersion("26.3-snapshot-9", versionType = "snapshot"),
                GameVersion("1.21", versionType = "release", isMajor = true),
                GameVersion("1.20.1", versionType = "release", isMajor = true),
            ),
        )

        assertEquals(listOf("1.21", "1.20.1"), metadata.releaseVersions())
    }

    @Test
    fun loadersAreOfferedOnlyWhereModrinthAcceptsThem() {
        val metadata = BrowseMetadata(
            loaders = listOf(
                ModLoader("fabric", listOf("mod", "modpack")),
                ModLoader("paper", listOf("plugin")),
                ModLoader("datapack", listOf("datapack")),
            ),
        )

        assertEquals(listOf("fabric"), metadata.loadersFor(BrowseCategory.Mod))
        assertEquals(listOf("paper"), metadata.loadersFor(BrowseCategory.Plugin))
        assertContains(metadata.loadersFor(BrowseCategory.All), "fabric")
    }

    @Test
    fun recentQueriesAreDeduplicatedAndCapped() {
        var history = emptyList<String>()
        repeat(10) { index -> history = ProjectSearchHistory.adding(history, "query-$index") }

        assertEquals(ProjectSearchHistory.MAX_ENTRIES, history.size)
        assertEquals("query-9", history.first())

        history = ProjectSearchHistory.adding(history, "QUERY-9")
        assertEquals(1, history.count { it.equals("query-9", ignoreCase = true) })
        assertEquals("QUERY-9", history.first())
    }

    @Test
    fun blankQueriesAreNeverRemembered() {
        assertTrue(ProjectSearchHistory.adding(emptyList(), "   ").isEmpty())
    }

    private fun hit(id: String) = ProjectSearchHit(projectId = id, title = id)
}
