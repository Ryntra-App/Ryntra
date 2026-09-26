package com.ryntra.mobile.ui.dashboard.project.versions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.components.formatProjectDate
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownBlockView
import com.ryntra.shared.model.MarkdownParser
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.ProjectVersionFile

/**
 * Everything Modrinth records about one release, in one place.
 *
 * The list card can only afford a two-block changelog preview and no file detail at
 * all, which left the full changelog unreachable for anyone without edit rights, and
 * the artifact hashes unreachable for everyone.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun VersionDetailSheet(
    version: ProjectVersion,
    dependencies: List<ProjectDependency>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val changelogBlocks = remember(version.changelog) {
        if (version.changelog.isBlank()) emptyList() else MarkdownParser.parse(version.changelog)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = version.versionNumber,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = version.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                SuggestionChip(onClick = {}, label = { Text(version.versionType.replaceFirstChar(Char::uppercase)) })
                if (version.featured) {
                    SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.version_detail_featured)) })
                }
                if (version.status.isNotBlank() && version.status != "listed") {
                    SuggestionChip(onClick = {}, label = { Text(version.status.replaceFirstChar(Char::uppercase)) })
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(
                    Lucide.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = formatExactCount(version.downloads),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp),
                )
                formatProjectDate(version.datePublished)?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }

            if (version.gameVersions.isNotEmpty() || version.loaders.isNotEmpty()) {
                SheetSection(stringResource(R.string.version_detail_compatibility))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    version.loaders.forEach { loader ->
                        SuggestionChip(onClick = {}, label = { Text(loader.replaceFirstChar(Char::uppercase)) })
                    }
                    version.gameVersions.forEach { gameVersion ->
                        SuggestionChip(onClick = {}, label = { Text(gameVersion) })
                    }
                }
            }

            SheetSection(stringResource(R.string.version_detail_changelog))
            if (changelogBlocks.isEmpty()) {
                Text(
                    text = stringResource(R.string.version_detail_changelog_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    changelogBlocks.forEach { block -> MarkdownBlockView(block) }
                }
            }

            if (version.files.isNotEmpty()) {
                SheetSection(stringResource(R.string.version_detail_files))
                version.files.forEach { file ->
                    VersionFileRow(
                        file = file,
                        onDownload = { uriHandler.openUri(file.url) },
                        onCopyHash = { label, value -> context.copyToClipboard(label, value) },
                    )
                }
            }

            if (dependencies.isNotEmpty()) {
                SheetSection(stringResource(R.string.version_detail_dependencies))
                dependencies.forEach { dependency ->
                    Text(
                        text = dependency.title
                            ?: dependency.fileName
                            ?: dependency.projectId
                            ?: dependency.versionId.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSection(title: String) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(top = 18.dp),
    )
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@Composable
private fun VersionFileRow(
    file: ProjectVersionFile,
    onDownload: () -> Unit,
    onCopyHash: (String, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(file.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDownload) {
                Icon(Lucide.Download, contentDescription = stringResource(R.string.version_detail_download))
            }
        }
        // Hashes are how a build is verified against what was published, so they are
        // offered as one-tap copies rather than text nobody can select on a phone.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            file.hashes.forEach { (algorithm, value) ->
                AssistChip(
                    onClick = { onCopyHash(algorithm.uppercase(), value) },
                    label = { Text(algorithm.uppercase()) },
                    leadingIcon = { Icon(Lucide.Copy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${bytes} ${units[unit]}" else "${"%.1f".format(value)} ${units[unit]}"
}

private fun Context.copyToClipboard(label: String, value: String) {
    getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(label, value))
}
