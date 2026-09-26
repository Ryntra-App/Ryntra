package com.ryntra.mobile.ui.dashboard.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.ryntraCard
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.PayoutStatus
import com.ryntra.shared.model.WalletHistory
import com.ryntra.shared.model.WalletTransaction
import com.ryntra.shared.model.WalletTransactionKind

@Composable
internal fun WalletTransactionRow(
    transaction: WalletTransaction,
    isCancelling: Boolean,
    isAnyActionRunning: Boolean,
    onCancel: (WalletTransaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RyntraDesign.colors
    val isIncome = transaction.kind == WalletTransactionKind.Income
    val isFailed = transaction.status == PayoutStatus.Failed ||
        transaction.status == PayoutStatus.Cancelled ||
        transaction.status == PayoutStatus.Cancelling
    var isConfirmingCancel by rememberSaveable(transaction.id) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .ryntraCard(RyntraDesign.contentShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        TransactionBadge(
            icon = if (isIncome) Lucide.ArrowDownLeft else Lucide.ArrowUpRight,
            isIncome = isIncome,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = transaction.title(),
                style = MaterialTheme.typography.titleSmall,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = transactionDetail(transaction),
                style = MaterialTheme.typography.bodySmall,
                color = if (isFailed) colors.destructive else colors.labelSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            transaction.methodAddress?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isIncome) "+" else "−") + formatWalletMoney(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) colors.positive else colors.labelPrimary,
                maxLines = 1,
            )
            if (transaction.canCancel) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(20.dp),
                    )
                } else {
                    TextButton(
                        onClick = { isConfirmingCancel = true },
                        enabled = !isAnyActionRunning,
                    ) {
                        Text(stringResource(R.string.wallet_cancel_withdrawal), color = colors.destructive)
                    }
                }
            }
        }
    }

    if (isConfirmingCancel) {
        AlertDialog(
            onDismissRequest = { isConfirmingCancel = false },
            title = { Text(stringResource(R.string.wallet_cancel_withdrawal_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.wallet_cancel_withdrawal_message,
                        formatWalletMoney(transaction.amount),
                        transaction.title(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmingCancel = false
                        onCancel(transaction)
                    },
                ) { Text(stringResource(R.string.wallet_cancel_withdrawal), color = colors.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingCancel = false }) { Text(stringResource(R.string.wallet_keep_withdrawal)) }
            },
        )
    }
}

@Composable
private fun transactionDetail(transaction: WalletTransaction): String {
    val parts = buildList {
        transaction.status?.let { add(it.label()) }
        add(formatWalletDate(transaction.created))
        transaction.fee?.takeIf { it > 0.0 }?.let { add(stringResource(R.string.wallet_fee, formatWalletMoney(it))) }
    }
    return parts.joinToString(" • ")
}

@Composable
private fun TransactionBadge(icon: ImageVector, isIncome: Boolean) {
    val colors = RyntraDesign.colors
    val tint = if (isIncome) colors.positive else colors.labelPrimary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Year chips plus the totals the site shows above its history: received, withdrawn, count. */
@Composable
internal fun WalletHistoryHeader(
    history: WalletHistory,
    onSelectYear: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (history.years.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = history.year == null,
                    onClick = { onSelectYear(null) },
                    label = { Text(stringResource(R.string.wallet_all_years)) },
                )
                history.years.forEach { year ->
                    FilterChip(
                        selected = history.year == year,
                        onClick = { onSelectYear(year) },
                        label = { Text(year.toString()) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HistoryTotal(stringResource(R.string.wallet_received), formatWalletMoney(history.received), Modifier.weight(1f))
            HistoryTotal(stringResource(R.string.wallet_withdrawn), formatWalletMoney(history.withdrawn), Modifier.weight(1f))
            HistoryTotal(
                label = stringResource(R.string.wallet_transactions),
                value = pluralStringResource(R.plurals.wallet_transaction_count, history.filtered.size, history.filtered.size),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HistoryTotal(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .ryntraCard(RyntraDesign.contentShape)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = RyntraDesign.colors.labelSecondary, maxLines = 1)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = RyntraDesign.colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
