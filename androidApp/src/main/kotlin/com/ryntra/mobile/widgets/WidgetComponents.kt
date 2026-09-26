package com.ryntra.mobile.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ryntra.mobile.AppScreenRequest
import com.ryntra.mobile.MainActivity
import com.ryntra.mobile.R
import com.ryntra.shared.model.WALLET_CURRENCY
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

/**
 * The three layouts every Ryntra widget has. Android picks the closest of these for the
 * cell size the user drags the widget to.
 */
internal enum class WidgetSize(val size: DpSize) {
    /** 2×2 cells: the one number the widget is about. */
    Small(DpSize(110.dp, 110.dp)),

    /** 4×2 cells: that number with its context beside it. */
    Medium(DpSize(250.dp, 110.dp)),

    /** 4×3 cells and up: room for a list or a full chart. */
    Large(DpSize(250.dp, 200.dp)),
    ;

    companion object {
        val all: Set<DpSize> = entries.map(WidgetSize::size).toSet()

        @Composable
        fun current(): WidgetSize {
            val size = LocalSize.current
            return when {
                size.width < Medium.size.width -> Small
                size.height < Large.size.height -> Medium
                else -> Large
            }
        }
    }
}

internal fun openAppIntent(context: Context, destination: AppScreenRequest): Intent =
    Intent(context, MainActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(MainActivity.EXTRA_OPEN_SCREEN, destination.name)

/** The rounded, themed card every Ryntra widget sits in; the whole card opens the app. */
@Composable
internal fun WidgetCard(
    context: Context,
    destination: AppScreenRequest,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
            .clickable(actionStartActivity(openAppIntent(context, destination))),
        content = content,
    )
}

@Composable
internal fun WidgetHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ryntra_launcher_monochrome),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(18.dp),
        )
        Text(
            text = title,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 6.dp),
        )
    }
}

@Composable
internal fun WidgetFigure(value: String, size: Int = 28, color: ColorProvider = GlanceTheme.colors.onSurface) {
    Text(
        text = value,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

@Composable
internal fun WidgetCaption(text: String, emphasized: Boolean = false, color: ColorProvider? = null) {
    Text(
        text = text,
        style = TextStyle(
            color = color ?: if (emphasized) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
        ),
        maxLines = 2,
    )
}

/** Label on the left, value on the right, one line — the rows of the larger layouts. */
@Composable
internal fun WidgetRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.padding(top = 6.dp)) {
        Text(
            text = label,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = value,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

/** Shown before the first refresh, or after sign-out. */
@Composable
internal fun WidgetSignedOut(context: Context) {
    WidgetCaption(context.getString(R.string.widget_sign_in))
}

internal fun widgetMoney(amount: Double): String =
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(WALLET_CURRENCY)
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

internal fun widgetCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)

/** Payout dates are UTC midnights; the device zone would show the day before in the Americas. */
internal fun widgetDate(isoTimestamp: String, style: FormatStyle = FormatStyle.MEDIUM): String = runCatching {
    Instant.parse(isoTimestamp).atOffset(ZoneOffset.UTC).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(style))
}.getOrDefault(isoTimestamp.take(10))
