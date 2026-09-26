package com.ryntra.shared.app

import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.AnalyticsReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Keeps recently fetched analytics, and shares a request that is still in flight.
 *
 * One range is an expensive Modrinth call: a doubled window, fetched twice over because
 * core metrics and revenue are separate requests. Without this, flipping between ranges
 * pays that cost again for data that is seconds old — and opening the tab while a
 * prefetch for the same range is still running starts a second, identical request.
 */
internal class AnalyticsCache(
    private val lifetime: Duration = DEFAULT_LIFETIME,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<AnalyticsCacheKey, Entry>()
    private val inFlight = mutableMapOf<AnalyticsCacheKey, Deferred<AnalyticsReport>>()

    suspend fun get(key: AnalyticsCacheKey): AnalyticsReport? = mutex.withLock { freshLocked(key) }

    suspend fun put(key: AnalyticsCacheKey, report: AnalyticsReport) = mutex.withLock {
        entries[key] = Entry(report, timeSource.markNow())
    }

    /**
     * Returns a fresh entry, joins a load already running for [key], or starts [load] in
     * [scope]. The load belongs to [scope] rather than to the caller, so a screen that
     * stops waiting — a range switched mid-request — does not cancel it for the others.
     */
    suspend fun getOrLoad(
        key: AnalyticsCacheKey,
        scope: CoroutineScope,
        load: suspend () -> AnalyticsReport,
    ): AnalyticsReport {
        val pending = mutex.withLock {
            freshLocked(key)?.let { return it }
            inFlight.getOrPut(key) {
                scope.async {
                    try {
                        load().also { report ->
                            // A partial report would otherwise be served back as though
                            // it were complete.
                            if (report.isCoreAvailable) put(key, report)
                        }
                    } finally {
                        withContext(NonCancellable) { mutex.withLock { inFlight.remove(key) } }
                    }
                }
            }
        }
        return pending.await()
    }

    suspend fun invalidate(key: AnalyticsCacheKey) {
        mutex.withLock { entries.remove(key) }
    }

    suspend fun clear() = mutex.withLock {
        entries.clear()
        inFlight.clear()
    }

    private fun freshLocked(key: AnalyticsCacheKey): AnalyticsReport? {
        val entry = entries[key] ?: return null
        if (entry.storedAt.elapsedNow() >= lifetime) {
            entries.remove(key)
            return null
        }
        return entry.report
    }

    private data class Entry(
        val report: AnalyticsReport,
        val storedAt: TimeMark,
    )

    private companion object {
        // Long enough to cover flipping between ranges, short enough that someone
        // watching a release land still sees today's numbers move.
        val DEFAULT_LIFETIME = 5.minutes
    }
}

/**
 * Ranges are requested with a moving `endTime`, so their timestamps never repeat and
 * cannot identify an entry. A range is identified by how many days it covers, how it
 * is sliced and over which projects, plus the account generation it was fetched under.
 * The day count has to be there explicitly: 90 and 180 days are both 45 slices a
 * period, and keying on slices alone served the 90-day report for 180.
 */
internal data class AnalyticsCacheKey(
    val generation: Int,
    val projectIds: List<String>,
    val periodDays: Int,
    val slices: Int,
    val currentSlices: Int,
)

internal fun AnalyticsQuery.analyticsCacheKey(generation: Int) = AnalyticsCacheKey(
    generation = generation,
    projectIds = projectIds.sorted(),
    periodDays = periodDays,
    slices = slices,
    currentSlices = currentSlices,
)
