package com.ryntra.mobile.ui.dashboard.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.formatCurrency
import com.ryntra.shared.model.PayoutStatus
import com.ryntra.shared.model.WALLET_CURRENCY
import com.ryntra.shared.model.WalletReport
import com.ryntra.shared.model.WalletTransaction
import com.ryntra.shared.model.WalletTransactionKind
import com.ryntra.shared.model.payoutMethodName
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

internal const val MODRINTH_REVENUE_URL = "https://modrinth.com/dashboard/revenue"
internal const val MODRINTH_REVENUE_INFO_URL = "https://modrinth.com/legal/cmp-info#pending"
internal const val MODRINTH_SUPPORT_URL = "https://support.modrinth.com"

private val walletDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

internal fun formatWalletMoney(amount: Double): String = formatCurrency(amount, WALLET_CURRENCY)

/**
 * Availability dates are UTC midnights; formatting them in the device zone would show the
 * day before for everyone west of Greenwich, which is not the date Modrinth promises.
 */
internal fun formatWalletDate(isoTimestamp: String): String = runCatching {
    Instant.parse(isoTimestamp).atOffset(ZoneOffset.UTC).toLocalDate().format(walletDateFormatter)
}.recoverCatching {
    LocalDate.parse(isoTimestamp.take(10)).format(walletDateFormatter)
}.getOrDefault(isoTimestamp.take(10))

/** "This month", "Last month", otherwise "September 2026". */
@Composable
internal fun walletMonthLabel(year: Int, month: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val period = runCatching { YearMonth.of(year, month) }.getOrNull() ?: return "$year"
    val now = YearMonth.now(ZoneOffset.UTC)
    return when (period) {
        now -> stringResource(R.string.wallet_this_month)
        now.minusMonths(1) -> stringResource(R.string.wallet_last_month_period)
        else -> {
            val monthName = period.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            "${monthName.replaceFirstChar { it.titlecase(locale) }} $year"
        }
    }
}

@Composable
internal fun WalletTransaction.title(): String = when (kind) {
    WalletTransactionKind.Income -> when (payoutSource) {
        "affiliates", "affilites" -> stringResource(R.string.wallet_source_affiliates)
        else -> stringResource(R.string.wallet_source_creator_rewards)
    }
    WalletTransactionKind.Withdrawal -> payoutMethodName(methodType)
}

@Composable
internal fun PayoutStatus.label(): String = stringResource(
    when (this) {
        PayoutStatus.Success -> R.string.wallet_status_success
        PayoutStatus.InTransit -> R.string.wallet_status_in_transit
        PayoutStatus.Cancelling -> R.string.wallet_status_cancelling
        PayoutStatus.Cancelled -> R.string.wallet_status_cancelled
        PayoutStatus.Failed -> R.string.wallet_status_failed
        PayoutStatus.Unknown -> R.string.wallet_status_unknown
    },
)

/** Why the wallet has nothing to show, from the HTTP statuses the payout routes returned. */
@Composable
internal fun walletUnavailableMessage(report: WalletReport?, errorMessage: String?): String = when {
    errorMessage != null -> errorMessage
    report == null -> stringResource(R.string.wallet_data_failed)
    report.balanceStatus == 0 && report.historyStatus == 0 -> stringResource(R.string.wallet_unreachable)
    report.balanceStatus in setOf(401, 403) || report.historyStatus in setOf(401, 403) ->
        stringResource(R.string.wallet_reconnect)
    else -> stringResource(R.string.wallet_no_details)
}
