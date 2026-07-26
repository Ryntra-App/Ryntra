import RyntraShared
import SwiftUI
import UniformTypeIdentifiers

struct ProjectVersionsManagementView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let versions: [ProjectVersion]
    let isReadOnly: Bool
    let isLoading: Bool
    let errorMessage: String?
    let onReload: () async -> Void

    @State private var editingVersion: ProjectVersion?
    @State private var isCreating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Releases").font(.title3.bold())
                Spacer()
                if !isReadOnly {
                    Button { isCreating = true } label: {
                        Image(systemName: "plus").frame(width: 34, height: 34)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ryntraGreen)
                }
            }

            if isLoading {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Loading releases").foregroundStyle(.secondary)
                }
                .padding(.vertical, 28)
            } else if let errorMessage, versions.isEmpty {
                managementEmpty(title: "Versions unavailable", message: errorMessage)
            } else if versions.isEmpty {
                managementEmpty(title: "No versions yet", message: "Published releases for this project will appear here.")
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(versions, id: \.id) { version in
                        ManagedVersionCard(version: version) {
                            if !isReadOnly { editingVersion = version }
                        }
                    }
                }
            }

            if let error = model.projectActionError {
                Text(error).font(.caption).foregroundStyle(.red)
            }
        }
        .sheet(isPresented: $isCreating) {
            VersionEditorSheet(project: project, version: nil) {
                await onReload()
                isCreating = false
            }
        }
        .sheet(
            isPresented: Binding(
                get: { editingVersion != nil },
                set: { if !$0 { editingVersion = nil } }
            )
        ) {
            if let editingVersion {
                VersionEditorSheet(project: project, version: editingVersion) {
                    await onReload()
                    self.editingVersion = nil
                }
            }
        }
    }

    private func managementEmpty(title: String, message: String) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(LocalizedStringKey(title)).font(.headline)
            Text(LocalizedStringKey(message)).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct ManagedVersionCard: View {
    let version: ProjectVersion
    let onOpen: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: onOpen) {
                VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(version.versionNumber).font(.headline.bold())
                        Text(version.name).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                    }
                    Spacer()
                    Label(ryntraExactCount(version.downloads), systemImage: "arrow.down")
                        .font(.caption.bold())
                        .foregroundStyle(Color.ryntraGreen)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(version.gameVersions, id: \.self) { chip($0, accent: false) }
                        ForEach(version.loaders, id: \.self) { chip($0.uppercased(), accent: true) }
                    }
                }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
            }
            .buttonStyle(.plain)
            Divider()
        }
    }

    private func chip(_ text: String, accent: Bool) -> some View {
        Text(text)
            .font(.caption2.bold())
            .foregroundStyle(accent ? Color.ryntraGreen : Color.primary)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(accent ? Color.ryntraGreen.opacity(0.12) : Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 7))
    }

}

