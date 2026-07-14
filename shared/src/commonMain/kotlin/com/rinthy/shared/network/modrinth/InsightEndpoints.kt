package com.rinthy.shared.network.modrinth

import com.rinthy.shared.model.AnalyticsMetrics
import com.rinthy.shared.model.AnalyticsPoint
import com.rinthy.shared.model.AnalyticsProjectEvent
import com.rinthy.shared.model.AnalyticsQuery
import com.rinthy.shared.model.PayoutTransaction
import com.rinthy.shared.network.AnalyticsResponse
import com.rinthy.shared.network.PayoutBalanceResponse
import com.rinthy.shared.network.PayoutHistoryResponse
import com.rinthy.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class InsightEndpoints(
    private val client: HttpClient,
) {
    suspend fun getAnalytics(query: AnalyticsQuery, includeRevenue: Boolean, token: String): AnalyticsResponse =
        try {
            val response = client.post("https://api.modrinth.com/v3/analytics") {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(AnalyticsRequest.from(query, includeRevenue))
            }
            if (!response.status.isSuccess()) {
                AnalyticsResponse(status = response.status.value)
            } else {
                val root = apiJson.parseToJsonElement(response.bodyAsText())
                AnalyticsResponse(
                    status = response.status.value,
                    points = normalizeAnalytics(root, query),
                    events = normalizeProjectEvents(root),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            AnalyticsResponse(status = 0)
        }

    suspend fun getPayoutHistory(userId: String, token: String): PayoutHistoryResponse =
        try {
            val response = client.get("user/$userId/payouts") { authorize(token) }
            if (!response.status.isSuccess()) {
                PayoutHistoryResponse(status = response.status.value)
            } else {
                val root = apiJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
                val transactions = (root?.get("payouts") as? JsonArray).orEmpty().mapNotNull { element ->
                    val payout = element as? JsonObject ?: return@mapNotNull null
                    PayoutTransaction(
                        created = payout.string("created").orEmpty(),
                        amount = payout.numberOrNull("amount") ?: 0.0,
                        status = payout.string("status").orEmpty(),
                    )
                }
                PayoutHistoryResponse(
                    status = response.status.value,
                    allTime = root.numberOrNull("all_time", "balance_all_time"),
                    lastMonth = root.numberOrNull("last_month", "last_30_days"),
                    transactions = transactions,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            PayoutHistoryResponse(status = 0)
        }

    suspend fun getPayoutBalance(token: String): PayoutBalanceResponse =
        try {
            val response = client.get("https://api.modrinth.com/v3/payout/balance") { authorize(token) }
            if (!response.status.isSuccess()) {
                PayoutBalanceResponse(status = response.status.value)
            } else {
                val root = apiJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
                PayoutBalanceResponse(
                    status = response.status.value,
                    available = root.numberOrNull(
                        "available_now",
                        "availableNow",
                        "available",
                        "balance_available",
                        "balanceAvailable",
                    ),
                    pending = root.numberOrNull("pending"),
                    withdrawnLifetime = root.numberOrNull("withdrawn_lifetime", "withdrawnLifetime"),
                    total = root.numberOrNull("balance", "total_balance", "totalBalance"),
                    currency = root?.string("currency"),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            PayoutBalanceResponse(status = 0)
        }
}

@Serializable
private data class AnalyticsRequest(
    @SerialName("time_range") val timeRange: AnalyticsTimeRange,
    @SerialName("return_metrics") val returnMetrics: AnalyticsReturnMetrics,
    @SerialName("project_ids") val projectIds: List<String>,
) {
    companion object {
        fun from(query: AnalyticsQuery, includeRevenue: Boolean) = AnalyticsRequest(
            timeRange = AnalyticsTimeRange(
                start = query.startTime,
                end = query.endTime,
                resolution = AnalyticsResolution(query.slices),
            ),
            returnMetrics = if (includeRevenue) {
                AnalyticsReturnMetrics(projectRevenue = AnalyticsMetricRequest(bucketBy = listOf("project_id")))
            } else {
                AnalyticsReturnMetrics(
                    projectDownloads = AnalyticsMetricRequest(bucketBy = listOf("project_id")),
                    projectViews = AnalyticsMetricRequest(bucketBy = listOf("project_id")),
                    projectPlaytime = AnalyticsMetricRequest(bucketBy = listOf("project_id")),
                )
            },
            projectIds = query.projectIds,
        )
    }
}

@Serializable
private data class AnalyticsTimeRange(
    val start: String,
    val end: String,
    val resolution: AnalyticsResolution,
)

@Serializable
private data class AnalyticsResolution(val slices: Int)

@Serializable
private data class AnalyticsReturnMetrics(
    @SerialName("project_downloads") val projectDownloads: AnalyticsMetricRequest? = null,
    @SerialName("project_views") val projectViews: AnalyticsMetricRequest? = null,
    @SerialName("project_playtime") val projectPlaytime: AnalyticsMetricRequest? = null,
    @SerialName("project_revenue") val projectRevenue: AnalyticsMetricRequest? = null,
)

@Serializable
private data class AnalyticsMetricRequest(
    @SerialName("bucket_by") val bucketBy: List<String>,
)

private fun normalizeAnalytics(root: JsonElement, query: AnalyticsQuery): List<AnalyticsPoint> {
    val slices = when (root) {
        is JsonArray -> root
        is JsonObject -> root["metrics"] as? JsonArray
            ?: listOf("data", "results", "analytics").firstNotNullOfOrNull { key -> root[key] as? JsonArray }
        else -> null
    } ?: return emptyList()

    return slices.mapIndexed { index, slice ->
        val entries = if (slice is JsonArray) slice else JsonArray(listOf(slice))
        val projectMetrics = mutableMapOf<String, AnalyticsMetrics>()
        var totals = AnalyticsMetrics()

        entries.forEach { element ->
            val entry = element as? JsonObject ?: return@forEach
            val projectId = entry.string("source_project")
            val metric = when (entry.string("metric_kind")) {
                "downloads" -> AnalyticsMetrics(downloads = entry.number("downloads"))
                "views" -> AnalyticsMetrics(views = entry.number("views"))
                "playtime" -> AnalyticsMetrics(playtimeSeconds = entry.number("seconds"))
                "revenue" -> AnalyticsMetrics(revenue = entry.number("revenue"))
                else -> directMetrics(entry)
            }
            totals += metric
            if (projectId != null) {
                projectMetrics[projectId] = projectMetrics.getOrElse(projectId) { AnalyticsMetrics() } + metric
            }
            (entry["projects"] as? JsonObject)?.forEach { (id, rawMetrics) ->
                val metrics = (rawMetrics as? JsonObject)?.let(::directMetrics) ?: AnalyticsMetrics()
                projectMetrics[id] = projectMetrics.getOrElse(id) { AnalyticsMetrics() } + metrics
            }
        }

        AnalyticsPoint(
            startTime = entries.firstNotNullOfOrNull { (it as? JsonObject)?.string("start_time") }
                ?: interpolateTimestamp(query, index),
            metrics = totals,
            projects = projectMetrics,
        )
    }
}

private fun normalizeProjectEvents(root: JsonElement): List<AnalyticsProjectEvent> {
    val events = (root as? JsonObject)?.get("project_events") as? JsonArray ?: return emptyList()
    return events.mapNotNull { element ->
        val event = element as? JsonObject ?: return@mapNotNull null
        val projectId = event.string("project_id") ?: return@mapNotNull null
        val timestamp = event.string("timestamp") ?: return@mapNotNull null
        val kind = event.string("kind") ?: return@mapNotNull null
        AnalyticsProjectEvent(
            projectId = projectId,
            timestamp = timestamp,
            kind = kind,
            versionId = event.string("version_id"),
            versionName = event.string("version_name"),
            versionNumber = event.string("version_number"),
            statusFrom = event.string("status_from"),
            statusTo = event.string("status_to"),
        )
    }
}

private fun directMetrics(value: JsonObject) = AnalyticsMetrics(
    downloads = value.number("downloads"),
    views = value.number("views"),
    playtimeSeconds = value.number("playtime").takeIf { it != 0.0 } ?: value.number("seconds"),
    revenue = value.number("revenue"),
)

private fun JsonObject.string(key: String): String? = runCatching { get(key)?.jsonPrimitive?.content }.getOrNull()

private fun JsonObject.number(key: String): Double = runCatching {
    get(key)?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
}.getOrDefault(0.0)

private fun JsonObject?.numberOrNull(vararg keys: String): Double? {
    if (this == null) return null
    return keys.firstNotNullOfOrNull { key ->
        runCatching { get(key)?.jsonPrimitive?.content?.toDoubleOrNull() }.getOrNull()
    }
}

private fun interpolateTimestamp(query: AnalyticsQuery, index: Int): String {
    if (index == 0) return query.startTime
    return if (index >= query.slices) query.endTime else "${query.startTime}#$index"
}
