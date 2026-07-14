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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Server
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.mobile.ui.components.RinthyEmptyState
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
    onAddGalleryImage: (String, ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteGalleryImage: (String, String) -> Unit = { _, _ -> },
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
    val resources = remember(project.sourceUrl, project.issuesUrl, project.wikiUrl, project.discordUrl) {
        listOfNotNull(
            project.sourceUrl?.let { ProjectResource("Source", it) },
            project.issuesUrl?.let { ProjectResource("Issues", it) },
            project.wikiUrl?.let { ProjectResource("Wiki", it) },
            project.discordUrl?.let { ProjectResource("Discord", it) },
        )
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
                    DetailSection("Summary", project.description.ifBlank { "No summary provided." })
                }
                if (markdownBlocks.isNotEmpty()) {
                    item(key = "description-heading", contentType = "heading") { DetailHeading("Description") }
                    itemsIndexed(
                        items = markdownBlocks,
                        key = { index, _ -> "markdown-$index" },
                        contentType = { _, block -> block.type },
                    ) { _, block ->
                        Box(modifier = Modifier.padding(bottom = 9.dp)) { MarkdownBlockView(block) }
                    }
                }
                item {
                    DetailHeading("Environment")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EnvironmentValue("Client", project.clientSide, Lucide.Monitor, Modifier.weight(1f))
                        EnvironmentValue("Server", project.serverSide, Lucide.Server, Modifier.weight(1f))
                    }
                }
                if (project.categories.isNotEmpty()) {
                    item {
                        DetailHeading("Categories")
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
                        DetailHeading("Gallery")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(project.gallery, key = { it.url }) { image ->
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = image.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 172.dp, height = 108.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                }
                if (dependencies.isNotEmpty()) {
                    item(key = "dependencies-heading", contentType = "heading") { DetailHeading("Dependencies") }
                    items(dependencies, key = { it.projectId ?: it.versionId ?: it.fileName.orEmpty() }, contentType = { "dependency" }) {
                        ProjectDependencyRow(it)
                    }
                }
                if (project.license != null || project.published != null) {
                    item {
                        DetailHeading("Details")
                        project.license?.let {
                            DetailValue(Lucide.Scale, "License", it.name ?: it.id)
                        }
                        project.published?.let {
                            DetailValue(Lucide.CalendarDays, "Published", it.take(10))
                        }
                    }
                }
                if (resources.isNotEmpty()) {
                    item {
                        DetailHeading("Resources")
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
                        RinthyEmptyState(title = "Versions unavailable", message = errorMessage)
                    }
                    versions.isEmpty() -> item {
                        RinthyEmptyState(
                            title = "No versions yet",
                            message = "Published releases for this project will appear here.",
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
                    onAddGalleryImage = { onAddGalleryImage(project.id, it) },
                    onDeleteGalleryImage = { onDeleteGalleryImage(project.id, it) },
                )
            }

            ProjectDetailTab.Members -> {
                item(key = "members-actions", contentType = "actions") {
                    MembersHeader(
                        title = "Members",
                        canInvite = canManageMembers && teamId != null,
                        onInvite = { isInvitingMember = true },
                    )
                }
                projectAction.errorMessage?.let { message ->
                    item(key = "members-error", contentType = "error") { ProjectActionError(message) }
                }
                when {
                    isLoading -> item { LoadingMembers() }
                    memberErrorMessage != null && members.isEmpty() && organizationMembers.isEmpty() -> item {
                        RinthyEmptyState(title = "Members unavailable", message = memberErrorMessage)
                    }
                    members.isEmpty() && organizationMembers.isEmpty() -> item {
                        RinthyEmptyState(
                            title = "No members found",
                            message = "Team members for this project will appear here.",
                        )
                    }
                    else -> {
                        if (organizationMembers.isNotEmpty()) {
                            item(key = "org-members-heading", contentType = "heading") {
                                Text(
                                    text = organizationName?.let { "Organization · $it" } ?: "Organization members",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                )
                                Text(
                                    text = "These people manage the project through the organization.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            items(
                                organizationMembers,
                                key = { "org-${it.user.id}" },
                                contentType = { "org-member" },
                            ) { member ->
                                Box(modifier = Modifier.animateItem()) {
                                    // Org membership is managed on the Teams tab — show read-only here.
                                    ProjectMemberCard(
                                        member = member,
                                        canManage = false,
                                        isCurrentUser = member.user.id == currentUserId,
                                        isBusy = false,
                                        onEdit = {},
                                        onRemove = {},
                                        onJoin = {},
                                    )
                                }
                            }
                        }
                        item(key = "project-members-heading", contentType = "heading") {
                            Text(
                                text = "Project team",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                        }
                        if (members.isEmpty()) {
                            item(key = "project-members-empty", contentType = "empty") {
                                Text(
                                    text = if (teamId == null) {
                                        "No project team id yet — invite becomes available once Modrinth returns the team."
                                    } else {
                                        "No direct project collaborators. Invite people to this project team, or manage organization members under Teams."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            items(members, key = { "team-${it.user.id}" }, contentType = { "member" }) { member ->
                                Box(modifier = Modifier.animateItem()) {
                                    ProjectMemberCard(
                                        member = member,
                                        canManage = canManageMembers,
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
