package com.ryntra.shared.model

/**
 * What the version list is narrowed to.
 *
 * A project with a few hundred versions is unreadable as a flat list, which is why
 * Modrinth's own version page filters by channel, loader and game version. An empty
 * set means "not filtering on this facet" rather than "match nothing".
 */
data class VersionFilter(
    val query: String = "",
    val channels: Set<String> = emptySet(),
    val loaders: Set<String> = emptySet(),
    val gameVersions: Set<String> = emptySet(),
    val featuredOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
            channels.isNotEmpty() ||
            loaders.isNotEmpty() ||
            gameVersions.isNotEmpty() ||
            featuredOnly

    fun toggleChannel(value: String) = copy(channels = channels.toggled(value))

    fun toggleLoader(value: String) = copy(loaders = loaders.toggled(value))

    fun toggleGameVersion(value: String) = copy(gameVersions = gameVersions.toggled(value))

    private fun Set<String>.toggled(value: String): Set<String> =
        if (contains(value)) minus(value) else plus(value)
}

/** The facet values that actually occur in a project's versions, ready to offer as filters. */
data class VersionFacets(
    val channels: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val gameVersions: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = channels.isEmpty() && loaders.isEmpty() && gameVersions.isEmpty()
}

/**
 * Swift-facing entry point.
 *
 * Kotlin extensions on `List` do not bridge to Swift under a name worth relying on, and
 * this project already keeps a member function where Swift needs one — see
 * `Project.attentionState()`. The extensions below delegate here so there is still one
 * implementation.
 */
object VersionFiltering {
    fun facets(versions: List<ProjectVersion>): VersionFacets = versions.versionFacets()

    fun apply(versions: List<ProjectVersion>, filter: VersionFilter): List<ProjectVersion> =
        versions.filteredBy(filter)
}

/**
 * Offering every loader and game version Modrinth knows about would bury the handful a
 * project actually ships for, so the choices come from the versions themselves.
 */
fun List<ProjectVersion>.versionFacets(): VersionFacets = VersionFacets(
    channels = map(ProjectVersion::versionType)
        .filter(String::isNotBlank)
        .distinct()
        .sortedBy { channel -> CHANNEL_ORDER.indexOf(channel).takeIf { it >= 0 } ?: CHANNEL_ORDER.size },
    loaders = flatMap(ProjectVersion::loaders).filter(String::isNotBlank).distinct().sorted(),
    gameVersions = flatMap(ProjectVersion::gameVersions)
        .filter(String::isNotBlank)
        .distinct()
        .sortedWith(GameVersionComparator),
)

fun List<ProjectVersion>.filteredBy(filter: VersionFilter): List<ProjectVersion> {
    if (!filter.isActive) return this
    val query = filter.query.trim()
    return filter { version ->
        (filter.channels.isEmpty() || version.versionType in filter.channels) &&
            (filter.loaders.isEmpty() || version.loaders.any { it in filter.loaders }) &&
            (filter.gameVersions.isEmpty() || version.gameVersions.any { it in filter.gameVersions }) &&
            (!filter.featuredOnly || version.featured) &&
            (query.isEmpty() || version.matches(query))
    }
}

private fun ProjectVersion.matches(query: String): Boolean =
    versionNumber.contains(query, ignoreCase = true) ||
        name.contains(query, ignoreCase = true) ||
        changelog.contains(query, ignoreCase = true) ||
        files.any { it.filename.contains(query, ignoreCase = true) }

private val CHANNEL_ORDER = listOf("release", "beta", "alpha")

/**
 * Newest first. Minecraft versions are dotted numbers, so a plain string sort puts
 * "1.9" above "1.10"; snapshots such as "24w14a" have no numeric form at all and are
 * kept together after the releases rather than interleaved wrongly.
 */
internal object GameVersionComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val left = a.numericParts()
        val right = b.numericParts()
        if (left == null && right == null) return b.compareTo(a)
        if (left == null) return 1
        if (right == null) return -1
        for (index in 0 until maxOf(left.size, right.size)) {
            val leftPart = left.getOrElse(index) { 0 }
            val rightPart = right.getOrElse(index) { 0 }
            if (leftPart != rightPart) return rightPart.compareTo(leftPart)
        }
        return b.compareTo(a)
    }

    private fun String.numericParts(): List<Int>? {
        val parts = split('.')
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        return numbers.takeIf { it.isNotEmpty() }
    }
}
