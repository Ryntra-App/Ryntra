import RinthyShared
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

    private var canManageMembers: Bool {
        guard let currentMember else { return false }
        let permissions = (currentMember.permissions as? NSNumber)?.int32Value ?? 0
        return currentMember.isOwner || permissions & (Int32(1) << 6) != 0
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Members").font(.title3.bold())
                Spacer()
                if canManageMembers, project.team != nil {
                    Button { isInviting = true } label: {
                        Image(systemName: "person.badge.plus").frame(width: 34, height: 34)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.rinthyGreen)
                }
            }

            if isLoading {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Loading members").foregroundStyle(.secondary)
                }
                .padding(.vertical, 28)
            } else if let errorMessage, members.isEmpty, organizationMembers.isEmpty {
                emptyState("Members unavailable", errorMessage)
            } else if members.isEmpty && organizationMembers.isEmpty {
                emptyState("No members found", "Team members for this project will appear here.")
            } else {
                if !organizationMembers.isEmpty {
                    Text(organizationName.map { "Organization · \($0)" } ?? "Organization members")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                    Text("These people manage the project through the organization.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    LazyVStack(spacing: 0) {
                        ForEach(organizationMembers, id: \.user.id) { member in
                            ManagedMemberCard(
                                member: member,
                                canManage: false,
                                isCurrentUser: member.user.id == model.currentAccountID,
                                onEdit: {},
                                onRemove: {},
                                onJoin: {}
                            )
                        }
                    }
                }

                Text("Project team")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .padding(.top, 10)

                if members.isEmpty {
                    Text("No direct project collaborators yet. Invite people here, or manage organization members under Teams.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 8)
                } else {
                    LazyVStack(spacing: 0) {
                        ForEach(members, id: \.user.id) { member in
                            ManagedMemberCard(
                                member: member,
                                canManage: canManageMembers,
                                isCurrentUser: member.user.id == model.currentAccountID,
                                onEdit: { editingMember = member },
                                onRemove: { Task { await remove(member) } },
                                onJoin: { Task { await join() } }
                            )
                        }
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

    private func emptyState(_ title: String, _ message: String) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title).font(.headline)
            Text(message).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.rinthySurface, in: RoundedRectangle(cornerRadius: 8))
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
    let canManage: Bool
    let isCurrentUser: Bool
    let onEdit: () -> Void
    let onRemove: () -> Void
    let onJoin: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 12) {
                AsyncImage(url: URL(string: member.user.avatarUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: { Circle().fill(.quaternary) }
                .frame(width: 44, height: 44)
                .clipShape(Circle())

                VStack(alignment: .leading, spacing: 3) {
                    HStack {
                        Text(member.user.username).font(.headline.bold())
                        if member.isOwner { Image(systemName: "crown.fill").foregroundStyle(Color.rinthyGreen) }
                    }
                    Text(member.role).font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer()
                if canManage && !member.isOwner {
                    Button(action: onEdit) { Image(systemName: "pencil") }
                    Button(role: .destructive, action: onRemove) { Image(systemName: "trash") }
                }
            }
                HStack {
                    Label(member.accepted ? "Accepted" : "Pending", systemImage: member.accepted ? "checkmark" : "envelope")
                        .font(.caption.bold())
                        .foregroundStyle(member.accepted ? Color.rinthyGreen : Color.orange)
                    if isCurrentUser && !member.accepted {
                        Button("Accept invitation", action: onJoin).buttonStyle(.borderedProminent).tint(.rinthyGreen)
                    }
                }
            }
            .padding(14)
            Divider()
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
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Button("Search") { Task { await search() } }
                        .disabled(query.trimmingCharacters(in: .whitespaces).isEmpty || isSearching)
                }
                if isSearching { ProgressView() }
                if let result {
                    Section("Result") {
                        HStack {
                            AsyncImage(url: URL(string: result.avatarUrl ?? "")) { image in
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
            .navigationTitle("Invite member")
            .navigationBarTitleDisplayMode(.inline)
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
        _payout = State(initialValue: member.payoutsSplit.map { String($0.doubleValue) } ?? "")
        _ordering = State(initialValue: String(member.ordering))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Member") {
                    TextField("Role", text: $role)
                    TextField("Payout split", text: $payout).keyboardType(.decimalPad)
                    TextField("Ordering", text: $ordering).keyboardType(.numberPad)
                }
                Section(showOrganizationPermissions ? "Default project permissions" : "Permissions") {
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
                    Section("Organization permissions") {
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
                    Button("Transfer ownership") { Task { await transfer() } }
                }
                if let localError { Section { Text(localError).foregroundStyle(.red) } }
            }
            .navigationTitle("Edit member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { Task { await save() } } }
            }
        }
    }

    @MainActor
    private func save() async {
        guard let teamID else {
            localError = "Team id is missing."
            return
        }
        let update = ProjectMemberUpdate(
            role: role,
            permissions: KotlinInt(int: permissions),
            organizationPermissions: showOrganizationPermissions
                ? KotlinInt(int: organizationPermissions)
                : nil,
            payoutsSplit: Double(payout).map { KotlinDouble(double: $0) },
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

    private let permissionNames = [
        "Upload versions", "Delete versions", "Edit details", "Edit description", "Manage invites",
        "Remove members", "Edit members", "Delete project", "View analytics", "View payouts"
    ]

    private let orgPermissionNames = [
        "Edit organization", "Manage invites", "Remove members", "Edit members",
        "Add projects", "Remove projects", "Delete organization", "Edit default project permissions"
    ]
}
