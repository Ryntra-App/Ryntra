package com.rinthy.mobile.ui.dashboard.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Wallet
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyProgressIndicator
import com.rinthy.mobile.ui.components.formatCurrency
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.PayoutTransaction
import com.rinthy.shared.model.WalletReport
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val payoutDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private data class WalletStat(
    @StringRes val labelRes: Int,
    val amount: Double,
)

@Composable
internal fun WalletSummary(
    report: WalletReport?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenRevenue: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RinthyDesign.colors.surface, shape)
            .border(0.75.dp, RinthyDesign.colors.separator, shape)
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .background(RinthyDesign.colors.accent.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
            ) {
                RinthyIcon(Lucide.Wallet, null, RinthyDesign.colors.accent, Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                Text(stringResource(R.string.wallet_modrinth), fontWeight = FontWeight.SemiBold)
                Text(
                    text = walletSubtitle(report, isLoading),
                    color = RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (isLoading) {
                RinthyProgressIndicator(RinthyDesign.colors.accent, Modifier.size(16.dp))
            }
        }

        if (report?.isAvailable == true) {
            WalletAmounts(report)
        } else if (!isLoading) {
            Text(
                text = walletUnavailableMessage(report, errorMessage),
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 15.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
                .background(RinthyDesign.colors.surfaceRaised, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onOpenRevenue)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.wallet_open_revenue),
                color = RinthyDesign.colors.accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            RinthyIcon(
                icon = Lucide.ExternalLink,
                contentDescription = null,
                tint = RinthyDesign.colors.accent,
                modifier = Modifier.padding(start = 7.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun WalletAmounts(report: WalletReport) {
    val primaryBalance = report.balance ?: report.available
    if (primaryBalance != null) {
        Text(
            text = stringResource(R.string.wallet_current_balance),
            color = RinthyDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = formatCurrency(primaryBalance, report.currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }

    val stats = buildList {
        if (report.balance != null) report.available?.let { add(WalletStat(R.string.wallet_available_now, it)) }
        report.pending?.let { add(WalletStat(R.string.wallet_pending, it)) }
        report.withdrawnLifetime?.let { add(WalletStat(R.string.wallet_paid_out, it)) }
        report.lifetimeEarnings?.let { add(WalletStat(R.string.wallet_lifetime_earnings, it)) }
    }
    stats.chunked(2).forEachIndexed { index, row ->
        WalletValuePair(
            first = row[0],
            second = row.getOrNull(1),
            currency = report.currency,
            modifier = Modifier.padding(top = if (index == 0) 18.dp else 14.dp),
        )
    }

    if (primaryBalance == null && stats.isEmpty()) {
        Text(
            text = stringResource(R.string.wallet_no_balance),
            color = RinthyDesign.colors.labelSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 15.dp),
        )
    }
}

@Composable
private fun WalletValuePair(
    first: WalletStat,
    second: WalletStat?,
    currency: String,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier.fillMaxWidth()) {
        WalletValue(stringResource(first.labelRes), formatCurrency(first.amount, currency), Modifier.weight(1f))
        if (second != null) {
            WalletValue(stringResource(second.labelRes), formatCurrency(second.amount, currency), Modifier.weight(1f))
        } else {
            Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun WalletValue(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = RinthyDesign.colors.labelSecondary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
internal fun PayoutTransactionRow(transaction: PayoutTransaction, currency: String) {
    val statusColor = payoutStatusColor(transaction.status)
    val missingDate = stringResource(R.string.wallet_date_missing)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
    ) {
        RinthyIcon(Lucide.Clock, null, statusColor, Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(
                text = transaction.status.ifBlank { stringResource(R.string.wallet_payout) }.replaceFirstChar(Char::uppercase),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatPayoutDate(transaction.created, missingDate),
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = formatCurrency(transaction.amount, currency),
            color = statusColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun payoutStatusColor(status: String): Color = when (status.lowercase()) {
    "paid", "sent", "completed", "success" -> RinthyDesign.colors.positive
    "pending", "processing" -> RinthyDesign.colors.warning
    "failed", "cancelled", "canceled" -> RinthyDesign.colors.destructive
    else -> RinthyDesign.colors.labelSecondary
}

@Composable
private fun walletSubtitle(report: WalletReport?, isLoading: Boolean): String = when {
    isLoading && report == null -> stringResource(R.string.wallet_loading)
    report?.isAvailable == true -> stringResource(R.string.wallet_activity)
    else -> stringResource(R.string.wallet_account)
}

private fun formatPayoutDate(value: String, missingDate: String): String = runCatching {
    LocalDate.parse(value.take(10)).format(payoutDateFormatter)
}.getOrDefault(value.take(10).ifBlank { missingDate })

@Composable
private fun walletUnavailableMessage(report: WalletReport?, errorMessage: String?): String = when {
    errorMessage != null -> errorMessage
    report == null -> stringResource(R.string.wallet_data_failed)
    report.balanceStatus == 0 && report.historyStatus == 0 -> stringResource(R.string.wallet_unreachable)
    report.balanceStatus in setOf(401, 403) || report.historyStatus in setOf(401, 403) ->
        stringResource(R.string.wallet_reconnect)
    else -> stringResource(R.string.wallet_no_details)
}
