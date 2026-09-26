package com.ryntra.mobile.ui.dashboard.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldAlert
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.WalletReport

/**
 * The two tax-compliance states Modrinth raises as site-wide banners: a TIN that failed the
 * IRS match, which locks withdrawals, and a form that became due past the yearly threshold.
 */
@Composable
internal fun WalletComplianceNotices(
    report: WalletReport,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RyntraDesign.colors
    Column(modifier) {
        if (report.isWithdrawalLocked) {
            WalletNotice(
                title = stringResource(R.string.wallet_tin_mismatch_title),
                message = stringResource(R.string.wallet_tin_mismatch_message),
                actionLabel = stringResource(R.string.wallet_contact_support),
                tone = colors.destructive,
                isLocked = true,
                onAction = { onOpenUrl(MODRINTH_SUPPORT_URL) },
            )
        } else if (report.isTaxFormRequired) {
            WalletNotice(
                title = stringResource(R.string.wallet_tax_form_title),
                message = stringResource(
                    R.string.wallet_tax_form_message,
                    formatWalletMoney(report.taxFormThreshold ?: 0.0),
                ),
                actionLabel = stringResource(R.string.wallet_tax_form_action),
                tone = colors.warning,
                isLocked = false,
                onAction = { onOpenUrl(MODRINTH_REVENUE_URL) },
            )
        }
    }
}

@Composable
private fun WalletNotice(
    title: String,
    message: String,
    actionLabel: String,
    tone: Color,
    isLocked: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RyntraDesign.contentShape)
            .background(tone.copy(alpha = 0.12f))
            .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
    ) {
        Icon(
            imageVector = if (isLocked) Lucide.ShieldAlert else Lucide.FileText,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = RyntraDesign.colors.labelPrimary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = RyntraDesign.colors.labelSecondary,
                modifier = Modifier.padding(top = 4.dp, end = 8.dp),
            )
            TextButton(onClick = onAction) { Text(actionLabel, color = tone) }
        }
    }
}
