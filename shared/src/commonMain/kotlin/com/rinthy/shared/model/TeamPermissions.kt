package com.rinthy.shared.model

/**
 * Bit flags used by Modrinth for team / organization membership.
 * See labrinth team permission enums.
 */
object ProjectPermissionBits {
    const val UploadVersion = 0
    const val DeleteVersion = 1
    const val EditDetails = 2
    const val EditBody = 3
    const val ManageInvites = 4
    const val RemoveMember = 5
    const val EditMember = 6
    const val DeleteProject = 7
    const val ViewAnalytics = 8
    const val ViewPayouts = 9

    val labels: List<String> = listOf(
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
}

object OrganizationPermissionBits {
    const val Edit = 0
    const val ManageInvites = 1
    const val RemoveMember = 2
    const val EditMember = 3
    const val AddProject = 4
    const val RemoveProject = 5
    const val DeleteOrganization = 6
    const val EditMemberDefaultPermissions = 7

    val labels: List<String> = listOf(
        "Edit organization",
        "Manage invites",
        "Remove members",
        "Edit members",
        "Add projects",
        "Remove projects",
        "Delete organization",
        "Edit default project permissions",
    )
}

fun ProjectMember?.hasProjectPermission(bit: Int): Boolean {
    if (this == null) return false
    if (isOwner) return true
    val bits = permissions ?: return false
    return bits and (1 shl bit) != 0
}

fun ProjectMember?.hasOrganizationPermission(bit: Int): Boolean {
    if (this == null) return false
    if (isOwner) return true
    val bits = organizationPermissions ?: return false
    return bits and (1 shl bit) != 0
}

fun ProjectMember?.canManageTeamMembers(): Boolean =
    this != null && (isOwner || hasProjectPermission(ProjectPermissionBits.EditMember) ||
        hasProjectPermission(ProjectPermissionBits.ManageInvites) ||
        hasProjectPermission(ProjectPermissionBits.RemoveMember))

fun ProjectMember?.canManageOrganizationMembers(): Boolean =
    this != null && (isOwner ||
        hasOrganizationPermission(OrganizationPermissionBits.EditMember) ||
        hasOrganizationPermission(OrganizationPermissionBits.ManageInvites) ||
        hasOrganizationPermission(OrganizationPermissionBits.RemoveMember))

/**
 * Combined roster for a project that may belong to an organization.
 * Organization members inherit access; project team is the direct collaborators list.
 */
data class ProjectTeamRoster(
    val projectMembers: List<ProjectMember> = emptyList(),
    val organizationMembers: List<ProjectMember> = emptyList(),
    val organization: Organization? = null,
) {
    val allMembers: List<ProjectMember>
        get() = (organizationMembers + projectMembers).distinctBy { it.user.id }
}
