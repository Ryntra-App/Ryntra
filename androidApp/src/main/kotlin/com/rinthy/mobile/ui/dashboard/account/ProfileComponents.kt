package com.rinthy.mobile.ui.dashboard.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.User
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.ProfileUpdateState
import com.rinthy.mobile.R
import com.rinthy.mobile.media.ImageUploadReader
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.components.RinthyTextField
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.ProjectUploadLimits
import com.rinthy.shared.model.Account
import com.rinthy.shared.model.ProjectFileUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileHeader(
    account: Account,
    isEditing: Boolean,
    isAvatarBusy: Boolean,
    onEditClick: () -> Unit,
    onChangeAvatar: (ProjectFileUpload) -> Unit,
    onDeleteAvatar: () -> Unit,
    onAvatarError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val motion = RinthyDesign.motion
    val showAvatarChrome = isEditing || isAvatarBusy
    val avatarOverlayAlpha by animateFloatAsState(
        targetValue = if (showAvatarChrome) 1f else 0f,
        animationSpec = tween(motion.duration(180)),
        label = "avatar overlay",
    )
    val tooLarge = stringResource(R.string.profile_avatar_too_large)
    val notImage = stringResource(R.string.profile_avatar_not_image)
    val unread = stringResource(R.string.profile_avatar_unreadable)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val upload = withContext(Dispatchers.IO) {
                ImageUploadReader.read(
                    context = context,
                    uri = uri,
                    fallbackName = "avatar.png",
                    maxBytes = ProjectUploadLimits.USER_AVATAR_BYTES,
                )
            }
            when {
                upload == null -> onAvatarError(unread)
                !upload.contentType.startsWith("image/") -> onAvatarError(notImage)
                upload.bytes.size > ProjectUploadLimits.USER_AVATAR_BYTES -> onAvatarError(tooLarge)
                else -> onChangeAvatar(upload)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(RinthyDesign.colors.surface)
                .border(1.dp, RinthyDesign.colors.accent.copy(alpha = 0.38f), CircleShape)
                .then(
                    if (isEditing) {
                        Modifier.clickable(enabled = !isAvatarBusy) {
                            picker.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif"))
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Camera overlay only while profile edit is open (matches name/bio editor).
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = avatarOverlayAlpha }
                    .background(RinthyDesign.colors.surface.copy(alpha = 0.42f)),
            ) {
                if (isAvatarBusy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = RinthyDesign.colors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                } else if (isEditing) {
                    RinthyIcon(
                        Lucide.Camera,
                        contentDescription = stringResource(R.string.profile_avatar_change),
                        tint = RinthyDesign.colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 15.dp, end = 10.dp)) {
            Text(
                text = account.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = account.role.replaceFirstChar(Char::uppercase),
                color = RinthyDesign.colors.accent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = account.id,
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            AnimatedVisibility(
                visible = isEditing,
                enter = fadeIn(tween(motion.duration(180))),
                exit = fadeOut(tween(motion.duration(130))),
            ) {
                Text(
                    text = stringResource(R.string.profile_avatar_hint),
                    color = RinthyDesign.colors.labelSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RinthyDesign.colors.surfaceRaised)
                .border(0.75.dp, RinthyDesign.colors.separator, CircleShape)
                .clickable(onClick = onEditClick),
        ) {
            RinthyIcon(
                icon = Lucide.Pencil,
                contentDescription = stringResource(R.string.profile_edit),
                tint = RinthyDesign.colors.accent,
                modifier = Modifier.size(19.dp),
            )
        }
    }
    AnimatedVisibility(
        visible = isEditing && !account.avatarUrl.isNullOrBlank(),
        enter = fadeIn(tween(motion.duration(180))),
        exit = fadeOut(tween(motion.duration(130))),
    ) {
        RinthySecondaryButton(
            text = stringResource(R.string.profile_avatar_remove),
            icon = Lucide.Trash2,
            onClick = onDeleteAvatar,
            enabled = !isAvatarBusy,
            isDestructive = true,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (!isEditing) {
        account.bio?.takeIf(String::isNotBlank)?.let { bio ->
            Text(
                text = bio,
                color = RinthyDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 17.dp),
            )
        }
    }
}

@Composable
internal fun ProfileEditor(
    account: Account,
    updateState: ProfileUpdateState,
    onUpdateProfile: (String, String, String) -> Unit,
) {
    var username by rememberSaveable(account.id) { mutableStateOf(account.username) }
    var bio by rememberSaveable(account.id) { mutableStateOf(account.bio.orEmpty()) }
    var syncedUsername by rememberSaveable(account.id) { mutableStateOf(account.username) }
    var syncedBio by rememberSaveable(account.id) { mutableStateOf(account.bio.orEmpty()) }

    LaunchedEffect(account.username, account.bio) {
        if (username == syncedUsername) username = account.username
        if (bio == syncedBio) bio = account.bio.orEmpty()
        syncedUsername = account.username
        syncedBio = account.bio.orEmpty()
    }

    val normalizedUsername = username.trim()
    val normalizedBio = bio.trim()
    val hasChanges = normalizedUsername != account.username || normalizedBio != account.bio.orEmpty()
    val canSave = normalizedUsername.isNotBlank() && hasChanges && !updateState.isSaving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        RinthyTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = stringResource(R.string.profile_username),
            leadingIcon = Lucide.User,
            leadingIconDescription = null,
            enabled = !updateState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        RinthyTextField(
            value = bio,
            onValueChange = { bio = it },
            placeholder = stringResource(R.string.profile_bio_hint),
            leadingIcon = Lucide.FileText,
            leadingIconDescription = null,
            enabled = !updateState.isSaving,
            singleLine = false,
            minLines = 4,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        AnimatedVisibility(
            visible = updateState.errorMessage != null,
            enter = fadeIn(tween(RinthyDesign.motion.duration(150))),
            exit = fadeOut(tween(RinthyDesign.motion.duration(110))),
        ) {
            Text(
                text = updateState.errorMessage.orEmpty(),
                color = RinthyDesign.colors.destructive,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
        RinthyPrimaryButton(
            text = stringResource(R.string.profile_save),
            icon = Lucide.Save,
            onClick = { onUpdateProfile(account.id, normalizedUsername, normalizedBio) },
            enabled = canSave,
            isLoading = updateState.isSaving,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
internal fun WorkspaceMetrics(projectCount: Int, organizationCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AccountMetric(Lucide.Package, stringResource(R.string.analytics_projects), projectCount.toString(), Modifier.weight(1f))
        AccountMetric(Lucide.UsersRound, stringResource(R.string.profile_teams), organizationCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun AccountMetric(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(RinthyDesign.colors.surface, shape)
            .border(0.75.dp, RinthyDesign.colors.separator, shape)
            .padding(horizontal = 13.dp, vertical = 14.dp),
    ) {
        RinthyIcon(icon, contentDescription = null, tint = RinthyDesign.colors.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, color = RinthyDesign.colors.labelSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
