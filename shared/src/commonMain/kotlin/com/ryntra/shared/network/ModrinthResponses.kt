package com.ryntra.shared.network

import com.ryntra.shared.model.AnalyticsPoint
import com.ryntra.shared.model.AnalyticsProjectEvent
import com.ryntra.shared.model.PayoutTransaction

data class AnalyticsResponse(
    val status: Int,
    val points: List<AnalyticsPoint> = emptyList(),
    val events: List<AnalyticsProjectEvent> = emptyList(),
)

data class PayoutHistoryResponse(
    val status: Int,
    val allTime: Double? = null,
    val lastMonth: Double? = null,
    val transactions: List<PayoutTransaction> = emptyList(),
)

data class PayoutBalanceResponse(
    val status: Int,
    val available: Double? = null,
    val pending: Double? = null,
    val withdrawnLifetime: Double? = null,
    val total: Double? = null,
    val currency: String? = null,
)
