package com.ryntra.mobile.ui.dashboard.project.disclosures

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.CircleDollarSign
import com.composables.icons.lucide.CircuitBoard
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.GitFork
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Megaphone
import com.composables.icons.lucide.RadioTower
import com.composables.icons.lucide.Sparkles
import com.ryntra.mobile.R
import com.ryntra.shared.model.AiUsage
import com.ryntra.shared.model.DisclosureIssue
import com.ryntra.shared.model.DisclosureType
import com.ryntra.shared.model.TelemetryConsent

/** Section 6 of the Content Rules, which is what the AI disclosure is judged against. */
internal const val CONTENT_RULES_URL = "https://modrinth.com/legal/rules#generative-ai"

internal val DisclosureType.icon: ImageVector
    get() = when (this) {
        DisclosureType.AiContent -> Lucide.Sparkles
        DisclosureType.Advertisements -> Lucide.Megaphone
        DisclosureType.EpilepsyTriggers -> Lucide.Eye
        DisclosureType.SystemInteractions -> Lucide.CircuitBoard
        DisclosureType.Telemetry -> Lucide.RadioTower
        DisclosureType.DerivativeWork -> Lucide.GitFork
        DisclosureType.PaidFeatures -> Lucide.CircleDollarSign
        DisclosureType.Archived -> Lucide.Archive
    }

@Composable
internal fun DisclosureType.title(): String = stringResource(
    when (this) {
        DisclosureType.AiContent -> R.string.disclosures_ai_title
        DisclosureType.Advertisements -> R.string.disclosures_ads_title
        DisclosureType.EpilepsyTriggers -> R.string.disclosures_epilepsy_title
        DisclosureType.SystemInteractions -> R.string.disclosures_system_title
        DisclosureType.Telemetry -> R.string.disclosures_telemetry_title
        DisclosureType.DerivativeWork -> R.string.disclosures_derivative_title
        DisclosureType.PaidFeatures -> R.string.disclosures_paid_title
        DisclosureType.Archived -> R.string.disclosures_archived_title
    },
)

@Composable
internal fun DisclosureType.description(): String = stringResource(
    when (this) {
        DisclosureType.AiContent -> R.string.disclosures_ai_description
        DisclosureType.Advertisements -> R.string.disclosures_ads_description
        DisclosureType.EpilepsyTriggers -> R.string.disclosures_epilepsy_description
        DisclosureType.SystemInteractions -> R.string.disclosures_system_description
        DisclosureType.Telemetry -> R.string.disclosures_telemetry_description
        DisclosureType.DerivativeWork -> R.string.disclosures_derivative_description
        DisclosureType.PaidFeatures -> R.string.disclosures_paid_description
        DisclosureType.Archived -> R.string.disclosures_archived_description
    },
)

@Composable
internal fun AiUsage.label(): String = stringResource(
    when (this) {
        AiUsage.Code -> R.string.disclosures_ai_use_code
        AiUsage.Assets -> R.string.disclosures_ai_use_assets
        AiUsage.Text -> R.string.disclosures_ai_use_text
        AiUsage.Functionality -> R.string.disclosures_ai_use_functionality
    },
)

@Composable
internal fun TelemetryConsent.label(): String = stringResource(
    when (this) {
        TelemetryConsent.OptIn -> R.string.disclosures_telemetry_consent_opt_in
        TelemetryConsent.OptOut -> R.string.disclosures_telemetry_consent_opt_out
        TelemetryConsent.AlwaysActive -> R.string.disclosures_telemetry_consent_always
    },
)

@Composable
internal fun DisclosureIssue.message(): String = stringResource(
    when (this) {
        DisclosureIssue.AdvertisingNote -> R.string.disclosures_issue_ads_note
        DisclosureIssue.PhotosensitivityNote -> R.string.disclosures_issue_epilepsy_note
        DisclosureIssue.SystemInteractionsNote -> R.string.disclosures_issue_system_note
        DisclosureIssue.TelemetryEmpty -> R.string.disclosures_issue_telemetry_empty
        DisclosureIssue.DerivativeEmpty -> R.string.disclosures_issue_derivative_empty
        DisclosureIssue.DerivativeSourceLabel -> R.string.disclosures_issue_derivative_label
        DisclosureIssue.PaidFeaturesEmpty -> R.string.disclosures_issue_paid_empty
    },
)
