package com.ryntra.mobile.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ryntra.mobile.security.SecureTokenStore
import com.ryntra.shared.model.WidgetSnapshot
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.network.WidgetSnapshotClient
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Entry points the app and the widget receivers use to keep the widgets current. */
internal object RyntraWidgets {
    private const val REFRESH_WORK = "ryntra-widget-refresh"
    private const val REFRESH_NOW_WORK = "ryntra-widget-refresh-now"
    private const val SERIES_MAX_AGE_MILLIS = 3 * 60 * 60 * 1000L
    private const val SNAPSHOT_MAX_AGE_MILLIS = 60 * 60 * 1000L
    private const val MIN_REFRESH_GAP_MILLIS = 10 * 60 * 1000L

    /**
     * Saves what the app just loaded and redraws every placed widget from it. The app does not
     * load the daily downloads series, so the one on disk is kept, and refreshed in the
     * background once it is older than [SERIES_MAX_AGE_MILLIS]. The refresh itself passes
     * [refreshStaleSeries] false, or a series Modrinth keeps failing to return would
     * re-queue the refresh forever.
     */
    suspend fun publish(context: Context, snapshot: WidgetSnapshot, refreshStaleSeries: Boolean = true) {
        val store = WidgetSnapshotStore(context)
        val merged = snapshot.withSeriesFrom(store.read())
        store.write(merged)
        redraw(context)
        val seriesAge = merged.dailyDownloadsUpdatedAtEpochMillis?.let { System.currentTimeMillis() - it }
        val isSeriesStale = seriesAge == null || seriesAge > SERIES_MAX_AGE_MILLIS
        if (refreshStaleSeries && isSeriesStale && hasPlacedWidgets(context)) {
            refreshNow(context)
        }
    }

    /**
     * Called whenever a widget is drawn: makes sure the hourly refresh exists, and fetches
     * straight away when there is nothing to show or it is out of date. This is what lets a
     * widget fill itself after a reboot, an app update, or without the app ever being opened.
     */
    fun ensureFresh(context: Context, snapshot: WidgetSnapshot?) {
        scheduleRefresh(context)
        val age = snapshot?.let { System.currentTimeMillis() - it.updatedAtEpochMillis }
        val seriesAge = snapshot?.dailyDownloadsUpdatedAtEpochMillis?.let { System.currentTimeMillis() - it }
        val isStale = age == null || age > SNAPSHOT_MAX_AGE_MILLIS || seriesAge == null || seriesAge > SERIES_MAX_AGE_MILLIS
        if (!isStale || SecureTokenStore(context).read() == null) return
        // Drawing a widget can itself follow a refresh; without a gap, data Modrinth keeps
        // failing to return would chain refreshes back to back.
        val store = WidgetSnapshotStore(context)
        val sinceLastAttempt = System.currentTimeMillis() - store.lastRefreshAttemptEpochMillis()
        if (sinceLastAttempt < MIN_REFRESH_GAP_MILLIS) return
        store.recordRefreshAttempt()
        refreshNow(context)
    }

    suspend fun clear(context: Context) {
        WidgetSnapshotStore(context).clear()
        redraw(context)
    }

    suspend fun redraw(context: Context) {
        WalletWidget().updateAll(context)
        StatsWidget().updateAll(context)
        DownloadsWidget().updateAll(context)
    }

    /**
     * Hourly while at least one widget is placed. Android caps how often a widget may ask to
     * be updated, and Modrinth's payout data changes daily at most.
     */
    fun scheduleRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One refresh now, for a chart that has no data yet; repeated taps collapse into one. */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_NOW_WORK, ExistingWorkPolicy.KEEP, request)
    }

    suspend fun cancelRefreshIfUnused(context: Context) {
        if (!hasPlacedWidgets(context)) WorkManager.getInstance(context).cancelUniqueWork(REFRESH_WORK)
    }

    private suspend fun hasPlacedWidgets(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return listOf(WalletWidget::class.java, StatsWidget::class.java, DownloadsWidget::class.java)
            .any { manager.getGlanceIds(it).isNotEmpty() }
    }
}

class WidgetRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val token = SecureTokenStore(applicationContext).read()
        if (token == null) {
            RyntraWidgets.clear(applicationContext)
            return Result.success()
        }
        val client = WidgetSnapshotClient()
        return try {
            val previous = WidgetSnapshotStore(applicationContext).read()
            RyntraWidgets.publish(applicationContext, client.load(token, previous), refreshStaleSeries = false)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ApiException) {
            // A revoked token will not start working on a retry; the app asks to sign in again.
            if (error.statusCode == 401 || error.statusCode == 403) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            client.close()
        }
    }
}
