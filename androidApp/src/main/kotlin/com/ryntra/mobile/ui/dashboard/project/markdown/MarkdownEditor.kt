package com.ryntra.mobile.ui.dashboard.project.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.MarkdownBlock
import com.ryntra.shared.model.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal enum class MarkdownEditorMode { Write, Preview }

@Composable
internal fun MarkdownEditor(
    markdown: String,
    mode: MarkdownEditorMode,
    placeholder: String,
    onMarkdownChange: (String) -> Unit,
    onModeChange: (MarkdownEditorMode) -> Unit,
    enabled: Boolean = true,
    minLines: Int = 9,
    isError: Boolean = false,
) {
    EditorModePicker(mode, onModeChange)
    if (mode == MarkdownEditorMode.Write) {
        RyntraTextField(
            value = markdown,
            onValueChange = onMarkdownChange,
            placeholder = placeholder,
            leadingIcon = Lucide.FileText,
            leadingIconDescription = null,
            singleLine = false,
            minLines = minLines,
            maxLines = Int.MAX_VALUE,
            enabled = enabled,
            isError = isError,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    } else {
        MarkdownDraftPreview(markdown, Modifier.padding(top = 10.dp), isError)
    }
}

@Composable
private fun EditorModePicker(selected: MarkdownEditorMode, onSelect: (MarkdownEditorMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surface, RoundedCornerShape(9.dp))
            .border(0.75.dp, RyntraDesign.colors.separator, RoundedCornerShape(9.dp))
            .padding(3.dp),
    ) {
        MarkdownEditorMode.entries.forEach { mode ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        if (selected == mode) RyntraDesign.colors.surfaceRaised else Color.Transparent,
                        RoundedCornerShape(7.dp),
                    )
                    .clickable(role = Role.Tab) { onSelect(mode) }
                    .semantics { this.selected = selected == mode },
            ) {
                Text(
                    stringResource(
                        if (mode == MarkdownEditorMode.Write) R.string.markdown_write else R.string.markdown_preview,
                    ),
                    color = if (selected == mode) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MarkdownDraftPreview(markdown: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    val blocks by produceState<List<MarkdownBlock>>(emptyList(), markdown) {
        delay(120)
        value = withContext(Dispatchers.Default) { MarkdownParser.parse(markdown) }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(RyntraDesign.colors.surface, RoundedCornerShape(8.dp))
            .border(
                0.75.dp,
                if (isError) RyntraDesign.colors.destructive else RyntraDesign.colors.separator,
                RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        if (blocks.isEmpty()) {
            Text(
                stringResource(R.string.markdown_preview_empty),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            blocks.forEach { MarkdownBlockView(it) }
        }
    }
}
