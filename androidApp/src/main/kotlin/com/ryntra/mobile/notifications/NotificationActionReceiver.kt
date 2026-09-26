package com.ryntra.mobile.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ryntra.mobile.R
import com.ryntra.mobile.preferences.AppLocale
import com.ryntra.mobile.security.SecureTokenStore
import com.ryntra.shared.network.NotificationPollingClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the buttons on a posted Modrinth notification without opening the app: mark it read,
 * or accept a team invitation. The system notification goes away only once Modrinth confirms.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = Action.entries.firstOrNull { it.intentAction == intent.action } ?: return
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID)?.takeIf(String::isNotBlank) ?: return
        val systemId = intent.getIntExtra(EXTRA_SYSTEM_ID, notificationId.hashCode())
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val succeeded = perform(appContext, action, notificationId)
                withContext(Dispatchers.Main) {
                    if (succeeded) {
                        NotificationManagerCompat.from(appContext).cancel(systemId)
                        NotificationRefreshSignal.send(appContext)
                    } else {
                        Toast.makeText(
                            AppLocale.wrap(appContext),
                            R.string.notification_action_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun perform(context: Context, action: Action, notificationId: String): Boolean {
        val token = SecureTokenStore(context).read() ?: return false
        val client = NotificationPollingClient()
        return try {
            when (action) {
                Action.MarkRead -> client.markRead(notificationId, token)
                Action.AcceptInvitation -> client.acceptInvitation(notificationId, token)
            }
            NotificationBadgeStore(context).recordPush(notificationId)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        } finally {
            client.close()
        }
    }

    internal enum class Action(val intentAction: String) {
        MarkRead("com.ryntra.mobile.notifications.MARK_READ"),
        AcceptInvitation("com.ryntra.mobile.notifications.ACCEPT_INVITATION"),
    }

    internal companion object {
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_SYSTEM_ID = "system_id"

        /** Adds the buttons a Modrinth notification supports to [builder]. */
        fun addActions(
            context: Context,
            builder: NotificationCompat.Builder,
            notificationId: String,
            canAcceptInvitation: Boolean,
        ): NotificationCompat.Builder {
            val localized = AppLocale.wrap(context)
            if (canAcceptInvitation) {
                builder.addAction(
                    0,
                    localized.getString(R.string.notification_action_accept),
                    actionIntent(context, Action.AcceptInvitation, notificationId),
                )
            }
            return builder.addAction(
                0,
                localized.getString(R.string.notification_action_mark_read),
                actionIntent(context, Action.MarkRead, notificationId),
            )
        }

        private fun actionIntent(context: Context, action: Action, notificationId: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java)
                .setAction(action.intentAction)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_SYSTEM_ID, notificationId.hashCode())
            return PendingIntent.getBroadcast(
                context,
                // Distinct per notification and per button, or one would overwrite the other.
                31 * notificationId.hashCode() + action.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
