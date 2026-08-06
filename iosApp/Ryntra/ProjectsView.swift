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
                        if !showProjectBanners {
                            ProjectRow(
                                project: project,
                                showDescription: false,
                                onFavoriteTap: { toggleFavorite(project.id) }
                            )
                        } else {
                            ProjectBannerCard(
                                project: project,
                                isFavorite: favoriteIds.contains(project.id),
                                onFavoriteTap: { toggleFavorite(project.id) }
                            )
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                    .onTapGesture { onProjectTap(project) }
                    .ryntraHoverHighlight()
                    .ryntraProjectContextMenu(project)
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
}

private struct CreateProjectView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

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

    private let environments = ["required", "optional", "unsupported", "unknown"]

    var bodyView: some View {
        NavigationStack {
            Group {
                if let metadata {
                    Form {
                        Section {
                            Label(
                                "Your project is created as a private draft. Add a release before submitting it for review.",
                                systemImage: "lock.shield"
                            )
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        }

                        switch step {
                        case 0: basics(metadata)
                        case 1: discoverability(metadata)
                        default: content
                        }

                        if let errorMessage {
                            Section { Text(errorMessage).foregroundStyle(.red) }
                        }
                    }
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
            .navigationTitle("Create project")
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) { dismiss() }.disabled(isSubmitting)
                }
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 1) {
                        Text("Create project").font(.headline)
                        Text("\(step + 1) of 3").font(.caption2).foregroundStyle(.secondary)
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                if metadata != nil {
                    HStack(spacing: 12) {
                        if step > 0 {
                            Button("Back") { step -= 1 }
                                .buttonStyle(.bordered)
                                .disabled(isSubmitting)
                        }
                        Button(step == 2 ? "Create draft" : "Continue") {
                            if step < 2 { step += 1 } else { Task { await create() } }
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.ryntraGreen)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .disabled(!canContinue || isSubmitting)
                        .overlay { if isSubmitting { ProgressView().tint(.white) } }
                    }
                    .padding()
                    .background(.bar)
                }
            }
        }
        .interactiveDismissDisabled(isDirty || isSubmitting)
        .task { await loadMetadata() }
        .onChange(of: selectedPhoto) { item in
            guard let item else { return }
            Task { await loadIcon(item) }
        }
    }

    var body: some View { bodyView }

    @ViewBuilder
    private func basics(_ metadata: ProjectCreationMetadata) -> some View {
        Section("Basics") {
            TextField("Project name", text: Binding(
                get: { title },
                set: { value in title = value; if !slugEdited { slug = value.modrinthSlug } }
            ))
            TextField("Modrinth URL slug", text: Binding(
                get: { slug },
                set: { slugEdited = true; slug = $0.lowercased().replacingOccurrences(of: " ", with: "-") }
            ))
            .ryntraNoAutocapitalization()
            .autocorrectionDisabled()
            TextField("Short summary", text: $summary, axis: .vertical).lineLimit(2...4)
            Picker("Project type", selection: $projectType) {
                ForEach(metadata.projectTypes, id: \.self) { Text($0.capitalized).tag($0) }
            }
        }
        Section("Artwork") {
            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                Label(iconName ?? "Choose project icon", systemImage: icon == nil ? "photo.badge.plus" : "checkmark.circle.fill")
            }
            if icon != nil {
                Button("Remove selected icon", role: .destructive) { icon = nil; iconName = nil; selectedPhoto = nil }
            }
            Text("PNG, JPEG, WebP or GIF · up to 256 KiB").font(.caption).foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    private func discoverability(_ metadata: ProjectCreationMetadata) -> some View {
        Section("Categories") {
            let available = metadata.categories.filter { $0.projectType == projectType }
            if available.isEmpty {
                Text("No categories are available for this type.").foregroundStyle(.secondary)
            } else {
                ForEach(available, id: \.name) { category in
                    Toggle(category.name.capitalized, isOn: Binding(
                        get: { categories.contains(category.name) },
                        set: { enabled in
                            if enabled { categories.insert(category.name) } else { categories.remove(category.name) }
                        }
                    ))
                }
            }
        }
        Section("Environment") {
            Picker("Client support", selection: $clientSide) {
                ForEach(environments, id: \.self) { Text($0.capitalized).tag($0) }
            }
            Picker("Server support", selection: $serverSide) {
                ForEach(environments, id: \.self) { Text($0.capitalized).tag($0) }
            }
        }
        Section("License") {
            Picker("SPDX license", selection: $licenseID) {
                ForEach(metadata.licenses, id: \.id) { license in
                    Text(license.name ?? license.id).tag(license.id)
                }
            }
            TextField("Custom SPDX ID", text: $licenseID)
                .ryntraNoAutocapitalization()
                .autocorrectionDisabled()
        }
    }

    @ViewBuilder
    private var content: some View {
        Section("Full description · GitHub Flavored Markdown") {
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
                TextEditor(text: $projectBody)
                    .font(.body.monospaced())
                    .frame(minHeight: 240)
                    .accessibilityLabel("Full project description")
            }
        }
        Section("Links · optional") {
            TextField("Source URL", text: $sourceURL).ryntraURLKeyboard()
            TextField("Issues URL", text: $issuesURL).ryntraURLKeyboard()
            TextField("Wiki URL", text: $wikiURL).ryntraURLKeyboard()
            TextField("Discord URL", text: $discordURL).ryntraURLKeyboard()
        }
        Section {
            Label("The draft stays private until you add a version and submit it to Modrinth moderation.", systemImage: "eye.slash")
                .font(.subheadline).foregroundStyle(.secondary)
        }
    }

    private var canContinue: Bool {
        switch step {
        case 0: return !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && (3...64).contains(slug.count) && !summary.isEmpty && !projectType.isEmpty
        case 1: return !licenseID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        default: return !projectBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && linksAreValid
        }
    }

    private var linksAreValid: Bool {
        [sourceURL, issuesURL, wikiURL, discordURL].allSatisfy { value in
            value.isEmpty || value.hasPrefix("https://") || value.hasPrefix("http://")
        }
    }

    private var isDirty: Bool { !title.isEmpty || !summary.isEmpty || !projectBody.isEmpty || icon != nil }

    @MainActor private func loadMetadata() async {
        errorMessage = nil
        do {
            let loaded = try await model.loadProjectCreationMetadata()
            metadata = loaded
            if projectType.isEmpty { projectType = loaded.projectTypes.first ?? "mod" }
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
                serverSide: serverSide, licenseId: licenseID, licenseUrl: nil,
                sourceUrl: sourceURL.nilIfEmpty, issuesUrl: issuesURL.nilIfEmpty,
                wikiUrl: wikiURL.nilIfEmpty, discordUrl: discordURL.nilIfEmpty, icon: icon
            )
            onCreated(try await model.createProject(request: request))
        } catch { errorMessage = error.localizedDescription }
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
