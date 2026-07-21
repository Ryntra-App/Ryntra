package com.ryntra.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NotificationRelayClient internal constructor(
    baseUrl: String,
    private val httpClient: HttpClient,
) {
    constructor(baseUrl: String) : this(baseUrl, createPlatformHttpClient())

    private val endpoint = baseUrl.trim().removeSuffix("/")

    init {
        require(endpoint.startsWith("https://")) { "Notification relay URL must use HTTPS." }
    }

    suspend fun registerInstallation(
        installationId: String,
        platform: String,
        pushToken: String,
        locale: String = "en",
        secret: String? = null,
    ): RelayRegistration {
        val response = httpClient.post("$endpoint/api/notifications/installations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            secret?.let { header(INSTALLATION_SECRET_HEADER, it) }
            setBody(RegisterInstallationRequest(installationId, platform, pushToken, locale))
        }
        return response.decodeRelayResponse()
    }

    suspend fun getStatus(installationId: String, secret: String): RelayStatus {
        val response = httpClient.get("$endpoint/api/notifications/installations") {
            header(INSTALLATION_SECRET_HEADER, secret)
            parameter("installation_id", installationId)
        }
        return response.decodeRelayResponse()
    }

    suspend fun createEnrollment(
        installationId: String,
        secret: String,
        clientState: String,
    ): RelayEnrollment {
        val response = httpClient.post("$endpoint/api/notifications/enrollments") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(INSTALLATION_SECRET_HEADER, secret)
            setBody(EnrollmentRequest(installationId, clientState))
        }
        return response.decodeRelayResponse()
    }

    suspend fun disconnect(installationId: String, secret: String) {
        httpClient.delete("$endpoint/api/notifications/installations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(INSTALLATION_SECRET_HEADER, secret)
            setBody(InstallationRequest(installationId))
        }.ensureRelaySuccess()
    }

    fun close() = httpClient.close()

    private companion object {
        const val INSTALLATION_SECRET_HEADER = "X-Installation-Secret"
    }
}

@Serializable
data class RelayRegistration(
    val installationSecret: String? = null,
    val isCreated: Boolean = false,
)

@Serializable
data class RelayStatus(
    val isConnected: Boolean,
    val platform: String,
    val connectedAt: String? = null,
    val disabledReason: String? = null,
)

@Serializable
data class RelayEnrollment(
    val authorizationUrl: String,
    val expiresIn: Int,
)

@Serializable
private data class RegisterInstallationRequest(
    val installationId: String,
    val platform: String,
    val pushToken: String,
    val locale: String,
)

@Serializable
private data class EnrollmentRequest(
    val installationId: String,
    val clientState: String,
)

@Serializable
private data class InstallationRequest(
    val installationId: String,
)

@Serializable
private data class RelayErrorResponse(
    val error: String? = null,
    val message: String? = null,
)

private suspend inline fun <reified T> HttpResponse.decodeRelayResponse(): T {
    ensureRelaySuccess()
    return body()
}

private suspend fun HttpResponse.ensureRelaySuccess() {
    if (status.isSuccess()) return
    val error = runCatching { apiJson.decodeFromString<RelayErrorResponse>(bodyAsText()) }.getOrNull()
    val safeMessage = when (status.value) {
        401 -> "This device is no longer authorized for instant notifications."
        404 -> "The notification service endpoint was not found."
        409 -> "This device registration already exists."
        429 -> "The notification service is busy. Try again shortly."
        else -> error?.message ?: "Notification service request failed (${status.value})."
    }
    throw ApiException(status.value, safeMessage)
}
