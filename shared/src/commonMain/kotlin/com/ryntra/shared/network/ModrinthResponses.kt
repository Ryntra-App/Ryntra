package com.ryntra.shared.network

import com.ryntra.shared.model.AnalyticsPoint
import com.ryntra.shared.model.AnalyticsProjectEvent
import com.ryntra.shared.model.WalletTransaction

data class AnalyticsResponse(
    val status: Int,
    val points: List<AnalyticsPoint> = emptyList(),
    val events: List<AnalyticsProjectEvent> = emptyList(),
)

data class PayoutHistoryResponse(
    val status: Int,
    val transactions: List<WalletTransaction> = emptyList(),
)

data class PayoutBalanceResponse(
    val status: Int,
    val available: Double? = null,
    val pending: Double? = null,
    val withdrawnLifetime: Double? = null,
    val withdrawnThisYear: Double? = null,
    /** ISO timestamp of each availability date to the revenue released on it. */
    val dates: Map<String, Double> = emptyMap(),
    val formCompletionStatus: String? = null,
)
