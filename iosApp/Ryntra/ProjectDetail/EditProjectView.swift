import PhotosUI
import RyntraShared
import SwiftUI

struct EditProjectView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("reduceMotion") private var appReduceMotion = false
    let project: Project
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
    @State private var iconItem: PhotosPickerItem?
    @State private var localError: String?
    @State private var isEditingDescription = false
    @State private var expandedSections: Set<EditProjectSection> = [.main]
    @State private var licenses: [ProjectLicense] = []
    @State private var isLoadingLicenses = false
    @State private var licenseLoadError: String?
    @State private var isShowingLicensePicker = false
    @State private var licenseQuery = ""

    init(
        project: Project,
        saveRequest: Int,
        onSaved: @escaping () async -> Void,
        onEditingStateChanged: @escaping (Bool, Bool) -> Void
    ) {
        self.project = project
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
        _status = State(initialValue: project.status)
        _licenseId = State(initialValue: project.license?.id ?? "")
    }

    private var hasChanges: Bool {
        title != project.title || summary != project.description_ || bodyText != project.body ||
        sourceUrl != (project.sourceUrl ?? "") || issuesUrl != (project.issuesUrl ?? "") ||
        wikiUrl != (project.wikiUrl ?? "") || discordUrl != (project.discordUrl ?? "") ||
        status != project.status || licenseId != (project.license?.id ?? "")
    }

    private var canSave: Bool {
        hasChanges && !model.isProjectSaving && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !summary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            editSection(.main, title: "Main information", systemImage: "pencil") {
                iconEditor
                field("Title", text: $title, systemImage: "tag")
                field("Summary", text: $summary, systemImage: "info.circle")
            }

            editSection(.description, title: "Full description", systemImage: "doc.text") {
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

            editSection(.gallery, title: "Gallery", systemImage: "photo.on.rectangle.angled") {
                ProjectGalleryEditor(project: project, onSaved: onSaved)
            }

            editSection(.publishing, title: "Status and license", systemImage: "checkmark.seal") {
                Picker("Status", selection: $status) {
                    ForEach(Array(Set([project.status, "draft", "unlisted", "archived"])).sorted(), id: \.self) {
                        Text(ProjectStatusSupport.statusLabel($0)).tag($0)
                    }
                }
                .pickerStyle(.segmented)
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
            }

            editSection(.links, title: "Links", systemImage: "link") {
                field("Source code", text: $sourceUrl, systemImage: "chevron.left.forwardslash.chevron.right")
                field("Issue tracker", text: $issuesUrl, systemImage: "ant")
                field("Wiki", text: $wikiUrl, systemImage: "book")
                field("Discord", text: $discordUrl, systemImage: "bubble.left.and.bubble.right")
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
            status: status == project.status ? nil : status,
            requestedStatus: nil,
            licenseId: licenseId == (project.license?.id ?? "") ? nil : licenseId
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
        HStack(spacing: 12) {
            RemoteImage(url: URL(string: project.iconUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 10).fill(.quaternary)
                    .overlay { Image(systemName: "photo") }
            }
            .frame(width: 64, height: 64)
            .clipShape(RoundedRectangle(cornerRadius: 10))
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
    }

    private func label(_ text: String) -> some View {
        Text(LocalizedStringKey(text)).font(.caption.weight(.medium)).foregroundStyle(.secondary)
    }

    private func field(_ title: String, text: Binding<String>, systemImage: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            label(title)
            HStack(spacing: 12) {
                Image(systemName: systemImage).foregroundStyle(.secondary).frame(width: 20)
                TextField(NSLocalizedString(title, comment: "Project field"), text: text)
            }
            .padding(.horizontal, 12)
            .frame(height: 46)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
        }
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
        systemImage: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        let isExpanded = expandedSections.contains(section)
        return VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(
                    RyntraMotion.resolved(
                        .easeOut(duration: 0.16),
                        reduceMotion: systemReduceMotion || appReduceMotion
                    )
                ) {
                    if isExpanded { expandedSections.remove(section) }
                    else { expandedSections.insert(section) }
                }
            } label: {
                HStack(spacing: 12) {
                    Label(NSLocalizedString(title, comment: "Project editor section"), systemImage: systemImage)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Spacer(minLength: 8)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(alignment: .leading, spacing: 14) {
                    content()
                }
                .padding(.top, 14)
                .transition(.opacity)
            }
        }
        .padding(16)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
    }
}

private enum EditProjectSection: Hashable {
    case main
    case description
    case gallery
    case publishing
    case links
}

private struct MarkdownProjectEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var text: String

    var body: some View {
        NavigationStack {
            TextEditor(text: $text)
                .font(.body)
                .padding(.horizontal, 12)
                .scrollContentBackground(.hidden)
                .background(Color.ryntraBackground)
                .navigationTitle(NSLocalizedString("Full description", comment: "Project editor title"))
                .ryntraInlineNavigationTitle()
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(NSLocalizedString("Done", comment: "Finish editing")) { dismiss() }
                    }
                }
        }
    }
}
