package com.ryntra.mobile.ui.dashboard.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Server
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.theme.RyntraDesign
import com.ryntra.mobile.ui.components.RyntraEmptyState
import com.ryntra.mobile.ui.dashboard.project.disclosures.disclosuresContentItems
import com.ryntra.mobile.ui.dashboard.project.edit.EditProjectContent
import com.ryntra.mobile.ui.dashboard.project.edit.ProjectEditDraft
import com.ryntra.mobile.ui.dashboard.project.gallery.ProjectGalleryOverviewStrip
import com.ryntra.mobile.ui.dashboard.project.gallery.ProjectGalleryViewerDialog
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownBlockView
import com.ryntra.mobile.ui.dashboard.project.members.InviteMemberDialog
import com.ryntra.mobile.ui.dashboard.project.members.MemberEditorDialog
import com.ryntra.mobile.ui.dashboard.project.members.MembersHeader
import com.ryntra.mobile.ui.dashboard.project.members.ProjectMemberCard
import com.ryntra.mobile.ui.dashboard.project.overview.CategoryChip
import com.ryntra.mobile.ui.dashboard.project.overview.DetailHeading
import com.ryntra.mobile.ui.dashboard.project.overview.DetailSection
import com.ryntra.mobile.ui.dashboard.project.overview.DetailValue
import com.ryntra.mobile.ui.dashboard.project.overview.EnvironmentValue
import com.ryntra.mobile.ui.dashboard.project.overview.LoadingMembers
import com.ryntra.mobile.ui.dashboard.project.overview.ProjectDependencyRow
import com.ryntra.mobile.ui.dashboard.project.overview.ProjectIdentity
import com.ryntra.mobile.ui.dashboard.project.overview.ProjectMetrics
import com.ryntra.mobile.ui.dashboard.project.overview.ProjectResource
import com.ryntra.mobile.ui.dashboard.project.overview.ResourceRow
import com.ryntra.mobile.ui.dashboard.project.sharecard.ShareCardStudio
import com.ryntra.mobile.ui.dashboard.project.versions.LoadingVersions
import com.ryntra.mobile.ui.dashboard.project.versions.VersionCard
import com.ryntra.mobile.ui.dashboard.project.versions.VersionEditorDialog
import com.ryntra.mobile.ui.dashboard.project.versions.VersionsHeader
import com.ryntra.mobile.ui.dashboard.projects.DeleteProjectDialog
import com.ryntra.shared.model.MarkdownParser
import com.ryntra.shared.model.MarkdownBlock
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.ProjectDependency
import com.ryntra.shared.model.ProjectDisclosureDraft
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMember
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.ProjectVersion
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.mobile.MemberSearchState
import com.ryntra.mobile.ProjectActionState
import com.ryntra.mobile.ProjectDisclosuresState
import com.ryntra.mobile.ProjectModerationState
import com.ryntra.mobile.ui.dashboard.project.moderation.moderationContentItems
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
    isReadOnly: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    memberErrorMessage: String? = null,
    projectUpdate: com.ryntra.mobile.ProjectUpdateState = com.ryntra.mobile.ProjectUpdateState(),
    onUpdateProject: (String, com.ryntra.shared.model.ProjectUpdate) -> Unit = { _, _ -> },
    onClearProjectUpdateStatus: () -> Unit = {},
    projectAction: ProjectActionState = ProjectActionState(),
    moderation: ProjectModerationState = ProjectModerationState(),
    disclosures: ProjectDisclosuresState = ProjectDisclosuresState(),
    memberSearch: MemberSearchState = MemberSearchState(),
    onChangeProjectIcon: (String, ProjectFileUpload) -> Unit = { _, _ -> },
    onDeleteProjectIcon: (String) -> Unit = {},
    onSubmitProjectForModeration: (String) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
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
    onLoadDisclosures: (String, Boolean) -> Unit = { _, _ -> },
    onSaveDisclosures: (String, ProjectDisclosureDraft) -> Unit = { _, _ -> },
    onLoadModeration: (String, Boolean) -> Unit = { _, _ -> },
    onSendModerationReply: (String, String, String?) -> Unit = { _, _, _ -> },
    onDeleteModerationMessage: (String, String) -> Unit = { _, _ -> },
    loadProjectCreationMetadata: suspend () -> ProjectCreationMetadata = { error("Unavailable") },
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    onRetry: () -> Unit = {},
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
    var versionPendingDeletion by remember(project.id) { mutableStateOf<ProjectVersion?>(null) }
    var isInvitingMember by remember(project.id) { mutableStateOf(false) }
    var editingMember by remember(project.id) { mutableStateOf<ProjectMember?>(null) }
    var memberPendingRemoval by remember(project.id) { mutableStateOf<ProjectMember?>(null) }
    var viewingGalleryImage by remember(project.id) { mutableStateOf<com.ryntra.shared.model.GalleryImage?>(null) }
    var moderationReplyTargetId by rememberSaveable(project.id) { mutableStateOf<String?>(null) }
    var isConfirmingSubmission by remember(project.id) { mutableStateOf(false) }
    var isConfirmingDeletion by remember(project.id) { mutableStateOf(false) }
    var isCreatingShareCard by remember(project.id) { mutableStateOf(false) }
    var editBaseline by remember(project.id) { mutableStateOf(ProjectEditDraft.from(project)) }
    var editDraft by remember(project.id) { mutableStateOf(editBaseline) }
    var pendingTab by remember(project.id) { mutableStateOf<ProjectDetailTab?>(null) }
    var disclosureDraft by remember(project.id) { mutableStateOf(disclosures.baseline) }
    // Modrinth owns the disclosure record: every load and every save rebaselines the editor.
    LaunchedEffect(project.id, disclosures.baseline) { disclosureDraft = disclosures.baseline }
    val hasEditChanges = editDraft != editBaseline
    val hasDisclosureChanges = disclosureDraft.hasChangesFrom(disclosures.baseline)
    val hasUnsavedChanges = hasEditChanges || hasDisclosureChanges
    val hasPendingChanges = when (selectedTab) {
        ProjectDetailTab.Edit -> hasEditChanges
        ProjectDetailTab.Disclosures -> hasDisclosureChanges
        else -> false
    }
    val canSavePendingChanges = when (selectedTab) {
        ProjectDetailTab.Edit -> editDraft.canSave
        ProjectDetailTab.Disclosures -> disclosureDraft.canSave
        else -> false
    }
    val isSavingPendingChanges = when (selectedTab) {
        ProjectDetailTab.Edit -> projectUpdate.isSaving
        ProjectDetailTab.Disclosures -> disclosures.isSaving
        else -> false
    }
    // Prefer project-team membership; fall back to org membership for permission checks.
    val currentMember = remember(members, organizationMembers, currentUserId) {
        members.firstOrNull { it.user.id == currentUserId }
            ?: organizationMembers.firstOrNull { it.user.id == currentUserId }
    }
    val canCreateVersions = !isReadOnly && currentMember.hasPermission(0)
    val canDeleteVersions = !isReadOnly && currentMember.hasPermission(1)
    val canSubmitProject = !isReadOnly && currentMember.hasPermission(2)
    val canDeleteProject = !isReadOnly && currentMember.hasPermission(7)
    val canManageMembers = !isReadOnly && (currentMember.hasPermission(6) || currentMember?.isOwner == true)
    val teamId = project.team

    LaunchedEffect(hasUnsavedChanges) {
        onUnsavedChangesChanged(hasUnsavedChanges)
    }

    LaunchedEffect(projectUpdate.isSuccess) {
        if (projectUpdate.isSuccess) {
            editBaseline = editDraft
            onClearProjectUpdateStatus()
        }
    }

    LaunchedEffect(isReadOnly) {
        if (isReadOnly && selectedTab !in setOf(ProjectDetailTab.Overview, ProjectDetailTab.Versions)) {
            selectedTab = ProjectDetailTab.Overview
        }
    }

    LaunchedEffect(selectedTab, project.id, isReadOnly) {
        if (!isReadOnly && selectedTab == ProjectDetailTab.Disclosures) {
            onLoadDisclosures(project.slug ?: project.id, false)
        }
    }

    LaunchedEffect(selectedTab, project.threadId, isReadOnly) {
        val threadId = project.threadId
        if (!isReadOnly && selectedTab == ProjectDetailTab.Moderation && !threadId.isNullOrBlank()) {
            onLoadModeration(threadId, false)
        }
    }

    LaunchedEffect(moderation.replyGeneration) {
        if (moderation.replyGeneration > 0) moderationReplyTargetId = null
    }

    LaunchedEffect(projectAction.successMessage) {
        if (projectAction.successMessage != null) {
            isCreatingVersion = false
            editingVersion = null
            versionPendingDeletion = null
            isInvitingMember = false
            editingMember = null
            memberPendingRemoval = null
            isConfirmingSubmission = false
            isConfirmingDeletion = false
            onClearProjectActionStatus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = if (hasPendingChanges) 116.dp else 36.dp,
        ),
    ) {
        item(key = "identity", contentType = "identity") {
            ProjectIdentity(project, onCreateShareCard = { isCreatingShareCard = true })
        }
        item(key = "tabs", contentType = "tabs") {
            ProjectDetailTabs(
                selected = selectedTab,
                isReadOnly = isReadOnly,
                onSelect = { requestedTab ->
                    if (hasPendingChanges && requestedTab != selectedTab) {
                        pendingTab = requestedTab
                    } else {
                        selectedTab = requestedTab
                    }
                },
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
                            HorizontalDivider(color = RyntraDesign.colors.separator)
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
                        RyntraEmptyState(
                            title = stringResource(R.string.project_versions_unavailable),
                            message = errorMessage,
                            actionLabel = stringResource(R.string.common_retry),
                            onAction = onRetry,
                        )
                    }
                    versions.isEmpty() -> item {
                        RyntraEmptyState(
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
                                onOpen = { if (canCreateVersions) editingVersion = version },
                                onEdit = { editingVersion = version },
                                onDelete = { versionPendingDeletion = version },
                            )
                        }
                    }
                }
            }

            ProjectDetailTab.Edit -> {
                item(key = "project-edit", contentType = "edit") {
                    EditProjectContent(
                        project = project,
                        draft = editDraft,
                        projectUpdate = projectUpdate,
                        onDraftChange = { editDraft = it },
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
                        loadProjectCreationMetadata = loadProjectCreationMetadata,
                    )
                }
                if (canDeleteProject) {
                    item(key = "project-delete", contentType = "destructive-action") {
                        ProjectDeleteAction(
                            enabled = !projectAction.isRunning,
                            onDelete = { isConfirmingDeletion = true },
                        )
                    }
                }
            }

            ProjectDetailTab.Disclosures -> {
                disclosuresContentItems(
                    state = disclosures,
                    draft = disclosureDraft,
                    canEdit = canSubmitProject,
                    versionCount = versions.size,
                    onChange = { entry -> disclosureDraft = disclosureDraft.replacing(entry) },
                    onRefresh = { onLoadDisclosures(project.slug ?: project.id, true) },
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
                        RyntraEmptyState(
                            title = stringResource(R.string.project_members_unavailable),
                            message = memberErrorMessage,
                            actionLabel = stringResource(R.string.common_retry),
                            onAction = onRetry,
                        )
                    }
                    rosterMembers.isEmpty() -> item {
                        RyntraEmptyState(
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
                                onRemove = { memberPendingRemoval = member },
                                onJoin = { project.team?.let(onJoinTeam) },
                            )
                        }
                    }
                }
            }

            ProjectDetailTab.Moderation -> {
                moderationContentItems(
                    project = project,
                    state = moderation,
                    currentUserId = currentUserId,
                    versionCount = versions.size,
                    canSubmitProject = canSubmitProject,
                    isSubmittingProject = projectAction.isRunning && projectAction.targetId == project.id,
                    submissionError = projectAction.errorMessage.takeIf { projectAction.targetId == project.id },
                    replyingToMessageId = moderationReplyTargetId,
                    onSubmitProject = { isConfirmingSubmission = true },
                    onReplyToMessage = { moderationReplyTargetId = it },
                    onRefresh = { project.threadId?.let { threadId -> onLoadModeration(threadId, true) } },
                    onSendReply = { body, replyingTo ->
                        project.threadId?.let { threadId -> onSendModerationReply(threadId, body, replyingTo) }
                    },
                    onDeleteMessage = { messageId ->
                        project.threadId?.let { threadId -> onDeleteModerationMessage(threadId, messageId) }
                    },
                )
            }
        }
    }

        AnimatedVisibility(
            visible = hasPendingChanges,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                if (selectedTab == ProjectDetailTab.Disclosures) {
                                    R.string.disclosures_unsaved
                                } else {
                                    R.string.project_edit_unsaved
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!canSavePendingChanges) {
                            Text(
                                stringResource(
                                    if (selectedTab == ProjectDetailTab.Disclosures) {
                                        R.string.disclosures_required_hint
                                    } else {
                                        R.string.project_edit_required_hint
                                    },
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Button(
                        enabled = canSavePendingChanges && !isSavingPendingChanges,
                        onClick = {
                            if (selectedTab == ProjectDetailTab.Disclosures) {
                                onSaveDisclosures(project.slug ?: project.id, disclosureDraft)
                            } else {
                                onUpdateProject(project.id, editDraft.toUpdate(editBaseline))
                            }
                        },
                    ) {
                        if (isSavingPendingChanges) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            androidx.compose.material3.Icon(Lucide.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(stringResource(R.string.project_edit_save), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    pendingTab?.let { requestedTab ->
        AlertDialog(
            onDismissRequest = { pendingTab = null },
            title = { Text(stringResource(R.string.project_edit_discard_title)) },
            text = { Text(stringResource(R.string.project_edit_discard_message)) },
            dismissButton = {
                TextButton(onClick = { pendingTab = null }) {
                    Text(stringResource(R.string.project_edit_keep_editing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedTab == ProjectDetailTab.Disclosures) {
                            disclosureDraft = disclosures.baseline
                        } else {
                            editDraft = editBaseline
                        }
                        selectedTab = requestedTab
                        pendingTab = null
                    },
                ) {
                    Text(stringResource(R.string.project_edit_discard), color = MaterialTheme.colorScheme.error)
                }
            },
        )
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
    versionPendingDeletion?.let { version ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.version_delete_title),
            message = stringResource(R.string.version_delete_message, version.versionNumber),
            confirmLabel = stringResource(R.string.version_delete_action),
            isRunning = projectAction.isRunning && projectAction.targetId == version.id,
            errorMessage = projectAction.errorMessage.takeIf { projectAction.targetId == version.id },
            onDismiss = {
                versionPendingDeletion = null
                onClearProjectActionStatus()
            },
            onConfirm = { onDeleteVersion(version.id) },
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
    memberPendingRemoval?.let { member ->
        project.team?.let { resolvedTeamId ->
            DestructiveConfirmationDialog(
                title = stringResource(R.string.member_remove_title),
                message = stringResource(R.string.member_remove_message, member.user.username),
                confirmLabel = stringResource(R.string.member_remove_action),
                isRunning = projectAction.isRunning && projectAction.targetId == member.user.id,
                errorMessage = projectAction.errorMessage.takeIf { projectAction.targetId == member.user.id },
                onDismiss = {
                    memberPendingRemoval = null
                    onClearProjectActionStatus()
                },
                onConfirm = { onRemoveMember(resolvedTeamId, member.user.id) },
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
    if (isConfirmingSubmission) {
        SubmitProjectDialog(
            project = project,
            isSubmitting = projectAction.isRunning && projectAction.targetId == project.id,
            errorMessage = projectAction.errorMessage.takeIf { projectAction.targetId == project.id },
            onDismiss = {
                isConfirmingSubmission = false
                onClearProjectActionStatus()
            },
            onConfirm = { onSubmitProjectForModeration(project.id) },
        )
    }
    if (isConfirmingDeletion) {
        DeleteProjectDialog(
            project = project,
            isDeleting = projectAction.isRunning && projectAction.targetId == project.id,
            errorMessage = projectAction.errorMessage.takeIf { projectAction.targetId == project.id },
            onDismiss = {
                isConfirmingDeletion = false
                onClearProjectActionStatus()
            },
            onConfirm = { onDeleteProject(project.id) },
        )
    }
    if (isCreatingShareCard) {
        ShareCardStudio(
            project = project,
            versions = versions,
            onDismiss = { isCreatingShareCard = false },
        )
    }
}

@Composable
internal fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isRunning: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message)
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isRunning, onClick = onDismiss) {
                Text(stringResource(R.string.destructive_action_cancel))
            }
        },
        confirmButton = {
            TextButton(enabled = !isRunning, onClick = onConfirm) {
                if (isRunning) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(confirmLabel, color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
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
