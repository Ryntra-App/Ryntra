package com.ryntra.shared.model

data class AnalyticsQuery(
    val startTime: String,
    val endTime: String,
    val slices: Int,
    val projectIds: List<String>,
    val currentStartTime: String = startTime,
    val currentSlices: Int = slices,
)

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
