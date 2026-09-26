package com.ryntra.shared.model

/** Modrinth keeps every creator balance in US dollars. */
const val WALLET_CURRENCY = "USD"

/** Lifecycle of a withdrawal, as labrinth reports it on `GET /v3/payout/history`. */
enum class PayoutStatus(val apiValue: String) {
    Success("success"),
    InTransit("in-transit"),
    Cancelling("cancelling"),
    Cancelled("cancelled"),
    Failed("failed"),
    Unknown("unknown"),
    ;

    companion object {
        fun fromApi(value: String?): PayoutStatus = entries.firstOrNull { it.apiValue == value } ?: Unknown
    }
}

enum class WalletTransactionKind {
    /** Revenue that became available to withdraw (`payout_available`). */
    Income,

    /** Money the creator sent out to a payout method (`withdrawal`). */
    Withdrawal,
}

/**
 * One row of the wallet history. Flat rather than a sealed type so the Swift layer can read it
 * without casting; the withdrawal-only fields are null on income.
 */
data class WalletTransaction(
    val kind: WalletTransactionKind,
    /** ISO-8601 timestamp. */
    val created: String,
    val amount: Double,
    val id: String? = null,
    val status: PayoutStatus? = null,
    val fee: Double? = null,
    /** `paypal`, `venmo`, `tremendous` or `muralpay`. */
    val methodType: String? = null,
    val methodAddress: String? = null,
    /** `creator_rewards` or `affiliates`, on income only. */
    val payoutSource: String? = null,
) {
    /** For Swift, which cannot switch on a bridged Kotlin enum reliably. */
    val isIncome: Boolean get() = kind == WalletTransactionKind.Income

    /** Modrinth only lets a withdrawal be cancelled while it is still in transit. */
    val canCancel: Boolean
        get() = kind == WalletTransactionKind.Withdrawal && status == PayoutStatus.InTransit && !id.isNullOrBlank()

    val year: Int? get() = created.take(4).toIntOrNull()

    /** `yyyy-MM`, the key the history groups rows by. */
    val yearMonth: String get() = created.take(7)
}

/** Revenue with a fixed date on which Modrinth makes it available to withdraw. */
data class ScheduledPayout(
    /** ISO-8601 timestamp of the day the money becomes available. */
    val date: String,
    val amount: Double,
)

/** Where the creator stands with the tax form Modrinth asks for past a yearly threshold. */
enum class TaxFormStatus(val apiValue: String) {
    Unknown("unknown"),
    Unrequested("unrequested"),
    Unsigned("unsigned"),
    TinMismatch("tin-mismatch"),
    Complete("complete"),
    ;

    companion object {
        fun fromApi(value: String?): TaxFormStatus? =
            value?.let { raw -> entries.firstOrNull { it.apiValue == raw } ?: Unknown }
    }
}

/**
 * Everything the wallet screens show, built the way modrinth.com/dashboard/revenue builds it:
 * the balance is what can be withdrawn now plus what Modrinth still holds, and the held part is
 * split into dated estimates and the current month's revenue that is still processing.
 */
data class WalletReport(
    val available: Double = 0.0,
    val pending: Double = 0.0,
    val withdrawnLifetime: Double = 0.0,
    val withdrawnThisYear: Double = 0.0,
    /** Finalised revenue waiting for its release date, soonest first. */
    val estimated: List<ScheduledPayout> = emptyList(),
    /** This month's revenue; it only gets a firm date once the month closes. */
    val processing: ScheduledPayout? = null,
    /** Null when Modrinth has tax compliance switched off for the account. */
    val taxFormStatus: TaxFormStatus? = null,
    /** US dollars a creator may withdraw this year before the tax form is required. */
    val taxFormThreshold: Double? = null,
    /** Newest first. */
    val transactions: List<WalletTransaction> = emptyList(),
    val balanceStatus: Int = 0,
    val historyStatus: Int = 0,
) {
    val isBalanceAvailable: Boolean get() = balanceStatus in 200..299
    val isHistoryAvailable: Boolean get() = historyStatus in 200..299
    val isAvailable: Boolean get() = isBalanceAvailable || isHistoryAvailable

    val balance: Double get() = available + pending
    val processingAmount: Double get() = processing?.amount ?: 0.0

    /** A TIN that did not match IRS records locks withdrawals until support resets the form. */
    val isWithdrawalLocked: Boolean get() = taxFormStatus == TaxFormStatus.TinMismatch

    /** Mirrors the site banner: over the yearly threshold without a completed form. */
    val isTaxFormRequired: Boolean
        get() {
            val threshold = taxFormThreshold ?: return false
            val status = taxFormStatus ?: TaxFormStatus.Unknown
            return withdrawnThisYear >= threshold &&
                status != TaxFormStatus.Complete &&
                status != TaxFormStatus.TinMismatch
        }

    val hasTransactions: Boolean get() = transactions.isNotEmpty()

    fun recentTransactions(limit: Int): List<WalletTransaction> = transactions.take(limit)
}

/**
 * The threshold that applies in [year], with Modrinth's own fallback: a year before the table
 * uses its first entry and a year after it uses its last.
 */
internal fun taxFormThresholdFor(year: Int, thresholds: Map<Int, Double>): Double? {
    if (thresholds.isEmpty()) return null
    thresholds[year]?.let { return it }
    val years = thresholds.keys.sorted()
    return if (year < years.first()) thresholds.getValue(years.first()) else thresholds.getValue(years.last())
}
