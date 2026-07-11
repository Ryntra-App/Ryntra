package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String = "developer",
)
