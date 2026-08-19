package com.ryntra.mobile.ui.dashboard.project.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.Play
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraIcon
import com.ryntra.shared.model.MarkdownEmbed
import com.ryntra.shared.model.MarkdownEmbedProvider

/**
 * A video or server embedded in a project description.
 *
 * The card opens the video in YouTube rather than playing it in place: an inline `WebView` inside
 * a lazily scrolled description means shipping a JavaScript runtime for content anyone can author,
 * and it fights the surrounding scroll on both platforms. Handing off to the app that already
 * handles playback is what native readers do.
 */
@Composable
internal fun MarkdownEmbedCard(embed: MarkdownEmbed, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val label = when (embed.provider) {
        MarkdownEmbedProvider.YouTube -> stringResource(R.string.markdown_embed_youtube)
        MarkdownEmbedProvider.Discord -> stringResource(R.string.markdown_embed_discord)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.markdown_embed_open, label),
            ) { uriHandler.openUri(embed.url) },
    ) {
        Column {
            embed.thumbnailUrl?.let { thumbnail ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                ) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.62f), CircleShape),
                    ) {
                        RyntraIcon(
                            icon = Lucide.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            ) {
                RyntraIcon(
                    icon = when (embed.provider) {
                        MarkdownEmbedProvider.YouTube -> Lucide.Play
                        MarkdownEmbedProvider.Discord -> Lucide.MessageSquareText
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RyntraIcon(
                    icon = Lucide.ExternalLink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
