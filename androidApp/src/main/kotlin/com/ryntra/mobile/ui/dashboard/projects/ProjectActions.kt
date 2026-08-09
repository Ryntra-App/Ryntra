package com.ryntra.mobile.ui.dashboard.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R
import com.ryntra.shared.model.Project

internal fun Project.modrinthPageUrl(): String {
    val reference = slug?.takeIf(String::isNotBlank) ?: id
    return "https://modrinth.com/project/$reference"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectActionsSheet(
    project: Project,
    canDelete: Boolean = false,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                ProjectArtwork(project, Modifier.size(48.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        project.slug ?: project.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ProjectActionRow(
                icon = Lucide.FolderOpen,
                title = stringResource(R.string.project_action_open),
                description = stringResource(R.string.project_action_open_hint),
                onClick = onOpen,
            )
            ProjectActionRow(
                icon = Lucide.ExternalLink,
                title = stringResource(R.string.project_action_open_browser),
                description = stringResource(R.string.project_action_open_browser_hint),
                onClick = onOpenInBrowser,
            )
            if (canDelete) {
                ProjectActionRow(
                    icon = Lucide.Trash2,
                    title = stringResource(R.string.project_action_delete),
                    description = stringResource(R.string.project_action_delete_hint),
                    isDestructive = true,
                    onClick = onDeleteRequest,
                )
            }
        }
    }
}

@Composable
private fun ProjectActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(title, color = color, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun DeleteProjectDialog(
    project: Project,
    isDeleting: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var confirmation by remember(project.id) { mutableStateOf("") }
    val matches = confirmation.trim() == project.title
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.project_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.project_delete_warning, project.title))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(stringResource(R.string.project_delete_confirm_label)) },
                    supportingText = { Text(stringResource(R.string.project_delete_confirm_hint, project.title)) },
                    isError = confirmation.isNotEmpty() && !matches,
                    enabled = !isDeleting,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = matches && !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    if (isDeleting) stringResource(R.string.project_delete_deleting)
                    else stringResource(R.string.project_action_delete),
                    color = if (matches && !isDeleting) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.padding(start = if (isDeleting) 8.dp else 0.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.project_action_cancel))
            }
        },
    )
}
