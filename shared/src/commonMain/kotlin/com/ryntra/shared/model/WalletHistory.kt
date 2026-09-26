package com.ryntra.shared.model

/** Transactions that fall in one calendar month, newest month first. */
data class WalletMonth(
    /** `yyyy-MM`. */
    val yearMonth: String,
    val transactions: List<WalletTransaction>,
) {
    val year: Int get() = yearMonth.take(4).toIntOrNull() ?: 0
    val month: Int get() = yearMonth.drop(5).take(2).toIntOrNull() ?: 0
}

/**
 * The transaction history page of Modrinth's revenue dashboard: a year filter, totals for the
 * selection and the rows grouped by month. [year] null means every year.
 */
data class WalletHistory(
    val transactions: List<WalletTransaction>,
    val year: Int? = null,
) {
    /** Years that have at least one transaction, newest first. */
    val years: List<Int> get() = transactions.mapNotNull(WalletTransaction::year).distinct().sortedDescending()

    val filtered: List<WalletTransaction>
        get() = if (year == null) transactions else transactions.filter { it.year == year }

    val received: Double
        get() = filtered.filter { it.kind == WalletTransactionKind.Income }.sumOf(WalletTransaction::amount)

    val withdrawn: Double
        get() = filtered.filter { it.kind == WalletTransactionKind.Withdrawal }.sumOf(WalletTransaction::amount)

    val months: List<WalletMonth>
        get() = filtered
            .groupBy(WalletTransaction::yearMonth)
            .map { (yearMonth, rows) -> WalletMonth(yearMonth, rows.sortedByDescending(WalletTransaction::created)) }
            .sortedByDescending(WalletMonth::yearMonth)

    fun withYear(year: Int?): WalletHistory = copy(year = year)

    /** Same columns as the site's "Download as CSV": Date, Type, Source, Status, Amount, Fee. */
    fun toCsv(): String = buildString {
        append("Date,Type,Source,Status,Amount,Fee\n")
        filtered.forEach { transaction ->
            val isWithdrawal = transaction.kind == WalletTransactionKind.Withdrawal
            val row = listOf(
                transaction.created.replace('T', ' ').take(19),
                if (isWithdrawal) "Withdrawal" else "Payout",
                if (isWithdrawal) payoutMethodName(transaction.methodType) else payoutSourceName(transaction.payoutSource),
                if (isWithdrawal) transaction.status?.apiValue.orEmpty() else "",
                transaction.amount.toString(),
                transaction.fee?.toString().orEmpty(),
            )
            append(row.joinToString(",", transform = ::escapeCsvField)).append('\n')
        }
    }
}

/** Brand names are not translated, so the shared layer can name them for every platform. */
fun payoutMethodName(methodType: String?): String = when (methodType) {
    "paypal" -> "PayPal"
    "venmo" -> "Venmo"
    "tremendous" -> "Tremendous"
    "muralpay" -> "Mural Pay"
    null, "" -> "Unknown"
    else -> methodType.replaceFirstChar(Char::uppercase)
}

internal fun payoutSourceName(source: String?): String =
    source?.split('_')?.joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) } ?: "Income"

private fun escapeCsvField(field: String): String =
    if (field.any { it == ',' || it == '"' || it == '\n' }) "\"${field.replace("\"", "\"\"")}\"" else field
