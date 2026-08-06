package com.ryntra.mobile.ui.dashboard.project.create

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditor
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.ProjectCreationRules
import com.ryntra.shared.model.ProjectFileUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateProjectDialog(
    loadMetadata: suspend () -> ProjectCreationMetadata,
    createProject: suspend (CreateProjectRequest) -> Result<Project>,
    onDismiss: () -> Unit,
    onCreated: (Project) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var metadata by remember { mutableStateOf<ProjectCreationMetadata?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var slugEdited by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(setOf<String>()) }
    var clientSide by remember { mutableStateOf("unknown") }
    var serverSide by remember { mutableStateOf("unknown") }
    var licenseId by remember { mutableStateOf("MIT") }
    var body by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var issuesUrl by remember { mutableStateOf("") }
    var wikiUrl by remember { mutableStateOf("") }
    var discordUrl by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf<ProjectFileUpload?>(null) }
    var editorMode by remember { mutableStateOf(MarkdownEditorMode.Write) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            icon = withContext(Dispatchers.IO) { context.projectIcon(uri) }
            if (icon == null) errorMessage = "Unable to read this image or it is larger than 256 KiB."
        }
    }

    LaunchedEffect(reloadKey) {
        loadError = null
        runCatching { loadMetadata() }.fold(
            onSuccess = { loaded ->
                metadata = loaded
                if (projectType.isBlank()) projectType = loaded.projectTypes.firstOrNull() ?: "mod"
            },
            onFailure = { loadError = it.message ?: "Unable to load Modrinth options." },
        )
    }

    val request = CreateProjectRequest(
        slug = slug, title = title, description = summary, body = body, projectType = projectType,
        categories = categories.toList(), clientSide = clientSide, serverSide = serverSide,
        licenseId = licenseId, sourceUrl = sourceUrl, issuesUrl = issuesUrl, wikiUrl = wikiUrl,
        discordUrl = discordUrl, icon = icon,
    )
    val errors = ProjectCreationRules.validate(request)
    val canAdvance = when (step) {
        0 -> title.isNotBlank() && slug.length >= ProjectCreationRules.SLUG_MIN_LENGTH && summary.isNotBlank() && projectType.isNotBlank()
        1 -> licenseId.isNotBlank()
        else -> errors.isEmpty()
    }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = RyntraDesign.colors.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.statusBarsPadding().imePadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) { Icon(Lucide.X, contentDescription = "Close") }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.project_create), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${step + 1} / 3", style = MaterialTheme.typography.labelSmall, color = RyntraDesign.colors.labelSecondary)
                    }
                }

                if (metadata == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(32.dp)) {
                        if (loadError == null) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.project_create_loading), modifier = Modifier.padding(top = 14.dp))
                        } else {
                            Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            RyntraSecondaryButton(stringResource(R.string.project_create_retry), Lucide.RefreshCw, onClick = { reloadKey++ })
                        }
                    }
                    return@Column
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
                ) {
                    item {
                        Text(stringResource(R.string.project_create_intro), color = RyntraDesign.colors.labelSecondary)
                        when (step) {
                            0 -> {
                                FormHeading(stringResource(R.string.project_create_identity))
                                FormField(stringResource(R.string.project_create_name), title, {
                                    title = it
                                    if (!slugEdited) slug = it.toSlug()
                                }, "My awesome project")
                                FormField(stringResource(R.string.project_create_slug), slug, {
                                    slugEdited = true; slug = it.lowercase().replace(' ', '-')
                                }, "my-awesome-project")
                                FormField(stringResource(R.string.project_create_summary), summary, { summary = it }, "A short, clear reason to install it")
                                FormHeading(stringResource(R.string.project_create_type))
                                ChoiceChips(metadata!!.projectTypes, projectType) { projectType = it; categories = emptySet() }
                                RyntraSecondaryButton(
                                    text = icon?.fileName ?: stringResource(R.string.project_create_add_icon),
                                    icon = Lucide.Image,
                                    onClick = { iconLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) },
                                )
                            }
                            1 -> {
                                FormHeading(stringResource(R.string.project_create_categories))
                                val available = metadata!!.categories.filter { it.projectType == projectType }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    available.forEach { category -> SelectChip(category.name, category.name in categories) {
                                        categories = if (category.name in categories) categories - category.name else categories + category.name
                                    } }
                                }
                                FormHeading(stringResource(R.string.project_create_client))
                                ChoiceChips(ProjectCreationRules.environmentValues, clientSide) { clientSide = it }
                                FormHeading(stringResource(R.string.project_create_server))
                                ChoiceChips(ProjectCreationRules.environmentValues, serverSide) { serverSide = it }
                                FormHeading(stringResource(R.string.project_create_license))
                                FormField("SPDX ID", licenseId, { licenseId = it }, "MIT")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    metadata!!.licenses.take(12).forEach { license -> SelectChip(license.id, licenseId == license.id) { licenseId = license.id } }
                                }
                            }
                            else -> {
                                FormHeading(stringResource(R.string.project_create_description))
                                MarkdownEditor(
                                    markdown = body, mode = editorMode,
                                    placeholder = "# About\n\nExplain features, installation and compatibility.",
                                    onMarkdownChange = { body = it }, onModeChange = { editorMode = it },
                                )
                                FormHeading(stringResource(R.string.project_create_links))
                                FormField("Source", sourceUrl, { sourceUrl = it }, "https://github.com/…")
                                FormField("Issues", issuesUrl, { issuesUrl = it }, "https://github.com/…/issues")
                                FormField("Wiki", wikiUrl, { wikiUrl = it }, "https://…")
                                FormField("Discord", discordUrl, { discordUrl = it }, "https://discord.gg/…")
                                Text(stringResource(R.string.project_create_private_note), color = RyntraDesign.colors.labelSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)) }
                        Spacer(Modifier.height(18.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    if (step > 0) RyntraSecondaryButton(
                        stringResource(R.string.project_create_back), Lucide.ArrowLeft,
                        onClick = { step-- }, modifier = Modifier.weight(0.42f), enabled = !isSubmitting,
                    )
                    RyntraPrimaryButton(
                        text = stringResource(if (step == 2) R.string.project_create_draft else R.string.project_create_next),
                        icon = if (step == 2) Lucide.Rocket else Lucide.ArrowRight,
                        enabled = canAdvance && !isSubmitting,
                        isLoading = isSubmitting,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            errorMessage = null
                            if (step < 2) step++ else scope.launch {
                                isSubmitting = true
                                createProject(request).fold(onSuccess = onCreated, onFailure = {
                                    errorMessage = it.message ?: "Unable to create project."
                                    isSubmitting = false
                                })
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable private fun FormHeading(text: String) = RyntraSectionLabel(text, Modifier.padding(top = 18.dp, bottom = 8.dp))

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = RyntraDesign.colors.labelSecondary)
    RyntraTextField(value, onChange, placeholder, Lucide.Hash, null, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ChoiceChips(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        values.forEach { SelectChip(it.replaceFirstChar(Char::uppercase), it == selected) { onSelect(it) } }
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) RyntraDesign.colors.accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) }
}

private fun String.toSlug(): String = lowercase().trim().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').take(64)

private fun android.content.Context.projectIcon(uri: Uri): ProjectFileUpload? {
    val bytes = contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(8192)
        val output = java.io.ByteArrayOutputStream()
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            if (output.size() > 256 * 1024) return null
        }
        output.toByteArray()
    } ?: return null
    var name = "project-icon.png"
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) name = cursor.getString(0)
    }
    return ProjectFileUpload(name, contentResolver.getType(uri) ?: "image/png", bytes)
}
