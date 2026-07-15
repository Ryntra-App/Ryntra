package com.ryntra.mobile.ui.dashboard.overview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.dashboard.projects.ProjectRow
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Project

@Composable
fun OverviewScreen(
    dashboard: Dashboard,
    onProjectClick: (Project) -> Unit = {},
) {
    val snapshot = remember(dashboard.projects) { dashboard.createOverviewSnapshot() }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = RyntraDesign.bottomContentPadding,
        ),
    ) {
        item(key = "overview-summary", contentType = "summary") {
            CreatorSummary(
                username = dashboard.account.username,
                projectCount = dashboard.projects.size,
                organizationCount = dashboard.organizations.size,
                snapshot = snapshot,
            )
        }
        item(key = "overview-attention-heading", contentType = "heading") {
            OverviewSectionHeader(
                title = stringResource(R.string.overview_needs_attention),
                supportingText = snapshot.attentionCount.takeIf { it > 0 }?.let {
                    pluralStringResource(R.plurals.overview_action_count, it, it)
                },
            )
        }
        if (snapshot.attentionProjects.isEmpty()) {
            item(key = "overview-all-clear", contentType = "status") { AllClearRow() }
        } else {
            snapshot.attentionProjects.forEach { project ->
                item(key = "attention-${project.id}", contentType = "attention") {
                    AttentionRow(project = project, onClick = { onProjectClick(project) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (snapshot.inReviewProjects.isNotEmpty()) {
            item(key = "overview-review-heading", contentType = "heading") {
                OverviewSectionHeader(
                    title = stringResource(R.string.overview_in_review),
                    supportingText = pluralStringResource(
                        R.plurals.overview_in_review_count,
                        snapshot.inReviewCount,
                        snapshot.inReviewCount,
                    ),
                )
            }
            snapshot.inReviewProjects.forEach { project ->
                item(key = "review-${project.id}", contentType = "review") {
                    InReviewRow(project = project, onClick = { onProjectClick(project) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        snapshot.leadingProject?.let { project ->
            item(key = "overview-leader-heading", contentType = "heading") {
                OverviewSectionHeader(
                    stringResource(R.string.overview_portfolio_leader),
                    stringResource(R.string.overview_most_downloaded),
                )
            }
            item(key = "overview-leader-${project.id}", contentType = "leader") {
                LeadingProjectRow(
                    project = project,
                    totalDownloads = snapshot.totalDownloads,
                    onClick = { onProjectClick(project) },
                )
            }
        }
        item(key = "overview-recent-heading", contentType = "heading") {
            OverviewSectionHeader(
                stringResource(R.string.overview_recently_updated),
                stringResource(R.string.overview_latest_activity),
            )
        }
        if (snapshot.recentProjects.isEmpty()) {
            item(key = "overview-empty", contentType = "empty") {
                RyntraEmptyState(
                    title = stringResource(R.string.projects_empty),
                    message = stringResource(R.string.projects_empty_hint),
                )
            }
        } else {
            snapshot.recentProjects.forEach { project ->
                item(key = "recent-${project.id}", contentType = "project") {
                    ProjectRow(
                        project = project,
                        showDescription = false,
                        showStatus = false,
                        onClick = { onProjectClick(project) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (snapshot.projectTypes.isNotEmpty()) {
            item(key = "overview-mix-heading", contentType = "heading") {
                OverviewSectionHeader(stringResource(R.string.overview_portfolio_mix))
            }
            snapshot.projectTypes.forEachIndexed { index, item ->
                item(key = "overview-mix-${item.kind.name}", contentType = "mix") {
                    PortfolioMixRow(
                        item = item,
                        totalProjects = dashboard.projects.size,
                        showDivider = index < snapshot.projectTypes.lastIndex,
                    )
                }
            }
        }
    }
}
