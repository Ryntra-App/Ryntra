package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsResolutionTest {
    @Test
    fun shortRangesKeepOneSliceADay() {
        assertEquals(7, AnalyticsResolution.slicesPerPeriod(7))
        assertEquals(30, AnalyticsResolution.slicesPerPeriod(30))
        assertEquals(1, AnalyticsResolution.daysPerSlice(30))
    }

    @Test
    fun longRangesUseMultiDayBuckets() {
        assertEquals(45, AnalyticsResolution.slicesPerPeriod(90))
        assertEquals(2, AnalyticsResolution.daysPerSlice(90))
        assertEquals(45, AnalyticsResolution.slicesPerPeriod(180))
        assertEquals(4, AnalyticsResolution.daysPerSlice(180))
    }

    @Test
    fun everyBucketSpansTheSameNumberOfDays() {
        (1..365).forEach { rangeDays ->
            val slices = AnalyticsResolution.slicesPerPeriod(rangeDays)
            assertEquals(0, rangeDays % slices, "range $rangeDays split into $slices slices")
        }
    }
}
