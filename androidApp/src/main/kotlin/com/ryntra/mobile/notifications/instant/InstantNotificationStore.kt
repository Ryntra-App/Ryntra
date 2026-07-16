package com.ryntra.mobile.notifications.instant

import android.content.Context
import androidx.core.content.edit
import com.ryntra.mobile.security.EncryptedValueStore
import java.util.UUID

internal class InstantNotificationStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secretStore = EncryptedValueStore(
        context = context,
        preferencesName = SECURE_PREFERENCES_NAME,
        keyAlias = "ryntra_notification_installation",
        valueKey = "installation_secret",
    )

    val installationId: String
        get() = preferences.getString(INSTALLATION_ID_KEY, null) ?: createInstallationId()

    var pendingState: String?
        get() = preferences.getString(PENDING_STATE_KEY, null)
        set(value) = preferences.edit { putString(PENDING_STATE_KEY, value) }

    var isConnected: Boolean
        get() = preferences.getBoolean(CONNECTED_KEY, false)
        set(value) = preferences.edit { putBoolean(CONNECTED_KEY, value) }

    fun readSecret(): String? = secretStore.read()

    fun writeSecret(secret: String) = secretStore.write(secret)

    fun resetRegistration() {
        secretStore.clear()
        preferences.edit {
            remove(INSTALLATION_ID_KEY)
            remove(PENDING_STATE_KEY)
            putBoolean(CONNECTED_KEY, false)
        }
    }

    private fun createInstallationId(): String {
        val value = UUID.randomUUID().toString().replace("-", "")
        preferences.edit { putString(INSTALLATION_ID_KEY, value) }
        return value
    }

    private companion object {
        const val PREFERENCES_NAME = "instant_notifications"
        const val SECURE_PREFERENCES_NAME = "secure_instant_notifications"
        const val INSTALLATION_ID_KEY = "installation_id"
        const val PENDING_STATE_KEY = "pending_oauth_state"
        const val CONNECTED_KEY = "connected"
    }
}
