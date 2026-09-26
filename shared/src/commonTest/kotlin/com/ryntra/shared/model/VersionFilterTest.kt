package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionFilterTest {
    private fun version(
        number: String,
        channel: String = "release",
        loaders: List<String> = listOf("fabric"),
        gameVersions: List<String> = listOf("1.21"),
        featured: Boolean = false,
        changelog: String = "",
        fileName: String = "mod.jar",
    ) = ProjectVersion(
        id = number,
        projectId = "project",
        name = "Build $number",
        versionNumber = number,
        versionType = channel,
        changelog = changelog,
        gameVersions = gameVersions,
        loaders = loaders,
        featured = featured,
        files = listOf(ProjectVersionFile(url = "https://example.invalid/$fileName", filename = fileName)),
    )

    @Test
    fun anInactiveFilterKeepsTheListUntouched() {
        val versions = listOf(version("1.0"), version("2.0", channel = "beta"))

        assertEquals(versions, versions.filteredBy(VersionFilter()))
    }

    @Test
    fun channelNarrowsToThatReleaseChannel() {
        val versions = listOf(version("1.0"), version("2.0", channel = "beta"), version("3.0", channel = "alpha"))

        val filtered = versions.filteredBy(VersionFilter(channels = setOf("beta")))

        assertEquals(listOf("2.0"), filtered.map(ProjectVersion::versionNumber))
    }

    @Test
    fun severalChannelsAreUnioned() {
        val versions = listOf(version("1.0"), version("2.0", channel = "beta"), version("3.0", channel = "alpha"))

        val filtered = versions.filteredBy(VersionFilter(channels = setOf("beta", "alpha")))

        assertEquals(listOf("2.0", "3.0"), filtered.map(ProjectVersion::versionNumber))
    }

    @Test
    fun aVersionMatchesWhenAnyOfItsLoadersIsSelected() {
        val versions = listOf(
            version("1.0", loaders = listOf("fabric", "quilt")),
            version("2.0", loaders = listOf("forge")),
        )

        val filtered = versions.filteredBy(VersionFilter(loaders = setOf("quilt")))

        assertEquals(listOf("1.0"), filtered.map(ProjectVersion::versionNumber))
    }

    @Test
    fun facetsAreCombinedWithAnd() {
        val versions = listOf(
            version("1.0", channel = "beta", loaders = listOf("fabric")),
            version("2.0", channel = "beta", loaders = listOf("forge")),
        )

        val filtered = versions.filteredBy(VersionFilter(channels = setOf("beta"), loaders = setOf("forge")))

        assertEquals(listOf("2.0"), filtered.map(ProjectVersion::versionNumber))
    }

    @Test
    fun featuredOnlyKeepsTheHighlightedBuilds() {
        val versions = listOf(version("1.0", featured = true), version("2.0"))

        val filtered = versions.filteredBy(VersionFilter(featuredOnly = true))

        assertEquals(listOf("1.0"), filtered.map(ProjectVersion::versionNumber))
    }

    @Test
    fun searchLooksAtTheChangelogAndTheFileName() {
        val versions = listOf(
            version("1.0", changelog = "Fixes the crash on startup"),
            version("2.0", fileName = "legacy-build.jar"),
            version("3.0"),
        )

        assertEquals(listOf("1.0"), versions.filteredBy(VersionFilter(query = "crash")).map(ProjectVersion::versionNumber))
        assertEquals(listOf("2.0"), versions.filteredBy(VersionFilter(query = "legacy")).map(ProjectVersion::versionNumber))
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpace() {
        val versions = listOf(version("1.0", changelog = "Adds Sodium support"))

        assertEquals(1, versions.filteredBy(VersionFilter(query = "  sodium ")).size)
    }

    @Test
    fun facetsOnlyOfferWhatTheProjectActuallyShips() {
        val versions = listOf(
            version("1.0", channel = "beta", loaders = listOf("fabric"), gameVersions = listOf("1.21")),
            version("2.0", channel = "release", loaders = listOf("forge"), gameVersions = listOf("1.20.1")),
        )

        val facets = versions.versionFacets()

        assertEquals(listOf("release", "beta"), facets.channels)
        assertEquals(listOf("fabric", "forge"), facets.loaders)
        assertEquals(listOf("1.21", "1.20.1"), facets.gameVersions)
    }

    @Test
    fun gameVersionsSortNewestFirstRatherThanAlphabetically() {
        val versions = listOf(
            version("1.0", gameVersions = listOf("1.9", "1.10", "1.20.1", "1.21", "1.8.9")),
        )

        val facets = versions.versionFacets()

        assertEquals(listOf("1.21", "1.20.1", "1.10", "1.9", "1.8.9"), facets.gameVersions)
    }

    @Test
    fun snapshotsAreKeptAfterTheNumberedReleases() {
        val versions = listOf(version("1.0", gameVersions = listOf("24w14a", "1.21", "23w31a")))

        val facets = versions.versionFacets()

        assertEquals(listOf("1.21", "24w14a", "23w31a"), facets.gameVersions)
    }

    @Test
    fun togglingAddsThenRemoves() {
        val filter = VersionFilter().toggleLoader("fabric")
        assertTrue(filter.loaders.contains("fabric"))
        assertTrue(filter.isActive)

        val cleared = filter.toggleLoader("fabric")
        assertFalse(cleared.loaders.contains("fabric"))
        assertFalse(cleared.isActive)
    }
}
