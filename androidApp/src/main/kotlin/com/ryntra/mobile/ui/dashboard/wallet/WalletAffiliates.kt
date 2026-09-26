package com.ryntra.mobile.ui.dashboard.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraContentLoading
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.components.ryntraCard
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.AffiliateCodeStats
import com.ryntra.shared.model.AffiliateReport
import com.ryntra.shared.model.MODRINTH_AFFILIATE_LINKS_URL

/**
 * Affiliate link performance, for the accounts Modrinth invited to its affiliate program.
 * Links are listed by code: their names, and creating or revoking them, are website-only.
 */
@Composable
internal fun WalletAffiliateSection(
    report: AffiliateReport?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        RyntraSectionLabel(
            text = stringResource(R.string.affiliate_title),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ryntraCard(RyntraDesign.contentShape)
                .padding(20.dp),
        ) {
            when {
                report != null -> AffiliateReportContent(report)
                isLoading -> RyntraContentLoading()
                else -> Text(
                    text = errorMessage ?: stringResource(R.string.affiliate_failed),
                    color = RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.affiliate_manage_hint),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = { onOpenUrl(MODRINTH_AFFILIATE_LINKS_URL) }) {
                Text(stringResource(R.string.affiliate_manage))
            }
        }
    }
}

@Composable
private fun AffiliateReportContent(report: AffiliateReport) {
    Text(
        text = stringResource(R.string.affiliate_period, report.rangeDays),
        color = RyntraDesign.colors.labelSecondary,
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        AffiliateFigure(stringResource(R.string.affiliate_clicks), formatExactCount(report.totalClicks), Modifier.weight(1f))
        AffiliateFigure(
            stringResource(R.string.affiliate_conversions),
            formatExactCount(report.totalConversions),
            Modifier.weight(1f),
        )
        AffiliateFigure(stringResource(R.string.affiliate_revenue), formatWalletMoney(report.totalRevenue), Modifier.weight(1f))
    }
    if (report.codes.isEmpty()) {
        Text(
            text = stringResource(R.string.affiliate_none),
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }
    report.codes.forEach { code ->
        HorizontalDivider(color = RyntraDesign.colors.separator, modifier = Modifier.padding(top = 12.dp))
        AffiliateCodeRow(code)
    }
}

@Composable
private fun AffiliateCodeRow(code: AffiliateCodeStats) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = code.link,
                style = MaterialTheme.typography.bodyMedium,
                color = RyntraDesign.colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    "${stringResource(R.string.affiliate_clicks)} ${formatExactCount(code.clicks)}",
                    "${stringResource(R.string.affiliate_conversions)} ${formatExactCount(code.conversions)}",
                    formatWalletMoney(code.revenue),
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = RyntraDesign.colors.labelSecondary,
            )
        }
        IconButton(onClick = { copyLink(context, code.link) }) {
            Icon(Lucide.Copy, contentDescription = stringResource(R.string.affiliate_copy))
        }
    }
}

@Composable
private fun AffiliateFigure(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = RyntraDesign.colors.labelSecondary, maxLines = 1)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = RyntraDesign.colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun copyLink(context: Context, link: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Modrinth", link))
    // Android 13 and later confirm a copy themselves; a second toast would double it.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.affiliate_copied, Toast.LENGTH_SHORT).show()
    }
}
