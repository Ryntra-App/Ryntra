package com.rinthy.mobile.ui.dashboard.project

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Server
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.mobile.ui.components.RinthyEmptyState
import com.rinthy.mobile.ui.dashboard.project.edit.EditProjectContent
import com.rinthy.mobile.ui.dashboard.project.gallery.ProjectGalleryOverviewStrip
import com.rinthy.mobile.ui.dashboard.project.gallery.ProjectGalleryViewerDialog
import com.rinthy.mobile.ui.dashboard.project.markdown.MarkdownBlockView
import com.rinthy.mobile.ui.dashboard.project.members.InviteMemberDialog
import com.rinthy.mobile.ui.dashboard.project.members.MemberEditorDialog
import com.rinthy.mobile.ui.dashboard.project.members.MembersHeader
import com.rinthy.mobile.ui.dashboard.project.members.ProjectMemberCard
import com.rinthy.mobile.ui.dashboard.project.overview.CategoryChip
import com.rinthy.mobile.ui.dashboard.project.overview.DetailHeading
import com.rinthy.mobile.ui.dashboard.project.overview.DetailSection
import com.rinthy.mobile.ui.dashboard.project.overview.DetailValue
import com.rinthy.mobile.ui.dashboard.project.overview.EnvironmentValue
import com.rinthy.mobile.ui.dashboard.project.overview.LoadingMembers
import com.rinthy.mobile.ui.dashboard.project.overview.ProjectDependencyRow
import com.rinthy.mobile.ui.dashboard.project.overview.ProjectIdentity
import com.rinthy.mobile.ui.dashboard.project.overview.ProjectMetrics
import com.rinthy.mobile.ui.dashboard.project.overview.ProjectResource
import com.rinthy.mobile.ui.dashboard.project.overview.ResourceRow
import com.rinthy.mobile.ui.dashboard.project.versions.LoadingVersions
import com.rinthy.mobile.ui.dashboard.project.versions.VersionCard
import com.rinthy.mobile.ui.dashboard.project.versions.VersionEditorDialog
import com.rinthy.mobile.ui.dashboard.project.versions.VersionsHeader
import com.rinthy.shared.model.MarkdownParser
import com.rinthy.shared.model.MarkdownBlock
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.CreateVersionRequest
import com.rinthy.shared.model.ProjectDependency
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate
import com.rinthy.shared.model.ProjectVersion
import com.rinthy.shared.model.VersionUpdate
import com.rinthy.mobile.MemberSearchState
import com.rinthy.mobile.ProjectActionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectDetailScreen(
    project: Project,
    versions: List<ProjectVersion> = emptyList(),
    dependencies: List<ProjectDependency> = emptyList(),
    members: List<ProjectMember> = emptyList(),
    organizationMembers: List<ProjectMember> = emptyList(),
    organizationName: String? = null,
    currentUserId: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    memberErrorMessage: String? = null,
    projectUpdate: com.rinthy.mobile.ProjectUpdateState = com.rinthy.mobile.ProjectUpdateState(),
    onUpdateProject: (String, com.rinthy.shared.model.ProjectUpdate) -> Unit = { _, _ -> },
    onClearProjectUpdateStatus: () -> Unit = {},
    projectAction: ProjectActionState = ProjectActionState(),
    memberSearch: MemberSearchState = MemberSearchState(),
    onChangeProjectIcon: (String, ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteProjectIcon: (String) -> Unit = {},
    onAddGalleryImage: (String, ProjectFileUpload, Boolean, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteGalleryImage: (String, String) -> Unit = { _, _ -> },
    onSetGalleryBanner: (String, String) -> Unit = { _, _ -> },
    onModifyGalleryImage: (String, String, String, String, Int?) -> Unit = { _, _, _, _, _ -> },
    onCreateVersion: (String, CreateVersionRequest) -> Unit = { _, _ -> },
    onUpdateVersion: (String, VersionUpdate) -> Unit = { _, _ -> },
    onDeleteVersion: (String) -> Unit = {},
    onSearchMember: (String) -> Unit = {},
    onInviteMember: (String, String) -> Unit = { _, _ -> },
    onUpdateMember: (String, String, ProjectMemberUpdate) -> Unit = { _, _, _ -> },
    onRemoveMember: (String, String) -> Unit = { _, _ -> },
    onJoinTeam: (String) -> Unit = {},
    onTransferOwnership: (String, String) -> Unit = { _, _ -> },
    onClearProjectActionStatus: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    var selectedTab by rememberSaveable(project.id) { mutableStateOf(ProjectDetailTab.Overview) }
    val sourceLabel = stringResource(R.string.project_resource_source)
    val issuesLabel = stringResource(R.string.project_resource_issues)
    val wikiLabel = stringResource(R.string.project_resource_wiki)
    val discordLabel = stringResource(R.string.project_resource_discord)
    val resources = remember(project.sourceUrl, project.issuesUrl, project.wikiUrl, project.discordUrl, sourceLabel, issuesLabel, wikiLabel, discordLabel) {
        listOfNotNull(
            project.sourceUrl?.let { ProjectResource(sourceLabel, it) },
            project.issuesUrl?.let { ProjectResource(issuesLabel, it) },
            project.wikiUrl?.let { ProjectResource(wikiLabel, it) },
            project.discordUrl?.let { ProjectResource(discordLabel, it) },
        )
    }
    val isOrganizationProject = !project.organization.isNullOrBlank() || organizationMembers.isNotEmpty()
    val rosterMembers = if (isOrganizationProject) {
        organizationMembers.ifEmpty { members }
    } else {
        members
    }
    val markdownBlocks by produceState<List<MarkdownBlock>>(
        initialValue = emptyList(),
        key1 = project.body,
    ) {
        value = withContext(Dispatchers.Default) { MarkdownParser.parse(project.body) }
    }
    var isCreatingVersion by remember(project.id) { mutableStateOf(false) }
    var editingVersion by remember(project.id) { mutableStateOf<ProjectVersion?>(null) }
    var isInvitingMember by remember(project.id) { mutableStateOf(false) }
    var editingMember by remember(project.id) { mutableStateOf<ProjectMember?>(null) }
    var viewingGalleryImage by remember(project.id) { mutableStateOf<com.rinthy.shared.model.GalleryImage?>(null) }
    // Prefer project-team membership; fall back to org membership for permission checks.
    val currentMember = remember(members, organizationMembers, currentUserId) {
        members.firstOrNull { it.user.id == currentUserId }
            ?: organizationMembers.firstOrNull { it.user.id == currentUserId }
    }
    val canCreateVersions = currentMember.hasPermission(0)
    val canDeleteVersions = currentMember.hasPermission(1)
    val canManageMembers = currentMember.hasPermission(6) || currentMember?.isOwner == true
    val teamId = project.team

    LaunchedEffect(projectAction.successMessage) {
        if (projectAction.successMessage != null) {
            isCreatingVersion = false
            editingVersion = null
            isInvitingMember = false
            editingMember = null
            onClearProjectActionStatus()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
    ) {
        item(key = "identity", contentType = "identity") { ProjectIdentity(project) }
        item(key = "tabs", contentType = "tabs") {
            ProjectDetailTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }

        when (selectedTab) {
            ProjectDetailTab.Overview -> {
                item(key = "metrics", contentType = "metrics") { ProjectMetrics(project) }
                item(key = "summary", contentType = "text-section") {
                    DetailSection(
                        stringResource(R.string.project_summary),
                        project.description.ifBlank { stringResource(R.string.project_summary_empty) },
                    )
                }
                if (markdownBlocks.isNotEmpty()) {
                    item(key = "description-heading", contentType = "heading") {
                        DetailHeading(stringResource(R.string.project_description))
                    }
                    itemsIndexed(
                        items = markdownBlocks,
                        key = { index, _ -> "markdown-$index" },
                        contentType = { _, block -> block.type },
                    ) { _, block ->
                        Box(modifier = Modifier.padding(bottom = 9.dp)) { MarkdownBlockView(block) }
                    }
                }
                item {
                    DetailHeading(stringResource(R.string.project_environment))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EnvironmentValue(stringResource(R.string.project_client), project.clientSide, Lucide.Monitor, Modifier.weight(1f))
                        EnvironmentValue(stringResource(R.string.project_server), project.serverSide, Lucide.Server, Modifier.weight(1f))
                    }
                }
                if (project.categories.isNotEmpty()) {
                    item {
                        DetailHeading(stringResource(R.string.project_categories))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            project.categories.forEach { category -> CategoryChip(category) }
                        }
                    }
                }
                if (project.gallery.isNotEmpty()) {
                    item {
                        DetailHeading(stringResource(R.string.project_gallery))
                        ProjectGalleryOverviewStrip(
                            gallery = project.gallery,
                            onOpen = { viewingGalleryImage = it },
                        )
                    }
                }
                if (dependencies.isNotEmpty()) {
                    item(key = "dependencies-heading", contentType = "heading") {
                        DetailHeading(stringResource(R.string.project_dependencies))
                    }
                    items(dependencies, key = { it.projectId ?: it.versionId ?: it.fileName.orEmpty() }, contentType = { "dependency" }) {
                        ProjectDependencyRow(it)
                    }
                }
                if (project.license != null || project.published != null) {
                    item {
                        DetailHeading(stringResource(R.string.project_details))
                        project.license?.let {
                            DetailValue(Lucide.Scale, stringResource(R.string.project_license), it.name ?: it.id)
                        }
                        project.published?.let {
                            DetailValue(Lucide.CalendarDays, stringResource(R.string.project_published), it.take(10))
                        }
                    }
                }
                if (resources.isNotEmpty()) {
                    item {
                        DetailHeading(stringResource(R.string.project_resources))
                        resources.forEach { resource ->
                            ResourceRow(resource.label) { uriHandler.openUri(resource.url) }
                            HorizontalDivider(color = RinthyDesign.colors.separator)
                        }
                    }
                }
            }

            ProjectDetailTab.Versions -> {
                item(key = "versions-actions", contentType = "actions") {
                    VersionsHeader(canCreateVersions) { isCreatingVersion = true }
                }
                projectAction.errorMessage?.let { message ->
                    item(key = "versions-error", contentType = "error") { ProjectActionError(message) }
                }
                when {
                    isLoading -> item { LoadingVersions() }
                    errorMessage != null && versions.isEmpty() -> item {
                        RinthyEmptyState(
                            title = stringResource(R.string.project_versions_unavailable),
                            message = errorMessage,
                        )
                    }
                    versions.isEmpty() -> item {
                        RinthyEmptyState(
                            title = stringResource(R.string.project_versions_empty),
                            message = stringResource(R.string.project_versions_empty_hint),
                        )
                    }
                    else -> items(versions, key = ProjectVersion::id, contentType = { "version" }) { version ->
                        Box(modifier = Modifier.animateItem()) {
                            VersionCard(
                                version = version,
                                canEdit = canCreateVersions,
                                canDelete = canDeleteVersions,
                                isBusy = projectAction.isRunning && projectAction.targetId == version.id,
                                onOpen = { editingVersion = version },
                                onEdit = { editingVersion = version },
                                onDelete = { onDeleteVersion(version.id) },
                            )
                        }
                    }
                }
            }

            ProjectDetailTab.Edit -> item {
                EditProjectContent(
                    project = project,
                    projectUpdate = projectUpdate,
                    onUpdate = { onUpdateProject(project.id, it) },
                    onClearStatus = onClearProjectUpdateStatus,
                    actionState = projectAction,
                    onChangeIcon = { onChangeProjectIcon(project.id, it) },
                    onDeleteIcon = { onDeleteProjectIcon(project.id) },
                    onAddGalleryImage = { file, featured, title, description ->
                        onAddGalleryImage(project.id, file, featured, title, description)
                    },
                    onDeleteGalleryImage = { onDeleteGalleryImage(project.id, it) },
                    onSetGalleryBanner = { onSetGalleryBanner(project.id, it) },
                    onModifyGalleryImage = { url, title, description, ordering ->
                        onModifyGalleryImage(project.id, url, title, description, ordering)
                    },
                )
            }

            ProjectDetailTab.Members -> {
                // Org-owned projects share the org roster (read-only here). Personal projects use the project team.
                val canInviteHere = !isOrganizationProject && canManageMembers && teamId != null
                item(key = "members-actions", contentType = "actions") {
                    MembersHeader(
                        title = stringResource(R.string.project_members_title),
                        canInvite = canInviteHere,
                        onInvite = { isInvitingMember = true },
                    )
                    if (isOrganizationProject) {
                        Text(
                            text = stringResource(R.string.project_members_org_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                projectAction.errorMessage?.let { message ->
                    item(key = "members-error", contentType = "error") { ProjectActionError(message) }
                }
                when {
                    isLoading -> item { LoadingMembers() }
                    memberErrorMessage != null && rosterMembers.isEmpty() -> item {
                        RinthyEmptyState(
                            title = stringResource(R.string.project_members_unavailable),
                            message = memberErrorMessage,
                        )
                    }
                    rosterMembers.isEmpty() -> item {
                        RinthyEmptyState(
                            title = stringResource(R.string.project_members_empty),
                            message = stringResource(R.string.project_members_empty_hint),
                        )
                    }
                    else -> items(rosterMembers, key = { it.user.id }, contentType = { "member" }) { member ->
                        Box(modifier = Modifier.animateItem()) {
                            ProjectMemberCard(
                                member = member,
                                canManage = !isOrganizationProject && canManageMembers,
                                isCurrentUser = member.user.id == currentUserId,
                                isBusy = projectAction.isRunning && projectAction.targetId == member.user.id,
                                onEdit = { editingMember = member },
                                onRemove = { project.team?.let { onRemoveMember(it, member.user.id) } },
                                onJoin = { project.team?.let(onJoinTeam) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (isCreatingVersion) {
        VersionEditorDialog(
            version = null,
            suggestedGameVersions = versions.flatMap(ProjectVersion::gameVersions).distinct(),
            suggestedLoaders = versions.flatMap(ProjectVersion::loaders).distinct(),
            isSaving = projectAction.isRunning,
            errorMessage = projectAction.errorMessage,
            onDismiss = {
                isCreatingVersion = false
                onClearProjectActionStatus()
            },
            onCreate = { onCreateVersion(project.id, it) },
            onUpdate = { _, _ -> },
        )
    }
    editingVersion?.let { version ->
        VersionEditorDialog(
            version = version,
            suggestedGameVersions = versions.flatMap(ProjectVersion::gameVersions).distinct(),
            suggestedLoaders = versions.flatMap(ProjectVersion::loaders).distinct(),
            isSaving = projectAction.isRunning,
            errorMessage = projectAction.errorMessage,
            onDismiss = {
                editingVersion = null
                onClearProjectActionStatus()
            },
            onCreate = {},
            onUpdate = onUpdateVersion,
        )
    }
    if (isInvitingMember && teamId != null) {
        InviteMemberDialog(
            search = memberSearch,
            isSaving = projectAction.isRunning,
            errorMessage = projectAction.errorMessage,
            onQueryChange = onSearchMember,
            onInvite = { onInviteMember(teamId, it) },
            onDismiss = {
                isInvitingMember = false
                onClearProjectActionStatus()
            },
        )
    }
    editingMember?.let { member ->
        project.team?.let { teamId ->
            MemberEditorDialog(
                member = member,
                isSaving = projectAction.isRunning,
                errorMessage = projectAction.errorMessage,
                onSave = { onUpdateMember(teamId, member.user.id, it) },
                onTransferOwnership = { onTransferOwnership(teamId, member.user.id) },
                onDismiss = {
                    editingMember = null
                    onClearProjectActionStatus()
                },
            )
        }
    }
    viewingGalleryImage?.let { image ->
        ProjectGalleryViewerDialog(
            image = image,
            canManage = false,
            onDismiss = { viewingGalleryImage = null },
        )
    }
}

private fun ProjectMember?.hasPermission(bit: Int): Boolean =
    this != null && (isOwner || ((permissions ?: 0) and (1 shl bit)) != 0)

@Composable
private fun ProjectActionError(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}
