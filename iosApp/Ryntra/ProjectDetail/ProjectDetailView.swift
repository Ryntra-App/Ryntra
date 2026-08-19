import RyntraShared
import SwiftUI

struct ProjectDetailView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    @State private var project: Project
    let isReadOnly: Bool
    @State private var selectedTab = ProjectDetailTab.overview
    @State private var versions: [ProjectVersion] = []
    @State private var dependencies: [ProjectDependency] = []
    @State private var members: [ProjectMember] = []
    @State private var organizationMembers: [ProjectMember] = []
    @State private var organizationName: String?
    @State private var markdownBlocks: [MarkdownBlock]
    @State private var isLoadingVersions = false
    @State private var isLoadingMembers = false
    @State private var versionError: String?
    @State private var memberError: String?
    @State private var isConfirmingSubmission = false
    @State private var isShowingShareCard = false
    @State private var isDeletingProject = false
    @State private var submissionError: String?
    @State private var editHasChanges = false
    @State private var editCanSave = false
    @State private var editSaveRequest = 0
    @State private var isSavingDisclosures = false
    @State private var pendingExit: PendingProjectExit?
    @State private var isConfirmingUnsavedChanges = false
    @State private var selectedGalleryURL: URL?

    init(project: Project, isReadOnly: Bool = false) {
        _project = State(initialValue: project)
        self.isReadOnly = isReadOnly
        _markdownBlocks = State(initialValue: [])
    }

    private var resources: [(String, URL)] {
        [
            (NSLocalizedString("Source", comment: "Project resource"), project.sourceUrl),
            (NSLocalizedString("Issues", comment: "Project resource"), project.issuesUrl),
            (NSLocalizedString("Wiki", comment: "Project resource"), project.wikiUrl),
            (NSLocalizedString("Discord", comment: "Project resource"), project.discordUrl),
        ].compactMap { label, url in
            guard let url, let parsed = URL(string: url), !url.isEmpty else { return nil }
            return (label, parsed)
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                identity
                projectTabs

                switch selectedTab {
                case .overview:
                    overviewContent
                case .versions:
                    ProjectVersionsManagementView(
                        project: project,
                        versions: versions,
                        canCreateOrEdit: !isReadOnly && hasProjectPermission(0),
                        canDelete: !isReadOnly && hasProjectPermission(1),
                        isLoading: isLoadingVersions,
                        errorMessage: versionError,
                        onReload: { await loadVersions() }
                    )
                case .disclosures:
                    ProjectDisclosuresView(
                        project: project,
                        canEditDetails: hasProjectPermission(2),
                        versionCount: versions.count,
                        saveRequest: editSaveRequest,
                        onEditingStateChanged: { hasChanges, canSave in
                            editHasChanges = hasChanges
                            editCanSave = canSave
                        },
                        onSavingChanged: { isSavingDisclosures = $0 },
                        onSaved: { disclosuresDidSave() }
                    )
                case .edit:
                    VStack(spacing: 16) {
                        EditProjectView(
                            project: project,
                            canEditDetails: hasProjectPermission(2),
                            canEditBody: hasProjectPermission(3),
                            saveRequest: editSaveRequest,
                            onSaved: { await editDidSave() },
                            onEditingStateChanged: { hasChanges, canSave in
                                editHasChanges = hasChanges
                                editCanSave = canSave
                            }
                        )
                        if hasProjectPermission(7) {
                            projectDeleteAction
                        }
                    }
                case .members:
                    ProjectMembersManagementView(
                        project: project,
                        members: members,
                        organizationMembers: organizationMembers,
                        organizationName: organizationName,
                        isLoading: isLoadingMembers,
                        errorMessage: memberError,
                        onReload: { await loadMembers() }
                    )
                case .moderation:
                    ProjectModerationView(
                        project: project,
                        currentUserID: model.currentAccountID,
                        versionCount: Int32(versions.count),
                        canSubmitProject: hasProjectPermission(2),
                        isSubmittingProject: model.isProjectActionRunning,
                        submissionError: submissionError ?? model.projectActionError,
                        onSubmitProject: { isConfirmingSubmission = true }
                    )
                }
            }
            .frame(maxWidth: 760, alignment: .leading)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 36)
        }
        .ryntraInteractiveKeyboardDismissal()
        .ryntraScreenBackground(Color.ryntraBackground)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if isEditingTab, editHasChanges {
                editSaveBar
            }
        }
        .interactiveDismissDisabled(isEditingTab && editHasChanges)
