package com.ryntra.shared.model

data class AnalyticsQuery(
    val startTime: String,
    val endTime: String,
    val slices: Int,
    val projectIds: List<String>,
    val currentStartTime: String = startTime,
    val currentSlices: Int = slices,
    /**
     * Days in the current period. Once long ranges share a slice count (see
     * [AnalyticsResolution]) the slices alone no longer tell 90 days from 180, so the
     * span has to travel with the query.
     */
    val periodDays: Int,
)

/**
 * Statuses an analytics report carries when Modrinth never produced an HTTP answer.
 *
 * They used to share a single 0, so a request that simply timed out on a long range was
 * reported to the user as a response that could not be read.
 */
object AnalyticsStatus {
    const val DECODE_FAILED = 0
    const val UNREACHABLE = -1
    const val TIMED_OUT = 408
}

/**
 * How finely a range is sliced for Modrinth.
 *
 * Every slice comes back as one object per project per metric, and the range is asked
 * for twice over to compare against the previous period. At one slice a day, 180 days
 * was 360 slices before revenue doubled it again — tens of thousands of objects for a
 * chart a phone draws a few dozen points wide. Long ranges therefore use whole-day
 * buckets of several days. The count always divides the range so every bucket spans
 * the same number of days, which is what the charts rely on to label them.
 */
object AnalyticsResolution {
    private const val MAX_SLICES_PER_PERIOD = 45

    fun slicesPerPeriod(rangeDays: Int): Int {
        require(rangeDays > 0) { "An analytics range must span at least one day." }
        var slices = minOf(rangeDays, MAX_SLICES_PER_PERIOD)
        while (rangeDays % slices != 0) slices--
        return slices
    }

    fun daysPerSlice(rangeDays: Int): Int = rangeDays / slicesPerPeriod(rangeDays)
}

data class AnalyticsMetrics(
    val downloads: Double = 0.0,
    val views: Double = 0.0,
    val playtimeSeconds: Double = 0.0,
    val revenue: Double = 0.0,
) {
    operator fun plus(other: AnalyticsMetrics) = AnalyticsMetrics(
        downloads = downloads + other.downloads,
        views = views + other.views,
        playtimeSeconds = playtimeSeconds + other.playtimeSeconds,
        revenue = revenue + other.revenue,
    )
}

data class AnalyticsPoint(
    val startTime: String,
    val metrics: AnalyticsMetrics,
    val projects: Map<String, AnalyticsMetrics> = emptyMap(),
)

data class AnalyticsProjectEvent(
    val projectId: String,
    val timestamp: String,
    val kind: String,
    val versionId: String? = null,
    val versionName: String? = null,
    val versionNumber: String? = null,
    val statusFrom: String? = null,
    val statusTo: String? = null,
)

data class AnalyticsReport(
    val points: List<AnalyticsPoint> = emptyList(),
    val revenuePoints: List<AnalyticsPoint> = emptyList(),
    val previousPoints: List<AnalyticsPoint> = emptyList(),
    val previousRevenuePoints: List<AnalyticsPoint> = emptyList(),
    val events: List<AnalyticsProjectEvent> = emptyList(),
    val periodStartTime: String = "",
    val periodEndTime: String = "",
    val coreStatus: Int = 0,
    val revenueStatus: Int = 0,
) {
    val isCoreAvailable: Boolean get() = coreStatus in 200..299
    val isRevenueAvailable: Boolean get() = revenueStatus in 200..299
    val totals: AnalyticsMetrics get() = points.fold(AnalyticsMetrics()) { total, point -> total + point.metrics }
    val previousTotals: AnalyticsMetrics
        get() = previousPoints.fold(AnalyticsMetrics()) { total, point -> total + point.metrics }
    val revenueTotal: Double get() = revenuePoints.sumOf { it.metrics.revenue }
    val previousRevenueTotal: Double get() = previousRevenuePoints.sumOf { it.metrics.revenue }
    val periodTotals: AnalyticsMetrics get() = totals.copy(revenue = revenueTotal)
    val previousPeriodTotals: AnalyticsMetrics get() = previousTotals.copy(revenue = previousRevenueTotal)

    fun projectMetrics(projectId: String): AnalyticsMetrics {
        return metricsForProject(projectId, points, revenuePoints)
    }

    fun previousProjectMetrics(projectId: String): AnalyticsMetrics {
        return metricsForProject(projectId, previousPoints, previousRevenuePoints)
    }

    fun percentageChange(current: Double, previous: Double): Double = when {
        previous == 0.0 && current == 0.0 -> 0.0
        previous == 0.0 -> 100.0
        else -> ((current - previous) / previous) * 100.0
    }

    private fun metricsForProject(
        projectId: String,
        corePoints: List<AnalyticsPoint>,
        projectRevenuePoints: List<AnalyticsPoint>,
    ): AnalyticsMetrics {
        val core = corePoints.fold(AnalyticsMetrics()) { total, point ->
            total + point.projects.getOrElse(projectId) { AnalyticsMetrics() }
        }
        val revenue = projectRevenuePoints.sumOf { point -> point.projects[projectId]?.revenue ?: 0.0 }
        return core.copy(revenue = revenue)
    }
}
