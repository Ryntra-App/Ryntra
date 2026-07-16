package com.ryntra.mobile.notifications.instant

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.ryntra.mobile.BuildConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal object FirebaseBootstrap {
    val isConfigured: Boolean
        get() = listOf(
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_APPLICATION_ID,
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_SENDER_ID,
        ).none(String::isBlank)

    fun initialize(context: Context): Boolean {
        if (!isConfigured) return false
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        FirebaseApp.initializeApp(context, options)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        return true
    }

    @Suppress("DEPRECATION")
    suspend fun token(context: Context): String {
        check(initialize(context)) { "Firebase is not configured for this build." }
        return FirebaseMessaging.getInstance().token.awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { completed ->
        when {
            completed.isSuccessful -> continuation.resume(completed.result)
            completed.exception != null -> continuation.resumeWithException(completed.exception!!)
            else -> continuation.resumeWithException(IllegalStateException("Firebase task did not return a result."))
        }
    }
}
