package com.ryntra.mobile.ui.dashboard.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RollingText
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.WalletReport

/** Modrinth's own palette for dated revenue, so the bar reads the same as on the site. */
private val estimatePalette = listOf(
    Color(0xFF4E9CFF),
    Color(0xFFC084FC),
    Color(0xFFFB923C),
    Color(0xFFF87171),
)

/** One slice of the balance: money that is available, dated, or still processing. */
private data class BalanceSlice(
    val label: String,
    val amount: Double,
    val color: Color,
    /** Held money is striped, as on the site; only available money is solid. */
    val isHeld: Boolean,
    val detail: String? = null,
)

@Composable
private fun balanceSlices(report: WalletReport): List<BalanceSlice> {
    val colors = RyntraDesign.colors
    return buildList {
        add(BalanceSlice(stringResource(R.string.wallet_available_now), report.available, colors.positive, isHeld = false))
        report.estimated.forEachIndexed { index, payout ->
            add(
                BalanceSlice(
                    label = stringResource(R.string.wallet_estimated_on, formatWalletDate(payout.date)),
                    amount = payout.amount,
                    color = estimatePalette[index % estimatePalette.size],
                    isHeld = true,
                    detail = stringResource(R.string.wallet_estimated_hint),
                ),
            )
        }
        add(
            BalanceSlice(
                label = stringResource(R.string.wallet_processing),
                amount = report.processingAmount,
                color = colors.labelSecondary,
                isHeld = true,
                detail = stringResource(R.string.wallet_processing_hint),
            ),
        )
    }
}

/**
 * The balance the way modrinth.com/dashboard/revenue lays it out: the total, a bar split by
 * when each part can be withdrawn, and one row per part with its amount.
 */
@Composable
internal fun WalletBalanceBreakdown(
    report: WalletReport,
    showDetails: Boolean,
    modifier: Modifier = Modifier,
) {
    val slices = balanceSlices(report)
    Column(modifier) {
        Text(
            text = stringResource(R.string.wallet_balance),
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
        RollingText(
            text = formatWalletMoney(report.balance),
            color = RyntraDesign.colors.labelPrimary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 40.sp, stepSize = 1.sp),
        )
        BalanceBar(
            slices = slices,
            total = report.balance,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(12.dp),
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            slices.forEachIndexed { index, slice ->
                if (index > 0) HorizontalDivider(color = RyntraDesign.colors.separator)
                BalanceRow(slice, showDetails)
            }
        }
    }
}

@Composable
private fun BalanceRow(slice: BalanceSlice, showDetails: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Swatch(slice, Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(slice.label, style = MaterialTheme.typography.bodyMedium, color = RyntraDesign.colors.labelPrimary)
            if (showDetails && slice.detail != null) {
                Text(
                    text = slice.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = RyntraDesign.colors.labelSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = formatWalletMoney(slice.amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = RyntraDesign.colors.labelPrimary,
        )
    }
}

@Composable
private fun Swatch(slice: BalanceSlice, modifier: Modifier) {
    Canvas(modifier.clearAndSetSemantics {}) {
        val shape = RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.minDimension / 2))
        drawSlice(shape, slice.color, slice.isHeld)
    }
}

/**
 * Proportional bar of the balance. Parts under 1 % are dropped, as on the site, so a cent of
 * available money does not draw a sliver the eye reads as a rendering glitch. The rows below
 * carry the same numbers as text, so the bar is hidden from accessibility services.
 */
@Composable
private fun BalanceBar(slices: List<BalanceSlice>, total: Double, modifier: Modifier) {
    val track = RyntraDesign.colors.separator
    val visible = if (total > 0.0) slices.filter { it.amount / total >= 0.01 } else emptyList()
    Canvas(modifier.clearAndSetSemantics {}) {
        val radius = CornerRadius(size.height / 2)
        if (visible.isEmpty()) {
            drawRoundRect(track, cornerRadius = radius)
            return@Canvas
        }
        val gap = 4.dp.toPx()
        val drawable = size.width - gap * (visible.size - 1)
        val visibleTotal = visible.sumOf(BalanceSlice::amount)
        var start = 0f
        visible.forEach { slice ->
            val width = (drawable * (slice.amount / visibleTotal)).toFloat()
            drawSlice(RoundRect(start, 0f, start + width, size.height, radius), slice.color, slice.isHeld)
            start += width + gap
        }
    }
}

private fun DrawScope.drawSlice(shape: RoundRect, color: Color, isHeld: Boolean) {
    val path = Path().apply { addRoundRect(shape) }
    if (!isHeld) {
        drawPath(path, color)
        return
    }
    drawPath(path, color.copy(alpha = 0.28f))
    clipPath(path) {
        val step = 7.dp.toPx()
        val stroke = 2.5.dp.toPx()
        var x = shape.left - shape.height
        while (x < shape.right + shape.height) {
            drawLine(
                color = color,
                start = Offset(x, shape.bottom),
                end = Offset(x + shape.height, shape.top),
                strokeWidth = stroke,
            )
            x += step
        }
    }
}
