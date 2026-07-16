package com.ryntra.mobile.ui.dashboard.project.moderation

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.CircleCheckBig
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareReply
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownBlockView
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Account
import com.ryntra.shared.model.MarkdownBlock
import com.ryntra.shared.model.MarkdownParser
import com.ryntra.shared.model.ModerationMessage
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ModerationMessageCard(
    message: ModerationMessage,
    author: Account?,
    isOwnMessage: Boolean,
    isDeleting: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    when (message.body.type) {
        "status_change" -> ModerationTimelineEvent(
            icon = Lucide.CircleCheckBig,
            text = stringResource(
                R.string.moderation_status_changed,
                moderationStatusLabel(message.body.oldStatus),
                moderationStatusLabel(message.body.newStatus),
            ),
            created = message.created,
        )
        "thread_closure" -> ModerationTimelineEvent(
            icon = Lucide.ShieldCheck,
            text = stringResource(R.string.moderation_thread_closed),
            created = message.created,
        )
        "deleted" -> ModerationTimelineEvent(
            icon = Lucide.Trash2,
            text = stringResource(R.string.moderation_message_deleted),
            created = message.created,
        )
        else -> ModerationTextMessage(
            message = message,
            author = author,
            isOwnMessage = isOwnMessage,
            isDeleting = isDeleting,
            onReply = onReply,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun ModerationTimelineEvent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    created: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(RyntraDesign.colors.accent.copy(alpha = 0.14f)),
        ) {
            Icon(icon, contentDescription = null, tint = RyntraDesign.colors.accent, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = created.toLocalModerationTime(LocalContext.current),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ModerationTextMessage(
    message: ModerationMessage,
    author: Account?,
    isOwnMessage: Boolean,
    isDeleting: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable(message.id) { mutableStateOf(false) }
    val isModerator = author?.role in setOf("moderator", "admin")
    val authorName = author?.username ?: stringResource(R.string.moderation_system)
    val markdown = message.body.body.orEmpty()
    val blocks by produceState<List<MarkdownBlock>>(emptyList(), markdown) {
        value = withContext(Dispatchers.Default) { MarkdownParser.parse(markdown) }
    }

    Surface(
        color = if (isModerator) {
            RyntraDesign.colors.accent.copy(alpha = 0.08f)
        } else {
            RyntraDesign.colors.surface
        },
        shape = RoundedCornerShape(11.dp),
        border = androidx.compose.foundation.BorderStroke(0.75.dp, RyntraDesign.colors.separator),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModerationAvatar(author)
                Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(authorName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (isModerator) {
                            Icon(
                                Lucide.ShieldCheck,
                                contentDescription = stringResource(R.string.moderation_moderator),
                                tint = RyntraDesign.colors.accent,
                                modifier = Modifier.padding(start = 5.dp).size(15.dp),
                            )
                        }
                    }
                    Text(
                        text = message.created.toLocalModerationTime(LocalContext.current),
                        color = RyntraDesign.colors.labelSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (message.body.isPrivate) {
                    Text(
                        text = stringResource(R.string.moderation_private_message),
                        color = RyntraDesign.colors.warning,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                if (blocks.isEmpty()) {
                    Text(markdown, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    blocks.forEach { block -> MarkdownBlockView(block) }
                }
            }
            HorizontalDivider(color = RyntraDesign.colors.separator, modifier = Modifier.padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onReply) {
                    Icon(Lucide.MessageSquareReply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.moderation_reply), modifier = Modifier.padding(start = 6.dp))
                }
                Spacer(Modifier.weight(1f))
                if (isOwnMessage) {
                    IconButton(onClick = { confirmDelete = true }, enabled = !isDeleting) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Lucide.Trash2,
                                contentDescription = stringResource(R.string.moderation_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.moderation_delete_title)) },
            text = { Text(stringResource(R.string.moderation_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.moderation_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.moderation_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun ModerationAvatar(author: Account?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(author?.username?.take(1)?.uppercase() ?: "M", fontWeight = FontWeight.Bold)
        author?.avatarUrl?.let { avatarUrl ->
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun moderationStatusLabel(status: String?): String = when (status?.lowercase()) {
    "approved" -> stringResource(R.string.project_status_approved)
    "archived" -> stringResource(R.string.project_status_archived)
    "rejected" -> stringResource(R.string.project_status_rejected)
    "draft" -> stringResource(R.string.project_status_draft)
    "unlisted" -> stringResource(R.string.project_status_unlisted)
    "processing" -> stringResource(R.string.project_status_processing)
    "withheld" -> stringResource(R.string.project_status_withheld)
    "scheduled" -> stringResource(R.string.project_status_scheduled)
    "private" -> stringResource(R.string.project_status_private)
    else -> stringResource(R.string.project_status_unknown)
}

private fun String.toLocalModerationTime(context: Context): String = runCatching {
    val timestamp = Instant.parse(this).toEpochMilli()
    val flags = DateUtils.FORMAT_SHOW_TIME or if (DateUtils.isToday(timestamp)) {
        0
    } else {
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
    DateUtils.formatDateTime(context, timestamp, flags)
}.getOrElse { substringBefore('T') }
