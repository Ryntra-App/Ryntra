package com.ryntra.mobile.auth

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthCoordinator(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun createAuthorizationUri(): Uri {
        val state = ByteArray(32).also(secureRandom::nextBytes).toHex()
        preferences.edit { putString(STATE_KEY, state) }
        return OAUTH_START_URL.toUri()
            .buildUpon()
            .appendQueryParameter("state", state)
            .build()
    }

    fun consumeCallback(uri: Uri): OAuthCallbackResult {
        if (uri.scheme !in CALLBACK_SCHEMES || uri.host != CALLBACK_HOST || uri.path != CALLBACK_PATH) {
            return OAuthCallbackResult.Ignored
        }

        val expectedState = preferences.getString(STATE_KEY, null)
        preferences.edit { remove(STATE_KEY) }
        val returnedState = uri.getQueryParameter("state")
        if (!statesMatch(expectedState, returnedState)) {
            return OAuthCallbackResult.Failure("Sign-in failed because the OAuth state did not match.")
        }

        if (!uri.getQueryParameter("error").isNullOrBlank()) {
            return OAuthCallbackResult.Failure("Modrinth sign-in was cancelled.")
        }

        val token = uri.getQueryParameter("token")?.trim()
        if (token.isNullOrEmpty() || token.length > MAX_TOKEN_LENGTH) {
            return OAuthCallbackResult.Failure("OAuth did not return a valid access token.")
        }
        return OAuthCallbackResult.Success(token)
    }

    fun clear() {
        preferences.edit { remove(STATE_KEY) }
    }

    private fun statesMatch(expected: String?, returned: String?): Boolean {
        if (expected == null || returned == null) return false
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            returned.toByteArray(Charsets.UTF_8),
        )
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        byte.toUByte().toString(radix = 16).padStart(length = 2, padChar = '0')
    }

    private companion object {
        const val PREFERENCES_NAME = "oauth_session"
        const val STATE_KEY = "expected_state"
        // The auth deployment still uses its original hostname and callback scheme.
        const val OAUTH_START_URL = "https://rinthy-auth.vercel.app/api/modrinth/start"
        val CALLBACK_SCHEMES = setOf("ryntra", "rinthy")
        const val CALLBACK_HOST = "auth"
        const val CALLBACK_PATH = "/callback"
        const val MAX_TOKEN_LENGTH = 4_096
    }
}

sealed interface OAuthCallbackResult {
    data object Ignored : OAuthCallbackResult

    data class Success(val token: String) : OAuthCallbackResult

    data class Failure(val message: String) : OAuthCallbackResult
}
