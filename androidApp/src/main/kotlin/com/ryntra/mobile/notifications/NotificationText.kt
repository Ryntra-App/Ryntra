package com.ryntra.mobile.notifications

import android.content.Context
import com.ryntra.mobile.R
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModrinthNotificationKind

internal data class NotificationText(
    val title: String,
    val body: String,
)

internal fun Context.notificationText(notification: ModrinthNotification): NotificationText {
    val projectTitle = notification.projectTitle?.takeIf(String::isNotBlank)
    val versionTitle = notification.versionTitle?.takeIf(String::isNotBlank)
    return when (notification.kind) {
        ModrinthNotificationKind.ProjectUpdate -> NotificationText(
            title = getString(R.string.notification_content_project_update_title),
            body = when {
                projectTitle != null && versionTitle != null -> getString(
                    R.string.notification_content_project_update_version,
                    versionTitle,
                    projectTitle,
                )
                projectTitle != null -> getString(R.string.notification_content_project_update_project, projectTitle)
                else -> getString(R.string.notification_content_project_update_generic)
            },
        )
        ModrinthNotificationKind.TeamInvite -> NotificationText(
            title = getString(R.string.notification_content_team_invite_title),
            body = projectTitle?.let { getString(R.string.notification_content_team_invite_project, it) }
                ?: getString(R.string.notification_content_team_invite_body),
        )
        ModrinthNotificationKind.StatusChange -> NotificationText(
            title = getString(R.string.notification_content_status_title),
            body = projectTitle?.let { getString(R.string.notification_content_status_project, it) }
                ?: getString(R.string.notification_content_status_generic),
        )
        ModrinthNotificationKind.ModeratorMessage -> NotificationText(
            title = getString(R.string.notification_content_moderator_title),
            body = projectTitle?.let { getString(R.string.notification_content_moderator_project, it) }
                ?: getString(R.string.notification_content_moderator_generic),
        )
        ModrinthNotificationKind.Unknown -> NotificationText(
            title = notification.title.removeMarkdownEmphasis(),
            body = notification.text.removeMarkdownEmphasis(),
        )
    }
}

private fun String.removeMarkdownEmphasis(): String = replace("**", "").replace("__", "")
