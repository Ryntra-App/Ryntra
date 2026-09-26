package com.ryntra.mobile.ui.dashboard.wallet

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraContentLoading
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.ryntraCard
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.AffiliateReport
import com.ryntra.shared.model.WalletHistory
import com.ryntra.shared.model.WalletReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The creator wallet, laid out like modrinth.com/dashboard/revenue and its transfers page:
 * balance by payout date, tax status, withdrawal, yearly totals and the full history.
 *
 * Withdrawing itself stays on the site: it runs through Modrinth's tax form and payout-method
 * verification, which have no public API.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun WalletScreen(
    report: WalletReport?,
    isLoading: Boolean,
    errorMessage: String?,
    cancellingPayoutId: String?,
    actionErrorMessage: String?,
    onRefresh: () -> Unit,
    onCancelPayout: (String) -> Unit,
    affiliate: WalletAffiliateState = WalletAffiliateState(),
) {
    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { uriHandler.openUri(it) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val history = remember(report?.transactions, selectedYear) {
        WalletHistory(report?.transactions.orEmpty(), selectedYear)
    }
    val exportCsv = rememberCsvExport(history)
    // The pull indicator answers the gesture only. Refreshes the app starts on its own,
    // like opening the screen, update the content in place instead of dropping it down.
    var isPullRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) { if (!isLoading) isPullRefresh = false }
    val pullState = rememberPullToRefreshState()
    val isPullIndicatorShown = isPullRefresh && isLoading

    PullToRefreshBox(
        isRefreshing = isPullIndicatorShown,
        onRefresh = {
            isPullRefresh = true
            onRefresh()
        },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (RyntraDesign.isPlatformNative) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = isPullIndicatorShown,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            } else {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isPullIndicatorShown,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = RyntraDesign.bottomContentPadding),
        ) {
            when {
                report?.isAvailable == true -> walletContent(
                    report = report,
                    history = history,
                    cancellingPayoutId = cancellingPayoutId,
                    actionErrorMessage = actionErrorMessage,
                    onSelectYear = { selectedYear = it },
                    onExportCsv = exportCsv,
                    onCancelPayout = onCancelPayout,
                    onOpenUrl = openUrl,
                    affiliate = affiliate,
                )
                isLoading -> item(key = "wallet-loading") {
                    RyntraContentLoading(label = stringResource(R.string.wallet_loading))
                }
                else -> item(key = "wallet-unavailable") {
                    RyntraEmptyState(
                        title = stringResource(R.string.wallet_unavailable_title),
                        message = walletUnavailableMessage(report, errorMessage),
                        icon = Lucide.CircleAlert,
                        actionLabel = stringResource(R.string.common_retry),
                        onAction = onRefresh,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.walletContent(
    report: WalletReport,
    history: WalletHistory,
    cancellingPayoutId: String?,
    actionErrorMessage: String?,
    onSelectYear: (Int?) -> Unit,
    onExportCsv: () -> Unit,
    onCancelPayout: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    affiliate: WalletAffiliateState,
) {
    item(key = "wallet-notices") { WalletComplianceNotices(report, onOpenUrl) }
    if (report.isBalanceAvailable) {
        item(key = "wallet-balance") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .ryntraCard(RyntraDesign.contentShape)
                    .padding(20.dp),
            ) {
                WalletBalanceBreakdown(report, showDetails = true)
                TextButton(onClick = { onOpenUrl(MODRINTH_REVENUE_INFO_URL) }) {
                    Text(stringResource(R.string.wallet_how_revenue_works))
                }
            }
        }
        item(key = "wallet-withdraw") {
            WithdrawSection(report, onOpenUrl)
        }
        item(key = "wallet-totals") {
            WithdrawnTotals(report)
        }
    }
    if (affiliate.isShown) {
        item(key = "wallet-affiliates") {
            WalletAffiliateSection(
                report = affiliate.report,
                isLoading = affiliate.isLoading,
                errorMessage = affiliate.errorMessage,
                onOpenUrl = onOpenUrl,
            )
        }
    }

    item(key = "wallet-history-title") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
        ) {
            RyntraSectionLabel(stringResource(R.string.wallet_transactions), modifier = Modifier.weight(1f))
            if (history.filtered.isNotEmpty()) {
                TextButton(onClick = onExportCsv) {
                    Text(stringResource(R.string.wallet_export_csv))
                }
            }
        }
    }
    actionErrorMessage?.let { message ->
        item(key = "wallet-action-error") {
            Text(
                text = message,
                color = RyntraDesign.colors.destructive,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    when {
        !report.isHistoryAvailable -> item(key = "wallet-history-unavailable") {
            Text(
                text = stringResource(R.string.wallet_history_unavailable),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        !report.hasTransactions -> item(key = "wallet-history-empty") {
            RyntraEmptyState(
                title = stringResource(R.string.wallet_no_transactions),
                message = stringResource(R.string.wallet_no_transactions_hint),
                icon = Lucide.ReceiptText,
            )
        }
        else -> {
            item(key = "wallet-history-header") {
                WalletHistoryHeader(history, onSelectYear, Modifier.padding(bottom = 8.dp))
            }
            history.months.forEach { month ->
                item(key = "wallet-month-${month.yearMonth}") {
                    Text(
                        text = walletMonthLabel(month.year, month.month),
                        style = MaterialTheme.typography.titleSmall,
                        color = RyntraDesign.colors.labelSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(
                    items = month.transactions,
                    key = { "wallet-row-${it.id ?: it.created}-${it.kind}-${it.amount}" },
                    contentType = { "wallet-transaction" },
                ) { transaction ->
                    WalletTransactionRow(
                        transaction = transaction,
                        isCancelling = transaction.id != null && transaction.id == cancellingPayoutId,
                        isAnyActionRunning = cancellingPayoutId != null,
                        onCancel = { it.id?.let(onCancelPayout) },
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WithdrawSection(report: WalletReport, onOpenUrl: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .ryntraCard(RyntraDesign.contentShape)
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.wallet_withdraw),
            style = MaterialTheme.typography.titleMedium,
            color = RyntraDesign.colors.labelPrimary,
        )
        Text(
            text = stringResource(
                if (report.isWithdrawalLocked) R.string.wallet_withdraw_locked else R.string.wallet_withdraw_description,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = RyntraDesign.colors.labelSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        RyntraPrimaryButton(
            text = stringResource(R.string.wallet_withdraw_on_modrinth, formatWalletMoney(report.available)),
            icon = Lucide.ArrowUpRight,
            onClick = { onOpenUrl(MODRINTH_REVENUE_URL) },
            enabled = !report.isWithdrawalLocked && report.available > 0.0,
        )
    }
}

@Composable
private fun WithdrawnTotals(report: WalletReport) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        TotalTile(
            label = stringResource(R.string.wallet_withdrawn_this_year),
            value = formatWalletMoney(report.withdrawnThisYear),
            footnote = report.taxFormThreshold?.let {
                stringResource(R.string.wallet_tax_threshold_hint, formatWalletMoney(it))
            },
            modifier = Modifier.weight(1f),
        )
        TotalTile(
            label = stringResource(R.string.wallet_withdrawn_lifetime),
            value = formatWalletMoney(report.withdrawnLifetime),
            footnote = null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TotalTile(label: String, value: String, footnote: String?, modifier: Modifier) {
    Column(
        modifier = modifier
            .ryntraCard(RyntraDesign.contentShape)
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = RyntraDesign.colors.labelSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RyntraDesign.colors.labelPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
        footnote?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = RyntraDesign.colors.labelSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Saves the visible history as the same CSV the site's "Download as CSV" produces. */
@Composable
private fun rememberCsvExport(history: WalletHistory): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = writeCsv(context, uri, history.toCsv())
            val message = if (saved) R.string.wallet_export_saved else R.string.wallet_export_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val fileName = "modrinth-transactions${history.year?.let { "-$it" }.orEmpty()}.csv"
    return { launcher.launch(fileName) }
}

/** False when the chosen location cannot be written, which the caller reports to the user. */
private suspend fun writeCsv(context: Context, uri: Uri, csv: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
        stream.use { it.write(csv.toByteArray()) }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** The affiliate part of the wallet; only affiliate accounts ever load a report. */
data class WalletAffiliateState(
    val report: AffiliateReport? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isShown: Boolean get() = report != null || isLoading || errorMessage != null
}
