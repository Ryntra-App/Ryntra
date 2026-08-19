package com.ryntra.mobile.ui.dashboard.project.disclosures

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BookText
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.ryntra.mobile.ProjectDisclosuresState
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.shared.model.ProjectDisclosure
import com.ryntra.shared.model.ProjectDisclosureDraft

/**
 * The Disclosures tab. Mirrors Modrinth's own settings page: one card per disclosure that applies
 * to this project type, with the rules that make a disclosure mandatory spelled out on each card.
 */
internal fun LazyListScope.disclosuresContentItems(
    state: ProjectDisclosuresState,
    draft: ProjectDisclosureDraft,
    canEdit: Boolean,
    versionCount: Int,
    onChange: (ProjectDisclosure) -> Unit,
    onRefresh: () -> Unit,
) {
    // Modrinth rejects disclosures on a project that has never published a file.
    val hasVersions = versionCount > 0
    val isEditable = canEdit && hasVersions

    item(key = "disclosures-header", contentType = "disclosures-header") {
        DisclosuresHeader(isLoading = state.isLoading, onRefresh = onRefresh)
    }

    if (!hasVersions) {
        item(key = "disclosures-versions", contentType = "disclosures-notice") {
            DisclosureNotice(
                title = stringResource(R.string.disclosures_versions_required),
                message = stringResource(R.string.disclosures_versions_required_hint),
            )
        }
    } else if (!canEdit) {
        item(key = "disclosures-permission", contentType = "disclosures-notice") {
            DisclosureNotice(
                title = stringResource(R.string.disclosures_no_permission),
                message = stringResource(R.string.disclosures_no_permission_hint),
            )
        }
    }

    state.errorMessage?.let { message ->
        item(key = "disclosures-error", contentType = "error") {
            DisclosuresError(
                message = if (state.requiresNewAuthorization) {
                    stringResource(R.string.disclosures_access_required)
                } else {
                    message
                },
                onRetry = onRefresh,
            )
        }
    }

    if (state.isLoading && !state.hasLoaded) {
        item(key = "disclosures-loading", contentType = "loading") {
            Box(Modifier.fillMaxWidth().padding(vertical = 42.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        return
    }

    if (draft.entries.isEmpty()) {
        item(key = "disclosures-empty", contentType = "empty") {
            RyntraEmptyState(
                title = stringResource(R.string.disclosures_none_applicable),
                message = stringResource(R.string.disclosures_none_applicable_hint),
            )
        }
        return
    }

    items(
        items = draft.entries,
        key = { it.type.apiValue },
        contentType = { "disclosure" },
    ) { entry ->
        DisclosureCard(
            entry = entry,
            canEdit = isEditable,
            onChange = onChange,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }

    val issues = draft.issues()
    if (issues.isNotEmpty()) {
        item(key = "disclosures-issues", contentType = "disclosures-issues") {
            DisclosureIssuesCard(messages = issues.map { it.message() })
        }
    }

    state.saveErrorMessage?.let { message ->
        item(key = "disclosures-save-error", contentType = "error") {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DisclosuresHeader(isLoading: Boolean, onRefresh: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.disclosures_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Lucide.RefreshCw,
                        contentDescription = stringResource(R.string.disclosures_refresh),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.disclosures_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        RyntraSecondaryButton(
            text = stringResource(R.string.disclosures_content_rules),
            icon = Lucide.BookText,
            onClick = { uriHandler.openUri(CONTENT_RULES_URL) },
        )
    }
}

@Composable
private fun DisclosureNotice(title: String, message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            RyntraIcon(
                icon = Lucide.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DisclosureIssuesCard(messages: List<String>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.disclosures_issues_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            messages.forEach { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DisclosuresError(message: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            RyntraSecondaryButton(
                text = stringResource(R.string.common_retry),
                icon = Lucide.RefreshCw,
                onClick = onRetry,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