#if !os(macOS)
        .navigationBarBackButtonHidden(isEditingTab && editHasChanges)
        .toolbar {
            if isEditingTab, editHasChanges {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        pendingExit = .dismiss
                        isConfirmingUnsavedChanges = true
                    } label: {
                        Label(NSLocalizedString("Back", comment: "Navigation action"), systemImage: "chevron.backward")
                    }
                }
            }
        }
#endif
        .task(id: project.id) {
            async let loadedVersions: Void = loadVersions()
            async let loadedMembers: Void = loadMembersIfAllowed()
            async let loadedMarkdown = parseMarkdown(project.body)
            markdownBlocks = await loadedMarkdown
            _ = await (loadedVersions, loadedMembers)
        }
        .confirmationDialog(
            project.status.lowercased() == "draft"
                ? NSLocalizedString("Submit to Modrinth moderation?", comment: "Project submission title")
                : NSLocalizedString("Resubmit to Modrinth moderation?", comment: "Project submission title"),
            isPresented: $isConfirmingSubmission,
            titleVisibility: .visible
        ) {
            Button(project.status.lowercased() == "draft"
                ? NSLocalizedString("Submit for review", comment: "Project submission action")
                : NSLocalizedString("Resubmit for review", comment: "Project submission action")) {
                Task { await submitForModeration() }
            }
            Button(NSLocalizedString("Cancel", comment: "Cancel"), role: .cancel) {}
        } message: {
            Text(NSLocalizedString(
                "Modrinth will review the project page, files, metadata and license before publishing it.",
                comment: "Project submission confirmation"
            ))
        }
        .confirmationDialog(
            NSLocalizedString("Save changes before leaving?", comment: "Unsaved project changes"),
            isPresented: $isConfirmingUnsavedChanges,
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("Save changes", comment: "Save project")) {
                guard editCanSave else { return }
                editSaveRequest += 1
            }
            .disabled(!editCanSave)
            Button(NSLocalizedString("Discard changes", comment: "Discard project edits"), role: .destructive) {
                completePendingExit()
            }
            Button(NSLocalizedString("Keep editing", comment: "Keep project edits"), role: .cancel) {
                pendingExit = nil
            }
        } message: {
            Text(NSLocalizedString(
                "Your project edits have not been saved.",
                comment: "Unsaved project changes detail"
            ))
        }
        .sheet(isPresented: $isDeletingProject) {
            ProjectDeleteSheet(project: project) {
                isDeletingProject = false
                dismiss()
            }
            .environmentObject(model)
        }
        .sheet(isPresented: $isShowingShareCard) {
            ProjectShareCardStudio(project: project, versions: versions)
        }
        .sheet(
            isPresented: Binding(
                get: { selectedGalleryURL != nil },
                set: { if !$0 { selectedGalleryURL = nil } }
            )
        ) {
            if let selectedGalleryURL {
                ProjectGalleryPreview(url: selectedGalleryURL)
            }
        }
    }

    @ViewBuilder
    private var overviewContent: some View {
        metrics
        section("Summary", value: project.description_.isEmpty ? "No summary provided." : project.description_)

        if !markdownBlocks.isEmpty {
            DetailHeading(title: "Description")
            VStack(alignment: .leading, spacing: 9) {
                ForEach(Array(markdownBlocks.enumerated()), id: \.offset) { _, block in
                    MarkdownBlockView(block: block)
                }
            }
        }

        environment
        dependenciesList
        categories
        gallery
        details
        resourcesList
    }

    private var projectTabs: some View {
        Group {
            if horizontalSizeClass == .compact {
                Menu {
                    ForEach(availableTabs, id: \.self) { tab in
                        Button {
                            requestTab(tab)
                        } label: {
                            Label(tab.label, systemImage: tab.symbol)
                        }
                    }
                } label: {
                    HStack(spacing: 10) {
                        Label(selectedTab.label, systemImage: selectedTab.symbol)
                            .font(.subheadline.weight(.semibold))
                        Spacer()
                        Text(NSLocalizedString("Project section", comment: "Project navigation"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                    .foregroundStyle(.primary)
                    .frame(minHeight: 44)
                    .padding(.horizontal, 12)
                    .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
                    .overlay {
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.ryntraSeparator, lineWidth: 0.5)
                    }
                }
                .accessibilityLabel(NSLocalizedString("Project section", comment: "Project navigation"))
                .accessibilityValue(selectedTab.label)
            } else if availableTabs.count > 4 {
                ScrollView(.horizontal, showsIndicators: false) {
                    projectTabBar(isScrollable: true)
                }
            } else {
                projectTabBar(isScrollable: false)
            }
        }
        .padding(.bottom, 18)
    }

    private func projectTabBar(isScrollable: Bool) -> some View {
        HStack(spacing: 0) {
            ForEach(availableTabs, id: \.self) { tab in
                Button {
                    requestTab(tab)
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: tab.symbol)
                            .font(.caption.weight(.semibold))
                        Text(tab.label)
                            .font(.caption.weight(selectedTab == tab ? .bold : .semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }
                    .foregroundStyle(selectedTab == tab ? Color.ryntraGreen : Color.secondary)
                    .frame(maxWidth: isScrollable ? nil : .infinity)
                    .frame(width: isScrollable ? 112 : nil)
                    .frame(height: 44)
                    .background(
                        selectedTab == tab ? Color.ryntraGreen.opacity(0.10) : Color.clear,
                        in: RoundedRectangle(cornerRadius: 8)
                    )
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selectedTab == tab ? .isSelected : [])
            }
        }
        .padding(3)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 11))
        .overlay {
            RoundedRectangle(cornerRadius: 11)
                .stroke(Color.primary.opacity(0.08), lineWidth: 0.5)
        }
    }

    private func loadVersions() async {
        isLoadingVersions = true
        versionError = nil
        do {
            let loaded = try await model.loadProjectVersions(project: project)
            versions = loaded
            dependencies = try await model.loadProjectDependencies(versions: loaded)
        } catch {
            versionError = error.localizedDescription
        }
        isLoadingVersions = false
    }

    private func reloadProject() async {
        do {
            project = try await model.loadProjectDetails(project: project)
            markdownBlocks = await parseMarkdown(project.body)
        } catch {
            versionError = error.localizedDescription
        }
    }

    private func parseMarkdown(_ source: String) async -> [MarkdownBlock] {
        await Task.detached(priority: .userInitiated) {
            MarkdownParser.shared.parse(markdown: source)
        }.value
    }

    private func loadMembers() async {
        isLoadingMembers = true
        memberError = nil
        do {
            // Refresh project first so team_id / organization are present for org projects.
            if project.team == nil || project.organization == nil {
                project = try await model.loadProjectDetails(project: project)
            }
            let roster = try await model.loadProjectTeamRoster(project: project)
            members = roster.projectMembers
            organizationMembers = roster.organizationMembers
            organizationName = roster.organization?.name
        } catch {
            memberError = error.localizedDescription
        }
        isLoadingMembers = false
    }

    private func loadMembersIfAllowed() async {
        guard !isReadOnly else { return }
        await loadMembers()
    }

    private var availableTabs: [ProjectDetailTab] {
        isReadOnly ? [.overview, .versions] : ProjectDetailTab.allCases
    }

    /// Both the editor and the disclosures tab keep unsaved work, and share the one save bar.
    private var isEditingTab: Bool { selectedTab == .edit || selectedTab == .disclosures }

    private var isSavingEdits: Bool {
        selectedTab == .disclosures ? isSavingDisclosures : model.isProjectSaving
    }

    @MainActor private func disclosuresDidSave() {
        editHasChanges = false
        editCanSave = false
        completePendingExit()
    }

    @MainActor private func editDidSave() async {
        await reloadProject()
        editHasChanges = false
        editCanSave = false
        completePendingExit()
    }

    private func requestTab(_ tab: ProjectDetailTab) {
        guard tab != selectedTab else { return }
        if isEditingTab, editHasChanges {
            pendingExit = .tab(tab)
            isConfirmingUnsavedChanges = true
        } else {
            selectedTab = tab
        }
    }

    private func completePendingExit() {
        guard let pendingExit else { return }
        self.pendingExit = nil
        editHasChanges = false
        editCanSave = false
        switch pendingExit {
        case .tab(let tab): selectedTab = tab
        case .dismiss: dismiss()
        }
    }

    private var editSaveBar: some View {
        VStack(spacing: 0) {
            Divider()
            Button {
                editSaveRequest += 1
            } label: {
                HStack(spacing: 8) {
                    if isSavingEdits {
                        ProgressView().controlSize(.small)
                    } else {
                        Image(systemName: "checkmark")
                    }
                    Text(NSLocalizedString(
                        isSavingEdits ? "Saving…" : "Save changes",
                        comment: "Project save action"
                    ))
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 44)
            }
            .buttonStyle(.borderedProminent)
            .tint(.ryntraGreen)
            .disabled(!editCanSave || isSavingEdits)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(.regularMaterial)
    }

    private var currentMemberships: [ProjectMember] {
        (members + organizationMembers).filter { $0.user.id == model.currentAccountID }
    }

    private var isCurrentUserOwner: Bool {
        currentMemberships.contains { $0.isOwner }
    }

    private var effectiveProjectPermissions: Int32 {
        currentMemberships.reduce(0) { accumulated, membership in
            accumulated | ((membership.permissions as? NSNumber)?.int32Value ?? 0)
        }
    }

    private func hasProjectPermission(_ bit: Int32) -> Bool {
        guard !currentMemberships.isEmpty else { return false }
        return isCurrentUserOwner || effectiveProjectPermissions & (Int32(1) << bit) != 0
    }

    private var projectDeleteAction: some View {
        Button(role: .destructive) {
            isDeletingProject = true
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "trash")
                VStack(alignment: .leading, spacing: 2) {
                    Text(NSLocalizedString("Delete this project", comment: "Project destructive action"))
                        .font(.body.weight(.medium))
                    Text(NSLocalizedString(
                        "Permanently remove the project, its versions and attached data.",
                        comment: "Project destructive action hint"
                    ))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(model.isProjectActionRunning)
        .padding(.vertical, 10)
    }

    @MainActor private func submitForModeration() async {
        submissionError = nil
        do {
            try await model.submitProjectForModeration(project: project)
            await reloadProject()
        } catch {
            submissionError = error.localizedDescription
        }
    }

    private var identity: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: 14) {
                identityArtwork
                identityDetails
                Spacer(minLength: 8)
                shareCardButton
            }
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    identityArtwork
                    Spacer()
                    shareCardButton
                }
                identityDetails
            }
        }
        .padding(.bottom, 22)
    }

    private var shareCardButton: some View {
        Button {
            isShowingShareCard = true
        } label: {
            Image(systemName: "square.and.arrow.up")
                .font(.body.weight(.semibold))
                .frame(width: 44, height: 44)
                .background(Color.ryntraGreen.opacity(0.12), in: Circle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(Color.ryntraGreen)
        .accessibilityLabel(NSLocalizedString("Create share card", comment: "Project share card action"))
        .accessibilityHint(NSLocalizedString(
            "Customize and share a project image.",
            comment: "Project share card action hint"
        ))
    }

    private var identityArtwork: some View {
        ProjectArtwork(project: project)
            .frame(width: 76, height: 76)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var identityDetails: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(project.title)
                .font(.title2.bold())
                .fixedSize(horizontal: false, vertical: true)
            Text(project.displayTypeLabel)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            if let organizationName {
                Label(organizationName, systemImage: "building.2")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(projectAccessSummary)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            if project.status != "approved" {
                Label(
                    project.localizedStatusLabel,
                    systemImage: project.status == "rejected" ? "exclamationmark.circle.fill" : "clock.fill"
                )
                .font(.caption2.weight(.semibold))
                .foregroundStyle(project.status == "rejected" ? Color.red : Color.orange)
                .padding(.top, 3)
            }
        }
    }

    private var metrics: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 0) {
                metric("Downloads", value: ryntraExactCount(project.downloads), systemImage: "arrow.down")
                Divider().frame(height: 50)
                metric("Followers", value: ryntraExactCount(project.followers), systemImage: "heart")
            }
            VStack(spacing: 10) {
                metric("Downloads", value: ryntraExactCount(project.downloads), systemImage: "arrow.down")
                Divider()
                metric("Followers", value: ryntraExactCount(project.followers), systemImage: "heart")
            }
        }
        .padding(.vertical, 14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
    }

    private var projectAccessSummary: String {
        guard !currentMemberships.isEmpty else {
            return NSLocalizedString("Access unavailable", comment: "Project access")
        }
        if isCurrentUserOwner {
            return NSLocalizedString("Owner · full project access", comment: "Project access")
        }
        let permissionCount = (0..<10).filter { effectiveProjectPermissions & (Int32(1) << $0) != 0 }.count
        if permissionCount == 0 {
            return NSLocalizedString("View only", comment: "Project access")
        }
        return String.localizedStringWithFormat(
            NSLocalizedString("%d project permissions", comment: "Project access count"),
            permissionCount
        )
    }

    private func metric(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.ryntraGreen)
            VStack(alignment: .leading, spacing: 1) {
                Text(LocalizedStringKey(label)).font(.caption2).foregroundStyle(.secondary)
                Text(value).font(.headline).monospacedDigit()
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func section(_ title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            DetailHeading(title: title)
            Text(value)
                .font(.body)
                .foregroundStyle(.secondary)
        }
    }

    private var environment: some View {
        VStack(alignment: .leading, spacing: 10) {
            DetailHeading(title: "Environment")
            HStack(spacing: 10) {
                environmentValue("Client", value: project.clientSide, systemImage: "desktopcomputer")
                environmentValue("Server", value: project.serverSide, systemImage: "server.rack")
            }
        }
    }

    @ViewBuilder
    private var dependenciesList: some View {
        if !dependencies.isEmpty {
            DetailHeading(title: "Dependencies")
            VStack(spacing: 8) {
                ForEach(Array(dependencies.enumerated()), id: \.offset) { _, dependency in
                    HStack(spacing: 10) {
                        RemoteImage(url: URL(string: dependency.iconUrl ?? "")) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 8).fill(.quaternary)
                        }
                        .frame(width: 40, height: 40)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(
                                dependency.title ?? dependency.fileName ??
                                    NSLocalizedString("External dependency", comment: "Project dependency")
                            )
                                .fontWeight(.semibold)
                            Text(NSLocalizedString(
                                dependency.dependencyType.capitalized,
                                comment: "Project dependency type"
                            ))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 5)
                }
            }
        }
    }

    private func environmentValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 9) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.ryntraGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text(LocalizedStringKey(label)).font(.caption2).foregroundStyle(.secondary)
                Text(NSLocalizedString(value.capitalized, comment: "Project environment value"))
                    .fontWeight(.semibold)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var categories: some View {
        if !project.categories.isEmpty {
            DetailHeading(title: "Categories")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(project.categories, id: \.self) { category in
                        Text(category.capitalized)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 7))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var gallery: some View {
        if !project.gallery.isEmpty {
            DetailHeading(title: "Gallery")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(project.gallery, id: \.url) { image in
                        Button {
                            selectedGalleryURL = URL(string: image.rawUrl ?? image.url)
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(.quaternary)
                                RemoteImage(url: URL(string: image.url)) { loaded in
                                    loaded
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 172, height: 108)
                                        .clipped()
                                } placeholder: {
                                    ProgressView()
                                }
                            }
                            .frame(width: 172, height: 108)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(image.title ?? NSLocalizedString("Open gallery image", comment: "Project gallery"))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var details: some View {
        if project.license != nil || project.published != nil {
            DetailHeading(title: "Details")
            if let license = project.license {
                DisclosureGroup {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(license.id)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color.ryntraGreen)
                        Text(projectLicenseDescription(license.id))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(NSLocalizedString(
                            "This summary is not legal advice. Review the complete terms before publishing or reusing the project.",
                            comment: "Project license legal note"
                        ))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        if let value = license.url, let url = URL(string: value) {
                            Link(destination: url) {
                                Label(
                                    NSLocalizedString("Read complete license terms", comment: "Project license action"),
                                    systemImage: "arrow.up.right.square"
                                )
                            }
                            .font(.subheadline.weight(.semibold))
                        }
                    }
                    .padding(.top, 8)
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "doc.text")
                            .foregroundStyle(Color.ryntraGreen)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(NSLocalizedString("License", comment: "Project detail"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(license.name ?? license.id)
                                .fontWeight(.semibold)
                        }
                    }
                }
                .padding(.vertical, 9)
            }
            if let published = project.published {
                detailValue("Published", value: String(published.prefix(10)), systemImage: "calendar")
            }
        }
    }

    @ViewBuilder
    private var resourcesList: some View {
        if !resources.isEmpty {
            DetailHeading(title: "Resources")
            ForEach(resources, id: \.0) { label, url in
                Link(destination: url) {
                    HStack {
                        Image(systemName: "globe")
                            .foregroundStyle(Color.ryntraGreen)
                        Text(label).fontWeight(.semibold)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 14)
                }
                Divider()
            }
        }
    }

    private func detailValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(Color.ryntraGreen)
            Text(LocalizedStringKey(label)).foregroundStyle(.secondary)
            Spacer()
            Text(value).fontWeight(.semibold).lineLimit(1)
        }
        .padding(.vertical, 9)
    }

}

