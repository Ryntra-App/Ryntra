package com.rinthy.shared.network

import com.rinthy.shared.model.AnalyticsPoint
import com.rinthy.shared.model.AnalyticsProjectEvent
import com.rinthy.shared.model.PayoutTransaction

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
