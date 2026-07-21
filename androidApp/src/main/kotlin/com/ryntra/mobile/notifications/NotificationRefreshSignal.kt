package com.ryntra.mobile.notifications

import android.content.Context
import android.content.Intent

internal object NotificationRefreshSignal {
    const val ACTION = "com.ryntra.mobile.action.NOTIFICATIONS_CHANGED"

    fun send(context: Context) {
        context.sendBroadcast(Intent(ACTION).setPackage(context.packageName))
    }
}
