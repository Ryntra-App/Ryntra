package com.ryntra.mobile.ui.dashboard.project.moderation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheckBig
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Clock3
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraPrimaryButton
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectSubmissionRequirement
import java.text.DateFormat
import java.time.Instant
import java.util.Date

@Composable
internal fun ProjectSubmissionStatus(
    project: Project,
    versionCount: Int,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit,
) {
    val readiness = project.moderationReadiness(versionCount)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
    ) {
        Text(
            text = stringResource(R.string.project_submission_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        when {
            project.canSubmitForModeration() && readiness.canSubmit -> {
                SubmissionMessage(
                    text = stringResource(R.string.project_submit_ready),
                    isComplete = true,
                )
                RyntraPrimaryButton(
                    text = stringResource(
                        if (project.status.lowercase() in setOf("rejected", "withheld")) {
                            R.string.project_action_resubmit
                        } else {
                            R.string.project_action_submit
                        },
                    ),
                    icon = Lucide.Send,
                    onClick = onSubmit,
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                )
            }

            project.canSubmitForModeration() -> {
                Text(
                    text = stringResource(R.string.project_submit_remaining),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                readiness.missingRequirements.forEach { requirement ->
                    SubmissionMessage(
                        text = submissionRequirementLabel(requirement),
                        isComplete = false,
                    )
                }
            }

            project.status.lowercase() == "processing" -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(Lucide.Clock3, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.project_status_processing), fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    text = stringResource(R.string.project_submit_in_review),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                project.queued?.let { queuedAt ->
                    Text(
                        text = stringResource(
                            R.string.project_submission_queued_on,
                            queuedAt.toLocalDate(LocalContext.current),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            else -> SubmissionMessage(
                text = stringResource(R.string.project_submission_not_available),
                isComplete = true,
            )
        }

        if (!canSubmit && project.canSubmitForModeration() && readiness.canSubmit) {
            Text(
                text = stringResource(R.string.project_submission_permission_required),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SubmissionMessage(text: String, isComplete: Boolean) {
    val color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (isComplete) Lucide.CircleCheckBig else Lucide.CircleAlert,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun submissionRequirementLabel(requirement: ProjectSubmissionRequirement): String = stringResource(
    when (requirement) {
        ProjectSubmissionRequirement.Version -> R.string.project_submit_requirement_version
        ProjectSubmissionRequirement.Icon -> R.string.project_submit_requirement_icon
        ProjectSubmissionRequirement.Summary -> R.string.project_submit_requirement_summary
        ProjectSubmissionRequirement.Description -> R.string.project_submit_requirement_description
        ProjectSubmissionRequirement.License -> R.string.project_submit_requirement_license
    },
)

private fun String.toLocalDate(context: Context): String = runCatching {
    DateFormat.getDateInstance(DateFormat.MEDIUM, context.resources.configuration.locales[0])
        .format(Date(Instant.parse(this).toEpochMilli()))
}.getOrElse { take(10) }
