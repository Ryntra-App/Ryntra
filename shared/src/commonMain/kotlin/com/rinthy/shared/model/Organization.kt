package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modrinth organization (v3).
 * List + detail responses may include [members] inline.
 */
@Serializable
data class Organization(
    val id: String,
    val slug: String,
    val name: String,
    val description: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("team_id") val teamId: String? = null,
    val color: Int? = null,
    val members: List<ProjectMember> = emptyList(),
) {
    val acceptedMembers: List<ProjectMember>
        get() = members.filter { it.accepted }.ifEmpty { members }

    val memberCount: Int
        get() = acceptedMembers.size

    val owner: ProjectMember?
        get() = acceptedMembers.firstOrNull { it.isOwner }
            ?: acceptedMembers.minByOrNull { it.ordering }
}
