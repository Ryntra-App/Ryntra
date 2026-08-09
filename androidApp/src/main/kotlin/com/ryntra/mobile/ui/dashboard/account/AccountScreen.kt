package com.ryntra.mobile.ui.dashboard.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import com.ryntra.mobile.BuildConfig
import com.ryntra.mobile.R
import com.ryntra.mobile.ProfileUpdateState
import com.ryntra.mobile.InstantNotificationState
import com.ryntra.mobile.preferences.AppLanguage
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.RyntraPreferences
import com.ryntra.mobile.preferences.ThemeStyle
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountScreen(
    account: Account,
    projectCount: Int,
    organizationCount: Int,
    profileUpdate: ProfileUpdateState,
    preferences: RyntraPreferences,
    instantNotifications: InstantNotificationState,
    onUpdateProfile: (String, String, String) -> Unit,
    onChangeAvatar: (String, com.ryntra.shared.model.ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteAvatar: (String) -> Unit = {},
    onThemeStyleChange: (ThemeStyle) -> Unit,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowFavoriteProjectsChange: (Boolean) -> Unit,
    onShowProjectBannersChange: (Boolean) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onGlassQualityChange: (GlassQuality) -> Unit,
    onLocalNotificationsChange: (Boolean) -> Unit,
    onStartInstantNotifications: () -> Unit,
    onDisconnectInstantNotifications: () -> Unit,
    onResetAppearance: () -> Unit,
    onExportPreferences: () -> String,
    onImportPreferences: (String) -> Result<Unit>,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val motion = RyntraDesign.motion
    var isEditorVisible by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf("") }
    val settingsExported = stringResource(R.string.settings_exported)
    val settingsExportFailed = stringResource(R.string.settings_export_failed)
    val settingsImported = stringResource(R.string.settings_imported)
    val settingsImportFailed = stringResource(R.string.settings_import_failed)
    val accountIdCopied = stringResource(R.string.settings_account_id_copied)
    val appearanceReset = stringResource(R.string.settings_appearance_reset)
    val imageCacheCleared = stringResource(R.string.settings_image_cache_cleared)
    val notificationPermissionDenied = stringResource(R.string.notifications_permission_denied)
    val supportAddressCopied = stringResource(R.string.support_address_copied)
    val authorUrl = stringResource(R.string.settings_author_url)

    fun showNotice(message: String) {
        notice = message
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(pendingExport)
                    } ?: error("Could not open the selected file.")
                }
            }
            showNotice(if (result.isSuccess) settingsExported else settingsExportFailed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not open the selected file.")
                }.fold(
                    onSuccess = onImportPreferences,
                    onFailure = { Result.failure(it) },
                )
            }
            showNotice(if (result.isSuccess) settingsImported else settingsImportFailed)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        onLocalNotificationsChange(isGranted)
        if (!isGranted) showNotice(notificationPermissionDenied)
    }
    val instantNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) onStartInstantNotifications() else showNotice(notificationPermissionDenied)
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2_200)
            notice = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
    ) {
        item(key = "profile-header", contentType = "profile-header") {
            ProfileHeader(
                account = account,
                isEditing = isEditorVisible,
                isAvatarBusy = profileUpdate.isSaving,
                onEditClick = { isEditorVisible = !isEditorVisible },
                onChangeAvatar = { onChangeAvatar(account.id, it) },
                onDeleteAvatar = { onDeleteAvatar(account.id) },
                onAvatarError = { showNotice(it) },
            )
            AnimatedVisibility(
                visible = isEditorVisible,
                enter = fadeIn(tween(motion.duration(180))) +
                    slideInVertically(tween(motion.duration(180))) { -it / 8 },
                exit = fadeOut(tween(motion.duration(130))) +
                    slideOutVertically(tween(motion.duration(130))) { -it / 8 },
            ) {
                ProfileEditor(
                    account = account,
                    updateState = profileUpdate,
                    onUpdateProfile = onUpdateProfile,
                )
            }
        }
        item(key = "profile-notice", contentType = "notice") {
            AnimatedVisibility(
                visible = notice != null,
                enter = fadeIn(tween(motion.duration(160))) +
                    slideInVertically(tween(motion.duration(160))) { -it / 4 },
                exit = fadeOut(tween(motion.duration(120))) +
                    slideOutVertically(tween(motion.duration(120))) { -it / 4 },
            ) {
                SettingsNotice(notice.orEmpty())
            }
        }
        item(key = "profile-workspace", contentType = "metrics") {
            Column(modifier = Modifier.padding(top = 28.dp)) {
                RyntraSectionLabel(stringResource(R.string.profile_workspace), modifier = Modifier.padding(start = 4.dp, bottom = 9.dp))
                WorkspaceMetrics(projectCount, organizationCount)
            }
        }
        item(key = "profile-account-tools", contentType = "settings-group") {
            AccountLinksSection(
                account = account,
                onOpenUrl = uriHandler::openUri,
                onCopyAccountId = {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Modrinth account ID", account.id))
                    showNotice(accountIdCopied)
                },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
        item(key = "profile-appearance", contentType = "settings-group") {
            AppearanceSettingsSection(
                preferences = preferences,
                onThemeStyleChange = onThemeStyleChange,
                onAppearanceModeChange = onAppearanceModeChange,
                onAppLanguageChange = onAppLanguageChange,
                onShowFavoriteProjectsChange = onShowFavoriteProjectsChange,
                onShowProjectBannersChange = onShowProjectBannersChange,
                onReduceMotionChange = onReduceMotionChange,
                onGlassQualityChange = onGlassQualityChange,
                onResetAppearance = {
                    onResetAppearance()
                    showNotice(appearanceReset)
                },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
        item(key = "profile-notifications", contentType = "settings-group") {
            NotificationSettingsSection(
                isEnabled = preferences.localNotificationsEnabled,
                instantState = instantNotifications,
                onEnabledChange = { isEnabled ->
                    if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onLocalNotificationsChange(isEnabled)
                    }
                },
                onInstantAction = {
                    if (instantNotifications.isConnected) {
                        onDisconnectInstantNotifications()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        instantNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onStartInstantNotifications()
                    }
                },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
        item(key = "profile-local-data", contentType = "settings-group") {
            LocalDataSettingsSection(
                onExport = {
                    pendingExport = onExportPreferences()
                    exportLauncher.launch("ryntra-settings-${account.username}.json")
                },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/json")) },
                onClearImageCache = {
                    scope.launch {
                        val imageLoader = SingletonImageLoader.get(context)
                        imageLoader.memoryCache?.clear()
                        withContext(Dispatchers.IO) { imageLoader.diskCache?.clear() }
                        showNotice(imageCacheCleared)
                    }
                },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
        item(key = "profile-support", contentType = "settings-group") {
            SupportAuthorSection(
                onOpenDonationAlerts = { uriHandler.openUri(SupportDetails.DONATION_ALERTS_URL) },
                onCopyTrc20 = {
                    context.copyToClipboard("USDT TRC20", SupportDetails.USDT_TRC20)
                    showNotice(supportAddressCopied)
                },
                onCopyTon = {
                    context.copyToClipboard("TON USDT", SupportDetails.TON_USDT)
                    showNotice(supportAddressCopied)
                },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
        item(key = "profile-about", contentType = "settings-group") {
            AboutSettingsSection(
                appVersion = BuildConfig.VERSION_NAME,
                onOpenReleases = { uriHandler.openUri("https://github.com/imsawiq/Ryntra/releases") },
                onOpenAuthor = { uriHandler.openUri(authorUrl) },
                onSignOut = onSignOut,
                modifier = Modifier.padding(top = 26.dp),
            )
        }
    }
}

private fun android.content.Context.copyToClipboard(label: String, value: String) {
    val clipboard = getSystemService(android.content.ClipboardManager::class.java)
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
}

@Composable
private fun SettingsNotice(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .background(RyntraDesign.colors.accent.copy(alpha = 0.10f), RyntraDesign.contentShape)
            .border(0.75.dp, RyntraDesign.colors.accent.copy(alpha = 0.24f), RyntraDesign.contentShape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(
            text = message,
            color = RyntraDesign.colors.labelPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
