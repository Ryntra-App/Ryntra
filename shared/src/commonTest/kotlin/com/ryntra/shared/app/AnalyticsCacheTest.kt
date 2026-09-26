package com.ryntra.shared.app

import com.ryntra.shared.model.AnalyticsMetrics
import com.ryntra.shared.model.AnalyticsPoint
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.AnalyticsReport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration

class AnalyticsCacheTest {
    private fun query(
        projectIds: List<String> = listOf("alpha", "beta"),
        slices: Int = 60,
        currentSlices: Int = 30,
        endTime: String = "2026-09-22T00:00:00Z",
        periodDays: Int = 30,
    ) = AnalyticsQuery(
        startTime = "2026-07-24T00:00:00Z",
        endTime = endTime,
        slices = slices,
        projectIds = projectIds,
        currentStartTime = "2026-08-23T00:00:00Z",
        currentSlices = currentSlices,
        periodDays = periodDays,
    )

    private fun report(downloads: Double) = AnalyticsReport(
        points = listOf(AnalyticsPoint("2026-08-23T00:00:00Z", AnalyticsMetrics(downloads = downloads))),
        coreStatus = 200,
    )

    @Test
    fun theSameRangeIsServedBackWithoutAnotherRequest() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(generation = 0)

        cache.put(key, report(downloads = 12.0))

        assertEquals(12.0, cache.get(key)?.totals?.downloads)
    }

    @Test
    fun aMovingEndTimeStillHitsTheSameEntry() = runTest {
        val cache = AnalyticsCache()
        cache.put(query(endTime = "2026-09-22T00:00:00Z").analyticsCacheKey(0), report(downloads = 5.0))

        // Every load asks for "now", so the timestamps differ on each call. Only the
        // shape of the range identifies it.
        val later = query(endTime = "2026-09-22T00:05:00Z").analyticsCacheKey(0)

        assertEquals(5.0, cache.get(later)?.totals?.downloads)
    }

    @Test
    fun projectOrderDoesNotSplitTheEntry() = runTest {
        val cache = AnalyticsCache()
        cache.put(query(projectIds = listOf("alpha", "beta")).analyticsCacheKey(0), report(downloads = 7.0))

        val reordered = query(projectIds = listOf("beta", "alpha")).analyticsCacheKey(0)

        assertEquals(7.0, cache.get(reordered)?.totals?.downloads)
    }

    @Test
    fun aDifferentRangeIsAMiss() = runTest {
        val cache = AnalyticsCache()
        cache.put(query(slices = 60, currentSlices = 30).analyticsCacheKey(0), report(downloads = 9.0))

        assertNull(cache.get(query(slices = 180, currentSlices = 90).analyticsCacheKey(0)))
    }

    @Test
    fun rangesSlicedAlikeButSpanningDifferentDaysAreSeparateEntries() = runTest {
        val cache = AnalyticsCache()
        // Both long ranges are 45 slices a period; only the span tells them apart.
        cache.put(query(slices = 90, currentSlices = 45, periodDays = 90).analyticsCacheKey(0), report(downloads = 90.0))

        assertNull(cache.get(query(slices = 90, currentSlices = 45, periodDays = 180).analyticsCacheKey(0)))
    }

    @Test
    fun aDifferentProjectSetIsAMiss() = runTest {
        val cache = AnalyticsCache()
        cache.put(query(projectIds = listOf("alpha")).analyticsCacheKey(0), report(downloads = 9.0))

        assertNull(cache.get(query(projectIds = listOf("alpha", "gamma")).analyticsCacheKey(0)))
    }

    @Test
    fun anotherAccountNeverReadsTheEntryLeftBehind() = runTest {
        val cache = AnalyticsCache()
        cache.put(query().analyticsCacheKey(generation = 0), report(downloads = 42.0))

        // Signing out bumps the generation, so a request that was still in flight
        // cannot hand its numbers to whoever signs in next.
        assertNull(cache.get(query().analyticsCacheKey(generation = 1)))
    }

    @Test
    fun anExpiredEntryIsDropped() = runTest {
        val cache = AnalyticsCache(lifetime = Duration.ZERO)
        val key = query().analyticsCacheKey(0)
        cache.put(key, report(downloads = 3.0))

        assertNull(cache.get(key))
    }

    @Test
    fun clearingDropsEverything() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(0)
        cache.put(key, report(downloads = 3.0))

        cache.clear()

        assertNull(cache.get(key))
    }

    @Test
    fun loadsForTheSameRangeInFlightTogetherShareOneRequest() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(0)
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        val load: suspend () -> AnalyticsReport = {
            requests++
            gate.await()
            report(downloads = 8.0)
        }

        // The prefetch and the screen opening while it runs.
        val prefetch = async { cache.getOrLoad(key, backgroundScope, load) }
        val screen = async { cache.getOrLoad(key, backgroundScope, load) }
        runCurrent()
        gate.complete(Unit)

        assertEquals(8.0, prefetch.await().totals.downloads)
        assertEquals(8.0, screen.await().totals.downloads)
        assertEquals(1, requests)
    }

    @Test
    fun aFinishedLoadIsServedWithoutAnotherRequest() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(0)
        var requests = 0
        val load: suspend () -> AnalyticsReport = { requests++; report(downloads = 4.0) }

        cache.getOrLoad(key, backgroundScope, load)
        cache.getOrLoad(key, backgroundScope, load)

        assertEquals(1, requests)
    }

    @Test
    fun aFailedReportIsNotKeptSoTheNextOpenTriesAgain() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(0)
        var requests = 0
        val load: suspend () -> AnalyticsReport = { requests++; AnalyticsReport(coreStatus = 503) }

        cache.getOrLoad(key, backgroundScope, load)
        cache.getOrLoad(key, backgroundScope, load)

        assertEquals(2, requests)
    }

    @Test
    fun invalidatingForcesTheNextLoadToFetch() = runTest {
        val cache = AnalyticsCache()
        val key = query().analyticsCacheKey(0)
        var requests = 0
        val load: suspend () -> AnalyticsReport = { requests++; report(downloads = 1.0) }

        cache.getOrLoad(key, backgroundScope, load)
        cache.invalidate(key)
        cache.getOrLoad(key, backgroundScope, load)

        assertEquals(2, requests)
    }
}
