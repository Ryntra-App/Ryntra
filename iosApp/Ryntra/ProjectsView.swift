import Foundation
import PhotosUI
import RyntraShared
import SwiftUI
import UniformTypeIdentifiers

struct ProjectsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("showFavoriteProjects") private var showFavoriteProjects = true
    @AppStorage("showProjectBanners") private var showProjectBanners = true
    @AppStorage("reduceMotion") private var appReduceMotion = false
    @AppStorage("projectSortMode") private var storedSortMode = ProjectSortMode.popular.rawValue
    @AppStorage("favoriteProjectIds") private var storedFavoriteIds = "[]"
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let projects: [Project]
    var onProjectTap: (Project) -> Void = { _ in }

    @State private var query = ""
    @State private var isCreatingProject = false
    @State private var projectPendingDeletion: Project?
    @State private var projectPendingShareCard: Project?
    @State private var deletePermissions: [String: Bool] = [:]
    @State private var permissionError: String?

    private var sortMode: ProjectSortMode {
        ProjectSortMode(rawValue: storedSortMode) ?? .popular
    }

    private var favoriteIds: Set<String> {
        guard let data = storedFavoriteIds.data(using: .utf8),
              let decoded = try? JSONDecoder().decode([String].self, from: data) else { return [] }
        return Set(decoded)
    }

    private var filteredProjects: [Project] {
        let filtered = query.isEmpty ? projects : projects.filter {
            $0.title.localizedCaseInsensitiveContains(query) ||
                $0.description_.localizedCaseInsensitiveContains(query) ||
                ($0.slug?.localizedCaseInsensitiveContains(query) ?? false)
        }
        return filtered.sortedForDisplay(
            mode: sortMode,
            favoriteIds: showFavoriteProjects ? favoriteIds : []
        )
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                searchField
                projectSummary
                sortPicker
            if filteredProjects.isEmpty {
                EmptyStateView(
                    title: projects.isEmpty ? "No projects yet" : "No matching projects",
                    systemImage: "shippingbox",
                    message: projects.isEmpty
                        ? "Managed projects will appear here."
                        : "Try another title, slug, or summary."
                )
            } else {
                ForEach(filteredProjects, id: \.id) { project in
                    Group {
                        if showProjectBanners {
                            ZStack(alignment: .topTrailing) {
                                Button { onProjectTap(project) } label: {
                                    ProjectBannerCard(
                                        project: project,
                                        isFavorite: favoriteIds.contains(project.id),
                                        onFavoriteTap: nil
                                    )
                                }
                                .buttonStyle(.plain)

                                favoriteButton(for: project)
                                    .padding(8)
                            }
                        } else {
                            HStack(spacing: 4) {
                                Button { onProjectTap(project) } label: {
                                    ProjectRow(
                                        project: project,
                                        showDescription: false,
                                        showsDisclosureIndicator: false
                                    )
                                }
                                .buttonStyle(.plain)
                                favoriteButton(for: project)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .ryntraHoverHighlight()
                    .ryntraProjectContextMenu(
                        project,
                        onOpen: { onProjectTap(project) },
                        onCreateShareCard: { projectPendingShareCard = project },
                        onRequestDelete: { Task { await requestProjectDeletion(project) } }
                    )
                }
            }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, isPlatformNative ? 20 : 96)
        }
        .background(Color.ryntraBackground)
        .refreshable { model.refresh() }
        .toolbar {
            ToolbarItem(placement: .ryntraTrailing) {
                Button { isCreatingProject = true } label: {
                    Label("Create project", systemImage: "plus")
                }
                .accessibilityHint("Creates a private Modrinth draft")
            }
        }
        .sheet(isPresented: $isCreatingProject) {
            CreateProjectView { project in
                isCreatingProject = false
                onProjectTap(project)
            }
            .environmentObject(model)
        }
        .sheet(
            isPresented: Binding(
                get: { projectPendingDeletion != nil },
                set: { if !$0 { projectPendingDeletion = nil } }
            )
        ) {
            if let projectPendingDeletion {
                ProjectDeleteSheet(project: projectPendingDeletion) {
                    self.projectPendingDeletion = nil
                }
                .environmentObject(model)
            }
        }
        .sheet(
            isPresented: Binding(
                get: { projectPendingShareCard != nil },
                set: { if !$0 { projectPendingShareCard = nil } }
            )
        ) {
            if let projectPendingShareCard {
                ProjectShareCardStudio(project: projectPendingShareCard, versions: [])
            }
        }
        .alert(
            NSLocalizedString("Can't delete project", comment: "Project permission error"),
            isPresented: Binding(
                get: { permissionError != nil },
                set: { if !$0 { permissionError = nil } }
            )
        ) {
            Button(NSLocalizedString("OK", comment: "Common action"), role: .cancel) {}
        } message: {
            Text(permissionError ?? "")
        }
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField(NSLocalizedString("Search projects", comment: "Project search"), text: $query)
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
        .frame(minHeight: 50)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var sortPicker: some View {
        Picker("Sort projects", selection: $storedSortMode) {
            ForEach(ProjectSortMode.allCases, id: \.self) { mode in
                Text(mode.label).tag(mode.rawValue)
            }
        }
        .pickerStyle(.segmented)
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    private var projectSummary: some View {
        HStack(spacing: 0) {
            summaryMetric("\(projects.count)", label: "Projects")
            summaryMetric(
                ryntraExactCount(projects.reduce(0) { $0 + $1.downloads }),
                label: "Downloads"
            )
            summaryMetric(
                ryntraExactCount(projects.reduce(0) { $0 + $1.followers }),
                label: "Followers"
            )
        }
        .padding(.vertical, 16)
        .background(
            isPlatformNative ? AnyShapeStyle(.thinMaterial) : AnyShapeStyle(Color.ryntraSurface),
            in: RoundedRectangle(cornerRadius: 16, style: .continuous)
        )
    }

    private func summaryMetric(_ value: String, label: String) -> some View {
        VStack(spacing: 1) {
            Text(value).font(.title3.weight(.semibold)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label), \(value)")
    }

    private func favoriteButton(for project: Project) -> some View {
        let isFavorite = favoriteIds.contains(project.id)
        return Button { toggleFavorite(project.id) } label: {
            Image(systemName: isFavorite ? "star.fill" : "star")
                .font(.caption.weight(.semibold))
                .foregroundStyle(isFavorite ? Color.ryntraGreen : Color.secondary)
                .ryntraMinimumTouchTarget()
                .background(.regularMaterial, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(NSLocalizedString(
            isFavorite ? "Remove favorite" : "Add favorite",
            comment: "Project favorite action"
        ))
    }

    private func toggleFavorite(_ projectId: String) {
        var updated = favoriteIds
        let update = {
            if updated.contains(projectId) {
                updated.remove(projectId)
            } else {
                updated.insert(projectId)
            }
            guard let data = try? JSONEncoder().encode(updated.sorted()),
                  let encoded = String(data: data, encoding: .utf8) else { return }
            storedFavoriteIds = encoded
        }
        if systemReduceMotion || appReduceMotion {
            update()
        } else {
            withAnimation(RyntraMotion.control, update)
        }
    }

    @MainActor
    private func requestProjectDeletion(_ project: Project) async {
        if let canDelete = deletePermissions[project.id] {
            if canDelete {
                projectPendingDeletion = project
            } else {
                permissionError = NSLocalizedString(
                    "You don't have permission to delete this project.",
                    comment: "Project permission error"
                )
            }
            return
        }
        do {
            let roster = try await model.loadProjectTeamRoster(project: project)
            let member = roster.projectMembers.first { $0.user.id == model.currentAccountID }
                ?? roster.organizationMembers.first { $0.user.id == model.currentAccountID }
            let permissions = (member?.permissions as? NSNumber)?.int32Value ?? 0
            let canDelete = member?.isOwner == true || permissions & (Int32(1) << 7) != 0
            deletePermissions[project.id] = canDelete
            if canDelete {
                projectPendingDeletion = project
            } else {
                permissionError = NSLocalizedString(
                    "You don't have permission to delete this project.",
                    comment: "Project permission error"
                )
            }
        } catch {
            permissionError = NSLocalizedString(
                "Could not verify project permissions. Check your connection and try again.",
                comment: "Project permission error"
            )
        }
    }
}

private struct CreateProjectView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let onCreated: (Project) -> Void

    @State private var metadata: ProjectCreationMetadata?
    @State private var step = 0
    @State private var title = ""
    @State private var slug = ""
    @State private var slugEdited = false
    @State private var summary = ""
    @State private var projectType = ""
    @State private var categories: Set<String> = []
    @State private var clientSide = "unknown"
    @State private var serverSide = "unknown"
    @State private var licenseID = "MIT"
    @State private var licenseURL = ""
    @State private var projectBody = ""
    @State private var sourceURL = ""
    @State private var issuesURL = ""
    @State private var wikiURL = ""
    @State private var discordURL = ""
    @State private var icon: ProjectFileUpload?
    @State private var iconName: String?
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var showingMarkdownPreview = false
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var validationAttemptedSteps: Set<Int> = []
    @State private var showingDiscardConfirmation = false
    @State private var showingCategoryPicker = false
    @State private var categoryQuery = ""
    @State private var showingLicensePicker = false
    @State private var licenseQuery = ""
    @FocusState private var focusedField: Field?

    private let environments = ["required", "optional", "unsupported", "unknown"]

    private enum Field: Hashable {
        case title, slug, summary, license, licenseURL, description, source, issues, wiki, discord
    }

    var bodyView: some View {
        NavigationStack {
            Group {
                if let metadata {
                    Form {
                        creationProgress

                        switch step {
                        case 0: basics(metadata)
                        case 1: discoverability(metadata)
                        default: content
                        }

                        if let errorMessage {
                            Section {
                                Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                                    .foregroundStyle(.red)
                                    .accessibilityLabel("Error: \(errorMessage)")
                            }
                        }
                    }
                    .formStyle(.grouped)
                    .scrollDismissesKeyboard(.interactively)
                } else if let errorMessage {
                    VStack(spacing: 12) {
                        Image(systemName: "wifi.exclamationmark").font(.largeTitle).foregroundStyle(.secondary)
                        Text("Could not load Modrinth options").font(.headline)
                        Text(errorMessage).foregroundStyle(.secondary).multilineTextAlignment(.center)
                        Button("Retry") { Task { await loadMetadata() } }
                    }
                    .padding(24)
                } else {
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("Loading Modrinth options…").foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("New project")
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        if isDirty { showingDiscardConfirmation = true } else { dismiss() }
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel("Close project creation")
                    .disabled(isSubmitting)
                }
            }
            .safeAreaInset(edge: .bottom) {
                if metadata != nil {
                    bottomActions
                }
            }
        }
        .interactiveDismissDisabled(isDirty || isSubmitting)
        .confirmationDialog(
            "Discard this draft?",
            isPresented: $showingDiscardConfirmation,
            titleVisibility: .visible
        ) {
            Button("Discard draft", role: .destructive) { dismiss() }
            Button("Keep editing", role: .cancel) {}
        } message: {
            Text("Everything entered on this screen will be lost.")
        }
        .sheet(isPresented: $showingCategoryPicker) {
            if let metadata { categoryPicker(metadata) }
        }
        .sheet(isPresented: $showingLicensePicker) {
            if let metadata {
                ProjectLicensePickerView(
                    licenses: metadata.licenses,
                    licenseID: $licenseID,
                    query: $licenseQuery,
                    onDismiss: { showingLicensePicker = false }
                )
            }
        }
        .task { await loadMetadata() }
        .onChange(of: selectedPhoto) { item in
            guard let item else { return }
            Task { await loadIcon(item) }
        }
        .onChange(of: licenseID) { _ in
            licenseURL = ""
        }
    }

    var body: some View { bodyView }

    private var creationProgress: some View {
        Section {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("Step \(step + 1) of 3")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.ryntraGreen)
                    Spacer()
                    Text(stepTitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                ProgressView(value: Double(step + 1), total: 3)
                    .tint(.ryntraGreen)
                    .accessibilityLabel("Project creation progress")
                    .accessibilityValue("Step \(step + 1) of 3")
                Text(stepHeadline)
                    .font(.title2.bold())
                    .accessibilityAddTraits(.isHeader)
                Text(stepHelp)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 4)
        }
        .listRowBackground(Color.clear)
    }

    private var bottomActions: some View {
        HStack(spacing: 12) {
            if step > 0 {
                Button {
                    focusedField = nil
                    changeStep(to: step - 1)
                } label: {
                    Label("Back", systemImage: "chevron.left")
                        .frame(minWidth: 76)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .disabled(isSubmitting)
            }
            Button {
                continueOrCreate()
            } label: {
                HStack(spacing: 8) {
                    if isSubmitting { ProgressView().tint(.white) }
                    Text(step == 2 ? "Create private draft" : "Continue")
                    if !isSubmitting { Image(systemName: step == 2 ? "paperplane.fill" : "chevron.right") }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .tint(.ryntraGreen)
            .disabled(isSubmitting)
        }
        .padding(.horizontal)
        .padding(.vertical, 12)
        .background(.bar)
        .overlay(alignment: .top) { Divider() }
    }

    private var stepTitle: String {
        switch step {
        case 0: return String(localized: "Basics")
        case 1: return String(localized: "Compatibility")
        default: return String(localized: "Project page")
        }
    }

    private var stepHeadline: String {
        switch step {
        case 0: return String(localized: "Give it an identity")
        case 1: return String(localized: "Set expectations")
        default: return String(localized: "Tell the full story")
        }
    }

    private var stepHelp: String {
        switch step {
        case 0: return String(localized: "Choose how creators will recognize and find your project.")
        case 1: return String(localized: "Explain where it works, how it is licensed, and how it should be discovered.")
        default: return String(localized: "Write the page people will read before they download.")
        }
    }

    private func changeStep(to newStep: Int) {
        if reduceMotion {
            step = newStep
        } else {
            withAnimation(.easeOut(duration: 0.2)) { step = newStep }
        }
    }

    @ViewBuilder
    private func basics(_ metadata: ProjectCreationMetadata) -> some View {
        Section {
            TextField("Project name", text: Binding(
                get: { title },
                set: { value in
                    title = String(value.prefix(65))
                    if !slugEdited { slug = value.modrinthSlug }
                }
            ))
            .focused($focusedField, equals: .title)
            .textContentType(.name)
            .submitLabel(.next)
            .onSubmit { focusedField = .slug }
            if shouldShowValidationErrors && title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                validationLabel("Enter a project name.")
            } else if (!title.isEmpty && title.trimmingCharacters(in: .whitespacesAndNewlines).count < 3) || title.count > 64 {
                validationLabel("Use 3–64 characters for the project name.")
            }

            TextField("Modrinth URL slug", text: Binding(
                get: { slug },
                set: {
                    slugEdited = true
                    slug = String($0.lowercased().replacingOccurrences(of: " ", with: "-").prefix(65))
                }
            ))
            .ryntraNoAutocapitalization()
            .autocorrectionDisabled()
            .focused($focusedField, equals: .slug)
            .submitLabel(.next)
            .onSubmit { focusedField = .summary }
            if shouldShowValidationErrors && slug.isEmpty {
                validationLabel("Add the project address. It is generated from the name automatically.")
            } else if !slug.isEmpty && !slug.isValidModrinthSlug {
                validationLabel("Use 3–64 supported letters, numbers, or URL-safe symbols.")
            }

            LabeledContent("Public URL") {
                Text("modrinth.com/\(projectType.modrinthRoute)/\(slug.isEmpty ? "…" : slug)")
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }

            TextField("Short summary", text: Binding(
                get: { summary },
                set: { summary = String($0.prefix(257)) }
            ), axis: .vertical)
                .lineLimit(1...3)
                .focused($focusedField, equals: .summary)
                .submitLabel(.done)

            if shouldShowValidationErrors && summary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                validationLabel("Add a short summary so people understand the project.")
            } else if (!summary.isEmpty && summary.trimmingCharacters(in: .whitespacesAndNewlines).count < 3) || summary.count > 256 {
                validationLabel("Use 3–256 characters for the summary.")
            }

            HStack {
                Text(summary.isEmpty ? "Explain why someone should install it." : "Shown in search and project lists.")
                Spacer()
                Text("\(summary.count)/256")
                    .monospacedDigit()
            }
            .font(.caption)
            .foregroundStyle(summary.count > 256 ? Color.red : Color.secondary)

            Picker("Project type", selection: $projectType) {
                ForEach(metadata.projectTypes.filter { $0 != "minecraft_java_server" }, id: \.self) {
                    Text($0.projectTypeDisplayName).tag($0)
                }
            }
            Text(projectTypeDescription(projectType))
                .font(.caption)
                .foregroundStyle(.secondary)
        } header: {
            Text("Identity")
        } footer: {
            Text("These details appear in Modrinth search results. The project type controls which categories are available next.")
        }

        Section {
            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                HStack(spacing: 14) {
                    Image(systemName: icon == nil ? "photo.badge.plus" : "checkmark.circle.fill")
                        .font(.title2)
                        .foregroundStyle(icon == nil ? Color.ryntraGreen : Color.ryntraGreen)
                        .frame(width: 44, height: 44)
                        .background(Color.ryntraGreen.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                    VStack(alignment: .leading, spacing: 3) {
                        Text(iconName ?? "Choose project icon")
                            .foregroundStyle(.primary)
                        Text(icon == nil ? "Square images work best" : "Ready to upload")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
            }
            if icon != nil {
                Button("Remove selected icon", role: .destructive) { icon = nil; iconName = nil; selectedPhoto = nil }
            }
        } header: {
            Text("Artwork")
        } footer: {
            Text("Optional. PNG, JPEG, WebP or GIF, up to 256 KiB. Use an image that remains recognizable at small sizes.")
        }

        Section {
            Label("Your project starts as a private draft", systemImage: "lock.shield.fill")
                .foregroundStyle(Color.ryntraGreen)
        } footer: {
            Text("Nothing is published until you add a version and submit the project for Modrinth review.")
        }
    }

    @ViewBuilder
    private func discoverability(_ metadata: ProjectCreationMetadata) -> some View {
        Section {
            let available = metadata.categories.filter { $0.projectType == projectType }
            if available.isEmpty {
                Text("No categories are available for this type.").foregroundStyle(.secondary)
            } else {
                Button { showingCategoryPicker = true } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "tag")
                            .foregroundStyle(Color.ryntraGreen)
                            .frame(width: 22)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(categories.isEmpty ? "Choose categories" : "\(categories.count) selected")
                                .foregroundStyle(.primary)
                            Text(categorySelectionSummary)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
            }
        } header: {
            HStack {
                Text("Categories")
                Spacer()
                if !categories.isEmpty { Text("\(categories.count) selected") }
            }
        } footer: {
            Text("Choose up to 3 focused categories that genuinely describe the project. They determine where it appears in Modrinth browsing and search.")
        }

        Section {
            Picker("Client support", selection: $clientSide) {
                ForEach(environments, id: \.self) { Text(environmentTitle($0)).tag($0) }
            }
            Picker("Server support", selection: $serverSide) {
                ForEach(environments, id: \.self) { Text(environmentTitle($0)).tag($0) }
            }
            environmentSummary("Client", value: clientSide, symbol: "desktopcomputer")
            environmentSummary("Server", value: serverSide, symbol: "server.rack")
        } header: {
            Text("Where does it run?")
        } footer: {
            Text("Set client and server support independently so users know exactly where they must install it.")
        }

        Section {
            ProjectLicenseSelectionButton(
                licenses: metadata.licenses,
                licenseID: licenseID,
                onOpen: { showingLicensePicker = true }
            )
            if isCustomLicense(metadata) {
                TextField("License terms URL", text: $licenseURL)
                    .ryntraURLKeyboard()
                    .ryntraNoAutocapitalization()
                    .autocorrectionDisabled()
                    .focused($focusedField, equals: .licenseURL)
                if shouldShowValidationErrors && !licenseURL.isWebURL {
                    validationLabel("Add a public URL with the complete custom license terms.")
                }
            }
        } header: {
            Text("License")
        } footer: {
            Text("Choose what other people may do with your project. These summaries are a quick guide, so read the full terms before publishing.")
        }
    }

    private var categorySelectionSummary: String {
        guard !categories.isEmpty else {
            return String(localized: "Optional. Add only categories that clearly fit.")
        }
        let names = categories.map(\.humanizedIdentifier).sorted()
        let visible = names.prefix(3).joined(separator: ", ")
        return names.count > 3
            ? String(format: String(localized: "%@ and %d more"), visible, names.count - 3)
            : visible
    }

    private func categoryPicker(_ metadata: ProjectCreationMetadata) -> some View {
        let available = metadata.categories.filter { category in
            category.projectType == projectType &&
                (categoryQuery.isEmpty || category.name.localizedCaseInsensitiveContains(categoryQuery))
        }
        let grouped = Dictionary(grouping: available, by: \.header)

        return NavigationStack {
            List {
                ForEach(grouped.keys.sorted(), id: \.self) { header in
                    Section(categoryGroupTitle(header)) {
                        ForEach(grouped[header] ?? [], id: \.name) { category in
                            Button {
                                if categories.contains(category.name) { categories.remove(category.name) }
                                else if categories.count < 3 { categories.insert(category.name) }
                            } label: {
                                HStack(alignment: .top, spacing: 12) {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(category.name.humanizedIdentifier)
                                            .foregroundStyle(.primary)
                                        Text(categoryDescription(category))
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Image(systemName: categories.contains(category.name) ? "checkmark.circle.fill" : "circle")
                                        .foregroundStyle(categories.contains(category.name) ? Color.ryntraGreen : Color.secondary)
                                }
                            }
                            .accessibilityAddTraits(categories.contains(category.name) ? .isSelected : [])
                        }
                    }
                }
            }
            .navigationTitle("Categories")
            .searchable(text: $categoryQuery, prompt: "Search categories")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showingCategoryPicker = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func categoryGroupTitle(_ header: String) -> String {
        switch header.lowercased() {
        case "features": return String(localized: "Affected features")
        case "resolutions": return String(localized: "Texture resolution")
        case "performance impact": return String(localized: "Performance impact")
        default: return String(localized: "Themes and purpose")
        }
    }

    private func categoryDescription(_ category: ProjectCategory) -> String {
        let descriptions: [String: String] = [
            "adventure": String(localized: "Exploration, progression, discoveries and new places to visit."),
            "cursed": String(localized: "Intentionally strange, chaotic or unconventional content."),
            "decoration": String(localized: "Decorative blocks, furniture and visual building details."),
            "economy": String(localized: "Currencies, shops, trading and player economies."),
            "equipment": String(localized: "Armor, tools, weapons and other usable gear."),
            "food": String(localized: "Food, farming, cooking and related survival systems."),
            "game-mechanics": String(localized: "Changes or expands the rules and systems of gameplay."),
            "library": String(localized: "A technical dependency intended mainly for other projects."),
            "magic": String(localized: "Spells, rituals, enchantments or supernatural systems."),
            "management": String(localized: "Automation or tools for managing worlds, players and resources."),
            "minigame": String(localized: "Adds a focused game mode or short repeatable activity."),
            "mobs": String(localized: "Adds or changes creatures, bosses and their behavior."),
            "optimization": String(localized: "Improves performance, memory use or loading speed."),
            "social": String(localized: "Designed for multiplayer interaction, cooperation or communities."),
            "multiplayer": String(localized: "Designed for multiplayer interaction, cooperation or communities."),
            "storage": String(localized: "Adds inventories, containers or item and fluid storage systems."),
            "technology": String(localized: "Machines, power networks, automation and technical progression."),
            "transportation": String(localized: "New ways to move players, items or entities."),
            "utility": String(localized: "Practical tools and quality-of-life improvements."),
            "worldgen": String(localized: "Changes terrain, dimensions, biomes or generated structures."),
            "challenging": String(localized: "Built around higher difficulty and demanding progression."),
            "combat": String(localized: "Focuses on fighting, weapons, enemies or combat systems."),
            "kitchen-sink": String(localized: "A broad modpack combining many systems without one narrow theme."),
            "lightweight": String(localized: "A smaller pack intended to load quickly and run on modest hardware."),
            "quests": String(localized: "Uses guided objectives, tasks and progression paths."),
            "realistic": String(localized: "Aims for realistic materials, lighting or visual detail."),
            "simplistic": String(localized: "Uses a clean, reduced and easy-to-read visual style."),
            "themed": String(localized: "Uses one consistent artistic or gameplay theme."),
            "tweaks": String(localized: "Makes focused adjustments rather than replacing the whole experience."),
            "vanilla-like": String(localized: "Preserves Minecraft’s familiar style while refining it."),
            "modded": String(localized: "Includes textures or assets for content added by mods.")
        ]
        if let description = descriptions[category.name.lowercased()] { return description }

        switch category.header.lowercased() {
        case "resolutions":
            return String(format: String(localized: "Uses %@ textures. Higher resolutions usually need more memory."), category.name)
        case "features":
            let format = category.projectType == "shader"
                ? String(localized: "Highlights the %@ visual effect.")
                : String(localized: "Changes or adds %@ assets.")
            return String(format: format, category.name.humanizedIdentifier)
        case "performance impact":
            switch category.name.lowercased() {
            case "potato": return String(localized: "Designed for very low-end hardware and maximum frame rate.")
            case "low": return String(localized: "Light performance cost, suitable for many integrated GPUs.")
            case "medium": return String(localized: "Balanced visuals and performance for mid-range hardware.")
            case "high": return String(localized: "Demanding effects intended for powerful graphics hardware.")
            case "screenshot": return String(localized: "Prioritizes maximum visual quality over playable frame rate.")
            default: return String(localized: "Use this tag when it accurately describes the project’s main content.")
            }
        default:
            return String(localized: "Use this tag when it accurately describes the project’s main content.")
        }
    }

    private func projectTypeDescription(_ type: String) -> String {
        switch type.lowercased() {
        case "mod":
            return String(localized: "A modification installed through a loader such as Fabric, Forge, NeoForge or Quilt.")
        case "modpack":
            return String(localized: "A curated collection of mods and configuration distributed as one experience.")
        case "resourcepack":
            return String(localized: "Textures, sounds, models or other assets that change Minecraft’s presentation.")
        case "shader":
            return String(localized: "A shader pack that changes lighting, shadows, atmosphere and rendering effects.")
        case "plugin":
            return String(localized: "Server-side functionality for platforms such as Paper, Spigot, Bukkit or Velocity.")
        case "datapack":
            return String(localized: "Vanilla-compatible gameplay content loaded by a Minecraft world.")
        default:
            return String(localized: "Choose the type that determines where the project appears and which metadata is available.")
        }
    }

    @ViewBuilder
    private var content: some View {
        Section {
            Picker("Description mode", selection: $showingMarkdownPreview) {
                Text("Write").tag(false)
                Text("Preview").tag(true)
            }
            .pickerStyle(.segmented)
            if showingMarkdownPreview {
                let blocks = MarkdownParser.shared.parse(markdown: projectBody)
                if blocks.isEmpty { Text("Nothing to preview yet.").foregroundStyle(.secondary) }
                ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in MarkdownBlockView(block: block) }
            } else {
                TextEditor(text: Binding(
                    get: { projectBody },
                    set: { projectBody = String($0.prefix(65_537)) }
                ))
                    .font(.body.monospaced())
                    .frame(minHeight: 240)
                    .focused($focusedField, equals: .description)
                    .accessibilityLabel("Full project description")
                    .overlay {
                        if shouldShowValidationErrors &&
                            (projectBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || projectBody.count > 65_536) {
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.red, lineWidth: 1)
                        }
                    }
            }
            if projectBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Label("Add a full description before creating the draft.", systemImage: shouldShowValidationErrors ? "exclamationmark.circle.fill" : "info.circle")
                    .font(.caption)
                    .foregroundStyle(shouldShowValidationErrors ? Color.red : Color.secondary)
            } else if projectBody.count > 65_536 {
                Label("The full description cannot exceed 65,536 characters.", systemImage: "exclamationmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        } header: {
            Text("Project page")
        } footer: {
            Text("Explain the main features, installation, requirements, and compatibility.")
        }

        Section {
            projectURLField("Source code", text: $sourceURL, field: .source)
            projectURLField("Issue tracker", text: $issuesURL, field: .issues)
            projectURLField("Documentation or wiki", text: $wikiURL, field: .wiki)
            projectURLField("Discord invite", text: $discordURL, field: .discord)
        } header: {
            Text("Helpful links")
        } footer: {
            Text("Optional. Help users find your source code, support, documentation, and community. Links must start with https:// or http://.")
        }

        Section {
            LabeledContent("Name", value: title)
            LabeledContent("Type", value: projectType.projectTypeDisplayName)
            LabeledContent("License", value: licenseID)
            LabeledContent("Categories", value: categories.isEmpty ? "None" : "\(categories.count)")
            Label("Ready to create as a private draft", systemImage: "checkmark.seal.fill")
                .foregroundStyle(canContinue ? Color.ryntraGreen : Color.secondary)
        } header: {
            Text("Review")
        } footer: {
            Text("The draft remains private until you add a version and submit it to Modrinth moderation.")
        }
    }

    private var canContinue: Bool {
        switch step {
        case 0:
            return (3...64).contains(title.trimmingCharacters(in: .whitespacesAndNewlines).count) &&
                slug.isValidModrinthSlug &&
                (3...256).contains(summary.trimmingCharacters(in: .whitespacesAndNewlines).count) &&
                !projectType.isEmpty
        case 1:
            guard !licenseID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
            return metadata.map { !isCustomLicense($0) || licenseURL.isWebURL } ?? false
        default: return !projectBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            projectBody.count <= 65_536 && linksAreValid
        }
    }

    private func isCustomLicense(_ metadata: ProjectCreationMetadata) -> Bool {
        !metadata.licenses.contains { $0.id.caseInsensitiveCompare(licenseID) == .orderedSame }
    }

    private var shouldShowValidationErrors: Bool { validationAttemptedSteps.contains(step) }

    private func validationLabel(_ message: LocalizedStringKey) -> some View {
        Label(message, systemImage: "exclamationmark.circle.fill")
            .font(.caption)
            .foregroundStyle(.red)
    }

    private func continueOrCreate() {
        focusedField = nil
        guard canContinue else {
            validationAttemptedSteps.insert(step)
            errorMessage = String(localized: "Complete the required fields highlighted below.")
            switch step {
            case 0:
                if title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || title.count > 64 {
                    focusedField = .title
                } else if !slug.isValidModrinthSlug {
                    focusedField = .slug
                } else {
                    focusedField = .summary
                }
            case 1: focusedField = metadata.map { isCustomLicense($0) } == true ? .licenseURL : .license
            default: focusedField = .description
            }
            return
        }

        errorMessage = nil
        if step < 2 {
            changeStep(to: step + 1)
        } else {
            Task { await create() }
        }
    }

    @ViewBuilder
    private func projectURLField(_ label: String, text: Binding<String>, field: Field) -> some View {
        TextField(label, text: text)
            .ryntraURLKeyboard()
            .focused($focusedField, equals: field)
            .submitLabel(field == .discord ? .done : .next)
        if !text.wrappedValue.isEmpty && !text.wrappedValue.isWebURL {
            Label("Enter a complete URL starting with https:// or http://", systemImage: "exclamationmark.circle.fill")
                .font(.caption)
                .foregroundStyle(.red)
        }
    }

    private func environmentSummary(_ title: String, value: String, symbol: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .foregroundStyle(Color.ryntraGreen)
                .frame(width: 22)
            VStack(alignment: .leading, spacing: 2) {
                Text("\(title): \(environmentTitle(value))")
                    .font(.subheadline.weight(.medium))
                Text(environmentHelp(value))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private func environmentTitle(_ value: String) -> String {
        switch value {
        case "required": return String(localized: "Required")
        case "optional": return String(localized: "Optional")
        case "unsupported": return String(localized: "Not supported")
        default: return String(localized: "Not confirmed")
        }
    }

    private func environmentHelp(_ value: String) -> String {
        switch value {
        case "required": return String(localized: "Users must install it here.")
        case "optional": return String(localized: "Installation unlocks features but is not required.")
        case "unsupported": return String(localized: "The project does not work here.")
        default: return String(localized: "Compatibility has not been confirmed yet.")
        }
    }

    private var linksAreValid: Bool {
        [sourceURL, issuesURL, wikiURL, discordURL].allSatisfy { value in
            value.isEmpty || value.isWebURL
        }
    }

    private var isDirty: Bool { !title.isEmpty || !summary.isEmpty || !projectBody.isEmpty || icon != nil }

    @MainActor private func loadMetadata() async {
        errorMessage = nil
        do {
            let loaded = try await model.loadProjectCreationMetadata()
            metadata = loaded
            if projectType.isEmpty || projectType == "minecraft_java_server" {
                projectType = loaded.projectTypes.first(where: { $0 != "minecraft_java_server" }) ?? "mod"
            }
        } catch { errorMessage = error.localizedDescription }
    }

    @MainActor private func loadIcon(_ item: PhotosPickerItem) async {
        do {
            guard let type = item.supportedContentTypes.first(where: { $0.conforms(to: .image) }),
                  let data = try await item.loadTransferable(type: Data.self), !data.isEmpty else {
                throw ProjectImageUploadError.unreadable
            }
            guard data.count <= 256 * 1024 else {
                errorMessage = "Project icon must be 256 KiB or smaller."
                selectedPhoto = nil
                return
            }
            let name = "project-icon.\(type.preferredFilenameExtension ?? "png")"
            icon = ProjectUploadFactory.shared.fromBase64(
                fileName: name,
                contentType: type.preferredMIMEType ?? "image/png",
                base64: data.base64EncodedString()
            )
            iconName = name
            errorMessage = nil
        } catch { errorMessage = error.localizedDescription }
    }

    @MainActor private func create() async {
        guard canContinue else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }
        do {
            let request = CreateProjectRequest(
                slug: slug, title: title, description: summary, body: projectBody, projectType: projectType,
                categories: Array(categories).sorted(), additionalCategories: [], clientSide: clientSide,
                serverSide: serverSide, licenseId: licenseID,
                licenseUrl: metadata.map { isCustomLicense($0) } == true ? licenseURL : nil,
                sourceUrl: sourceURL.nilIfEmpty, issuesUrl: issuesURL.nilIfEmpty,
                wikiUrl: wikiURL.nilIfEmpty, discordUrl: discordURL.nilIfEmpty, icon: icon
            )
            onCreated(try await model.createProject(request: request))
        } catch {
            let details = error.localizedDescription.lowercased()
            if details.contains("409") || details.contains("conflict") ||
                (details.contains("slug") && (details.contains("taken") || details.contains("exists"))) {
                step = 0
                focusedField = .slug
            }
            errorMessage = projectCreationErrorMessage(error)
        }
    }

    private func projectCreationErrorMessage(_ error: Error) -> String {
        let details = error.localizedDescription.lowercased()
        if details.contains("token") && (details.contains("invalid") || details.contains("expired")) {
            return String(localized: "Your Modrinth session expired. Sign in again, then retry.")
        }
        if details.contains("permission") || details.contains("forbidden") {
            return String(localized: "Your Modrinth account does not have permission to create projects.")
        }
        if details.contains("too many requests") || details.contains("429") {
            return String(localized: "Modrinth is receiving too many requests. Wait a moment and try again.")
        }
        if details.contains("409") || details.contains("conflict") ||
            (details.contains("slug") && (details.contains("taken") || details.contains("exists"))) {
            return String(localized: "That Modrinth URL is already in use. Choose another project address.")
        }
        if details.contains("initial_versions") || details.contains("parsing") || details.contains("serialization") || details.contains("json") {
            return String(localized: "The draft could not be created because Modrinth returned an unexpected response. Your entries are saved, so try again.")
        }
        if !details.isEmpty && !details.contains("modrinth could not create") {
            return String(format: String(localized: "Modrinth rejected the draft: %@"), error.localizedDescription)
        }
        return String(localized: "Modrinth could not create the project. Check the highlighted fields and try again.")
    }
}

struct ProjectLicenseSelectionButton: View {
    let licenses: [ProjectLicense]
    let licenseID: String
    let onOpen: () -> Void

    private var selectedName: String {
        licenses.first { $0.id.caseInsensitiveCompare(licenseID) == .orderedSame }?.name ?? licenseID
    }

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "doc.text")
                    .foregroundStyle(Color.ryntraGreen)
                    .frame(width: 22)
                VStack(alignment: .leading, spacing: 4) {
                    Text(selectedName)
                        .foregroundStyle(.primary)
                    Text(licenseID)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.ryntraGreen)
                    Text(projectLicenseDescription(licenseID))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                        .padding(.top, 2)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

struct ProjectLicensePickerView: View {
    let licenses: [ProjectLicense]
    @Binding var licenseID: String
    @Binding var query: String
    let onDismiss: () -> Void

    private var normalizedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var matches: [ProjectLicense] {
        licenses
            .filter { license in
                normalizedQuery.isEmpty || license.id.localizedCaseInsensitiveContains(normalizedQuery) ||
                    (license.name?.localizedCaseInsensitiveContains(normalizedQuery) ?? false)
            }
            .sorted { left, right in
                let leftSelected = left.id.caseInsensitiveCompare(licenseID) == .orderedSame
                let rightSelected = right.id.caseInsensitiveCompare(licenseID) == .orderedSame
                if leftSelected != rightSelected { return leftSelected }
                return (left.name ?? left.id).localizedCaseInsensitiveCompare(right.name ?? right.id) == .orderedAscending
            }
    }

    private var hasExactMatch: Bool {
        licenses.contains { $0.id.caseInsensitiveCompare(normalizedQuery) == .orderedSame }
    }

    private var customLicenseID: String? {
        guard !normalizedQuery.isEmpty else { return nil }
        let prefix = "LicenseRef-"
        let rawSuffix = normalizedQuery.lowercased().hasPrefix(prefix.lowercased())
            ? String(normalizedQuery.dropFirst(prefix.count))
            : normalizedQuery
        let suffix = rawSuffix
            .replacingOccurrences(of: "[^A-Za-z0-9.-]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        guard !suffix.isEmpty, !["UNKNOWN", "NOASSERTION"].contains(suffix.uppercased()) else { return nil }
        return "LicenseRef-\(suffix)"
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(matches, id: \.id) { license in
                        Button {
                            licenseID = license.id
                            onDismiss()
                        } label: {
                            HStack(alignment: .top, spacing: 12) {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(license.name ?? license.id)
                                        .foregroundStyle(.primary)
                                    Text(license.id)
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(Color.ryntraGreen)
                                    Text(projectLicenseDescription(license.id))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                if license.id.caseInsensitiveCompare(licenseID) == .orderedSame {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.ryntraGreen)
                                }
                            }
                        }
                        .accessibilityAddTraits(license.id.caseInsensitiveCompare(licenseID) == .orderedSame ? .isSelected : [])
                    }
                } footer: {
                    Text("License summaries are not legal advice. Review the complete terms before publishing.")
                }

                if let customLicenseID, !hasExactMatch {
                    Section {
                        Button("Use “\(customLicenseID)”") {
                            licenseID = customLicenseID
                            onDismiss()
                        }
                    } header: {
                        Text("Custom identifier")
                    } footer: {
                        Text("Use a custom identifier only when you know Modrinth accepts it.")
                    }
                }
            }
            .navigationTitle("Choose a license")
            .searchable(text: $query, prompt: "Search by name or SPDX ID")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

func projectLicenseDescription(_ id: String) -> String {
    let normalized = id.uppercased()
    switch normalized {
    case "MIT": return String(localized: "Permissive: reuse, modification and commercial use are allowed when the copyright and license notice are kept.")
    case let value where value.hasPrefix("APACHE-"):
        return String(localized: "Permissive, with an explicit patent grant. Keep the license, notices and mark significant changes.")
    case let value where value.hasPrefix("AGPL-"):
        return String(localized: "Strong copyleft that also requires source availability when modified software is offered over a network.")
    case let value where value.hasPrefix("LGPL-"):
        return String(localized: "Weak copyleft: linking is allowed, while changes to the covered component must remain available under the LGPL.")
    case let value where value.hasPrefix("GPL-"):
        return String(localized: "Strong copyleft: distributed derivatives must remain under the GPL and provide their source code.")
    case let value where value.hasPrefix("MPL-"):
        return String(localized: "File-level copyleft: modified covered files stay open, while separate files may use another license.")
    case let value where value.hasPrefix("BSD-") || value == "0BSD":
        return String(localized: "Permissive: reuse and commercial use are generally allowed when the copyright notice and conditions are kept.")
    case "CC0-1.0":
        return String(localized: "A public-domain dedication intended to waive copyright restrictions as far as legally possible.")
    case let value where value.hasPrefix("CC-BY"):
        return String(localized: "Reuse and modification are allowed with attribution. Some variants add share-alike or non-commercial restrictions.")
    case "ARR", "ALL-RIGHTS-RESERVED":
        return String(localized: "All rights reserved: others receive no permission to reuse or modify the project without your consent.")
    default:
        return String(localized: "Terms vary. Check permissions for modification, redistribution, source code and commercial use.")
    }
}

private extension String {
    var modrinthSlug: String {
        lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "[^a-z0-9_-]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))
            .prefix(64).description
    }

    var nilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    var isWebURL: Bool {
        guard let components = URLComponents(string: self),
              let scheme = components.scheme?.lowercased(),
              scheme == "https" || scheme == "http",
              let host = components.host,
              host.contains(".") else { return false }
        return true
    }

    var isValidModrinthSlug: Bool {
        range(of: "^[A-Za-z0-9_!@$()`.+,\\\"'\\-]{3,64}$", options: .regularExpression) != nil
    }

    var humanizedIdentifier: String {
        replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .capitalized
    }

    var projectTypeDisplayName: String {
        switch self {
        case "mod": return String(localized: "Mod")
        case "modpack": return String(localized: "Modpack")
        case "resourcepack": return String(localized: "Resource pack")
        case "shader": return String(localized: "Shader")
        case "plugin": return String(localized: "Plugin")
        case "datapack": return String(localized: "Data pack")
        default: return String(localized: "Other")
        }
    }

    var modrinthRoute: String {
        switch self {
        case "mod": return "mod"
        case "plugin": return "plugin"
        case "modpack": return "modpack"
        case "resourcepack": return "resourcepack"
        case "shader": return "shader"
        case "datapack": return "datapack"
        case "server", "minecraft_java_server": return "server"
        default: return "project"
        }
    }

}

struct ProjectRow: View {
    let project: Project
    var showDescription = true
    var showStatus = true
    var isFavorite = false
    var onFavoriteTap: (() -> Void)?
    var showsDisclosureIndicator = true

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            ProjectArtwork(project: project)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(project.title).fontWeight(.semibold).lineLimit(1)
                    Spacer(minLength: 0)
                    if showStatus, project.status != "approved" {
                        statusLabel
                    }
                }
                Text(project.slug.map { "\($0)  ·  \(project.displayTypeLabel)" }
                     ?? project.displayTypeLabel)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                if showDescription, !project.description_.isEmpty {
                    Text(project.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .padding(.top, 1)
                }
                HStack(spacing: 13) {
                    metric(ryntraExactCount(project.downloads), symbol: "arrow.down", tint: Color.ryntraGreen)
                    metric(ryntraExactCount(project.followers), symbol: "heart", tint: Color.ryntraGreen)
                    if let updated = ryntraProjectDate(project.updated) {
                        Text(updated).font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .padding(.top, 2)
            }

            if let onFavoriteTap {
                Button(action: onFavoriteTap) {
                    Image(systemName: isFavorite ? "star.fill" : "star")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(isFavorite ? Color.ryntraGreen : Color.secondary)
                        .frame(width: 36, height: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isFavorite ? "Remove favorite" : "Add favorite")
            } else if showsDisclosureIndicator {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 6)
    }

    private var statusLabel: some View {
        return Text(project.localizedStatusLabel)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(statusColor)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(statusColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
    }

    private var statusColor: Color {
        switch project.status {
        case "rejected", "withheld": return .red
        case "processing", "scheduled", "draft": return .orange
        default: return .secondary
        }
    }

    private func metric(_ value: String, symbol: String, tint: Color) -> some View {
        HStack(spacing: 4) {
            Image(systemName: symbol).foregroundStyle(tint)
            Text(value).foregroundStyle(.secondary)
        }
        .font(.caption2)
    }
}

private enum ProjectSortMode: String, CaseIterable {
    case popular
    case updated
    case title
    case followers

    var label: String {
        switch self {
        case .popular: return NSLocalizedString("Popular", comment: "Project sort")
        case .updated: return NSLocalizedString("Updated", comment: "Project sort")
        case .title: return NSLocalizedString("A-Z", comment: "Project sort")
        case .followers: return NSLocalizedString("Followers", comment: "Project sort")
        }
    }
}

private extension Array where Element == Project {
    func sortedForDisplay(mode: ProjectSortMode, favoriteIds: Set<String>) -> [Project] {
        let sorted: [Project]
        switch mode {
        case .popular:
            sorted = self.sorted { $0.downloads > $1.downloads }
        case .updated:
            sorted = self.sorted { ($0.updated ?? "") > ($1.updated ?? "") }
        case .title:
            sorted = self.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        case .followers:
            sorted = self.sorted { $0.followers > $1.followers }
        }
        return sorted.sorted { favoriteIds.contains($0.id) && !favoriteIds.contains($1.id) }
    }
}

func ryntraProjectDate(_ value: String?) -> String? {
    guard let value, let date = projectISODateFormatter.date(from: value) else { return nil }
    return date.formatted(.dateTime.month(.abbreviated).day())
}

private let projectISODateFormatter = ISO8601DateFormatter()

struct EmptyStateView: View {
    let title: String
    let systemImage: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(title).font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}

struct ProjectArtwork: View {
    let project: Project

    var body: some View {
        RemoteImage(url: URL(string: project.iconUrl ?? "")) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            RoundedRectangle(cornerRadius: 10)
                .fill(.quaternary)
                .overlay(Text(String(project.title.prefix(1))).fontWeight(.bold))
        }
        .frame(width: 56, height: 56)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityHidden(true)
    }
}
