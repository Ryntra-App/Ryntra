package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.AffiliateCodeStats
import com.ryntra.shared.model.AffiliateReport
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
import kotlinx.serialization.json.jsonPrimitive

/**
 * Affiliate code performance from `POST /v3/analytics`. The request asks for one time slice over
 * the whole range, so every code comes back as a single total per metric.
 */
internal class AffiliateEndpoints(
    private val client: HttpClient,
) {
    suspend fun getReport(startTime: String, endTime: String, rangeDays: Int, token: String): AffiliateReport {
        val root: JsonElement = client.post(ANALYTICS_URL) {
            authorize(token)
            // Modrinth computes analytics on request; the same allowance the charts get.
            timeout {
                requestTimeoutMillis = AFFILIATE_TIMEOUT_MILLIS
                socketTimeoutMillis = AFFILIATE_TIMEOUT_MILLIS
            }
            contentType(ContentType.Application.Json)
            setBody(affiliateRequest(startTime, endTime))
        }.decode()
        return AffiliateReport(rangeDays = rangeDays, codes = parseAffiliateStats(root))
    }

    private companion object {
        const val ANALYTICS_URL = "https://api.modrinth.com/v3/analytics"
        const val AFFILIATE_TIMEOUT_MILLIS = 60_000L
    }
}

internal fun affiliateRequest(startTime: String, endTime: String): JsonObject {
    val byCode = JsonObject(mapOf("bucket_by" to JsonArray(listOf(JsonPrimitive("affiliate_code_id")))))
    return JsonObject(
        mapOf(
            "time_range" to JsonObject(
                mapOf(
                    "start" to JsonPrimitive(startTime),
                    "end" to JsonPrimitive(endTime),
                    "resolution" to JsonObject(mapOf("slices" to JsonPrimitive(1))),
                ),
            ),
            "return_metrics" to JsonObject(
                mapOf(
                    "affiliate_code_clicks" to byCode,
                    "affiliate_code_conversions" to byCode,
                    "affiliate_code_revenue" to byCode,
                ),
            ),
        ),
    )
}

/**
 * The response is a list of time slices, each a list of entries. Affiliate entries carry
 * `source_affiliate_code` and a `metric_kind`; project entries in the same list are skipped.
 */
internal fun parseAffiliateStats(root: JsonElement): List<AffiliateCodeStats> {
    val entries = (root as? JsonArray).orEmpty()
        .flatMap { slice -> (slice as? JsonArray).orEmpty() }
        .mapNotNull { it as? JsonObject }
    val byCode = linkedMapOf<String, AffiliateCodeStats>()
    entries.forEach { entry ->
        val code = entry.text("source_affiliate_code") ?: return@forEach
        val current = byCode[code] ?: AffiliateCodeStats(id = code)
        byCode[code] = when (entry.text("metric_kind")) {
            "clicks" -> current.copy(clicks = current.clicks + entry.number("clicks").toLong())
            "conversions" -> current.copy(conversions = current.conversions + entry.number("conversions").toLong())
            "revenue" -> current.copy(revenue = current.revenue + entry.number("revenue"))
            else -> current
        }
    }
    return byCode.values.sortedWith(
        compareByDescending<AffiliateCodeStats> { it.revenue }.thenByDescending { it.clicks },
    )
}

private fun JsonObject.text(key: String): String? =
    (get(key) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }

private fun JsonObject.number(key: String): Double =
    (get(key) as? JsonPrimitive)?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
