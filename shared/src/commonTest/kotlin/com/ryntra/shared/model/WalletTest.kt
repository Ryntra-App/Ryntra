package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalletTest {
    // 2026-09-26T12:00:00Z
    private val now = 1_790_424_000_000L

    @Test
    fun currentMonthsRevenueIsProcessingAndFinalisedDatesAreEstimates() {
        val split = WalletSchedule.split(
            dates = mapOf(
                "2026-11-29T00:00:00Z" to 5.78,
                "2026-09-29T00:00:00Z" to 2.87,
                "2026-10-30T00:00:00Z" to 5.33,
                "2026-08-30T00:00:00Z" to 4.0,
            ),
            nowEpochMillis = now,
        )

        assertEquals(
            listOf(ScheduledPayout("2026-09-29T00:00:00Z", 2.87), ScheduledPayout("2026-10-30T00:00:00Z", 5.33)),
            split.estimated,
        )
        assertEquals(ScheduledPayout("2026-11-29T00:00:00Z", 5.78), split.processing)
    }

    @Test
    fun noProcessingEntryWhenNothingWasEarnedThisMonth() {
        val split = WalletSchedule.split(mapOf("2026-10-30T00:00:00Z" to 1.0), now)

        assertNull(split.processing)
        assertEquals(1, split.estimated.size)
    }

    @Test
    fun yearMonthHandlesLeapDaysAndYearBoundaries() {
        assertEquals(YearMonth(2024, 2), YearMonth.ofEpochMillis(1_709_164_800_000L)) // 2024-02-29
        assertEquals(YearMonth(2025, 12), YearMonth.ofEpochMillis(1_767_225_599_000L)) // 2025-12-31T23:59:59Z
        assertEquals(YearMonth(2026, 1), YearMonth.ofEpochMillis(1_767_225_600_000L))
    }

    @Test
    fun taxThresholdFallsBackToTheNearestKnownYear() {
        val thresholds = mapOf(2025 to 600.0, 2026 to 2000.0)

        assertEquals(600.0, taxFormThresholdFor(2024, thresholds))
        assertEquals(2000.0, taxFormThresholdFor(2026, thresholds))
        assertEquals(2000.0, taxFormThresholdFor(2030, thresholds))
        assertNull(taxFormThresholdFor(2026, emptyMap()))
    }

    @Test
    fun taxFormIsRequiredOnlyPastTheThresholdWithoutACompletedForm() {
        val report = WalletReport(withdrawnThisYear = 2_100.0, taxFormThreshold = 2_000.0)

        assertTrue(report.isTaxFormRequired)
        assertFalse(report.copy(taxFormStatus = TaxFormStatus.Complete).isTaxFormRequired)
        assertFalse(report.copy(withdrawnThisYear = 10.0).isTaxFormRequired)
        assertTrue(report.copy(taxFormStatus = TaxFormStatus.TinMismatch).isWithdrawalLocked)
    }

    @Test
    fun historyFiltersByYearAndTotalsEachDirection() {
        val history = WalletHistory(
            listOf(
                WalletTransaction(WalletTransactionKind.Withdrawal, "2026-08-02T10:00:00Z", 25.0, id = "a"),
                WalletTransaction(WalletTransactionKind.Income, "2026-07-30T00:00:00Z", 2.5),
                WalletTransaction(WalletTransactionKind.Income, "2025-12-30T00:00:00Z", 4.0),
            ),
        )

        assertEquals(listOf(2026, 2025), history.years)
        assertEquals(6.5, history.received)
        val thisYear = history.withYear(2026)
        assertEquals(2.5, thisYear.received)
        assertEquals(25.0, thisYear.withdrawn)
        assertEquals(listOf("2026-08", "2026-07"), thisYear.months.map(WalletMonth::yearMonth))
    }

    @Test
    fun csvQuotesFieldsThatNeedIt() {
        val csv = WalletHistory(
            listOf(
                WalletTransaction(
                    WalletTransactionKind.Withdrawal,
                    "2026-08-02T10:00:00Z",
                    25.0,
                    status = PayoutStatus.Success,
                    fee = 0.5,
                    methodType = "paypal",
                ),
            ),
        ).toCsv()

        assertEquals(
            "Date,Type,Source,Status,Amount,Fee\n2026-08-02 10:00:00,Withdrawal,PayPal,success,25.0,0.5\n",
            csv,
        )
    }
}
