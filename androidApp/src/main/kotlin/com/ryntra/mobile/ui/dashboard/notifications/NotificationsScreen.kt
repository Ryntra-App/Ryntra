package com.ryntra.mobile.ui.dashboard.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Inbox
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.UserPlus
import com.ryntra.mobile.NotificationState
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraProgressIndicator
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModrinthNotificationKind

@Composable
fun NotificationsScreen(
    state: NotificationState,
    onRefresh: () -> Unit,
    onMarkRead: (List<String>) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var isArchiveVisible by rememberSaveable { mutableStateOf(false) }
    val unreadIds = state.items.filterNot(ModrinthNotification::read).map(ModrinthNotification::id)
    val visibleNotifications = state.items.filter { it.read == isArchiveVisible }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "notification-actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.notifications_unread_count, state.unreadCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.notifications_source_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = RyntraDesign.colors.labelSecondary,
                    )
                }
                Row {
                    IconButton(onClick = { isArchiveVisible = !isArchiveVisible }) {
                        Icon(
                            imageVector = if (isArchiveVisible) Lucide.Inbox else Lucide.Archive,
                            contentDescription = stringResource(
                                if (isArchiveVisible) R.string.notifications_show_inbox else R.string.notifications_show_archive,
                            ),
                            tint = if (isArchiveVisible) RyntraDesign.colors.accent else RyntraDesign.colors.labelPrimary,
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Icon(Lucide.RefreshCw, contentDescription = stringResource(R.string.notifications_refresh))
                    }
                    if (!isArchiveVisible) {
                        IconButton(onClick = { onMarkRead(unreadIds) }, enabled = unreadIds.isNotEmpty()) {
                            Icon(Lucide.CheckCheck, contentDescription = stringResource(R.string.notifications_mark_all_read))
                        }
                    }
                }
            }
        }

        if (state.isLoading && state.items.isEmpty()) {
            item(key = "notification-loading") {
                Box(Modifier.fillMaxWidth().padding(top = 72.dp), contentAlignment = Alignment.Center) {
                    RyntraProgressIndicator(
                        color = RyntraDesign.colors.accent,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        } else if (visibleNotifications.isEmpty()) {
            item(key = "notification-empty") {
                EmptyNotifications(
                    errorMessage = state.errorMessage,
                    isArchiveVisible = isArchiveVisible,
                    onRefresh = onRefresh,
                )
            }
        } else {
            items(visibleNotifications, key = ModrinthNotification::id, contentType = { "notification" }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        if (!notification.read) onMarkRead(listOf(notification.id))
                        uriHandler.openUri(notification.link.toModrinthUrl())
                    },
                )
            }
        }

        state.errorMessage?.let { message ->
            item(key = "notification-error") {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: ModrinthNotification, onClick: () -> Unit) {
    val colors = RyntraDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (notification.read) colors.surface else colors.accent.copy(alpha = 0.10f),
                shape = RyntraDesign.contentShape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colors.accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notification.kind.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = notification.title.removeMarkdownEmphasis(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (notification.read) FontWeight.Medium else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notification.text.removeMarkdownEmphasis(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notification.created.replace('T', ' ').substringBefore('.').removeSuffix("Z"),
                style = MaterialTheme.typography.labelSmall,
                color = colors.labelSecondary.copy(alpha = 0.72f),
            )
        }
        if (!notification.read) {
            Box(Modifier.padding(top = 5.dp).size(8.dp).background(colors.accent, CircleShape))
        }
    }
}

@Composable
private fun EmptyNotifications(
    errorMessage: String?,
    isArchiveVisible: Boolean,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = when {
                errorMessage != null -> Lucide.CircleAlert
                isArchiveVisible -> Lucide.Archive
                else -> Lucide.Bell
            },
            contentDescription = null,
            tint = RyntraDesign.colors.labelSecondary.copy(alpha = 0.72f),
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(
                when {
                    errorMessage != null -> R.string.notifications_load_failed
                    isArchiveVisible -> R.string.notifications_archive_empty
                    else -> R.string.notifications_empty
                },
            ),
            color = RyntraDesign.colors.labelSecondary,
        )
        if (errorMessage != null) {
            Button(onClick = onRefresh) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

private val ModrinthNotificationKind.icon
    get() = when (this) {
        ModrinthNotificationKind.ProjectUpdate -> Lucide.Rocket
        ModrinthNotificationKind.TeamInvite -> Lucide.UserPlus
        ModrinthNotificationKind.StatusChange -> Lucide.CheckCheck
        ModrinthNotificationKind.ModeratorMessage -> Lucide.MessageSquareText
        ModrinthNotificationKind.Unknown -> Lucide.Bell
    }

private fun String.removeMarkdownEmphasis(): String = replace("**", "").replace("__", "")

private fun String.toModrinthUrl(): String = when {
    startsWith("https://") || startsWith("http://") -> this
    else -> "https://modrinth.com/${trimStart('/')}"
}
