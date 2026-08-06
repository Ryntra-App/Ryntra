import RyntraShared
import SwiftUI

struct OrganizationsView: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let organizations: [Organization]
#if os(macOS)
    /// Opening a team is routed through the dashboard so it joins the same
    /// navigation path as profile and notifications. Keeping it local would put
    /// it outside `path`, and then picking a tab could not dismiss it.
    let onOpenOrganization: (Organization) -> Void
#else
    @State private var selectedOrganization: Organization?
#endif
    @State private var query = ""

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    private var visibleOrganizations: [Organization] {
        guard !query.isEmpty else { return organizations }
        return organizations.filter {
            $0.name.localizedCaseInsensitiveContains(query) ||
                $0.slug.localizedCaseInsensitiveContains(query) ||
                $0.description_.localizedCaseInsensitiveContains(query)
        }
    }

    private var totalMembers: Int {
        organizations.reduce(0) { $0 + Int($1.members.count) }
    }

    var body: some View {
#if os(macOS)
        organizationList
#else
        Group {
            if let selectedOrganization {
                OrganizationDetailView(organization: selectedOrganization)
            } else {
                organizationList
            }
        }
        .toolbar {
            if selectedOrganization != nil {
                ToolbarItem(placement: .ryntraLeading) {
                    Button {
                        selectedOrganization = nil
                    } label: {
                        Label(NSLocalizedString("Teams", comment: "Back to teams"), systemImage: "chevron.left")
                    }
                }
            }
        }
#endif
    }

    private var organizationList: some View {
        List {
#if os(macOS)
            // A `searchable` field lands in the window titlebar on macOS, so it
            // would appear and disappear as this tab comes and goes, shoving
            // the rest of the toolbar around. Keeping it in the content mirrors
            // how Projects searches and leaves the titlebar identical on every
            // tab.
            Section {
                searchField
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            }
#endif

            Section {
                teamsSummary
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            }

            Section {
                if visibleOrganizations.isEmpty {
                    EmptyStateView(
                        title: organizations.isEmpty
                            ? NSLocalizedString("No organizations yet", comment: "Teams empty")
                            : NSLocalizedString("No matching teams", comment: "Teams search empty"),
                        systemImage: "person.3",
                        message: organizations.isEmpty
                            ? NSLocalizedString(
                                "Create or join an organization on Modrinth. Personal projects stay in Projects.",
                                comment: "Teams empty hint"
                            )
                            : NSLocalizedString("Try another name, slug, or description.", comment: "Teams search hint")
                    )
                    .listRowSeparator(.hidden)

                    if organizations.isEmpty {
                        Link(destination: URL(string: "https://modrinth.com/dashboard/organizations")!) {
                            Label(
                                NSLocalizedString("Manage organizations on Modrinth", comment: "Open orgs"),
                                systemImage: "arrow.up.right.square"
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                        }
                        .tint(Color.ryntraGreen)
                        .listRowSeparator(.hidden)
                    }
                } else {
                    ForEach(visibleOrganizations, id: \.id) { organization in
                        Button {
#if os(macOS)
                            onOpenOrganization(organization)
#else
                            selectedOrganization = organization
#endif
                        } label: {
                            OrganizationCard(organization: organization)
                        }
                        .buttonStyle(.plain)
                        .ryntraHoverHighlight()
                        .contextMenu {
                            if let url = URL(string: "https://modrinth.com/organization/\(organization.slug)") {
                                Button {
                                    ryntraOpenExternalURL(url)
                                } label: {
                                    Label(
                                        NSLocalizedString("Open on Modrinth", comment: "Project context action"),
                                        systemImage: "arrow.up.right.square"
                                    )
                                }
                                Button {
                                    ryntraCopyToPasteboard(url.absoluteString)
                                } label: {
                                    Label(
                                        NSLocalizedString("Copy link", comment: "Project context action"),
                                        systemImage: "link"
                                    )
                                }
                            }
                        }
                        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                    }
                }
            } header: {
                VStack(alignment: .leading, spacing: 4) {
                    RyntraSectionLabel(text: NSLocalizedString("Managed teams", comment: "Teams section"))
                    Text(NSLocalizedString("Organizations on Modrinth where you collaborate with others", comment: "Teams hint"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .textCase(nil)
                }
            }

            if !isPlatformNative {
                Color.clear
                    .frame(height: 90)
                    .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .ryntraOpaqueListBackground()
#if !os(macOS)
        .searchable(
            text: $query,
            prompt: NSLocalizedString("Search teams", comment: "Teams search")
        )
#endif
        .refreshable { model.refresh() }
    }

#if os(macOS)
    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField(NSLocalizedString("Search teams", comment: "Teams search"), text: $query)
                .textFieldStyle(.plain)
                .ryntraNoAutocapitalization()
                .autocorrectionDisabled()
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(NSLocalizedString("Clear search", comment: "Search action"))
            }
        }
        .padding(.horizontal, 14)
        .frame(minHeight: 40)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
#endif

    private var teamsSummary: some View {
        HStack(spacing: 0) {
            summaryMetric("\(organizations.count)", label: NSLocalizedString("Teams", comment: "Teams metric"))
            summaryMetric("\(totalMembers)", label: NSLocalizedString("Members", comment: "Members metric"))
        }
        .padding(.vertical, 13)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.ryntraSeparator, lineWidth: 0.5))
    }

    private func summaryMetric(_ value: String, label: String) -> some View {
        VStack(spacing: 1) {
            Text(value).font(.headline).monospacedDigit()
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct OrganizationCard: View {
    let organization: Organization

    private var members: [ProjectMember] {
        organization.members
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                organizationIcon(size: 56, corner: 12)
                VStack(alignment: .leading, spacing: 3) {
                    Text(organization.name).fontWeight(.semibold).lineLimit(1)
                    Text("@\(organization.slug)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.ryntraGreen)
                        .lineLimit(1)
                    if !organization.description_.isEmpty {
                        Text(organization.description_)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                            .padding(.top, 2)
                    }
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }

            HStack {
                MemberAvatarStack(members: members, maxVisible: 5)
                Spacer()
                Text(memberCountLabel)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.ryntraSeparator, lineWidth: 0.5))
    }

    private var memberCountLabel: String {
        let count = members.count
        if count == 0 {
            return NSLocalizedString("Members unavailable", comment: "No member payload")
        }
        return String.localizedStringWithFormat(
            NSLocalizedString("%d members", comment: "Member count"),
            count
        )
    }

    private func organizationIcon(size: CGFloat, corner: CGFloat) -> some View {
        RemoteImage(url: URL(string: organization.iconUrl ?? "")) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            RoundedRectangle(cornerRadius: corner)
                .fill(.quaternary)
                .overlay {
                    Text(String(organization.name.prefix(1)).uppercased())
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: corner))
    }
}

private struct MemberAvatarStack: View {
    let members: [ProjectMember]
    var maxVisible = 5

    var body: some View {
        let visible = Array(members.prefix(maxVisible))
        HStack(spacing: -10) {
            if visible.isEmpty {
                Circle()
                    .fill(Color.ryntraGreen.opacity(0.12))
                    .frame(width: 28, height: 28)
                    .overlay {
                        Image(systemName: "person.2.fill")
                            .font(.caption2)
                            .foregroundStyle(Color.ryntraGreen)
                    }
            } else {
                ForEach(Array(visible.enumerated()), id: \.element.user.id) { _, member in
                    RemoteImage(url: URL(string: member.user.avatarUrl ?? "")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle().fill(.quaternary)
                    }
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.ryntraSurface, lineWidth: 1.5))
                }
                if members.count > maxVisible {
                    Circle()
                        .fill(Color.ryntraSurfaceRaised)
                        .frame(width: 28, height: 28)
                        .overlay {
                            Text("+\(members.count - maxVisible)")
                                .font(.caption2.weight(.semibold))
                        }
                        .overlay(Circle().stroke(Color.ryntraSurface, lineWidth: 1.5))
                }
            }
        }
    }
}

