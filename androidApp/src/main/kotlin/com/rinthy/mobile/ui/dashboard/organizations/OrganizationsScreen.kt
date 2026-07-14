package com.rinthy.mobile.ui.dashboard.organizations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyEmptyState
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.components.RinthySearchField
import com.rinthy.mobile.ui.components.RinthySectionLabel
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.Organization

@Composable
fun OrganizationsScreen(
    organizations: List<Organization>,
    onOrganizationClick: (Organization) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val visible = remember(organizations, query) {
        organizations.filter { org ->
            query.isBlank() ||
                org.name.contains(query, ignoreCase = true) ||
                org.slug.contains(query, ignoreCase = true) ||
                org.description.contains(query, ignoreCase = true)
        }
    }
    val totalMembers = remember(organizations) {
        organizations.sumOf { it.memberCount.coerceAtLeast(0) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = RinthyDesign.bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "teams-summary", contentType = "summary") {
            TeamsSummaryBand(
                teamCount = organizations.size,
                memberCount = totalMembers,
            )
            RinthySearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.organizations_search),
                leadingIcon = Lucide.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }

        item(key = "teams-heading", contentType = "heading") {
            RinthySectionLabel(
                text = stringResource(R.string.organizations_managed),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.organizations_managed_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (visible.isEmpty()) {
            item(key = "teams-empty", contentType = "empty") {
                RinthyEmptyState(
                    title = stringResource(
                        if (organizations.isEmpty()) R.string.organizations_empty else R.string.organizations_no_matches,
                    ),
                    message = stringResource(
                        if (organizations.isEmpty()) {
                            R.string.organizations_empty_hint
                        } else {
                            R.string.organizations_no_matches_hint
                        },
                    ),
                )
                if (organizations.isEmpty()) {
                    OpenOrganizationsOnModrinth(
                        onClick = { uriHandler.openUri("https://modrinth.com/dashboard/organizations") },
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        } else {
            items(visible, key = Organization::id, contentType = { "organization" }) { organization ->
                Box(modifier = Modifier.animateItem()) {
                    OrganizationCard(
                        organization = organization,
                        onClick = { onOrganizationClick(organization) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamsSummaryBand(teamCount: Int, memberCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surface)
            .border(0.75.dp, RinthyDesign.colors.separator, RoundedCornerShape(10.dp))
            .padding(vertical = 13.dp),
    ) {
        SummaryMetric(
            value = teamCount.toString(),
            label = stringResource(R.string.nav_teams),
            modifier = Modifier.weight(1f),
        )
        SummaryMetric(
            value = memberCount.toString(),
            label = stringResource(R.string.organizations_members),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun OrganizationCard(
    organization: Organization,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val members = organization.acceptedMembers
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RinthyDesign.colors.surface)
            .border(0.75.dp, RinthyDesign.colors.separator, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrganizationIcon(organization.iconUrl, organization.name, Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    organization.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${organization.slug}",
                    color = RinthyDesign.colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (organization.description.isNotBlank()) {
                    Text(
                        organization.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            RinthyIcon(
                Lucide.ChevronRight,
                contentDescription = null,
                tint = RinthyDesign.colors.labelSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            MemberAvatarStack(members = members, maxVisible = 5)
            Text(
                text = if (members.isEmpty()) {
                    stringResource(R.string.organizations_members_unknown)
                } else {
                    pluralStringResource(R.plurals.organizations_member_count, members.size, members.size)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun OrganizationIcon(iconUrl: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            name.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
internal fun MemberAvatarStack(
    members: List<com.rinthy.shared.model.ProjectMember>,
    maxVisible: Int = 5,
) {
    val visible = members.take(maxVisible)
    if (visible.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RinthyDesign.colors.accent.copy(alpha = 0.12f)),
            ) {
                RinthyIcon(Lucide.UsersRound, null, RinthyDesign.colors.accent, Modifier.size(14.dp))
            }
        }
        return
    }
    Box {
        visible.forEachIndexed { index, member ->
            AsyncImage(
                model = member.user.avatarUrl,
                contentDescription = member.user.username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = (index * 18).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, RinthyDesign.colors.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        if (members.size > maxVisible) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = (visible.size * 18).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, RinthyDesign.colors.surface, CircleShape),
            ) {
                Text(
                    "+${members.size - maxVisible}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun OpenOrganizationsOnModrinth(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(R.string.organizations_open_modrinth),
            color = RinthyDesign.colors.accent,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
        RinthyIcon(
            Lucide.ExternalLink,
            null,
            RinthyDesign.colors.accent,
            Modifier.padding(start = 8.dp).size(16.dp),
        )
    }
}
