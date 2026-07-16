package com.ryntra.mobile.security

import android.content.Context
class SecureTokenStore(context: Context) {
    private val storage = EncryptedValueStore(context, PREFERENCES_NAME, KEY_ALIAS, TOKEN_KEY)

    fun read(): String? = storage.read()

    fun write(token: String) = storage.write(token)

    fun clear() = storage.clear()

    private companion object {
        const val KEY_ALIAS = "ryntra_session_token"
        const val PREFERENCES_NAME = "secure_session"
        const val TOKEN_KEY = "token"
    }
}