struct OrganizationDetailView: View {
    @EnvironmentObject private var model: AppModel

    let organization: Organization
    @State private var detail: Organization?
    @State private var projects: [Project] = []
    @State private var members: [ProjectMember] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var isInviting = false
    @State private var editingMember: ProjectMember?
    @State private var localError: String?

    private var displayOrganization: Organization {
        detail ?? organization
    }

    private var displayMembers: [ProjectMember] {
        if !members.isEmpty { return members }
        return displayOrganization.members
    }

    private var teamID: String? {
        displayOrganization.teamId
    }

    private var currentMember: ProjectMember? {
        displayMembers.first { $0.user.id == model.currentAccountID }
    }

    private var canManageMembers: Bool {
        guard let currentMember else { return false }
        if currentMember.isOwner { return true }
        let orgPerms = (currentMember.organizationPermissions as? NSNumber)?.int32Value ?? 0
        // Manage invites (1), remove (2), edit member (3)
        return orgPerms & (Int32(1) << 1) != 0
            || orgPerms & (Int32(1) << 2) != 0
            || orgPerms & (Int32(1) << 3) != 0
    }

    var body: some View {
        List {
            Section {
                organizationHeader
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            }

            Section {
                if isLoading && displayMembers.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text(NSLocalizedString("Loading members", comment: "Org members loading"))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 12)
                } else if displayMembers.isEmpty {
                    Text(NSLocalizedString("No members yet. Invite collaborators to this organization.", comment: "Org members empty"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(displayMembers, id: \.user.id) { member in
                        ManagedMemberCard(
                            member: member,
                            canManage: canManageMembers && teamID != nil,
                            isCurrentUser: member.user.id == model.currentAccountID,
                            onEdit: { editingMember = member },
                            onRemove: { Task { await remove(member) } },
                            onJoin: { Task { await join() } }
                        )
                        .listRowInsets(EdgeInsets())
                        .listRowSeparator(.hidden)
                    }
                }
                if let localError {
                    Text(localError).font(.caption).foregroundStyle(.red)
                }
            } header: {
                HStack {
                    RyntraSectionLabel(text: NSLocalizedString("Members", comment: "Members section"))
                    Spacer()
                    if canManageMembers, teamID != nil {
                        Button {
                            isInviting = true
                        } label: {
                            Image(systemName: "person.badge.plus")
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.ryntraGreen)
                    }
                }
            }

            Section {
                if isLoading && projects.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text(NSLocalizedString("Loading projects", comment: "Org projects loading"))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 12)
                } else if let errorMessage, projects.isEmpty {
                    EmptyStateView(
                        title: NSLocalizedString("Projects unavailable", comment: "Org projects error"),
                        systemImage: "exclamationmark.triangle",
                        message: errorMessage
                    )
                } else if projects.isEmpty {
                    EmptyStateView(
                        title: NSLocalizedString("No organization projects", comment: "Org projects empty"),
                        systemImage: "shippingbox",
                        message: NSLocalizedString(
                            "Projects transferred into this organization will appear here.",
                            comment: "Org projects empty hint"
                        )
                    )
                } else {
                    ForEach(projects, id: \.id) { project in
                        NavigationLink {
                            ProjectDetailView(project: project, isReadOnly: false)
                                .navigationTitle(project.title)
                                .ryntraInlineNavigationTitle()
                        } label: {
                            ProjectRow(
                                project: project,
                                showDescription: true,
                                showStatus: true,
                                showsDisclosureIndicator: false
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            } header: {
                RyntraSectionLabel(
                    text: String.localizedStringWithFormat(
                        NSLocalizedString("%d projects", comment: "Project count"),
                        projects.count
                    )
                )
            }
        }
        .listStyle(.plain)
        .task(id: organization.id) {
            await loadDetail()
        }
        .sheet(isPresented: $isInviting) {
            InviteMemberSheet(teamID: teamID) {
                await reloadMembers()
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
                    teamID: teamID,
                    member: editingMember,
                    showOrganizationPermissions: true
                ) {
                    await reloadMembers()
                    self.editingMember = nil
                }
            }
        }
    }

    private var organizationHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                RemoteImage(url: URL(string: displayOrganization.iconUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 14).fill(.quaternary)
                }
                .frame(width: 72, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 4) {
                    Text(displayOrganization.name).font(.title2.bold())
                    Text("@\(displayOrganization.slug)")
                        .font(.subheadline)
                        .foregroundStyle(Color.ryntraGreen)
                }
            }

