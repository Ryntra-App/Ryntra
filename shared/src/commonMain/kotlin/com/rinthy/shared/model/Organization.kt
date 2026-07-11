package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: String,
    val slug: String,
    val name: String,
    val description: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("team_id") val teamId: String? = null,
)
