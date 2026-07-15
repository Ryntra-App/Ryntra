package com.ryntra.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ryntra.mobile.R
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectAttentionKind
import com.ryntra.shared.model.ProjectAttentionState

@Composable
fun Project.statusLabel(): String = when (status.lowercase()) {
    "approved" -> stringResource(R.string.project_status_approved)
    "archived" -> stringResource(R.string.project_status_archived)
    "rejected" -> stringResource(R.string.project_status_rejected)
    "draft" -> stringResource(R.string.project_status_draft)
    "unlisted" -> stringResource(R.string.project_status_unlisted)
    "processing" -> stringResource(R.string.project_status_processing)
    "withheld" -> stringResource(R.string.project_status_withheld)
    "scheduled" -> stringResource(R.string.project_status_scheduled)
    "private" -> stringResource(R.string.project_status_private)
    "unknown" -> stringResource(R.string.project_status_unknown)
    else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
fun Project.attentionMessage(): String {
    val state = attentionState()
    val base = state.kind.attentionMessage()
    val note = state.moderatorNote
    return if (!note.isNullOrBlank()) {
        if (state.kind == ProjectAttentionKind.Rejected || state.kind == ProjectAttentionKind.Withheld) {
            "$base · $note"
        } else {
            // Moderator left feedback on another status — surface the note first.
            note
        }
    } else {
        base
    }
}

@Composable
fun ProjectAttentionKind.attentionMessage(): String = stringResource(
    when (this) {
        ProjectAttentionKind.ReviewForPublication -> R.string.attention_review_for_publication
        ProjectAttentionKind.InReview -> R.string.attention_in_review
        ProjectAttentionKind.Rejected -> R.string.attention_rejected
        ProjectAttentionKind.Withheld -> R.string.attention_withheld
        ProjectAttentionKind.Scheduled -> R.string.attention_scheduled
        ProjectAttentionKind.Draft -> R.string.attention_draft
        ProjectAttentionKind.Unlisted -> R.string.attention_unlisted
        ProjectAttentionKind.Private -> R.string.attention_private
        ProjectAttentionKind.Archived -> R.string.project_status_archived
        ProjectAttentionKind.Approved -> R.string.project_status_approved
        ProjectAttentionKind.Unknown -> R.string.attention_unknown
    },
)

@Composable
fun ProjectAttentionState.severityColor(): androidx.compose.ui.graphics.Color {
    val colors = com.ryntra.mobile.ui.theme.RyntraDesign.colors
    return when (kind) {
        ProjectAttentionKind.Rejected, ProjectAttentionKind.Withheld ->
            androidx.compose.material3.MaterialTheme.colorScheme.error
        ProjectAttentionKind.ReviewForPublication,
        ProjectAttentionKind.InReview,
        ProjectAttentionKind.Scheduled,
        ProjectAttentionKind.Draft,
        -> colors.warning
        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    }
}
