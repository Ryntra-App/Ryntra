package com.ryntra.shared.network

import com.ryntra.shared.data.buildWalletReport
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.WidgetSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

/**
 * Loads what the home-screen widgets show, for a background refresh with no app running.
 * Only the account and its projects are required; a wallet or chart failure leaves those
 * parts of the widget on their previous data instead of failing the refresh.
 */
class WidgetSnapshotClient {
    private val api = ModrinthApi(createPlatformHttpClient())

    @OptIn(ExperimentalTime::class)
    suspend fun load(token: String, previous: WidgetSnapshot?): WidgetSnapshot = coroutineScope {
        val now = Clock.System.now()
        val account = api.getCurrentAccount(token)
        val projects = async { api.getProjects(account.id, token) }
        val balance = async { api.getPayoutBalance(token) }
        val series = async {
            optional {
                api.getDailyDownloads(
                    startTime = (now - WidgetSnapshot.SERIES_DAYS.days).toString(),
                    endTime = now.toString(),
                    days = WidgetSnapshot.SERIES_DAYS,
                    token = token,
                )
            }.orEmpty()
        }
        val wallet = buildWalletReport(
            balance = balance.await(),
            history = PayoutHistoryResponse(status = 0),
            taxFormThresholds = emptyMap(),
            nowEpochMillis = now.toEpochMilliseconds(),
        )
        WidgetSnapshot.from(
            dashboard = Dashboard(account = account, projects = projects.await(), organizations = emptyList()),
            wallet = wallet,
            nowEpochMillis = now.toEpochMilliseconds(),
            dailyDownloads = series.await(),
        ).withSeriesFrom(previous)
    }

    fun close() = api.close()

    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }
}
