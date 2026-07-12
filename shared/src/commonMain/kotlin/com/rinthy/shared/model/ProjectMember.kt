package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectMember(
    val user: Account,
    @SerialName("team_id") val teamId: String,
    val role: String = "Member",
    @SerialName("is_owner") val isOwner: Boolean = false,
    val permissions: Int = 0,
    @SerialName("organization_permissions") val organizationPermissions: Int = 0,
    @SerialName("payouts_split") val payoutsSplit: Double? = null,
    val ordering: Int = 0,
    val accepted: Boolean = false,
)
