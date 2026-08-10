import PhotosUI
import RyntraShared
import SwiftUI

struct EditProjectView: View {
    @EnvironmentObject private var model: AppModel
    let project: Project
    let canEditDetails: Bool
    let canEditBody: Bool
    let saveRequest: Int
    let onSaved: () async -> Void
    let onEditingStateChanged: (Bool, Bool) -> Void

    @State private var title: String
    @State private var summary: String
    @State private var bodyText: String
    @State private var sourceUrl: String
    @State private var issuesUrl: String
    @State private var wikiUrl: String
    @State private var discordUrl: String
    @State private var status: String
    @State private var licenseId: String
    @State private var licenseURL: String
    @State private var iconItem: PhotosPickerItem?
    @State private var localError: String?
    @State private var isEditingDescription = false
    @State private var expandedSections: Set<EditProjectSection> = [.main]
    @State private var licenses: [ProjectLicense] = []
    @State private var isLoadingLicenses = false
    @State private var licenseLoadError: String?
    @State private var isShowingLicensePicker = false
    @State private var licenseQuery = ""
    @State private var isTitleTouched = false
    @State private var isSummaryTouched = false

    init(
        project: Project,
        canEditDetails: Bool,
        canEditBody: Bool,
        saveRequest: Int,
        onSaved: @escaping () async -> Void,
        onEditingStateChanged: @escaping (Bool, Bool) -> Void
    ) {
        self.project = project
        self.canEditDetails = canEditDetails
        self.canEditBody = canEditBody
        self.saveRequest = saveRequest
        self.onSaved = onSaved
        self.onEditingStateChanged = onEditingStateChanged
        _title = State(initialValue: project.title)
        _summary = State(initialValue: project.description_)
        _bodyText = State(initialValue: project.body)
        _sourceUrl = State(initialValue: project.sourceUrl ?? "")
        _issuesUrl = State(initialValue: project.issuesUrl ?? "")
        _wikiUrl = State(initialValue: project.wikiUrl ?? "")
        _discordUrl = State(initialValue: project.discordUrl ?? "")
        _status = State(initialValue: Self.usesDirectStatus(project.status)
            ? project.status
            : (project.requestedStatus ?? "draft"))
        _licenseId = State(initialValue: project.license?.id ?? "")
        _licenseURL = State(initialValue: project.license?.url ?? "")
    }

    private var hasChanges: Bool {
        title != project.title || summary != project.description_ || bodyText != project.body ||
        sourceUrl != (project.sourceUrl ?? "") || issuesUrl != (project.issuesUrl ?? "") ||
        wikiUrl != (project.wikiUrl ?? "") || discordUrl != (project.discordUrl ?? "") ||
        status != baselineStatus || licenseId != (project.license?.id ?? "") ||
            licenseURL != (project.license?.url ?? "")
    }

    private var baselineStatus: String {
        Self.usesDirectStatus(project.status) ? project.status : (project.requestedStatus ?? "draft")
    }

    private var usesDirectStatus: Bool { Self.usesDirectStatus(project.status) }

    private var statusOptions: [String] {
        let choices = ["approved", "archived", "unlisted", "private", "draft"]
        return Array(Set(choices + [status])).sorted()
    }

    private static func usesDirectStatus(_ status: String) -> Bool {
        ["approved", "archived", "unlisted", "private"].contains(status.lowercased())
    }

    private var canSave: Bool {
        hasChanges && !model.isProjectSaving && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !summary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            (!isCustomLicense || isWebURL(licenseURL))
    }

    private var isCustomLicense: Bool {
        !licenses.isEmpty && !licenses.contains { $0.id.caseInsensitiveCompare(licenseId) == .orderedSame }
    }

