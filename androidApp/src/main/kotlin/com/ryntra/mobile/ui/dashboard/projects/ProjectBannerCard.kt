package com.ryntra.mobile.ui.dashboard.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign

@Composable
internal fun ProjectBannerCard(
    model: ProjectRowModel,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onFavoriteClick: (() -> Unit)?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val project = model.project
    val shape = RyntraDesign.contentShape
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            RyntraDesign.colors.surface
        },
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Project selection container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            RyntraDesign.colors.separator
        },
        animationSpec = tween(RyntraDesign.motion.duration(160)),
        label = "Project selection border",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(if (isSelected) 1.dp else 0.75.dp, borderColor, shape)
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            onClickLabel = stringResource(R.string.project_action_open),
                            onLongClickLabel = stringResource(R.string.project_action_more),
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier.clickable(
                            onClickLabel = stringResource(R.string.project_action_open),
                            onClick = onClick,
                        )
                    },
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
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
                        color = RyntraDesign.colors.labelSecondary,
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
                                RyntraDesign.colors.labelSecondary,
                            )
                        }
                    }
                }
            }
        }

        if (onFavoriteClick != null) {
            val favoriteContainer by animateColorAsState(
                targetValue = if (isFavorite) {
                    RyntraDesign.colors.accent.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                },
                animationSpec = tween(RyntraDesign.motion.duration(180)),
                label = "Favorite container",
            )
            val favoriteTint by animateColorAsState(
                targetValue = if (isFavorite) RyntraDesign.colors.accent else RyntraDesign.colors.labelSecondary,
                animationSpec = tween(RyntraDesign.motion.duration(180)),
                label = "Favorite tint",
            )
            val favoriteScale by animateFloatAsState(
                targetValue = if (isFavorite && !RyntraDesign.motion.isReduced) 1.08f else 1f,
                animationSpec = tween(RyntraDesign.motion.duration(180)),
                label = "Favorite scale",
            )
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .zIndex(1f)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(favoriteContainer),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Lucide.Star,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.projects_remove_favorite else R.string.projects_add_favorite,
                    ),
                    tint = favoriteTint,
                    modifier = Modifier.size(22.dp).graphicsLayer {
                        scaleX = favoriteScale
                        scaleY = favoriteScale
                    },
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
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = value,
            color = RyntraDesign.colors.labelSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
