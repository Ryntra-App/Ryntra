package com.ryntra.mobile.ui.dashboard.project.sharecard

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectVersion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareCardStudio(
    project: Project,
    versions: List<ProjectVersion>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val scope = rememberCoroutineScope()
    var format by rememberSaveable(project.id) { mutableStateOf(ShareCardFormat.Square) }
    var template by rememberSaveable(project.id) { mutableStateOf(ShareCardTemplate.Release) }
    var paletteId by rememberSaveable(project.id) { mutableStateOf(shareCardPalettes.first().id) }
    var selectedVersionId by rememberSaveable(project.id) { mutableStateOf(versions.firstOrNull()?.id) }
    var headline by rememberSaveable(project.id) {
        mutableStateOf(defaultShareCardHeadline(context, ShareCardTemplate.Release, versions.firstOrNull()))
    }
    var description by rememberSaveable(project.id) {
        mutableStateOf(defaultShareCardDescription(project, ShareCardTemplate.Release, versions.firstOrNull()))
    }
    var isPreviewReady by remember { mutableStateOf(false) }
    var previewBounds by remember { mutableStateOf(Rect.Zero) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val selectedVersion = versions.firstOrNull { it.id == selectedVersionId } ?: versions.firstOrNull()
    val palette = shareCardPalettes.first { it.id == paletteId }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.share_card_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.share_card_subtitle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isExporting) {
                            Icon(Lucide.X, contentDescription = stringResource(R.string.share_card_close))
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                    ) {
                        exportError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Button(
                            enabled = isPreviewReady && !isExporting && headline.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    exportError = null
                                    try {
                                        val uri = context.createShareCardUri(
                                            bitmap = rootView.captureRegion(previewBounds.toIntRect()),
                                            projectSlug = project.slug ?: project.id,
                                        )
                                        context.openShareCardChooser(
                                            uri = uri,
                                            chooserTitle = context.getString(R.string.share_card_chooser),
                                        )
                                    } catch (error: Exception) {
                                        Log.e("ShareCardStudio", "Could not export share card", error)
                                        exportError = context.getString(R.string.share_card_error)
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Icon(Lucide.Share2, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = stringResource(
                                    if (isExporting) R.string.share_card_preparing else R.string.share_card_share,
                                ),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                item(key = "preview") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                previewBounds = it.boundsInRoot()
                                isPreviewReady = it.size.width > 0 && it.size.height > 0
                            },
                    ) {
                        ShareCardPreview(
                            project = project,
                            version = selectedVersion,
                            template = template,
                            format = format,
                            palette = palette,
                            headline = headline,
                            description = description,
                            modifier = Modifier.fillMaxWidth().then(Modifier.aspectRatio(format.ratio)),
                        )
                    }
                }
                item(key = "template") {
                    StudioChoiceSection(stringResource(R.string.share_card_template)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ShareCardTemplate.entries, key = ShareCardTemplate::name) { option ->
                                FilterChip(
                                    selected = template == option,
                                    onClick = {
                                        template = option
                                        headline = defaultShareCardHeadline(context, option, selectedVersion)
                                        description = defaultShareCardDescription(project, option, selectedVersion)
                                    },
                                    label = { Text(stringResource(option.labelRes)) },
                                )
                            }
                        }
                    }
                }
                item(key = "format") {
                    StudioChoiceSection(stringResource(R.string.share_card_format)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShareCardFormat.entries.forEach { option ->
                                FilterChip(
                                    selected = format == option,
                                    onClick = { format = option },
                                    label = { Text(stringResource(option.labelRes)) },
                                )
                            }
                        }
                    }
                }
                item(key = "style") {
                    StudioChoiceSection(stringResource(R.string.share_card_style)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            shareCardPalettes.forEach { option ->
                                PaletteChoice(
                                    palette = option,
                                    selected = option.id == paletteId,
                                    onClick = { paletteId = option.id },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                if (versions.isNotEmpty()) {
                    item(key = "version") {
                        StudioChoiceSection(stringResource(R.string.share_card_version)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(versions, key = ProjectVersion::id) { version ->
                                    FilterChip(
                                        selected = selectedVersion?.id == version.id,
                                        onClick = {
                                            selectedVersionId = version.id
                                            headline = defaultShareCardHeadline(context, template, version)
                                            description = defaultShareCardDescription(project, template, version)
                                        },
                                        label = {
                                            Text(
                                                version.versionNumber + version.loaders.firstOrNull()?.let {
                                                    " · ${it.replaceFirstChar(Char::uppercase)}"
                                                }.orEmpty(),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "headline") {
                    OutlinedTextField(
                        value = headline,
                        onValueChange = { if (it.length <= 90) headline = it },
                        label = { Text(stringResource(R.string.share_card_headline)) },
                        supportingText = { Text(stringResource(R.string.share_card_headline_hint, headline.length)) },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "description") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 240) description = it },
                        label = { Text(stringResource(R.string.share_card_description)) },
                        supportingText = {
                            Text(stringResource(R.string.share_card_description_hint, description.length))
                        },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioChoiceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun PaletteChoice(
    palette: ShareCardPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .padding(5.dp)
                    .background(palette.accent, CircleShape),
            )
            Text(stringResource(palette.labelRes), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

private fun defaultShareCardHeadline(
    context: Context,
    template: ShareCardTemplate,
    version: ProjectVersion?,
): String = when (template) {
    ShareCardTemplate.Release -> version?.versionNumber?.let {
        context.getString(R.string.share_card_default_release, it)
    } ?: context.getString(R.string.share_card_default_update)
    ShareCardTemplate.Milestone -> context.getString(R.string.share_card_default_milestone)
    ShareCardTemplate.Testers -> context.getString(R.string.share_card_default_testers)
}

private fun defaultShareCardDescription(
    project: Project,
    template: ShareCardTemplate,
    version: ProjectVersion?,
): String {
    val changelog = shareCardHighlights(version?.changelog.orEmpty())
    return if (template == ShareCardTemplate.Release && changelog.isNotEmpty()) {
        changelog.joinToString(separator = "\n") { "• $it" }
    } else {
        project.description.trim().take(240)
    }
}

private fun Rect.toIntRect(): IntRect = IntRect(
    left = left.toInt(),
    top = top.toInt(),
    right = right.toInt(),
    bottom = bottom.toInt(),
)
