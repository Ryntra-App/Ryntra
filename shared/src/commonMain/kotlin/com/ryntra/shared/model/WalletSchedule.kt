package com.ryntra.shared.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Splits the dated revenue from `GET /v3/payout/balance` the way Modrinth's revenue page does.
 *
 * Revenue earned in month X becomes available 60 days after the end of X. Dates still in the
 * future whose source month is the current one are "processing" — the month is still open, so
 * the figure keeps growing. Every other future date is a finalised "estimated" payout.
 */
object WalletSchedule {
    private const val AVAILABILITY_DELAY_DAYS = 60L
    private const val MILLIS_PER_DAY = 86_400_000L

    data class Split(
        val estimated: List<ScheduledPayout>,
        val processing: ScheduledPayout?,
    )

    @OptIn(ExperimentalTime::class)
    fun split(dates: Map<String, Double>, nowEpochMillis: Long): Split {
        val upcoming = dates.mapNotNull { (date, amount) ->
            val millis = runCatching { Instant.parse(date).toEpochMilliseconds() }.getOrNull()
            millis?.takeIf { it > nowEpochMillis }?.let { UpcomingDate(date, it, amount) }
        }.sortedBy(UpcomingDate::epochMillis)

        val currentMonth = YearMonth.ofEpochMillis(nowEpochMillis)
        val processing = upcoming.firstOrNull { entry ->
            YearMonth.ofEpochMillis(entry.epochMillis - AVAILABILITY_DELAY_DAYS * MILLIS_PER_DAY) == currentMonth
        }
        return Split(
            estimated = upcoming.filter { it !== processing }.map(UpcomingDate::toPayout),
            processing = processing?.toPayout(),
        )
    }

    private class UpcomingDate(val date: String, val epochMillis: Long, val amount: Double) {
        fun toPayout() = ScheduledPayout(date, amount)
    }
}

/** A UTC calendar month, enough to compare months without a date-time dependency. */
internal data class YearMonth(val year: Int, val month: Int) {
    companion object {
        /** Civil-from-days (H. Hinnant), valid for the whole proleptic Gregorian calendar. */
        fun ofEpochMillis(epochMillis: Long): YearMonth {
            val days = epochMillis.floorDiv(86_400_000L)
            val shifted = days + 719_468
            val era = shifted.floorDiv(146_097L)
            val dayOfEra = shifted - era * 146_097
            val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val monthIndex = (5 * dayOfYear + 2) / 153
            val month = (if (monthIndex < 10) monthIndex + 3 else monthIndex - 9).toInt()
            val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
            return YearMonth(year, month)
        }
    }
}
