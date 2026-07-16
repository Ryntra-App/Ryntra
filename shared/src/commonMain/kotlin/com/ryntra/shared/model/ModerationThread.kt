package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModerationThread(
    val id: String,
    val type: String,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("report_id") val reportId: String? = null,
    val messages: List<ModerationMessage> = emptyList(),
    val members: List<Account> = emptyList(),
) {
    fun authorOf(message: ModerationMessage): Account? =
        message.authorId?.let { authorId -> members.firstOrNull { it.id == authorId } }
}

@Serializable
data class ModerationMessage(
    val id: String,
    @SerialName("author_id") val authorId: String? = null,
    val body: ModerationMessageBody,
    val created: String,
)

@Serializable
data class ModerationMessageBody(
    val type: String,
    val body: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("replying_to") val replyingTo: String? = null,
    @SerialName("old_status") val oldStatus: String? = null,
    @SerialName("new_status") val newStatus: String? = null,
)
