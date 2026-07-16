package com.ryntra.mobile.notifications.instant

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.ryntra.mobile.BuildConfig
import com.ryntra.shared.network.ApiException
import com.ryntra.shared.network.NotificationRelayClient
import java.security.MessageDigest
import java.security.SecureRandom

internal class InstantNotificationCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val store = InstantNotificationStore(appContext)
    private val relay = NotificationRelayClient(BuildConfig.BACKEND_URL)
    private val secureRandom = SecureRandom()

    val isAvailable: Boolean
        get() = FirebaseBootstrap.isConfigured

    val isConnected: Boolean
        get() = store.isConnected

    suspend fun createAuthorizationUri(): Uri {
        val pushToken = FirebaseBootstrap.token(appContext)
        val credentials = ensureRegistration(pushToken)
        val clientState = randomState()
        store.pendingState = clientState
        val enrollment = relay.createEnrollment(credentials.first, credentials.second, clientState)
        return enrollment.authorizationUrl.toUri()
    }

    fun consumeCallback(uri: Uri): InstantCallbackResult {
        if (uri.scheme !in CALLBACK_SCHEMES || uri.host != CALLBACK_HOST || uri.path != CALLBACK_PATH) {
            return InstantCallbackResult.Ignored
        }
        val expected = store.pendingState
        store.pendingState = null
        if (!statesMatch(expected, uri.getQueryParameter("state"))) {
            return InstantCallbackResult.Failure("Instant notification authorization expired. Try again.")
        }
        if (!uri.getQueryParameter("error").isNullOrBlank() || uri.getQueryParameter("status") != "connected") {
            return InstantCallbackResult.Failure("Instant notification authorization was cancelled.")
        }
        store.isConnected = true
        return InstantCallbackResult.Success
    }

    suspend fun updatePushToken(pushToken: String) {
        val secret = store.readSecret() ?: return
        relay.registerInstallation(store.installationId, PLATFORM, pushToken, secret)
    }

    suspend fun disconnect() {
        val secret = store.readSecret()
        if (secret != null) relay.disconnect(store.installationId, secret)
        store.resetRegistration()
    }

    fun close() = relay.close()

    private suspend fun ensureRegistration(pushToken: String): Pair<String, String> {
        val existingSecret = store.readSecret()
        if (existingSecret != null) {
            return try {
                relay.registerInstallation(store.installationId, PLATFORM, pushToken, existingSecret)
                store.installationId to existingSecret
            } catch (error: ApiException) {
                if (error.statusCode != 401) throw error
                store.resetRegistration()
                registerNewInstallation(pushToken)
            }
        }
        return registerNewInstallation(pushToken)
    }

    private suspend fun registerNewInstallation(pushToken: String): Pair<String, String> {
        val installationId = store.installationId
        val registration = relay.registerInstallation(installationId, PLATFORM, pushToken)
        val secret = requireNotNull(registration.installationSecret) {
            "Notification service did not return installation credentials."
        }
        store.writeSecret(secret)
        return installationId to secret
    }

    private fun randomState(): String = ByteArray(32).also(secureRandom::nextBytes).joinToString("") {
        it.toUByte().toString(16).padStart(2, '0')
    }

    private fun statesMatch(expected: String?, returned: String?): Boolean {
        if (expected == null || returned == null) return false
        return MessageDigest.isEqual(expected.toByteArray(), returned.toByteArray())
    }

    private companion object {
        const val PLATFORM = "android"
        val CALLBACK_SCHEMES = setOf("ryntra", "rinthy")
        const val CALLBACK_HOST = "notifications"
        const val CALLBACK_PATH = "/callback"
    }
}

internal sealed interface InstantCallbackResult {
    data object Ignored : InstantCallbackResult
    data object Success : InstantCallbackResult
    data class Failure(val message: String) : InstantCallbackResult
}
