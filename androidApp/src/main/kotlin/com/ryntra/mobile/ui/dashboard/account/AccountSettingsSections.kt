package com.ryntra.mobile.ui.dashboard.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.User
import com.composables.icons.lucide.ZapOff
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.ImageOff
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Upload
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.BellRing
import com.ryntra.mobile.InstantNotificationState
import com.composables.icons.lucide.HeartHandshake
import com.ryntra.mobile.preferences.AppLanguage
import com.ryntra.mobile.preferences.GlassQuality
import com.ryntra.mobile.preferences.AppearanceMode
import com.ryntra.mobile.preferences.RyntraPreferences
import com.ryntra.mobile.preferences.ThemeStyle
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Account

@Composable
internal fun AccountLinksSection(
    account: Account,
    onOpenUrl: (String) -> Unit,
    onCopyAccountId: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.settings_account), modifier = modifier) {
        SettingsRow(
            icon = Lucide.ExternalLink,
            title = stringResource(R.string.settings_open_profile),
            subtitle = stringResource(R.string.settings_open_profile_hint),
            onClick = { onOpenUrl("https://modrinth.com/user/${account.username}") },
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.KeyRound,
            title = stringResource(R.string.settings_api_tokens),
            subtitle = stringResource(R.string.settings_api_tokens_hint),
            onClick = { onOpenUrl("https://modrinth.com/settings/pats") },
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Wallet,
            title = stringResource(R.string.settings_creator_payouts),
            subtitle = stringResource(R.string.settings_creator_payouts_hint),
            onClick = { onOpenUrl("https://modrinth.com/dashboard/revenue") },
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Copy,
            title = stringResource(R.string.settings_copy_account_id),
            subtitle = account.id,
            onClick = onCopyAccountId,
        )
    }
}

@Composable
internal fun AppearanceSettingsSection(
    preferences: RyntraPreferences,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowFavoriteProjectsChange: (Boolean) -> Unit,
    onShowProjectBannersChange: (Boolean) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onGlassQualityChange: (GlassQuality) -> Unit,
    onResetAppearance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.settings_appearance), modifier = modifier) {
        SettingsRow(
            icon = Lucide.Sparkles,
            title = stringResource(R.string.settings_interface_style),
            subtitle = if (preferences.themeStyle == ThemeStyle.Platform) {
                stringResource(R.string.settings_native_style)
            } else {
                stringResource(R.string.settings_ryntra_style)
            },
        )
        ThemeStylePicker(selected = preferences.themeStyle, onSelect = onThemeStyleChange)
        if (preferences.themeStyle == ThemeStyle.Platform) {
            SettingsDivider()
            SettingsRow(
                icon = Lucide.Monitor,
                title = stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_hint),
            )
            AppearanceModePicker(selected = preferences.appearanceMode, onSelect = onAppearanceModeChange)
        }
        SettingsDivider()
        SettingsRow(
            icon = Lucide.ImageOff,
            title = stringResource(R.string.settings_project_banners),
            subtitle = stringResource(R.string.settings_project_banners_hint),
            trailing = {
                RyntraSwitch(
                    checked = preferences.showProjectBanners,
                    onCheckedChange = onShowProjectBannersChange,
                    contentDescription = stringResource(R.string.settings_project_banners),
                )
            },
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Globe,
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_hint),
        )
        AppLanguagePicker(selected = preferences.appLanguage, onSelect = onAppLanguageChange)
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Star,
            title = stringResource(R.string.settings_favorites),
            subtitle = stringResource(R.string.settings_favorites_hint),
            trailing = {
                RyntraSwitch(
                    checked = preferences.showFavoriteProjects,
                    onCheckedChange = onShowFavoriteProjectsChange,
                    contentDescription = stringResource(R.string.settings_favorites),
                )
            },
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.ZapOff,
            title = stringResource(R.string.settings_reduce_motion),
            subtitle = stringResource(R.string.settings_reduce_motion_hint),
            trailing = {
                RyntraSwitch(
                    checked = preferences.reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                    contentDescription = stringResource(R.string.settings_reduce_motion),
                )
            },
        )
        SettingsDivider()
        if (preferences.themeStyle == ThemeStyle.Ryntra) {
            SettingsRow(
                icon = Lucide.Sparkles,
                title = stringResource(R.string.settings_glass_quality),
                subtitle = stringResource(R.string.settings_glass_quality_hint),
            )
            GlassQualityPicker(selected = preferences.glassQuality, onSelect = onGlassQualityChange)
            SettingsDivider()
        }
        SettingsRow(
            icon = Lucide.RotateCcw,
            title = stringResource(R.string.settings_reset_appearance),
            subtitle = stringResource(R.string.settings_reset_appearance_hint),
            onClick = onResetAppearance,
        )
    }
}

