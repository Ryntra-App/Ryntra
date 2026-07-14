package com.rinthy.shared.model

data class PayoutTransaction(
    val created: String,
    val amount: Double,
    val status: String,
)

data class WalletReport(
    val currency: String = "USD",
    val wallet: String? = null,
    val walletType: String? = null,
    val payoutAddress: String? = null,
    val available: Double? = null,
    val pending: Double? = null,
    val withdrawnLifetime: Double? = null,
    val balance: Double? = null,
    val allTime: Double? = null,
    val lastMonth: Double? = null,
    val transactions: List<PayoutTransaction> = emptyList(),
    val balanceStatus: Int = 0,
    val historyStatus: Int = 0,
) {
    val isAvailable: Boolean get() = balanceStatus in 200..299 || historyStatus in 200..299 || balance != null

    val lifetimeEarnings: Double?
        get() = allTime ?: if (balance == null && withdrawnLifetime == null) {
            null
        } else {
            (balance ?: 0.0) + (withdrawnLifetime ?: 0.0)
        }
}
