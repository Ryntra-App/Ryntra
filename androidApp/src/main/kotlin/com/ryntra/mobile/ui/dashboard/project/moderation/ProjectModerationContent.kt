package com.ryntra.mobile.ui.dashboard.project.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.X
import com.ryntra.mobile.ProjectModerationState
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditor
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.ModerationMessage
import com.ryntra.shared.model.ModerationThread
import com.ryntra.shared.model.Project

internal fun LazyListScope.moderationContentItems(
    project: Project,
    state: ProjectModerationState,
    currentUserId: String?,
    versionCount: Int,
    canSubmitProject: Boolean,
    isSubmittingProject: Boolean,
    submissionError: String?,
    replyingToMessageId: String?,
    onSubmitProject: () -> Unit,
    onReplyToMessage: (String?) -> Unit,
    onRefresh: () -> Unit,
    onSendReply: (String, String?) -> Unit,
    onDeleteMessage: (String) -> Unit,
) {
    item(key = "moderation-submission", contentType = "moderation-submission") {
        ProjectSubmissionStatus(
            project = project,
            versionCount = versionCount,
            canSubmit = canSubmitProject,
            isSubmitting = isSubmittingProject,
            errorMessage = submissionError,
            onSubmit = onSubmitProject,
        )
    }
    item(key = "moderation-header", contentType = "moderation-header") {
        ModerationHeader(isLoading = state.isLoading, onRefresh = onRefresh)
    }

    if (project.threadId.isNullOrBlank()) {
        item(key = "moderation-no-thread", contentType = "empty") {
            RyntraEmptyState(
                title = stringResource(R.string.moderation_no_thread),
                message = stringResource(R.string.moderation_no_thread_hint),
            )
        }
        return
    }

    if (state.isLoading && state.thread == null) {
        item(key = "moderation-loading", contentType = "loading") {
            Box(Modifier.fillMaxWidth().padding(vertical = 42.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        return
    }

    state.errorMessage?.let { error ->
        item(key = "moderation-error", contentType = "error") {
            ModerationError(
                message = if (state.requiresNewAuthorization) {
                    stringResource(R.string.moderation_access_required)
                } else {
                    error
                },
                onRetry = onRefresh,
            )
        }
    }

    val thread = state.thread ?: return
    if (thread.messages.isEmpty()) {
        item(key = "moderation-empty", contentType = "empty") {
            RyntraEmptyState(
                title = stringResource(R.string.moderation_empty),
                message = stringResource(R.string.moderation_empty_hint),
            )
        }
    } else {
        items(
            items = thread.messages.sortedBy(ModerationMessage::created),
            key = ModerationMessage::id,
            contentType = { message -> "moderation-${message.body.type}" },
        ) { message ->
            ModerationMessageCard(
                message = message,
                author = thread.authorOf(message),
                isOwnMessage = message.authorId == currentUserId,
                isDeleting = state.deletingMessageId == message.id,
                onReply = { onReplyToMessage(message.id) },
                onDelete = { onDeleteMessage(message.id) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (project.status.lowercase() !in setOf("draft", "private")) {
        item(key = "moderation-composer", contentType = "moderation-composer") {
            ModerationComposer(
                thread = thread,
                replyingToMessageId = replyingToMessageId,
                isSending = state.isSending,
                replyGeneration = state.replyGeneration,
                onCancelReply = { onReplyToMessage(null) },
                onSend = onSendReply,
            )
        }
    }
}

@Composable
private fun ModerationHeader(isLoading: Boolean, onRefresh: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.moderation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Lucide.RefreshCw,
                    contentDescription = stringResource(R.string.moderation_refresh),
                )
            }
        }
    }
}

@Composable
private fun ModerationError(message: String, onRetry: () -> Unit) {
    Surface(
        color = RyntraDesign.colors.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.42f),
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            RyntraSecondaryButton(
                text = stringResource(R.string.common_retry),
                icon = Lucide.RefreshCw,
                onClick = onRetry,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ModerationComposer(
    thread: ModerationThread,
    replyingToMessageId: String?,
    isSending: Boolean,
    replyGeneration: Int,
    onCancelReply: () -> Unit,
    onSend: (String, String?) -> Unit,
) {
    var draft by rememberSaveable(thread.id) { mutableStateOf("") }
    var mode by rememberSaveable(thread.id) { mutableStateOf(MarkdownEditorMode.Write) }
    LaunchedEffect(replyGeneration) {
        if (replyGeneration > 0) {
            draft = ""
            mode = MarkdownEditorMode.Write
            onCancelReply()
        }
    }
    val replyTarget = remember(thread, replyingToMessageId) {
        thread.messages.firstOrNull { it.id == replyingToMessageId }
    }
    val replyAuthor = replyTarget?.let(thread::authorOf)?.username
        ?: stringResource(R.string.moderation_moderator)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
        replyTarget?.let {
            Surface(
                color = RyntraDesign.colors.accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = stringResource(R.string.moderation_replying_to, replyAuthor),
                        color = RyntraDesign.colors.accent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onCancelReply) {
                        Icon(Lucide.X, contentDescription = stringResource(R.string.moderation_cancel_reply))
                    }
                }
            }
        }
        MarkdownEditor(
            markdown = draft,
            mode = mode,
            placeholder = stringResource(R.string.moderation_reply_placeholder),
            onMarkdownChange = { draft = it.take(10_000) },
            onModeChange = { mode = it },
            enabled = !isSending,
            minLines = 5,
        )
        RyntraPrimaryButton(
            text = stringResource(R.string.moderation_send),
            icon = Lucide.Send,
            onClick = { onSend(draft, replyingToMessageId) },
            enabled = draft.isNotBlank(),
            isLoading = isSending,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