private struct VersionEditorSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let project: Project
    let version: ProjectVersion?
    let onSaved: () async -> Void

    @State private var name: String
    @State private var versionNumber: String
    @State private var changelog: String
    @State private var gameVersions: String
    @State private var loaders: String
    @State private var versionType: String
    @State private var isFeatured: Bool
    @State private var changelogMode = 0
    @State private var changelogPreview: [MarkdownBlock] = []
    @State private var selectedFiles: [ProjectFileUpload] = []
    @State private var selectedFileNames: [String] = []
    @State private var selectedFileSizes: [Int] = []
    @State private var primaryFileIndex = 0
    @State private var isReadingFiles = false
    @State private var isFileImporterPresented = false
    @State private var localError: String?

    init(project: Project, version: ProjectVersion?, onSaved: @escaping () async -> Void) {
        self.project = project
        self.version = version
        self.onSaved = onSaved
        _name = State(initialValue: version?.name ?? "")
        _versionNumber = State(initialValue: version?.versionNumber ?? "")
        _changelog = State(initialValue: version?.changelog ?? "")
        _gameVersions = State(initialValue: version?.gameVersions.joined(separator: ", ") ?? "")
        _loaders = State(initialValue: version?.loaders.joined(separator: ", ") ?? "")
        _versionType = State(initialValue: version?.versionType ?? "release")
        _isFeatured = State(initialValue: version?.featured ?? false)
    }

    private var parsedGameVersions: [String] { csv(gameVersions) }
    private var parsedLoaders: [String] { csv(loaders) }
    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !versionNumber.trimmingCharacters(in: .whitespaces).isEmpty &&
        !parsedGameVersions.isEmpty && !parsedLoaders.isEmpty &&
        (version != nil || !selectedFiles.isEmpty) && !isReadingFiles
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Version") {
                    TextField("Name", text: $name)
                    TextField("Version number", text: $versionNumber)
                    Picker("Release channel", selection: $versionType) {
                        Text("Release").tag("release")
                        Text("Beta").tag("beta")
                        Text("Alpha").tag("alpha")
                    }
                    Toggle("Featured", isOn: $isFeatured)
                }
                Section("Compatibility") {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("Minecraft versions").font(.caption).foregroundStyle(.secondary)
                        TextField("1.21.5, 1.21.4", text: $gameVersions)
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Loaders").font(.caption).foregroundStyle(.secondary)
                        TextField("fabric, quilt", text: $loaders)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 7) {
                                ForEach(["fabric", "forge", "neoforge", "quilt"], id: \.self) { loader in
                                    loaderChip(loader)
                                }
                            }
                        }
                    }
                }
                Section("Changelog") {
                    Picker("Changelog mode", selection: $changelogMode) {
                        Text("Write").tag(0)
                        Text("Preview").tag(1)
                    }
                    .pickerStyle(.segmented)
                    if changelogMode == 0 {
                        TextEditor(text: $changelog)
                            .frame(minHeight: 170)
                            .scrollContentBackground(.hidden)
                            .background(Color.ryntraSurface)
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            if changelogPreview.isEmpty {
                                Text("Nothing to preview").font(.caption).foregroundStyle(.secondary)
                            }
                            ForEach(Array(changelogPreview.enumerated()), id: \.offset) { _, block in
                                MarkdownBlockView(block: block)
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 170, alignment: .topLeading)
                    }
                }
                if version == nil {
                    Section("Files") {
                        Button {
                            isFileImporterPresented = true
                        } label: {
                            Label(isReadingFiles ? "Reading files" : "Add version files", systemImage: "doc.badge.plus")
                        }
                        .disabled(isReadingFiles)
                        ForEach(selectedFileNames.indices, id: \.self) { index in
                            HStack {
                                Button {
                                    primaryFileIndex = index
                                } label: {
                                    Image(systemName: primaryFileIndex == index ? "checkmark.circle.fill" : "circle")
                                        .foregroundStyle(primaryFileIndex == index ? Color.ryntraGreen : Color.secondary)
                                }
                                .buttonStyle(.plain)
                                Text(selectedFileNames[index]).lineLimit(1)
                                Spacer()
                                Button(role: .destructive) { removeFile(at: index) } label: {
                                    Image(systemName: "trash")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } else if let version, !version.files.isEmpty {
                    Section("Files") {
                        ForEach(version.files, id: \.url) { file in
                            if let fileURL = URL(string: file.url) {
                                Link(file.filename, destination: fileURL)
                            } else {
                                Text(file.filename)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                if let error = localError ?? model.projectActionError {
                    Section { Text(error).foregroundStyle(.red) }
                }
                if let version {
                    Section {
                        Button(role: .destructive) {
                            Task {
                                do {
                                    try await model.deleteVersion(versionID: version.id)
                                    await onSaved()
                                    dismiss()
                                } catch { localError = error.localizedDescription }
                            }
                        } label: {
                            Label("Delete version", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle(version == nil ? "Create version" : "Edit version")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { Task { await save() } }
                        .disabled(!canSave || model.isProjectActionRunning)
                }
            }
            .fileImporter(
                isPresented: $isFileImporterPresented,
                allowedContentTypes: [.data, .archive, .zip],
                allowsMultipleSelection: true
            ) { result in
                Task { await importFiles(result) }
            }
            .onChange(of: changelogMode) { mode in
                if mode == 1 {
                    changelogPreview = MarkdownParser.shared.parse(markdown: changelog)
                }
            }
        }
    }

    @MainActor
    private func save() async {
        localError = nil
        do {
            if let version {
                let update = VersionUpdate(
                    name: name,
                    versionNumber: versionNumber,
                    changelog: changelog,
                    dependencies: nil,
                    gameVersions: parsedGameVersions,
                    versionType: versionType,
                    loaders: parsedLoaders,
                    featured: KotlinBoolean(bool: isFeatured),
                    status: nil
                )
                try await model.updateVersion(versionID: version.id, update: update)
            } else if !selectedFiles.isEmpty {
                let request = CreateVersionRequest(
                    name: name,
                    versionNumber: versionNumber,
                    changelog: changelog,
                    dependencies: [],
                    gameVersions: parsedGameVersions,
                    versionType: versionType,
                    loaders: parsedLoaders,
                    featured: isFeatured,
                    files: selectedFiles,
                    primaryFileIndex: Int32(primaryFileIndex)
                )
                try await model.createVersion(project: project, request: request)
            }
            await onSaved()
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }

    private func csv(_ value: String) -> [String] {
        Array(Set(value.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty })).sorted()
    }

    private func loaderChip(_ loader: String) -> some View {
        let isSelected = parsedLoaders.contains(loader)
        return Button {
            var values = parsedLoaders
            if isSelected { values.removeAll { $0 == loader } } else { values.append(loader) }
            loaders = values.sorted().joined(separator: ", ")
        } label: {
            Text(loader.capitalized)
                .font(.caption.weight(.semibold))
                .foregroundStyle(isSelected ? Color.ryntraGreen : Color.secondary)
                .padding(.horizontal, 9)
                .padding(.vertical, 6)
                .background(isSelected ? Color.ryntraGreen.opacity(0.12) : Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 7))
        }
        .buttonStyle(.plain)
    }

    @MainActor
    private func importFiles(_ result: Result<[URL], Error>) async {
        isReadingFiles = true
        defer { isReadingFiles = false }
        do {
            let urls = try result.get()
            let payloads = try await Task.detached(priority: .userInitiated) {
                try urls.map { url -> (String, String, String, Int) in
                    let hasAccess = url.startAccessingSecurityScopedResource()
                    defer { if hasAccess { url.stopAccessingSecurityScopedResource() } }
                    let data = try Data(contentsOf: url, options: .mappedIfSafe)
                    let mime = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType ?? "application/octet-stream"
                    return (url.lastPathComponent, mime, data.base64EncodedString(), data.count)
                }
            }.value
            let totalSize = payloads.reduce(selectedFileSizes.reduce(0, +)) { $0 + $1.3 }
            guard totalSize <= 128 * 1024 * 1024 else {
                localError = "Version files must be 128 MiB or smaller in total."
                return
            }
            for payload in payloads where !selectedFileNames.contains(payload.0) {
                selectedFiles.append(
                    ProjectUploadFactory.shared.fromBase64(
                        fileName: payload.0,
                        contentType: payload.1,
                        base64: payload.2
                    )
                )
                selectedFileNames.append(payload.0)
                selectedFileSizes.append(payload.3)
            }
            localError = nil
        } catch {
            localError = error.localizedDescription
        }
    }

    private func removeFile(at index: Int) {
        selectedFiles.remove(at: index)
        selectedFileNames.remove(at: index)
        selectedFileSizes.remove(at: index)
        primaryFileIndex = min(primaryFileIndex, max(selectedFiles.count - 1, 0))
    }
}
