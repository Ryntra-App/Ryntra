package com.rinthy.mobile.ui.dashboard.project.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.rinthy.mobile.ProjectActionState
import com.rinthy.mobile.R
import com.rinthy.mobile.media.ImageUploadReader
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.GalleryImage
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectUploadLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProjectGalleryManageSection(
    gallery: List<GalleryImage>,
    isBusy: Boolean,
    actionState: ProjectActionState,
    onAdd: (file: ProjectFileUpload, featured: Boolean, title: String, description: String) -> Unit,
    onDelete: (imageUrl: String) -> Unit,
    onSetBanner: (imageUrl: String) -> Unit,
    onSaveMeta: (imageUrl: String, title: String, description: String, ordering: Int?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingAdd by remember { mutableStateOf(false) }
    var pendingFeatured by remember { mutableStateOf(false) }
    var pendingTitle by remember { mutableStateOf("") }
    var pendingDescription by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<GalleryImage?>(null) }
    var editor by remember { mutableStateOf<GalleryImage?>(null) }
    var deleteTarget by remember { mutableStateOf<GalleryImage?>(null) }
    val tooLarge = stringResource(R.string.project_gallery_too_large)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val upload = withContext(Dispatchers.IO) {
                ImageUploadReader.read(context, uri, "gallery.png", ProjectUploadLimits.GALLERY_IMAGE_BYTES)
            }
            if (upload == null) {
                localError = tooLarge
                return@launch
            }
            onAdd(upload, pendingFeatured, pendingTitle.trim(), pendingDescription.trim())
            pendingAdd = false
            pendingFeatured = false
            pendingTitle = ""
            pendingDescription = ""
            localError = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (gallery.isEmpty()) {
            Text(
                stringResource(R.string.project_gallery_empty),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.project_gallery_empty_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        } else {
            gallery.sortedWith(
                compareByDescending<GalleryImage> { it.featured }.thenBy { it.ordering },
            ).forEach { image ->
                GalleryManageTile(
                    image = image,
                    isBusy = isBusy && actionState.targetId == image.url,
                    onOpen = { viewer = image },
                    onSetBanner = { onSetBanner(image.url) },
                    onEdit = { editor = image },
                    onDelete = { deleteTarget = image },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
        (localError ?: actionState.errorMessage)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        RinthySecondaryButton(
            text = stringResource(R.string.project_gallery_add_title),
            icon = Lucide.ImagePlus,
            enabled = !isBusy,
            onClick = { pendingAdd = true },
        )
    }

    if (pendingAdd) {
        GalleryAddDialog(
            featured = pendingFeatured,
            title = pendingTitle,
            description = pendingDescription,
            isBusy = isBusy,
            onFeaturedChange = { pendingFeatured = it },
            onTitleChange = { pendingTitle = it },
            onDescriptionChange = { pendingDescription = it },
            onPick = { picker.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) },
            onDismiss = { pendingAdd = false },
        )
    }

    viewer?.let { image ->
        GalleryFullscreenDialog(
            image = image,
            onDismiss = { viewer = null },
            onSetBanner = {
                onSetBanner(image.url)
                viewer = null
            },
            onEdit = {
                viewer = null
                editor = image
            },
            onDelete = {
                viewer = null
                deleteTarget = image
            },
        )
    }

    editor?.let { image ->
        GalleryEditDialog(
            image = image,
            isBusy = isBusy,
            onDismiss = { editor = null },
            onSave = { title, description, ordering ->
                onSaveMeta(image.url, title, description, ordering)
                editor = null
            },
            onSetBanner = {
                onSetBanner(image.url)
                editor = null
            },
        )
    }

    deleteTarget?.let { image ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.project_gallery_delete_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(image.url)
                        deleteTarget = null
                    },
                ) {
                    Text(stringResource(R.string.project_gallery_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.project_gallery_close))
                }
            },
        )
    }
}

@Composable
internal fun ProjectGalleryOverviewStrip(
    gallery: List<GalleryImage>,
    onOpen: (GalleryImage) -> Unit,
) {
    if (gallery.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        gallery.sortedWith(
            compareByDescending<GalleryImage> { it.featured }.thenBy { it.ordering },
        ).forEach { image ->
            Box(
                modifier = Modifier
                    .width(172.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpen(image) },
            ) {
                AsyncImage(
                    model = image.url,
                    contentDescription = image.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (image.featured) {
                    BannerBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
                }
                Icon(
                    Lucide.Maximize2,
                    contentDescription = stringResource(R.string.project_gallery_open),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(3.dp),
                )
            }
        }
    }
}

@Composable
private fun GalleryManageTile(
    image: GalleryImage,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onSetBanner: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RinthyDesign.colors.surface)
            .border(0.75.dp, RinthyDesign.colors.separator, shape)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpen),
        ) {
            AsyncImage(
                model = image.url,
                contentDescription = image.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (image.featured) {
                BannerBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            if (isBusy) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            }
        }
        image.displayTitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            IconButton(onClick = onOpen, enabled = !isBusy) {
                Icon(Lucide.Maximize2, contentDescription = stringResource(R.string.project_gallery_open))
            }
            if (!image.featured) {
                IconButton(onClick = onSetBanner, enabled = !isBusy) {
                    Icon(Lucide.Star, contentDescription = stringResource(R.string.project_gallery_set_banner))
                }
            }
            IconButton(onClick = onEdit, enabled = !isBusy) {
                Icon(Lucide.Pencil, contentDescription = stringResource(R.string.project_gallery_edit))
            }
            IconButton(onClick = onDelete, enabled = !isBusy) {
                Icon(
                    Lucide.Trash2,
                    contentDescription = stringResource(R.string.project_gallery_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BannerBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier,
    ) {
        Text(
            stringResource(R.string.project_gallery_banner_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
