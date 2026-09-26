package com.ryntra.mobile.ui.dashboard.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraContentLoading
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.ryntraCard
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.WalletReport

/**
 * The wallet on the analytics tab: the full balance breakdown, so the payout dates are one
 * glance away, and the way into the wallet screen for history and withdrawals.
 */
@Composable
internal fun WalletSummaryCard(
    report: WalletReport?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenWallet: () -> Unit,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .ryntraCard(RyntraDesign.contentShape)
            .padding(20.dp),
    ) {
        when {
            report?.isBalanceAvailable == true -> {
                WalletComplianceNotices(report, onOpenUrl)
                WalletBalanceBreakdown(report, showDetails = false)
                RyntraPrimaryButton(
                    text = stringResource(R.string.wallet_open),
                    icon = Lucide.ChevronRight,
                    onClick = onOpenWallet,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            isLoading -> RyntraContentLoading(label = stringResource(R.string.wallet_loading))
            else -> {
                Text(
                    text = walletUnavailableMessage(report, errorMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RyntraDesign.colors.labelSecondary,
                )
                RyntraSecondaryButton(
                    text = stringResource(R.string.common_retry),
                    icon = Lucide.RefreshCw,
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
