import RyntraShared
import SwiftUI

struct ProjectMembersManagementView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let members: [ProjectMember]
    var organizationMembers: [ProjectMember] = []
    var organizationName: String? = nil
    let isLoading: Bool
    let errorMessage: String?
    let onReload: () async -> Void

    @State private var editingMember: ProjectMember?
    @State private var isInviting = false
    @State private var localError: String?

    private var currentMember: ProjectMember? {
        members.first { $0.user.id == model.currentAccountID }
            ?? organizationMembers.first { $0.user.id == model.currentAccountID }
    }

    private func hasProjectPermission(_ bit: Int32) -> Bool {
        guard let currentMember else { return false }
        let permissions = (currentMember.permissions as? NSNumber)?.int32Value ?? 0
        return currentMember.isOwner || permissions & (Int32(1) << bit) != 0
    }

    private var isOrganizationProject: Bool {
        !(project.organization?.isEmpty ?? true) || !organizationMembers.isEmpty
    }

    private var rosterMembers: [ProjectMember] {
        if isOrganizationProject {
            organizationMembers.isEmpty ? members : organizationMembers
        } else {
            members
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(NSLocalizedString("Members", comment: "Project tab")).font(.title3.bold())
                Spacer()
                if !isOrganizationProject, hasProjectPermission(4), project.team != nil {
                    Button { isInviting = true } label: {
                        Image(systemName: "person.badge.plus")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ryntraGreen)
                    .ryntraMinimumTouchTarget()
                    .accessibilityLabel(NSLocalizedString("Invite member", comment: "Member action"))
                }
            }

            if isOrganizationProject {
                Text(NSLocalizedString(
                    "Managed through the organization. Edit members under Teams.",
                    comment: "Org project members hint"
                ))
                .font(.caption)
                .foregroundStyle(.secondary)
            }

            if isLoading {
                HStack(spacing: 10) {
                    ProgressView()
                    Text(NSLocalizedString("Loading members", comment: "Members loading"))
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 28)
            } else if let errorMessage, rosterMembers.isEmpty {
                emptyState(
                    NSLocalizedString("Members unavailable", comment: "Members error"),
                    errorMessage,
                    retry: onReload
                )
            } else if rosterMembers.isEmpty {
                emptyState(
                    NSLocalizedString("No members found", comment: ""),
                    NSLocalizedString("Team members for this project will appear here.", comment: "")
                )
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(rosterMembers, id: \.user.id) { member in
                        ManagedMemberCard(
                            member: member,
                            canEdit: !isOrganizationProject && hasProjectPermission(6),
                            canRemove: !isOrganizationProject && hasProjectPermission(5),
                            isCurrentUser: member.user.id == model.currentAccountID,
                            onEdit: { editingMember = member },
                            onRemove: { Task { await remove(member) } },
                            onJoin: { Task { await join() } }
                        )
                    }
                }
            }

            if let error = localError ?? model.projectActionError {
                Text(error).font(.caption).foregroundStyle(.red)
            }
        }
        .sheet(isPresented: $isInviting) {
            InviteMemberSheet(teamID: project.team) {
                await onReload()
                isInviting = false
            }
        }
        .sheet(
            isPresented: Binding(
                get: { editingMember != nil },
                set: { if !$0 { editingMember = nil } }
            )
        ) {
            if let editingMember {
                MemberEditorSheet(
                    teamID: project.team,
                    member: editingMember,
                    showOrganizationPermissions: false
                ) {
                    await onReload()
                    self.editingMember = nil
                }
            }
        }
    }

    private func emptyState(
        _ title: String,
        _ message: String,
        retry: (() async -> Void)? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(LocalizedStringKey(title)).font(.headline)
            Text(LocalizedStringKey(message)).foregroundStyle(.secondary)
            if let retry {
                Button {
                    Task { await retry() }
                } label: {
                    Label(NSLocalizedString("Retry", comment: "Common action"), systemImage: "arrow.clockwise")
                }
                .buttonStyle(.bordered)
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
    }

    @MainActor
    private func remove(_ member: ProjectMember) async {
        guard let teamID = project.team else { return }
        do {
            try await model.removeMember(teamID: teamID, userID: member.user.id)
            await onReload()
        } catch { localError = error.localizedDescription }
    }

    @MainActor
    private func join() async {
        guard let teamID = project.team else { return }
        do {
            try await model.joinTeam(teamID: teamID)
            await onReload()
        } catch { localError = error.localizedDescription }
    }
}

