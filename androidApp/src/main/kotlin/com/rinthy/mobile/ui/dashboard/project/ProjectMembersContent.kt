package com.rinthy.mobile.ui.dashboard.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.UserPlus
import com.rinthy.mobile.MemberSearchState
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.components.RinthyTextField
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate

@Composable
internal fun MembersHeader(
    title: String = "Team members",
    canInvite: Boolean,
    onInvite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (canInvite) {
            IconButton(onClick = onInvite) {
                Icon(Lucide.UserPlus, contentDescription = "Invite member")
            }
        }
    }
}

@Composable
internal fun ProjectMemberCard(
    member: ProjectMember,
    canManage: Boolean,
    isCurrentUser: Boolean,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onJoin: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = member.user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(member.user.username, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (member.isOwner) {
                            Icon(Lucide.Crown, contentDescription = "Owner", tint = RinthyDesign.colors.positive, modifier = Modifier.padding(start = 7.dp).size(15.dp))
                        }
                    }
                    Text(
                        member.role.ifBlank { if (member.isOwner) "Owner" else "Member" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (isBusy) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else if (canManage && !member.isOwner) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Lucide.Pencil, contentDescription = "Edit member", modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Lucide.Trash2, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                MemberStatus(member.accepted)
                if (isCurrentUser && !member.accepted) {
                    Surface(
                        color = RinthyDesign.colors.positive,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.padding(start = 8.dp).clickable(onClick = onJoin),
                    ) {
                        Text("Accept invitation", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                    }
                }
            }
            HorizontalDivider(
                color = RinthyDesign.colors.separator,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
internal fun InviteMemberDialog(
    search: MemberSearchState,
    isSaving: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(search.query) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Invite member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                RinthyTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it)
                    },
                    placeholder = "Exact Modrinth username",
                    leadingIcon = Lucide.Search,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                when {
                    search.isSearching -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(top = 18.dp).size(22.dp))
                    search.user != null -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        AsyncImage(
                            model = search.user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Text(search.user.username, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        IconButton(onClick = { onInvite(search.user.id) }, enabled = !isSaving) {
                            if (isSaving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            else Icon(Lucide.UserPlus, contentDescription = "Invite ${search.user.username}")
                        }
                    }
                    search.query.isNotEmpty() && search.errorMessage == null -> Text(
                        "User not found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                (errorMessage ?: search.errorMessage)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                Spacer(Modifier.height(16.dp))
                RinthySecondaryButton("Close", Lucide.Trash2, onDismiss)
            }
        }
    }
}

@Composable
internal fun MemberEditorDialog(
    member: ProjectMember,
    isSaving: Boolean,
    errorMessage: String?,
    /** When true, also edit organization_permissions (org team members). */
    showOrganizationPermissions: Boolean = false,
    projectPermissionLabels: List<String> = PROJECT_PERMISSIONS,
    organizationPermissionLabels: List<String> = ORGANIZATION_PERMISSIONS,
    onSave: (ProjectMemberUpdate) -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit,
) {
    var role by remember(member.user.id) { mutableStateOf(member.role) }
    var permissions by remember(member.user.id) { mutableIntStateOf(member.permissions ?: 0) }
    var organizationPermissions by remember(member.user.id) {
        mutableIntStateOf(member.organizationPermissions ?: 0)
    }
    var payoutsSplit by remember(member.user.id) { mutableStateOf(member.payoutsSplit?.toString().orEmpty()) }
    var ordering by remember(member.user.id) { mutableStateOf(member.ordering.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp).heightIn(max = 720.dp)) {
                Text("Edit ${member.user.username}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.padding(top = 16.dp).weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    MemberField("Role", role, { role = it }, "Member")
                    Text(
                        if (showOrganizationPermissions) "Default project permissions" else "Permissions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PermissionChipRow(
                        labels = projectPermissionLabels,
                        bits = permissions,
                        onBitsChange = { permissions = it },
                        modifier = Modifier.padding(top = 7.dp, bottom = 14.dp),
                    )
                    if (showOrganizationPermissions) {
                        Text(
                            "Organization permissions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PermissionChipRow(
                            labels = organizationPermissionLabels,
                            bits = organizationPermissions,
                            onBitsChange = { organizationPermissions = it },
                            modifier = Modifier.padding(top = 7.dp, bottom = 14.dp),
                        )
                    }
                    MemberField("Payout split", payoutsSplit, { payoutsSplit = it }, "0")
                    MemberField("Ordering", ordering, { ordering = it }, "0")
                    RinthySecondaryButton(
                        text = "Transfer ownership",
                        icon = Lucide.Crown,
                        enabled = !isSaving,
                        onClick = onTransferOwnership,
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
                    RinthySecondaryButton("Cancel", Lucide.Trash2, onDismiss, modifier = Modifier.weight(1f))
                    RinthyPrimaryButton(
                        text = "Save",
                        icon = Lucide.Save,
                        isLoading = isSaving,
                        enabled = role.isNotBlank() && !isSaving,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSave(
                                ProjectMemberUpdate(
                                    role = role.trim(),
                                    permissions = permissions,
                                    organizationPermissions = if (showOrganizationPermissions) {
                                        organizationPermissions
                                    } else {
                                        null
                                    },
                                    payoutsSplit = payoutsSplit.toDoubleOrNull(),
                                    ordering = ordering.toIntOrNull(),
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionChipRow(
    labels: List<String>,
    bits: Int,
    onBitsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        labels.forEachIndexed { bit, label ->
            val enabled = bits and (1 shl bit) != 0
            Surface(
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier.clickable {
                    onBitsChange(
                        if (enabled) bits and (1 shl bit).inv() else bits or (1 shl bit),
                    )
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    if (enabled) Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = if (enabled) 5.dp else 0.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    RinthyTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = Lucide.Pencil,
        leadingIconDescription = null,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
    )
}

@Composable
private fun MemberStatus(accepted: Boolean) {
    val icon = if (accepted) Lucide.Check else Lucide.Mail
    val label = if (accepted) "Accepted" else "Pending"
    val tint = if (accepted) RinthyDesign.colors.positive else MaterialTheme.colorScheme.tertiary
    Surface(color = tint.copy(alpha = 0.12f), contentColor = tint, shape = RoundedCornerShape(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
        }
    }
}

internal val PROJECT_PERMISSIONS = listOf(
    "Upload versions",
    "Delete versions",
    "Edit details",
    "Edit description",
    "Manage invites",
    "Remove members",
    "Edit members",
    "Delete project",
    "View analytics",
    "View payouts",
)

internal val ORGANIZATION_PERMISSIONS = listOf(
    "Edit organization",
    "Manage invites",
    "Remove members",
    "Edit members",
    "Add projects",
    "Remove projects",
    "Delete organization",
    "Edit default project permissions",
)
