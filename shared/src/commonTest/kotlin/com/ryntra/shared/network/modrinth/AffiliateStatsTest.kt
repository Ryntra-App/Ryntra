package com.ryntra.shared.network.modrinth

import com.ryntra.shared.network.apiJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AffiliateStatsTest {
    @Test
    fun metricsOfOneCodeAreMergedAndProjectEntriesIgnored() {
        val root = apiJson.parseToJsonElement(
            """{"metrics":[[
                {"source_affiliate_code":"afl1","metric_kind":"clicks","clicks":40},
                {"source_affiliate_code":"afl1","metric_kind":"conversions","conversions":3},
                {"source_affiliate_code":"afl1","metric_kind":"revenue","revenue":"4.50"},
                {"source_affiliate_code":"afl2","metric_kind":"clicks","clicks":90},
                {"source_project":"proj1","metric_kind":"views","views":12}
            ]]}""",
        )

        val stats = parseAffiliateStats(root)

        assertEquals(listOf("afl1", "afl2"), stats.map { it.id })
        assertEquals(40, stats[0].clicks)
        assertEquals(3, stats[0].conversions)
        assertEquals(4.5, stats[0].revenue)
        assertEquals("https://modrinth.gg?afl=afl1", stats[0].link)
    }

    @Test
    fun requestAsksForOneSliceOfEveryAffiliateMetric() {
        val request = affiliateRequest("2026-08-27T00:00:00Z", "2026-09-26T00:00:00Z")

        val metrics = request["return_metrics"]!!.jsonObject
        assertEquals(
            setOf("affiliate_code_clicks", "affiliate_code_conversions", "affiliate_code_revenue"),
            metrics.keys,
        )
        assertEquals(
            "1",
            request["time_range"]!!.jsonObject["resolution"]!!.jsonObject["slices"]!!.jsonPrimitive.content,
        )
    }
}