@Composable
internal fun LocalDataSettingsSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearImageCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.settings_local_data), modifier = modifier) {
        SettingsRow(
            icon = Lucide.Download,
            title = stringResource(R.string.settings_export),
            subtitle = stringResource(R.string.settings_export_hint),
            onClick = onExport,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Upload,
            title = stringResource(R.string.settings_import),
            subtitle = stringResource(R.string.settings_import_hint),
            onClick = onImport,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.ImageOff,
            title = stringResource(R.string.settings_clear_images),
            subtitle = stringResource(R.string.settings_clear_images_hint),
            onClick = onClearImageCache,
        )
    }
}

@Composable
internal fun NotificationSettingsSection(
    isEnabled: Boolean,
    instantState: InstantNotificationState,
    onEnabledChange: (Boolean) -> Unit,
    onInstantAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.notifications_settings_title), modifier = modifier) {
        SettingsRow(
            icon = Lucide.Bell,
            title = stringResource(R.string.notifications_local_title),
            subtitle = stringResource(R.string.notifications_local_hint),
            trailing = {
                RyntraSwitch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    contentDescription = stringResource(R.string.notifications_local_title),
                    enabled = !instantState.isConnected,
                )
            },
        )
        SettingsDivider()
        val instantSubtitle = when {
            !instantState.isAvailable -> stringResource(R.string.notifications_instant_unavailable)
            instantState.isLoading -> stringResource(R.string.notifications_instant_loading)
            instantState.errorMessage != null -> instantState.errorMessage
            instantState.isConnected -> stringResource(R.string.notifications_instant_connected)
            else -> stringResource(R.string.notifications_instant_hint)
        }
        SettingsRow(
            icon = Lucide.BellRing,
            title = stringResource(R.string.notifications_instant_title),
            subtitle = instantSubtitle,
            onClick = if (instantState.isAvailable && !instantState.isLoading) onInstantAction else null,
            isDestructive = instantState.isConnected,
        )
    }
}

@Composable
internal fun AboutSettingsSection(
    appVersion: String,
    onOpenReleases: () -> Unit,
    onOpenAuthor: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.settings_about), modifier = modifier) {
        SettingsRow(
            icon = Lucide.Info,
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.settings_unofficial_with_version, appVersion),
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.User,
            title = stringResource(R.string.settings_author),
            subtitle = stringResource(R.string.settings_author_name),
            onClick = onOpenAuthor,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Github,
            title = stringResource(R.string.settings_releases),
            subtitle = stringResource(R.string.settings_releases_hint),
            onClick = onOpenReleases,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.LogOut,
            title = stringResource(R.string.settings_sign_out),
            subtitle = stringResource(R.string.settings_sign_out_hint),
            onClick = onSignOut,
            isDestructive = true,
        )
    }
}

@Composable
internal fun SupportAuthorSection(
    onOpenDonationAlerts: () -> Unit,
    onCopyTrc20: () -> Unit,
    onCopyTon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(title = stringResource(R.string.support_title), modifier = modifier) {
        SettingsRow(
            icon = Lucide.HeartHandshake,
            title = stringResource(R.string.support_donation_alerts),
            subtitle = stringResource(R.string.support_donation_alerts_hint),
            onClick = onOpenDonationAlerts,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Copy,
            title = stringResource(R.string.support_usdt_trc20),
            subtitle = SupportDetails.USDT_TRC20,
            onClick = onCopyTrc20,
        )
        SettingsDivider()
        SettingsRow(
            icon = Lucide.Copy,
            title = stringResource(R.string.support_ton_usdt),
            subtitle = SupportDetails.TON_USDT,
            onClick = onCopyTon,
        )
    }
}
