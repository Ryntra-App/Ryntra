package com.ryntra.mobile.ui.dashboard.project.create

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.network.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    loadMetadata: suspend () -> ProjectCreationMetadata,
    createProject: suspend (CreateProjectRequest) -> Result<Project>,
    onDismiss: () -> Unit,
    onCreated: (Project) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember { CreateProjectDraft() }
    val listState = rememberLazyListState()
    var metadata by remember { mutableStateOf<ProjectCreationMetadata?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var iconError by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    fun requestDismiss() {
        if (isSubmitting) return
        if (draft.isDirty) showDiscardConfirmation = true else onDismiss()
    }

    BackHandler(onBack = ::requestDismiss)

    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            draft.icon = withContext(Dispatchers.IO) { context.readProjectIcon(uri) }
            iconError = if (draft.icon == null) context.getString(R.string.project_create_icon_error) else null
        }
    }

    LaunchedEffect(reloadKey) {
        loadError = null
        runCatching { loadMetadata() }.fold(
            onSuccess = { loaded ->
                metadata = loaded
                if (draft.projectType.isBlank() || draft.projectType == HIDDEN_PROJECT_TYPE) {
                    draft.projectType = loaded.projectTypes.firstOrNull { it != HIDDEN_PROJECT_TYPE } ?: "mod"
                }
            },
            onFailure = { loadError = it.message ?: context.getString(R.string.project_create_load_error) },
        )
    }

    LaunchedEffect(draft.step) { listState.scrollToItem(0) }

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = RyntraDesign.colors.background, modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = RyntraDesign.colors.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.project_create), fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = ::requestDismiss, enabled = !isSubmitting) {
                                Icon(Lucide.X, contentDescription = stringResource(R.string.project_create_close))
                            }
                        }
                    )
                },
                bottomBar = {
                    if (metadata != null) {
                        CreateProjectBottomBar(
                            step = draft.step,
                            isSubmitting = isSubmitting,
                            onBack = { draft.step-- },
                            onContinue = {
                                submitError = null
                                if (!draft.isStepValid()) {
                                    draft.markValidationAttempted()
                                    submitError = context.getString(R.string.project_create_fix_required_fields)
                                    scope.launch { listState.animateScrollToItem(0) }
                                } else if (draft.step < CREATE_PROJECT_STEP_COUNT - 1) {
                                    draft.step++
                                } else {
                                    scope.launch {
                                        isSubmitting = true
                                        createProject(draft.toRequest()).fold(
                                            onSuccess = onCreated,
                                            onFailure = {
                                                submitError = context.projectCreationErrorMessage(it)
                                                isSubmitting = false
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                },
                modifier = Modifier.imePadding(),
            ) { contentPadding ->
                when {
                    metadata != null -> LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp)
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                            ) {
                                ProjectStepProgress(draft.step)
                                ProjectCreationStepHeader(draft.step)
                                submitError?.let { ProjectCreationError(it) }
                                when (draft.step) {
                                    0 -> ProjectBasicsStep(
                                        draft = draft,
                                        metadata = metadata!!,
                                        iconError = iconError,
                                        showValidationErrors = draft.shouldShowValidationErrors(0),
                                        onChooseIcon = {
                                            iconLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif"))
                                        },
                                    )
                                    1 -> ProjectCompatibilityStep(draft, metadata!!)
                                    else -> ProjectPageStep(
                                        draft = draft,
                                        showValidationErrors = draft.shouldShowValidationErrors(2),
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                            }
                        }
                    }
                    loadError != null -> ProjectCreationLoadError(loadError.orEmpty()) { reloadKey++ }
                    else -> ProjectCreationLoading()
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.project_create_discard_title)) },
            text = { Text(stringResource(R.string.project_create_discard_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.project_create_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text(stringResource(R.string.project_create_keep_editing)) }
            },
        )
    }
}

@Composable
private fun CreateProjectBottomBar(
    step: Int,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                if (step > 0) {
                    RyntraSecondaryButton(
                        text = stringResource(R.string.project_create_back),
                        icon = Lucide.ArrowLeft,
                        onClick = onBack,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(0.55f),
                    )
                }
                RyntraPrimaryButton(
                    text = stringResource(if (step == 2) R.string.project_create_draft else R.string.project_create_next),
                    icon = if (step == 2) Lucide.Rocket else Lucide.ArrowRight,
                    onClick = onContinue,
                    enabled = !isSubmitting,
                    isLoading = isSubmitting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProjectStepProgress(step: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(CREATE_PROJECT_STEP_COUNT) { index ->
            Surface(
                color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.weight(1f).height(4.dp),
                content = {},
            )
        }
    }
}

@Composable
private fun ProjectCreationLoading() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.project_create_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectCreationLoadError(message: String, onRetry: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.project_create_load_error), style = MaterialTheme.typography.titleMedium)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RyntraSecondaryButton(stringResource(R.string.project_create_retry), Lucide.RefreshCw, onRetry)
        }
    }
}

private const val CREATE_PROJECT_STEP_COUNT = 3
internal const val HIDDEN_PROJECT_TYPE = "minecraft_java_server"

private fun android.content.Context.projectCreationErrorMessage(error: Throwable): String {
    val apiError = error as? ApiException
    if (apiError != null) {
        Log.w(
            "RyntraProjectCreate",
            "Modrinth rejected project creation: status=${apiError.statusCode}, code=${apiError.errorCode}, description=${apiError.message}",
        )
    }
    val details = error.message.orEmpty().lowercase()
    return when {
        apiError?.statusCode == 401 || "401" in details || "token" in details && ("invalid" in details || "expired" in details) ->
            getString(R.string.project_create_error_session)
        apiError?.statusCode == 403 || "403" in details || "permission" in details -> getString(R.string.project_create_error_permission)
        apiError?.statusCode == 409 || "409" in details || "already exists" in details ||
            "slug" in details && ("taken" in details || "collision" in details) ->
            getString(R.string.project_create_error_slug_taken)
        apiError?.statusCode == 429 || "429" in details || "too many requests" in details -> getString(R.string.project_create_error_rate_limit)
        "initial_versions" in details || "parsing" in details || "serialization" in details || "json" in details ->
            getString(R.string.project_create_error_response)
        apiError != null && apiError.message.isNotBlank() ->
            getString(R.string.project_create_error_rejected, apiError.message)
        else -> getString(R.string.project_create_submit_error)
    }
}

private fun android.content.Context.readProjectIcon(uri: Uri): ProjectFileUpload? {
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
