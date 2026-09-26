package com.ryntra.shared.network.modrinth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Downloads per day across all of the caller's projects, for the widget chart.
 *
 * Asks only for downloads, not grouped by anything, so each day comes back as one small
 * entry per project — a fraction of what the in-app analytics request carries.
 */
internal class DownloadSeriesEndpoints(
    private val client: HttpClient,
) {
    suspend fun getDailyDownloads(startTime: String, endTime: String, days: Int, token: String): List<Long> {
        val root: JsonElement = client.post(ANALYTICS_URL) {
            authorize(token)
            timeout {
                requestTimeoutMillis = SERIES_TIMEOUT_MILLIS
                socketTimeoutMillis = SERIES_TIMEOUT_MILLIS
            }
            contentType(ContentType.Application.Json)
            setBody(downloadSeriesRequest(startTime, endTime, days))
        }.decode()
        return parseDailyDownloads(root, days)
    }

    private companion object {
        const val ANALYTICS_URL = "https://api.modrinth.com/v3/analytics"
        const val SERIES_TIMEOUT_MILLIS = 60_000L
    }
}

internal fun downloadSeriesRequest(startTime: String, endTime: String, days: Int): JsonObject = JsonObject(
    mapOf(
        "time_range" to JsonObject(
            mapOf(
                "start" to JsonPrimitive(startTime),
                "end" to JsonPrimitive(endTime),
                "resolution" to JsonObject(mapOf("slices" to JsonPrimitive(days))),
            ),
        ),
        "return_metrics" to JsonObject(
            mapOf("project_downloads" to JsonObject(mapOf("bucket_by" to JsonArray(emptyList())))),
        ),
    ),
)

/** One total per slice, oldest first, always [days] long so the chart's x axis is fixed. */
internal fun parseDailyDownloads(root: JsonElement, days: Int): List<Long> {
    val slices = (root as? JsonArray).orEmpty()
    return List(days) { index ->
        (slices.getOrNull(index) as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .filter { (it["metric_kind"] as? JsonPrimitive)?.content == "downloads" }
            .sumOf { (it["downloads"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L }
    }
}