struct ManagedMemberCard: View {
    let member: ProjectMember
    let canEdit: Bool
    let canRemove: Bool
    let isCurrentUser: Bool
    let onEdit: () -> Void
    let onRemove: () -> Void
    let onJoin: () -> Void
    @State private var isConfirmingRemoval = false

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 12) {
                RemoteImage(url: URL(string: member.user.avatarUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: { Circle().fill(.quaternary) }
                .frame(width: 44, height: 44)
                .clipShape(Circle())

                VStack(alignment: .leading, spacing: 3) {
                    HStack {
                        Text(member.user.username).font(.headline.bold())
                        if member.isOwner { Image(systemName: "crown.fill").foregroundStyle(Color.ryntraGreen) }
                    }
                    Text(member.role).font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer()
                if !member.isOwner {
                    if canEdit {
                        Button(action: onEdit) {
                            Image(systemName: "pencil").ryntraMinimumTouchTarget()
                        }
                        .accessibilityLabel(NSLocalizedString("Edit member", comment: "Member action"))
                    }
                    if canRemove {
                        Button(role: .destructive) {
                            isConfirmingRemoval = true
                        } label: {
                            Image(systemName: "trash").ryntraMinimumTouchTarget()
                        }
                        .accessibilityLabel(NSLocalizedString("Remove member", comment: "Member action"))
                    }
                }
            }
                HStack {
                    Label(member.accepted ? "Accepted" : "Pending", systemImage: member.accepted ? "checkmark" : "envelope")
                        .font(.caption.bold())
                        .foregroundStyle(member.accepted ? Color.ryntraGreen : Color.orange)
                    if isCurrentUser && !member.accepted {
                        Button("Accept invitation", action: onJoin).buttonStyle(.borderedProminent).tint(.ryntraGreen)
                    }
                }
            }
            .padding(14)
            Divider()
        }
        .confirmationDialog(
            NSLocalizedString("Remove this member?", comment: "Member removal confirmation"),
            isPresented: $isConfirmingRemoval,
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("Remove member", comment: "Member action"), role: .destructive, action: onRemove)
            Button(NSLocalizedString("Cancel", comment: "Common action"), role: .cancel) {}
        } message: {
            Text(NSLocalizedString(
                "They will lose access granted through this team.",
                comment: "Member removal confirmation"
            ))
        }
    }
}

struct InviteMemberSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let teamID: String?
    let onSaved: () async -> Void

    @State private var query = ""
    @State private var result: Account?
    @State private var isSearching = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            List {
                Section {
                    TextField("Exact Modrinth username", text: $query)
                        .ryntraNoAutocapitalization()
                        .autocorrectionDisabled()
                        .submitLabel(.search)
                        .onSubmit {
                            guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                            Task { await search() }
                        }
                    Button("Search") { Task { await search() } }
                        .disabled(query.trimmingCharacters(in: .whitespaces).isEmpty || isSearching)
                }
                if isSearching { ProgressView() }
                if let result {
                    Section("Result") {
                        HStack {
                            RemoteImage(url: URL(string: result.avatarUrl ?? "")) { image in
                                image.resizable().scaledToFill()
                            } placeholder: { Circle().fill(.quaternary) }
                            .frame(width: 38, height: 38).clipShape(Circle())
                            Text(result.username).fontWeight(.semibold)
                            Spacer()
                            Button("Invite") { Task { await invite(result) } }
                        }
                    }
                }
                if let localError { Section { Text(localError).foregroundStyle(.red) } }
            }
            .ryntraInteractiveKeyboardDismissal()
            .navigationTitle("Invite member")
            .ryntraInlineNavigationTitle()
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close") { dismiss() } } }
        }
    }

    @MainActor
    private func search() async {
        isSearching = true
        localError = nil
        defer { isSearching = false }
        do {
            result = try await model.findUser(username: query)
            if result == nil { localError = "User not found." }
        } catch { localError = error.localizedDescription }
    }

    @MainActor
    private func invite(_ user: Account) async {
        guard let teamID else {
            localError = "Team id is missing."
            return
        }
        do {
            try await model.inviteMember(teamID: teamID, userID: user.id)
            await onSaved()
            dismiss()
        } catch { localError = error.localizedDescription }
    }
}

