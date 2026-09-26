package com.ryntra.shared.model

/**
 * Public Modrinth catalogue browsing. See GET `/v2/search`.
 *
 * This is the read-only half of the app: everything here describes projects the signed-in user
 * does not necessarily own, so nothing in this file grants an edit path.
 */
enum class ProjectSearchSort(val apiValue: String) {
    Relevance("relevance"),
    Downloads("downloads"),
    Follows("follows"),
    Updated("updated"),
    Newest("newest"),
    ;

    companion object {
        /** Swift cannot reach a Kotlin enum's `entries`, so the sort picker reads this instead. */
        val all: List<ProjectSearchSort> = entries.toList()

        fun fromApi(value: String): ProjectSearchSort =
            entries.firstOrNull { it.apiValue == value } ?: Relevance
    }
}

/**
 * Content types the catalogue can be filtered by.
 *
 * Modrinth also exposes `minecraft_java_server`, but that is its paid server hosting product
 * rather than downloadable content, so it is deliberately absent here.
 */
enum class BrowseCategory(val apiValue: String?) {
    All(null),
    Mod("mod"),
    Plugin("plugin"),
    DataPack("datapack"),
    Shader("shader"),
    ResourcePack("resourcepack"),
    Modpack("modpack"),
    ;

    companion object {
        /** Every browsable category, in the order the catalogue tabs show them. */
        val browsable: List<BrowseCategory> = entries.toList()

        fun fromApi(value: String?): BrowseCategory =
            entries.firstOrNull { it.apiValue == value } ?: All
    }
}

data class ProjectSearchQuery(
    val text: String = "",
    val category: BrowseCategory = BrowseCategory.All,
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val sort: ProjectSearchSort = ProjectSearchSort.Relevance,
    val offset: Int = 0,
    val limit: Int = PAGE_SIZE,
) {
    val hasText: Boolean get() = text.isNotBlank()

    val hasFilters: Boolean
        get() = category != BrowseCategory.All || gameVersions.isNotEmpty() || loaders.isNotEmpty()

    /** Relevance is meaningless without a query, so an unfiltered browse leads with popularity. */
    val effectiveSort: ProjectSearchSort
        get() = if (!hasText && sort == ProjectSearchSort.Relevance) ProjectSearchSort.Downloads else sort

    fun withText(text: String): ProjectSearchQuery = copy(text = text, offset = 0)

    fun withCategory(category: BrowseCategory): ProjectSearchQuery = copy(
        category = category,
        // Loaders are per content type: keeping Fabric selected while switching to resource packs
        // would silently return nothing.
        loaders = emptyList(),
        offset = 0,
    )

    fun withSort(sort: ProjectSearchSort): ProjectSearchQuery = copy(sort = sort, offset = 0)

    fun togglingGameVersion(version: String): ProjectSearchQuery = copy(
        gameVersions = gameVersions.toggled(version),
        offset = 0,
    )

    fun togglingLoader(loader: String): ProjectSearchQuery = copy(
        loaders = loaders.toggled(loader),
        offset = 0,
    )

    fun cleared(): ProjectSearchQuery = ProjectSearchQuery(text = text, sort = sort)

    /** Resets what the filter sheet sets, keeping the content type chosen above it. */
    fun withoutVersionAndLoaderFilters(): ProjectSearchQuery = copy(gameVersions = emptyList(), loaders = emptyList(), offset = 0)

    fun nextPage(): ProjectSearchQuery = copy(offset = offset + limit)

    private fun List<String>.toggled(value: String): List<String> =
        if (value in this) filterNot { it == value } else this + value

    companion object {
        const val PAGE_SIZE = 20

        /** Swift has no access to Kotlin default arguments; this is the unfiltered starting point. */
        fun initial(): ProjectSearchQuery = ProjectSearchQuery()
    }
}

