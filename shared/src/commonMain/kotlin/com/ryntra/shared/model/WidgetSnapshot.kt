package com.ryntra.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A dated payout as the widgets list it. */
@Serializable
data class WidgetPayout(
    /** ISO timestamp; null for this month's revenue, which has no firm date yet. */
    val date: String? = null,
    val amount: Double,
)

@Serializable
data class WidgetProject(
    val title: String,
    val downloads: Long,
)

/** A chart window over [WidgetSnapshot.dailyDownloads]. */
data class DownloadWindow(
    val days: Int,
    /** Oldest first. */
    val values: List<Long>,
    val total: Long,
    /** The same number of days just before; null when the series does not reach that far. */
    val previousTotal: Long?,
) {
    /** Percent change against [previousTotal], or null when there is nothing to compare. */
    val changePercent: Double?
        get() {
            val previous = previousTotal ?: return null
            if (previous == 0L) return null
            return (total - previous) * 100.0 / previous
        }
}

/**
 * What the home-screen widgets show, stored as one small JSON blob so a widget renders
 * instantly from disk and never waits on the network.
 */
@Serializable
data class WidgetSnapshot(
    val username: String,
    val downloads: Long,
    val followers: Long,
    val projectCount: Int,
    /** Busiest first, at most [TOP_PROJECTS]. */
    val topProjects: List<WidgetProject> = emptyList(),
    /** Null when the payout routes did not answer; the widget then hides the money. */
    val balance: Double? = null,
    val available: Double? = null,
    /** Finalised dates soonest first, then this month's processing revenue. */
    val upcomingPayouts: List<WidgetPayout> = emptyList(),
    /** Downloads per UTC day for the last [SERIES_DAYS] days, oldest first; empty until loaded. */
    val dailyDownloads: List<Long> = emptyList(),
    val dailyDownloadsUpdatedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
) {
    val nextPayout: WidgetPayout? get() = upcomingPayouts.firstOrNull()

    fun downloadWindow(days: Int): DownloadWindow? {
        if (dailyDownloads.size < days || days <= 0) return null
        val values = dailyDownloads.takeLast(days)
        val previous = dailyDownloads.dropLast(days).takeLast(days).takeIf { it.size == days }
        return DownloadWindow(days, values, values.sum(), previous?.sum())
    }

    /**
     * The app knows the account, projects and wallet but not the daily series, which only the
     * background refresh loads; a snapshot from the app keeps the series already on disk.
     */
    fun withSeriesFrom(previous: WidgetSnapshot?): WidgetSnapshot =
        if (dailyDownloads.isNotEmpty() || previous == null || previous.username != username) {
            this
        } else {
            copy(
                dailyDownloads = previous.dailyDownloads,
                dailyDownloadsUpdatedAtEpochMillis = previous.dailyDownloadsUpdatedAtEpochMillis,
            )
        }

    fun toJson(): String = snapshotJson.encodeToString(serializer(), this)

    companion object {
        const val SERIES_DAYS = 90
        const val TOP_PROJECTS = 3

        /** Null for a missing or unreadable blob, e.g. one written by an older build. */
        fun fromJson(json: String?): WidgetSnapshot? =
            json?.let { runCatching { snapshotJson.decodeFromString(serializer(), it) }.getOrNull() }

        fun from(
            dashboard: Dashboard,
            wallet: WalletReport?,
            nowEpochMillis: Long,
            dailyDownloads: List<Long> = emptyList(),
        ): WidgetSnapshot {
            val loadedWallet = wallet?.takeIf { it.isBalanceAvailable }
            val payouts = loadedWallet?.let { report ->
                report.estimated.map { WidgetPayout(it.date, it.amount) } +
                    listOfNotNull(report.processing?.let { WidgetPayout(date = null, amount = it.amount) })
            }.orEmpty()
            return WidgetSnapshot(
                username = dashboard.account.username,
                downloads = dashboard.projects.sumOf(Project::downloads),
                followers = dashboard.projects.sumOf(Project::followers),
                projectCount = dashboard.projects.size,
                topProjects = dashboard.projects
                    .sortedByDescending(Project::downloads)
                    .take(TOP_PROJECTS)
                    .map { WidgetProject(it.title, it.downloads) },
                balance = loadedWallet?.balance,
                available = loadedWallet?.available,
                upcomingPayouts = payouts,
                dailyDownloads = dailyDownloads,
                dailyDownloadsUpdatedAtEpochMillis = nowEpochMillis.takeIf { dailyDownloads.isNotEmpty() },
                updatedAtEpochMillis = nowEpochMillis,
            )
        }
    }
}

private val snapshotJson = Json { ignoreUnknownKeys = true }
