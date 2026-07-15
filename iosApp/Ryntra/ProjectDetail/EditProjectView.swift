import PhotosUI
import RyntraShared
import SwiftUI

struct EditProjectView: View {
    @EnvironmentObject private var model: AppModel
    let project: Project
    let onSaved: () async -> Void

    @State private var title: String
    @State private var summary: String
    @State private var bodyText: String
    @State private var sourceUrl: String
    @State private var issuesUrl: String
    @State private var wikiUrl: String
    @State private var discordUrl: String
    @State private var status: String
    @State private var licenseId: String
    @State private var descriptionMode = 0
    @State private var descriptionPreview: [MarkdownBlock] = []
    @State private var iconItem: PhotosPickerItem?
    @State private var localError: String?

    init(project: Project, onSaved: @escaping () async -> Void) {
        self.project = project
        self.onSaved = onSaved
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

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            heading("Icon")
            HStack(spacing: 12) {
                AsyncImage(url: URL(string: project.iconUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 8).fill(.quaternary)
                        .overlay { Image(systemName: "photo") }
                }
                .frame(width: 72, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: 8))
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

            heading("Main information")
            field("Title", text: $title, systemImage: "tag")
            field("Summary", text: $summary, systemImage: "info.circle")
            VStack(alignment: .leading, spacing: 8) {
                label("Description (Markdown)")
                Picker("Description mode", selection: $descriptionMode) {
                    Text("Write").tag(0)
                    Text("Preview").tag(1)
                }
                .pickerStyle(.segmented)
                if descriptionMode == 0 {
                    TextEditor(text: $bodyText)
                        .frame(minHeight: 190)
                        .padding(8)
                        .scrollContentBackground(.hidden)
                        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
                } else {
                    VStack(alignment: .leading, spacing: 8) {
                        if descriptionPreview.isEmpty {
                            Text("Nothing to preview").font(.caption).foregroundStyle(.secondary)
                        }
                        ForEach(Array(descriptionPreview.enumerated()), id: \.offset) { _, block in
                            MarkdownBlockView(block: block)
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 190, alignment: .topLeading)
                    .padding(12)
                    .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
                }
            }
            .onChange(of: descriptionMode) { mode in
                if mode == 1 {
                    descriptionPreview = MarkdownParser.shared.parse(markdown: bodyText)
                }
            }

            heading("Gallery")
            ProjectGalleryEditor(project: project, onSaved: onSaved)

            heading("Status and license")
            Picker("Status", selection: $status) {
                ForEach(Array(Set([project.status, "draft", "unlisted", "archived"])).sorted(), id: \.self) {
                    Text(ProjectStatusSupport.statusLabel($0)).tag($0)
                }
            }
            .pickerStyle(.segmented)
            field("License ID", text: $licenseId, systemImage: "doc.text")

            heading("Links")
            field("Source code", text: $sourceUrl, systemImage: "link")
            field("Issue tracker", text: $issuesUrl, systemImage: "ant")
            field("Wiki", text: $wikiUrl, systemImage: "book")
            field("Discord", text: $discordUrl, systemImage: "bubble.left.and.bubble.right")

            if let error = localError ?? model.projectUpdateError ?? model.projectActionError {
                Text(error).font(.caption).foregroundStyle(.red)
            }

            Button { Task { await save() } } label: {
                HStack {
                    if model.isProjectSaving { ProgressView().tint(.white) }
                    else { Image(systemName: model.projectUpdateSuccess ? "checkmark" : "square.and.arrow.down") }
                    Text(LocalizedStringKey(model.projectUpdateSuccess ? "Saved" : "Save changes"))
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(hasChanges ? Color.ryntraGreen : Color.secondary.opacity(0.3), in: RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(.white)
            }
            .disabled(!hasChanges || model.isProjectSaving || title.isEmpty || summary.isEmpty)
        }
        .padding(.bottom, 24)
        .onChange(of: iconItem) { item in
            guard let item else { return }
            Task { await uploadIcon(item) }
        }
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
        if model.projectUpdateError == nil { await onSaved() }
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

    private func heading(_ title: String) -> some View {
        Text(LocalizedStringKey(title)).font(.headline.bold()).padding(.top, 8)
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
}