private struct DetailHeading: View {
    let title: String
    var body: some View {
        Text(NSLocalizedString(title, comment: "Project section").uppercased())
            .font(.caption.weight(.bold))
            .foregroundStyle(Color.ryntraGreen)
            .padding(.top, 30)
            .padding(.bottom, 11)
    }
}

private enum ProjectDetailTab: CaseIterable {
    case overview
    case versions
    case edit
    case disclosures
    case members
    case moderation

    var label: String {
        switch self {
        case .overview: return NSLocalizedString("Overview", comment: "Project tab")
        case .versions: return NSLocalizedString("Versions", comment: "Project tab")
        case .edit: return NSLocalizedString("Edit", comment: "Project tab")
        case .disclosures: return NSLocalizedString("Disclosures", comment: "Project tab")
        case .members: return NSLocalizedString("Members", comment: "Project tab")
        case .moderation: return NSLocalizedString("Moderation", comment: "Project tab")
        }
    }

    var symbol: String {
        switch self {
        case .overview: return "square.grid.2x2.fill"
        case .versions: return "arrow.down.circle.fill"
        case .edit: return "slider.horizontal.3"
        case .disclosures: return "checkmark.shield.fill"
        case .members: return "person.3.fill"
        case .moderation: return "text.bubble.fill"
        }
    }
}

private enum PendingProjectExit {
    case tab(ProjectDetailTab)
    case dismiss
}

private struct ProjectGalleryPreview: View {
    @Environment(\.dismiss) private var dismiss
    let url: URL

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                RemoteImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFit()
                } placeholder: {
                    ProgressView().tint(.white)
                }
                .padding()
            }
            .navigationTitle(NSLocalizedString("Gallery image", comment: "Project gallery"))
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Done", comment: "Close gallery")) { dismiss() }
                }
            }
        }
    }
}
