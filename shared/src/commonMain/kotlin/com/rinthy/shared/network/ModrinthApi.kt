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
import kotlinx.coroutines.CancellationException

class ModrinthApi(
    private val httpClient: HttpClient,
) {
    suspend fun getCurrentAccount(token: String): Account =
        httpClient.get("user") { authorize(token) }.decode()

    suspend fun getProjects(userId: String, token: String): List<Project> =
        httpClient.get("user/$userId/projects") { authorize(token) }.decode()

    suspend fun getProject(projectIdOrSlug: String, token: String): Project =
        httpClient.get("project/$projectIdOrSlug") { authorize(token) }.decode()

    suspend fun getOrganizations(userId: String, token: String): List<Organization> {
        val endpoints = listOf(
            "https://api.modrinth.com/v3/user/$userId/organizations",
            "https://api.modrinth.com/v2/user/$userId/organizations",
        )
        var authorizationFailure: ApiException? = null

        for (endpoint in endpoints) {
            try {
                return httpClient.get(endpoint) { authorize(token) }.decode()
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                if (error.statusCode == 401 || error.statusCode == 403) {
                    authorizationFailure = error
                }
            } catch (_: Exception) {
                // Organizations are optional dashboard data; try the fallback API version.
            }
        }

        authorizationFailure?.let { throw it }
        return emptyList()
    }

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
