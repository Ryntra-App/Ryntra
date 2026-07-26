package com.ryntra.mobile.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownBlockView
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.MarkdownBlock
import com.ryntra.shared.model.MarkdownParser
import com.ryntra.shared.updates.AppUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppUpdateDialog(
    update: AppUpdate,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val markdownBlocks by produceState<List<MarkdownBlock>>(emptyList(), update.notes) {
        value = withContext(Dispatchers.Default) { MarkdownParser.parse(update.notes) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RyntraDesign.contentShape,
            color = RyntraDesign.colors.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(top = 22.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "Update available",
                        color = RyntraDesign.colors.accent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = update.title,
                        color = RyntraDesign.colors.labelPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Ryntra ${update.version}",
                        color = RyntraDesign.colors.labelSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 360.dp)
                        .padding(horizontal = 22.dp),
                ) {
                    if (markdownBlocks.isEmpty()) {
                        Text(
                            text = "A new release is ready to download.",
                            color = RyntraDesign.colors.labelSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            itemsIndexed(markdownBlocks) { index, block ->
                                Box(modifier = Modifier.padding(bottom = if (index == markdownBlocks.lastIndex) 0.dp else 2.dp)) {
                                    MarkdownBlockView(block)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                    TextButton(onClick = onDownload) {
                        Text("Download")
                    }
                }
            }
        }
    }
}
