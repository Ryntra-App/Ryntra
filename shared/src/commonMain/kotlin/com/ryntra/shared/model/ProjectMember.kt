package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectMember(
    val user: Account,
    @SerialName("team_id") val teamId: String = "",
    val role: String = "Member",
    @SerialName("is_owner") val isOwner: Boolean = false,
    // Organization member payloads often send null for these bitfields.
    val permissions: Int? = null,
    @SerialName("organization_permissions") val organizationPermissions: Int? = null,
    @SerialName("payouts_split") val payoutsSplit: Double? = null,
    val ordering: Int = 0,
    val accepted: Boolean = true,
)
