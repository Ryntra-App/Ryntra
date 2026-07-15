package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsReportTest {
    @Test
    fun totalsAndComparisonIncludeRevenueFromItsScopedRequest() {
        val report = AnalyticsReport(
            points = listOf(point(downloads = 20.0, views = 40.0)),
            revenuePoints = listOf(point(revenue = 3.5)),
            previousPoints = listOf(point(downloads = 10.0, views = 50.0)),
            previousRevenuePoints = listOf(point(revenue = 2.0)),
        )

        assertEquals(20.0, report.periodTotals.downloads)
        assertEquals(3.5, report.periodTotals.revenue)
        assertEquals(100.0, report.percentageChange(report.periodTotals.downloads, report.previousPeriodTotals.downloads))
        assertEquals(-20.0, report.percentageChange(report.periodTotals.views, report.previousPeriodTotals.views))
    }

    @Test
    fun missingPreviousPeriodProducesStableComparison() {
        val report = AnalyticsReport()

        assertEquals(0.0, report.percentageChange(0.0, 0.0))
        assertEquals(100.0, report.percentageChange(4.0, 0.0))
    }

    private fun point(
        downloads: Double = 0.0,
        views: Double = 0.0,
        revenue: Double = 0.0,
    ) = AnalyticsPoint(
        startTime = "2026-07-01T00:00:00Z",
        metrics = AnalyticsMetrics(downloads = downloads, views = views, revenue = revenue),
        projects = mapOf(
            "project-1" to AnalyticsMetrics(downloads = downloads, views = views, revenue = revenue),
        ),
    )
}
