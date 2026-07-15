package com.ryntra.mobile.ui.components

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val integerFormat = ThreadLocal.withInitial(NumberFormat::getIntegerInstance)
private val currencyFormats = ThreadLocal.withInitial { mutableMapOf<String, NumberFormat>() }
private val projectDateFormat = ThreadLocal.withInitial {
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
}

fun formatExactCount(value: Long): String = checkNotNull(integerFormat.get()).format(value)

fun formatCurrency(value: Double, currencyCode: String): String {
    val normalizedCode = currencyCode.uppercase()
    val formatter = checkNotNull(currencyFormats.get()).getOrPut(normalizedCode) {
        NumberFormat.getCurrencyInstance().apply {
            currency = runCatching { Currency.getInstance(normalizedCode) }.getOrDefault(Currency.getInstance("USD"))
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    return formatter.format(value)
}

fun formatProjectDate(value: String?): String? {
    val isoDate = value?.take(10) ?: return null
    return runCatching {
        LocalDate.parse(isoDate).format(checkNotNull(projectDateFormat.get()))
    }.getOrNull()
}
