package com.ryntra.mobile.ui.dashboard.project.sharecard

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.ryntra.mobile.R
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectVersion
import java.text.NumberFormat

internal enum class ShareCardFormat(
    val ratio: Float,
    @StringRes val labelRes: Int,
) {
    Square(1f, R.string.share_card_format_square),
    Post(1.91f, R.string.share_card_format_post),
    Story(9f / 16f, R.string.share_card_format_story),
}

internal enum class ShareCardTemplate(@StringRes val labelRes: Int) {
    Release(R.string.share_card_template_release),
    Milestone(R.string.share_card_template_milestone),
    Testers(R.string.share_card_template_testers),
}

internal data class ShareCardPalette(
    val id: String,
    @StringRes val labelRes: Int,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val foreground: Color,
    val secondary: Color,
    val accent: Color,
    val chip: Color,
)

internal val shareCardPalettes = listOf(
    ShareCardPalette(
        id = "midnight",
        labelRes = R.string.share_card_style_midnight,
        backgroundStart = Color(0xFF171B32),
        backgroundEnd = Color(0xFF090B17),
        foreground = Color(0xFFF4F2FF),
        secondary = Color(0xFFB9B8CF),
        accent = Color(0xFFA9B7FF),
        chip = Color(0xFF272D4B),
    ),
    ShareCardPalette(
        id = "moss",
        labelRes = R.string.share_card_style_moss,
        backgroundStart = Color(0xFF2A362A),
        backgroundEnd = Color(0xFF171F18),
        foreground = Color(0xFFF2F2E8),
        secondary = Color(0xFFC1C6B4),
        accent = Color(0xFFD7B26D),
        chip = Color(0xFF3B4938),
    ),
    ShareCardPalette(
        id = "paper",
        labelRes = R.string.share_card_style_paper,
        backgroundStart = Color(0xFFF4F1E8),
        backgroundEnd = Color(0xFFFFFCF3),
        foreground = Color(0xFF1D2A22),
        secondary = Color(0xFF59645D),
        accent = Color(0xFF296447),
        chip = Color(0xFFE0E8E1),
    ),
)

@Composable
internal fun ShareCardPreview(
    project: Project,
    version: ProjectVersion?,
    template: ShareCardTemplate,
    format: ShareCardFormat,
    palette: ShareCardPalette,
    headline: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(palette.backgroundStart, palette.backgroundEnd))),
    ) {
        val isWide = format == ShareCardFormat.Post
        val isStory = format == ShareCardFormat.Story
        val padding = when {
            isStory -> 28.dp
            isWide -> 16.dp
            else -> 26.dp
        }
        val iconSize = when {
            isStory -> 74.dp
            isWide -> 42.dp
            else -> 56.dp
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.accent.copy(alpha = 0.12f),
                radius = size.minDimension * 0.42f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.08f),
            )
            drawCircle(
                color = palette.foreground.copy(alpha = 0.045f),
                radius = size.minDimension * 0.26f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.96f),
            )
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ProjectMark(project = project, palette = palette, size = iconSize)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = stringResource(template.eyebrowRes()),
                        color = palette.accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = project.title,
                        color = palette.foreground,
                        style = if (isWide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isStory) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = headline,
                    color = palette.foreground,
                    style = when {
                        isStory -> MaterialTheme.typography.headlineLarge
                        isWide -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.Black,
                    maxLines = if (isWide) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = palette.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = when {
                            isStory -> 7
                            isWide -> 2
                            else -> 4
                        },
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = if (isWide) 5.dp else 12.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val tags = buildList {
                    version?.versionNumber?.takeIf(String::isNotBlank)?.let(::add)
                    version?.loaders?.firstOrNull()?.let { add(it.replaceFirstChar(Char::uppercase)) }
                    version?.gameVersions?.firstOrNull()?.let(::add)
                    if (template == ShareCardTemplate.Milestone) {
                        add(NumberFormat.getIntegerInstance().format(project.downloads))
                    }
                }.take(if (isWide) 2 else 3)
                if (tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag -> ShareCardTag(tag, palette) }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = if (tags.isEmpty()) 0.dp else 10.dp),
                ) {
                    val slug = project.slug ?: project.id
                    Text(
                        text = "modrinth.com/project/$slug",
                        color = palette.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "RYNTRA",
                        color = palette.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectMark(project: Project, palette: ShareCardPalette, size: androidx.compose.ui.unit.Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(palette.chip),
    ) {
        Text(
            text = project.title.take(1).uppercase(),
            color = palette.foreground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        project.iconUrl?.let { url ->
            val context = LocalContext.current
            val softwareImageRequest = remember(context, url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
            }
            AsyncImage(
                model = softwareImageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ShareCardTag(text: String, palette: ShareCardPalette) {
    Surface(color = palette.chip, shape = CircleShape) {
        Text(
            text = text,
            color = palette.foreground,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

internal fun shareCardHighlights(changelog: String): List<String> = changelog
    .lineSequence()
    .map(String::trim)
    .map { it.removePrefix("-").removePrefix("*").removePrefix("+").trim() }
    .filter { it.isNotBlank() && !it.startsWith("#") }
    .map(::stripInlineMarkdown)
    .filter(String::isNotBlank)
    .map { if (it.length > 86) it.take(83).trimEnd() + "…" else it }
    .take(3)
    .toList()

private fun stripInlineMarkdown(value: String): String = value
    .replace(Regex("""!\[([^]]*)]\([^)]*\)"""), "")
    .replace(Regex("""\[([^]]+)]\([^)]*\)"""), "$1")
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")
    .trim()

@StringRes
private fun ShareCardTemplate.eyebrowRes(): Int = when (this) {
    ShareCardTemplate.Release -> R.string.share_card_eyebrow_release
    ShareCardTemplate.Milestone -> R.string.share_card_eyebrow_milestone
    ShareCardTemplate.Testers -> R.string.share_card_eyebrow_testers
}
