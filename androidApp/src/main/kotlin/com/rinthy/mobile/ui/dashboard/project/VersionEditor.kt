package com.rinthy.mobile.ui.dashboard.project

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.CreateVersionRequest
import com.rinthy.shared.model.ProjectDependency
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.model.VersionUpdate
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        isReadingFiles = true
        scope.launch {
            val result = readVersionUploads(context, uris, files)
            result.onSuccess { selected ->
                files = selected
                primaryFileIndex = primaryFileIndex.coerceIn(0, files.lastIndex.coerceAtLeast(0))
                fileError = null
            }.onFailure { fileError = it.message ?: "Unable to read the selected files." }
            isReadingFiles = false
        }
    }

    val warnings = buildList {
        if (name.isBlank()) add("Add a release name")
        if (versionNumber.isBlank()) add("Add a version number")
        if (gameVersions.isEmpty()) add("Select at least one Minecraft version")
        if (loaders.isEmpty()) add("Select at least one loader")
        if (changelog.isBlank()) add("Describe the changes in this release")
        if (version == null && files.isEmpty()) add("Attach at least one version file")
    }
    val canSave = warnings.none { warning ->
        warning != "Describe the changes in this release"
    } && !isReadingFiles && !isSaving

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
                .background(RinthyDesign.colors.background)
                .statusBarsPadding(),
        ) {
            VersionEditorTopBar(version, onDismiss)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(key = "version-basics", contentType = "form") {
                    VersionEditorSection("Release information", "Name this build clearly for project users") {
                        VersionEditorField("Name", name, { name = it }, "Release name")
                        VersionEditorField(
                            "Version number",
                            versionNumber,
                            { versionNumber = it },
                            "1.0.0",
                            Modifier.padding(top = 14.dp),
                        )
                    }
                }
                item(key = "version-channel", contentType = "choices") {
                    VersionEditorSection("Release channel", "Choose how stable this build is") {
                        ReleaseChannelPicker(versionType) { versionType = it }
                    }
                }
                item(key = "version-game", contentType = "choices") {
                    VersionEditorSection("Minecraft versions", "Tap to select; add a version if it is not listed") {
                        ValueChoices(
                            values = suggestedGameVersions,
                            selected = gameVersions,
                            customValue = gameVersionInput,
                            customPlaceholder = "For example 1.21.5",
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
                    VersionEditorSection("Loaders", "Select every platform supported by this file") {
                        ValueChoices(
                            values = suggestedLoaders,
                            selected = loaders,
                            customValue = loaderInput,
                            customPlaceholder = "For example fabric",
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
                    VersionEditorSection("Dependencies", "Tap a dependency type to cycle required, optional, incompatible, and embedded") {
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
                    VersionEditorSection("Changelog", "Write GitHub Flavored Markdown and verify it before publishing") {
                        MarkdownEditor(
                            markdown = changelog,
                            mode = changelogMode,
                            placeholder = "## Changes\n- Added ...\n- Fixed ...",
                            onMarkdownChange = { changelog = it },
                            onModeChange = { changelogMode = it },
                        )
                    }
                }
                if (version == null) {
                    item(key = "version-files", contentType = "files") {
                        VersionEditorSection("Files", "Add one or more builds and select the primary download") {
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
                    VersionEditorSection("Visibility") {
                        FeaturedToggle(featured) { featured = !featured }
                    }
                }
                item(key = "version-checklist", contentType = "checklist") {
                    VersionEditorSection("Release checklist") {
                        ReleaseChecklist(warnings)
                        (fileError ?: errorMessage)?.let { message ->
                            Text(
                                message,
                                color = RinthyDesign.colors.destructive,
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
