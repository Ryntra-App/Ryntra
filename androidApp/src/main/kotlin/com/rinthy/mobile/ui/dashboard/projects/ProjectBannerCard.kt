package com.rinthy.mobile.ui.dashboard.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Star
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.theme.RinthyDesign

@Composable
internal fun ProjectBannerCard(
    model: ProjectRowModel,
    isFavorite: Boolean,
    onFavoriteClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val project = model.project
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(RinthyDesign.colors.surface)
                .border(0.75.dp, RinthyDesign.colors.separator, shape)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val bannerUrl = project.bannerUrl
                if (bannerUrl != null) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = project.title.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                ProjectArtwork(project, Modifier.size(48.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (project.status != "approved") StatusLabel(project.status)
                    }
                    Text(
                        text = model.subtitle,
                        color = RinthyDesign.colors.labelSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 7.dp),
                    ) {
                        ProjectBannerMetric(Lucide.Download, model.downloads, MaterialTheme.colorScheme.primary)
                        ProjectBannerMetric(Lucide.Heart, model.followers, MaterialTheme.colorScheme.tertiary)
                        model.updated?.let { date ->
                            ProjectBannerMetric(
                                Lucide.RefreshCw,
                                stringResource(R.string.project_updated_label) + " " + date,
                                RinthyDesign.colors.labelSecondary,
                            )
                        }
                    }
                }
            }
        }

        if (onFavoriteClick != null) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .zIndex(1f)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
            ) {
                // Lucide = stroke outline (empty). Material Filled = solid when favorited.
                // Material Outlined.Star looks nearly solid at small sizes — do not use it.
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Lucide.Star,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.projects_remove_favorite else R.string.projects_add_favorite,
                    ),
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ProjectBannerMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = value,
            color = RinthyDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
