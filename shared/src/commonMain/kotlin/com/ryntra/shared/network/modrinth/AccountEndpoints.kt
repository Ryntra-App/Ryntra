package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.Account
import com.ryntra.shared.model.AccountProfileUpdate
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.network.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class AccountEndpoints(
    private val client: HttpClient,
) {
    suspend fun getCurrent(token: String): Account =
        client.get("user") { authorize(token) }.decode()

    suspend fun updateProfile(userId: String, update: AccountProfileUpdate, token: String) {
        client.patch("user/$userId") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(update)
        }.ensureSuccess()
    }

    /**
     * PATCH `/user/{id}/icon` — avatar up to 2 MiB (OpenAPI Image body).
     * Some labrinth builds also accept `ext` like project icons; send it for compatibility.
     */
    suspend fun changeAvatar(userId: String, file: ProjectFileUpload, token: String) {
        client.patch("user/$userId/icon") {
            authorize(token)
            parameter("ext", file.imageExtension())
            contentType(ContentType.parse(file.contentType))
            setBody(file.bytes)
        }.ensureSuccess()
    }

    suspend fun deleteAvatar(userId: String, token: String) {
        client.delete("user/$userId/icon") { authorize(token) }.ensureSuccess()
    }

    suspend fun findUser(username: String, token: String): Account? {
        val normalized = username.trim()
        if (normalized.isEmpty() || normalized.length > 39 || normalized.any(Char::isWhitespace)) return null
        return try {
            client.get("user/$normalized") { authorize(token) }.decode()
        } catch (error: ApiException) {
            if (error.statusCode == 404) null else throw error
        }
    }
}
