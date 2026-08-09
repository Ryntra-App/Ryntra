package com.ryntra.mobile.ui.dashboard.project.versions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.Hash
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditor
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditorMode
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import kotlinx.coroutines.launch

@Composable
internal fun VersionEditorDialog(
    version: ProjectVersion?,
    suggestedGameVersions: List<String>,
    suggestedLoaders: List<String>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (CreateVersionRequest) -> Unit,
    onUpdate: (String, VersionUpdate) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(version?.id) { mutableStateOf(version?.name.orEmpty()) }
    var versionNumber by remember(version?.id) { mutableStateOf(version?.versionNumber.orEmpty()) }
    var changelog by remember(version?.id) { mutableStateOf(version?.changelog.orEmpty()) }
    var changelogMode by remember(version?.id) { mutableStateOf(MarkdownEditorMode.Write) }
    var versionType by remember(version?.id) { mutableStateOf(version?.versionType ?: "release") }
    var gameVersions by remember(version?.id) { mutableStateOf(version?.gameVersions.orEmpty()) }
    var loaders by remember(version?.id) { mutableStateOf(version?.loaders.orEmpty()) }
    var gameVersionInput by remember { mutableStateOf("") }
    var loaderInput by remember { mutableStateOf("") }
    var dependencies by remember(version?.id) { mutableStateOf(version?.dependencies.orEmpty()) }
    var dependencyInput by remember { mutableStateOf("") }
    var featured by remember(version?.id) { mutableStateOf(version?.featured ?: false) }
    var files by remember(version?.id) { mutableStateOf<List<ProjectFileUpload>>(emptyList()) }
    var primaryFileIndex by remember(version?.id) { mutableIntStateOf(0) }
    var isReadingFiles by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val fileReadError = stringResource(R.string.version_editor_file_read_error)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        isReadingFiles = true
        scope.launch {
            val result = readVersionUploads(context, uris, files)
            result.onSuccess { selected ->
                files = selected
                primaryFileIndex = primaryFileIndex.coerceIn(0, files.lastIndex.coerceAtLeast(0))
                fileError = null
            }.onFailure { fileError = fileReadError }
            isReadingFiles = false
        }
    }

    val warnings = buildList {
        if (name.isBlank()) add(stringResource(R.string.version_editor_warning_name))
        if (versionNumber.isBlank()) add(stringResource(R.string.version_editor_warning_number))
        if (gameVersions.isEmpty()) add(stringResource(R.string.version_editor_warning_game_version))
        if (loaders.isEmpty()) add(stringResource(R.string.version_editor_warning_loader))
        if (changelog.isBlank()) add(stringResource(R.string.version_editor_warning_changelog))
        if (version == null && files.isEmpty()) add(stringResource(R.string.version_editor_warning_file))
    }
    val canSave = name.isNotBlank() && versionNumber.isNotBlank() && gameVersions.isNotEmpty() &&
        loaders.isNotEmpty() && (version != null || files.isNotEmpty()) && !isReadingFiles && !isSaving

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RyntraDesign.colors.background)
                .statusBarsPadding(),
        ) {
            VersionEditorTopBar(version, onDismiss)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(key = "version-basics", contentType = "form") {
                    VersionEditorSection(
                        stringResource(R.string.version_editor_release_info),
                        stringResource(R.string.version_editor_release_info_hint),
                    ) {
                        VersionEditorField(
                            stringResource(R.string.version_editor_name),
                            name,
                            { name = it },
                            stringResource(R.string.version_editor_name_hint),
                        )
                        VersionEditorField(
                            stringResource(R.string.version_editor_number),
                            versionNumber,
                            { versionNumber = it },
                            "1.0.0",
                            Modifier.padding(top = 14.dp),
                            leadingIcon = com.composables.icons.lucide.Lucide.Hash,
                        )
                    }
                }
                item(key = "version-channel", contentType = "choices") {
                    VersionEditorSection(stringResource(R.string.version_editor_channel), stringResource(R.string.version_editor_channel_hint)) {
                        ReleaseChannelPicker(versionType) { versionType = it }
                    }
                }
                item(key = "version-game", contentType = "choices") {
                    VersionEditorSection(stringResource(R.string.version_editor_game_versions), stringResource(R.string.version_editor_game_versions_hint)) {
                        ValueChoices(
                            values = suggestedGameVersions,
                            selected = gameVersions,
                            customValue = gameVersionInput,
                            customPlaceholder = stringResource(R.string.version_editor_game_version_example),
                            onCustomValueChange = { gameVersionInput = it },
                            onToggle = { gameVersions = gameVersions.toggle(it) },
                            onAddCustom = {
                                val value = gameVersionInput.trim()
                                if (value.isNotEmpty()) {
                                    gameVersions = (gameVersions + value).distinct()
                                    gameVersionInput = ""
                                }
                            },
                        )
                    }
                }
                item(key = "version-loaders", contentType = "choices") {
                    VersionEditorSection(stringResource(R.string.version_editor_loaders), stringResource(R.string.version_editor_loaders_hint)) {
                        ValueChoices(
                            values = suggestedLoaders,
                            selected = loaders,
                            customValue = loaderInput,
                            customPlaceholder = stringResource(R.string.version_editor_loader_example),
                            onCustomValueChange = { loaderInput = it },
                            onToggle = { loaders = loaders.toggle(it) },
                            onAddCustom = {
                                val value = loaderInput.trim().lowercase()
                                if (value.isNotEmpty()) {
                                    loaders = (loaders + value).distinct()
                                    loaderInput = ""
                                }
                            },
                        )
                    }
                }
                item(key = "version-dependencies", contentType = "dependencies") {
                    VersionEditorSection(stringResource(R.string.version_editor_dependencies), stringResource(R.string.version_editor_dependencies_hint)) {
                        DependencyEditor(
                            dependencies = dependencies,
                            input = dependencyInput,
                            onInputChange = { dependencyInput = it },
                            onAdd = {
                                val projectId = dependencyInput.trim()
                                if (projectId.isNotEmpty() && dependencies.none { it.projectId == projectId }) {
                                    dependencies = dependencies + ProjectDependency(projectId = projectId)
                                    dependencyInput = ""
                                }
                            },
                            onChangeType = { index ->
                                dependencies = dependencies.mapIndexed { dependencyIndex, dependency ->
                                    if (dependencyIndex == index) dependency.copy(dependencyType = dependency.dependencyType.nextDependencyType()) else dependency
                                }
                            },
                            onRemove = { index -> dependencies = dependencies.filterIndexed { dependencyIndex, _ -> dependencyIndex != index } },
                        )
                    }
                }
                item(key = "version-changelog", contentType = "markdown") {
                    VersionEditorSection(stringResource(R.string.version_editor_changelog), stringResource(R.string.version_editor_changelog_hint)) {
                        MarkdownEditor(
                            markdown = changelog,
                            mode = changelogMode,
                            placeholder = stringResource(R.string.version_editor_changelog_placeholder),
                            onMarkdownChange = { changelog = it },
                            onModeChange = { changelogMode = it },
                        )
                    }
                }
                if (version == null) {
                    item(key = "version-files", contentType = "files") {
                        VersionEditorSection(stringResource(R.string.version_editor_files), stringResource(R.string.version_editor_files_hint)) {
                            VersionFilesEditor(
                                files = files,
                                primaryIndex = primaryFileIndex,
                                isReading = isReadingFiles,
                                onChoose = {
                                    launcher.launch(
                                        arrayOf(
                                            "application/java-archive",
                                            "application/zip",
                                            "application/octet-stream",
                                        ),
                                    )
                                },
                                onSelectPrimary = { primaryFileIndex = it },
                                onRemove = { index ->
                                    files = files.filterIndexed { fileIndex, _ -> fileIndex != index }
                                    primaryFileIndex = primaryFileIndex.coerceIn(0, files.lastIndex.coerceAtLeast(0))
                                },
                            )
                        }
                    }
                }
                item(key = "version-featured", contentType = "toggle") {
                    VersionEditorSection(stringResource(R.string.version_editor_visibility)) {
                        FeaturedToggle(featured) { featured = !featured }
                    }
                }
                item(key = "version-checklist", contentType = "checklist") {
                    VersionEditorSection(stringResource(R.string.version_editor_checklist)) {
                        ReleaseChecklist(warnings)
                        (fileError ?: errorMessage)?.let { message ->
                            Text(
                                message,
                                color = RyntraDesign.colors.destructive,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
            VersionEditorActions(
                isCreating = version == null,
                canSave = canSave,
                isSaving = isSaving,
                onDismiss = onDismiss,
                onSave = {
                    if (version == null) {
                        onCreate(
                            CreateVersionRequest(
                                name = name.trim(),
                                versionNumber = versionNumber.trim(),
                                changelog = changelog,
                                dependencies = dependencies,
                                gameVersions = gameVersions,
                                versionType = versionType,
                                loaders = loaders,
                                featured = featured,
                                files = files,
                                primaryFileIndex = primaryFileIndex,
                            ),
                        )
                    } else {
                        onUpdate(
                            version.id,
                            VersionUpdate(
                                name = name.trim(),
                                versionNumber = versionNumber.trim(),
                                changelog = changelog,
                                dependencies = dependencies,
                                gameVersions = gameVersions,
                                versionType = versionType,
                                loaders = loaders,
                                featured = featured,
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun List<String>.toggle(value: String): List<String> =
    if (value in this) filterNot { it == value } else this + value

private fun String.nextDependencyType(): String {
    val types = listOf("required", "optional", "incompatible", "embedded")
    return types[(types.indexOf(this).takeIf { it >= 0 } ?: 0).plus(1) % types.size]
}
