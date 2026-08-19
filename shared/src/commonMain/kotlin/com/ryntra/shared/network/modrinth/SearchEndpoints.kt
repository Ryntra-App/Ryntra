package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ProjectSearchHit
import com.ryntra.shared.model.ProjectSearchPage
import com.ryntra.shared.model.ProjectSearchQuery
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Public catalogue search. See GET `/v2/search`.
 *
 * The route is unauthenticated, but the token is still sent so the request counts against the
 * user's own rate limit rather than the shared anonymous pool.
 */
internal class SearchEndpoints(
    private val client: HttpClient,
) {
    suspend fun search(query: ProjectSearchQuery, token: String?): ProjectSearchPage {
        val response: SearchResponseDto = client.get("search") {
            token?.takeIf { it.isNotBlank() }?.let { authorize(it) }
            parameter("query", query.text.trim())
            parameter("index", query.effectiveSort.apiValue)
            parameter("offset", query.offset)
            parameter("limit", query.limit)
            buildSearchFacets(query)?.let { parameter("facets", it) }
        }.decode()
        return response.toPage()
    }
}

/**
 * Modrinth facets are an array of arrays: entries within one array are OR-ed, the arrays
 * themselves are AND-ed. Loaders are not a facet of their own on v2 — they live in `categories`
 * alongside real categories.
 *
 * Returns null when nothing is filtered, because an empty `[]` still narrows the query.
 */
internal fun buildSearchFacets(query: ProjectSearchQuery): String? {
    val groups = mutableListOf<List<String>>()
    query.category.apiValue?.let { groups += listOf("project_type:$it") }
    if (query.gameVersions.isNotEmpty()) {
        groups += query.gameVersions.map { "versions:$it" }
    }
    if (query.loaders.isNotEmpty()) {
        groups += query.loaders.map { "categories:$it" }
    }
    if (groups.isEmpty()) return null
    return apiJson.encodeToString(groups)
}

@Serializable
private data class SearchResponseDto(
    val hits: List<SearchHitDto> = emptyList(),
    val offset: Int = 0,
    val limit: Int = ProjectSearchQuery.PAGE_SIZE,
    @SerialName("total_hits") val totalHits: Int = 0,
) {
    fun toPage(): ProjectSearchPage = ProjectSearchPage(
        hits = hits.map(SearchHitDto::toModel),
        offset = offset,
        limit = limit,
        totalHits = totalHits,
    )
}

@Serializable
private data class SearchHitDto(
    @SerialName("project_id") val projectId: String,
    val slug: String? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("project_type") val projectType: String = "project",
    val author: String? = null,
    val organization: String? = null,
    val categories: List<String> = emptyList(),
    @SerialName("display_categories") val displayCategories: List<String> = emptyList(),
    val downloads: Long = 0,
    val follows: Long = 0,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("featured_gallery") val featuredGallery: String? = null,
    val gallery: List<String> = emptyList(),
    val versions: List<String> = emptyList(),
    @SerialName("client_side") val clientSide: String = "unknown",
    @SerialName("server_side") val serverSide: String = "unknown",
    val license: String? = null,
    @SerialName("date_modified") val dateModified: String? = null,
) {
    fun toModel(): ProjectSearchHit = ProjectSearchHit(
        projectId = projectId,
        slug = slug,
        title = title.ifBlank { slug ?: projectId },
        description = description,
        projectType = projectType,
        author = author,
        organization = organization,
        categories = categories,
        displayCategories = displayCategories.ifEmpty { categories },
        downloads = downloads,
        follows = follows,
        iconUrl = iconUrl?.takeIf { it.isNotBlank() },
        galleryUrl = featuredGallery?.takeIf { it.isNotBlank() } ?: gallery.firstOrNull(),
        gameVersions = versions,
        clientSide = clientSide,
        serverSide = serverSide,
        license = license?.takeIf { it.isNotBlank() },
        dateModified = dateModified,
    )
}
