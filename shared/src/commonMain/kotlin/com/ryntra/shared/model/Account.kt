package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Account(
    val id: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String = "developer",
    @SerialName("payout_data") val payoutData: PayoutData? = null,
)

@Serializable
data class PayoutData(
    @SerialName("balance") private val rawBalance: JsonElement? = null,
    @SerialName("payout_wallet") val wallet: String? = null,
    @SerialName("payout_wallet_type") val walletType: String? = null,
    @SerialName("payout_address") val address: String? = null,
    val currency: String? = null,
) {
    val balance: Double? get() = rawBalance?.jsonPrimitive?.doubleOrNull
}

@Serializable
data class AccountProfileUpdate(
    val username: String,
    val bio: String,
)
