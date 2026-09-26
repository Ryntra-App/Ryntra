package com.ryntra.shared.model

import com.ryntra.shared.network.apiJson
import com.ryntra.shared.network.modrinth.parseDailyDownloads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetSnapshotTest {
    private fun snapshot(series: List<Long>) = WidgetSnapshot(
        username = "alex",
        downloads = 0,
        followers = 0,
        projectCount = 0,
        dailyDownloads = series,
        updatedAtEpochMillis = 0,
    )

    @Test
    fun windowComparesAgainstThePeriodJustBefore() {
        val window = snapshot(listOf(5L, 5L, 5L, 10L, 10L, 10L))
            .downloadWindow(3)!!

        assertEquals(listOf(10L, 10L, 10L), window.values)
        assertEquals(30, window.total)
        assertEquals(15, window.previousTotal)
        assertEquals(100.0, window.changePercent)
    }

    @Test
    fun theLongestWindowHasNothingToCompareWith() {
        val window = snapshot(List(90) { 1L }).downloadWindow(90)!!

        assertEquals(90, window.total)
        assertNull(window.previousTotal)
    }

    @Test
    fun snapshotsFromTheAppKeepTheSeriesOnDisk() {
        val stored = snapshot(List(90) { 2L }).copy(dailyDownloadsUpdatedAtEpochMillis = 5)

        val merged = snapshot(emptyList()).withSeriesFrom(stored)

        assertEquals(stored.dailyDownloads, merged.dailyDownloads)
        assertEquals(5L, merged.dailyDownloadsUpdatedAtEpochMillis)
    }

    @Test
    fun dailyDownloadsSumEveryProjectPerDayAndPadMissingDays() {
        val root = apiJson.parseToJsonElement(
            """[
                [{"source_project":"a","metric_kind":"downloads","downloads":3},
                 {"source_project":"b","metric_kind":"downloads","downloads":4}],
                [{"source_project":"a","metric_kind":"downloads","downloads":1}]
            ]""",
        )

        assertEquals(listOf(7L, 1L, 0L), parseDailyDownloads(root, days = 3))
    }
}
