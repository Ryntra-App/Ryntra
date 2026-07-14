package com.rinthy.shared.network.modrinth

import com.rinthy.shared.model.Account
import com.rinthy.shared.model.AccountProfileUpdate
import com.rinthy.shared.network.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
