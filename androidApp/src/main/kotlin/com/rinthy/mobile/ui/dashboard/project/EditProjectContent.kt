package com.rinthy.mobile.ui.dashboard.project

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import com.rinthy.mobile.ProjectActionState
import com.rinthy.mobile.ProjectUpdateState
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.components.RinthyTextField
import com.rinthy.mobile.ui.components.RinthySectionLabel
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
internal fun EditProjectContent(
    project: Project,
    projectUpdate: ProjectUpdateState,
    actionState: ProjectActionState,
    onUpdate: (ProjectUpdate) -> Unit,
    onClearStatus: () -> Unit,
    onChangeIcon: (ProjectFileUpload) -> Unit,
    onDeleteIcon: () -> Unit,
    onAddGalleryImage: (ProjectFileUpload) -> Unit,
    onDeleteGalleryImage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(project.id, project.title) { mutableStateOf(project.title) }
    var description by remember(project.id, project.description) { mutableStateOf(project.description) }
    var body by remember(project.id, project.body) { mutableStateOf(project.body) }
    var sourceUrl by remember(project.id, project.sourceUrl) { mutableStateOf(project.sourceUrl.orEmpty()) }
    var issuesUrl by remember(project.id, project.issuesUrl) { mutableStateOf(project.issuesUrl.orEmpty()) }
    var wikiUrl by remember(project.id, project.wikiUrl) { mutableStateOf(project.wikiUrl.orEmpty()) }
    var discordUrl by remember(project.id, project.discordUrl) { mutableStateOf(project.discordUrl.orEmpty()) }
    var status by remember(project.id, project.status) { mutableStateOf(project.status) }
    var licenseId by remember(project.id, project.license?.id) { mutableStateOf(project.license?.id.orEmpty()) }
    var bodyEditorMode by remember(project.id) { mutableStateOf(MarkdownEditorMode.Write) }
    var localUploadError by remember { mutableStateOf<String?>(null) }
    var isReadingImage by remember { mutableStateOf(false) }
    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isReadingImage = true
        scope.launch {
            val upload = withContext(Dispatchers.IO) { context.readUpload(uri, "project-icon.png") }
            isReadingImage = false
            if (upload == null) localUploadError = "Unable to read that image."
            else if (upload.bytes.size > 256 * 1024) localUploadError = "Project icons must be 256 KiB or smaller."
            else {
                localUploadError = null
                onChangeIcon(upload)
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isReadingImage = true
        scope.launch {
            val upload = withContext(Dispatchers.IO) { context.readUpload(uri, "gallery-image.png") }
            isReadingImage = false
            if (upload == null) localUploadError = "Unable to read that image."
            else {
                localUploadError = null
                onAddGalleryImage(upload)
            }
        }
    }

    LaunchedEffect(projectUpdate.isSuccess) {
        if (projectUpdate.isSuccess) onClearStatus()
    }

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        EditHeading("Icon")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(72.dp),
            ) {
                if (project.iconUrl != null) {
                    AsyncImage(project.iconUrl, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center) { Icon(Lucide.Image, contentDescription = null) }
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                RinthySecondaryButton(
                    text = if (isReadingImage) "Reading image" else "Upload icon",
                    icon = Lucide.Upload,
                    enabled = !isReadingImage && !actionState.isRunning,
                    onClick = { iconLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) },
                )
                if (project.iconUrl != null) {
                    Spacer(Modifier.height(8.dp))
                    RinthySecondaryButton("Remove icon", Lucide.Trash2, onDeleteIcon, isDestructive = true)
                }
            }
        }

        EditHeading("Main information")
        EditField("Title", title, { title = it }, "Project title", Lucide.Hash)
        EditField("Summary", description, { description = it }, "Brief project summary", Lucide.Info)
        EditLabel("Description (Markdown)")
        MarkdownEditor(
            markdown = body,
            mode = bodyEditorMode,
            placeholder = "# About this project\n\nDescribe features, installation, and compatibility.",
            onMarkdownChange = { body = it },
            onModeChange = { bodyEditorMode = it },
        )

        EditHeading("Gallery")
        if (project.gallery.isNotEmpty()) {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                project.gallery.forEach { image ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1.45f).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(
                            model = image.url,
                            contentDescription = image.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        IconButton(
                            onClick = { onDeleteGalleryImage(image.url) },
                            modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(8.dp)),
                        ) {
                            Icon(Lucide.Trash2, contentDescription = "Delete gallery image", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        RinthySecondaryButton(
            text = "Add gallery image",
            icon = Lucide.Upload,
            enabled = !isReadingImage && !actionState.isRunning,
            onClick = { galleryLauncher.launch(arrayOf("image/*")) },
        )

        EditHeading("Status and license")
        EditLabel("Status")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(project.status, "draft", "unlisted", "archived").distinct().forEach { value ->
                Surface(
                    color = if (status == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.clickable { status = value },
                ) {
                    Text(value.replaceFirstChar(Char::uppercase), modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        EditField("License ID", licenseId, { licenseId = it }, "MIT", Lucide.FileText)

        EditHeading("Links")
        EditField("Source code", sourceUrl, { sourceUrl = it }, "https://github.com/...", Lucide.Github)
        EditField("Issue tracker", issuesUrl, { issuesUrl = it }, "https://github.com/.../issues", Lucide.Link)
        EditField("Wiki", wikiUrl, { wikiUrl = it }, "https://...", Lucide.Globe)
        EditField("Discord", discordUrl, { discordUrl = it }, "https://discord.gg/...", Lucide.MessageSquareText)

        (localUploadError ?: projectUpdate.errorMessage ?: actionState.errorMessage)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (actionState.isRunning) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text("Updating project assets", modifier = Modifier.padding(start = 8.dp))
            }
        }

        val hasChanges = title != project.title || description != project.description || body != project.body ||
            sourceUrl != project.sourceUrl.orEmpty() || issuesUrl != project.issuesUrl.orEmpty() ||
            wikiUrl != project.wikiUrl.orEmpty() || discordUrl != project.discordUrl.orEmpty() ||
            status != project.status || licenseId != project.license?.id.orEmpty()

        RinthyPrimaryButton(
            text = if (projectUpdate.isSuccess) "Saved" else "Save changes",
            icon = if (projectUpdate.isSuccess) Lucide.Check else Lucide.Save,
            enabled = hasChanges && !projectUpdate.isSaving && title.isNotBlank() && description.isNotBlank(),
            isLoading = projectUpdate.isSaving,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onUpdate(
                    ProjectUpdate(
                        title = title.takeIf { it != project.title },
                        description = description.takeIf { it != project.description },
                        body = body.takeIf { it != project.body },
                        sourceUrl = sourceUrl.takeIf { it != project.sourceUrl.orEmpty() },
                        issuesUrl = issuesUrl.takeIf { it != project.issuesUrl.orEmpty() },
                        wikiUrl = wikiUrl.takeIf { it != project.wikiUrl.orEmpty() },
                        discordUrl = discordUrl.takeIf { it != project.discordUrl.orEmpty() },
                        status = status.takeIf { it != project.status },
                        licenseId = licenseId.takeIf { it != project.license?.id.orEmpty() },
                    )
                )
            },
        )
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
    RinthyTextField(
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
    RinthySectionLabel(title, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
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
