package com.ryntra.mobile.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ryntra.mobile.MainActivity
import com.ryntra.mobile.R
import com.ryntra.mobile.preferences.AppLocale
import com.ryntra.mobile.security.SecureTokenStore
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.network.NotificationPollingClient
import com.ryntra.shared.network.ApiException
import kotlinx.coroutines.CancellationException

class NotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val token = SecureTokenStore(applicationContext).read() ?: return Result.success()
        val client = NotificationPollingClient()
        return try {
            val notifications = client.load(token)
            presentNewNotifications(notifications)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ApiException) {
            if (error.statusCode == 401 || error.statusCode == 403) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            client.close()
        }
    }

    private fun presentNewNotifications(notifications: List<ModrinthNotification>) {
        val store = LocalNotificationStore(applicationContext)
        val currentIds = notifications.map(ModrinthNotification::id)
        if (!store.hasCompletedInitialSync()) {
            store.updateKnownIds(currentIds)
            return
        }

        val knownIds = store.knownIds()
        val newUnread = notifications.filter { !it.read && it.id !in knownIds }
        val badgeStore = NotificationBadgeStore(applicationContext)
        val unseen = newUnread.filter { badgeStore.recordPush(it.id) }
        if (canPostNotifications()) unseen.take(MAX_PER_RUN).forEach(::showNotification)
        if (unseen.isNotEmpty()) NotificationRefreshSignal.send(applicationContext)
        store.updateKnownIds((currentIds + knownIds).distinct())
    }

    private fun showNotification(notification: ModrinthNotification) {
        val localizedContext = AppLocale.wrap(applicationContext)
        val text = localizedContext.notificationText(notification)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DEEP_LINK, notification.link)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val built = NotificationCompat.Builder(applicationContext, NotificationChannels.MODRINTH_UPDATES)
            .setSmallIcon(R.drawable.ryntra_launcher_monochrome)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        if (!canPostNotifications()) return
        try {
            NotificationManagerCompat.from(applicationContext).notify(notification.id.hashCode(), built)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and the framework call.
        }
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    private companion object {
        const val MAX_PER_RUN = 5
    }
}
