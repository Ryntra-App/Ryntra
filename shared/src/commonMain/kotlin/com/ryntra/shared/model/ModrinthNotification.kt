package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    @Transient val projectTitle: String? = null,
    @Transient val versionTitle: String? = null,
) {
    val kind: ModrinthNotificationKind
        get() = ModrinthNotificationKind.fromApiValue(type)

    val projectReference: String?
        get() = ModrinthNotificationLink.parse(link)?.projectIdOrSlug
}

data class ModrinthNotificationLink(
    val projectIdOrSlug: String,
    val versionId: String? = null,
) {
    companion object {
        private val projectRoutes = setOf("mod", "plugin", "datapack", "shader", "resourcepack", "project")

        fun parse(link: String): ModrinthNotificationLink? {
            val route = when {
                "modrinth.com/" in link -> link.substringAfter("modrinth.com/")
                link.startsWith("ryntra://modrinth/") -> link.substringAfter("ryntra://modrinth/")
                else -> link
            }
            val path = route
                .substringBefore('?')
                .substringBefore('#')
                .trim('/')
            val segments = path.split('/').filter(String::isNotBlank)
            if (segments.size < 2 || segments.first().lowercase() !in projectRoutes) return null
            val versionMarker = segments.indexOf("version")
            return ModrinthNotificationLink(
                projectIdOrSlug = segments[1],
                versionId = versionMarker.takeIf { it >= 0 }?.let { segments.getOrNull(it + 1) },
            )
        }
    }
}

@Serializable
data class ModrinthNotificationAction(
    val title: String = "",
    @SerialName("action_route") val actionRoute: List<String> = emptyList(),
) {
    val teamJoinId: String?
        get() {
            if (actionRoute.size != 2 || actionRoute[0].uppercase() != "POST") return null
            val segments = actionRoute[1].trim('/').split('/')
            if (segments.size != 3 || segments[0] != "team" || segments[2] != "join") return null
            return segments[1].takeIf { TEAM_ID.matches(it) }
        }

    private companion object {
        val TEAM_ID = Regex("^[A-Za-z0-9_-]{1,128}$")
    }
}

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
