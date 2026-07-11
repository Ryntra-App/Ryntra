package com.rinthy.shared.network

import com.rinthy.shared.model.Account
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.Project
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class ModrinthApi(
    private val httpClient: HttpClient,
) {
    suspend fun getCurrentAccount(token: String): Account =
        httpClient.get("user") { authorize(token) }.decode()

    suspend fun getProjects(userId: String, token: String): List<Project> =
        httpClient.get("user/$userId/projects") { authorize(token) }.decode()

    suspend fun getOrganizations(userId: String, token: String): List<Organization> =
        httpClient.get("user/$userId/organizations") { authorize(token) }.decode()

    fun close() = httpClient.close()

    private fun io.ktor.client.request.HttpRequestBuilder.authorize(token: String) {
        val authorization = if (token.count { it == '.' } == 2 && ' ' !in token) {
            "Bearer $token"
        } else {
            token
        }
        header(HttpHeaders.Authorization, authorization)
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        if (status.isSuccess()) return body()

        val responseText = bodyAsText()
        val apiError = runCatching {
            apiJson.decodeFromString<ErrorResponse>(responseText)
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
}
