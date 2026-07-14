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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.UsersRound
import com.rinthy.mobile.MemberSearchState
import com.rinthy.mobile.ProjectActionState
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyEmptyState
import com.rinthy.mobile.ui.components.RinthyIcon
import com.rinthy.mobile.ui.components.RinthySectionLabel
import com.rinthy.mobile.ui.dashboard.project.members.InviteMemberDialog
import com.rinthy.mobile.ui.dashboard.project.members.MemberEditorDialog
import com.rinthy.mobile.ui.dashboard.project.members.MembersHeader
import com.rinthy.mobile.ui.dashboard.project.members.ProjectMemberCard
import com.rinthy.mobile.ui.dashboard.projects.ProjectBannerCard
import com.rinthy.mobile.ui.dashboard.projects.toProjectRowModel
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.OrganizationPermissionBits
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate
import com.rinthy.shared.model.canManageOrganizationMembers
import com.rinthy.shared.model.hasOrganizationPermission

@Composable
fun OrganizationDetailScreen(
    organization: Organization,
    projects: List<Project>,
    members: List<ProjectMember> = organization.members,
    currentUserId: String? = null,
    isLoading: Boolean,
    errorMessage: String?,
    projectAction: ProjectActionState = ProjectActionState(),
    memberSearch: MemberSearchState = MemberSearchState(),
    onProjectClick: (Project) -> Unit,
    onSearchMember: (String) -> Unit = {},
    onInviteMember: (String, String) -> Unit = { _, _ -> },
    onUpdateMember: (String, String, ProjectMemberUpdate) -> Unit = { _, _, _ -> },
    onRemoveMember: (String, String) -> Unit = { _, _ -> },
    onJoinTeam: (String) -> Unit = {},
    onTransferOwnership: (String, String) -> Unit = { _, _ -> },
    onClearProjectActionStatus: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val displayMembers = members.ifEmpty { organization.acceptedMembers }
    val shape = RoundedCornerShape(12.dp)
    val teamId = organization.teamId
    val currentMember = remember(displayMembers, currentUserId) {
        displayMembers.firstOrNull { it.user.id == currentUserId }
    }
    val canManage = currentMember.canManageOrganizationMembers() || currentMember?.isOwner == true
    val canInvite = canManage &&
        (currentMember?.isOwner == true || currentMember.hasOrganizationPermission(OrganizationPermissionBits.ManageInvites))
    var isInviting by remember(organization.id) { mutableStateOf(false) }
    var editingMember by remember(organization.id) { mutableStateOf<ProjectMember?>(null) }

    LaunchedEffect(projectAction.successMessage) {
        if (projectAction.successMessage != null) {
            isInviting = false
            editingMember = null
            onClearProjectActionStatus()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = RinthyDesign.bottomContentPadding,
        ),
    ) {
        item(key = "org-header", contentType = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(RinthyDesign.colors.surface)
                    .border(0.75.dp, RinthyDesign.colors.separator, shape)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrganizationIcon(organization.iconUrl, organization.name, Modifier.size(72.dp))
                    Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(
                            organization.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "@${organization.slug}",
                            color = RinthyDesign.colors.accent,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                if (organization.description.isNotBlank()) {
                    Text(
                        organization.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    OrgMetricChip(
                        icon = Lucide.UsersRound,
                        label = stringResource(R.string.organizations_members),
                        value = displayMembers.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    OrgMetricChip(
                        icon = Lucide.Package,
                        label = stringResource(R.string.analytics_projects),
                        value = if (isLoading && projects.isEmpty()) "—" else projects.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RinthyDesign.colors.surfaceRaised)
                        .clickable {
                            uriHandler.openUri("https://modrinth.com/organization/${organization.slug}")
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        stringResource(R.string.organizations_open_page),
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
        }

        item(key = "org-members-heading", contentType = "heading") {
            MembersHeader(
                title = stringResource(R.string.organizations_members),
                canInvite = canInvite && teamId != null,
                onInvite = { isInviting = true },
            )
            Text(
                text = stringResource(R.string.organizations_members_manage_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        projectAction.errorMessage?.let { message ->
            item(key = "org-members-action-error", contentType = "error") {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }

        when {
            isLoading && displayMembers.isEmpty() -> {
                item(key = "org-members-loading", contentType = "loading") {
                    LoadingRow(stringResource(R.string.organizations_loading_members))
                }
            }
            displayMembers.isEmpty() -> {
                item(key = "org-members-empty", contentType = "empty") {
                    RinthyEmptyState(
                        title = stringResource(R.string.organizations_members_empty),
                        message = if (teamId == null) {
                            stringResource(R.string.organizations_members_no_team)
                        } else {
                            stringResource(R.string.organizations_members_empty_hint)
                        },
                    )
                }
            }
            else -> {
                items(displayMembers, key = { it.user.id }, contentType = { "member" }) { member ->
                    Box(modifier = Modifier.animateItem()) {
                        ProjectMemberCard(
                            member = member,
                            canManage = canManage && teamId != null,
                            isCurrentUser = member.user.id == currentUserId,
                            isBusy = projectAction.isRunning && projectAction.targetId == member.user.id,
                            onEdit = { editingMember = member },
                            onRemove = {
                                teamId?.let { onRemoveMember(it, member.user.id) }
                            },
                            onJoin = { teamId?.let(onJoinTeam) },
                        )
                    }
                }
            }
        }

        item(key = "org-projects-heading", contentType = "heading") {
            RinthySectionLabel(
                text = pluralStringResource(
                    R.plurals.overview_project_count,
                    projects.size.coerceAtLeast(1),
                    projects.size,
                ),
                modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
            )
        }

        when {
            isLoading && projects.isEmpty() -> {
                item(key = "org-projects-loading", contentType = "loading") {
                    LoadingRow(stringResource(R.string.organizations_loading_projects))
                }
            }
            errorMessage != null && projects.isEmpty() -> {
                item(key = "org-projects-error", contentType = "empty") {
                    RinthyEmptyState(
                        title = stringResource(R.string.organizations_projects_failed),
                        message = errorMessage,
                    )
                }
            }
            projects.isEmpty() -> {
                item(key = "org-projects-empty", contentType = "empty") {
                    RinthyEmptyState(
                        title = stringResource(R.string.organizations_projects_empty),
                        message = stringResource(R.string.organizations_projects_empty_hint),
                    )
                }
            }
            else -> {
                items(projects, key = Project::id, contentType = { "project" }) { project ->
                    Box(
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 10.dp),
                    ) {
                        ProjectBannerCard(
                            model = project.toProjectRowModel(),
                            isFavorite = false,
                            onFavoriteClick = null,
                            onClick = { onProjectClick(project) },
                        )
                    }
                }
            }
        }
    }

    if (isInviting && teamId != null) {
        InviteMemberDialog(
            search = memberSearch,
            isSaving = projectAction.isRunning,
            errorMessage = projectAction.errorMessage,
            onQueryChange = onSearchMember,
            onInvite = { onInviteMember(teamId, it) },
            onDismiss = {
                isInviting = false
                onClearProjectActionStatus()
            },
        )
    }

    editingMember?.let { member ->
        teamId?.let { resolvedTeamId ->
            MemberEditorDialog(
                member = member,
                isSaving = projectAction.isRunning,
                errorMessage = projectAction.errorMessage,
                showOrganizationPermissions = true,
                onSave = { update -> onUpdateMember(resolvedTeamId, member.user.id, update) },
                onTransferOwnership = { onTransferOwnership(resolvedTeamId, member.user.id) },
                onDismiss = {
                    editingMember = null
                    onClearProjectActionStatus()
                },
            )
        }
    }
}

@Composable
private fun OrgMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RinthyDesign.colors.surfaceRaised)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        RinthyIcon(icon, null, RinthyDesign.colors.accent, Modifier.size(16.dp))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
