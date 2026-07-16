package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthNotification(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String? = null,
    val title: String,
    val text: String,
    val link: String,
    val read: Boolean,
    val created: String,
    val actions: List<ModrinthNotificationAction> = emptyList(),
) {
    val kind: ModrinthNotificationKind
        get() = ModrinthNotificationKind.fromApiValue(type)
}

@Serializable
data class ModrinthNotificationAction(
    val title: String = "",
    @SerialName("action_route") val actionRoute: List<String> = emptyList(),
)

enum class ModrinthNotificationKind {
    ProjectUpdate,
    TeamInvite,
    StatusChange,
    ModeratorMessage,
    Unknown;

    companion object {
        fun fromApiValue(value: String?): ModrinthNotificationKind = when (value) {
            "project_update" -> ProjectUpdate
            "team_invite" -> TeamInvite
            "status_change" -> StatusChange
            "moderator_message" -> ModeratorMessage
            else -> Unknown
        }
    }
}