            if !displayOrganization.description_.isEmpty {
                Text(displayOrganization.description_)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 10) {
                metricChip(
                    value: "\(displayMembers.count)",
                    label: NSLocalizedString("Members", comment: "Members"),
                    symbol: "person.2.fill"
                )
                metricChip(
                    value: isLoading && projects.isEmpty ? "—" : "\(projects.count)",
                    label: NSLocalizedString("Projects", comment: "Projects"),
                    symbol: "shippingbox.fill"
                )
            }

            if let organizationURL = URL(string: "https://modrinth.com/organization/\(displayOrganization.slug)") {
                Link(destination: organizationURL) {
                    Label(
                        NSLocalizedString("Open on Modrinth", comment: "Open org page"),
                        systemImage: "arrow.up.right.square"
                    )
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.bordered)
                .tint(Color.ryntraGreen)
            }
        }
        .padding(16)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.ryntraSeparator, lineWidth: 0.5))
    }

    private func metricChip(value: String, label: String, symbol: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).foregroundStyle(Color.ryntraGreen)
            VStack(alignment: .leading, spacing: 1) {
                Text(value).fontWeight(.bold)
                Text(label).font(.caption2).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity)
        .background(Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 10))
    }

    private func loadDetail() async {
        isLoading = true
        errorMessage = nil
        members = organization.members
        do {
            let loaded = try await model.loadOrganizationDetail(organization: organization)
            detail = loaded.organization
            projects = loaded.projects
            members = loaded.members
        } catch {
            errorMessage = error.localizedDescription
            // Keep list-level members if detail fails.
            if members.isEmpty {
                members = organization.members
            }
        }
        isLoading = false
    }

    private func reloadMembers() async {
        do {
            members = try await model.loadOrganizationMembers(organization: displayOrganization)
            localError = nil
        } catch {
            localError = error.localizedDescription
        }
    }

    @MainActor
    private func remove(_ member: ProjectMember) async {
        guard let teamID else { return }
        do {
            try await model.removeMember(teamID: teamID, userID: member.user.id)
            await reloadMembers()
        } catch {
            localError = error.localizedDescription
        }
    }

    @MainActor
    private func join() async {
        guard let teamID else { return }
        do {
            try await model.joinTeam(teamID: teamID)
            await reloadMembers()
        } catch {
            localError = error.localizedDescription
        }
    }
}

private struct OrganizationMemberRow: View {
    let member: ProjectMember

    var body: some View {
        HStack(spacing: 12) {
            RemoteImage(url: URL(string: member.user.avatarUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle().fill(.quaternary)
            }
            .frame(width: 44, height: 44)
            .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(member.user.username).fontWeight(.semibold).lineLimit(1)
                    if member.isOwner {
                        Image(systemName: "crown.fill")
                            .font(.caption2)
                            .foregroundStyle(.orange)
                            .accessibilityLabel(NSLocalizedString("Owner", comment: "Org owner"))
                    }
                }
                Text(member.role.isEmpty ? NSLocalizedString("Member", comment: "Default role") : member.role)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            if !member.accepted {
                Text(NSLocalizedString("Pending", comment: "Invite pending"))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.orange)
            }
        }
        .padding(.vertical, 4)
    }
}