    private func isWebURL(_ value: String) -> Bool {
        guard let components = URLComponents(string: value),
              let scheme = components.scheme?.lowercased(),
              scheme == "https" || scheme == "http",
              let host = components.host,
              host.contains(".") else { return false }
        return true
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            editSection(.main, title: "Main information", summary: title, systemImage: "pencil", isEnabled: canEditDetails) {
                iconEditor
                field("Title", text: $title, systemImage: "tag")
                    .onChange(of: title) { _ in isTitleTouched = true }
                if isTitleTouched && title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    requiredFieldMessage
                }
                field("Summary", text: $summary, systemImage: "info.circle")
                    .onChange(of: summary) { _ in isSummaryTouched = true }
                if isSummaryTouched && summary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    requiredFieldMessage
                }
            }

            editSection(
                .description,
                title: "Full description",
                summary: bodyText.isEmpty ? NSLocalizedString("Not added", comment: "Collapsed editor summary") : NSLocalizedString("Added", comment: "Collapsed editor summary"),
                systemImage: "doc.text",
                isEnabled: canEditBody
            ) {
                Text(bodyText.isEmpty
                    ? NSLocalizedString("No full description yet", comment: "Project description editor")
                    : bodyText)
                    .font(.subheadline)
                    .foregroundStyle(bodyText.isEmpty ? Color.secondary : Color.primary)
                    .lineLimit(4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    isEditingDescription = true
                } label: {
                    Label(NSLocalizedString("Edit description", comment: "Project editor action"), systemImage: "square.and.pencil")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            editSection(
                .gallery,
                title: "Gallery",
                summary: String.localizedStringWithFormat(NSLocalizedString("Gallery: %d", comment: "Collapsed editor summary"), project.gallery.count),
                systemImage: "photo.on.rectangle.angled",
                isEnabled: canEditDetails
            ) {
                ProjectGalleryEditor(project: project, onSaved: onSaved)
            }

            editSection(.publishing, title: "Status and license", summary: licenseId, systemImage: "checkmark.seal", isEnabled: canEditDetails) {
                Picker("Status", selection: $status) {
                    ForEach(statusOptions, id: \.self) {
                        Text(ProjectStatusSupport.statusLabel($0)).tag($0)
                    }
                }
                .pickerStyle(.menu)
                .disabled(project.status.lowercased() == "processing")
                if !licenses.isEmpty {
                    ProjectLicenseSelectionButton(
                        licenses: licenses,
                        licenseID: licenseId,
                        onOpen: { isShowingLicensePicker = true }
                    )
                    Text("Choose what other people may do with your project. These summaries are a quick guide, so read the full terms before publishing.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else if isLoadingLicenses {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text("Loading Modrinth options…")
                            .foregroundStyle(.secondary)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(licenseLoadError ?? String(localized: "Could not load Modrinth options"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button("Retry") { Task { await loadLicenses() } }
                            .buttonStyle(.bordered)
                    }
                }
                if isCustomLicense {
                    field("License terms URL", text: $licenseURL, systemImage: "link", isURL: true)
                    Text(NSLocalizedString(
                        "A custom license needs a public URL with its complete terms.",
                        comment: "Custom license guidance"
                    ))
                    .font(.caption)
                    .foregroundStyle(licenseURL.isEmpty || isWebURL(licenseURL) ? Color.secondary : Color.red)
                }
            }

            editSection(
                .links,
                title: "Links",
                summary: [sourceUrl, issuesUrl, wikiUrl, discordUrl].filter { !$0.isEmpty }.isEmpty
                    ? NSLocalizedString("Not added", comment: "Collapsed editor summary")
                    : NSLocalizedString("Added", comment: "Collapsed editor summary"),
                systemImage: "link",
                isEnabled: canEditDetails
            ) {
                field("Source code", text: $sourceUrl, systemImage: "chevron.left.forwardslash.chevron.right", isURL: true)
                field("Issue tracker", text: $issuesUrl, systemImage: "ant", isURL: true)
                field("Wiki", text: $wikiUrl, systemImage: "book", isURL: true)
                field("Discord", text: $discordUrl, systemImage: "bubble.left.and.bubble.right", isURL: true)
            }

            if let error = localError ?? model.projectUpdateError ?? model.projectActionError {
                Text(error).font(.caption).foregroundStyle(.red)
            }

        }
        .padding(.bottom, 24)
        .onAppear { reportEditingState() }
        .onChange(of: hasChanges) { _ in reportEditingState() }
        .onChange(of: canSave) { _ in reportEditingState() }
        .onChange(of: saveRequest) { _ in
            guard canSave else { return }
            Task { await save() }
        }
        .onChange(of: iconItem) { item in
            guard let item else { return }
            Task { await uploadIcon(item) }
        }
        .sheet(isPresented: $isEditingDescription) {
            MarkdownProjectEditor(text: $bodyText)
        }
        .sheet(isPresented: $isShowingLicensePicker) {
            ProjectLicensePickerView(
                licenses: licenses,
                licenseID: $licenseId,
                query: $licenseQuery,
                onDismiss: { isShowingLicensePicker = false }
            )
        }
        .onChange(of: licenseId) { selected in
            if selected.caseInsensitiveCompare(project.license?.id ?? "") != .orderedSame {
                licenseURL = ""
            }
        }
        .task(id: project.id) { await loadLicenses() }
    }

    @MainActor
    private func save() async {
        let update = ProjectUpdate(
            title: title == project.title ? nil : title,
            description: summary == project.description_ ? nil : summary,
            body: bodyText == project.body ? nil : bodyText,
            sourceUrl: sourceUrl == (project.sourceUrl ?? "") ? nil : sourceUrl,
            issuesUrl: issuesUrl == (project.issuesUrl ?? "") ? nil : issuesUrl,
            wikiUrl: wikiUrl == (project.wikiUrl ?? "") ? nil : wikiUrl,
            discordUrl: discordUrl == (project.discordUrl ?? "") ? nil : discordUrl,
            status: usesDirectStatus && status != baselineStatus ? status : nil,
            requestedStatus: !usesDirectStatus && status != baselineStatus ? status : nil,
            licenseId: licenseId == (project.license?.id ?? "") ? nil : licenseId,
            licenseUrl: isCustomLicense && licenseURL != (project.license?.url ?? "") ? licenseURL : nil
        )
        await model.updateProject(projectId: project.id, update: update)
        if model.projectUpdateError == nil {
            onEditingStateChanged(false, false)
            await onSaved()
        }
    }

    @MainActor
    private func uploadIcon(_ item: PhotosPickerItem) async {
        defer { iconItem = nil }
        do {
            let upload = try await ProjectImageUploadReader.load(
                item,
                baseName: "project-icon",
                maxBytes: 256 * 1024
            )
            try await model.changeProjectIcon(project: project, file: upload)
            await onSaved()
        } catch { localError = error.localizedDescription }
    }

    @MainActor
    private func deleteIcon() async {
        do {
            try await model.deleteProjectIcon(project: project)
            await onSaved()
        } catch { localError = error.localizedDescription }
    }

    private var iconEditor: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) {
                projectIcon
                iconActions
            }
            VStack(alignment: .leading, spacing: 12) {
                projectIcon
                iconActions
            }
        }
    }

    private var projectIcon: some View {
        RemoteImage(url: URL(string: project.iconUrl ?? "")) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            RoundedRectangle(cornerRadius: 10).fill(.quaternary)
                .overlay { Image(systemName: "photo") }
        }
        .frame(width: 64, height: 64)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var iconActions: some View {
        VStack(spacing: 8) {
            PhotosPicker(selection: $iconItem, matching: .images) {
                Label("Upload icon", systemImage: "square.and.arrow.up")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            if project.iconUrl != nil {
                Button(role: .destructive) {
                    Task { await deleteIcon() }
                } label: {
                    Label("Remove icon", systemImage: "trash").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    @ViewBuilder
    private func field(_ title: String, text: Binding<String>, systemImage: String, isURL: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Label(NSLocalizedString(title, comment: "Project field"), systemImage: systemImage)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
            if isURL {
                TextField(NSLocalizedString(title, comment: "Project field"), text: text)
                    .textFieldStyle(.roundedBorder)
                    .ryntraURLKeyboard()
                    .ryntraNoAutocapitalization()
                    .autocorrectionDisabled()
                    .frame(minHeight: 44)
            } else {
                TextField(NSLocalizedString(title, comment: "Project field"), text: text)
                    .textFieldStyle(.roundedBorder)
                    .frame(minHeight: 44)
            }
        }
    }

    private var requiredFieldMessage: some View {
        Label(
            NSLocalizedString("Required field", comment: "Project editor validation"),
            systemImage: "exclamationmark.circle.fill"
        )
        .font(.caption)
        .foregroundStyle(.red)
    }

    private func reportEditingState() {
        onEditingStateChanged(hasChanges, canSave)
    }

    @MainActor
    private func loadLicenses() async {
        isLoadingLicenses = true
        licenseLoadError = nil
        do {
            let metadata = try await model.loadProjectCreationMetadata()
            licenses = metadata.licenses
        } catch {
            licenseLoadError = error.localizedDescription
        }
        isLoadingLicenses = false
    }

    private func editSection<Content: View>(
        _ section: EditProjectSection,
        title: String,
        summary: String,
        systemImage: String,
        isEnabled: Bool,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        DisclosureGroup(
            isExpanded: Binding(
                get: { expandedSections.contains(section) },
                set: { isExpanded in
                    if isExpanded { expandedSections.insert(section) }
                    else { expandedSections.remove(section) }
                }
            )
        ) {
            VStack(alignment: .leading, spacing: 14) {
                if !isEnabled {
                    Label(
                        NSLocalizedString("You don’t have permission to edit this section.", comment: "Project edit permission"),
                        systemImage: "lock"
                    )
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
                content()
            }
            .disabled(!isEnabled)
            .padding(.top, 14)
        } label: {
            HStack(spacing: 10) {
                Label(NSLocalizedString(title, comment: "Project editor section"), systemImage: systemImage)
                    .font(.headline)
                Spacer(minLength: 8)
                if !expandedSections.contains(section) {
                    Text(summary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 10)
        .overlay(alignment: .bottom) { Divider() }
    }
}

private enum EditProjectSection: Hashable {
    case main
    case description
    case gallery
    case publishing
    case links
}
