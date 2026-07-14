package com.rinthy.mobile.ui.dashboard.project

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.rinthy.mobile.ProjectActionState
import com.rinthy.mobile.R
import com.rinthy.mobile.media.ImageUploadReader
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.components.RinthyTextField
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.GalleryImage
import com.rinthy.shared.model.ProjectFileUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_GALLERY_BYTES = 5 * 1024 * 1024

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
                ImageUploadReader.read(context, uri, "gallery.png", MAX_GALLERY_BYTES)
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
        Text(
            stringResource(R.string.project_gallery_manage_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 10.dp),
        )
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

@Composable
internal fun ProjectGalleryViewerDialog(
    image: GalleryImage,
    canManage: Boolean = false,
    onDismiss: () -> Unit,
    onSetBanner: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    GalleryFullscreenDialog(
        image = image,
        canManage = canManage,
        onDismiss = onDismiss,
        onSetBanner = onSetBanner ?: {},
        onEdit = onEdit ?: {},
        onDelete = onDelete ?: {},
    )
}

@Composable
private fun GalleryFullscreenDialog(
    image: GalleryImage,
    canManage: Boolean = true,
    onDismiss: () -> Unit,
    onSetBanner: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        image.displayTitle ?: stringResource(R.string.project_gallery),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Lucide.X, contentDescription = stringResource(R.string.project_gallery_close))
                    }
                }
                AsyncImage(
                    model = image.rawUrl?.takeIf { it.isNotBlank() } ?: image.url,
                    contentDescription = image.displayTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 520.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface),
                )
                image.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (canManage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        if (!image.featured) {
                            RinthySecondaryButton(
                                text = stringResource(R.string.project_gallery_set_banner),
                                icon = Lucide.Star,
                                onClick = onSetBanner,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        RinthySecondaryButton(
                            text = stringResource(R.string.project_gallery_edit),
                            icon = Lucide.Pencil,
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                        )
                        RinthySecondaryButton(
                            text = stringResource(R.string.project_gallery_delete),
                            icon = Lucide.Trash2,
                            onClick = onDelete,
                            isDestructive = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryEditDialog(
    image: GalleryImage,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, ordering: Int?) -> Unit,
    onSetBanner: () -> Unit,
) {
    var title by remember(image.url) { mutableStateOf(image.displayTitle.orEmpty()) }
    var description by remember(image.url) { mutableStateOf(image.description.orEmpty()) }
    var ordering by remember(image.url) { mutableStateOf(image.ordering.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.project_gallery_edit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                RinthyTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.project_gallery_title_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                RinthyTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(R.string.project_gallery_description_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                RinthyTextField(
                    value = ordering,
                    onValueChange = { ordering = it.filter(Char::isDigit).take(4) },
                    placeholder = stringResource(R.string.project_gallery_ordering_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!image.featured) {
                    Spacer(Modifier.height(12.dp))
                    RinthySecondaryButton(
                        text = stringResource(R.string.project_gallery_set_banner),
                        icon = Lucide.Star,
                        enabled = !isBusy,
                        onClick = onSetBanner,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    RinthySecondaryButton(
                        text = stringResource(R.string.project_gallery_close),
                        icon = Lucide.X,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    RinthyPrimaryButton(
                        text = stringResource(R.string.project_gallery_save),
                        icon = Lucide.Pencil,
                        isLoading = isBusy,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        onClick = { onSave(title.trim(), description.trim(), ordering.toIntOrNull()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryAddDialog(
    featured: Boolean,
    title: String,
    description: String,
    isBusy: Boolean,
    onFeaturedChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    stringResource(R.string.project_gallery_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                RinthyTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.project_gallery_title_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                RinthyTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    placeholder = stringResource(R.string.project_gallery_description_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = if (featured) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFeaturedChange(!featured) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Icon(Lucide.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.project_gallery_featured),
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    RinthySecondaryButton(
                        text = stringResource(R.string.project_gallery_close),
                        icon = Lucide.X,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    RinthyPrimaryButton(
                        text = stringResource(R.string.project_edit_add_gallery),
                        icon = Lucide.ImagePlus,
                        isLoading = isBusy,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        onClick = onPick,
                    )
                }
            }
        }
    }
}
