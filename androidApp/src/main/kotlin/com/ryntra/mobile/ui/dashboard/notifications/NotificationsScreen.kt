package com.ryntra.mobile.ui.dashboard.notifications

import com.ryntra.mobile.ui.components.RyntraChoiceGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.toShape
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.animateColorAsState
import android.content.Context
import android.text.format.DateUtils
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.ryntra.mobile.notifications.notificationText
import com.ryntra.mobile.ui.components.RyntraContentLoading
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModrinthNotificationKind
import java.time.Instant

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun NotificationsScreen(
    state: NotificationState,
    onRefresh: () -> Unit,
    onMarkRead: (List<String>) -> Unit,
    onAcceptInvitation: (ModrinthNotification) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var isArchiveVisible by rememberSaveable { mutableStateOf(false) }
    val unreadIds = state.items.filterNot(ModrinthNotification::read).map(ModrinthNotification::id)
    val visibleNotifications = state.items.filter { it.read == isArchiveVisible }
    val isPlatformNative = RyntraDesign.isPlatformNative
    val pullState = rememberPullToRefreshState()
    // The pull indicator answers the gesture only. The screen also refreshes on its own —
    // when it opens, when the app returns to the foreground, when a push arrives — and
    // tying the indicator to every one of those made it drop down unasked and look stuck.
    var isPullRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isPullRefresh = false }
    val isPullRefreshing = isPullRefresh && state.isLoading
    val isFirstLoad = state.isLoading && state.items.isEmpty()

    PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            isPullRefresh = true
            onRefresh()
        },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (isPlatformNative) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = isPullRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            } else {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isPullRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(if (isPlatformNative) 8.dp else 10.dp),
    ) {
        item(key = "notification-actions") {
            if (isPlatformNative) {
                PlatformNotificationHeader(
                    unreadCount = state.unreadCount,
                    isArchiveVisible = isArchiveVisible,
                    isLoading = state.isLoading,
                    canMarkAllRead = unreadIds.isNotEmpty(),
                    onShowArchive = { isArchiveVisible = it },
                    onRefresh = onRefresh,
                    onMarkAllRead = { onMarkRead(unreadIds) },
                )
                return@item
            }
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

        if (isFirstLoad) {
            item(key = "notification-loading") {
                RyntraContentLoading(
                    label = stringResource(R.string.notifications_loading),
                    modifier = Modifier.padding(top = 32.dp),
                )
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
                // Marking one read moves it to the archive; the rest close the gap
                // rather than jumping into it.
                NotificationRow(
                    modifier = Modifier.animateItem(),
                    notification = notification,
                    isActionLoading = state.activeActionNotificationId == notification.id,
                    isAnyActionLoading = state.activeActionNotificationId != null,
                    onAcceptInvitation = { onAcceptInvitation(notification) },
                    onClick = {
                        if (!notification.read) onMarkRead(listOf(notification.id))
                        notification.projectReference?.let(onOpenProject)
                            ?: uriHandler.openUri(notification.link.toModrinthUrl())
                    },
                )
            }
        }

        // With nothing listed, the empty state already says the load failed and offers a retry.
        state.errorMessage?.takeIf { state.items.isNotEmpty() }?.let { message ->
            item(key = "notification-error") {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
    }
}

@Composable
private fun NotificationRow(
    modifier: Modifier = Modifier,
    notification: ModrinthNotification,
    isActionLoading: Boolean,
    isAnyActionLoading: Boolean,
    onAcceptInvitation: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = RyntraDesign.colors
    val context = LocalContext.current
    val localizedText = context.notificationText(notification)
    val canAcceptInvitation = notification.actions.any { it.teamJoinId != null }
    val isPlatformNative = colors.isPlatformNative
    val scheme = MaterialTheme.colorScheme
    // Unread rows sit a tone higher than read ones instead of taking a translucent wash of
    // the accent, which Material has no role for.
    val container by animateColorAsState(
        targetValue = when {
            !isPlatformNative && notification.read -> colors.surface
            !isPlatformNative -> colors.accent.copy(alpha = 0.10f)
            notification.read -> scheme.surfaceContainer
            else -> scheme.surfaceContainerHighest
        },
        animationSpec = RyntraDesign.effectsSpec(),
        label = "Notification tone",
    )
    val badgeContainer = when {
        !isPlatformNative -> colors.accent.copy(alpha = 0.14f)
        notification.read -> scheme.secondaryContainer
        else -> scheme.primaryContainer
    }
    val badgeContent = when {
        !isPlatformNative -> colors.accent
        notification.read -> scheme.onSecondaryContainer
        else -> scheme.onPrimaryContainer
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Clipped before the click so the ripple stays inside the rounded card.
            .clip(RyntraDesign.contentShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(if (isPlatformNative) 16.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (isPlatformNative) 40.dp else 38.dp)
                .background(badgeContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notification.kind.icon,
                contentDescription = null,
                tint = badgeContent,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = localizedText.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (notification.read) FontWeight.Medium else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = localizedText.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notification.created.toLocalNotificationTime(context),
                style = MaterialTheme.typography.labelSmall,
                color = colors.labelSecondary.copy(alpha = 0.72f),
            )
            if (canAcceptInvitation && !notification.read) {
                Button(
                    onClick = onAcceptInvitation,
                    enabled = !isAnyActionLoading,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        stringResource(
                            if (isActionLoading) R.string.notifications_accepting_invitation
                            else R.string.notifications_accept_invitation,
                        ),
                    )
                }
            }
        }
        if (!notification.read) {
            Box(Modifier.padding(top = 5.dp).size(8.dp).background(colors.accent, CircleShape))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        val icon = when {
            errorMessage != null -> Lucide.CircleAlert
            isArchiveVisible -> Lucide.Archive
            else -> Lucide.Bell
        }
        if (RyntraDesign.isPlatformNative) {
            val scheme = MaterialTheme.colorScheme
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(if (errorMessage != null) scheme.errorContainer else scheme.secondaryContainer),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (errorMessage != null) scheme.onErrorContainer else scheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RyntraDesign.colors.labelSecondary.copy(alpha = 0.72f),
                modifier = Modifier.size(36.dp),
            )
        }
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

private fun String.toLocalNotificationTime(context: Context): String = runCatching {
    val timestamp = Instant.parse(this).toEpochMilli()
    val flags = DateUtils.FORMAT_SHOW_TIME or if (DateUtils.isToday(timestamp)) {
        0
    } else {
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
    DateUtils.formatDateTime(context, timestamp, flags)
}.getOrElse { substringBefore('T') }

private fun String.toModrinthUrl(): String = when {
    startsWith("https://") || startsWith("http://") -> this
    else -> "https://modrinth.com/${trimStart('/')}"
}

/**
 * Inbox and archive are a connected choice rather than an unlabeled icon that flips
 * meaning, and the two actions stay as labelled-by-description icon buttons — the
 * refresh button is also the accessible alternative to pulling the list.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun PlatformNotificationHeader(
    unreadCount: Int,
    isArchiveVisible: Boolean,
    isLoading: Boolean,
    canMarkAllRead: Boolean,
    onShowArchive: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notifications_unread_count, unreadCount),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.notifications_source_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(Lucide.RefreshCw, contentDescription = stringResource(R.string.notifications_refresh))
            }
            if (!isArchiveVisible) {
                IconButton(onClick = onMarkAllRead, enabled = canMarkAllRead) {
                    Icon(Lucide.CheckCheck, contentDescription = stringResource(R.string.notifications_mark_all_read))
                }
            }
        }
        RyntraChoiceGroup(
            options = listOf(false, true),
            isChecked = { it == isArchiveVisible },
            onToggle = onShowArchive,
            label = { showsArchive ->
                stringResource(if (showsArchive) R.string.notifications_archive else R.string.notifications_inbox)
            },
            icon = { showsArchive -> if (showsArchive) Lucide.Archive else Lucide.Inbox },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