/** One catalogue entry. Carries just enough to render a card and to seed the detail screen. */
data class ProjectSearchHit(
    val projectId: String,
    val slug: String? = null,
    val title: String,
    val description: String = "",
    val projectType: String = "project",
    val author: String? = null,
    val organization: String? = null,
    val categories: List<String> = emptyList(),
    val displayCategories: List<String> = emptyList(),
    val downloads: Long = 0,
    val follows: Long = 0,
    val iconUrl: String? = null,
    val galleryUrl: String? = null,
    val gameVersions: List<String> = emptyList(),
    val clientSide: String = "unknown",
    val serverSide: String = "unknown",
    val license: String? = null,
    val dateModified: String? = null,
) {
    /** Stable key for opening the project; the slug reads better in logs and deep links. */
    val reference: String get() = slug?.takeIf { it.isNotBlank() } ?: projectId

    val byline: String? get() = organization?.takeIf { it.isNotBlank() } ?: author?.takeIf { it.isNotBlank() }

    /** Search results fold loaders into `categories`, so the same list feeds both arguments. */
    fun displayKind(): ProjectDisplayKind = ProjectDisplayKind.from(
        projectType = projectType,
        loaders = categories,
        categories = categories,
    )

    /** Newest supported game version, which is what a browser scanning a list actually looks for. */
    fun latestGameVersion(): String? = gameVersions.lastOrNull()

    /**
     * Stable key for the platform type label. Kotlin/Native rewrites enum entry names on export,
     * so the Apple UI matches on this rather than on a bridged [ProjectDisplayKind] case.
     */
    val kindKey: String
        get() = when (displayKind()) {
            ProjectDisplayKind.Mod -> "mod"
            ProjectDisplayKind.Plugin -> "plugin"
            ProjectDisplayKind.Hybrid -> "hybrid"
            ProjectDisplayKind.Modpack -> "modpack"
            ProjectDisplayKind.ResourcePack -> "resourcepack"
            ProjectDisplayKind.Shader -> "shader"
            ProjectDisplayKind.DataPack -> "datapack"
            ProjectDisplayKind.Server -> "server"
            ProjectDisplayKind.Project -> "project"
        }

    /**
     * Seed for the shared detail screen, which reloads the full project itself. Search results
     * omit the body, gallery and team, so only the fields a card already showed are carried over
     * and the rest arrive with the real fetch.
     */
    fun toProjectSeed(): Project = Project(
        id = projectId,
        slug = slug,
        title = title,
        description = description,
        projectType = projectType,
        categories = categories,
        clientSide = clientSide,
        serverSide = serverSide,
        iconUrl = iconUrl,
        downloads = downloads,
        followers = follows,
        organization = organization,
        updated = dateModified,
    )
}

data class ProjectSearchPage(
    val hits: List<ProjectSearchHit> = emptyList(),
    val offset: Int = 0,
    val limit: Int = ProjectSearchQuery.PAGE_SIZE,
    val totalHits: Int = 0,
) {
    val hasMore: Boolean get() = offset + hits.size < totalHits
}

/** Shown while the search field is empty, so the catalogue never opens as a blank page. */
data class BrowseHighlights(
    val popular: List<ProjectSearchHit> = emptyList(),
    val recentlyUpdated: List<ProjectSearchHit> = emptyList(),
) {
    val isEmpty: Boolean get() = popular.isEmpty() && recentlyUpdated.isEmpty()
}

data class GameVersion(
    val version: String,
    val versionType: String = "release",
    val isMajor: Boolean = false,
) {
    val isRelease: Boolean get() = versionType == "release"
}

data class ModLoader(
    val name: String,
    val supportedProjectTypes: List<String> = emptyList(),
    /** Modrinth's own SVG for the loader, stroked with `currentColor`. */
    val iconSvg: String? = null,
)

/** Filter options fetched once from Modrinth's tag routes. */
data class BrowseMetadata(
    val gameVersions: List<GameVersion> = emptyList(),
    val loaders: List<ModLoader> = emptyList(),
) {
    /**
     * Snapshots outnumber releases nine to one and nobody filters by them, so the picker offers
     * releases only, newest first.
     */
    fun releaseVersions(limit: Int = MAX_VERSION_OPTIONS): List<String> =
        gameVersions.filter { it.isRelease }.map { it.version }.take(limit)

    /** The same list at its default size, for callers that cannot pass a Kotlin default. */
    val defaultReleaseVersions: List<String> get() = releaseVersions()

    /** Loaders Modrinth accepts for [category]; an unfiltered browse offers the common ones. */
    fun loadersFor(category: BrowseCategory): List<String> {
        val projectType = category.apiValue ?: return loaders
            .filter { it.name in COMMON_LOADERS }
            .map { it.name }
        return loaders.filter { projectType in it.supportedProjectTypes }.map { it.name }
    }

    /** The SVG Modrinth draws next to [loader], if it has one. */
    fun loaderIcon(loader: String): String? = loaders.firstOrNull { it.name == loader }?.iconSvg

    companion object {
        const val MAX_VERSION_OPTIONS = 40

        private val COMMON_LOADERS = setOf(
            "fabric",
            "forge",
            "neoforge",
            "quilt",
            "paper",
            "spigot",
            "bukkit",
            "velocity",
            "datapack",
        )
    }
}

/** Recent queries, most recent first. Storage is per platform; the rules are not. */
object ProjectSearchHistory {
    const val MAX_ENTRIES = 8

    fun adding(history: List<String>, query: String): List<String> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return history
        return (listOf(normalized) + history.filterNot { it.equals(normalized, ignoreCase = true) })
            .take(MAX_ENTRIES)
    }

    fun removing(history: List<String>, query: String): List<String> =
        history.filterNot { it.equals(query.trim(), ignoreCase = true) }
}
