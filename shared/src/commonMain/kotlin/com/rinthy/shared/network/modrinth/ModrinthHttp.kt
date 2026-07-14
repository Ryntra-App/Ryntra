package com.rinthy.shared.network.modrinth

import com.rinthy.shared.network.ApiException
import com.rinthy.shared.network.apiJson
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal fun HttpRequestBuilder.authorize(token: String) {
    val normalized = token.trim()
    val authorization = if (normalized.count { it == '.' } == 2 && ' ' !in normalized) {
        "Bearer $normalized"
    } else {
        normalized
    }
    header(HttpHeaders.Authorization, authorization)
}

internal suspend inline fun <reified T> HttpResponse.decode(): T {
    ensureSuccess()
    return body()
}

internal suspend fun HttpResponse.ensureSuccess() {
    if (status.isSuccess()) return

    val apiError = runCatching {
        apiJson.decodeFromString<ErrorResponse>(bodyAsText())
    }.getOrNull()
    val safeMessage = when (status.value) {
        401 -> "The access token is invalid or expired."
        403 -> "This token does not have the required permission."
        429 -> "Modrinth is receiving too many requests. Try again shortly."
        else -> apiError?.description ?: "Modrinth request failed (${status.value})."
    }
    throw ApiException(status.value, safeMessage)
}

@Serializable
private data class ErrorResponse(
    val error: String? = null,
    val description: String? = null,
)
