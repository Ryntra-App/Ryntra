package com.ryntra.mobile.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import com.ryntra.mobile.AppScreenRequest
import com.ryntra.mobile.R
import com.ryntra.shared.model.WidgetPayout
import com.ryntra.shared.model.WidgetSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.FormatStyle

/**
 * The creator balance and when it is paid out, as on Modrinth's revenue page:
 * small shows the balance and the next date, medium adds what is available now, and large
 * lists every upcoming payout.
 */
class WalletWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSize.all)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { WidgetSnapshotStore(context).read() }
        provideContent {
            GlanceTheme {
                WidgetCard(context, AppScreenRequest.Wallet) {
                    WidgetHeader(context.getString(R.string.widget_wallet_title))
                    Spacer(GlanceModifier.defaultWeight())
                    val balance = snapshot?.balance
                    when {
                        snapshot == null -> WidgetSignedOut(context)
                        balance == null -> WidgetCaption(context.getString(R.string.widget_wallet_unavailable))
                        else -> when (WidgetSize.current()) {
                            WidgetSize.Small -> SmallWallet(context, snapshot, balance)
                            WidgetSize.Medium -> MediumWallet(context, snapshot, balance)
                            WidgetSize.Large -> LargeWallet(context, snapshot, balance)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallWallet(context: Context, snapshot: WidgetSnapshot, balance: Double) {
        WidgetFigure(widgetMoney(balance), size = 24)
        snapshot.nextPayout?.let { WidgetCaption(payoutLine(context, it, FormatStyle.SHORT), emphasized = true) }
    }

    @Composable
    private fun MediumWallet(context: Context, snapshot: WidgetSnapshot, balance: Double) {
        Row {
            Column(modifier = GlanceModifier.defaultWeight()) {
                WidgetFigure(widgetMoney(balance), size = 28)
                WidgetCaption(context.getString(R.string.widget_wallet_balance))
            }
            Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                snapshot.available?.let {
                    WidgetFigure(widgetMoney(it), size = 16)
                    WidgetCaption(context.getString(R.string.widget_wallet_available_label))
                }
                snapshot.nextPayout?.let {
                    WidgetCaption(payoutLine(context, it, FormatStyle.MEDIUM), emphasized = true)
                }
            }
        }
    }

    @Composable
    private fun LargeWallet(context: Context, snapshot: WidgetSnapshot, balance: Double) {
        WidgetFigure(widgetMoney(balance), size = 32)
        snapshot.available?.let {
            WidgetRow(context.getString(R.string.widget_wallet_available_label), widgetMoney(it))
        }
        snapshot.upcomingPayouts.take(MAX_LARGE_ROWS).forEach { payout ->
            WidgetRow(payoutLabel(context, payout), widgetMoney(payout.amount))
        }
    }

    private fun payoutLabel(context: Context, payout: WidgetPayout): String =
        payout.date?.let { context.getString(R.string.widget_wallet_estimated, widgetDate(it)) }
            ?: context.getString(R.string.widget_wallet_processing)

    private fun payoutLine(context: Context, payout: WidgetPayout, style: FormatStyle): String {
        val whenText = payout.date?.let { widgetDate(it, style) } ?: context.getString(R.string.widget_wallet_processing)
        return context.getString(R.string.widget_wallet_next, widgetMoney(payout.amount), whenText)
    }

    private companion object {
        /** Available now plus three dates fill a 4×3 widget without clipping. */
        const val MAX_LARGE_ROWS = 3
    }
}

class WalletWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WalletWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RyntraWidgets.scheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelRefreshIfUnused(context)
    }
}
