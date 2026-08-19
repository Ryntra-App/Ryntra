package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.BrowseCategory
import com.ryntra.shared.model.ProjectSearchQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchFacetsTest {
    @Test
    fun anUnfilteredBrowseSendsNoFacetsAtAll() {
        assertNull(buildSearchFacets(ProjectSearchQuery(text = "sodium")))
    }

    @Test
    fun categoryBecomesAProjectTypeFacet() {
        assertEquals(
            """[["project_type:mod"]]""",
            buildSearchFacets(ProjectSearchQuery(category = BrowseCategory.Mod)),
        )
    }

    @Test
    fun valuesInOneGroupAreAlternativesWhileGroupsAreCombined() {
        val facets = buildSearchFacets(
            ProjectSearchQuery(
                category = BrowseCategory.Mod,
                gameVersions = listOf("1.21", "1.20.1"),
                loaders = listOf("fabric", "quilt"),
            ),
        )

        assertEquals(
            """[["project_type:mod"],["versions:1.21","versions:1.20.1"],""" +
                """["categories:fabric","categories:quilt"]]""",
            facets,
        )
    }

    @Test
    fun loadersRideTheCategoriesFacetBecauseV2HasNoLoaderFacet() {
        assertEquals(
            """[["categories:paper"]]""",
            buildSearchFacets(ProjectSearchQuery(loaders = listOf("paper"))),
        )
    }
}
