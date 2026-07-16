package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ModerationThread
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ThreadEndpoints(
    private val client: HttpClient,
) {
    suspend fun get(threadId: String, token: String): ModerationThread =
        client.get("thread/$threadId") { authorize(token) }.decode()

    suspend fun reply(threadId: String, body: String, replyingTo: String?, token: String) {
        client.post("thread/$threadId") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(ThreadReply(type = "text", body = body, replyingTo = replyingTo))
        }.ensureSuccess()
    }

    suspend fun deleteMessage(messageId: String, token: String) {
        client.delete("message/$messageId") { authorize(token) }.ensureSuccess()
    }
}

@Serializable
private data class ThreadReply(
    val type: String,
    val body: String,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("replying_to") val replyingTo: String? = null,
)
