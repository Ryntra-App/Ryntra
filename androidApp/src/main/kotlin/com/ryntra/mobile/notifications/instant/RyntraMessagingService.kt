package com.ryntra.mobile.notifications.instant

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ryntra.mobile.MainActivity
import com.ryntra.mobile.R
import com.ryntra.mobile.notifications.NotificationActionReceiver
import com.ryntra.mobile.notifications.NotificationChannels
import com.ryntra.mobile.notifications.NotificationBadgeStore
import com.ryntra.mobile.notifications.NotificationRefreshSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Suppress("OVERRIDE_DEPRECATION")
class RyntraMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            val coordinator = InstantNotificationCoordinator(applicationContext)
            try {
                coordinator.updatePushToken(token)
            } finally {
                coordinator.close()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        val notificationId = message.data["notification_id"] ?: message.messageId ?: "$title\n$body"
        if (!NotificationBadgeStore(this).recordPush(notificationId)) return
        NotificationRefreshSignal.send(this)
        NotificationChannels.create(this)
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DEEP_LINK, message.data["deep_link"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, NotificationChannels.MODRINTH_UPDATES)
            .setSmallIcon(R.drawable.ryntra_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        // Only a push the relay tied to a Modrinth notification id can be marked read. The
        // push does not say whether it is an invitation, so accepting stays in the app.
        message.data["notification_id"]?.takeIf(String::isNotBlank)?.let { modrinthId ->
            NotificationActionReceiver.addActions(this, builder, modrinthId, canAcceptInvitation = false)
        }
        val notification = builder.build()
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(notificationId.hashCode(), notification)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
