package com.ryntra.mobile.ui.dashboard.project.edit

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ryntra.mobile.R
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.Images
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import com.ryntra.mobile.ProjectActionState
import com.ryntra.mobile.ProjectUpdateState
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.dashboard.project.gallery.ProjectGalleryManageSection
import com.ryntra.mobile.ui.dashboard.project.create.LicenseSelector
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditor
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.ProjectLicense
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectUploadLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
internal fun EditProjectContent(
    project: Project,
    draft: ProjectEditDraft,
    projectUpdate: ProjectUpdateState,
    actionState: ProjectActionState,
    onDraftChange: (ProjectEditDraft) -> Unit,
    onChangeIcon: (ProjectFileUpload) -> Unit,
    onDeleteIcon: () -> Unit,
    onAddGalleryImage: (ProjectFileUpload, Boolean, String, String) -> Unit,
    onDeleteGalleryImage: (String) -> Unit,
    onSetGalleryBanner: (String) -> Unit,
    onModifyGalleryImage: (url: String, title: String, description: String, ordering: Int?) -> Unit,
    loadProjectCreationMetadata: suspend () -> ProjectCreationMetadata,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bodyEditorMode by remember(project.id) { mutableStateOf(MarkdownEditorMode.Write) }
    var mainExpanded by rememberSaveable(project.id) { mutableStateOf(true) }
    var mediaExpanded by rememberSaveable(project.id) { mutableStateOf(false) }
    var publishingExpanded by rememberSaveable(project.id) { mutableStateOf(false) }
    var linksExpanded by rememberSaveable(project.id) { mutableStateOf(false) }
    var localUploadError by remember { mutableStateOf<String?>(null) }
    var licenses by remember(project.id) { mutableStateOf<List<ProjectLicense>>(emptyList()) }
    var isLoadingLicenses by remember(project.id) { mutableStateOf(true) }
    var licenseLoadError by remember(project.id) { mutableStateOf(false) }
    var licenseLoadGeneration by remember(project.id) { mutableStateOf(0) }
    var isReadingImage by remember { mutableStateOf(false) }
    val imageUnreadable = stringResource(R.string.project_edit_image_unreadable)
    val iconTooLarge = stringResource(R.string.project_edit_icon_too_large)
    LaunchedEffect(project.id, licenseLoadGeneration) {
        isLoadingLicenses = true
        licenseLoadError = false
        runCatching { loadProjectCreationMetadata().licenses }
            .onSuccess { licenses = it }
            .onFailure { licenseLoadError = true }
        isLoadingLicenses = false
    }
    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isReadingImage = true
        scope.launch {
            val upload = withContext(Dispatchers.IO) { context.readUpload(uri, "project-icon.png") }
            isReadingImage = false
            if (upload == null) localUploadError = imageUnreadable
            else if (upload.bytes.size > ProjectUploadLimits.PROJECT_ICON_BYTES) localUploadError = iconTooLarge
            else {
                localUploadError = null
                onChangeIcon(upload)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        EditSection(
            title = stringResource(R.string.project_edit_main),
            summary = stringResource(R.string.project_edit_main_hint),
            icon = Lucide.FileText,
            expanded = mainExpanded,
            onToggle = { mainExpanded = !mainExpanded },
        ) {
            EditField(
                stringResource(R.string.project_edit_title),
                draft.title,
                { onDraftChange(draft.copy(title = it)) },
                stringResource(R.string.project_edit_title_hint),
                Lucide.Hash,
            )
            EditField(
                stringResource(R.string.project_edit_summary),
                draft.summary,
                { onDraftChange(draft.copy(summary = it)) },
                stringResource(R.string.project_edit_summary_hint),
                Lucide.Info,
            )
            EditLabel(stringResource(R.string.project_edit_description_md))
            MarkdownEditor(
                markdown = draft.description,
                mode = bodyEditorMode,
                placeholder = stringResource(R.string.project_edit_description_hint),
                onMarkdownChange = { onDraftChange(draft.copy(description = it)) },
                onModeChange = { bodyEditorMode = it },
            )
        }

        EditSection(
            title = stringResource(R.string.project_edit_media),
            summary = stringResource(R.string.project_edit_media_hint),
            icon = Lucide.Images,
            expanded = mediaExpanded,
            onToggle = { mediaExpanded = !mediaExpanded },
        ) {
            EditLabel(stringResource(R.string.project_edit_icon))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(72.dp),
                ) {
                    if (project.iconUrl != null) {
                        AsyncImage(project.iconUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Icon(Lucide.Image, contentDescription = null) }
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    RyntraSecondaryButton(
                        text = if (isReadingImage) {
                            stringResource(R.string.project_edit_reading_image)
                        } else {
                            stringResource(R.string.project_edit_upload_icon)
                        },
                        icon = Lucide.Upload,
                        enabled = !isReadingImage && !actionState.isRunning,
                        onClick = { iconLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) },
                    )
                    if (project.iconUrl != null) {
                        Spacer(Modifier.height(8.dp))
                        RyntraSecondaryButton(
                            stringResource(R.string.project_edit_remove_icon),
                            Lucide.Trash2,
                            onDeleteIcon,
                            isDestructive = true,
                        )
                    }
                }
            }
            EditHeading(stringResource(R.string.project_edit_gallery))
            ProjectGalleryManageSection(
                gallery = project.gallery,
                isBusy = actionState.isRunning,
                actionState = actionState,
                onAdd = onAddGalleryImage,
                onDelete = onDeleteGalleryImage,
                onSetBanner = onSetGalleryBanner,
                onSaveMeta = onModifyGalleryImage,
            )
        }

        EditSection(
            title = stringResource(R.string.project_edit_status_license),
            summary = stringResource(R.string.project_edit_status_license_hint),
            icon = Lucide.Settings2,
            expanded = publishingExpanded,
            onToggle = { publishingExpanded = !publishingExpanded },
        ) {
            EditLabel(stringResource(R.string.project_edit_status))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(project.status, "draft", "unlisted", "archived").distinct().forEach { value ->
                    val statusLabel = when (value.lowercase()) {
                        "draft" -> stringResource(R.string.project_status_draft)
                        "unlisted" -> stringResource(R.string.project_status_unlisted)
                        "archived" -> stringResource(R.string.project_status_archived)
                        "approved" -> stringResource(R.string.project_status_approved)
                        "processing" -> stringResource(R.string.project_status_processing)
                        else -> value.replaceFirstChar(Char::uppercase)
                    }
                    Surface(
                        color = if (draft.status == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (draft.status == value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onDraftChange(draft.copy(status = value)) },
                    ) {
                        Text(statusLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            if (isLoadingLicenses) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.project_create_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LicenseSelector(
                    licenses = licenses,
                    selectedId = draft.licenseId,
                    onSelect = { onDraftChange(draft.copy(licenseId = it)) },
                )
            }
            if (licenseLoadError) {
                RyntraSecondaryButton(
                    text = stringResource(R.string.project_create_retry),
                    icon = Lucide.Settings2,
                    onClick = { licenseLoadGeneration += 1 },
                )
                Text(
                    stringResource(R.string.project_create_load_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        EditSection(
            title = stringResource(R.string.project_edit_links),
            summary = stringResource(R.string.project_edit_links_hint),
            icon = Lucide.Link,
            expanded = linksExpanded,
            onToggle = { linksExpanded = !linksExpanded },
        ) {
            EditField(stringResource(R.string.project_edit_source), draft.sourceUrl, { onDraftChange(draft.copy(sourceUrl = it)) }, "https://github.com/...", Lucide.Github)
            EditField(stringResource(R.string.project_edit_issues), draft.issuesUrl, { onDraftChange(draft.copy(issuesUrl = it)) }, "https://github.com/.../issues", Lucide.Link)
            EditField(stringResource(R.string.project_edit_wiki), draft.wikiUrl, { onDraftChange(draft.copy(wikiUrl = it)) }, "https://...", Lucide.Globe)
            EditField(stringResource(R.string.project_edit_discord), draft.discordUrl, { onDraftChange(draft.copy(discordUrl = it)) }, "https://discord.gg/...", Lucide.MessageSquareText)
        }

        (localUploadError ?: projectUpdate.errorMessage ?: actionState.errorMessage)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (actionState.isRunning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.project_edit_updating_assets), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun EditSection(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val motion = RyntraDesign.motion
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(motion.duration(140)),
        label = "Edit section",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    Lucide.ChevronDown,
                    contentDescription = stringResource(if (expanded) R.string.project_edit_collapse else R.string.project_edit_expand),
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(motion.duration(170))) +
                    fadeIn(animationSpec = tween(motion.duration(120))),
                exit = shrinkVertically(animationSpec = tween(motion.duration(120))) +
                    fadeOut(animationSpec = tween(motion.duration(80))),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp)) { content() }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    EditLabel(label)
    RyntraTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = icon,
        leadingIconDescription = null,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun EditHeading(title: String) {
    RyntraSectionLabel(title, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
}

@Composable
private fun EditLabel(label: String, modifier: Modifier = Modifier) {
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = modifier.padding(bottom = 6.dp))
}

private fun android.content.Context.readUpload(uri: android.net.Uri, fallbackName: String): ProjectFileUpload? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytesLimited(MAX_IMAGE_BYTES) } ?: return null
    var fileName = fallbackName
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) fileName = cursor.getString(0)
    }
    return ProjectFileUpload(fileName, contentResolver.getType(uri) ?: "image/png", bytes)
}

private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
