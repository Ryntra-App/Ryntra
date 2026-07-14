package com.rinthy.mobile.ui.dashboard.project.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.MarkdownBlock
import com.rinthy.shared.model.MarkdownBlockType
import com.rinthy.shared.model.MarkdownImage
import com.rinthy.shared.model.MarkdownSpanType

@Composable
internal fun MarkdownBlockView(block: MarkdownBlock) {
    when (block.type) {
        MarkdownBlockType.Divider -> {
            HorizontalDivider(color = RinthyDesign.colors.separator, modifier = Modifier.padding(vertical = 6.dp))
            return
        }

        MarkdownBlockType.Image -> {
            MarkdownImageRow(block.images)
            return
        }

        MarkdownBlockType.CodeBlock, MarkdownBlockType.Table -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            return
        }

        else -> Unit
    }

    val codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val linkColor = RinthyDesign.colors.accent
    val annotatedString = remember(block, codeBackground, linkColor) {
        buildAnnotatedString {
            append(block.content)
            block.spans.forEach { span ->
                val style = when (span.type) {
                    MarkdownSpanType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                    MarkdownSpanType.Italic -> SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    MarkdownSpanType.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    MarkdownSpanType.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
                    MarkdownSpanType.Link -> SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                }
                addStyle(style, span.start, span.end)
                if (span.type == MarkdownSpanType.Link) {
                    span.linkUrl?.let { addLink(LinkAnnotation.Url(it), span.start, span.end) }
                }
            }
        }
    }
    val style = when (block.type) {
        MarkdownBlockType.Heading -> when (block.level) {
            1 -> MaterialTheme.typography.headlineMedium
            2 -> MaterialTheme.typography.headlineSmall
            else -> MaterialTheme.typography.titleMedium
        }
        else -> MaterialTheme.typography.bodyMedium
    }
    val color = if (block.type == MarkdownBlockType.Heading) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    when (block.type) {
        MarkdownBlockType.Quote -> Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(end = 10.dp).width(3.dp).heightIn(min = 20.dp)
                    .clip(RoundedCornerShape(2.dp)).background(RinthyDesign.colors.accent),
            )
            Text(text = annotatedString, style = style, color = color)
        }
        MarkdownBlockType.ListItem -> Row {
            Text(
                text = when (block.checked) {
                    true -> "[x] "
                    false -> "[ ] "
                    null -> if (block.ordered) "${block.ordinal}. " else "- "
                },
                style = style,
                color = color,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = annotatedString, style = style, color = color)
        }
        else -> Text(text = annotatedString, style = style, color = color)
    }
}

@Composable
private fun MarkdownImageRow(images: List<MarkdownImage>) {
    if (images.isEmpty()) return
    val uriHandler = LocalUriHandler.current

    if (images.size == 1 && !images.first().isBadge) {
        val image = images.first()
        val link = image.linkUrl
        AsyncImage(
            model = image.url,
            contentDescription = image.alt.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier.widthIn(max = 640.dp).heightIn(max = 360.dp).clip(RoundedCornerShape(8.dp))
                .then(if (link != null) Modifier.clickable { uriHandler.openUri(link) } else Modifier),
        )
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        images.forEach { image ->
            val link = image.linkUrl
            AsyncImage(
                model = image.url,
                contentDescription = image.alt.ifBlank { null },
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(36.dp).clip(RoundedCornerShape(5.dp))
                    .then(if (link != null) Modifier.clickable { uriHandler.openUri(link) } else Modifier),
            )
        }
    }
}
