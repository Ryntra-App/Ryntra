package com.rinthy.mobile.ui.dashboard.project.members

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
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
import com.rinthy.mobile.R
import com.rinthy.mobile.ui.components.RinthyPrimaryButton
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.rinthy.mobile.ui.components.RinthyTextField
import com.rinthy.mobile.ui.theme.RinthyDesign
import com.rinthy.shared.model.ProjectMember
import com.rinthy.shared.model.ProjectMemberUpdate

@Composable
internal fun MembersHeader(
    title: String,
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
                Icon(Lucide.UserPlus, contentDescription = stringResource(R.string.project_members_invite))
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
                        member.role.ifBlank {
                            if (member.isOwner) {
                                stringResource(R.string.project_members_owner)
                            } else {
                                stringResource(R.string.project_members_role_default)
                            }
                        },
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
                        Icon(Lucide.Pencil, contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Lucide.Trash2, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
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
                        Text(
                            stringResource(R.string.project_members_accept),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
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
                Text(stringResource(R.string.project_members_invite), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.project_members_invite_username_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                RinthyTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it)
                    },
                    placeholder = stringResource(R.string.project_members_invite_username),
                    leadingIcon = Lucide.Search,
                    leadingIconDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
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
                            else Icon(Lucide.UserPlus, contentDescription = stringResource(R.string.project_members_invite))
                        }
                    }
                    search.query.isNotEmpty() && search.errorMessage == null -> Text(
                        stringResource(R.string.project_members_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                (errorMessage ?: search.errorMessage)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                Spacer(Modifier.height(16.dp))
                RinthySecondaryButton(stringResource(R.string.project_members_close), Lucide.Trash2, onDismiss)
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
    onSave: (ProjectMemberUpdate) -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit,
) {
    var role by remember(member.user.id) { mutableStateOf(member.role) }
    var permissions by remember(member.user.id) { mutableIntStateOf(member.permissions ?: 0) }
    var organizationPermissions by remember(member.user.id) {
        mutableIntStateOf(member.organizationPermissions ?: 0)
    }
    var payoutsSplit by remember(member.user.id) {
        mutableStateOf(formatPayoutShare(member.payoutsSplit))
    }
    var ordering by remember(member.user.id) { mutableStateOf(member.ordering.toString()) }
    var confirmTransfer by remember { mutableStateOf(false) }
    val projectPermissionLabels = projectPermissionLabels()
    val organizationPermissionLabels = organizationPermissionLabels()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp).heightIn(max = 720.dp)) {
                Text(
                    stringResource(R.string.project_members_edit, member.user.username),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(modifier = Modifier.padding(top = 16.dp).weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    MemberField(stringResource(R.string.project_members_role), role, { role = it }, stringResource(R.string.project_members_role_default))
                    Text(
                        if (showOrganizationPermissions) {
                            stringResource(R.string.project_members_default_project_permissions)
                        } else {
                            stringResource(R.string.project_members_permissions)
                        },
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
                            stringResource(R.string.project_members_org_permissions),
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
                    MemberField(
                        label = stringResource(R.string.project_members_payout_split),
                        value = payoutsSplit,
                        onValueChange = { raw ->
                            payoutsSplit = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                        },
                        placeholder = stringResource(R.string.project_members_payout_placeholder),
                    )
                    Text(
                        stringResource(R.string.project_members_payout_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    MemberField(stringResource(R.string.project_members_ordering), ordering, { ordering = it }, "0")
                    RinthySecondaryButton(
                        text = stringResource(R.string.project_members_transfer),
                        icon = Lucide.Crown,
                        enabled = !isSaving,
                        onClick = { confirmTransfer = true },
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
                    RinthySecondaryButton(stringResource(R.string.project_members_cancel), Lucide.Trash2, onDismiss, modifier = Modifier.weight(1f))
                    RinthyPrimaryButton(
                        text = stringResource(R.string.project_members_save),
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
                                    payoutsSplit = parsePayoutShare(payoutsSplit),
                                    ordering = ordering.toIntOrNull(),
                                )
                            )
                        },
                    )
                }
            }
        }
    }

    if (confirmTransfer) {
        AlertDialog(
            onDismissRequest = { confirmTransfer = false },
            title = { Text(stringResource(R.string.project_members_transfer_title)) },
            text = {
                Text(stringResource(R.string.project_members_transfer_message, member.user.username))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmTransfer = false
                        onTransferOwnership()
                    },
                ) {
                    Text(stringResource(R.string.project_members_transfer_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTransfer = false }) {
                    Text(stringResource(R.string.project_members_cancel))
                }
            },
        )
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
    val label = if (accepted) {
        stringResource(R.string.project_members_accepted)
    } else {
        stringResource(R.string.project_members_pending)
    }
    val tint = if (accepted) RinthyDesign.colors.positive else MaterialTheme.colorScheme.tertiary
    Surface(color = tint.copy(alpha = 0.12f), contentColor = tint, shape = RoundedCornerShape(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
        }
    }
}

@Composable
private fun projectPermissionLabels(): List<String> = listOf(
    stringResource(R.string.perm_upload_versions),
    stringResource(R.string.perm_delete_versions),
    stringResource(R.string.perm_edit_details),
    stringResource(R.string.perm_edit_description),
    stringResource(R.string.perm_manage_invites),
    stringResource(R.string.perm_remove_members),
    stringResource(R.string.perm_edit_members),
    stringResource(R.string.perm_delete_project),
    stringResource(R.string.perm_view_analytics),
    stringResource(R.string.perm_view_payouts),
)

@Composable
private fun organizationPermissionLabels(): List<String> = listOf(
    stringResource(R.string.perm_org_edit),
    stringResource(R.string.perm_org_manage_invites),
    stringResource(R.string.perm_org_remove_members),
    stringResource(R.string.perm_org_edit_members),
    stringResource(R.string.perm_org_add_projects),
    stringResource(R.string.perm_org_remove_projects),
    stringResource(R.string.perm_org_delete),
    stringResource(R.string.perm_org_edit_default_permissions),
)

/** Show whole percents without trailing ".0". */
private fun formatPayoutShare(value: Double?): String {
    if (value == null) return ""
    val rounded = if (kotlin.math.abs(value - value.toLong()) < 0.0001) {
        value.toLong().toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }
    return rounded
}

private fun parsePayoutShare(raw: String): Double? {
    val normalized = raw.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    return normalized.toDoubleOrNull()?.coerceIn(0.0, 100.0)
}
