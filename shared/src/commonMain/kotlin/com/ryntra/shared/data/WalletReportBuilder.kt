package com.ryntra.shared.data

import com.ryntra.shared.model.TaxFormStatus
import com.ryntra.shared.model.WalletReport
import com.ryntra.shared.model.WalletSchedule
import com.ryntra.shared.model.YearMonth
import com.ryntra.shared.model.taxFormThresholdFor
import com.ryntra.shared.network.PayoutBalanceResponse
import com.ryntra.shared.network.PayoutHistoryResponse

/** Folds the three payout responses into the report the wallet screens render. */
internal fun buildWalletReport(
    balance: PayoutBalanceResponse,
    history: PayoutHistoryResponse,
    taxFormThresholds: Map<Int, Double>,
    nowEpochMillis: Long,
): WalletReport {
    val schedule = WalletSchedule.split(balance.dates, nowEpochMillis)
    val year = YearMonth.ofEpochMillis(nowEpochMillis).year
    return WalletReport(
        available = balance.available ?: 0.0,
        pending = balance.pending ?: 0.0,
        withdrawnLifetime = balance.withdrawnLifetime ?: 0.0,
        withdrawnThisYear = balance.withdrawnThisYear ?: 0.0,
        estimated = schedule.estimated,
        processing = schedule.processing,
        taxFormStatus = TaxFormStatus.fromApi(balance.formCompletionStatus),
        taxFormThreshold = taxFormThresholdFor(year, taxFormThresholds),
        transactions = history.transactions,
        balanceStatus = balance.status,
        historyStatus = history.status,
    )
}