struct MemberEditorSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let teamID: String?
    let member: ProjectMember
    var showOrganizationPermissions: Bool = false
    let onSaved: () async -> Void

    @State private var role: String
    @State private var permissions: Int32
    @State private var organizationPermissions: Int32
    @State private var payout: String
    @State private var ordering: String
    @State private var localError: String?
    @State private var confirmTransfer = false

    init(
        teamID: String?,
        member: ProjectMember,
        showOrganizationPermissions: Bool = false,
        onSaved: @escaping () async -> Void
    ) {
        self.teamID = teamID
        self.member = member
        self.showOrganizationPermissions = showOrganizationPermissions
        self.onSaved = onSaved
        _role = State(initialValue: member.role)
        _permissions = State(initialValue: (member.permissions as? NSNumber)?.int32Value ?? 0)
        _organizationPermissions = State(initialValue: (member.organizationPermissions as? NSNumber)?.int32Value ?? 0)
        _payout = State(initialValue: Self.formatPayout(member.payoutsSplit?.doubleValue))
        _ordering = State(initialValue: String(member.ordering))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(NSLocalizedString("Role", comment: "Member field"), text: $role)
                    TextField(
                        NSLocalizedString("Payout share (%)", comment: "Member field"),
                        text: $payout
                    )
                    .ryntraNumericKeyboard(decimal: true)
                    Text(NSLocalizedString(
                        "Percent of team revenue for this member (0–100). Shares of all accepted members should add up to 100.",
                        comment: "Payout hint"
                    ))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    TextField(NSLocalizedString("Ordering", comment: "Member field"), text: $ordering)
                        .ryntraNumericKeyboard()
                } header: {
                    Text(NSLocalizedString("Member", comment: "Member section"))
                }
                Section(
                    showOrganizationPermissions
                        ? NSLocalizedString("Default project permissions", comment: "Permissions")
                        : NSLocalizedString("Permissions", comment: "Permissions")
                ) {
                    ForEach(Array(permissionNames.enumerated()), id: \.offset) { bit, label in
                        Toggle(
                            label,
                            isOn: Binding(
                                get: { permissions & (Int32(1) << bit) != 0 },
                                set: { enabled in
                                    let mask = Int32(1) << bit
                                    permissions = enabled ? permissions | mask : permissions & ~mask
                                }
                            )
                        )
                    }
                }
                if showOrganizationPermissions {
                    Section(NSLocalizedString("Organization permissions", comment: "Permissions")) {
                        ForEach(Array(orgPermissionNames.enumerated()), id: \.offset) { bit, label in
                            Toggle(
                                label,
                                isOn: Binding(
                                    get: { organizationPermissions & (Int32(1) << bit) != 0 },
                                    set: { enabled in
                                        let mask = Int32(1) << bit
                                        organizationPermissions = enabled
                                            ? organizationPermissions | mask
                                            : organizationPermissions & ~mask
                                    }
                                )
                            )
                        }
                    }
                }
                Section {
                    Button(NSLocalizedString("Transfer ownership", comment: "Member action"), role: .destructive) {
                        confirmTransfer = true
                    }
                }
                if let localError { Section { Text(localError).foregroundStyle(.red) } }
            }
            .navigationTitle(NSLocalizedString("Edit member", comment: "Member editor"))
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("Cancel", comment: "")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Save", comment: "")) { Task { await save() } }
                }
            }
            .confirmationDialog(
                NSLocalizedString("Transfer ownership?", comment: "Confirm title"),
                isPresented: $confirmTransfer,
                titleVisibility: .visible
            ) {
                Button(NSLocalizedString("Transfer", comment: "Confirm"), role: .destructive) {
                    Task { await transfer() }
                }
                Button(NSLocalizedString("Cancel", comment: ""), role: .cancel) {}
            } message: {
                Text(
                    String(
                        format: NSLocalizedString(
                            "Make %@ the new owner? You will lose owner rights if you are the current owner.",
                            comment: "Confirm message"
                        ),
                        member.user.username
                    )
                )
            }
        }
    }

    @MainActor
    private func save() async {
        guard let teamID else {
            localError = "Team id is missing."
            return
        }
        let payoutValue = Double(payout.replacingOccurrences(of: ",", with: "."))
            .map { min(100, max(0, $0)) }
        let update = ProjectMemberUpdate(
            role: role,
            permissions: KotlinInt(int: permissions),
            organizationPermissions: showOrganizationPermissions
                ? KotlinInt(int: organizationPermissions)
                : nil,
            payoutsSplit: payoutValue.map { KotlinDouble(double: $0) },
            ordering: Int32(ordering).map { KotlinInt(int: $0) }
        )
        do {
            try await model.updateMember(teamID: teamID, userID: member.user.id, update: update)
            await onSaved()
            dismiss()
        } catch { localError = error.localizedDescription }
    }

    @MainActor
    private func transfer() async {
        guard let teamID else {
            localError = "Team id is missing."
            return
        }
        do {
            try await model.transferOwnership(teamID: teamID, userID: member.user.id)
            await onSaved()
            dismiss()
        } catch { localError = error.localizedDescription }
    }

    private static func formatPayout(_ value: Double?) -> String {
        guard let value else { return "" }
        if abs(value - value.rounded()) < 0.0001 {
            return String(Int(value.rounded()))
        }
        return String(value)
    }

    private var permissionNames: [String] {
        [
            NSLocalizedString("Upload versions", comment: "Perm"),
            NSLocalizedString("Delete versions", comment: "Perm"),
            NSLocalizedString("Edit details", comment: "Perm"),
            NSLocalizedString("Edit description", comment: "Perm"),
            NSLocalizedString("Manage invites", comment: "Perm"),
            NSLocalizedString("Remove members", comment: "Perm"),
            NSLocalizedString("Edit members", comment: "Perm"),
            NSLocalizedString("Delete project", comment: "Perm"),
            NSLocalizedString("View analytics", comment: "Perm"),
            NSLocalizedString("View payouts", comment: "Perm"),
        ]
    }

    private var orgPermissionNames: [String] {
        [
            NSLocalizedString("Edit organization", comment: "Perm"),
            NSLocalizedString("Manage invites", comment: "Perm"),
            NSLocalizedString("Remove members", comment: "Perm"),
            NSLocalizedString("Edit members", comment: "Perm"),
            NSLocalizedString("Add projects", comment: "Perm"),
            NSLocalizedString("Remove projects", comment: "Perm"),
            NSLocalizedString("Delete organization", comment: "Perm"),
            NSLocalizedString("Edit default project permissions", comment: "Perm"),
        ]
    }
}
