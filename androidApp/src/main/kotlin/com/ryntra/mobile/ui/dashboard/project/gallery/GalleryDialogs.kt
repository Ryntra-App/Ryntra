package com.ryntra.mobile.ui.dashboard.project.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.mobile.ui.components.RyntraSecondaryButton
import com.ryntra.mobile.ui.components.RyntraTextField
import com.ryntra.shared.model.GalleryImage

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
internal fun GalleryFullscreenDialog(
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
                            RyntraSecondaryButton(
                                text = stringResource(R.string.project_gallery_set_banner),
                                icon = Lucide.Star,
                                onClick = onSetBanner,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        RyntraSecondaryButton(
                            text = stringResource(R.string.project_gallery_edit),
                            icon = Lucide.Pencil,
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                        )
                        RyntraSecondaryButton(
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
internal fun GalleryEditDialog(
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
                RyntraTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.project_gallery_title_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                RyntraTextField(
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
                RyntraTextField(
                    value = ordering,
                    onValueChange = { ordering = it.filter(Char::isDigit).take(4) },
                    placeholder = stringResource(R.string.project_gallery_ordering_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!image.featured) {
                    Spacer(Modifier.height(12.dp))
                    RyntraSecondaryButton(
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
                    RyntraSecondaryButton(
                        text = stringResource(R.string.project_gallery_close),
                        icon = Lucide.X,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    RyntraPrimaryButton(
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
internal fun GalleryAddDialog(
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
                RyntraTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.project_gallery_title_field),
                    leadingIcon = Lucide.Pencil,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                RyntraTextField(
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
                    RyntraSecondaryButton(
                        text = stringResource(R.string.project_gallery_close),
                        icon = Lucide.X,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    RyntraPrimaryButton(
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
