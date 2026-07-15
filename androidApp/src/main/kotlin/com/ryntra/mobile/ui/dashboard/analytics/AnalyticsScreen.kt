package com.ryntra.mobile.ui.dashboard.analytics

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ImageOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Wallet
import com.ryntra.mobile.R
import com.ryntra.mobile.AnalyticsState
import com.ryntra.mobile.ui.components.RyntraSectionLabel
import com.ryntra.mobile.ui.components.formatExactCount
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.shared.model.AnalyticsMetrics
import com.ryntra.shared.model.AnalyticsReport
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Project
import java.util.Locale
import kotlin.math.roundToLong

internal enum class AnalyticsMetric(
    @StringRes val labelRes: Int,
    val label: String,
) {
    Downloads(R.string.analytics_downloads, "Downloads"),
    Views(R.string.analytics_views, "Views"),
    Playtime(R.string.analytics_playtime, "Playtime"),
    Revenue(R.string.analytics_revenue, "Revenue"),
}

internal data class ProjectInsight(
    val project: Project,
    val metrics: AnalyticsMetrics,
)

private data class HealthIssue(
    val id: String,
    val label: String,
    val detail: String,
    val count: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val nonPublicStatuses = setOf("draft", "unlisted", "archived", "processing", "rejected")

@Composable
fun AnalyticsScreen(
    dashboard: Dashboard,
    state: AnalyticsState,
    onRangeChange: (Int) -> Unit,
) {
    var selectedMetricName by rememberSaveable { mutableStateOf(AnalyticsMetric.Downloads.name) }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMetric = AnalyticsMetric.valueOf(selectedMetricName)
    val projects = dashboard.projects
    val projectIds = remember(projects) { projects.map(Project::id) }
    val report = state.report
    val wallet = state.wallet
    val isCoreAvailable = report?.isCoreAvailable != false
    val uriHandler = LocalUriHandler.current
    val lifetimeDownloads = remember(projects) { projects.sumOf(Project::downloads) }
    val lifetimeFollowers = remember(projects) { projects.sumOf(Project::followers) }
    val currentMetrics = remember(report, selectedProjectId) {
        selectedProjectId?.let { report?.projectMetrics(it) } ?: report?.periodTotals ?: AnalyticsMetrics()
    }
    val previousMetrics = remember(report, selectedProjectId) {
        selectedProjectId?.let { report?.previousProjectMetrics(it) } ?: report?.previousPeriodTotals ?: AnalyticsMetrics()
    }
    val insights = remember(projects, report, selectedMetric, selectedProjectId) {
        projects
            .filter { selectedProjectId == null || it.id == selectedProjectId }
            .map { project -> ProjectInsight(project, report?.projectMetrics(project.id) ?: AnalyticsMetrics()) }
            .sortedByDescending { selectedMetric.value(it.metrics) }
    }
    val health = projectHealth(projects)

    LaunchedEffect(projectIds, state.rangeDays) {
        if (selectedProjectId !in projectIds) selectedProjectId = null
        onRangeChange(state.rangeDays)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = RyntraDesign.bottomContentPadding,
        ),
    ) {
        item(key = "analytics-range", contentType = "controls") {
            AnalyticsRangeHeader(
                selectedDays = state.rangeDays,
                isLoading = state.isLoading,
                isLive = report?.isCoreAvailable == true,
                onSelect = onRangeChange,
            )
        }
        item(key = "analytics-wallet-title", contentType = "heading") {
            RyntraSectionLabel(stringResource(R.string.analytics_wallet), modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
        }
        item(key = "analytics-wallet", contentType = "wallet") {
            WalletSummary(
                report = state.wallet,
                isLoading = state.isLoading && state.wallet == null,
                errorMessage = state.walletErrorMessage,
                onOpenRevenue = { uriHandler.openUri("https://modrinth.com/dashboard/revenue") },
            )
        }
        if (wallet != null && wallet.transactions.isNotEmpty()) {
            item(key = "analytics-payouts-title", contentType = "heading") {
                RyntraSectionLabel(stringResource(R.string.analytics_recent_payouts), modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
            }
            itemsIndexed(
                items = wallet.transactions.take(5),
                key = { index, payout -> "${payout.created}-${payout.amount}-${payout.status}-$index" },
                contentType = { _, _ -> "payout" },
            ) { _, payout ->
                PayoutTransactionRow(payout, wallet.currency)
            }
        }
        item(key = "analytics-period-title", contentType = "heading") {
            RyntraSectionLabel(
                text = stringResource(R.string.analytics_last_days, state.rangeDays),
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
            )
            AnalyticsProjectPicker(
                projects = projects,
                selectedProjectId = selectedProjectId,
                onSelect = { selectedProjectId = it },
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (report != null && !report.isCoreAvailable) {
                AnalyticsNotice(report.analyticsAvailabilityMessage())
            } else if (state.errorMessage != null) {
                AnalyticsNotice(state.errorMessage)
            }
        }
        item(key = "analytics-period", contentType = "metrics") {
            AnalyticsMetricRow(
                first = MetricValue(
                    AnalyticsMetric.Downloads,
                    Lucide.Download,
                    stringResource(R.string.analytics_downloads),
                    if (isCoreAvailable) formatMetric(AnalyticsMetric.Downloads, currentMetrics.downloads) else stringResource(R.string.analytics_unavailable),
                    if (isCoreAvailable) report?.percentageChange(currentMetrics.downloads, previousMetrics.downloads) else null,
                ),
                second = MetricValue(
                    AnalyticsMetric.Views,
                    Lucide.Eye,
                    stringResource(R.string.analytics_views),
                    if (isCoreAvailable) formatMetric(AnalyticsMetric.Views, currentMetrics.views) else stringResource(R.string.analytics_unavailable),
                    if (isCoreAvailable) report?.percentageChange(currentMetrics.views, previousMetrics.views) else null,
                ),
                selectedMetric = selectedMetric,
                onMetricSelect = { selectedMetricName = it.name },
            )
            AnalyticsMetricRow(
                first = MetricValue(
                    AnalyticsMetric.Playtime,
                    Lucide.Timer,
                    stringResource(R.string.analytics_playtime),
                    if (isCoreAvailable) formatMetric(AnalyticsMetric.Playtime, currentMetrics.playtimeSeconds) else stringResource(R.string.analytics_unavailable),
                    if (isCoreAvailable) report?.percentageChange(currentMetrics.playtimeSeconds, previousMetrics.playtimeSeconds) else null,
                ),
                second = MetricValue(
                    AnalyticsMetric.Revenue,
                    Lucide.Wallet,
                    stringResource(R.string.analytics_revenue),
                    if (report?.isRevenueAvailable == true) formatMetric(AnalyticsMetric.Revenue, currentMetrics.revenue) else stringResource(R.string.analytics_unavailable),
                    if (report?.isRevenueAvailable == true) {
                        report.percentageChange(currentMetrics.revenue, previousMetrics.revenue)
                    } else {
                        null
                    },
                ),
                modifier = Modifier.padding(top = 10.dp),
                selectedMetric = selectedMetric,
                onMetricSelect = { selectedMetricName = it.name },
            )
        }
        item(key = "analytics-trend-title", contentType = "heading") {
            RyntraSectionLabel(stringResource(R.string.analytics_trend), modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
            AnalyticsMetricPicker(selectedMetric) { selectedMetricName = it.name }
        }
        item(key = "analytics-trend", contentType = "chart") {
            AnalyticsTrend(
                report = report,
                projects = projects,
                selectedProjectId = selectedProjectId,
                metric = selectedMetric,
                rangeDays = state.rangeDays,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        item(key = "analytics-projects-title", contentType = "heading") {
            RyntraSectionLabel(stringResource(R.string.analytics_breakdown), modifier = Modifier.padding(top = 30.dp, bottom = 4.dp))
            Text(
                text = stringResource(R.string.analytics_breakdown_hint),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (insights.isEmpty()) {
            item(key = "analytics-projects-empty", contentType = "empty") {
                AnalyticsNotice(
                    if (report?.isCoreAvailable == true) stringResource(R.string.analytics_no_activity)
                    else stringResource(R.string.analytics_project_data_unavailable),
                )
            }
        } else {
            items(
                items = insights,
                key = { "${selectedMetric.name}-${it.project.id}" },
                contentType = { "project-insight" },
            ) { insight ->
                Box(modifier = Modifier.animateItem()) {
                    AnalyticsBreakdownRow(
                        insight = insight,
                        previousMetrics = report?.previousProjectMetrics(insight.project.id) ?: AnalyticsMetrics(),
                        metric = selectedMetric,
                        share = selectedMetric.value(insight.metrics) /
                            selectedMetric.value(currentMetrics).coerceAtLeast(1.0),
                    )
                }
            }
        }
        item(key = "analytics-lifetime-title", contentType = "heading") {
            RyntraSectionLabel(stringResource(R.string.analytics_lifetime), modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
            Text(
                text = stringResource(R.string.analytics_lifetime_hint),
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        item(key = "analytics-lifetime", contentType = "metrics") {
            AnalyticsMetricRow(
                first = MetricValue(null, Lucide.Download, stringResource(R.string.analytics_downloads), formatExact(lifetimeDownloads)),
                second = MetricValue(null, Lucide.Heart, stringResource(R.string.analytics_followers), formatExact(lifetimeFollowers)),
            )
            AnalyticsMetricRow(
                first = MetricValue(null, Lucide.Package, stringResource(R.string.analytics_projects), formatExact(projects.size.toLong())),
                second = MetricValue(null, Lucide.Activity, stringResource(R.string.analytics_active), formatExact(projects.count { it.status == "approved" }.toLong())),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        item(key = "analytics-health-title", contentType = "heading") {
            RyntraSectionLabel(stringResource(R.string.analytics_project_health), modifier = Modifier.padding(top = 30.dp, bottom = 8.dp))
        }
        items(health, key = HealthIssue::id, contentType = { "health" }) { issue ->
            HealthRow(issue.icon, issue.label, issue.detail, issue.count)
        }
    }
}

internal fun AnalyticsMetric.value(metrics: AnalyticsMetrics): Double = when (this) {
    AnalyticsMetric.Downloads -> metrics.downloads
    AnalyticsMetric.Views -> metrics.views
    AnalyticsMetric.Playtime -> metrics.playtimeSeconds
    AnalyticsMetric.Revenue -> metrics.revenue
}

internal fun formatMetric(metric: AnalyticsMetric, value: Double): String = when (metric) {
    AnalyticsMetric.Downloads, AnalyticsMetric.Views -> formatExact(value.roundToLong())
    AnalyticsMetric.Playtime -> formatPlaytime(value)
    AnalyticsMetric.Revenue -> String.format(Locale.US, "$%.2f", value)
}

private fun formatExact(value: Long): String = formatExactCount(value)

private fun formatPlaytime(seconds: Double): String {
    val totalMinutes = (seconds / 60.0).roundToLong().coerceAtLeast(0)
    if (totalMinutes < 60) return "${formatExact(totalMinutes)}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${formatExact(hours)}h" else "${formatExact(hours)}h ${minutes}m"
}

@Composable
private fun AnalyticsReport.analyticsAvailabilityMessage(): String = when (coreStatus) {
    401 -> stringResource(R.string.analytics_status_fresh_sign_in)
    403 -> stringResource(R.string.analytics_status_permission)
    429 -> stringResource(R.string.analytics_status_rate_limit)
    0 -> stringResource(R.string.analytics_status_decode)
    else -> stringResource(R.string.analytics_status_failed, coreStatus)
}

@Composable
private fun projectHealth(projects: List<Project>): List<HealthIssue> {
    val hidden = projects.filter { it.status in nonPublicStatuses }
    val missingIcons = projects.filter { it.iconUrl.isNullOrBlank() }
    val weakDescriptions = projects.filter { it.body.trim().length < 160 }
    return listOf(
        HealthIssue(
            id = "visibility",
            label = stringResource(R.string.analytics_health_visibility),
            detail = hidden.take(3).joinToString { it.title }.ifBlank { stringResource(R.string.analytics_health_visibility_ok) },
            count = hidden.size,
            icon = Lucide.Activity,
        ),
        HealthIssue(
            id = "icons",
            label = stringResource(R.string.analytics_health_icons),
            detail = missingIcons.take(3).joinToString { it.title }.ifBlank { stringResource(R.string.analytics_health_icons_ok) },
            count = missingIcons.size,
            icon = Lucide.ImageOff,
        ),
        HealthIssue(
            id = "descriptions",
            label = stringResource(R.string.analytics_health_descriptions),
            detail = weakDescriptions.take(3).joinToString { it.title }.ifBlank { stringResource(R.string.analytics_health_descriptions_ok) },
            count = weakDescriptions.size,
            icon = Lucide.FileText,
        ),
    )
}
